package io.github.manjago.anakata.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.manjago.anakata.core.maze.Direction;
import io.github.manjago.anakata.descriptions.CommandWords;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class AdminParserTest {

    private final AdminParser parser = new AdminParser();

    @Test
    void readsSeedWordAndAlias() {
        assertEquals(parsed(new AdminCommand.Seed()), parser.parse("сид"));
        assertEquals(parsed(new AdminCommand.Seed()), parser.parse("seed"));
    }

    @Test
    void readsCoordinatesWordAndAlias() {
        assertEquals(parsed(new AdminCommand.Coordinates()), parser.parse("координаты"));
        assertEquals(parsed(new AdminCommand.Coordinates()), parser.parse("coords"));
    }

    @Test
    void readsStatsWordAndAlias() {
        assertEquals(parsed(new AdminCommand.Stats()), parser.parse("статистика"));
        assertEquals(parsed(new AdminCommand.Stats()), parser.parse("stats"));
    }

    @Test
    void readsTeleportWithCoordinates() {
        assertEquals(parsed(new AdminCommand.Teleport(List.of(1, 2))), parser.parse("телепорт 1 2"));
        assertEquals(parsed(new AdminCommand.Teleport(List.of(3, 4, 5))), parser.parse("tp 3 4 5"));
    }

    /** Голова команды — «новый»; хвост «уровень» необязателен, как и сид. */
    @Test
    void readsNewLevelVariants() {
        AdminCommand.NewLevel random = new AdminCommand.NewLevel(OptionalLong.empty());
        assertEquals(parsed(random), parser.parse("новый уровень"));
        assertEquals(parsed(random), parser.parse("новый"));
        assertEquals(parsed(random), parser.parse("new"));

        AdminCommand.NewLevel seeded = new AdminCommand.NewLevel(OptionalLong.of(42L));
        assertEquals(parsed(seeded), parser.parse("новый уровень 42"));
        assertEquals(parsed(seeded), parser.parse("новый 42"));
        assertEquals(parsed(seeded), parser.parse("new 42"));
    }

    /** Строка нормализуется как язык мира: края, регистр, неразрывный пробел. */
    @Test
    void normalizesLikeTheWorldLanguage() {
        assertEquals(parsed(new AdminCommand.Seed()), parser.parse("  СИД  "));
        assertEquals(parsed(new AdminCommand.Seed()), parser.parse("\u00A0сид\u00A0"));
    }

    /**
     * Без координат команда всё равно разбирается в телепорт с пустым списком:
     * число координат сверяет адаптер, знающий размерность уровня, — он и печатает
     * точную подсказку. Парсер про размерность не знает.
     */
    @Test
    void teleportWithoutCoordinatesIsAnEmptyTeleport() {
        assertEquals(parsed(new AdminCommand.Teleport(List.of())), parser.parse("телепорт"));
        assertEquals(parsed(new AdminCommand.Teleport(List.of())), parser.parse("tp"));
    }

    @Test
    void teleportWithNonNumericCoordinateIsAHint() {
        assertEquals(new AdminParser.Result.Invalid(AdminText.TELEPORT_HINT),
                parser.parse("телепорт 1 верх"));
    }

    @Test
    void newLevelWithNonNumericSeedIsASeedHint() {
        assertEquals(new AdminParser.Result.Invalid(ConsoleText.SEED_HINT),
                parser.parse("новый уровень abc"));
    }

    @Test
    void newLevelWithExtraArgumentsIsAHint() {
        assertEquals(new AdminParser.Result.Invalid(AdminText.NEW_LEVEL_HINT),
                parser.parse("новый уровень 1 2"));
    }

    /** У команд без аргументов лишний аргумент — ошибка, а не молчание. */
    @Test
    void commandsWithoutArgumentsRejectExtraWords() {
        assertEquals(new AdminParser.Result.Invalid(AdminText.SEED_READONLY_HINT), parser.parse("сид 42"));
        assertEquals(new AdminParser.Result.Invalid(AdminText.COORDINATES_NO_ARGS_HINT),
                parser.parse("координаты тут"));
        assertEquals(new AdminParser.Result.Invalid(AdminText.STATS_NO_ARGS_HINT),
                parser.parse("статистика x"));
    }

    /**
     * Админка разбирается раньше мира и молча перекрыла бы мировое слово. Поэтому
     * головы админки (у «новый уровень» голова — «новый») не должны пересекаться
     * со словами {@link CommandWords}: направлениями, помощью и выходом; и внутри
     * самой админки дублей быть не должно.
     */
    @Test
    void adminHeadWordsDoNotCollideWithWorldWords() {
        List<String> heads = AdminParser.headWords();

        assertEquals(heads.size(), new HashSet<>(heads).size(), "дубли в админ-словах: " + heads);
        for (String head : heads) {
            assertFalse(worldWords().contains(head),
                    "админ-слово перекрывает мировое: " + head);
        }
    }

    private static List<String> worldWords() {
        List<String> words = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            words.add(CommandWords.name(direction));
            words.addAll(CommandWords.aliases(direction));
        }
        words.addAll(CommandWords.helpWords());
        words.addAll(CommandWords.exitWords());
        return words;
    }

    /** Всё, что не админка, уходит в обычный разбор без изменений. */
    @Test
    void nonAdminLinesAreNotAdmin() {
        assertEquals(new AdminParser.Result.NotAdmin(), parser.parse("север"));
        assertEquals(new AdminParser.Result.NotAdmin(), parser.parse("помощь"));
        assertEquals(new AdminParser.Result.NotAdmin(), parser.parse("абракадабра"));
        assertEquals(new AdminParser.Result.NotAdmin(), parser.parse(""));
        assertEquals(new AdminParser.Result.NotAdmin(), parser.parse("   "));
    }

    /** Слово, лишь начинающееся с админского, админкой не считается. */
    @Test
    void wordMerelyStartingLikeAdminIsNotAdmin() {
        assertEquals(new AdminParser.Result.NotAdmin(), parser.parse("телепортер"));
        assertEquals(new AdminParser.Result.NotAdmin(), parser.parse("статистикал"));
    }

    @Test
    void rejectsNullLine() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }

    private static AdminParser.Result parsed(AdminCommand command) {
        return new AdminParser.Result.Parsed(command);
    }
}
