package io.github.manjago.anakata.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.junit.jupiter.api.Test;

class ConsoleGameTest {

    private static final LevelParams PARAMS = new LevelParams(42L, new int[] {3, 3}, 4);
    private static final RoomDescriber ROOMS = new RoomDescriber();

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

    private static String roomLine(Level level, int cell) {
        return ROOMS.describe(level.roomView(cell)).get(0);
    }

    private static String run(GameSession session, String input) {
        BufferedReader in = new BufferedReader(new StringReader(input));
        StringWriter output = new StringWriter();
        PrintWriter out = new PrintWriter(output, true);

        new ConsoleGame(in, out, session).run();
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
