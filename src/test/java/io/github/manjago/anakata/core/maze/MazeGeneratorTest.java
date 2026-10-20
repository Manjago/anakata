package io.github.manjago.anakata.core.maze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.manjago.anakata.core.rng.Seeders;
import io.github.manjago.anakata.core.rng.SplitMix64;
import org.junit.jupiter.api.Test;

class MazeGeneratorTest {

    private final MazeGenerator generator = new BacktrackerMazeGenerator();

    /**
     * Четыре свойства лабиринта из контракта дизайна разом и для N = 2, 3, 4:
     * все клетки достижимы из старта (BFS), проходов ровно на один меньше числа
     * клеток, тот же сид даёт тот же лабиринт, и всё это работает в каждой
     * размерности.
     */
    @Test
    void satisfiesMazeProperties() {
        Grid[] grids = {
                new Grid(2, 7),
                new Grid(6, 6),
                new Grid(4, 4, 4),
                new Grid(2, 2, 2, 2),
                new Grid(3, 3, 3, 3)};
        long[] seeds = {0L, 42L, 1234567L};
        for (Grid grid : grids) {
            for (long seed : seeds) {
                Maze maze = generator.generate(grid, Seeders.branch(seed, Seeders.Branch.MAZE));
                Maze repeat = generator.generate(grid, Seeders.branch(seed, Seeders.Branch.MAZE));
                assertConnected(grid, maze);
                assertSymmetricAndInside(grid, maze);
                assertEquals(grid.cells() - 1, maze.passageCount(),
                        "идеальный лабиринт должен быть деревом для решётки " + grid
                                + " и сида " + seed);
                assertSameMaze(grid, maze, repeat);
            }
        }
    }

    private static void assertConnected(Grid grid, Maze maze) {
        int[] distance = maze.distances(0);
        for (int cell = 0; cell < grid.cells(); cell++) {
            assertTrue(distance[cell] >= 0,
                    "клетка " + cell + " недостижима в решётке " + grid);
        }
    }

    /**
     * Один обход проверяет оба инварианта симметричной маски: у каждого прохода
     * сосед внутри решётки и отвечает обратным битом. Без второго инварианта
     * {@link Maze#passageCount()} при асимметрии тихо округлил бы вниз.
     */
    private static void assertSymmetricAndInside(Grid grid, Maze maze) {
        for (int cell = 0; cell < grid.cells(); cell++) {
            for (Direction direction : grid.directions()) {
                if (!maze.hasPassage(cell, direction)) {
                    continue;
                }
                int neighbor = grid.neighbor(cell, direction);
                assertNotEquals(-1, neighbor,
                        "проход из клетки " + cell + " ведёт за решётку по " + direction);
                assertTrue(maze.hasPassage(neighbor, direction.opposite()),
                        "проход из клетки " + cell + " по " + direction
                                + " односторонний в решётке " + grid);
            }
        }
    }

    /** Тот же сид должен давать тот же лабиринт — побитово те же маски. */
    private static void assertSameMaze(Grid grid, Maze first, Maze second) {
        for (int cell = 0; cell < grid.cells(); cell++) {
            assertEquals(first.mask(cell), second.mask(cell),
                    "тот же сид дал разный лабиринт: клетка " + cell + " решётки " + grid);
        }
    }

    @Test
    void differentSeedsYieldDifferentMazes() {
        Grid grid = new Grid(8, 8);
        Maze first = generator.generate(grid, Seeders.branch(1L, Seeders.Branch.MAZE));
        Maze second = generator.generate(grid, Seeders.branch(2L, Seeders.Branch.MAZE));
        boolean differ = false;
        for (int cell = 0; cell < grid.cells() && !differ; cell++) {
            differ = first.mask(cell) != second.mask(cell);
        }
        assertTrue(differ, "разные сиды дали одинаковый лабиринт");
    }

    /**
     * Растяжка, а не независимый эталон: фиксирует всё, что неявно входит в
     * контракт сида, — порядок направлений вместе с их осями и знаками,
     * потребление RNG (один {@code nextLong(count)} на шаг) и сам алгоритм.
     * Если тест падает, лабиринты по опубликованным сидам изменились — менять
     * нельзя молча.
     */
    @Test
    void seedContractIsStable() {
        assertMazeHash(new Grid(6, 6), 42L, 8574847908786647418L);
        assertMazeHash(new Grid(4, 4, 4), 42L, 6757658400346792183L);
        assertMazeHash(new Grid(3, 3, 3, 3), 42L, 1327685404948861894L);
    }

    private static void assertMazeHash(Grid grid, long seed, long expected) {
        Maze maze = new BacktrackerMazeGenerator()
                .generate(grid, Seeders.branch(seed, Seeders.Branch.MAZE));
        assertEquals(expected, mazeHash(maze), "контракт сида сдвинулся для решётки " + grid);
    }

    /** Свёртка масок генератором проекта: та же разболтка, что и в ветках сида. */
    private static long mazeHash(Maze maze) {
        long hash = 0;
        for (int cell = 0; cell < maze.grid().cells(); cell++) {
            hash = new SplitMix64(hash ^ maze.mask(cell)).nextLong();
        }
        return hash;
    }
}
