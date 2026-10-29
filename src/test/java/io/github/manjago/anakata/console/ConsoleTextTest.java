package io.github.manjago.anakata.console;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.manjago.anakata.core.level.Level;
import io.github.manjago.anakata.core.level.LevelParams;
import io.github.manjago.anakata.descriptions.CommandWords;
import org.junit.jupiter.api.Test;

class ConsoleTextTest {

    /** Приглашения печатают переданный дефолт — число и размеры в одном месте. */
    @Test
    void promptsShowTheGivenDefaults() {
        assertTrue(ConsoleText.askDensity(7).contains("7"), ConsoleText.askDensity(7));
        assertTrue(ConsoleText.askSizes(new int[] {4, 5}).contains("4 5"),
                ConsoleText.askSizes(new int[] {4, 5}));
    }

    /** Шпаргалка берёт слова помощи и выхода из таблицы, а не набирает руками. */
    @Test
    void cheatSheetNamesHelpAndExitFromCommandWords() {
        String sheet = String.join("\n", ConsoleText.cheatSheet());

        assertTrue(sheet.contains(CommandWords.HELP_NAME), sheet);
        assertTrue(sheet.contains(CommandWords.EXIT_NAME), sheet);
        for (String alias : CommandWords.helpAliases()) {
            assertTrue(sheet.contains(alias), "нет алиаса помощи " + alias + " в " + sheet);
        }
        for (String alias : CommandWords.exitAliases()) {
            assertTrue(sheet.contains(alias), "нет алиаса выхода " + alias + " в " + sheet);
        }
    }

    /** Шапка несёт сид (чтобы поделиться уровнем) и настоящее число приметных комнат. */
    @Test
    void levelHeaderTellsSeedAndActualLandmarkCount() {
        Level level = Level.build(new LevelParams(1L, new int[] {4, 4}, 4));

        String header = ConsoleText.level(level);

        assertTrue(header.contains("4×4"), header);
        assertTrue(header.contains("сид 1"), header);
        assertTrue(header.contains("раз в 4"), header);
        assertTrue(header.contains("всего приметных: " + landmarks(level)), header);
    }

    private static int landmarks(Level level) {
        int count = 0;
        for (int cell = 0; cell < level.grid().cells(); cell++) {
            if (level.roomView(cell).landmark() != null) {
                count++;
            }
        }
        return count;
    }
}
