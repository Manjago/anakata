package io.github.manjago.anakata.descriptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.manjago.anakata.core.game.Outcome;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutcomeDescriberTest {

    private static final OutcomeDescriber DESCRIBER = new OutcomeDescriber();

    /** Обычный ход говорить нечего: о нём расскажет комната. */
    @Test
    void movedHasNoNarration() {
        assertTrue(DESCRIBER.describe(Outcome.MOVED).isEmpty());
    }

    @Test
    void blockedExplainsThatThereIsNoPassage() {
        List<String> lines = DESCRIBER.describe(Outcome.BLOCKED);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("не пройти"), lines.get(0));
    }

    @Test
    void rejectsNullOutcome() {
        assertThrows(NullPointerException.class, () -> DESCRIBER.describe(null));
    }
}
