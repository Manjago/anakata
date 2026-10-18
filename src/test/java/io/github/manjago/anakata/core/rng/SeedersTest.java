package io.github.manjago.anakata.core.rng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SeedersTest {

    @Test
    void sameRootAndNameYieldSameSequence() {
        Rng root = new SplitMix64(42L);
        Rng first = Seeders.branch(root, "maze");
        Rng second = Seeders.branch(root, "maze");
        for (int i = 0; i < 1000; i++) {
            assertEquals(first.nextLong(), second.nextLong(), "расхождение на шаге " + i);
        }
    }

    @Test
    void differentNamesYieldDifferentStreams() {
        Rng root = new SplitMix64(42L);
        Rng maze = Seeders.branch(root, "maze");
        Rng letters = Seeders.branch(root, "letters");
        boolean differ = false;
        for (int i = 0; i < 1000 && !differ; i++) {
            differ = maze.nextLong() != letters.nextLong();
        }
        assertTrue(differ, "потоки 'maze' и 'letters' не разошлись ни на одном шаге");
    }

    @Test
    void branchDoesNotAdvanceRoot() {
        Rng plain = new SplitMix64(7L);
        Rng withBranch = new SplitMix64(7L);
        Seeders.branch(withBranch, "maze");
        Seeders.branch(withBranch, "letters");
        for (int i = 0; i < 1000; i++) {
            assertEquals(plain.nextLong(), withBranch.nextLong(),
                    "ответвление продвинуло корень на шаге " + i);
        }
    }

    @Test
    void branchesAreIndependentOfRootConsumption() {
        // Очерёдность создания веток не влияет на их содержимое: каждая выводится
        // только из состояния корня и имени.
        Rng rootLazy = new SplitMix64(99L);
        Rng lazyLetters = Seeders.branch(rootLazy, "letters");
        for (int i = 0; i < 500; i++) {
            rootLazy.nextLong();
        }
        Rng rootEager = new SplitMix64(99L);
        Seeders.branch(rootEager, "maze");
        Rng eagerLetters = Seeders.branch(rootEager, "letters");
        for (int i = 0; i < 1000; i++) {
            assertEquals(lazyLetters.nextLong(), eagerLetters.nextLong(),
                    "ветка зависит от проматывания корня на шаге " + i);
        }
    }

    @Test
    void branchSeedFromEmptyAndFullNameDiffers() {
        Rng root = new SplitMix64(1L);
        Rng empty = Seeders.branch(root, "");
        Rng word = Seeders.branch(root, "letters");
        assertNotEquals(empty.nextLong(), word.nextLong());
    }

    @Test
    void knownBranchYieldsReferenceFirstValue() {
        // Эталон фиксирует свёртку имени: при изменении hashName тест упадёт,
        // чтобы выводы веток не менялись незаметно между версиями.
        Rng root = new SplitMix64(0L);
        Rng maze = Seeders.branch(root, "maze");
        assertEquals(8530126917174426528L, maze.nextLong());
    }
}
