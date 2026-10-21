package io.github.manjago.anakata.core.landmark;

import io.github.manjago.anakata.core.maze.Grid;
import io.github.manjago.anakata.core.rng.Seeders;
import io.github.manjago.anakata.core.rng.SplitMix64;
import java.util.Objects;

/**
 * Счётная схема ориентиров: примечательность клетки из сида и координат.
 *
 * <p>Ориентиры не хранятся и не генерируются потоком — идентичность выводится
 * формулой, поэтому схему можно спрашивать в любом порядке и она не зависит от
 * лабиринта. Одна и та же клетка всегда даёт один и тот же ориентир: не более
 * одного на клетку. Соль — готовый {@code long}-подсид уровня, обычно
 * {@code Seeders.derive(seed, Branch.LANDMARKS)}; генератор здесь не нужен,
 * поэтому схема принимает не {@link SplitMix64}, а готовое число.
 *
 * <p><b>Схема — часть контракта сида.</b> Хеш растёт из соли и координат клетки:
 * число раундов свёртки равно числу осей, поэтому размерность в схему входит, а
 * размеры решётки — нет. Клетка с одними координатами даёт один ориентир и в
 * 6×6, и в 10×10 с тем же сидом: схема знает про положение клетки, а не про
 * границы уровня. Порядок констант {@link LandmarkKind}, число видов и
 * {@link LandmarkKind#VARIANTS} тоже входят в схему, а формула зафиксирована
 * эталонным тестом: менять её молча нельзя — ориентиры по всем опубликованным
 * сидам станут другими.
 */
public final class Landmarks {

    /** Виды по индексу — порядок тот же, что у {@code values()}. */
    private static final LandmarkKind[] KINDS = LandmarkKind.values();

    private final Grid grid;
    private final long salt;
    private final long threshold;

    /**
     * Создаёт схему уровня.
     *
     * @param grid    решётка уровня
     * @param salt    соль — готовый {@code long}-подсид уровня
     * @param inEvery средняя плотность: примерно одна примечательная клетка на
     *                {@code inEvery}, не меньше 1
     */
    public Landmarks(Grid grid, long salt, int inEvery) {
        if (inEvery < 1) {
            throw new IllegalArgumentException("плотность должна быть не меньше 1: " + inEvery);
        }
        this.grid = Objects.requireNonNull(grid, "решётка");
        this.salt = salt;
        this.threshold = Long.divideUnsigned(-1L, inEvery);
    }

    /**
     * Ориентир клетки — или {@code null}, если клетка непримечательна.
     *
     * <p>{@code null} — штатный результат, а не ошибка: большинство клеток
     * непримечательны.
     *
     * <p>Схема: соль сворачивается с координатами по одной через
     * {@link SplitMix64#mix}, затем присутствие решает беззнаковое сравнение
     * хеша с порогом {@code (2^64 - 1) / inEvery} (деление беззнаковое, с
     * округлением вниз) — клетка примечательна, если хеш не больше порога. Вид и
     * вариант берутся из беззнаковых остатков двух следующих разболток
     * ({@link Long#remainderUnsigned}, чтобы знак хеша не сбивал выбор) и от
     * плотности не зависят.
     *
     * @param cell линейный индекс клетки
     * @return ориентир или {@code null}
     */
    public Landmark at(int cell) {
        long h = salt;
        for (int axis = 0; axis < grid.dimensions(); axis++) {
            h = SplitMix64.mix(h ^ grid.coordinate(cell, axis));
        }
        if (Long.compareUnsigned(h, threshold) > 0) {
            return null;
        }
        long kindHash = SplitMix64.mix(h);
        long variantHash = SplitMix64.mix(kindHash);
        LandmarkKind kind = KINDS[(int) Long.remainderUnsigned(kindHash, KINDS.length)];
        int variant = (int) Long.remainderUnsigned(variantHash, LandmarkKind.VARIANTS);
        return new Landmark(kind, variant);
    }
}
