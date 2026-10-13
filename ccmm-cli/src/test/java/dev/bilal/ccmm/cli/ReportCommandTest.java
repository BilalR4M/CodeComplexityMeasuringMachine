package dev.bilal.ccmm.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportCommandTest {
    @TempDir
    Path root;

    @Test
    void markdownHasASectionPerFileAndAMethodTable() throws Exception {
        Files.writeString(root.resolve("Ok.java"), "class Ok { void m() { int x = 1; } }");
        Files.writeString(root.resolve("Broken.java"), "class {");

        Captured captured = run("report", root.toString(), "--format", "markdown");

        assertEquals(0, captured.code);
        int broken = captured.out.indexOf("## `Broken.java`");
        int ok = captured.out.indexOf("## `Ok.java`");
        assertTrue(broken >= 0 && ok > broken, captured.out);
        String failed = captured.out.substring(broken, ok);
        assertTrue(failed.contains("Parse failed:"));
        assertFalse(failed.contains("| Method |"));
        assertFalse(failed.contains("Maintainability:"));
        assertTrue(captured.out.contains("| Method | Line | Cyclomatic | Nesting | Volume | Maintainability |"));
        assertTrue(captured.out.contains("| m |"));
    }

    @Test
    void aSingleFileIsOneSection() throws Exception {
        Path file = root.resolve("Only.java");
        Files.writeString(file, "class Only { void ready() {} }");

        Captured captured = run("report", file.toString(), "--format=markdown");

        assertEquals(0, captured.code);
        assertTrue(captured.out.contains("## `Only.java`"));
        assertTrue(captured.out.contains("| ready |"));
        assertFalse(captured.out.contains("Parse failed:"));
    }

    @Test
    void badArgumentsExitTwo() throws Exception {
        Path file = root.resolve("Only.java");
        Files.writeString(file, "class Only {}");

        assertEquals(2, run("report").code);
        assertEquals(2, run("report", "--format", "json", file.toString()).code);
        assertEquals(2, run("report", file.toString(), "extra").code);
        Captured missing = run("report", root.resolve("missing.java").toString());
        assertEquals(2, missing.code);
        assertTrue(missing.err.contains("does not exist"));
    }

    private Captured run(String... args) {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(errBytes, true, StandardCharsets.UTF_8);
        int code = Main.run(args, out, err);
        return new Captured(
                code,
                outBytes.toString(StandardCharsets.UTF_8),
                errBytes.toString(StandardCharsets.UTF_8));
    }

    private record Captured(int code, String out, String err) {}
}
