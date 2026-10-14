package io.github.manjago.anakata.style;

import static java.nio.file.FileVisitResult.CONTINUE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Следит за правилом «каждый файл заканчивается переводом строки».
 * Нарушение падает сборку на фазе test.
 */
class NewlineAtEndOfFileTest {

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            ".java", ".md", ".xml", ".properties", ".conf", ".txt");

    private static final Set<String> TEXT_NAMES = Set.of("LICENSE", ".gitignore");

    private static final Set<String> SKIPPED_DIRECTORIES = Set.of(
            ".git", "target", ".idea", ".vscode");

    private static boolean isTrackedTextFile(Path file) {
        String name = file.getFileName().toString();
        if (TEXT_NAMES.contains(name)) {
            return true;
        }
        String fileName = name.toLowerCase(Locale.ROOT);
        return TEXT_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    @Test
    void projectTextFilesEndWithNewline() {
        verify(Path.of(System.getProperty("user.dir")));
    }

    @Test
    void verifyFindsFileWithoutFinalNewline(@TempDir Path root) throws IOException {
        Path violating = root.resolve("broken.java");
        Files.writeString(violating, "code", StandardCharsets.UTF_8);

        assertThrows(AssertionError.class, () -> verify(root));
    }

    static void verify(Path root) {
        List<String> violations = new ArrayList<>();
        walk(root, violations);
        if (!violations.isEmpty()) {
            fail("Файлы не заканчиваются переводом строки:\n" + String.join("\n", violations));
        }
    }

    private static void walk(Path root, List<String> violations) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return SKIPPED_DIRECTORIES.contains(dir.getFileName().toString())
                            ? FileVisitResult.SKIP_SUBTREE
                            : CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    if (isTrackedTextFile(file) && !endsWithNewline(file)) {
                        violations.add("  " + normalize(root, file));
                    }
                    return CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось обойти дерево " + root, e);
        }
    }

    private static boolean endsWithNewline(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        return bytes.length > 0 && bytes[bytes.length - 1] == '\n';
    }

    private static String normalize(Path root, Path file) {
        return root.relativize(file).toString();
    }
}
