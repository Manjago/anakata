package io.github.manjago.anakata.core.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.manjago.anakata.core.maze.Direction;
import io.github.manjago.anakata.core.room.RoomView;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReplyTest {

    private static final RoomView ROOM = new RoomView(List.of(Direction.NORTH, Direction.EAST), null);

    @Test
    void copiesAvailableActions() {
        List<Command> mutable = new ArrayList<>();
        mutable.add(new Command.Move(Direction.NORTH));
        Reply reply = new Reply(ROOM, Outcome.MOVED, mutable);

        mutable.add(new Command.Move(Direction.SOUTH));

        assertEquals(List.of(new Command.Move(Direction.NORTH)), reply.availableActions());
        assertThrows(UnsupportedOperationException.class,
                () -> reply.availableActions().add(new Command.Move(Direction.EAST)));
    }

    @Test
    void rejectsNullRoom() {
        assertThrows(NullPointerException.class,
                () -> new Reply(null, Outcome.MOVED, List.of()));
    }

    @Test
    void rejectsNullOutcome() {
        assertThrows(NullPointerException.class, () -> new Reply(ROOM, null, List.of()));
    }

    @Test
    void rejectsNullActions() {
        assertThrows(NullPointerException.class, () -> new Reply(ROOM, Outcome.MOVED, null));
    }

    @Test
    void rejectsNullActionElement() {
        List<Command> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> new Reply(ROOM, Outcome.MOVED, withNull));
    }
}
