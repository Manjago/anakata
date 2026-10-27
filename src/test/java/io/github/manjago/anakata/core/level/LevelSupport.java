package io.github.manjago.anakata.core.level;

import io.github.manjago.anakata.core.maze.Direction;
import java.util.List;

/**
 * Мелкие помощники тестов уровня.
 */
public final class LevelSupport {

    private LevelSupport() {
    }

    /**
     * Направление в клетке, где прохода нет, — стена или край решётки.
     *
     * <p>У клетки-угла такие направления есть всегда: наружу прохода не бывает,
     * поэтому для клетки старта результат не {@code null}.
     *
     * @param level уровень
     * @param cell  линейный индекс клетки
     * @return направление без прохода или {@code null}, если все выходы открыты
     */
    public static Direction blockedDirection(Level level, int cell) {
        List<Direction> exits = level.roomView(cell).exits();
        for (Direction direction : level.grid().directions()) {
            if (!exits.contains(direction)) {
                return direction;
            }
        }
        return null;
    }
}
