package io.github.manjago.anakata.console;

import io.github.manjago.anakata.core.game.Command;
import io.github.manjago.anakata.core.game.GameSession;
import io.github.manjago.anakata.core.game.Outcome;
import io.github.manjago.anakata.core.game.Reply;
import io.github.manjago.anakata.descriptions.CommandParser;
import io.github.manjago.anakata.descriptions.CommandWords;
import io.github.manjago.anakata.descriptions.OutcomeDescriber;
import io.github.manjago.anakata.descriptions.RoomDescriber;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

/**
 * Игровой цикл консоли: показ комнаты, ввод строки, разбор, исполнение, ответ.
 *
 * <p>Консоль — инструмент администратора, а не интерфейс игрока: цикл лишь
 * подключает ввод-вывод к ядру, а слова и тексты берёт из пакета описаний.
 * Поведение адаптера (что показать, когда выйти) — здесь; само ядро про строки и
 * про выход не знает.
 *
 * <p>После удачного хода показывается комната, после неудачного — только нарратив
 * исхода: комната та же, и повторять её незачем. Обычный ход нарратива не требует,
 * о нём рассказывает комната.
 */
public final class ConsoleGame {

    private final BufferedReader in;
    private final PrintWriter out;
    private final GameSession session;
    private final CommandParser parser = new CommandParser();
    private final RoomDescriber rooms = new RoomDescriber();
    private final OutcomeDescriber outcomes = new OutcomeDescriber();

    /**
     * @param in      источник строк
     * @param out     приёмник текста
     * @param session начатая игровая сессия
     */
    public ConsoleGame(BufferedReader in, PrintWriter out, GameSession session) {
        this.in = Objects.requireNonNull(in, "ввод");
        this.out = Objects.requireNonNull(out, "вывод");
        this.session = Objects.requireNonNull(session, "сессия");
    }

    /** Ведёт цикл до выхода игрока или конца ввода. */
    public void run() {
        printLines(rooms.describe(session.roomView()));
        while (true) {
            out.print(ConsoleText.PROMPT);
            out.flush();
            String line = readLine();
            if (line == null) {
                out.println(ConsoleText.farewell());
                return;
            }
            switch (parser.parse(line)) {
                case CommandParser.ParseResult.Parsed parsed -> play(parsed.command());
                case CommandParser.ParseResult.Help _ ->
                        printLines(CommandWords.help(session.level().grid().dimensions()));
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

    private void play(Command command) {
        Reply reply = session.execute(command);
        printLines(outcomes.describe(reply.outcome()));
        if (showsRoom(reply.outcome())) {
            printLines(rooms.describe(reply.room()));
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
