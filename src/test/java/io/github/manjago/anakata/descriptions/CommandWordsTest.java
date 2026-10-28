package io.github.manjago.anakata.descriptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.manjago.anakata.core.maze.Direction;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CommandWordsTest {

    @Test
    void everyDirectionHasADistinctName() {
        Set<String> names = new HashSet<>();
        for (Direction direction : Direction.values()) {
            String name = CommandWords.name(direction);
            assertFalse(name.isBlank(), "пустое имя направления " + direction);
            assertTrue(names.add(name), "повтор имени направления: " + name);
        }
    }

    /**
     * Алиасы — латиница в нижнем регистре или знаки пунктуации: русских коротких
     * форм у нас нет. У каждого направления и у помощи есть хотя бы один алиас.
     */
    @Test
    void aliasesAreLatinLowercaseOrPunctuation() {
        for (Direction direction : Direction.values()) {
            List<String> aliases = CommandWords.aliases(direction);
            assertFalse(aliases.isEmpty(), "у направления " + direction + " нет алиасов");
            for (String alias : aliases) {
                assertAliasForm(alias);
            }
        }
        List<String> helpAliases = CommandWords.helpAliases();
        assertFalse(helpAliases.isEmpty(), "у помощи нет алиасов");
        for (String alias : helpAliases) {
            assertAliasForm(alias);
        }
    }

    /** Таблица слов без коллизий: ни одно слово не принадлежит двум командам. */
    @Test
    void wordsHaveNoCollisions() {
        Set<String> words = new HashSet<>();
        for (Direction direction : Direction.values()) {
            String name = CommandWords.name(direction);
            assertTrue(words.add(name), "повтор слова: " + name);
            for (String alias : CommandWords.aliases(direction)) {
                assertTrue(words.add(alias), "повтор алиаса: " + alias);
            }
        }
        for (String helpWord : CommandWords.helpWords()) {
            assertTrue(words.add(helpWord), "повтор слова помощи: " + helpWord);
        }
    }

    /**
     * Текст помощи знает только направления своей размерности: в 2D нет ни «вверх»,
     * ни «ана». Слова ищем токенами, а не подстрокой: короткий алиас (например,
     * «a») через {@code contains} нашёлся бы внутри почти любого слова с этой
     * буквой.
     */
    @Test
    void helpListsOnlyDirectionsOfTheDimension() {
        for (int dimensions = 2; dimensions <= 4; dimensions++) {
            Set<String> tokens = tokens(String.join("\n", CommandWords.help(dimensions)));
            assertFalse(tokens.isEmpty(), "пустая помощь для " + dimensions + "D");
            assertTrue(tokens.contains(CommandWords.HELP_NAME), "нет слова помощи в " + dimensions + "D");
            for (Direction direction : Direction.values()) {
                boolean expected = direction.axis() < dimensions;
                String name = CommandWords.name(direction);
                assertEquals(expected, tokens.contains(name), name + " в помощи для " + dimensions + "D");
                for (String alias : CommandWords.aliases(direction)) {
                    assertEquals(expected, tokens.contains(alias),
                            "алиас " + alias + " в помощи для " + dimensions + "D");
                }
            }
        }
    }

    @Test
    void helpRejectsDimensionOutsideRange() {
        assertThrows(IllegalArgumentException.class, () -> CommandWords.help(1));
        assertThrows(IllegalArgumentException.class, () -> CommandWords.help(5));
    }

    private static void assertAliasForm(String alias) {
        assertFalse(alias.isBlank(), "пустой алиас");
        assertTrue(alias.matches("[a-z\\p{Punct}]+"), "недопустимый алиас: " + alias);
    }

    /** Токены текста: латинские и русские слова и числа, без знаков. */
    private static Set<String> tokens(String text) {
        Set<String> tokens = new HashSet<>();
        for (String token : text.split("[^\\p{L}\\p{N}]+")) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
