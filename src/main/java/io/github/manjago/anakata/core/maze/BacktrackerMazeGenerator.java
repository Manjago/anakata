package io.github.manjago.anakata.core.maze;

import io.github.manjago.anakata.core.rng.SplitMix64;

/**
 * Генератор лабиринта обходом в глубину (recursive backtracker).
 *
 * <p>Строит идеальный лабиринт — дерево: между любыми двумя клетками ровно один
 * путь, и проходов ровно на один меньше числа клеток. Характер — длинные
 * извилистые коридоры.
 *
 * <p>Обход итеративный, с явным стеком: рекурсия на десятках тысяч клеток в 4D
 * сложила бы стек. Старт — клетка с индексом {@code 0}; выбор соседа равномерен
 * через {@link SplitMix64#nextLong(long)}.
 */
public final class BacktrackerMazeGenerator implements MazeGenerator {

    @Override
    public Maze generate(Grid grid, SplitMix64 rng) {
        Maze maze = new Maze(grid);
        Direction[] directions = grid.directions();
        int[] candidateCells = new int[directions.length];
        Direction[] candidateDirections = new Direction[directions.length];
        boolean[] visited = new boolean[grid.cells()];
        int[] stack = new int[grid.cells()];
        int top = 0;

        visited[0] = true;
        stack[top++] = 0;

        while (top > 0) {
            int cell = stack[top - 1];
            int count = 0;
            for (Direction direction : directions) {
                int neighbor = grid.neighbor(cell, direction);
                if (neighbor >= 0 && !visited[neighbor]) {
                    candidateCells[count] = neighbor;
                    candidateDirections[count] = direction;
                    count++;
                }
            }
            if (count == 0) {
                // Тупик: все соседи уже в дереве — возвращаемся назад.
                top--;
                continue;
            }
            int choice = (int) rng.nextLong(count);
            maze.carve(cell, candidateDirections[choice]);
            visited[candidateCells[choice]] = true;
            stack[top++] = candidateCells[choice];
        }
        return maze;
    }
}
