package io.github.manjago.anakata.descriptions;

import io.github.manjago.anakata.core.maze.Direction;
import java.util.ArrayList;
import java.util.List;

/**
 * Словарь слов игрока: канонические названия направлений, короткие алиасы и слова
 * помощи.
 *
 * <p>Здесь только слова, но не их смысл и не разбор строки — разбор в
 * {@link CommandParser}. Канонические названия нужны и показу, и вводу:
 * {@code RoomDescriber} берёт их для строки выходов, а {@link CommandParser} — для
 * таблицы слов, поэтому «север» в описании комнаты и слово команды совпадают по
 * построению, а не случайно.
 *
 * <p>Алиасы — латиница в нижнем регистре и знаки (например, «?» у помощи).
 * Русских коротких форм нет: короткие односложны и спорны («в» — это и «восток»,
 * и «вверх»), а латинская буква и короче, и однозначнее. Целевой ввод в
 * мессенджерах — кнопки с каноническими словами, алиасы — для ручного ввода
 * (консоль, строка в чате). Форму держат тесты: непустота и уникальность слов,
 * допустимая раскладка алиасов.
 *
 * <p>Слова — контент, а не контракт сида: их можно менять между релизами.
 */
public final class CommandWords {

    /** Каноническое слово помощи. */
    public static final String HELP_NAME = "помощь";

    private static final List<String> HELP_ALIASES = List.of("help", "?");

    private static final List<String> HELP_WORDS = buildHelpWords();

    private CommandWords() {
    }

    /**
     * Каноническое русское название направления — то же, что игрок видит в строке
     * выходов.
     */
    public static String name(Direction direction) {
        return switch (direction) {
            case NORTH -> "север";
            case SOUTH -> "юг";
            case EAST -> "восток";
            case WEST -> "запад";
            case UP -> "вверх";
            case DOWN -> "вниз";
            case ANA -> "ана";
            case KATA -> "ката";
        };
    }

    /** Короткие латинские алиасы направления. */
    public static List<String> aliases(Direction direction) {
        return switch (direction) {
            case NORTH -> List.of("n");
            case SOUTH -> List.of("s");
            case EAST -> List.of("e");
            case WEST -> List.of("w");
            case UP -> List.of("u");
            case DOWN -> List.of("d");
            case ANA -> List.of("a");
            case KATA -> List.of("k");
        };
    }

    /** Все слова помощи: каноническое и алиасы. */
    public static List<String> helpWords() {
        return HELP_WORDS;
    }

    /** Короткие алиасы помощи. */
    public static List<String> helpAliases() {
        return HELP_ALIASES;
    }

    /** Является ли слово запросом помощи — любое из {@link #helpWords()}. */
    public static boolean isHelp(String word) {
        return HELP_WORDS.contains(word);
    }

    /**
     * Текст помощи списком строк для уровня заданной размерности: канонические
     * слова и алиасы ходов, доступных в этой размерности, и слово помощи.
     * Генерируется из этой же таблицы, поэтому подсказка не разъедется с разбором.
     * «Выход» и админ-команды сюда не входят — их дописывает адаптер.
     *
     * @param dimensions размерность уровня, от 2 до 4
     * @return строки помощи в порядке показа
     * @throws IllegalArgumentException если размерность вне 2..4
     */
    public static List<String> help(int dimensions) {
        if (dimensions < 2 || dimensions > 4) {
            throw new IllegalArgumentException("размерность должна быть 2, 3 или 4: " + dimensions);
        }
        StringBuilder moves = new StringBuilder("Ходи так: ");
        boolean first = true;
        for (Direction direction : Direction.values()) {
            if (direction.axis() >= dimensions) {
                continue;
            }
            if (!first) {
                moves.append(", ");
            }
            first = false;
            moves.append(name(direction))
                    .append(" (")
                    .append(String.join(", ", aliases(direction)))
                    .append(')');
        }
        return List.of(
                moves.append('.').toString(),
                "Подсказка: " + HELP_NAME + " (" + String.join(", ", HELP_ALIASES) + ").");
    }

    private static List<String> buildHelpWords() {
        List<String> words = new ArrayList<>(1 + HELP_ALIASES.size());
        words.add(HELP_NAME);
        words.addAll(HELP_ALIASES);
        return List.copyOf(words);
    }
}
