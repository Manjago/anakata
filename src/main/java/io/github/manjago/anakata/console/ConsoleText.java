package io.github.manjago.anakata.console;

import io.github.manjago.anakata.core.level.Level;
import io.github.manjago.anakata.core.level.LevelParams;
import io.github.manjago.anakata.descriptions.CommandWords;
import java.util.List;

/**
 * Русские строки консольного адаптера — хром взаимодействия, а не язык мира.
 *
 * <p>Здесь только то, что специфично для консоли: шапка со шпаргалкой, приглашение,
 * ответ на непонятый ввод, прощание, подсказки к полям ввода. Описания мира и
 * команд (комната, ориентир, исход хода, слова хода и помощи) живут в пакете
 * {@code descriptions}: мир должен звучать одинаково во всех адаптерах, а хром — у
 * каждого свой (см. DESIGN.md, «Архитектура»).
 *
 * <p>Строки — контент, а не контракт: их можно менять между релизами.
 */
public final class ConsoleText {

    /** Приглашение к вводу команды. */
    public static final String PROMPT = "> ";

    /** Приглашение ввести сид: без числового дефолта, поэтому константа. */
    public static final String ASK_SEED = "Сид (пусто — случайный): ";

    /**
     * Приглашение ввести размеры. Дефолт подставляет вызывающий, поэтому значение
     * живёт в одном месте и не разъезжается с приглашением.
     *
     * @param defaultSizes размеры по умолчанию
     * @return строка приглашения
     */
    public static String askSizes(int[] defaultSizes) {
        return "Размеры по осям (пусто — " + numbers(defaultSizes) + "): ";
    }

    /**
     * Приглашение ввести плотность ориентиров. Дефолт тоже подставляет
     * вызывающий.
     *
     * @param defaultDensity плотность по умолчанию
     * @return строка приглашения
     */
    public static String askDensity(int defaultDensity) {
        return "Ориентиры — примерно одна приметная комната раз в N (пусто — "
                + defaultDensity + "): ";
    }

    /**
     * Числа через пробел: «6 6» или координаты «1 2 3».
     *
     * <p>Единая склейка для размеров, координат и их подсказок: свой цикл в каждом
     * месте разъехался бы (пробелы, порядок). Разделитель — обычный пробел;
     * скобки, если нужны, добавляет вызывающий.
     *
     * @param values числа
     * @return строка чисел через пробел
     */
    static String numbers(int[] values) {
        StringBuilder text = new StringBuilder();
        for (int value : values) {
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(value);
        }
        return text.toString();
    }

    /** Примеры формата размеров для подсказок. */
    public static final String SIZES_EXAMPLES = "6 6, 5 5 5, 4 4 4 4";

    /** Формат размеров — общей частью всех подсказок о них. */
    public static final String SIZES_HINT =
            "Нужно от 2 до 4 чисел через пробел, каждое не меньше 2: " + SIZES_EXAMPLES + ".";

    /** Подсказка после непонятого сида. */
    public static final String SEED_HINT = "Не понял сид. Нужно целое число.";

    /** Подсказка после непонятой плотности. */
    public static final String DENSITY_HINT =
            "Не понял плотность. Нужно целое число не меньше 1.";

    private ConsoleText() {
    }

    /**
     * Шапка и шпаргалка: что за игра и как задавать параметры. Слова помощи и
     * выхода берутся из {@link CommandWords}, а не набираются руками: подсказка не
     * должна разъезжаться с разбором (см. DESIGN.md, «Разбор строки»).
     */
    public static List<String> cheatSheet() {
        return List.of(
                "anakata — консоль администратора.",
                "Размерность — это число чисел: " + SIZES_EXAMPLES
                        + " (2D, 3D, 4D по числу чисел).",
                "Ориентир — редкая примета комнаты (предмет, звук, запах…);"
                        + " по ней узнаёшь место. Пустые комнаты не описываются.",
                "Пустое поле — значение по умолчанию. В любой момент — "
                        + word(CommandWords.EXIT_NAME, CommandWords.exitAliases())
                        + "; в игре — "
                        + word(CommandWords.HELP_NAME, CommandWords.helpAliases())
                        + " и ходы.");
    }

    /** Слово с алиасами скобкой: «выход (exit, q)». */
    private static String word(String name, List<String> aliases) {
        return name + " (" + String.join(", ", aliases) + ")";
    }

    /** Ответ на нечисловые размеры — в едином ключе с другими ошибками размеров. */
    public static String sizesNotNumbers() {
        return "Не понял размеры. " + SIZES_HINT;
    }

    /** Ответ на размеры, которые не построить: причина от {@code Grid} и формат. */
    public static String sizesProblem(String reason) {
        return "Не понял размеры: " + reason + ". " + SIZES_HINT;
    }

    /**
     * Шапка собранного уровня: сид печатаем, чтобы им можно было поделиться, а
     * число приметных комнат — чтобы плотность не выглядела пустой абстракцией:
     * при редких ориентирах на маленьком уровне их легко не встретить вовсе.
     */
    public static String level(Level level) {
        LevelParams params = level.params();
        int[] sizes = params.sizes();
        StringBuilder dimensions = new StringBuilder();
        for (int axis = 0; axis < sizes.length; axis++) {
            if (axis > 0) {
                dimensions.append('×');
            }
            dimensions.append(sizes[axis]);
        }
        return "Уровень: " + dimensions + ", сид " + params.seed()
                + ". Ориентиры: примерно одна приметная комната раз в " + params.landmarkInEvery()
                + " (всего приметных: " + level.landmarkCount() + ").";
    }

    /** Ответ на слово не из таблицы. {@code word} уже нормализован парсером. */
    public static String unknown(String word) {
        return "Не понял: " + word + ".";
    }

    /** Прощание при выходе или конце ввода. */
    public static String farewell() {
        return "До встречи.";
    }
}
