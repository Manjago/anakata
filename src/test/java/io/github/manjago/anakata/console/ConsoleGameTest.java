package io.github.manjago.anakata.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.manjago.anakata.core.game.GameSession;
import io.github.manjago.anakata.core.level.Level;
import io.github.manjago.anakata.core.level.LevelParams;
import io.github.manjago.anakata.core.level.LevelSupport;
import io.github.manjago.anakata.core.maze.Direction;
import io.github.manjago.anakata.descriptions.CommandWords;
import io.github.manjago.anakata.descriptions.RoomDescriber;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ConsoleGameTest {

    private static final LevelParams PARAMS = new LevelParams(42L, new int[] {3, 3}, 4);
    private static final RoomDescriber ROOMS = new RoomDescriber();
    private static final long RANDOM_SEED = 4242L;

    /** Пример телепорта из подсказки — от «например «телепорт » до закрывающей кавычки. */
    private static final Pattern HINT_EXAMPLE = Pattern.compile("например «телепорт ([\\d ]+?)»");

    @Test
    void showsStartingRoomAndBidsFarewellOnExit() {
        GameSession session = new GameSession(Level.build(PARAMS));

        String output = run(session, "выход\n");

        assertTrue(output.contains(roomLine(session.level(), session.cell())), output);
        assertTrue(output.contains(ConsoleText.farewell()), output);
    }

    /** Ход в открытый выход показывает комнату, куда игрок попал. */
    @Test
    void movingShowsTheDestinationRoom() {
        Level level = Level.build(PARAMS);
        GameSession session = new GameSession(level);
        Direction exit = session.roomView().exits().get(0);
        int destination = level.grid().neighbor(session.cell(), exit);

        String output = run(session, CommandWords.name(exit) + "\nвыход\n");

        assertTrue(output.contains(roomLine(level, destination)), output);
    }

    /** Ход в стену: только нарратив исхода, комнату не перерисовываем. */
    @Test
    void blockedMoveShowsNarrationWithoutRedrawingTheRoom() {
        Level level = Level.build(PARAMS);
        GameSession session = new GameSession(level);
        Direction wall = LevelSupport.blockedDirection(level, session.cell());
        assertNotNull(wall, "у клетки старта не нашлось стены");

        String output = run(session, CommandWords.name(wall) + "\nвыход\n");

        assertTrue(output.contains("Туда не пройти."), output);
        assertEquals(1, count(output, roomLine(level, session.cell())), output);
    }

    @Test
    void helpListsMovesAndExitOfTheDimension() {
        GameSession session = new GameSession(Level.build(PARAMS));

        String output = run(session, "помощь\nвыход\n");

        assertTrue(output.contains("Ходи так:"), output);
        assertTrue(output.contains(CommandWords.EXIT_NAME), output);
    }

    @Test
    void unknownWordIsReportedAndTheLoopContinues() {
        GameSession session = new GameSession(Level.build(PARAMS));

        String output = run(session, "абракадабра\nвыход\n");

        assertTrue(output.contains("Не понял: абракадабра."), output);
        assertTrue(output.contains(ConsoleText.farewell()), output);
    }

    /** Пустой ввод — просто переспросить, а не «не понял». */
    @Test
    void blankInputIsSilentlyReprompted() {
        GameSession session = new GameSession(Level.build(PARAMS));

        String output = run(session, "\nвыход\n");

        assertFalse(output.contains("Не понял"), output);
        assertTrue(output.contains(ConsoleText.farewell()), output);
    }

    @Test
    void exitAliasStopsTheLoop() {
        GameSession session = new GameSession(Level.build(PARAMS));

        String output = run(session, "q\n");

        assertTrue(output.contains(ConsoleText.farewell()), output);
        assertFalse(output.contains("Не понял"), output);
    }

    /** Конец ввода (Ctrl-D) — тоже выход, с прощанием. */
    @Test
    void endOfInputBidsFarewell() {
        GameSession session = new GameSession(Level.build(PARAMS));

        String output = run(session, "");

        assertTrue(output.contains(ConsoleText.farewell()), output);
    }

    @Test
    void seedCommandPrintsTheLevelSeed() {
        GameSession session = new GameSession(Level.build(PARAMS));

        String output = run(session, "сид\nвыход\n");

        assertTrue(output.contains(AdminText.seed(PARAMS.seed())), output);
    }

    /** Тумблер координат печатает их сразу и дальше перед каждой комнатой. */
    @Test
    void coordinatesToggleShowsCoordinatesAndKeepsThemAfterMoving() {
        Level level = Level.build(PARAMS);
        GameSession session = new GameSession(level);
        Direction exit = session.roomView().exits().get(0);
        int destination = level.grid().neighbor(session.cell(), exit);

        String output = run(session, "координаты\n" + CommandWords.name(exit) + "\nвыход\n");

        assertTrue(output.contains(coordinates(level, level.startCell())), output);
        assertTrue(output.contains(coordinates(level, destination)), output);
    }

    @Test
    void coordinatesCanBeTurnedOffAgain() {
        GameSession session = new GameSession(Level.build(PARAMS));

        String output = run(session, "координаты\nкоординаты\nвыход\n");

        assertTrue(output.contains(AdminText.COORDINATES_HIDDEN), output);
    }

    @Test
    void teleportMovesToGivenCoordinates() {
        Level level = Level.build(PARAMS);
        GameSession session = new GameSession(level);
        int target = level.grid().cells() - 1;

        String output = run(session, "телепорт " + inlineCoordinates(level, target) + "\nвыход\n");

        assertEquals(target, session.cell());
        assertTrue(output.contains(roomLine(level, target)), output);
    }

    @Test
    void teleportOutOfRangeReportsAndKeepsThePlayer() {
        Level level = Level.build(PARAMS);
        GameSession session = new GameSession(level);
        int start = session.cell();
        // Причину берём у Grid, а не литералом: правка текста в ядре не сломает тест.
        String reason = assertThrows(IndexOutOfBoundsException.class,
                () -> level.grid().index(99, 99)).getMessage();

        String output = run(session, "телепорт 99 99\nвыход\n");

        assertEquals(start, session.cell());
        assertTrue(output.contains(AdminText.teleportProblem(reason)), output);
    }

    /**
     * Число координат сверяется с размерностью уровня, и подсказка — точная под
     * этот уровень, а не захардкоженный диапазон.
     */
    @Test
    void teleportWithTooFewCoordinatesReportsTheLevelHint() {
        Level level = Level.build(PARAMS);
        GameSession session = new GameSession(level);

        String output = run(session, "телепорт 1\nвыход\n");

        assertTrue(output.contains(AdminText.teleportHint(level.grid())), output);
        assertEquals(level.startCell(), session.cell());
    }

    @Test
    void teleportWithTooManyCoordinatesReportsTheLevelHint() {
        Level level = Level.build(PARAMS);
        GameSession session = new GameSession(level);

        String output = run(session, "телепорт 0 0 0\nвыход\n");

        assertTrue(output.contains(AdminText.teleportHint(level.grid())), output);
        assertEquals(level.startCell(), session.cell());
    }

    /** Пример из подсказки — настоящая команда уже в 2D. */
    @Test
    void teleportHintExampleTeleportsInTwoDimensions() {
        assertHintExampleTeleports(new int[] {2, 2});
    }

    /** И в 3D с осями по 2: пример «номер оси» предлагал бы несуществующую клетку. */
    @Test
    void teleportHintExampleTeleportsInThreeDimensions() {
        assertHintExampleTeleports(new int[] {2, 2, 2});
    }

    /**
     * Берёт пример из текста подсказки (а не повторяет формулу — иначе тест
     * проверял бы сам себя) и отправляет его командой. Перед этим уходим в другую
     * клетку, чтобы прыжок в пример был настоящим, даже если старт совпал.
     */
    private static void assertHintExampleTeleports(int[] sizes) {
        Level level = Level.build(new LevelParams(42L, sizes, 4));
        GameSession session = new GameSession(level);

        String hintOutput = run(new GameSession(level), "телепорт 1\nвыход\n");
        String example = hintExampleFrom(hintOutput);
        int target = level.grid().index(parseCoordinates(example));
        int detour = (target + 1) % level.grid().cells();

        String output = run(session, "телепорт " + inlineCoordinates(level, detour)
                + "\nтелепорт " + example + "\nвыход\n");

        assertEquals(target, session.cell());
        assertTrue(output.contains(roomLine(level, target)), output);
    }

    /** Пример телепорта, выуженный из текста подсказки: «0 1». */
    private static String hintExampleFrom(String output) {
        Matcher matcher = HINT_EXAMPLE.matcher(output);
        assertTrue(matcher.find(), "в подсказке нет примера телепорта: " + output);
        return matcher.group(1);
    }

    private static int[] parseCoordinates(String text) {
        String[] parts = text.split(" ");
        int[] coordinates = new int[parts.length];
        for (int axis = 0; axis < parts.length; axis++) {
            coordinates[axis] = Integer.parseInt(parts[axis]);
        }
        return coordinates;
    }

    /**
     * Пересборку проверяем командой «сид»: она читает сид из сессии после замены,
     * поэтому падает, если сессия не сменилась. Шапка нового уровня для этого не
     * годится — она печатается и без замены сессии.
     */
    @Test
    void newLevelWithSeedRebuildsWithThatSeed() {
        GameSession session = new GameSession(Level.build(PARAMS));

        String output = run(session, "новый уровень 7\nсид\nвыход\n");

        assertTrue(output.contains(AdminText.seed(7L)), output);
    }

    @Test
    void newLevelWithoutSeedUsesTheInjectedSource() {
        GameSession session = new GameSession(Level.build(PARAMS));

        String output = run(session, "новый уровень\nсид\nвыход\n");

        assertTrue(output.contains(AdminText.seed(RANDOM_SEED)), output);
    }

    /**
     * Пересборка меняет только сид: размеры и плотность те же. Проверяем по
     * состоянию сессии после пересборки — «сид» ловит смену уровня, «статистика»
     * читает его размеры и плотность.
     */
    @Test
    void newLevelKeepsSizesAndDensity() {
        GameSession session = new GameSession(Level.build(PARAMS));

        String output = run(session, "новый уровень 8\nсид\nстатистика\nвыход\n");

        assertTrue(output.contains(AdminText.seed(8L)), output);
        assertTrue(output.contains("3×3"), output);
        assertTrue(output.contains("раз в 4"), output);
    }

    @Test
    void statsReportsMazeBreakdown() {
        Level level = Level.build(PARAMS);
        GameSession session = new GameSession(level);

        String output = run(session, "статистика\nвыход\n");

        assertTrue(output.contains("Лабиринт: проходов " + (level.grid().cells() - 1)), output);
        assertTrue(output.contains("тупиков "), output);
    }

    /** Админ-слова не считаются «не понял»: их разбирает адаптер. */
    @Test
    void adminWordsAreNotReportedAsUnknown() {
        GameSession session = new GameSession(Level.build(PARAMS));

        String output = run(session, "статистика\nвыход\n");

        assertFalse(output.contains("Не понял"), output);
    }

    @Test
    void helpListsAdminCommands() {
        GameSession session = new GameSession(Level.build(PARAMS));

        String output = run(session, "помощь\nвыход\n");

        assertTrue(output.contains(AdminParser.TELEPORT_NAME), output);
        assertTrue(output.contains(AdminParser.STATS_NAME), output);
        assertTrue(output.contains(AdminParser.NEW_LEVEL_NAME), output);
        for (String alias : AdminParser.TELEPORT_ALIASES) {
            assertTrue(output.contains(alias), "нет алиаса телепорта " + alias + " в " + output);
        }
    }

    private static String roomLine(Level level, int cell) {
        return ROOMS.describe(level.roomView(cell)).get(0);
    }

    /** Координаты клетки строкой вида {@code [1 2]}. */
    private static String coordinates(Level level, int cell) {
        return AdminText.coordinates(coordinateArray(level, cell));
    }

    /** Координаты клетки без скобок — для ввода в «телепорт». */
    private static String inlineCoordinates(Level level, int cell) {
        return ConsoleText.numbers(coordinateArray(level, cell));
    }

    private static int[] coordinateArray(Level level, int cell) {
        int[] coordinates = new int[level.grid().dimensions()];
        level.grid().coordinates(cell, coordinates);
        return coordinates;
    }

    private static String run(GameSession session, String input) {
        BufferedReader in = new BufferedReader(new StringReader(input));
        StringWriter output = new StringWriter();
        PrintWriter out = new PrintWriter(output, true);

        new ConsoleGame(in, out, session, () -> RANDOM_SEED).run();
        return output.toString();
    }

    private static int count(String haystack, String needle) {
        int occurrences = 0;
        int from = 0;
        while (true) {
            int at = haystack.indexOf(needle, from);
            if (at < 0) {
                return occurrences;
            }
            occurrences++;
            from = at + needle.length();
        }
    }
}
