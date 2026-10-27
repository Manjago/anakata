package io.github.manjago.anakata.core.game;

import io.github.manjago.anakata.core.room.RoomView;
import java.util.List;
import java.util.Objects;

/**
 * Ответ ядра на команду: что игрок видит и что с ним стало.
 *
 * <p>Это данные, а не строки: русские фразы из них делает пакет описаний.
 * {@code room} — комната, где игрок оказался после команды (при неудачном ходе —
 * та же, где был). {@code outcome} — исход действия. {@code availableActions} —
 * команды, доступные в текущей комнате: сейчас это выходы, позже сюда добавится
 * «назвать слово». Доступные команды берут отсюда адаптеры, чтобы не выводить их
 * заново у себя (Telegram рисует по ним кнопки).
 *
 * <p>Состояние игрока — собранные буквы и cooldown — появится вместе с пунктом 6.
 *
 * @param room             вид комнаты после команды
 * @param outcome          исход действия
 * @param availableActions доступные в комнате команды
 */
public record Reply(RoomView room, Outcome outcome, List<Command> availableActions) {

    /**
     * @throws NullPointerException если вид комнаты, исход или список команд не
     *                              заданы, а также если среди команд есть
     *                              {@code null}
     */
    public Reply {
        Objects.requireNonNull(room, "комната");
        Objects.requireNonNull(outcome, "исход");
        Objects.requireNonNull(availableActions, "доступные команды");
        availableActions = List.copyOf(availableActions);
    }
}
