package io.github.manjago.anakata.core.rng;

/**
 * Неизменяемый снимок состояния генератора {@link SplitMix64}.
 *
 * @param s текущее состояние генератора
 */
public record SplitMix64State(long s) implements RngState {
}
