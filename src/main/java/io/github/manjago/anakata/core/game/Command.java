package io.github.manjago.anakata.core.game;

import io.github.manjago.anakata.core.maze.Direction;
import java.util.Objects;

/**
 * Команда игрока, меняющая мир.
 *
 * <p>В ядре живут только такие команды: ход и позже — назвать слово. Помощь,
 * выход из игры, пересборка уровня и админ-команды — дело адаптера, ядро про них
 * не знает (см. DESIGN.md, «Границы команд»).
 *
 * <p>Тип закрыт ({@code sealed}): форм пока одна — {@link Move}. {@link GameSession}
 * переключается по форме через {@code switch} без {@code default}, поэтому новая
 * форма не сможет остаться незамеченной. Разбор строк в {@code descriptions} при
 * этом табличный ({@code CommandParser}: слово → команда), и «назвать слово»
 * потребует перестроить его с «одно слово — одна команда» на «первое слово плюс
 * аргумент», а не добавить ещё ветку.
 */
public sealed interface Command permits Command.Move {

    /**
     * Ход в направлении. Существует ли проход — решает уровень, а не команда:
     * разбор слова знает лишь слово, а размерность — {@code Level}.
     *
     * @param direction направление хода
     */
    record Move(Direction direction) implements Command {

        /** @throws NullPointerException если направление не задано */
        public Move {
            Objects.requireNonNull(direction, "направление");
        }
    }
}
