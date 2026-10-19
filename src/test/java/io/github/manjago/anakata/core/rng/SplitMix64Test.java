package io.github.manjago.anakata.core.rng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class SplitMix64Test {

    /**
     * Эталонные значения nextLong() для seed 0 сверены с референсной реализацией
     * SplitMix64 (Sebastiano Vigna, https://prng.di.unimi.it/splitmix64.c). Сам
     * алгоритм предложили Гай Стил, Дуг Ли и Кристин Флад в 2014 году.
     *
     * <p>Формула из первоисточника идентична реализации {@link SplitMix64}. Значения
     * получены независимым прогоном C-версии из источника и в беззнаковом виде равны
     * 0xe220a8397b1dcdaf, 0x6e789e6aa1b965f4, 0x06c45d188009454f, 0xf88bb8a8724c81ec.
     */
    @Test
    void seedZeroMatchesReferenceImplementation() {
        SplitMix64 rng = new SplitMix64(0L);
        assertEquals(-2152535657050944081L, rng.nextLong());
        assertEquals(7960286522194355700L, rng.nextLong());
        assertEquals(487617019471545679L, rng.nextLong());
        assertEquals(-537132696929009172L, rng.nextLong());
    }

    /**
     * Эталонные значения nextLong() для сида 1234567 сверены с официальной референсной
     * реализацией SplitMix64 (Sebastiano Vigna, https://prng.di.unimi.it/splitmix64.c):
     * C-версия из источника скомпилирована и запущена, результаты совпали. В беззнаковом
     * виде значения равны 0x599ed017fb08fc85, 0x2c73f08458540fa5, 0x883ebce5a3f27c77,
     * 0x3fbef740e9177b3f.
     */
    @Test
    void seedNonZeroMatchesReferenceImplementation() {
        SplitMix64 rng = new SplitMix64(1234567L);
        assertEquals(6457827717110365317L, rng.nextLong());
        assertEquals(3203168211198807973L, rng.nextLong());
        assertEquals(-8629252141511181193L, rng.nextLong());
        assertEquals(4593380528125082431L, rng.nextLong());
    }

    @Test
    void sameSeedYieldsSameSequence() {
        long seed = 0xCAFEBABE12345678L;
        SplitMix64 first = new SplitMix64(seed);
        SplitMix64 second = new SplitMix64(seed);
        for (int i = 0; i < 1000; i++) {
            assertEquals(first.nextLong(), second.nextLong(), "расхождение на шаге " + i);
        }
    }

    @Test
    void differentSeedsYieldsDifferentSequence() {
        SplitMix64 first = new SplitMix64(1L);
        SplitMix64 second = new SplitMix64(2L);
        boolean differ = false;
        for (int i = 0; i < 1000 && !differ; i++) {
            differ = first.nextLong() != second.nextLong();
        }
        assertTrue(differ, "последовательности для разных сидов совпали");
    }

    @Test
    void nextLongBoundedStaysInRange() {
        SplitMix64 rng = new SplitMix64(123L);
        long[] bounds = {1, 2, 7, 10, 100, 1000, Long.MAX_VALUE};
        for (long bound : bounds) {
            for (int i = 0; i < 10_000; i++) {
                long v = rng.nextLong(bound);
                assertTrue(v >= 0 && v < bound, "значение вне [0, " + bound + "): " + v);
            }
        }
    }

    @Test
    void boundedProducesAllValues() {
        SplitMix64 rng = new SplitMix64(0xDEADBEEFL);
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            seen.add(rng.nextLong(5));
        }
        assertEquals(Set.of(0L, 1L, 2L, 3L, 4L), seen);
    }

    @Test
    void rejectsNonPositiveBound() {
        SplitMix64 rng = new SplitMix64(1L);
        assertThrows(IllegalArgumentException.class, () -> rng.nextLong(0));
        assertThrows(IllegalArgumentException.class, () -> rng.nextLong(-1));
    }

    @Test
    void nextLongBoundOneIsAlwaysZero() {
        SplitMix64 rng = new SplitMix64(1L);
        for (int i = 0; i < 100; i++) {
            assertEquals(0, rng.nextLong(1));
        }
    }

    /**
     * {@code bound == 1} не короткое замыкание: диапазонный вызов тратит один
     * выход генератора, как в JDK, поэтому следующий {@link SplitMix64#nextLong()}
     * возвращает уже второй выход потока.
     */
    @Test
    void boundOneConsumesOneDraw() {
        SplitMix64 rng = new SplitMix64(0L);
        assertEquals(0L, rng.nextLong(1));
        assertEquals(7960286522194355700L, rng.nextLong());
    }

    /**
     * Диапазонные значения сверены с JDK-референсом того же алгоритма —
     * {@code SplittableRandom} ({@code RandomSupport.boundedNextLong}). Значения
     * сняты прогоном JDK, а не нашим кодом, и зафиксированы здесь как контракт,
     * чтобы тест не зависел от версии рантайма. Покрыты {@code bound == 1},
     * степени двойки, {@code (1L << 62) + 1} и {@link Long#MAX_VALUE}.
     */
    @Test
    void boundedValuesMatchReferenceImplementation() {
        assertBounded(0L, 1L, 0L, 0L, 0L, 0L);
        assertBounded(0L, 2L, 1L, 0L, 1L, 0L);
        assertBounded(0L, 3L, 0L, 0L, 0L, 2L);
        assertBounded(0L, 4L, 3L, 0L, 3L, 0L);
        assertBounded(0L, 7L, 4L, 4L, 4L, 2L);
        assertBounded(0L, 10L, 7L, 0L, 9L, 2L);
        assertBounded(0L, 1000L, 767L, 850L, 839L, 222L);
        assertBounded(0L, 1024L, 431L, 500L, 335L, 492L);
        assertBounded(0L, (1L << 62) + 1,
                3980143261097177850L, 243808509735772839L,
                980875101213047373L, 3019047300631581045L);
        assertBounded(0L, Long.MAX_VALUE,
                8147104208329303767L, 3980143261097177850L,
                243808509735772839L, 8954805688390271222L);
        assertBounded(1234567L, 2L, 1L, 1L, 1L, 1L);
        assertBounded(1234567L, 3L, 1L, 0L, 1L, 0L);
        assertBounded(1234567L, 4L, 1L, 1L, 3L, 3L);
        assertBounded(1234567L, 7L, 0L, 4L, 1L, 1L);
        assertBounded(1234567L, 1000L, 658L, 986L, 211L, 215L);
        assertBounded(1234567L, 1024L, 133L, 933L, 119L, 831L);
        assertBounded(-1L, 1L, 0L, 0L, 0L, 0L);
        assertBounded(-1L, 2L, 0L, 1L, 1L, 0L);
        assertBounded(-1L, 3L, 1L, 1L, 0L, 0L);
        assertBounded(-1L, 4L, 0L, 1L, 1L, 2L);
        assertBounded(-1L, 7L, 0L, 0L, 3L, 3L);
        assertBounded(-1L, 10L, 8L, 4L, 0L, 1L);
        assertBounded(-1L, 1000L, 968L, 484L, 500L, 921L);
        assertBounded(-1L, 1024L, 32L, 713L, 489L, 722L);
        assertBounded(-1L, (1L << 62) + 1,
                2024363799162208500L, 3931318902156738921L,
                2319021877215838258L, 112353042671515406L);
        assertBounded(-1L, Long.MAX_VALUE,
                8245168133484221968L, 8417223528544944484L,
                2024363799162208500L, 3931318902156738921L);
    }

    private static void assertBounded(long seed, long bound, long... expected) {
        SplitMix64 rng = new SplitMix64(seed);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], rng.nextLong(bound),
                    "сид " + seed + ", bound " + bound + ", шаг " + i);
        }
    }

    /**
     * Дифференциальная проверка против {@link SplittableRandom}: значения в
     * диапазоне совпадают на каждом шаге, и после серии состояние генераторов
     * синхронно (обе стороны потребили одинаковое число выходов).
     *
     * <p>Если падает только этот тест, а зашитый
     * {@code boundedValuesMatchReferenceImplementation} проходит — изменился JDK.
     * {@code nextLong(bound)} в таком случае не чинить: пересмотр семантики
     * ломает уже опубликованные сиды.
     */
    @Test
    void boundedValuesMatchSplittableRandom() {
        long[] seeds = {0L, 1234567L, -1L, 0xCAFEBABE12345678L};
        long[] bounds = {1L, 2L, 3L, 4L, 7L, 10L, 1000L, 1024L, (1L << 62) + 1, Long.MAX_VALUE};
        for (long seed : seeds) {
            for (long bound : bounds) {
                SplitMix64 ours = new SplitMix64(seed);
                SplittableRandom reference = new SplittableRandom(seed);
                for (int i = 0; i < 1000; i++) {
                    assertEquals(reference.nextLong(bound), ours.nextLong(bound),
                            "разошлось с JDK: сид " + seed + ", bound " + bound + ", шаг " + i
                                    + " — если boundedValuesMatchReferenceImplementation проходит,"
                                    + " изменился JDK, nextLong(bound) не чинить");
                }
                assertEquals(reference.nextLong(), ours.nextLong(),
                        "состояние разошлось с JDK после сид " + seed + ", bound " + bound
                                + " — если boundedValuesMatchReferenceImplementation проходит,"
                                + " изменился JDK, nextLong(bound) не чинить");
            }
        }
    }
}

