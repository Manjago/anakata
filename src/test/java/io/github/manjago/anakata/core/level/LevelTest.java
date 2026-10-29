package io.github.manjago.anakata.core.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.manjago.anakata.core.landmark.Landmarks;
import io.github.manjago.anakata.core.maze.BacktrackerMazeGenerator;
import io.github.manjago.anakata.core.maze.Direction;
import io.github.manjago.anakata.core.maze.Maze;
import io.github.manjago.anakata.core.rng.Seeders;
import io.github.manjago.anakata.core.room.RoomView;
import java.util.List;
import org.junit.jupiter.api.Test;

class LevelTest {

    private static final LevelParams PARAMS = new LevelParams(42L, new int[] {5, 6}, 4);

    @Test
    void sameParamsYieldSameLevel() {
        Level first = Level.build(PARAMS);
        Level second = Level.build(PARAMS);
        for (int cell = 0; cell < first.grid().cells(); cell++) {
            assertEquals(first.roomView(cell), second.roomView(cell), "клетка " + cell);
        }
    }

    /**
     * {@code build} берёт лабиринт из ветки {@link Seeders.Branch#MAZE}, а
     * ориентиры — из соли {@link Seeders.Branch#LANDMARKS}: вид каждой комнаты
     * совпадает с независимо собранными лабиринтом и схемой. Если уровень
     * перепутает ветку, тест это поймает.
     */
    @Test
    void roomViewMatchesIndependentMazeAndLandmarks() {
        Level level = Level.build(PARAMS);
        Maze maze = new BacktrackerMazeGenerator()
                .generate(level.grid(), Seeders.branch(PARAMS.seed(), Seeders.Branch.MAZE));
        Landmarks landmarks = new Landmarks(level.grid(),
                Seeders.derive(PARAMS.seed(), Seeders.Branch.LANDMARKS), PARAMS.landmarkInEvery());

        boolean sawLandmark = false;
        for (int cell = 0; cell < level.grid().cells(); cell++) {
            RoomView view = level.roomView(cell);
            assertEquals(List.of(maze.exits(cell)), view.exits(), "выходы клетки " + cell);
            assertEquals(landmarks.at(cell), view.landmark(), "ориентир клетки " + cell);
            sawLandmark |= view.landmark() != null;
        }
        assertTrue(sawLandmark, "схема не дала ни одной примечательной клетки");
    }

    @Test
    void differentSeedsYieldDifferentLevels() {
        Level first = Level.build(new LevelParams(1L, new int[] {8, 8}, 4));
        Level second = Level.build(new LevelParams(2L, new int[] {8, 8}, 4));
        boolean differ = false;
        for (int cell = 0; cell < first.grid().cells() && !differ; cell++) {
            differ = !first.roomView(cell).equals(second.roomView(cell));
        }
        assertTrue(differ, "разные сиды дали одинаковый уровень");
    }

    @Test
    void startCellIsZeroAndHasExits() {
        Level level = Level.build(PARAMS);
        assertEquals(0, level.startCell());
        assertFalse(level.roomView(level.startCell()).exits().isEmpty());
    }

    @Test
    void gridReflectsParamsSizes() {
        Level level = Level.build(PARAMS);
        assertEquals(PARAMS.sizes().length, level.grid().dimensions());
        for (int axis = 0; axis < PARAMS.sizes().length; axis++) {
            assertEquals(PARAMS.sizes()[axis], level.grid().size(axis));
        }
    }

    @Test
    void roomViewRejectsCellOutsideGrid() {
        Level level = Level.build(PARAMS);
        assertThrows(IndexOutOfBoundsException.class, () -> level.roomView(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> level.roomView(level.grid().cells()));
    }

    @Test
    void buildRejectsNullParams() {
        assertThrows(NullPointerException.class, () -> Level.build(null));
    }

    @Test
    void stepMovesAlongPassageAndBack() {
        Level level = Level.build(PARAMS);
        int start = level.startCell();
        Direction exit = level.roomView(start).exits().get(0);
        int destination = level.grid().neighbor(start, exit);

        assertEquals(destination, level.step(start, exit));
        assertEquals(start, level.step(destination, exit.opposite()));
    }

    @Test
    void stepReportsNoPassageIntoWall() {
        Level level = Level.build(PARAMS);
        int start = level.startCell();
        Direction wall = LevelSupport.blockedDirection(level, start);
        assertNotNull(wall, "у клетки старта не нашлось стены");

        assertEquals(Level.NO_PASSAGE, level.step(start, wall));
    }

    /** Направление вне размерности — тот же «нет прохода», а не исключение. */
    @Test
    void stepReportsNoPassageOutsideDimension() {
        Level level = Level.build(PARAMS);
        assertEquals(Level.NO_PASSAGE, level.step(level.startCell(), Direction.UP));
    }

    @Test
    void stepRejectsCellOutsideGrid() {
        Level level = Level.build(PARAMS);
        assertThrows(IndexOutOfBoundsException.class,
                () -> level.step(-1, Direction.NORTH));
        assertThrows(IndexOutOfBoundsException.class,
                () -> level.step(level.grid().cells(), Direction.NORTH));
        // Направление вне размерности замыкает проверку прохода, поэтому клетку
        // здесь валидирует только явный checkCell.
        assertThrows(IndexOutOfBoundsException.class,
                () -> level.step(-1, Direction.UP));
        assertThrows(IndexOutOfBoundsException.class,
                () -> level.step(level.grid().cells(), Direction.UP));
    }

    @Test
    void stepRejectsNullDirection() {
        Level level = Level.build(PARAMS);
        assertThrows(NullPointerException.class, () -> level.step(level.startCell(), null));
    }

    /** {@code landmarkCount} согласован с тем, что показывает {@code roomView}. */
    @Test
    void landmarkCountMatchesRoomViews() {
        Level level = Level.build(new LevelParams(42L, new int[] {5, 5, 5}, 3));
        int expected = 0;
        for (int cell = 0; cell < level.grid().cells(); cell++) {
            if (level.roomView(cell).landmark() != null) {
                expected++;
            }
        }
        assertEquals(expected, level.landmarkCount());
    }

    /** Плотность 1 делает приметной каждую клетку — верхняя граница счёта. */
    @Test
    void densityOneMarksEveryCell() {
        Level level = Level.build(new LevelParams(1L, new int[] {3, 3}, 1));
        assertEquals(level.grid().cells(), level.landmarkCount());
    }

    /**
     * Правила держат {@code Grid} и {@code Landmarks}, поэтому сборка падает на
     * них: размерность вне 2–4, размер оси меньше 2 и плотность меньше 1.
     */
    @Test
    void buildRejectsInvalidParams() {
        assertThrows(IllegalArgumentException.class, () -> Level.build(new LevelParams(1L, new int[] {6}, 4)));
        assertThrows(IllegalArgumentException.class, () -> Level.build(new LevelParams(1L, new int[] {6, 6, 6, 6, 6}, 4)));
        assertThrows(IllegalArgumentException.class, () -> Level.build(new LevelParams(1L, new int[] {6, 1}, 4)));
        assertThrows(IllegalArgumentException.class, () -> Level.build(new LevelParams(1L, new int[] {6, 6}, 0)));
    }
}
