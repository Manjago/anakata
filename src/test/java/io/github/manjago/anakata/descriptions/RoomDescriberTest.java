package io.github.manjago.anakata.descriptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.manjago.anakata.core.landmark.Landmark;
import io.github.manjago.anakata.core.landmark.LandmarkKind;
import io.github.manjago.anakata.core.landmark.Landmarks;
import io.github.manjago.anakata.core.maze.BacktrackerMazeGenerator;
import io.github.manjago.anakata.core.maze.Direction;
import io.github.manjago.anakata.core.maze.Grid;
import io.github.manjago.anakata.core.maze.Maze;
import io.github.manjago.anakata.core.rng.Seeders;
import io.github.manjago.anakata.core.room.RoomView;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoomDescriberTest {

    private static final RoomDescriber DESCRIBER = new RoomDescriber();

    @Test
    void everyDirectionHasADistinctName() {
        Set<String> names = new HashSet<>();
        for (Direction direction : Direction.values()) {
            String name = RoomDescriber.directionName(direction);
            assertFalse(name.isBlank(), "пустое имя направления " + direction);
            assertTrue(names.add(name), "повтор имени направления: " + name);
        }
    }

    @Test
    void deadEndHasOneExit() {
        assertEquals(List.of("Тупик. Выход: север."), describe(Direction.NORTH));
    }

    @Test
    void corridorWithTwoOppositeExitsIsStillACorridor() {
        assertEquals(List.of("Коридор. Выходы: север и юг."),
                describe(Direction.NORTH, Direction.SOUTH));
    }

    /**
     * Три и больше выходов — развилка, независимо от того, сколько их: у порога
     * лишь нижняя граница, а не точное число.
     */
    @Test
    void threeOrMoreExitsAreAJunction() {
        assertEquals(
                List.of("Развилка. Выходы: север, юг и восток."),
                describe(Direction.NORTH, Direction.SOUTH, Direction.EAST));
        assertEquals(
                List.of("Развилка. Выходы: север, юг, восток и запад."),
                describe(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST));
    }

    /** Выходы — множество: порядок на входе не меняет текст, его задаёт ядро. */
    @Test
    void exitOrderDoesNotChangeTheText() {
        List<String> first = describe(Direction.NORTH, Direction.EAST);
        List<String> second = describe(Direction.EAST, Direction.NORTH);
        assertEquals(first, second);
    }

    @Test
    void roomWithoutLandmarkHasOnlyExitsLine() {
        assertEquals(1, describe(Direction.WEST).size());
    }

    /**
     * Покрытие и различимость: у каждой пары «вид + вариант» есть своя непустая
     * строка, и все сорок восемь строк разны. Пустая или совпавшая строка убила бы
     * различимость, ради которой ориентиры и нужны. Снимок текста здесь не
     * проверяется — фразы это контент, и меняться между релизами они могут.
     */
    @Test
    void everyKindAndVariantHasADistinctPhrase() {
        Set<String> phrases = new HashSet<>();
        for (LandmarkKind kind : LandmarkKind.values()) {
            for (int variant = 0; variant < LandmarkKind.VARIANTS; variant++) {
                List<String> lines = DESCRIBER.describe(
                        new RoomView(List.of(Direction.NORTH), new Landmark(kind, variant)));
                assertEquals(2, lines.size(), "нет строки ориентира для " + kind + "/" + variant);
                String phrase = lines.get(1);
                assertFalse(phrase.isBlank(), "пустая фраза для " + kind + "/" + variant);
                assertTrue(phrases.add(phrase), "повтор фразы: " + phrase);
            }
        }
        assertEquals(LandmarkKind.values().length * LandmarkKind.VARIANTS, phrases.size());
    }

    @Test
    void landmarkComesAfterTheExitsLine() {
        List<String> lines = DESCRIBER.describe(new RoomView(
                List.of(Direction.NORTH, Direction.SOUTH),
                new Landmark(LandmarkKind.WALL_PATTERN, 0)));
        assertEquals(2, lines.size());
        assertEquals("Коридор. Выходы: север и юг.", lines.get(0));
        assertEquals("На стене выцарапана спираль.", lines.get(1));
    }

    /**
     * Сквозная протяжка «сид → лабиринт → ориентиры → RoomView → текст»: выходы
     * вида комнаты совпадают с {@code maze.exits}, ориентир — с {@code landmarks.at},
     * а число строк описания определяется наличием ориентира. Среди клеток должны
     * быть и примечательные, и пустые — иначе тест не проверил бы ветку пустой
     * комнаты.
     */
    @Test
    void describesRoomsBuiltFromSeedAndLabyrinth() {
        long seed = 42L;
        Grid grid = new Grid(6, 6, 6);
        Maze maze = new BacktrackerMazeGenerator()
                .generate(grid, Seeders.branch(seed, Seeders.Branch.MAZE));
        Landmarks landmarks = new Landmarks(
                grid, Seeders.derive(seed, Seeders.Branch.LANDMARKS), 2);

        boolean sawLandmark = false;
        boolean sawEmpty = false;
        for (int cell = 0; cell < grid.cells(); cell++) {
            RoomView view = RoomView.of(maze, landmarks, cell);
            List<Direction> expectedExits = List.of(maze.exits(cell));
            assertEquals(expectedExits, view.exits(), "выходы клетки " + cell);
            assertEquals(landmarks.at(cell), view.landmark(), "ориентир клетки " + cell);

            List<String> lines = DESCRIBER.describe(view);
            assertEquals(view.landmark() == null ? 1 : 2, lines.size(),
                    "число строк для клетки " + cell);
            if (view.landmark() == null) {
                sawEmpty = true;
            } else {
                sawLandmark = true;
            }
        }
        assertTrue(sawLandmark, "среди клеток не нашлось примечательной");
        assertTrue(sawEmpty, "среди клеток не нашлось пустой");
    }

    @Test
    void rejectsNullView() {
        assertThrows(NullPointerException.class, () -> DESCRIBER.describe(null));
    }

    private static List<String> describe(Direction... exits) {
        return DESCRIBER.describe(new RoomView(List.of(exits), null));
    }
}
