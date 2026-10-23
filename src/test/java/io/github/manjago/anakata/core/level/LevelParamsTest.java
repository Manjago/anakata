package io.github.manjago.anakata.core.level;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LevelParamsTest {

    @Test
    void rejectsNullSizes() {
        assertThrows(NullPointerException.class, () -> new LevelParams(42L, null, 8));
    }

    /**
     * Размеры копируются на входе: иначе правка переданного массива меняла бы уже
     * собранные параметры.
     */
    @Test
    void defensivelyCopiesSizesOnInput() {
        int[] sizes = {6, 6};
        LevelParams params = new LevelParams(42L, sizes, 8);
        sizes[0] = 99;
        assertArrayEquals(new int[] {6, 6}, params.sizes());
    }

    /**
     * Размеры копируются и на чтении: компонент изменяемый, и без копии наружу
     * утёк бы внутренний массив.
     */
    @Test
    void defensivelyCopiesSizesOnRead() {
        LevelParams params = new LevelParams(42L, new int[] {6, 6}, 8);
        params.sizes()[0] = 99;
        assertArrayEquals(new int[] {6, 6}, params.sizes());
    }

    @Test
    void equalParamsAreEqualAndShareHashCode() {
        LevelParams first = new LevelParams(42L, new int[] {6, 6, 6}, 8);
        LevelParams second = new LevelParams(42L, new int[] {6, 6, 6}, 8);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void paramsDifferBySeedSizesOrDensity() {
        LevelParams base = new LevelParams(42L, new int[] {6, 6, 6}, 8);
        assertNotEquals(base, new LevelParams(43L, new int[] {6, 6, 6}, 8));
        assertNotEquals(base, new LevelParams(42L, new int[] {6, 6, 5}, 8));
        assertNotEquals(base, new LevelParams(42L, new int[] {6, 6, 6}, 7));
    }

    @Test
    void toStringShowsSizesAsNumbers() {
        String text = new LevelParams(42L, new int[] {6, 6, 6}, 8).toString();
        assertTrue(text.contains("[6, 6, 6]"), text);
    }
}
