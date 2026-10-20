package io.github.manjago.anakata.core.maze;

import io.github.manjago.anakata.core.rng.SplitMix64;

/**
 * Генератор лабиринта: строит проходы на готовой решётке.
 *
 * <p>Алгоритмов мы хотим уметь несколько, и они дают разный характер лабиринта,
 * поэтому генерация спрятана за этим интерфейсом. Генератор детерминирован: один
 * и тот же сид и параметры дают один и тот же лабиринт.
 */
public interface MazeGenerator {

    /**
     * Строит лабиринт на решётке.
     *
     * @param grid решётка
     * @param rng  генератор, уже отведённый под лабиринт веткой сида
     *             ({@code Seeders.branch(seed, MAZE)})
     * @return построенный лабиринт
     */
    Maze generate(Grid grid, SplitMix64 rng);
}
