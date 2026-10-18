package io.github.manjago.anakata.core.rng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SeedersTest {

    @Test
    void sameSeedAndNameYieldSameSequence() {
        long seed = 42L;
        SplitMix64 first = Seeders.branch(seed, Seeders.Branch.MAZE);
        SplitMix64 second = Seeders.branch(seed, Seeders.Branch.MAZE);
        for (int i = 0; i < 1000; i++) {
            assertEquals(first.nextLong(), second.nextLong(), "расхождение на шаге " + i);
        }
    }

    @Test
    void differentNamesYieldDifferentStreams() {
        long seed = 42L;
        SplitMix64 maze = Seeders.branch(seed, Seeders.Branch.MAZE);
        SplitMix64 letters = Seeders.branch(seed, Seeders.Branch.LETTERS);
        boolean differ = false;
        for (int i = 0; i < 1000 && !differ; i++) {
            differ = maze.nextLong() != letters.nextLong();
        }
        assertTrue(differ, "потоки MAZE и LETTERS не разошлись ни на одном шаге");
    }

    @Test
    void knownBranchYieldsReferenceFirstValue() {
        // Эталон фиксирует свёртку имени: при изменении literal тест упадёт,
        // чтобы выводы веток не менялись незаметно между версиями.
        long seed = 0L;
        SplitMix64 maze = Seeders.branch(seed, Seeders.Branch.MAZE);
        assertEquals(8530126917174426528L, maze.nextLong());
    }

    @Test
    void deriveIsDeterministicAndNamesDiffer() {
        long seed = 42L;
        assertEquals(Seeders.derive(seed, Seeders.Branch.MAZE), Seeders.derive(seed, Seeders.Branch.MAZE),
                "derive не детерминирован для одного сида и имени");
        assertNotEquals(Seeders.derive(seed, Seeders.Branch.MAZE), Seeders.derive(seed, Seeders.Branch.LETTERS),
                "derive дал одинаковый сид для разных имён");
    }

    @Test
    void deriveFeedsBranchAndHierarchy() {
        // branch — это генератор из derive; производный сид можно снова развести деревом.
        long seed = 42L;
        long mazeSeed = Seeders.derive(seed, Seeders.Branch.MAZE);
        SplitMix64 fromDerive = new SplitMix64(mazeSeed);
        SplitMix64 fromBranch = Seeders.branch(seed, Seeders.Branch.MAZE);
        for (int i = 0; i < 1000; i++) {
            assertEquals(fromDerive.nextLong(), fromBranch.nextLong(),
                    "branch и derive(generator) разошлись на шаге " + i);
        }
    }

    @Test
    void deriveIsHierarchical() {
        // Результат derive — обычный long: его можно снова развести деревом.
        long seed = 42L;
        long maze = Seeders.derive(seed, Seeders.Branch.MAZE);
        long wordFromMaze = Seeders.derive(maze, Seeders.Branch.WORD);
        long lettersFromMaze = Seeders.derive(maze, Seeders.Branch.LETTERS);
        assertNotEquals(maze, wordFromMaze);
        assertNotEquals(wordFromMaze, lettersFromMaze);

        // Ветка от ветки детерминированна: тот же путь — тот же поток.
        long maze2 = Seeders.derive(seed, Seeders.Branch.MAZE);
        long wordFromMaze2 = Seeders.derive(maze2, Seeders.Branch.WORD);
        assertEquals(wordFromMaze, wordFromMaze2);
    }

    @Test
    void branchLiteralsAreUnique() {
        // Литералы — часть контракта сида: дубль стал бы копипастой потоков,
        // когда разные константы хешируются одинаково.
        Set<String> literals = new HashSet<>();
        for (Seeders.Branch branch : Seeders.Branch.values()) {
            assertTrue(literals.add(branch.literal()), "повтор литерала: " + branch);
        }
    }
}
