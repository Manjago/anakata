package io.github.manjago.anakata.core.rng;

/**
 * Генератор псевдослучайных чисел SplitMix64. Алгоритм предложили Гай Стил, Дуг Ли
 * и Кристин Флад в 2014 году; эталонная реализация Себастьяна Виньи использовалась
 * для сверки тестов.
 *
 * <p>Состояние генератора — одно 64-битное слово. Каждое следующее значение получается
 * применением разболтки к текущему состоянию, затем состояние продвигается золотым
 * сечением {@code 0x9E3779B97F4A7C15}. Простой один-long-алгоритм даёт достаточно
 * качественную последовательность для генерации лабиринта, ориентиров и букв.
 */
public final class SplitMix64 {

    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

    /** Текущее состояние генератора. */
    private long s;

    /**
     * Создаёт генератор из указанного начального состояния.
     */
    public SplitMix64(long seed) {
        s = seed;
    }

    /** Следующее 64-битное псевдослучайное значение. */
    public long nextLong() {
        return mix(s += GOLDEN_GAMMA);
    }

    /**
     * Разболтка 64-битного значения — стандартная битовая смесь SplitMix64.
     *
     * <p>Применяется и в {@link #nextLong()} для преобразования продвинутого состояния
     * в выходное значение, и в {@link Seeders} для свёртки имени сидератора.
     */
    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Следующее псевдослучайное значение из диапазона {@code [0, bound)}, {@code bound > 0}. */
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
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound должен быть положительным: " + bound);
        }
        return (int) nextLong(bound);
    }

    /** Следующее псевдослучайное логическое значение. */
    public boolean nextBoolean() {
        return (nextLong() < 0);
    }
}
