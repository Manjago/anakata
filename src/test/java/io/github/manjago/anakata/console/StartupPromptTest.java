package io.github.manjago.anakata.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.manjago.anakata.core.level.LevelParams;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StartupPromptTest {

    private static final long RANDOM_SEED = 424242L;

    @Test
    void readsSizesSeedAndDensity() {
        Result result = run("3 4\n7\n5\n");

        assertEquals(Optional.of(new LevelParams(7L, new int[] {3, 4}, 5)), result.params());
    }

    /** Размерность — число чисел: четыре числа дают 4D. */
    @Test
    void dimensionIsTheNumberOfSizes() {
        Result result = run("3 3 3 3\n1\n1\n");

        LevelParams params = result.params().orElseThrow();
        assertEquals(4, params.sizes().length);
    }

    @Test
    void blankSizesFallBackToDefault() {
        Result result = run("\n9\n2\n");

        assertEquals(Optional.of(new LevelParams(9L, new int[] {6, 6}, 2)), result.params());
    }

    @Test
    void blankSeedUsesTheInjectedRandomSource() {
        Result result = run("3 3\n\n2\n");

        assertEquals(RANDOM_SEED, result.params().orElseThrow().seed());
    }

    @Test
    void blankDensityFallsBackToDefault() {
        Result result = run("3 3\n1\n\n");

        assertEquals(StartupPrompt.DEFAULT_DENSITY, result.params().orElseThrow().landmarkInEvery());
    }

    @Test
    void repeatedInvalidSizesAreReasked() {
        Result result = run("6\n1 1\na b\n5 5 5 5 5\n3 3\n1\n2\n");

        assertEquals(Optional.of(new LevelParams(1L, new int[] {3, 3}, 2)), result.params());
    }

    /** Нечисловые размеры — та же подсказка «Не понял размеры», что и у остальных. */
    @Test
    void nonNumericSizesAreReportedAsSizeProblem() {
        Result result = run("a b\n3 3\n1\n2\n");

        assertEquals(Optional.of(new LevelParams(1L, new int[] {3, 3}, 2)), result.params());
        assertTrue(result.output().contains("Не понял размеры"), result.output());
    }

    /** Одна ось — не размерность: причину даёт {@code Grid}, поле переспрашивается. */
    @Test
    void singleAxisIsReasked() {
        Result result = run("6\n3 3\n1\n2\n");

        assertEquals(Optional.of(new LevelParams(1L, new int[] {3, 3}, 2)), result.params());
    }

    /** Причина от {@code Grid} называет позицию с единицы: «2 1» — второе число. */
    @Test
    void sizesProblemNamesPositionFromOne() {
        Result result = run("2 1\n3 3\n1\n2\n");

        assertEquals(Optional.of(new LevelParams(1L, new int[] {3, 3}, 2)), result.params());
        assertTrue(result.output().contains("№2"), result.output());
    }

    /**
     * Приглашение размеров показывает дефолт. Ожидание строим из константы, а не
     * из {@code ConsoleText.askSizes}: иначе тест сравнивал бы вывод с самим собой
     * и не заметил бы выпавший дефолт.
     */
    @Test
    void sizesPromptShowsTheDefault() {
        Result result = run("\n1\n2\n");

        assertTrue(result.output().contains("пусто — " + sizesText(StartupPrompt.defaultSizes())),
                result.output());
    }

    /** Приглашение плотности показывает дефолт — тем же независимым способом. */
    @Test
    void densityPromptShowsTheDefault() {
        Result result = run("3 3\n1\n\n");

        assertTrue(result.output().contains("пусто — " + StartupPrompt.DEFAULT_DENSITY),
                result.output());
    }

    private static String sizesText(int[] sizes) {
        StringBuilder text = new StringBuilder();
        for (int size : sizes) {
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(size);
        }
        return text.toString();
    }

    /**
     * Размеры сверх потолка клеток консоли переспрашиваются, а не уходят в
     * {@code OutOfMemoryError} при сборке уровня.
     */
    @Test
    void sizesOverTheCellCeilingAreReaskedNotThrown() {
        Result result = run("20000 20000\n3 3\n1\n2\n");

        assertEquals(Optional.of(new LevelParams(1L, new int[] {3, 3}, 2)), result.params());
        assertTrue(result.output().contains("потолок"), result.output());
    }

    /** Клеток больше {@code Integer.MAX_VALUE}: тоже переспрос, а не стектрейс. */
    @Test
    void sizesOverIntegerMaxCellsAreReaskedNotThrown() {
        Result result = run("100000 100000\n3 3\n1\n2\n");

        assertEquals(Optional.of(new LevelParams(1L, new int[] {3, 3}, 2)), result.params());
    }

    @Test
    void invalidSeedIsReasked() {
        Result result = run("3 3\nzzz\n12\n2\n");

        assertEquals(12L, result.params().orElseThrow().seed());
    }

    @Test
    void densityBelowOneIsReasked() {
        Result result = run("3 3\n1\n0\n-4\n4\n");

        assertEquals(4, result.params().orElseThrow().landmarkInEvery());
    }

    /** Конец ввода до конца расспроса — не ошибка, а сигнал «не начинать». */
    @Test
    void endOfInputStopsThePrompt() {
        Result result = run("");

        assertTrue(result.params().isEmpty());
        assertTrue(result.output().contains(ConsoleText.askSizes(StartupPrompt.defaultSizes())),
                result.output());
    }

    @Test
    void endOfInputInTheMiddleStopsThePrompt() {
        Result result = run("3 3\n");

        assertTrue(result.params().isEmpty());
        assertTrue(result.output().contains(ConsoleText.ASK_SEED), result.output());
    }

    /**
     * Слово выхода распознаётся сразу, а не как непонятое поле: подсказки
     * «не понял» не должно быть. Проверяем именно это, иначе тест проходил бы и на
     * простом конце ввода.
     */
    @Test
    void exitWordStopsSizesPrompt() {
        Result result = run("q\n");

        assertTrue(result.params().isEmpty());
        assertFalse(result.output().contains(ConsoleText.SIZES_HINT), result.output());
    }

    @Test
    void exitWordStopsSeedPrompt() {
        Result result = run("3 3\nq\n");

        assertTrue(result.params().isEmpty());
        assertFalse(result.output().contains(ConsoleText.SEED_HINT), result.output());
    }

    @Test
    void exitWordStopsDensityPrompt() {
        Result result = run("3 3\n1\nвыход\n");

        assertTrue(result.params().isEmpty());
        assertFalse(result.output().contains(ConsoleText.DENSITY_HINT), result.output());
    }

    private static Result run(String input) {
        BufferedReader in = new BufferedReader(new StringReader(input));
        StringWriter output = new StringWriter();
        PrintWriter out = new PrintWriter(output, true);
        StartupPrompt prompt = new StartupPrompt(in, out, () -> RANDOM_SEED);

        Optional<LevelParams> params = prompt.ask();
        return new Result(params, output.toString());
    }

    private record Result(Optional<LevelParams> params, String output) {
    }
}
