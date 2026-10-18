package io.github.manjago.anakata.core.rng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SeedersTest {

    @Test
    void sameSeedAndNameYieldSameSequence() {
        long seed = 42L;
        SplitMix64 first = Seeders.branch(seed, "maze");
        SplitMix64 second = Seeders.branch(seed, "maze");
        for (int i = 0; i < 1000; i++) {
            assertEquals(first.nextLong(), second.nextLong(), "расхождение на шаге " + i);
        }
    }

    @Test
    void differentNamesYieldDifferentStreams() {
        long seed = 42L;
        SplitMix64 maze = Seeders.branch(seed, "maze");
        SplitMix64 letters = Seeders.branch(seed, "letters");
        boolean differ = false;
        for (int i = 0; i < 1000 && !differ; i++) {
            differ = maze.nextLong() != letters.nextLong();
        }
        assertTrue(differ, "потоки 'maze' и 'letters' не разошлись ни на одном шаге");
    }

    @Test
    void branchSeedFromEmptyAndFullNameDiffers() {
        long seed = 1L;
        SplitMix64 empty = Seeders.branch(seed, "");
        SplitMix64 word = Seeders.branch(seed, "letters");
        assertNotEquals(empty.nextLong(), word.nextLong());
    }

    @Test
    void knownBranchYieldsReferenceFirstValue() {
        // Эталон фиксирует свёртку имени: при изменении hashName тест упадёт,
        // чтобы выводы веток не менялись незаметно между версиями.
        long seed = 0L;
        SplitMix64 maze = Seeders.branch(seed, "maze");
        assertEquals(8530126917174426528L, maze.nextLong());
    }

    @Test
    void deriveIsDeterministicAndNamesDiffer() {
        long seed = 42L;
        assertEquals(Seeders.derive(seed, "maze"), Seeders.derive(seed, "maze"),
                "derive не детерминирован для одного сида и имени");
        assertNotEquals(Seeders.derive(seed, "maze"), Seeders.derive(seed, "letters"),
                "derive дал одинаковый сид для разных имён");
    }

    @Test
    void deriveFeedsBranchAndHierarchy() {
        // branch — это генератор из derive; производный сид можно снова развести деревом.
        long seed = 42L;
        long mazeSeed = Seeders.derive(seed, "maze");
        SplitMix64 fromDerive = new SplitMix64(mazeSeed);
        SplitMix64 fromBranch = Seeders.branch(seed, "maze");
        for (int i = 0; i < 1000; i++) {
            assertEquals(fromDerive.nextLong(), fromBranch.nextLong(),
                    "branch и derive(generator) разошлись на шаге " + i);
        }
    }

    @Test
    void deriveIsHierarchical() {
        // Результат derive — обычный long: его можно снова развести деревом.
        long seed = 42L;
        long maze = Seeders.derive(seed, "maze");
        long wordFromMaze = Seeders.derive(maze, "word");
        long lettersFromMaze = Seeders.derive(maze, "letters");
        assertNotEquals(maze, wordFromMaze);
        assertNotEquals(wordFromMaze, lettersFromMaze);

        // Ветка от ветки детерминированна: тот же путь — тот же поток.
        long maze2 = Seeders.derive(seed, "maze");
        long wordFromMaze2 = Seeders.derive(maze2, "word");
        assertEquals(wordFromMaze, wordFromMaze2);
    }
}
