package io.github.manjago.anakata.core.landmark;

import java.util.Objects;

/**
 * Ориентир клетки — пара «вид + вариант».
 *
 * <p>Видов шесть, вариантов у каждого вида {@link LandmarkKind#VARIANTS}. Пара
 * собирается счётной схемой {@link Landmarks} из сида и координат клетки, а не
 * хранится: одна и та же клетка всегда даёт один и тот же ориентир.
 *
 * <p>Русская фраза для пары живёт в пакете описаний, а не здесь.
 */
public record Landmark(LandmarkKind kind, int variant) {

    /**
     * @throws NullPointerException     если вид не задан
     * @throws IllegalArgumentException если вариант вне {@code [0, VARIANTS)}
     */
    public Landmark {
        Objects.requireNonNull(kind, "вид");
        if (variant < 0 || variant >= LandmarkKind.VARIANTS) {
            throw new IllegalArgumentException("вариант вне диапазона: " + variant);
        }
    }
}
