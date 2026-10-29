package io.github.manjago.anakata.console;

import io.github.manjago.anakata.core.level.LevelParams;
import io.github.manjago.anakata.core.maze.Grid;
import io.github.manjago.anakata.descriptions.CommandParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Расспрос параметров уровня при старте консоли: размеры, сид, плотность
 * ориентиров.
 *
 * <p>Размерность задаётся числом чисел в строке размеров: {@code 6 6} — 2D,
 * {@code 5 5 5} — 3D, {@code 4 4 4 4} — 4D, поэтому отдельного вопроса о
 * размерности нет. Пустое поле — значение по умолчанию: размеры {@code 6 6}, сид
 * случайный, плотность {@link #DEFAULT_DENSITY}. Непонятое поле переспрашивается,
 * а не роняет старт. Шапку собранного уровня печатает уже {@link ConsoleApplication}, когда
 * уровень готов и приметные комнаты можно посчитать.
 *
 * <p><b>Размеры проверяет {@link Grid}, а не свой список правил.</b> Иначе
 * валидация разъедется с ядром (её источник правды — {@code Grid}), и ввод
 * вроде {@code 100000 100000} уронил бы старт стектрейсом вместо переспроса.
 * Сверх этого у консоли есть свой потолок клеток {@link #MAX_CELLS}: ядру он не
 * нужен (бот ограничит по-своему), а админскому инструменту — чтобы опечатка в
 * размерах не ушла в {@code OutOfMemoryError}.
 *
 * <p>Случайность и ввод-вывод — здесь, в адаптере, а не в ядре. Источник
 * случайного сида инъектируется ({@link LongSupplier}), поэтому расспрос
 * проверяется тестом без настоящей случайности.
 */
public final class StartupPrompt {

    /**
     * Потолок числа клеток уровня для консоли.
     *
     * <p>Миллион — с большим запасом: любой осмысленный админский уровень
     * (100×100 в 2D, 31×31×31 в 3D, 31⁴ в 4D) в него укладывается, а генерация
     * съедает около пяти массивов по клетке (visited, стек, маски) — считанные
     * мегабайты, то есть доли секунды на дефолтной куче. Всё, что больше, — почти
     * наверняка опечатка в размерах, и её дешевле переспросить, чем ловить OOM.
     */
    static final int MAX_CELLS = 1_000_000;

    /**
     * Плотность ориентиров по умолчанию: примерно одна приметная комната раз в
     * пять. Единственный источник значения — {@link ConsoleText#askDensity(int)}
     * печатает приглашение из этой константы, а не набирает число руками.
     * Значение временное: калибруется игрой (см. DESIGN.md, открытые вопросы).
     */
    static final int DEFAULT_DENSITY = 5;

    /** Размеры по умолчанию; {@link ConsoleText#askSizes} печатает их из этого поля. */
    private static final int[] DEFAULT_SIZES = {6, 6};

    /**
     * Копия размеров по умолчанию. Отдаём копию, а не сам массив: иначе тест (или
     * любой класс пакета) мог бы записать в него значение и тихо поменять дефолт.
     */
    static int[] defaultSizes() {
        return DEFAULT_SIZES.clone();
    }

    private final BufferedReader in;
    private final PrintWriter out;
    private final LongSupplier randomSeed;
    private final CommandParser parser = new CommandParser();

    /**
     * @param in         источник строк
     * @param out        приёмник подсказок
     * @param randomSeed источник случайного сида, когда поле оставлено пустым
     */
    public StartupPrompt(BufferedReader in, PrintWriter out, LongSupplier randomSeed) {
        this.in = Objects.requireNonNull(in, "ввод");
        this.out = Objects.requireNonNull(out, "вывод");
        this.randomSeed = Objects.requireNonNull(randomSeed, "источник сида");
    }

    /**
     * Спрашивает параметры уровня, переспрашивая непонятые поля.
     *
     * @return параметры уровня или пусто, если ввод закончился либо введено слово
     *         выхода до конца расспроса
     */
    public Optional<LevelParams> ask() {
        Optional<int[]> sizes = askSizes();
        if (sizes.isEmpty()) {
            return Optional.empty();
        }
        Optional<Long> seed = askSeed();
        if (seed.isEmpty()) {
            return Optional.empty();
        }
        Optional<Integer> density = askDensity();
        if (density.isEmpty()) {
            return Optional.empty();
        }
        LevelParams params = new LevelParams(seed.get(), sizes.get(), density.get());
        return Optional.of(params);
    }

    private Optional<int[]> askSizes() {
        while (true) {
            Optional<String> line = prompt(ConsoleText.askSizes(DEFAULT_SIZES));
            if (line.isEmpty()) {
                return Optional.empty();
            }
            String trimmed = line.get().strip();
            if (trimmed.isEmpty()) {
                return Optional.of(DEFAULT_SIZES.clone());
            }
            Optional<int[]> sizes = parseNumbers(trimmed);
            if (sizes.isEmpty()) {
                out.println(ConsoleText.sizesNotNumbers());
                continue;
            }
            String problem = sizesProblem(sizes.get());
            if (problem != null) {
                out.println(ConsoleText.sizesProblem(problem));
                continue;
            }
            return sizes;
        }
    }

    private Optional<Long> askSeed() {
        while (true) {
            Optional<String> line = prompt(ConsoleText.ASK_SEED);
            if (line.isEmpty()) {
                return Optional.empty();
            }
            String trimmed = line.get().strip();
            if (trimmed.isEmpty()) {
                return Optional.of(randomSeed.getAsLong());
            }
            try {
                return Optional.of(Long.parseLong(trimmed));
            } catch (NumberFormatException notANumber) {
                out.println(ConsoleText.SEED_HINT);
            }
        }
    }

    private Optional<Integer> askDensity() {
        while (true) {
            Optional<String> line = prompt(ConsoleText.askDensity(DEFAULT_DENSITY));
            if (line.isEmpty()) {
                return Optional.empty();
            }
            String trimmed = line.get().strip();
            if (trimmed.isEmpty()) {
                return Optional.of(DEFAULT_DENSITY);
            }
            int density;
            try {
                density = Integer.parseInt(trimmed);
            } catch (NumberFormatException notANumber) {
                out.println(ConsoleText.DENSITY_HINT);
                continue;
            }
            if (density < 1) {
                out.println(ConsoleText.DENSITY_HINT);
                continue;
            }
            return Optional.of(density);
        }
    }

    /**
     * Печатает приглашение и читает строку. Пусто — конец ввода или слово выхода:
     * выход распознаётся теми же словами и нормализацией, что и в игре, поэтому
     * {@code q} работает и на этапе расспроса.
     */
    private Optional<String> prompt(String question) {
        out.print(question);
        out.flush();
        String line = readLine();
        if (line == null || parser.isExit(line)) {
            return Optional.empty();
        }
        return Optional.of(line);
    }

    private String readLine() {
        try {
            return in.readLine();
        } catch (IOException failure) {
            throw new UncheckedIOException("не удалось прочитать ввод", failure);
        }
    }

    /** Все пробелы — числа через пробел; пусто, если хоть один токен не число. */
    private static Optional<int[]> parseNumbers(String trimmed) {
        String[] parts = trimmed.split("\\s+");
        int[] numbers = new int[parts.length];
        for (int index = 0; index < parts.length; index++) {
            try {
                numbers[index] = Integer.parseInt(parts[index]);
            } catch (NumberFormatException notANumber) {
                return Optional.empty();
            }
        }
        return Optional.of(numbers);
    }

    /**
     * Собирает {@link Grid} из введённых размеров — источник правды о форме и
     * границах — и возвращает причину, по которой уровень не построить, или
     * {@code null}, если размеры годны.
     */
    private static String sizesProblem(int[] sizes) {
        Grid grid;
        try {
            grid = new Grid(sizes);
        } catch (IllegalArgumentException badShape) {
            return badShape.getMessage();
        }
        if (grid.cells() > MAX_CELLS) {
            return "слишком много клеток: " + grid.cells() + " (потолок " + MAX_CELLS + ")";
        }
        return null;
    }
}
