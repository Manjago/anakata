package io.github.manjago.anakata.console;

import io.github.manjago.anakata.descriptions.CommandParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Разбирает строку консоли в админ-команду.
 *
 * <p>Админ-команды — инструмент администратора, поэтому живут в консоли, а не в
 * общем пакете описаний: мир должен звучать одинаково во всех адаптерах, а
 * отладочные прыжки и пересборка уровня — забота консоли (см. DESIGN.md, «Уровень
 * и игровой цикл»). Ядро про эти слова не знает.
 *
 * <p>Разбор идёт раньше {@link CommandParser}: не-админские строки возвращаются
 * как {@link Result.NotAdmin} и уходят в обычный разбор без изменений. Строка
 * нормализуется тем же {@link CommandParser#normalize}, что и язык мира: одна
 * нормализация на всех, чтобы разбор не разъехался. Первое слово —
 * голова команды, остальные — аргументы; лишние аргументы у команд без них —
 * ошибка, а не молчаливое проглатывание.
 */
public final class AdminParser {

    static final String SEED_NAME = "сид";
    static final List<String> SEED_ALIASES = List.of("seed");

    static final String COORDINATES_NAME = "координаты";
    static final List<String> COORDINATES_ALIASES = List.of("coords");

    static final String TELEPORT_NAME = "телепорт";
    static final List<String> TELEPORT_ALIASES = List.of("tp");

    /** Канон — фраза из двух слов; хвост «уровень» после головы необязателен. */
    static final String NEW_LEVEL_NAME = "новый уровень";
    static final List<String> NEW_LEVEL_ALIASES = List.of("new");

    static final String STATS_NAME = "статистика";
    static final List<String> STATS_ALIASES = List.of("stats");

    /**
     * Что оказалось на входе: админ-команда, ошибка в аргументах или строка не
     * про админку.
     */
    public sealed interface Result permits Result.NotAdmin, Result.Parsed, Result.Invalid {

        /** Строка не админская — разбирать дальше обычным ходом. */
        record NotAdmin() implements Result {
        }

        /** Разобранная админ-команда. */
        record Parsed(AdminCommand command) implements Result {

            /** @throws NullPointerException если команда не задана */
            public Parsed {
                Objects.requireNonNull(command, "команда");
            }
        }

        /** Админское слово узнано, но аргументы не годятся; несёт подсказку. */
        record Invalid(String message) implements Result {

            /** @throws NullPointerException если сообщение не задано */
            public Invalid {
                Objects.requireNonNull(message, "сообщение");
            }
        }
    }

    /** Каноническое слово и алиасы — для текста помощи ({@link AdminText}). */
    static List<String> seedWords() {
        return words(SEED_NAME, SEED_ALIASES);
    }

    /** @see #seedWords() */
    static List<String> coordinatesWords() {
        return words(COORDINATES_NAME, COORDINATES_ALIASES);
    }

    /** @see #seedWords() */
    static List<String> teleportWords() {
        return words(TELEPORT_NAME, TELEPORT_ALIASES);
    }

    /** @see #seedWords() */
    static List<String> newLevelWords() {
        return words(NEW_LEVEL_NAME, NEW_LEVEL_ALIASES);
    }

    /** @see #seedWords() */
    static List<String> statsWords() {
        return words(STATS_NAME, STATS_ALIASES);
    }

    /**
     * Все слова-головы, по которым {@link #parse} узнаёт админку: канонические
     * головы и алиасы. Нужны тесту на коллизии со словами мира — перекрытие молча
     * съело бы мировое слово, потому что админка разбирается первой.
     */
    static List<String> headWords() {
        List<String> heads = new ArrayList<>();
        heads.addAll(heads(SEED_NAME, SEED_ALIASES));
        heads.addAll(heads(COORDINATES_NAME, COORDINATES_ALIASES));
        heads.addAll(heads(TELEPORT_NAME, TELEPORT_ALIASES));
        heads.addAll(heads(NEW_LEVEL_NAME, NEW_LEVEL_ALIASES));
        heads.addAll(heads(STATS_NAME, STATS_ALIASES));
        return List.copyOf(heads);
    }

    private static List<String> heads(String name, List<String> aliases) {
        List<String> heads = new ArrayList<>(1 + aliases.size());
        heads.add(headOf(name));
        heads.addAll(aliases);
        return heads;
    }

    private static List<String> words(String name, List<String> aliases) {
        List<String> words = new ArrayList<>(1 + aliases.size());
        words.add(name);
        words.addAll(aliases);
        return List.copyOf(words);
    }

    /**
     * Разбирает строку консоли в админ-команду.
     *
     * @param line строка ввода
     * @return результат разбора
     * @throws NullPointerException если строка не задана
     */
    public Result parse(String line) {
        Objects.requireNonNull(line, "строка");
        String normalized = CommandParser.normalize(line);
        if (normalized.isEmpty()) {
            return new Result.NotAdmin();
        }
        String[] tokens = normalized.split("\\s+");
        String head = tokens[0];
        if (matchesHead(head, TELEPORT_NAME, TELEPORT_ALIASES)) {
            return parseTeleport(tokens);
        }
        if (matchesHead(head, NEW_LEVEL_NAME, NEW_LEVEL_ALIASES)) {
            return parseNewLevel(tokens);
        }
        if (matchesHead(head, SEED_NAME, SEED_ALIASES)) {
            return withoutArguments(tokens, new AdminCommand.Seed(), AdminText.SEED_READONLY_HINT);
        }
        if (matchesHead(head, COORDINATES_NAME, COORDINATES_ALIASES)) {
            return withoutArguments(tokens, new AdminCommand.Coordinates(),
                    AdminText.COORDINATES_NO_ARGS_HINT);
        }
        if (matchesHead(head, STATS_NAME, STATS_ALIASES)) {
            return withoutArguments(tokens, new AdminCommand.Stats(), AdminText.STATS_NO_ARGS_HINT);
        }
        return new Result.NotAdmin();
    }

    /**
     * Координаты телепорта: числа после головы. Их число не сверяем — размерность
     * уровня парсеру неизвестна, поэтому счёт и точную подсказку держит адаптер
     * ({@link ConsoleGame}), а здесь остаются только разбор чисел и общая ошибка.
     */
    private static Result parseTeleport(String[] tokens) {
        List<Integer> coordinates = new ArrayList<>(tokens.length - 1);
        for (int index = 1; index < tokens.length; index++) {
            try {
                coordinates.add(Integer.parseInt(tokens[index]));
            } catch (NumberFormatException notANumber) {
                return new Result.Invalid(AdminText.TELEPORT_HINT);
            }
        }
        return new Result.Parsed(new AdminCommand.Teleport(coordinates));
    }

    private static Result parseNewLevel(String[] tokens) {
        int index = 1;
        String[] name = NEW_LEVEL_NAME.split(" ");
        for (int word = 1; word < name.length && index < tokens.length
                && name[word].equals(tokens[index]); word++) {
            index++;
        }
        if (index == tokens.length) {
            return new Result.Parsed(new AdminCommand.NewLevel(OptionalLong.empty()));
        }
        if (index + 1 == tokens.length) {
            try {
                long seed = Long.parseLong(tokens[index]);
                return new Result.Parsed(new AdminCommand.NewLevel(OptionalLong.of(seed)));
            } catch (NumberFormatException notANumber) {
                return new Result.Invalid(ConsoleText.SEED_HINT);
            }
        }
        return new Result.Invalid(AdminText.NEW_LEVEL_HINT);
    }

    private static Result withoutArguments(String[] tokens, AdminCommand command, String hint) {
        if (tokens.length > 1) {
            return new Result.Invalid(hint);
        }
        return new Result.Parsed(command);
    }

    /** Совпадает ли первое слово с головой канона или с алиасом. */
    private static boolean matchesHead(String token, String name, List<String> aliases) {
        return token.equals(headOf(name)) || aliases.contains(token);
    }

    /** Голова канона — первое слово: у «новый уровень» это «новый». */
    private static String headOf(String name) {
        return name.split(" ", 2)[0];
    }
}
