package io.github.manjago.anakata.core.maze;

import java.util.Arrays;

/**
 * Лабиринт на решётке: проходы между клетками.
 *
 * <p>Проходы хранятся в едином массиве масок, индексированном тем же линейным
 * индексом клетки: {@code masks[cell]} — множество открытых выходов этой клетки.
 * Проход между соседями записан дважды, битом с каждой стороны, поэтому
 * {@link #carve} всегда выставляет оба бита, а {@link #hasPassage} остаётся
 * локальным чтением. Симметрию держит только {@code carve}, и она проверяется
 * тестом.
 *
 * <p>Направлений не больше восьми ({@code 2N}, N = 2, 3, 4), поэтому маска
 * помещается в байт. Снаружи лабиринт доступен только на чтение; заполняет его
 * генератор через {@link #carve}.
 */
public final class Maze {

    private final Grid grid;
    private final byte[] masks;

    /**
     * Создаёт пустой лабиринт — без единого прохода. Массив масок заводится
     * здесь и только здесь: заполнять его снаружи нельзя, иначе сломались бы
     * симметрия и «за границей прохода нет», на которых держится {@link #hasPassage}.
     *
     * @param grid решётка
     */
    Maze(Grid grid) {
        this.grid = grid;
        this.masks = new byte[grid.cells()];
    }

    /** Решётка, на которой построен лабиринт. */
    public Grid grid() {
        return grid;
    }

    /**
     * Открыт ли проход из клетки {@code cell} в направлении {@code direction}.
     * За границей решётки прохода нет.
     */
    public boolean hasPassage(int cell, Direction direction) {
        grid.checkCell(cell);
        grid.checkDirection(direction);
        return (masks[cell] & (1 << direction.bit())) != 0;
    }

    /**
     * Открытые выходы клетки в порядке {@link Grid#directions()}. Игра работает
     * с этим списком, а не с маской: битовая раскладка остаётся внутри ядра.
     */
    public Direction[] exits(int cell) {
        grid.checkCell(cell);
        Direction[] directions = grid.directions();
        Direction[] exits = new Direction[directions.length];
        int count = 0;
        for (Direction direction : directions) {
            if ((masks[cell] & (1 << direction.bit())) != 0) {
                exits[count++] = direction;
            }
        }
        return Arrays.copyOf(exits, count);
    }

    /** Маска выходов клетки: бит направления выставлен, если проход открыт. */
    int mask(int cell) {
        grid.checkCell(cell);
        return masks[cell] & 0xFF;
    }

    /**
     * Число открытых проходов в лабиринте — рёбер, а не выставленных битов.
     * Каждый проход записан дважды, поэтому битов вдвое больше.
     */
    public int passageCount() {
        int bits = 0;
        for (byte mask : masks) {
            bits += Integer.bitCount(mask & 0xFF);
        }
        return bits / 2;
    }

    /**
     * Открывает проход между клеткой {@code cell} и её соседом в направлении
     * {@code direction}. Выставляет биты с обеих сторон, поэтому проход всегда
     * симметричен.
     *
     * @throws IllegalArgumentException если соседа нет — направление выводит за решётку
     */
    void carve(int cell, Direction direction) {
        int neighbor = grid.neighbor(cell, direction);
        if (neighbor < 0) {
            throw new IllegalArgumentException(
                    "нет соседа из клетки " + cell + " в направлении " + direction);
        }
        masks[cell] |= (byte) (1 << direction.bit());
        masks[neighbor] |= (byte) (1 << direction.opposite().bit());
    }

    /**
     * Расстояния по проходам от клетки {@code from} до всех клеток — обычный
     * обход в ширину. Недостижимым клеткам соответствует {@code -1}. Метод нужен
     * тестам на достижимость и, позже, расстановке букв подальше от старта.
     *
     * @param from клетка начала обхода
     * @return массив длиной {@code grid.cells()} с расстояниями в ходах
     */
    public int[] distances(int from) {
        grid.checkCell(from);
        int[] distance = new int[grid.cells()];
        Arrays.fill(distance, -1);
        int[] queue = new int[grid.cells()];
        Direction[] directions = grid.directions();
        int head = 0;
        int tail = 0;
        distance[from] = 0;
        queue[tail++] = from;
        while (head < tail) {
            int cell = queue[head++];
            for (Direction direction : directions) {
                if ((masks[cell] & (1 << direction.bit())) == 0) {
                    continue;
                }
                int neighbor = grid.neighbor(cell, direction);
                if (neighbor < 0 || distance[neighbor] != -1) {
                    continue;
                }
                distance[neighbor] = distance[cell] + 1;
                queue[tail++] = neighbor;
            }
        }
        return distance;
    }
}
