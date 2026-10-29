package io.github.manjago.anakata.core.maze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GridTest {

    @Test
    void countsCells() {
        assertEquals(12, new Grid(3, 4).cells());
        assertEquals(60, new Grid(3, 4, 5).cells());
        assertEquals(81, new Grid(3, 3, 3, 3).cells());
    }

    @Test
    void indexAndCoordinatesAreInverse() {
        Grid[] grids = {new Grid(3, 4), new Grid(4, 5, 6), new Grid(3, 3, 4, 2)};
        for (Grid grid : grids) {
            int[] coordinates = new int[grid.dimensions()];
            for (int cell = 0; cell < grid.cells(); cell++) {
                grid.coordinates(cell, coordinates);
                assertEquals(cell, grid.index(coordinates),
                        "индекс не сошёлся на клетке " + cell + " решётки " + grid);
            }
        }
    }

    @Test
    void coordinateMatchesFullDecomposition() {
        Grid[] grids = {new Grid(3, 4), new Grid(4, 5, 6), new Grid(3, 3, 4, 2)};
        for (Grid grid : grids) {
            int[] coordinates = new int[grid.dimensions()];
            for (int cell = 0; cell < grid.cells(); cell++) {
                grid.coordinates(cell, coordinates);
                for (int axis = 0; axis < grid.dimensions(); axis++) {
                    assertEquals(coordinates[axis], grid.coordinate(cell, axis),
                            "ось " + axis + " клетки " + cell + " решётки " + grid);
                }
            }
        }
    }

    @Test
    void indexFollowsFirstAxisFastest() {
        Grid grid = new Grid(3, 4);
        assertEquals(0, grid.index(0, 0));
        assertEquals(1, grid.index(1, 0));
        assertEquals(3, grid.index(0, 1));
        assertEquals(11, grid.index(2, 3));
    }

    /**
     * Свойство вместо ручных кейсов: сосед смещён ровно на {@code sign} по своей
     * оси, совпадает по остальным, а вне границы его нет. Решётки с разными
     * размерами по осям ловят ошибки в страйдах, а 3D/4D проверяются наравне с 2D.
     */
    @Test
    void neighborShiftsExactlyOneOnItsAxisOrIsOutside() {
        Grid[] grids = {new Grid(3, 4), new Grid(4, 5, 6), new Grid(3, 3, 4, 2)};
        for (Grid grid : grids) {
            int[] coordinates = new int[grid.dimensions()];
            int[] neighborCoordinates = new int[grid.dimensions()];
            for (int cell = 0; cell < grid.cells(); cell++) {
                grid.coordinates(cell, coordinates);
                for (Direction direction : grid.directions()) {
                    int axis = direction.axis();
                    int neighbor = grid.neighbor(cell, direction);
                    boolean inside = direction.sign() > 0
                            ? coordinates[axis] + 1 < grid.size(axis)
                            : coordinates[axis] > 0;
                    if (!inside) {
                        assertEquals(-1, neighbor,
                                "у клетки " + cell + " решётки " + grid
                                        + " не должно быть соседа по " + direction);
                        continue;
                    }
                    grid.coordinates(neighbor, neighborCoordinates);
                    for (int other = 0; other < grid.dimensions(); other++) {
                        int expected = other == axis
                                ? coordinates[other] + direction.sign()
                                : coordinates[other];
                        assertEquals(expected, neighborCoordinates[other],
                                "ось " + other + " соседа по " + direction
                                        + " в решётке " + grid);
                    }
                }
            }
        }
    }

    @Test
    void directionsMatchDimension() {
        assertEquals(4, new Grid(3, 3).directions().length);
        assertEquals(6, new Grid(3, 3, 3).directions().length);
        assertEquals(8, new Grid(3, 3, 3, 3).directions().length);
    }

    @Test
    void rejectsTooSmallAxis() {
        assertThrows(IllegalArgumentException.class, () -> new Grid(1, 3));
        assertThrows(IllegalArgumentException.class, () -> new Grid(3, 0));
    }

    /**
     * Сообщение называет позицию с единицы и само значение: «2 1» — ошибка во
     * втором числе, а не в «оси 1» (нулевой по счёту).
     */
    @Test
    void tooSmallAxisMessageNamesPositionFromOne() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new Grid(2, 1));

        assertTrue(failure.getMessage().contains("№2"), failure.getMessage());
        assertTrue(failure.getMessage().contains(": 1"), failure.getMessage());
    }

    /** Та же однозначность у координаты и у несуществующей оси. */
    @Test
    void axisMessagesNamePositionFromOne() {
        Grid grid = new Grid(3, 4);

        IndexOutOfBoundsException outOfRange = assertThrows(IndexOutOfBoundsException.class,
                () -> grid.index(0, 5));
        assertTrue(outOfRange.getMessage().contains("№2"), outOfRange.getMessage());

        IndexOutOfBoundsException noAxis = assertThrows(IndexOutOfBoundsException.class,
                () -> grid.coordinate(0, 2));
        assertTrue(noAxis.getMessage().contains("№3"), noAxis.getMessage());
    }

    @Test
    void rejectsDimensionOutsideTwoToFour() {
        assertThrows(IllegalArgumentException.class, () -> new Grid());
        assertThrows(IllegalArgumentException.class, () -> new Grid(7));
        assertThrows(IllegalArgumentException.class, () -> new Grid(2, 2, 2, 2, 2));
    }

    @Test
    void rejectsDirectionOutsideDimension() {
        Grid grid = new Grid(3, 3);
        assertThrows(IllegalArgumentException.class, () -> grid.neighbor(0, Direction.UP));
        assertThrows(IllegalArgumentException.class, () -> grid.neighbor(0, Direction.ANA));
    }

    @Test
    void rejectsCellAndCoordinateOutOfRange() {
        Grid grid = new Grid(3, 4);
        int[] coordinates = new int[2];
        assertThrows(IndexOutOfBoundsException.class, () -> grid.coordinates(-1, coordinates));
        assertThrows(IndexOutOfBoundsException.class, () -> grid.coordinates(12, coordinates));
        assertThrows(IndexOutOfBoundsException.class, () -> grid.index(3, 0));
        assertThrows(IllegalArgumentException.class, () -> grid.index(1));
        assertThrows(IndexOutOfBoundsException.class, () -> grid.coordinate(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> grid.coordinate(12, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> grid.coordinate(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> grid.coordinate(0, 2));
    }

    @Test
    void printsDimensions() {
        assertEquals("3×4×5", new Grid(3, 4, 5).toString());
    }
}
