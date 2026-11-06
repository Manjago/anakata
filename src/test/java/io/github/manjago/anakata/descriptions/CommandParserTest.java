package io.github.manjago.anakata.descriptions;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

    /** {@code words} нормализует как {@code parse} и делит по сериям пробелов. */
    @Test
    void wordsSplitsOnOrdinarySpacesAndNormalizes() {
        assertArrayEquals(new String[] {"север", "1"}, CommandParser.words("  СЕВЕР  1 "));
    }

    /** Неразрывный пробел внутри строки — такой же разделитель, как обычный. */
    @Test
    void wordsTreatsNonBreakingSpaceAsSeparator() {
        assertArrayEquals(new String[] {"телепорт", "1", "2"},
                CommandParser.words("телепорт\u00A01\u00A02"));
    }

    @Test
    void wordsSplitsOnTabs() {
        assertArrayEquals(new String[] {"a", "b"}, CommandParser.words("a\tb"));
    }

    /** Серии разделителей дают одно разделение, а не пустые слова. */
    @Test
    void wordsCollapsesRunsOfSeparators() {
        assertArrayEquals(new String[] {"a", "b"},
                CommandParser.words(" \u00A0a\t\t b\u00A0 "));
    }

    /** Пустая и пробельная строка — пустой массив, а не {@code [""]}. */
    @Test
    void wordsOfBlankInputIsEmpty() {
        assertArrayEquals(new String[0], CommandParser.words(""));
        assertArrayEquals(new String[0], CommandParser.words("   "));
        assertArrayEquals(new String[0], CommandParser.words("\u00A0\t "));
    }

    @Test
    void wordsRejectsNull() {
        assertThrows(NullPointerException.class, () -> CommandParser.words(null));
    }

    @Test
    void parseRejectsNull() {
        assertThrows(NullPointerException.class, () -> PARSER.parse(null));
    }

    private static ParseResult move(Direction direction) {
        return new ParseResult.Parsed(new Command.Move(direction));
    }
}
