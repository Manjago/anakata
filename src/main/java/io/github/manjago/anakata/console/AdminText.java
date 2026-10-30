package io.github.manjago.anakata.console;

import io.github.manjago.anakata.core.level.Level;
import io.github.manjago.anakata.core.maze.Grid;
import java.util.List;

/**
 * Русские строки админ-команд консоли — хром администратора, а не язык мира.
 *
 * <p>Админка консольная (см. DESIGN.md, «Уровень и игровой цикл»), поэтому её
 * тексты рядом с {@link ConsoleText}, а не в общем пакете описаний: мир должен
 * звучать одинаково во всех адаптерах, а отладочный инструмент — свой у каждого.
 * Слова команд берём у {@link AdminParser}, чтобы подсказка не разъехалась с
 * разбором.
 *
 * <p>Строки — контент, а не контракт: их можно менять между релизами.
 */
public final class AdminText {

    /** Общая подсказка, когда координаты телепорта не числа; точную форму даёт {@link #teleportHint}. */
    public static final String TELEPORT_HINT =
            "Не понял координаты телепорта. Нужны целые числа через пробел.";

    /** Подсказка после лишнего аргумента у пересборки. */
    public static final String NEW_LEVEL_HINT =
            "«новый уровень [сид]» — размеры и плотность те же, сид необязателен.";

    /** Напоминание, что сид только показывается, а задаётся пересборкой. */
    public static final String SEED_READONLY_HINT =
            "Сид только показывается. Задать новый — «новый уровень <сид>».";

    /** Подсказка после лишнего аргумента у показа координат. */
    public static final String COORDINATES_NO_ARGS_HINT = "«координаты» — без аргументов.";

    /** Подсказка после лишнего аргумента у статистики. */
    public static final String STATS_NO_ARGS_HINT = "«статистика» — без аргументов.";

    /** Ответ на выключение показа координат. */
    public static final String COORDINATES_HIDDEN = "Показ координат выключен.";

    private AdminText() {
    }

    /** Строки админки для общей помощи в игре; дописываются после слов мира. */
    public static List<String> help() {
        return List.of(
                "Админ: " + word(AdminParser.seedWords()) + " — показать сид; "
                        + word(AdminParser.coordinatesWords()) + " — показывать координаты клетки.",
                word(AdminParser.newLevelWords()) + " [сид] — пересобрать уровень; "
                        + word(AdminParser.teleportWords()) + " x y … — прыгнуть в клетку.",
                word(AdminParser.statsWords()) + " — сводка по уровню и лабиринту.");
    }

    /** Слово с алиасами скобкой: «телепорт (tp)». */
    private static String word(List<String> words) {
        if (words.size() == 1) {
            return words.get(0);
        }
        return words.get(0) + " (" + String.join(", ", words.subList(1, words.size())) + ")";
    }

    /** Ответ на команду «сид»: сам сид, чтобы им можно было поделиться. */
    public static String seed(long seed) {
        return "Сид: " + seed;
    }

    /**
     * Подсказка, когда координат телепорта не столько, сколько измерений.
     *
     * <p>Строится из самой решётки: сколько чисел ждём, столько и печатаем, а
     * пример — по одному числу на ось, не выходящее за размер. Захардкоженный
     * диапазон разъехался бы с уровнем, а пример «номер оси» на оси длиной 2
     * предлагал бы несуществующую клетку.
     *
     * @param grid решётка уровня
     * @return строка подсказки
     */
    public static String teleportHint(Grid grid) {
        int dimensions = grid.dimensions();
        int[] example = new int[dimensions];
        for (int axis = 0; axis < dimensions; axis++) {
            example[axis] = Math.min(axis, grid.size(axis) - 1);
        }
        return "Телепорт: нужно " + dimensions + " " + numberWord(dimensions)
                + " — по числу измерений, например «телепорт " + ConsoleText.numbers(example)
                + "».";
    }

    /** Честное склонение «число/числа/чисел» для счётного слова. */
    static String numberWord(int count) {
        int lastTwo = count % 100;
        if (lastTwo >= 11 && lastTwo <= 14) {
            return "чисел";
        }
        return switch (count % 10) {
            case 1 -> "число";
            case 2, 3, 4 -> "числа";
            default -> "чисел";
        };
    }

    /**
     * Строка координат клетки — вектор в скобках: {@code [1 2 3]}.
     *
     * <p>Скобки отличают служебную приписку от текста комнаты, а форма вектора
     * повторяет {@code int[N]} из ядра: у клетки ровно столько чисел, сколько
     * измерений.
     *
     * @param coordinates координаты по осям
     * @return строка вида {@code [1 2 3]}
     */
    public static String coordinates(int[] coordinates) {
        return "[" + ConsoleText.numbers(coordinates) + "]";
    }

    /** Ответ на непонятые координаты телепорта: причина от {@code Grid}. */
    public static String teleportProblem(String reason) {
        return "Телепорт: " + reason + ".";
    }

    /**
     * Сводка по уровню и лабиринту: размеры, клетки, ориентиры и разбор клеток по
     * числу выходов. Графа нет — считаем по видам комнат через тот же
     * {@code Level.roomView}, что показывает игру, поэтому числа всегда согласованы.
     *
     * <p>Нужна администратору для калибровки плотности и характера лабиринта
     * (см. DESIGN.md, «Открытые вопросы»): у идеального дерева проходов ровно на
     * один меньше числа клеток, а доля тупиков отличает извилистый бэктрекер от
     * «много коротких тупиков» Прима.
     *
     * @param level уровень
     * @return строки сводки
     */
    public static List<String> stats(Level level) {
        int deadEnds = 0;
        int corridors = 0;
        int junctions = 0;
        int exitBits = 0;
        for (int cell = 0; cell < level.grid().cells(); cell++) {
            int exits = level.roomView(cell).exits().size();
            exitBits += exits;
            if (exits == 1) {
                deadEnds++;
            } else if (exits == 2) {
                corridors++;
            } else {
                // 0 выходов не бывает — RoomView запрещает комнату без выходов,
                // так что сюда попадают только развилки.
                junctions++;
            }
        }
        return List.of(
                ConsoleText.level(level),
                "Лабиринт: проходов " + exitBits / 2 + ", тупиков " + deadEnds
                        + ", коридоров " + corridors + ", развилок " + junctions + ".");
    }
}
