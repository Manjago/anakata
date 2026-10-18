package io.github.manjago.anakata.core.rng;

/**
 * Фабрика сидераторов — детерминированных ответвлений потока от базового сида.
 *
 * <p>Сидераторы независимы, «параллельны»: с одним и тем же сидом и именем всегда
 * получается один и тот же поток, поэтому в снапшоте достаточно хранить базовый сид
 * уровня — все ветки выводятся заново. Реализация привязана к {@link SplitMix64}.
 */
public final class Seeders {

    private Seeders() {
        // Утилитарный класс без экземпляров.
    }

    /**
     * Возвращает сид ответвления {@code name} от базового сида {@code seed}.
     *
     * <p>Это {@code mix(seed ^ hashName(name))}: свёртка базового сида и хеша имени.
     * Результат — обычный {@code long}, им можно продолжить иерархию веток
     * (например, снова передать в {@code derive} с другим именем) или построить из
     * него генератор. Разные имена у одного сида дают разные, не пересекающиеся потоки.
     *
     * @param seed  базовый сид
     * @param name  имя потока, например {@code "maze"} или {@code "letters"}
     * @return сид ответвления
     */
    public static long derive(long seed, String name) {
        return SplitMix64.mix(seed ^ hashName(name));
    }

    /**
     * Возвращает новый генератор — ответвление базового сида {@code seed} с именем
     * {@code name}.
     *
     * @param seed  базовый сид
     * @param name  имя потока, например {@code "maze"} или {@code "letters"}
     * @return новый генератор-сидератор
     */
    public static SplitMix64 branch(long seed, String name) {
        return new SplitMix64(derive(seed, name));
    }

    /**
     * 64-битная свёртка строки без внешних хешей: каждая кодовая точка смешивается
     * с накопленной суммой через разболтку SplitMix64 {@link SplitMix64#mix}.
     */
    private static long hashName(String name) {
        long h = 0x9E3779B97F4A7C15L;
        for (int i = 0; i < name.length(); i++) {
            h = SplitMix64.mix(h ^ name.charAt(i));
        }
        return h;
    }
}
