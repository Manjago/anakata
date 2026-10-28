package io.github.manjago.anakata.descriptions;

import io.github.manjago.anakata.core.landmark.Landmark;
import io.github.manjago.anakata.core.landmark.LandmarkKind;
import io.github.manjago.anakata.core.maze.Direction;
import io.github.manjago.anakata.core.room.RoomView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Превращает {@link RoomView} в русские фразы — первую вертикальную протяжку
 * «данные ядра → текст».
 *
 * <p>На вход идёт только вид комнаты: ни решётки, ни лабиринта, ни счётной схемы
 * ориентиров, ни сида. Иначе появился бы соблазн дочитать мир в обход ядра, а
 * описания должны уметь ровно то, что ядро уже посчитало.
 *
 * <p>Результат — строки в порядке показа: сначала выходы, затем фраза ориентира,
 * если он есть. Пустая комната описывается одной строкой выходов: нейтральная
 * фраза в каждой клетке противоречила бы тому, что запоминается только
 * примечательное. Склейка строк — дело адаптера.
 *
 * <p>Фразы ориентиров — контент, а не контракт сида: уровень от них не зависит,
 * их можно менять между релизами. Заморожена только форма: шесть видов по восемь
 * фраз. Полноту держит {@code switch} без {@code default} (у нового вида не
 * найдётся ветки, и сборка сломается), связь «массив ↔ вид» — имена массивов (её
 * видно глазом на ревью, но компилятор за ней не следит), длину массивов —
 * статическая проверка при загрузке класса.
 */
public final class RoomDescriber {

    /** Фразы предметов: элемент на вариант, индекс — номер варианта. */
    private static final String[] OBJECT_PHRASES = {
            "В полу вырублена каменная чаша.",
            "В углу громоздится сломанный ткацкий станок.",
            "Высоко под потолком висит обрывок цепи.",
            "Посреди комнаты высится статуя без головы.",
            "У стены стоит телега без колёс.",
            "Стоит пустой каменный трон.",
            "В углу стоит массивная наковальня.",
            "Поперёк комнаты лежит поваленная колонна.",
    };

    /** Фразы узоров на стенах: элемент на вариант, индекс — номер варианта. */
    private static final String[] WALL_PATTERN_PHRASES = {
            "На стене выцарапана спираль.",
            "Стены покрыты сеткой тонких трещин.",
            "Вдоль стен тянется полоса синей глазури.",
            "Стены расписаны бледными силуэтами птиц.",
            "На стене проступает грубый отпечаток ладони.",
            "Кирпичи уложены внахлёст, как чешуя.",
            "Стены исчерчены ровными рядами насечек.",
            "По стенам вьётся вырезанная из камня лоза.",
    };

    /** Фразы текстур пола: элемент на вариант, индекс — номер варианта. */
    private static final String[] FLOOR_TEXTURE_PHRASES = {
            "Пол выложен мозаикой.",
            "Под ногами скрипит мелкий песок.",
            "Пол — гладкие плиты розового мрамора.",
            "Пол вымощен крупной речной галькой.",
            "Доски пола рассохлись и ходят под ногами.",
            "Пол покрыт мягким мхом.",
            "Пол устлан колючей соломой.",
            "Пол — сплошное чёрное стекло.",
    };

    /** Фразы освещения: элемент на вариант, индекс — номер варианта. */
    private static final String[] LIGHT_PHRASES = {
            "С потолка льётся зелёный свет.",
            "В углу мерцает одинокая свеча.",
            "Комнату заливает резкий белый свет.",
            "Свет сочится, будто из самих стен.",
            "Комнату наполняет тусклый янтарный свет.",
            "Комнату прошивают редкие синие искры.",
            "Царит густой красноватый полумрак.",
            "Свет медленно пульсирует, то ярче, то тусклее.",
    };

    /** Фразы звуков: элемент на вариант, индекс — номер варианта. */
    private static final String[] SOUND_PHRASES = {
            "Где-то капает вода.",
            "Слышно, как гудит ветер.",
            "Слышно, как потрескивают угли.",
            "В тишине звенит одинокая струна.",
            "Слышно стрекотание сверчка.",
            "Где-то мерно стучит маятник.",
            "Стены отзываются глухим рокотом.",
            "Издалека доносится колокольный звон.",
    };

    /** Фразы запахов: элемент на вариант, индекс — номер варианта. */
    private static final String[] SMELL_PHRASES = {
            "Пахнет грибами.",
            "Тянет горьким дымом.",
            "Пахнет старой бумагой.",
            "В воздухе висит запах ладана.",
            "Пахнет полынью.",
            "Откуда-то тянет солью и морем.",
            "Пахнет ржавым железом.",
            "Пахнет цветущей липой.",
    };

    static {
        checkLength(OBJECT_PHRASES, LandmarkKind.OBJECT);
        checkLength(WALL_PATTERN_PHRASES, LandmarkKind.WALL_PATTERN);
        checkLength(FLOOR_TEXTURE_PHRASES, LandmarkKind.FLOOR_TEXTURE);
        checkLength(LIGHT_PHRASES, LandmarkKind.LIGHT);
        checkLength(SOUND_PHRASES, LandmarkKind.SOUND);
        checkLength(SMELL_PHRASES, LandmarkKind.SMELL);
    }

    /**
     * Описывает комнату строками в порядке показа: выходы, затем ориентир, если он
     * есть.
     *
     * @param view вид комнаты
     * @return одна или две строки
     */
    public List<String> describe(RoomView view) {
        Objects.requireNonNull(view, "комната");
        List<String> lines = new ArrayList<>(2);
        lines.add(exitsLine(view.exits()));
        Landmark landmark = view.landmark();
        if (landmark != null) {
            lines.add(phrases(landmark.kind())[landmark.variant()]);
        }
        return List.copyOf(lines);
    }

    /**
     * Фразы вида. {@code switch} без {@code default} держит полноту: у нового
     * вида не найдётся ветки, и сборка сломается. Связь «массив ↔ вид» — на
     * именах массивов, её проверяет глаз, а не компилятор.
     */
    private static String[] phrases(LandmarkKind kind) {
        return switch (kind) {
            case OBJECT -> OBJECT_PHRASES;
            case WALL_PATTERN -> WALL_PATTERN_PHRASES;
            case FLOOR_TEXTURE -> FLOOR_TEXTURE_PHRASES;
            case LIGHT -> LIGHT_PHRASES;
            case SOUND -> SOUND_PHRASES;
            case SMELL -> SMELL_PHRASES;
        };
    }

    /**
     * Строка выходов: классификация по числу выходов и их перечисление по-русски.
     * Управляет текстом именно число, а не геометрия: два выхода — коридор, даже
     * если он поворачивает.
     */
    private static String exitsLine(List<Direction> exits) {
        StringBuilder builder = new StringBuilder();
        builder.append(classification(exits.size())).append(". ");
        builder.append(exits.size() == 1 ? "Выход: " : "Выходы: ");
        for (int i = 0; i < exits.size(); i++) {
            if (i > 0) {
                builder.append(i == exits.size() - 1 ? " и " : ", ");
            }
            builder.append(CommandWords.name(exits.get(i)));
        }
        return builder.append('.').toString();
    }

    /** Один выход — тупик, два — коридор, три и больше — развилка. */
    private static String classification(int exits) {
        if (exits == 1) {
            return "Тупик";
        }
        return exits == 2 ? "Коридор" : "Развилка";
    }

    /** Форма таблицы — часть обещания DESIGN: у каждого вида ровно {@code VARIANTS} фраз. */
    private static void checkLength(String[] phrases, LandmarkKind kind) {
        if (phrases.length != LandmarkKind.VARIANTS) {
            throw new IllegalStateException(
                    "у вида " + kind + " " + phrases.length + " фраз вместо " + LandmarkKind.VARIANTS);
        }
    }
}
