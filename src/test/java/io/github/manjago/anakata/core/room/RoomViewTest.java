package io.github.manjago.anakata.core.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.manjago.anakata.core.landmark.Landmark;
import io.github.manjago.anakata.core.landmark.LandmarkKind;
import io.github.manjago.anakata.core.maze.Direction;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoomViewTest {

    @Test
    void rejectsNullExits() {
        assertThrows(NullPointerException.class, () -> new RoomView(null, null));
    }

    @Test
    void rejectsEmptyExits() {
        assertThrows(IllegalArgumentException.class, () -> new RoomView(List.of(), null));
    }

    @Test
    void rejectsNullExit() {
        List<Direction> exits = new ArrayList<>();
        exits.add(Direction.NORTH);
        exits.add(null);
        assertThrows(NullPointerException.class, () -> new RoomView(exits, null));
    }

    @Test
    void rejectsDuplicateExit() {
        assertThrows(IllegalArgumentException.class,
                () -> new RoomView(List.of(Direction.NORTH, Direction.NORTH), null));
    }

    @Test
    void allowsNullLandmark() {
        assertNull(new RoomView(List.of(Direction.NORTH), null).landmark());
    }

    /**
     * Выходы копируются при создании: список — компонент {@code record}, и без
     * копии правка исходного списка меняла бы уже готовый вид комнаты.
     */
    @Test
    void defensivelyCopiesExits() {
        List<Direction> exits = new ArrayList<>(List.of(Direction.NORTH));
        RoomView view = new RoomView(exits, null);
        exits.add(Direction.SOUTH);
        assertEquals(List.of(Direction.NORTH), view.exits());
        assertThrows(UnsupportedOperationException.class, () -> view.exits().add(Direction.SOUTH));
    }

    /**
     * Выходы — множество, а не последовательность: порядок на входе не влияет ни
     * на равенство, ни на {@code hashCode}, а хранятся они в каноническом порядке
     * направлений. Это заодно фиксирует выбор {@code List}, а не массива: равенство
     * идёт по содержимому.
     */
    @Test
    void equalRoomsAreEqualRegardlessOfExitOrder() {
        Landmark landmark = new Landmark(LandmarkKind.SOUND, 3);
        RoomView first = new RoomView(List.of(Direction.NORTH, Direction.EAST), landmark);
        RoomView second = new RoomView(List.of(Direction.EAST, Direction.NORTH), landmark);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(List.of(Direction.NORTH, Direction.EAST), first.exits());
    }
}
