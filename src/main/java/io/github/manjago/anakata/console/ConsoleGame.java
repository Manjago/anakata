package io.github.manjago.anakata.console;

import io.github.manjago.anakata.core.game.Command;
import io.github.manjago.anakata.core.game.GameSession;
import io.github.manjago.anakata.core.game.Outcome;
import io.github.manjago.anakata.core.game.Reply;
import io.github.manjago.anakata.core.level.Level;
import io.github.manjago.anakata.core.level.LevelParams;
import io.github.manjago.anakata.core.maze.Grid;
import io.github.manjago.anakata.descriptions.CommandParser;
import io.github.manjago.anakata.descriptions.CommandWords;
import io.github.manjago.anakata.descriptions.OutcomeDescriber;
import io.github.manjago.anakata.descriptions.RoomDescriber;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.function.LongSupplier;

/**
 * Игровой цикл консоли: показ комнаты, ввод строки, разбор, исполнение, ответ.
 *
 * <p>Консоль — инструмент администратора, а не интерфейс игрока: цикл лишь
 * подключает ввод-вывод к ядру, а слова и тексты берёт из пакета описаний;
 * админ-команды разбирает и исполняет сам адаптер (см. DESIGN.md, «Уровень и
 * игровой цикл»). Поведение адаптера (что показать, когда выйти) — здесь; само
 * ядро про строки и про выход не знает.
 *
 * <p>После удачного хода показывается комната, после неудачного — только нарратив
 * исхода: комната та же, и повторять её незачем. Обычный ход нарратива не требует,
 * о нём рассказывает комната. Админ-команды, наоборот, показывают свой результат:
 * телепорт и пересборка рисуют комнату, остальные печатают ответ.
 */
public final class ConsoleGame {

    private final BufferedReader in;
    private final PrintWriter out;
    private final LongSupplier randomSeed;
    private final CommandParser parser = new CommandParser();
    private final AdminParser adminParser = new AdminParser();
    private final RoomDescriber rooms = new RoomDescriber();
    private final OutcomeDescriber outcomes = new OutcomeDescriber();

    /** Сессия сменяемая: «новый уровень» пересобирает её на месте. */
    private GameSession session;

    /** Показывать ли координаты клетки перед комнатой; тумблер «координаты». */
    private boolean showCoordinates;

    /**
     * @param in         источник строк
     * @param out        приёмник текста
     * @param session    начатая игровая сессия
     * @param randomSeed источник сида для «новый уровень» без аргумента
     */
    public ConsoleGame(BufferedReader in, PrintWriter out, GameSession session,
            LongSupplier randomSeed) {
        this.in = Objects.requireNonNull(in, "ввод");
        this.out = Objects.requireNonNull(out, "вывод");
        this.session = Objects.requireNonNull(session, "сессия");
        this.randomSeed = Objects.requireNonNull(randomSeed, "источник сида");
    }

    /** Ведёт цикл до выхода игрока или конца ввода. */
    public void run() {
        printRoom();
        while (true) {
            out.print(ConsoleText.PROMPT);
            out.flush();
            String line = readLine();
            if (line == null) {
                out.println(ConsoleText.farewell());
                return;
            }
            if (handleAdmin(line)) {
                continue;
            }
            switch (parser.parse(line)) {
                case CommandParser.ParseResult.Parsed parsed -> play(parsed.command());
                case CommandParser.ParseResult.Help _ -> printHelp();
                case CommandParser.ParseResult.Empty _ -> {
                    // Пустой ввод — просто переспросить, а не ошибка игрока.
                }
                case CommandParser.ParseResult.Exit _ -> {
                    out.println(ConsoleText.farewell());
                    return;
                }
                case CommandParser.ParseResult.Unknown unknown ->
                        out.println(ConsoleText.unknown(unknown.word()));
            }
        }
    }

    /**
     * Пробует разобрать и исполнить админ-команду.
     *
     * @return {@code true}, если строка была админской и обработана здесь
     */
    private boolean handleAdmin(String line) {
        return switch (adminParser.parse(line)) {
            case AdminParser.Result.Parsed parsed -> {
                executeAdmin(parsed.command());
                yield true;
            }
            case AdminParser.Result.Invalid invalid -> {
                out.println(invalid.message());
                yield true;
            }
            case AdminParser.Result.NotAdmin _ -> false;
        };
    }

    private void executeAdmin(AdminCommand command) {
        switch (command) {
            case AdminCommand.Seed _ -> out.println(AdminText.seed(session.level().params().seed()));
            case AdminCommand.Coordinates _ -> toggleCoordinates();
            case AdminCommand.Teleport teleport -> teleport(teleport.coordinates());
            case AdminCommand.NewLevel newLevel -> newLevel(newLevel.seed());
            case AdminCommand.Stats _ -> printLines(AdminText.stats(session.level()));
        }
    }

    private void toggleCoordinates() {
        showCoordinates = !showCoordinates;
        if (showCoordinates) {
            out.println(currentCoordinates());
        } else {
            out.println(AdminText.COORDINATES_HIDDEN);
        }
    }

    private void teleport(List<Integer> coordinates) {
        Grid grid = session.level().grid();
        if (coordinates.size() != grid.dimensions()) {
            out.println(AdminText.teleportHint(grid));
            return;
        }
        int cell;
        try {
            cell = grid.index(coordinates.stream().mapToInt(Integer::intValue).toArray());
        } catch (IndexOutOfBoundsException outside) {
            out.println(AdminText.teleportProblem(outside.getMessage()));
            return;
        }
        session.teleport(cell);
        printRoom();
    }

    /**
     * Пересобирает уровень из тех же размеров и плотности: сид задан явно, иначе
     * случайный. Настройка показа координат переживает смену уровня — это свойство
     * консоли, а не уровня.
     */
    private void newLevel(OptionalLong seed) {
        LevelParams current = session.level().params();
        long chosen = seed.orElseGet(randomSeed::getAsLong);
        Level level = Level.build(new LevelParams(chosen, current.sizes(), current.landmarkInEvery()));
        session = new GameSession(level);
        out.println(ConsoleText.level(level));
        printRoom();
    }

    private void play(Command command) {
        Reply reply = session.execute(command);
        printLines(outcomes.describe(reply.outcome()));
        if (showsRoom(reply.outcome())) {
            printRoom();
        }
    }

    /**
     * Показывать ли комнату после исхода. Switch-выражение по {@link Outcome} —
     * намеренно: у нового исхода не найдётся ветки, и сборка сломается, а не
     * проглотит случай молча.
     */
    private static boolean showsRoom(Outcome outcome) {
        return switch (outcome) {
            case MOVED -> true;
            case BLOCKED -> false;
        };
    }

    private void printRoom() {
        if (showCoordinates) {
            out.println(currentCoordinates());
        }
        printLines(rooms.describe(session.roomView()));
    }

    /** Координаты текущей клетки строкой вида {@code [1 2 3]}. */
    private String currentCoordinates() {
        int[] coordinates = new int[session.level().grid().dimensions()];
        session.level().grid().coordinates(session.cell(), coordinates);
        return AdminText.coordinates(coordinates);
    }

    /** Помощь: слова мира из описаний плюс админ-строки адаптера. */
    private void printHelp() {
        List<String> help = new ArrayList<>(
                CommandWords.help(session.level().grid().dimensions()));
        help.addAll(AdminText.help());
        printLines(help);
    }

    private void printLines(List<String> lines) {
        for (String line : lines) {
            out.println(line);
        }
    }

    private String readLine() {
        try {
            return in.readLine();
        } catch (IOException failure) {
            throw new UncheckedIOException("не удалось прочитать ввод", failure);
        }
    }
}
