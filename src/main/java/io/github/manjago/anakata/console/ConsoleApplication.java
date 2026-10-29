package io.github.manjago.anakata.console;

import io.github.manjago.anakata.core.game.GameSession;
import io.github.manjago.anakata.core.level.Level;
import io.github.manjago.anakata.core.level.LevelParams;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Точка входа консоли: подключает настоящие потоки, случайность и запускает цикл.
 *
 * <p>Отделена от точки входа бота (пункт 8): консоль — инструмент администратора,
 * запускается в терминале. Роль этого класса — только собрать зависимости (stdio,
 * источник сида) и передать их {@link StartupPrompt} и {@link ConsoleGame};
 * логика живёт в них и проверяется тестами без настоящего ввода-вывода.
 *
 * <p>Флаг {@code --help} (или {@code -h}) печатает шпаргалку и выходит, не заходя
 * в игру.
 */
public final class ConsoleApplication {

    private ConsoleApplication() {
    }

    /**
     * @param args аргументы командной строки; поддерживается {@code --help}/{@code -h}
     */
    public static void main(String[] args) {
        PrintWriter out = new PrintWriter(
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
        if (wantsHelp(args)) {
            printLines(out, ConsoleText.cheatSheet());
            return;
        }

        printLines(out, ConsoleText.cheatSheet());
        out.println();

        BufferedReader in = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
        LongSupplier randomSeed = new SecureRandom()::nextLong;
        Optional<LevelParams> params = new StartupPrompt(in, out, randomSeed).ask();
        if (params.isEmpty()) {
            out.println(ConsoleText.farewell());
            return;
        }
        Level level = Level.build(params.get());
        out.println(ConsoleText.level(level));
        new ConsoleGame(in, out, new GameSession(level)).run();
    }

    private static boolean wantsHelp(String[] args) {
        return args.length > 0 && ("--help".equals(args[0]) || "-h".equals(args[0]));
    }

    private static void printLines(PrintWriter out, List<String> lines) {
        for (String line : lines) {
            out.println(line);
        }
    }
}
