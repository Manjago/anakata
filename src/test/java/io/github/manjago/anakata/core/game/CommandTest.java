package io.github.manjago.anakata.core.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.manjago.anakata.core.maze.Direction;
import org.junit.jupiter.api.Test;

class CommandTest {

    @Test
    void moveKeepsDirection() {
        assertEquals(Direction.ANA, new Command.Move(Direction.ANA).direction());
    }

    @Test
    void movesWithSameDirectionAreEqual() {
        assertEquals(new Command.Move(Direction.NORTH), new Command.Move(Direction.NORTH));
        assertNotEquals(new Command.Move(Direction.NORTH), new Command.Move(Direction.SOUTH));
    }

    @Test
    void moveRejectsNullDirection() {
        assertThrows(NullPointerException.class, () -> new Command.Move(null));
    }
}
