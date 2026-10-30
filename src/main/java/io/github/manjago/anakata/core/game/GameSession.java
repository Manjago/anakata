package io.github.manjago.anakata.core.game;

import io.github.manjago.anakata.core.level.Level;
import io.github.manjago.anakata.core.maze.Direction;
import io.github.manjago.anakata.core.room.RoomView;
import java.util.List;
import java.util.Objects;

/**
 * Состояние хода в ядре: уровень и изменяемая часть — текущая клетка.
 *
 * <p>Мутабельность чистоте ядра не противоречит: ядру запрещены ввод-вывод, часы
 * и случайность, а не изменяемость. Сессия при этом детерминирована: одна и та же
 * последовательность команд над одним уровнем даёт одну и ту же клетку.
 *
 * <p>Позже здесь же поселятся собранные буквы, счётчик ошибок и cooldown — ровно
 * те поля, что войдут в снапшот (см. DESIGN.md, «Модель сохранения»).
 */
public final class GameSession {

    private final Level level;
    private int cell;

    /**
     * Начинает игру в клетке старта уровня.
     *
     * @param level собранный уровень
     * @throws NullPointerException если уровень не задан
     */
    public GameSession(Level level) {
        this.level = Objects.requireNonNull(level, "уровень");
        this.cell = level.startCell();
    }

    /** Уровень, на котором идёт игра. */
    public Level level() {
        return level;
    }

    /** Текущая клетка игрока. */
    public int cell() {
        return cell;
    }

    /**
     * Переносит игрока в заданную клетку, не проверяя проходов.
     *
     * <p>Это не команда игрока (в {@link Command} её нет, и ядро про слово
     * «телепорт» не знает), а возможность для адаптера-инструмента: консоли
     * администратора позволено прыгать куда угодно для отладки. Переход через
     * стену здесь намеренный — ценность команды именно в этом.
     *
     * @param cell линейный индекс клетки
     * @throws IndexOutOfBoundsException если клетка вне решётки
     */
    public void teleport(int cell) {
        level.grid().checkCell(cell);
        this.cell = cell;
    }

    /** Вид текущей комнаты — для показа до первой команды. */
    public RoomView roomView() {
        return level.roomView(cell);
    }

    /**
     * Выполняет команду и возвращает ответ.
     *
     * <p>Команда меняет состояние только при успехе: неудачный ход не двигает
     * игрока (см. {@link Outcome#BLOCKED}).
     *
     * @param command команда игрока
     * @return ответ ядра
     * @throws NullPointerException если команда не задана
     */
    public Reply execute(Command command) {
        Objects.requireNonNull(command, "команда");
        return switch (command) {
            case Command.Move move -> move(move.direction());
        };
    }

    private Reply move(Direction direction) {
        int destination = level.step(cell, direction);
        if (destination == Level.NO_PASSAGE) {
            return reply(Outcome.BLOCKED);
        }
        cell = destination;
        return reply(Outcome.MOVED);
    }

    private Reply reply(Outcome outcome) {
        RoomView room = level.roomView(cell);
        List<Command> actions = room.exits().stream()
                .<Command>map(Command.Move::new)
                .toList();
        return new Reply(room, outcome, actions);
    }
}
