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

class AnalyzeCommandTest {
    @TempDir
    Path root;

    @Test
    void textSummaryListsAFailedParseAndStillExitsZero() throws Exception {
        Files.writeString(root.resolve("Ok.java"), "class Ok { void m() { int x = 1; } }");
        Files.writeString(root.resolve("Broken.java"), "class {");
        byte[] big = new byte[1024 * 1024 + 1];
        Files.write(root.resolve("Big.java"), big);

        Captured captured = run("analyze", root.toString());

        assertEquals(0, captured.code);
        assertTrue(captured.out.contains("Ok.java"));
        assertTrue(captured.out.contains("  ok "));
        assertTrue(captured.out.contains("Broken.java"));
        assertTrue(captured.out.contains("  failed"));
        assertTrue(captured.out.contains("skipped"));
        assertTrue(captured.out.contains("  Big.java"));
        assertTrue(captured.err.isEmpty());
    }

    @Test
    void jsonPrintsTheReportModel() throws Exception {
        Files.writeString(root.resolve("Broken.java"), "class {");

        Captured captured = run("analyze", root.toString(), "--format", "json");

        assertEquals(0, captured.code);
        assertTrue(captured.out.contains("\"kind\": \"failed\""));
        assertTrue(captured.out.contains("\"relativePath\": \"Broken.java\""));
        assertTrue(captured.out.contains("\"message\": \""));
        assertFalse(captured.out.contains("\"message\": \"\""));
        assertTrue(captured.out.contains("\"skipped\": ["));
        assertTrue(captured.out.endsWith("}\n"));
    }

    @Test
    void aSingleFileUsesItsName() throws Exception {
        Path file = root.resolve("Only.java");
        Files.writeString(file, "class Only {}");

        Captured captured = run("analyze", file.toString(), "--format=json");

        assertEquals(0, captured.code);
        assertTrue(captured.out.contains("\"kind\": \"ok\""));
        assertTrue(captured.out.contains("\"relativePath\": \"Only.java\""));
        assertTrue(captured.out.contains("\"physicalLines\": "));
        assertTrue(captured.out.contains("\"methods\": ["));
    }

    @Test
    void badArgumentsExitTwo() throws Exception {
        Path file = root.resolve("Only.java");
        Files.writeString(file, "class Only {}");

        assertEquals(2, run().code);
        assertEquals(2, run("analyze").code);
        assertEquals(2, run("nope", root.toString()).code);
        assertEquals(2, run("analyze", "--format", "yaml", file.toString()).code);
        assertEquals(2, run("analyze", file.toString(), "extra").code);
        assertEquals(2, run("analyze", root.resolve("missing.java").toString()).code);
        Captured missing = run("analyze", root.resolve("missing.java").toString());
        assertTrue(missing.err.contains("usage:") || missing.err.contains("does not exist"));
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
