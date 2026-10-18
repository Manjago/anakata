package io.github.manjago.anakata.core.rng;

/**
 * Фабрика сидераторов — детерминированных ответвлений потока от базового сида.
 *
 * <p>Сидераторы независимы, «параллельны»: с одним и тем же сидом и именем всегда
 * получается один и тот же поток, поэтому в снапшоте достаточно хранить базовый сид
 * уровня — все ветки выводятся заново. Реализация привязана к {@link SplitMix64}.
 *
 * <p>Имена веток — {@link Branch}, а не произвольные строки: любая опечатка или
 * переименование в строке тихо меняет потоки и уровни по всем опубликованным сидам.
 * Хешируется явное строковое поле константы, а не {@code name()} и не {@code ordinal()},
 * чтобы переименование константы не сломало контракт сида.
 */
public final class Seeders {

    private Seeders() {
        // Утилитарный класс без экземпляров.
    }

    /**
     * Имена сидераторов — часть контракта сида. Менять нельзя.
     */
    public enum Branch {
        MAZE("maze"),
        LETTERS("letters"),
        WORD("word");

        private final String literal;

        Branch(String literal) {
            this.literal = literal;
        }

        /** Строковое имя, по которому расходится поток. Виден в пакете для тестов. */
        String literal() {
            return literal;
        }
    }

    /**
     * Возвращает сид ответвления {@code name} от базового сида {@code seed}.
     *
     * <p>Это {@code mix(seed ^ hash(literal))}: свёртка базового сида и хеша имени.
     * Результат — обычный {@code long}, им можно продолжить иерархию веток
     * (например, снова передать в {@code derive} с другой веткой) или построить из
     * него генератор. Разные имена у одного сида дают разные сиды ответвлений.
     *
     * @param seed  базовый сид
     * @param name  имя потока
     * @return сид ответвления
     */
    public static long derive(long seed, Branch name) {
        return SplitMix64.mix(seed ^ hashLiteral(name.literal()));
    }

    /**
     * Возвращает новый генератор — ответвление базового сида {@code seed} с именем
     * {@code name}.
     *
     * @param seed  базовый сид
     * @param name  имя потока
     * @return новый генератор-сидератор
     */
    public static SplitMix64 branch(long seed, Branch name) {
        return new SplitMix64(derive(seed, name));
    }

    /**
     * 64-битная свёртка строки без внешних хешей: каждый символ смешивается
     * с накопленной суммой через разболтку SplitMix64 {@link SplitMix64#mix}.
     */
    private static long hashLiteral(String literal) {
        long h = 0x9E3779B97F4A7C15L;
        for (int i = 0; i < literal.length(); i++) {
            h = SplitMix64.mix(h ^ literal.charAt(i));
        }
        return h;
    }
}
