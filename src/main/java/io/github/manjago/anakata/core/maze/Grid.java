package io.github.manjago.anakata.core.maze;

/**
 * Система координат N-мерной решётки.
 *
 * <p>Решётка — всегда плотный гиперкуб с размерами {@code sizes[axis]} по осям.
 * Клетка задаётся координатами {@code int[N]} и однозначно — линейным индексом.
 * Первая ось самая быстрая: {@code stride[0] == 1}, а
 * {@code stride[axis] == stride[axis - 1] * sizes[axis - 1]}.
 *
 * <p>Класс хранит только арифметику: ни клеток, ни проходов здесь нет. Проходы
 * лежат отдельно, в {@link Maze}, и индексируются тем же линейным индексом.
 * Внутри ядра работают с индексами — это даёт детерминированный порядок обхода
 * и массивы без хешей; координаты нужны там, где важна читаемость.
 */
public final class Grid {

    private final int[] sizes;
    private final int[] strides;
    private final int cells;
    private final Direction[] directions;

    /**
     * Создаёт решётку с заданными размерами по осям.
     *
     * @param sizes размеры по осям, каждой оси не меньше 2; осей от 2 до 4
     */
    public Grid(int... sizes) {
        if (sizes.length < 2 || sizes.length > 4) {
            throw new IllegalArgumentException(
                    "размерность должна быть 2, 3 или 4, получено " + sizes.length);
        }
        this.sizes = sizes.clone();
        this.strides = new int[sizes.length];
        int total = 1;
        for (int axis = 0; axis < sizes.length; axis++) {
            if (sizes[axis] < 2) {
                throw new IllegalArgumentException(
                        "размер оси " + axis + " меньше 2: " + sizes[axis]);
            }
            if (total > Integer.MAX_VALUE / sizes[axis]) {
                throw new IllegalArgumentException("слишком много клеток в решётке");
            }
            strides[axis] = total;
            total *= sizes[axis];
        }
        this.cells = total;
        this.directions = buildDirections(sizes.length);
    }

    private static Direction[] buildDirections(int dimensions) {
        int count = 0;
        for (Direction direction : Direction.values()) {
            if (direction.axis() < dimensions) {
                count++;
            }
        }
        Direction[] valid = new Direction[count];
        int index = 0;
        for (Direction direction : Direction.values()) {
            if (direction.axis() < dimensions) {
                valid[index++] = direction;
            }
        }
        return valid;
    }

    /** Число измерений решётки. */
    public int dimensions() {
        return sizes.length;
    }

    /** Размер решётки по оси {@code axis}. */
    public int size(int axis) {
        if (axis < 0 || axis >= sizes.length) {
            throw new IndexOutOfBoundsException("нет оси " + axis);
        }
        return sizes[axis];
    }

    /** Общее число клеток в решётке. */
    public int cells() {
        return cells;
    }

    /**
     * Направления, существующие в этой размерности: их ровно {@code 2 * dimensions()}.
     */
    public Direction[] directions() {
        return directions.clone();
    }

    /**
     * Линейный индекс клетки по её координатам.
     *
     * @param coordinates координаты длиной {@code dimensions()}, каждая в своих границах
     * @return линейный индекс
     */
    public int index(int... coordinates) {
        if (coordinates.length != sizes.length) {
            throw new IllegalArgumentException(
                    "нужно " + sizes.length + " координат, получено " + coordinates.length);
        }
        int index = 0;
        for (int axis = 0; axis < sizes.length; axis++) {
            int coordinate = coordinates[axis];
            if (coordinate < 0 || coordinate >= sizes[axis]) {
                throw new IndexOutOfBoundsException(
                        "координата " + coordinate + " вне оси " + axis);
            }
            index += coordinate * strides[axis];
        }
        return index;
    }

    /**
     * Раскладывает линейный индекс на координаты по осям.
     *
     * @param index линейный индекс клетки
     * @param out   массив длиной не меньше {@code dimensions()}, куда пишутся координаты
     */
    public void coordinates(int index, int[] out) {
        checkCell(index);
        if (out.length < sizes.length) {
            throw new IllegalArgumentException(
                    "нужен массив длиной " + sizes.length + ", получен " + out.length);
        }
        for (int axis = 0; axis < sizes.length; axis++) {
            out[axis] = (index / strides[axis]) % sizes[axis];
        }
    }

    /**
     * Индекс соседней клетки в направлении {@code direction}.
     *
     * @return индекс соседа или {@code -1}, если сосед за границей решётки
     */
    public int neighbor(int cell, Direction direction) {
        checkCell(cell);
        checkDirection(direction);
        int axis = direction.axis();
        int coordinate = (cell / strides[axis]) % sizes[axis];
        if (direction.sign() > 0) {
            return coordinate + 1 < sizes[axis] ? cell + strides[axis] : -1;
        }
        return coordinate > 0 ? cell - strides[axis] : -1;
    }

    /**
     * Проверяет, что клетка в границах решётки.
     */
    void checkCell(int cell) {
        if (cell < 0 || cell >= cells) {
            throw new IndexOutOfBoundsException("клетка вне решётки: " + cell);
        }
    }

    /**
     * Проверяет, что направление существует в этой размерности.
     */
    void checkDirection(Direction direction) {
        if (direction.axis() >= sizes.length) {
            throw new IllegalArgumentException(
                    "направление " + direction + " вне размерности " + sizes.length + "D");
        }
    }

    /** Читаемое представление размеров, например {@code 3×4×5}. */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int axis = 0; axis < sizes.length; axis++) {
            if (axis > 0) {
                builder.append('×');
            }
            builder.append(sizes[axis]);
        }
        return builder.toString();
    }
}
