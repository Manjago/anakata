package io.github.manjago.anakata.descriptions;

import io.github.manjago.anakata.core.game.Outcome;
import java.util.List;
import java.util.Objects;

/**
 * Нарратив исхода команды — то, чем мир ответил на ход.
 *
 * <p>Это язык мира, а не хром адаптера: {@link Outcome} — состояние из
 * {@link io.github.manjago.anakata.core.game.Reply}, и отвечать на неудачный ход
 * придётся любому адаптеру, поэтому фраза живёт здесь, а не в консоли. Иначе
 * Telegram либо продублирует строку, либо потянет за собой адаптер.
 *
 * <p>Список строк, а не одна строка: исход может потребовать нескольких строк, и
 * пустой список — штатный ответ (обычный ход говорить нечего, о нём расскажет
 * комната). Успешный ход поэтому возвращает пусто, а не отдельную фразу-паразит.
 */
public final class OutcomeDescriber {

    /**
     * Строки исхода в порядке показа; пусто, если сказать нечего.
     *
     * @param outcome исход команды
     * @return строки нарратива, возможно пустые
     * @throws NullPointerException если исход не задан
     */
    public List<String> describe(Outcome outcome) {
        Objects.requireNonNull(outcome, "исход");
        return switch (outcome) {
            case MOVED -> List.of();
            case BLOCKED -> List.of("Туда не пройти.");
        };
    }
}
