package io.github.manjago.anakata.core.maze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DirectionTest {

    @Test
    void oppositeIsInvolution() {
        for (Direction direction : Direction.values()) {
            assertNotEquals(direction, direction.opposite());
            assertEquals(direction, direction.opposite().opposite());
        }
    }

    @Test
    void oppositeKeepsAxisAndFlipsSign() {
        for (Direction direction : Direction.values()) {
            assertEquals(direction.axis(), direction.opposite().axis());
            assertEquals(-direction.sign(), direction.opposite().sign());
        }
    }

    /**
     * Бит — уникальное число от 0 до 7: Direction не больше восьми, и вся раскладка
     * масок вмещается в байт. Таблица {@code BY_BIT} в {@link Direction#opposite()}
     * держится ровно на этой полноте.
     */
    @Test
    void bitsAreUniqueWithinByte() {
        Set<Integer> bits = new HashSet<>();
        for (Direction direction : Direction.values()) {
            int bit = direction.bit();
            assertTrue(bit >= 0 && bit < 8, "бит вне байта: " + bit);
            assertTrue(bits.add(bit), "повтор бита: " + bit);
        }
        assertEquals(8, bits.size());
    }
}
