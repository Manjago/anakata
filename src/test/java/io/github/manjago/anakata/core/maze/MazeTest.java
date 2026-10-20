package io.github.manjago.anakata.core.maze;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MazeTest {

    @Test
    void carveOpensPassageOnBothSides() {
        Grid grid = new Grid(3, 3);
        Maze maze = new Maze(grid);
        int cell = grid.index(1, 1);
        int north = grid.neighbor(cell, Direction.NORTH);

        maze.carve(cell, Direction.NORTH);

        assertTrue(maze.hasPassage(cell, Direction.NORTH));
        assertTrue(maze.hasPassage(north, Direction.SOUTH));
        assertEquals(1, maze.passageCount());
    }

    @Test
    void passageOutsideGridIsAbsent() {
        Grid grid = new Grid(3, 3);
        Maze maze = new Maze(grid);
        int origin = grid.index(0, 0);
        int far = grid.index(2, 2);

        assertFalse(maze.hasPassage(origin, Direction.SOUTH));
        assertFalse(maze.hasPassage(origin, Direction.WEST));
        assertFalse(maze.hasPassage(far, Direction.NORTH));
        assertFalse(maze.hasPassage(far, Direction.EAST));
    }

    @Test
    void carveWithoutNeighborThrows() {
        Grid grid = new Grid(3, 3);
        Maze maze = new Maze(grid);
        int origin = grid.index(0, 0);
        assertThrows(IllegalArgumentException.class, () -> maze.carve(origin, Direction.SOUTH));
    }

    @Test
    void directionOutsideDimensionThrows() {
        Grid grid = new Grid(3, 3);
        Maze maze = new Maze(grid);
        assertThrows(IllegalArgumentException.class, () -> maze.hasPassage(0, Direction.UP));
    }

    @Test
    void maskListsOpenExits() {
        Grid grid = new Grid(3, 3);
        Maze maze = new Maze(grid);
        int cell = grid.index(1, 1);

        maze.carve(cell, Direction.NORTH);
        maze.carve(cell, Direction.EAST);

        int expected = (1 << Direction.NORTH.bit()) | (1 << Direction.EAST.bit());
        assertEquals(expected, maze.mask(cell));
        assertEquals(2, maze.passageCount());
    }

    @Test
    void exitsListOpenDirectionsInFixedOrder() {
        Grid grid = new Grid(3, 3, 3);
        Maze maze = new Maze(grid);
        int cell = grid.index(1, 1, 1);

        maze.carve(cell, Direction.NORTH);
        maze.carve(cell, Direction.EAST);
        maze.carve(cell, Direction.UP);

        assertArrayEquals(new Direction[]{Direction.NORTH, Direction.EAST, Direction.UP},
                maze.exits(cell));
    }

    @Test
    void distancesFollowPassagesAndMarkUnreachable() {
        // Прямая из трёх клеток вдоль первой оси; остальные недостижимы.
        Grid grid = new Grid(3, 3);
        Maze maze = new Maze(grid);
        maze.carve(grid.index(0, 0), Direction.NORTH);
        maze.carve(grid.index(1, 0), Direction.NORTH);

        int[] distance = maze.distances(grid.index(0, 0));

        assertEquals(0, distance[grid.index(0, 0)]);
        assertEquals(1, distance[grid.index(1, 0)]);
        assertEquals(2, distance[grid.index(2, 0)]);
        assertEquals(-1, distance[grid.index(0, 1)]);
    }
}
