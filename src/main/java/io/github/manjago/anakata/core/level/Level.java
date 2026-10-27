package io.github.manjago.anakata.core.level;

import io.github.manjago.anakata.core.landmark.Landmarks;
import io.github.manjago.anakata.core.maze.BacktrackerMazeGenerator;
import io.github.manjago.anakata.core.maze.Direction;
import io.github.manjago.anakata.core.maze.Grid;
import io.github.manjago.anakata.core.maze.Maze;
import io.github.manjago.anakata.core.room.RoomView;
import io.github.manjago.anakata.core.rng.Seeders;
import java.util.List;
import java.util.Objects;

/**
 * Собранный уровень: решётка, лабиринт и схема ориентиров.
 *
 * <p>Уровень собирается в одном месте — {@link #build(LevelParams)}: из
 * параметров строятся {@link Grid}, {@link Maze} (ветка
 * {@link Seeders.Branch#MAZE}) и {@link Landmarks} (соль
 * {@link Seeders#derive(long, Seeders.Branch)} от ветки
 * {@link Seeders.Branch#LANDMARKS}) — все три из одной решётки. Лабиринт и
 * ориентиры друг от друга не зависят, но развести их по коду нельзя: они
 * приватны и живут только здесь. Владение заменяет отдельную проверку
 * соответствия {@code Grid} — подставить чужую схему случайно не выйдет.
 *
 * <p>Алгоритм генерации пока один — {@link BacktrackerMazeGenerator}, он зашит в
 * {@link #build(LevelParams)}. Алгоритм определяет лабиринт наравне с сидом,
 * поэтому, когда появится второй, его идентификатор станет полем
 * {@link LevelParams} и войдёт в снапшот (см. DESIGN.md, «Генерация лабиринта»).
 */
public final class Level {

    /** Старт игрока — клетка с индексом 0; игрок его не выбирает. */
    private static final int START_CELL = 0;

    /**
     * Нет прохода: стена, край решётки или направление вне размерности уровня.
     *
     * <p>Отдельная константа, а не голый {@code -1}: у {@link Grid#neighbor}
     * свой {@code -1} — «нет соседа», — и совпадение значений не должно выглядеть
     * как одно понятие. Здесь же смысл шире: прохода нет по любой причине.
     */
    public static final int NO_PASSAGE = -1;

    private final LevelParams params;
    private final Grid grid;
    private final Maze maze;
    private final Landmarks landmarks;

    private Level(LevelParams params, Grid grid, Maze maze, Landmarks landmarks) {
        this.params = params;
        this.grid = grid;
        this.maze = maze;
        this.landmarks = landmarks;
    }

    /**
     * Собирает уровень из параметров.
     *
     * @param params сид, размеры и плотность ориентиров
     * @return собранный уровень
     * @throws NullPointerException     если {@code params} не заданы
     * @throws IllegalArgumentException если размерность или размеры оси вне
     *                                  допустимых, а также плотность меньше 1 —
     *                                  правила держат {@link Grid} и {@link Landmarks}
     */
    public static Level build(LevelParams params) {
        Objects.requireNonNull(params, "параметры уровня");
        Grid grid = new Grid(params.sizes());
        Maze maze = new BacktrackerMazeGenerator()
                .generate(grid, Seeders.branch(params.seed(), Seeders.Branch.MAZE));
        Landmarks landmarks = new Landmarks(grid,
                Seeders.derive(params.seed(), Seeders.Branch.LANDMARKS),
                params.landmarkInEvery());
        return new Level(params, grid, maze, landmarks);
    }

    /** Параметры, из которых собран уровень. */
    public LevelParams params() {
        return params;
    }

    /** Решётка уровня. */
    public Grid grid() {
        return grid;
    }

    /** Клетка старта игрока. */
    public int startCell() {
        return START_CELL;
    }

    /**
     * Вид комнаты в клетке — глазами игрока: выходы лабиринта и, иногда, ориентир.
     *
     * <p>Вид собирается только из собственных лабиринта и схемы ориентиров уровня,
     * поэтому чужую схему сюда не подставить.
     *
     * @param cell линейный индекс клетки
     * @return вид комнаты
     * @throws IndexOutOfBoundsException если клетка вне решётки
     */
    public RoomView roomView(int cell) {
        List<Direction> exits = List.of(maze.exits(cell));
        return new RoomView(exits, landmarks.at(cell));
    }

    /**
     * Шаг из клетки в направлении: индекс соседа или {@link #NO_PASSAGE}.
     *
     * <p>Проход проверяет сам уровень — лабиринт приватен, и наружу торчит ровно
     * этот вопрос. {@link #NO_PASSAGE} возвращается и когда прохода нет (стена,
     * край решётки), и когда направления нет в размерности уровня, — например,
     * «вверх» в 2D. Для игрока это одно и то же «туда не пройти», поэтому исход
     * один и исключения нет: разбор слова знает лишь само слово, а размерность —
     * уровень.
     *
     * @param cell      линейный индекс клетки
     * @param direction направление хода
     * @return индекс соседней клетки или {@link #NO_PASSAGE}
     * @throws IndexOutOfBoundsException если клетка вне решётки
     * @throws NullPointerException      если направление не задано
     */
    public int step(int cell, Direction direction) {
        Objects.requireNonNull(direction, "направление");
        grid.checkCell(cell);
        if (direction.axis() >= grid.dimensions() || !maze.hasPassage(cell, direction)) {
            return NO_PASSAGE;
        }
        return grid.neighbor(cell, direction);
    }
}
