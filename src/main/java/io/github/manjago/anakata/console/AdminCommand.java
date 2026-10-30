package io.github.manjago.anakata.console;

import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Админ-команда консоли — то, что {@link AdminParser} разобрал из строки.
 *
 * <p>Админ-команды — инструмент администратора, а не ход игрока: в ядро они не
 * уходят (см. DESIGN.md, «Уровень и игровой цикл»). Ядру админка незнакома и по
 * словам, и по смыслу; {@link #Teleport} и {@link #NewLevel} лишь пользуются
 * возможностями ядра — {@code GameSession.teleport} и пересборкой {@code Level}.
 *
 * <p>Хранимые координаты — список, а не массив: {@code record} с массивом сломал
 * бы {@code equals} и {@code hashCode}. Список неизменяемый.
 */
public sealed interface AdminCommand {

    /** Показать сид текущего уровня. */
    record Seed() implements AdminCommand {
    }

    /** Переключить показ координат клетки. */
    record Coordinates() implements AdminCommand {
    }

    /**
     * Перенести игрока в клетку по координатам.
     *
     * @param coordinates координаты по осям, длина сверяется с размерностью уровня
     */
    record Teleport(List<Integer> coordinates) implements AdminCommand {

        /** @throws NullPointerException если список или координата не заданы */
        public Teleport {
            Objects.requireNonNull(coordinates, "координаты");
            for (Integer coordinate : coordinates) {
                Objects.requireNonNull(coordinate, "координата");
            }
            coordinates = List.copyOf(coordinates);
        }
    }

    /**
     * Пересобрать уровень с теми же размерами и плотностью.
     *
     * @param seed сид нового уровня; пусто — взять случайный
     */
    record NewLevel(OptionalLong seed) implements AdminCommand {

        /** @throws NullPointerException если сид не задан */
        public NewLevel {
            Objects.requireNonNull(seed, "сид");
        }
    }

    /** Показать сводку по уровню и лабиринту. */
    record Stats() implements AdminCommand {
    }
}
