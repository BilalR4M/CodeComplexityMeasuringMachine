package dev.bilal.ccmm.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GateCommandTest {
    private static final String RULES = """
            rules:
              cyclomatic: 1
              nesting: 3
              maintainability: 0
            """;

    @TempDir
    Path root;

    @Test
    void aChangedMethodThatBreaksARuleExitsOne() throws Exception {
        commit("class Seed { void m() {} }");
        String base = head();
        Files.writeString(root.resolve("Bad.java"), """
                class Bad {
                  void m(int a) {
                    if (a > 0) {
                      a = 1;
                    }
                  }
                }
                """);
        commit("test: add a method over the cyclomatic maximum");

        Captured captured = gate("--base", base);

        assertEquals(1, captured.code);
        assertTrue(captured.out.contains("| Bad.java | m | cyclomatic | 2 | 1 |"));
        assertTrue(captured.out.contains("| File | Method | Rule | Value | Limit |"));
    }

    @Test
    void anUnchangedMethodIsNotGated() throws Exception {
        Files.writeString(root.resolve("Complex.java"), """
                class Complex {
                  void m(int a) {
                    if (a > 0) {
                      a = 1;
                    }
                  }
                }
                """);
        commit("test: add a method that already breaks the rule");
        String base = head();
        Files.writeString(root.resolve("Plain.java"), "class Plain { void m() {} }");
        commit("test: add a method inside the rule");

        Captured captured = gate("--format", "markdown", "--base", base);

        assertEquals(0, captured.code);
        assertTrue(captured.out.contains("No changed methods break the rules."));
        assertFalse(captured.out.contains("Complex.java"));
    }

    @Test
    void aParseFailureIsListedAndDoesNotFailTheGate() throws Exception {
        commit("class Seed { void m() {} }");
        String base = head();
        Files.writeString(root.resolve("Broken.java"), "class {");
        commit("test: add a file that does not parse");

        Captured captured = gate("--base=" + base);

        assertEquals(0, captured.code);
        assertTrue(captured.out.contains("No changed methods break the rules."));
        assertTrue(captured.out.contains("## Unparsed"));
        assertTrue(captured.out.contains("Broken.java"));
    }

    @Test
    void aMissingRuleFileIsAnError() throws Exception {
        git("init");
        Captured captured = gate("--base", "HEAD");

        assertEquals(2, captured.code);
        assertTrue(captured.err.contains("missing .ccmm.yml"));
    }

    @Test
    void aMalformedRuleFileIsAnError() throws Exception {
        git("init");
        Files.writeString(root.resolve(".ccmm.yml"), "nope\n");

        Captured captured = gate("--base", "HEAD");

        assertEquals(2, captured.code);
        assertTrue(captured.err.contains("malformed"));
    }

    @Test
    void badArgumentsExitTwo() {
        assertEquals(2, gate().code);
        assertEquals(2, gate("--format", "text", "--base", "HEAD").code);
        assertTrue(gate().err.contains("usage:"));
    }

    private void commit(String source) throws Exception {
        if (!Files.exists(root.resolve(".git"))) {
            git("init");
            Files.writeString(root.resolve(".ccmm.yml"), RULES);
            Files.writeString(root.resolve("Seed.java"), source);
        }
        git("add", ".");
        git("commit", "-m", "test: record the gate fixture");
    }

    private String head() throws Exception {
        Process process = gitProcess("rev-parse", "HEAD");
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
        return output.strip();
    }

    private void git(String... args) throws Exception {
        Process process = gitProcess(args);
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
    }

    private Process gitProcess(String... args) throws Exception {
        Path hooks = root.resolve("empty-hooks");
        Files.createDirectories(hooks);
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-c");
        command.add("user.email=ccmm-test@example.com");
        command.add("-c");
        command.add("user.name=ccmm-test");
        command.add("-c");
        command.add("commit.gpgsign=false");
        command.add("-c");
        command.add("core.hooksPath=" + hooks);
        command.addAll(List.of(args));
        return new ProcessBuilder(command)
                .directory(root.toFile())
                .redirectErrorStream(true)
                .start();
    }

    private Captured gate(String... args) {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(errBytes, true, StandardCharsets.UTF_8);
        int code = GateCommand.run(List.of(args), out, err, root);
        return new Captured(
                code,
                outBytes.toString(StandardCharsets.UTF_8),
                errBytes.toString(StandardCharsets.UTF_8));
    }

    private record Captured(int code, String out, String err) {}
}
