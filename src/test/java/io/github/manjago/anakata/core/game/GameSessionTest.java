package io.github.manjago.anakata.core.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.manjago.anakata.core.level.Level;
import io.github.manjago.anakata.core.level.LevelParams;
import io.github.manjago.anakata.core.level.LevelSupport;
import io.github.manjago.anakata.core.maze.Direction;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameSessionTest {

    private static final LevelParams PARAMS = new LevelParams(42L, new int[] {5, 6}, 4);

    @Test
    void startsAtLevelStartCell() {
        Level level = Level.build(PARAMS);
        GameSession session = new GameSession(level);

        assertEquals(level.startCell(), session.cell());
        assertEquals(level.roomView(level.startCell()), session.roomView());
    }

    @Test
    void moveAlongOpenExitChangesCell() {
        Level level = Level.build(PARAMS);
        GameSession session = new GameSession(level);
        Direction exit = session.roomView().exits().get(0);
        int destination = level.grid().neighbor(session.cell(), exit);

        Reply reply = session.execute(new Command.Move(exit));

        assertEquals(Outcome.MOVED, reply.outcome());
        assertEquals(destination, session.cell());
        assertEquals(level.roomView(destination), reply.room());
    }

    @Test
    void moveIntoWallStaysPut() {
        Level level = Level.build(PARAMS);
        GameSession session = new GameSession(level);
        int start = session.cell();
        Direction wall = LevelSupport.blockedDirection(level, start);
        assertNotNull(wall, "у клетки старта не нашлось стены");

        Reply reply = session.execute(new Command.Move(wall));

        assertEquals(Outcome.BLOCKED, reply.outcome());
        assertEquals(start, session.cell());
        assertEquals(level.roomView(start), reply.room());
    }

    /** В 2D-уровне «вверх» — направление вне размерности: тот же исход, без исключения. */
    @Test
    void directionOutsideDimensionIsBlocked() {
        Level level = Level.build(PARAMS);
        GameSession session = new GameSession(level);
        int start = session.cell();

        Reply reply = session.execute(new Command.Move(Direction.UP));

        assertEquals(Outcome.BLOCKED, reply.outcome());
        assertEquals(start, session.cell());
    }

    /** Проход симметричен, поэтому шаг в выход и обратно возвращает на старт. */
    @Test
    void moveAndBackReturnsToStart() {
        Level level = Level.build(PARAMS);
        GameSession session = new GameSession(level);
        int start = session.cell();
        Direction exit = session.roomView().exits().get(0);

        session.execute(new Command.Move(exit));
        Reply back = session.execute(new Command.Move(exit.opposite()));

        assertEquals(Outcome.MOVED, back.outcome());
        assertEquals(start, session.cell());
    }

    /** Команды действительно водят по лабиринту: путь BFS приводит в каждую клетку. */
    @Test
    void commandsNavigateToEveryReachableCell() {
        Level level = Level.build(PARAMS);
        int[] parentDirection = new int[level.grid().cells()];
        Arrays.fill(parentDirection, -1);
        boolean[] seen = new boolean[level.grid().cells()];
        Deque<Integer> queue = new ArrayDeque<>();
        seen[level.startCell()] = true;
        queue.add(level.startCell());
        while (!queue.isEmpty()) {
            int cell = queue.poll();
            for (Direction direction : level.roomView(cell).exits()) {
                int neighbor = level.grid().neighbor(cell, direction);
                if (!seen[neighbor]) {
                    seen[neighbor] = true;
                    parentDirection[neighbor] = direction.ordinal();
                    queue.add(neighbor);
                }
            }
        }

        for (int target = 0; target < level.grid().cells(); target++) {
            assertTrue(seen[target], "клетка " + target + " недостижима");
            GameSession session = new GameSession(level);
            for (Direction direction : pathTo(level, parentDirection, target)) {
                Reply reply = session.execute(new Command.Move(direction));
                assertEquals(Outcome.MOVED, reply.outcome(), "шаг к клетке " + target);
            }
            assertEquals(target, session.cell(), "путь к клетке " + target);
        }
    }

    /** Одинаковые параметры и команды дают одинаковые ответы и клетку — на этом держится снапшот. */
    @Test
    void sameCommandsYieldSameCell() {
        GameSession first = new GameSession(Level.build(PARAMS));
        GameSession second = new GameSession(Level.build(PARAMS));
        List<Direction> script = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH,
                Direction.WEST, Direction.NORTH, Direction.EAST);

        for (Direction direction : script) {
            assertEquals(first.execute(new Command.Move(direction)),
                    second.execute(new Command.Move(direction)));
        }
        assertEquals(first.cell(), second.cell());
    }

    @Test
    void availableActionsAreRoomExits() {
        Level level = Level.build(PARAMS);
        GameSession session = new GameSession(level);
        Direction exit = session.roomView().exits().get(0);

        Reply reply = session.execute(new Command.Move(exit));

        List<Command> expected = new ArrayList<>();
        for (Direction direction : reply.room().exits()) {
            expected.add(new Command.Move(direction));
        }
        assertEquals(expected, reply.availableActions());
    }

    @Test
    void sessionRejectsNullLevel() {
        assertThrows(NullPointerException.class, () -> new GameSession(null));
    }

    @Test
    void executeRejectsNullCommand() {
        GameSession session = new GameSession(Level.build(PARAMS));
        assertThrows(NullPointerException.class, () -> session.execute(null));
    }

    /** Путь от старта до {@code target} по дереву обхода. */
    private static List<Direction> pathTo(Level level, int[] parentDirection, int target) {
        Deque<Direction> path = new ArrayDeque<>();
        int cell = target;
        while (cell != level.startCell()) {
            Direction step = Direction.values()[parentDirection[cell]];
            path.addFirst(step);
            cell = level.grid().neighbor(cell, step.opposite());
        }
        return List.copyOf(path);
    }
}
