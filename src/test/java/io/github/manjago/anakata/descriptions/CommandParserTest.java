package io.github.manjago.anakata.descriptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.manjago.anakata.core.game.Command;
import io.github.manjago.anakata.core.maze.Direction;
import io.github.manjago.anakata.descriptions.CommandParser.ParseResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class CommandParserTest {

    private static final CommandParser PARSER = new CommandParser();

    /** Каноническое слово и каждый алиас ведут в своё направление. */
    @Test
    void everyCanonicalWordAndAliasParsesToItsDirection() {
        for (Direction direction : Direction.values()) {
            List<String> words = new ArrayList<>();
            words.add(CommandWords.name(direction));
            words.addAll(CommandWords.aliases(direction));
            for (String word : words) {
                assertEquals(move(direction), PARSER.parse(word), "слово " + word);
            }
        }
    }

    @Test
    void parsingIgnoresCaseAndSurroundingSpaces() {
        assertEquals(move(Direction.NORTH), PARSER.parse("  СЕВЕР  "));
        assertEquals(move(Direction.WEST), PARSER.parse("W"));
    }

    @Test
    void helpWordsAreRecognized() {
        for (String word : CommandWords.helpWords()) {
            assertEquals(new ParseResult.Help(), PARSER.parse(word), "слово " + word);
        }
    }

    @Test
    void exitWordsAreRecognized() {
        for (String word : CommandWords.exitWords()) {
            assertEquals(new ParseResult.Exit(), PARSER.parse(word), "слово " + word);
        }
    }

    /** {@code isExit} нормализует строку так же, как {@code parse}. */
    @Test
    void isExitRecognizesExitWordsRegardlessOfCaseAndSpaces() {
        for (String word : CommandWords.exitWords()) {
            assertTrue(PARSER.isExit("  " + word.toUpperCase(Locale.ROOT) + "  "), word);
        }
        assertFalse(PARSER.isExit("север"));
        assertFalse(PARSER.isExit(""));
        assertFalse(PARSER.isExit("  "));
    }

    @Test
    void unknownWordsAreReportedAsIs() {
        assertEquals(new ParseResult.Unknown("абракадабра"), PARSER.parse("  Абракадабра "));
        assertEquals(new ParseResult.Unknown("на север"), PARSER.parse("на север"));
    }

    /** Пустая и пробельная строка — отдельный исход «переспроси», а не «не понял». */
    @Test
    void blankInputIsEmpty() {
        assertEquals(new ParseResult.Empty(), PARSER.parse(""));
        assertEquals(new ParseResult.Empty(), PARSER.parse("   "));
        assertEquals(new ParseResult.Empty(), PARSER.parse("\u00A0\t "));
    }

    /** Неразрывный пробел по краям обрезается, как обычный. */
    @Test
    void trimmingTreatsNonBreakingSpaceAsSpace() {
        assertEquals(new ParseResult.Empty(), PARSER.parse("\u00A0"));
        assertEquals(move(Direction.NORTH), PARSER.parse("\u00A0Север\u00A0"));
    }

    /** Парсер размерности не знает: «ана» разберётся и на 2D, отсеет её уровень. */
    @Test
    void parserDoesNotKnowTheDimension() {
        assertEquals(move(Direction.ANA), PARSER.parse("ана"));
    }

    @Test
    void parseRejectsNull() {
        assertThrows(NullPointerException.class, () -> PARSER.parse(null));
    }

    private static ParseResult move(Direction direction) {
        return new ParseResult.Parsed(new Command.Move(direction));
    }
}
