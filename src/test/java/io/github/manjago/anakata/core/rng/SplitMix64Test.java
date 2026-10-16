package io.github.manjago.anakata.core.rng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
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
    void restoredStateContinuesSequence() {
        long seed = 42L;
        SplitMix64 original = new SplitMix64(seed);
        for (int i = 0; i < 100; i++) {
            original.nextLong();
        }
        SplitMix64State snapshot = original.state();

        SplitMix64 restored = new SplitMix64(snapshot);
        for (int i = 0; i < 1000; i++) {
            assertEquals(original.nextLong(), restored.nextLong(), "восстановление разошлось на шаге " + i);
        }
    }

    @Test
    void stateIsIndependentSnapshot() {
        SplitMix64 rng = new SplitMix64(7L);
        rng.nextLong();
        SplitMix64State snapshot = rng.state();
        rng.nextLong();
        // Снимок не меняется при дальнейшей генерации исходного генератора.
        SplitMix64 fromSnapshot = new SplitMix64(snapshot);
        SplitMix64 expected = new SplitMix64(7L);
        expected.nextLong(); // та же точка, что и snapshot
        assertEquals(expected.nextLong(), fromSnapshot.nextLong());
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
    void nextIntBoundedStaysInRange() {
        SplitMix64 rng = new SplitMix64(321L);
        int[] bounds = {1, 2, 5, 97, 1000};
        for (int bound : bounds) {
            for (int i = 0; i < 10_000; i++) {
                int v = rng.nextInt(bound);
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
    void nextBooleanYieldsBothValues() {
        SplitMix64 rng = new SplitMix64(9L);
        boolean sawTrue = false;
        boolean sawFalse = false;
        for (int i = 0; i < 10_000; i++) {
            if (rng.nextBoolean()) {
                sawTrue = true;
            } else {
                sawFalse = true;
            }
        }
        assertTrue(sawTrue, "nextBoolean() не дал ни одного true");
        assertTrue(sawFalse, "nextBoolean() не дал ни одного false");
    }

    @Test
    void rejectsNonPositiveBound() {
        SplitMix64 rng = new SplitMix64(1L);
        assertThrows(IllegalArgumentException.class, () -> rng.nextLong(0));
        assertThrows(IllegalArgumentException.class, () -> rng.nextLong(-1));
        assertThrows(IllegalArgumentException.class, () -> rng.nextInt(0));
        assertThrows(IllegalArgumentException.class, () -> rng.nextInt(-5));
    }

    @Test
    void nextLongBoundOneIsAlwaysZero() {
        SplitMix64 rng = new SplitMix64(1L);
        for (int i = 0; i < 100; i++) {
            assertEquals(0, rng.nextLong(1));
        }
    }
}

