package io.github.manjago.anakata.core.landmark;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.manjago.anakata.core.maze.Grid;
import io.github.manjago.anakata.core.rng.Seeders;
import io.github.manjago.anakata.core.rng.SplitMix64;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LandmarksTest {

    /** Соль — обычный подсид ветки ориентиров; тесты берут её так же, как возьмёт игра. */
    private static Landmarks landmarks(Grid grid, long seed, int inEvery) {
        return new Landmarks(grid, Seeders.derive(seed, Seeders.Branch.LANDMARKS), inEvery);
    }

    @Test
    void sameSaltGridAndCellYieldSameLandmark() {
        Grid grid = new Grid(5, 6);
        Landmarks first = landmarks(grid, 42L, 8);
        Landmarks second = landmarks(grid, 42L, 8);
        for (int cell = 0; cell < grid.cells(); cell++) {
            assertEquals(first.at(cell), second.at(cell), "клетка " + cell);
        }
    }

    @Test
    void differentSaltsYieldDifferentFields() {
        Grid grid = new Grid(16, 16);
        Landmarks first = landmarks(grid, 1L, 8);
        Landmarks second = landmarks(grid, 2L, 8);
        boolean differ = false;
        for (int cell = 0; cell < grid.cells() && !differ; cell++) {
            differ = !Objects.equals(first.at(cell), second.at(cell));
        }
        assertTrue(differ, "разные соли дали одинаковое поле ориентиров");
    }

    /**
     * Присутствие решает порог, а не остаток, поэтому доля примечательных клеток
     * на большой решётке должна быть близка к {@code 1 / inEvery}. Тест
     * детерминированный: соль фиксирована, допуск только страхует от вырожденной
     * формулы. Проверяются и кратная плотность, и нет.
     */
    @Test
    void densityMatchesInEvery() {
        Grid grid = new Grid(64, 64);
        for (int inEvery : new int[] {7, 8}) {
            Landmarks landmarks = landmarks(grid, 42L, inEvery);
            int present = 0;
            for (int cell = 0; cell < grid.cells(); cell++) {
                if (landmarks.at(cell) != null) {
                    present++;
                }
            }
            double expected = (double) grid.cells() / inEvery;
            assertEquals(expected, present, expected * 0.1,
                    "плотность разошлась при inEvery = " + inEvery + ": " + present);
        }
    }

    @Test
    void inEveryOneMakesEveryCellNotable() {
        Grid grid = new Grid(8, 8, 8);
        Landmarks landmarks = landmarks(grid, 7L, 1);
        for (int cell = 0; cell < grid.cells(); cell++) {
            assertNotNull(landmarks.at(cell), "клетка " + cell + " без ориентира");
        }
    }

    @Test
    void rejectsInEveryBelowOne() {
        Grid grid = new Grid(3, 3);
        assertThrows(IllegalArgumentException.class, () -> new Landmarks(grid, 42L, 0));
        assertThrows(IllegalArgumentException.class, () -> new Landmarks(grid, 42L, -3));
    }

    @Test
    void rejectsNullGrid() {
        assertThrows(NullPointerException.class, () -> new Landmarks(null, 42L, 1));
    }

    @Test
    void rejectsCellOutsideGrid() {
        Grid grid = new Grid(3, 3);
        Landmarks landmarks = landmarks(grid, 42L, 1);
        assertThrows(IndexOutOfBoundsException.class, () -> landmarks.at(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> landmarks.at(grid.cells()));
    }

    @Test
    void everyLandmarkHasKnownKindAndVariantInRange() {
        Grid grid = new Grid(32, 32);
        Landmarks landmarks = landmarks(grid, 123L, 1);
        for (int cell = 0; cell < grid.cells(); cell++) {
            Landmark landmark = landmarks.at(cell);
            assertNotNull(landmark);
            assertNotNull(landmark.kind());
            assertTrue(landmark.variant() >= 0 && landmark.variant() < LandmarkKind.VARIANTS,
                    "вариант вне диапазона: " + landmark.variant());
        }
    }

    /**
     * Каждый из шести видов и все восемь вариантов должны встретиться хотя бы
     * раз: вырожденная схема (вид всегда первый, вариант всегда нулевой) прошла
     * бы проверки диапазона, но убила бы различимость.
     */
    @Test
    void coversEveryKindAndVariant() {
        Grid grid = new Grid(64, 64);
        Landmarks landmarks = landmarks(grid, 2024L, 1);
        Set<Landmark> seen = new HashSet<>();
        for (int cell = 0; cell < grid.cells(); cell++) {
            seen.add(landmarks.at(cell));
        }
        for (LandmarkKind kind : LandmarkKind.values()) {
            for (int variant = 0; variant < LandmarkKind.VARIANTS; variant++) {
                assertTrue(seen.contains(new Landmark(kind, variant)),
                        "не встретилась пара " + kind + "/" + variant);
            }
        }
    }

    /**
     * {@code fieldHash} кодирует ориентир через {@code ordinal()} и потому
     * перестановку видов не видит: по тому же индексу снова лежит константа с
     * тем же ordinal. Порядок фиксируется здесь явно — он часть контракта сида.
     */
    @Test
    void kindOrderIsStable() {
        assertArrayEquals(new LandmarkKind[] {
                LandmarkKind.OBJECT,
                LandmarkKind.WALL_PATTERN,
                LandmarkKind.FLOOR_TEXTURE,
                LandmarkKind.LIGHT,
                LandmarkKind.SOUND,
                LandmarkKind.SMELL}, LandmarkKind.values());
    }

    /**
     * Размерность входит в хеш по раунду свёртки на ось, поэтому координаты
     * (x, y) плоской решётки и (x, y, 0) объёмной в целом дают разные ориентиры
     * (отдельная пара может совпасть случайно, поэтому тест требует хотя бы
     * одного различия). Индекс у них общий: третья ось самая медленная.
     */
    @Test
    void dimensionParticipatesInTheScheme() {
        Grid flat = new Grid(4, 4);
        Grid tall = new Grid(4, 4, 2);
        Landmarks inPlane = landmarks(flat, 42L, 1);
        Landmarks inSpace = landmarks(tall, 42L, 1);
        boolean differ = false;
        for (int cell = 0; cell < flat.cells() && !differ; cell++) {
            differ = !Objects.equals(inPlane.at(cell), inSpace.at(cell));
        }
        assertTrue(differ, "размерность не влияет на ориентир");
    }

    /**
     * Плотность — это порог по хешу, а не остаток, поэтому поля вложены при
     * любой смене плотности: рост {@code inEvery} только убирает ориентиры, но
     * не добавляет. Для остатка вложенность держалась бы лишь на кратных.
     */
    @Test
    void sparserDensityOnlyRemovesLandmarks() {
        Grid grid = new Grid(64, 64);
        Landmarks dense = landmarks(grid, 9L, 5);
        Landmarks sparse = landmarks(grid, 9L, 8);
        boolean removed = false;
        for (int cell = 0; cell < grid.cells(); cell++) {
            Landmark sparseLandmark = sparse.at(cell);
            if (sparseLandmark != null) {
                assertEquals(sparseLandmark, dense.at(cell),
                        "клетка " + cell + " разошлась между inEvery = 8 и inEvery = 5");
            }
            removed |= sparseLandmark == null && dense.at(cell) != null;
        }
        assertTrue(removed, "плотности 5 и 8 не различились");
    }

    /**
     * Вид и вариант считаются из того же хеша, что и присутствие, но от порога
     * не зависят: ориентир примечательной клетки одинаков при любой плотности.
     */
    @Test
    void kindAndVariantIgnoreDensity() {
        Grid grid = new Grid(32, 32);
        Landmarks sparse = landmarks(grid, 11L, 4);
        Landmarks every = landmarks(grid, 11L, 1);
        for (int cell = 0; cell < grid.cells(); cell++) {
            Landmark landmark = sparse.at(cell);
            if (landmark != null) {
                assertEquals(landmark, every.at(cell), "клетка " + cell);
            }
        }
    }

    @Test
    void landmarkRejectsNullKindAndVariantOutOfRange() {
        assertThrows(NullPointerException.class, () -> new Landmark(null, 0));
        assertThrows(IllegalArgumentException.class, () -> new Landmark(LandmarkKind.SOUND, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new Landmark(LandmarkKind.SOUND, LandmarkKind.VARIANTS));
    }

    /**
     * Растяжка, а не независимый эталон: фиксирует целиком счётную формулу —
     * свёртку соли с координатами, порог присутствия, выбор вида и варианта.
     * Если тест падает, ориентиры по опубликованным сидам изменились — менять
     * нельзя молча.
     */
    @Test
    void seedContractIsStable() {
        assertFieldHash(new Grid(6, 6), 42L, 8, -7001948771908451080L);
        assertFieldHash(new Grid(4, 4, 4), 42L, 8, 1109340933548615472L);
        assertFieldHash(new Grid(3, 3, 3, 3), 42L, 8, -3783795218684683325L);
    }

    private static void assertFieldHash(Grid grid, long seed, int inEvery, long expected) {
        assertEquals(expected, fieldHash(grid, landmarks(grid, seed, inEvery)),
                "контракт сида сдвинулся для решётки " + grid);
    }

    /** Свёртка поля ориентиров генератором проекта: та же разболтка, что и в сиде. */
    private static long fieldHash(Grid grid, Landmarks landmarks) {
        long hash = 0;
        for (int cell = 0; cell < grid.cells(); cell++) {
            Landmark landmark = landmarks.at(cell);
            long code = landmark == null
                    ? 0L
                    : 1L + landmark.kind().ordinal() * LandmarkKind.VARIANTS + landmark.variant();
            hash = new SplitMix64(hash ^ code).nextLong();
        }
        return hash;
    }
}
