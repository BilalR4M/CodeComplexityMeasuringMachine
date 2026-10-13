package dev.bilal.ccmm.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectScanTest {
    private final Analyzer analyzer = new Analyzer();

    @TempDir
    Path root;

    @Test
    void eachJavaFileIsOneResult() throws Exception {
        Files.createDirectories(root.resolve("a"));
        Files.writeString(root.resolve("a/A.java"), "class A {}");
        Files.writeString(root.resolve("B.java"), "class {");
        Files.writeString(root.resolve("notes.txt"), "not java");

        ProjectReport report = analyzer.scan(root);

        assertEquals(List.of("B.java", "a/A.java"), report.files().stream().map(ProjectScanTest::path).toList());
        assertEquals(List.of(), report.skipped());
        assertInstanceOf(AnalysisResult.Failed.class, report.files().get(0));
        assertInstanceOf(AnalysisResult.Ok.class, report.files().get(1));
    }

    @Test
    void filesLargerThanOneMebibyteAreSkipped() throws Exception {
        Files.writeString(root.resolve("Small.java"), "class Small {}");
        byte[] big = new byte[(int) ProjectScanner.MAX_FILE_BYTES + 1];
        System.arraycopy("class Big {}".getBytes(), 0, big, 0, "class Big {}".length());
        Files.write(root.resolve("Big.java"), big);
        byte[] exact = new byte[(int) ProjectScanner.MAX_FILE_BYTES];
        java.util.Arrays.fill(exact, (byte) ' ');
        System.arraycopy("class Exact {}".getBytes(), 0, exact, 0, "class Exact {}".length());
        Files.write(root.resolve("Exact.java"), exact);

        ProjectReport report = analyzer.scan(root);

        assertEquals(List.of("Exact.java", "Small.java"), report.files().stream().map(ProjectScanTest::path).toList());
        assertInstanceOf(AnalysisResult.Ok.class, report.files().get(0));
        assertEquals(List.of("Big.java"), report.skipped());
    }

    @Test
    void symlinksThatLeaveTheRootAreSkipped() throws Exception {
        Path outside = root.getParent().resolve("outside-scan-" + root.getFileName());
        Files.createDirectories(outside);
        try {
            Files.writeString(outside.resolve("Secret.java"), "class Secret {}");
            Files.writeString(root.resolve("In.java"), "class In {}");
            Files.createDirectories(root.resolve("keep"));
            Files.writeString(root.resolve("keep/Real.java"), "class Real {}");
            try {
                Files.createSymbolicLink(root.resolve("Escape.java"), outside.resolve("Secret.java"));
                Files.createSymbolicLink(root.resolve("escape-dir"), outside);
                Files.createSymbolicLink(root.resolve("Alias.java"), root.resolve("keep/Real.java"));
            } catch (FileSystemException exception) {
                org.junit.jupiter.api.Assumptions.abort(
                        "symlink privilege is not available: " + exception.getMessage());
            }

            ProjectReport report = analyzer.scan(root);

            assertEquals(
                    List.of("Alias.java", "In.java", "keep/Real.java"),
                    report.files().stream().map(ProjectScanTest::path).toList());
            assertEquals(List.of("Escape.java", "escape-dir"), report.skipped());
            assertTrue(report.skipped().stream().noneMatch(path -> path.contains("..") || path.contains(":")));
        } finally {
            Files.walk(outside).sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (java.io.IOException ignored) {
                    // best-effort cleanup of the sibling directory
                }
            });
        }
    }

    @Test
    void aMissingDirectoryIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> analyzer.scan(null));
        assertThrows(IllegalArgumentException.class, () -> analyzer.scan(root.resolve("missing")));
    }

    @Test
    void aFewHundredFilesStayInsideTheBudget() throws Exception {
        int count = 300;
        for (int i = 0; i < count; i++) {
            Path dir = root.resolve("p" + (i % 20));
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("C" + i + ".java"), "class C" + i + " { int m() { return " + i + "; } }");
        }
        long start = System.nanoTime();
        ProjectReport report = analyzer.scan(root);
        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        long usedHeap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        assertEquals(count, report.files().size());
        assertEquals(List.of(), report.skipped());
        assertTrue(elapsedMs < 3000, "scan took " + elapsedMs + " ms");
        assertTrue(usedHeap < 500L * 1024 * 1024, "heap " + usedHeap);
        System.out.println("PROJECT_SCAN files=" + count + " ms=" + elapsedMs + " heapBytes=" + usedHeap);
    }

    private static String path(AnalysisResult result) {
        return switch (result) {
            case AnalysisResult.Ok ok -> ok.relativePath();
            case AnalysisResult.Failed failed -> failed.relativePath();
        };
    }
}
