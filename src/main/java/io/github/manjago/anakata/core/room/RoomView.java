package io.github.manjago.anakata.core.room;

import io.github.manjago.anakata.core.landmark.Landmark;
import io.github.manjago.anakata.core.maze.Direction;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Вид комнаты глазами игрока: выходы и, иногда, ориентир.
 *
 * <p>Это данные ядра, а не строка: русские фразы из них делает пакет описаний.
 * Координаты клетки игроку не показываются, поэтому их здесь нет.
 *
 * <p>Выходы — список, а не массив: {@code record} с массивом сломал бы
 * {@code equals} и {@code hashCode} и оставил бы изменяемый компонент. Выходы по
 * смыслу — множество, а не последовательность, поэтому в конструкторе они
 * приводятся к каноническому виду: дубли отклоняются, а список сортируется по
 * порядку объявления {@link Direction} (тому же, что отдаёт {@code Maze.exits}).
 * Один и тот же набор выходов всегда даёт равные виды комнаты и одинаковый текст.
 *
 * <p>{@code landmark} может быть {@code null}: большинство комнат пусты.
 */
public record RoomView(List<Direction> exits, Landmark landmark) {

    /**
     * @throws NullPointerException     если список выходов или какой-то выход не задан
     * @throws IllegalArgumentException если у комнаты нет выходов или выход повторяется
     */
    public RoomView {
        Objects.requireNonNull(exits, "выходы");
        if (exits.isEmpty()) {
            // У связного лабиринта клетка всегда имеет хотя бы один выход,
            // поэтому пустой список — ошибка вызывающего, а не штатный случай.
            throw new IllegalArgumentException("у комнаты нет выходов");
        }
        List<Direction> canonical = new ArrayList<>(exits);
        for (Direction exit : canonical) {
            Objects.requireNonNull(exit, "выход");
        }
        canonical.sort(Comparator.comparingInt(Direction::ordinal));
        for (int i = 1; i < canonical.size(); i++) {
            if (canonical.get(i) == canonical.get(i - 1)) {
                throw new IllegalArgumentException("повтор выхода: " + canonical.get(i));
            }
        }
        exits = List.copyOf(canonical);
    }
}
