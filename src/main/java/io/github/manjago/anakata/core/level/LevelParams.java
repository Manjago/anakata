package io.github.manjago.anakata.core.level;

import java.util.Arrays;
import java.util.Objects;

/**
 * Параметры уровня — ровно то, что хранится в снапшоте: сид, размеры по осям и
 * плотность ориентиров. Из них строится {@link Level}, а сам лабиринт не хранится
 * и пересобирается из этих полей.
 *
 * <p>Размерность — это длина {@code sizes}: отдельного поля нет. Правила
 * (размерность 2–4, размер оси не меньше 2, плотность не меньше 1) здесь не
 * повторяются — их держат {@link io.github.manjago.anakata.core.maze.Grid} и
 * {@link io.github.manjago.anakata.core.landmark.Landmarks}, поэтому невалидные
 * параметры падают на {@link Level#build}.
 *
 * <p>Размеры хранятся массивом {@code int[]}, а не списком: так их видят
 * {@code Grid} и контракт дизайна. Массив — единственный изменяемый компонент,
 * поэтому он копируется и на входе, и на чтении, иначе правка чужого массива
 * меняла бы уже собранные параметры. Record сравнивает массив по ссылке, а из-за
 * копии ссылки у равных параметров никогда не совпадут, поэтому {@code equals} и
 * {@code hashCode} переопределены по содержимому.
 *
 * @param seed           базовый сид уровня
 * @param sizes          размеры по осям, каждой оси не меньше 2; осей от 2 до 4
 * @param landmarkInEvery средняя плотность ориентиров: примерно одна
 *                        примечательная клетка на {@code landmarkInEvery}
 */
public record LevelParams(long seed, int[] sizes, int landmarkInEvery) {

    /** @throws NullPointerException если {@code sizes} не задан */
    public LevelParams {
        Objects.requireNonNull(sizes, "размеры");
        sizes = sizes.clone();
    }

    /** Копия размеров: компонент изменяемый, наружу отдаём его копию. */
    @Override
    public int[] sizes() {
        return sizes.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LevelParams params)) {
            return false;
        }
        return seed == params.seed
                && landmarkInEvery == params.landmarkInEvery
                && Arrays.equals(sizes, params.sizes);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * Long.hashCode(seed) + Arrays.hashCode(sizes)) + landmarkInEvery;
    }

    @Override
    public String toString() {
        return "LevelParams[seed=" + seed + ", sizes=" + Arrays.toString(sizes)
                + ", landmarkInEvery=" + landmarkInEvery + ']';
    }
}
