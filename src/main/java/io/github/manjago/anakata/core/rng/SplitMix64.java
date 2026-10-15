package io.github.manjago.anakata.core.rng;

/**
 * Генератор псевдослучайных чисел SplitMix64 (вариант Себастьяна Виньи).
 *
 * <p>Состояние генератора — одно 64-битное слово. Каждое следующее значение получается
 * применением разболтки к текущему состоянию, затем состояние продвигается золотым
 * сечением {@code 0x9E3779B97F4A7C15}. Простой один-long-алгоритм даёт достаточно
 * качественную последовательность для генерации лабиринта, ориентиров и букв.
 */
public final class SplitMix64 implements Rng {

    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

    /** Текущее состояние генератора. */
    private long s;

    /**
     * Создаёт генератор из указанного начального состояния.
     */
    public SplitMix64(long seed) {
        s = seed;
    }

    /** Создаёт генератор, восстановленный из снимка состояния. */
    public SplitMix64(SplitMix64State state) {
        s = state.s();
    }

    /** Снимок текущего состояния генератора. */
    @Override
    public SplitMix64State state() {
        return new SplitMix64State(s);
    }

    /** Следующее 64-битное псевдослучайное значение. */
    @Override
    public long nextLong() {
        long z = s += GOLDEN_GAMMA;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Следующее псевдослучайное значение из диапазона {@code [0, bound)}, {@code bound > 0}. */
    @Override
    public long nextLong(long bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound должен быть положительным: " + bound);
        }
        if (bound == 1) {
            return 0;
        }
        long r;
        long u;
        long m = bound - 1;
        // Безбиасовый выбор значения из [0, bound): берём равномерное число из
        // [0, 2^63) и отсекаем «хвост», где остаток был бы смещён.
        do {
            u = nextLong() >>> 1;
            r = u % bound;
        } while (u - r + m < 0);
        return r;
    }

    /** Следующее псевдослучайное значение из диапазона {@code [0, bound)}, {@code bound > 0}. */
    @Override
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound должен быть положительным: " + bound);
        }
        return (int) nextLong(bound);
    }

    /** Следующее псевдослучайное логическое значение. */
    @Override
    public boolean nextBoolean() {
        return (nextLong() & 1) != 0;
    }
}
