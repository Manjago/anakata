package io.github.manjago.anakata.console;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.manjago.anakata.core.maze.Grid;
import org.junit.jupiter.api.Test;

class AdminTextTest {

    /** Полный текст подсказки — литералом: сравнение с самой функцией ничего не ловит. */
    @Test
    void teleportHintIsTheExactTextForTwoDimensions() {
        assertEquals("Телепорт: нужно 2 числа — по числу измерений, например «телепорт 0 1».",
                AdminText.teleportHint(new Grid(3, 3)));
    }

    /** Пример не выходит за оси: на оси длиной 2 номер оси уже запрещён. */
    @Test
    void teleportHintExampleFitsTheGrid() {
        assertEquals("Телепорт: нужно 3 числа — по числу измерений, например «телепорт 0 1 1».",
                AdminText.teleportHint(new Grid(2, 2, 2)));
    }

    /** Склонение честное: единица, двойка-тройка-четвёрка, всё прочее. */
    @Test
    void numberWordDeclinesCounts() {
        assertEquals("число", AdminText.numberWord(1));
        assertEquals("число", AdminText.numberWord(21));
        assertEquals("числа", AdminText.numberWord(2));
        assertEquals("числа", AdminText.numberWord(22));
        assertEquals("чисел", AdminText.numberWord(5));
        assertEquals("чисел", AdminText.numberWord(11));
        assertEquals("чисел", AdminText.numberWord(25));
    }
}
