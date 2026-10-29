package io.github.manjago.anakata.descriptions;

import io.github.manjago.anakata.core.game.Command;
import io.github.manjago.anakata.core.maze.Direction;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Разбирает строку игрока в типизированный результат по таблице слов
 * {@link CommandWords}.
 *
 * <p>Вход — одна строка, на выходе {@link ParseResult}: команда ядра, запрос помощи,
 * выход из игры, пустой ввод или «не понял». Ядро про строки не знает — слова живут
 * в пакете описаний. Слова помощи и выхода здесь по той же причине, что и
 * остальные: это ввод игрока. Но поведение (что показать, выйти из цикла) — дело
 * адаптера, {@code descriptions} лишь распознаёт запрос (см. DESIGN.md, «Разбор
 * строки»).
 *
 * <p>Разбор строгий: строка обрезается по краям, приводится к нижнему регистру и
 * сравнивается со словом целиком (подробности — у {@link #normalize}). Ни
 * префиксов, ни лишних слов: «на север» — не команда. Парсер не знает размерности
 * уровня, поэтому «ана» разберётся и на 2D: отсеять недоступное направление — дело
 * {@code Level}.
 */
public final class CommandParser {

    private static final Map<String, Direction> DIRECTION_WORDS = buildDirectionWords();

    /**
     * Что оказалось на входе: команда ядра, запрос помощи, пустая строка или
     * неразобранное слово.
     */
    public sealed interface ParseResult
            permits ParseResult.Parsed, ParseResult.Help, ParseResult.Exit,
                    ParseResult.Empty, ParseResult.Unknown {

        /** Команда ядра; сейчас это всегда ход. */
        record Parsed(Command command) implements ParseResult {

            /** @throws NullPointerException если команда не задана */
            public Parsed {
                Objects.requireNonNull(command, "команда");
            }
        }

        /** Запрос помощи; что показать — решает адаптер. */
        record Help() implements ParseResult {
        }

        /**
         * Запрос выхода из игры; что делать (завершить цикл) — решает адаптер,
         * ядро про выход не знает.
         */
        record Exit() implements ParseResult {
        }

        /**
         * Пустой ввод: строка из одних пробелов. Отдельный исход, а не «не понял»:
         * адаптеру это «просто переспроси», а не ошибка игрока.
         */
        record Empty() implements ParseResult {
        }

        /** Слово не из таблицы; несёт нормализованное слово, чтобы адаптер его показал. */
        record Unknown(String word) implements ParseResult {

            /** @throws NullPointerException если слово не задано */
            public Unknown {
                Objects.requireNonNull(word, "слово");
            }
        }
    }

    /**
     * Разбирает строку игрока. Строка обрезается по краям (пробелы и неразрывный
     * пробел), приводится к нижнему регистру и сравнивается со словом целиком.
     *
     * @param line строка ввода
     * @return результат разбора
     * @throws NullPointerException если строка не задана
     */
    public ParseResult parse(String line) {
        Objects.requireNonNull(line, "строка");
        String word = normalize(line);
        if (word.isEmpty()) {
            return new ParseResult.Empty();
        }
        Direction direction = DIRECTION_WORDS.get(word);
        if (direction != null) {
            return new ParseResult.Parsed(new Command.Move(direction));
        }
        if (CommandWords.isHelp(word)) {
            return new ParseResult.Help();
        }
        if (CommandWords.isExit(word)) {
            return new ParseResult.Exit();
        }
        return new ParseResult.Unknown(word);
    }

    /**
     * Является ли строка словом выхода — с той же нормализацией, что и
     * {@link #parse}. Нужно адаптерам, которые распознают выход до полного
     * разбора команды: консоль так выходит на этапе расспроса параметров.
     *
     * @param line строка ввода
     * @return {@code true}, если строка — одно из {@link CommandWords#exitWords()}
     * @throws NullPointerException если строка не задана
     */
    public boolean isExit(String line) {
        Objects.requireNonNull(line, "строка");
        return CommandWords.isExit(normalize(line));
    }

    /**
     * Обрезает края и приводит к нижнему регистру.
     *
     * <p>Края режем не только по {@link String#strip()}: тот пропускает неразрывный
     * пробел (U+00A0), а его легко притащить копипастом из мессенджера. Внутри
     * строки символы не трогаем — «на север» остаётся неразобранным словом.
     *
     * <p>Регистр опускаем через {@link Locale#ROOT}: иначе локаль с другим правилом
     * (например, турецкая) сделала бы разбор недетерминированным.
     */
    private static String normalize(String line) {
        int from = 0;
        int to = line.length();
        while (from < to && isSpace(line.charAt(from))) {
            from++;
        }
        while (to > from && isSpace(line.charAt(to - 1))) {
            to--;
        }
        return line.substring(from, to).toLowerCase(Locale.ROOT);
    }

    /** Пробел по {@code strip()} или разрядный пробел (в том числе неразрывный). */
    private static boolean isSpace(char symbol) {
        return Character.isWhitespace(symbol) || Character.isSpaceChar(symbol);
    }

    /**
     * Таблица «слово → направление» из канонических слов и алиасов. Повтор слова
     * или пересечение со словом помощи либо выхода — ошибка контента, а не хода
     * игры, поэтому она падает при загрузке класса, а не выдаёт молча чужое
     * направление.
     */
    private static Map<String, Direction> buildDirectionWords() {
        Map<String, Direction> words = new HashMap<>();
        for (Direction direction : Direction.values()) {
            addWord(words, CommandWords.name(direction), direction);
            for (String alias : CommandWords.aliases(direction)) {
                addWord(words, alias, direction);
            }
        }
        checkNoOverlap(words, CommandWords.helpWords(), "помощи");
        checkNoOverlap(words, CommandWords.exitWords(), "выхода");
        return Map.copyOf(words);
    }

    private static void checkNoOverlap(Map<String, Direction> words, List<String> other, String what) {
        for (String word : other) {
            if (words.containsKey(word)) {
                throw new IllegalStateException("слово " + what + " пересекается с ходом: " + word);
            }
        }
    }

    private static void addWord(Map<String, Direction> words, String word, Direction direction) {
        Direction previous = words.put(word, direction);
        if (previous != null) {
            throw new IllegalStateException("слово " + word + " уже принадлежит " + previous);
        }
    }
}
