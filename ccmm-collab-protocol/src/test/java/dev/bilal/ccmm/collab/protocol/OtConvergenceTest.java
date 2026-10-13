package dev.bilal.ccmm.collab.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class OtConvergenceTest {
    @Test
    void concurrentInsertsAtDifferentPositionsConverge() {
        String base = "hello";
        TextOp left = TextOp.insert(0, "X", 0, "a");
        TextOp right = TextOp.insert(5, "Y", 0, "b");

        assertConverge(base, left, right, "XhelloY");
        assertConverge(base, right, left, "XhelloY");
    }

    @Test
    void concurrentInsertsAtTheSamePositionFollowServerOrder() {
        String base = "ab";
        TextOp a = TextOp.insert(1, "X", 0, "a");
        TextOp b = TextOp.insert(1, "Y", 0, "b");

        assertConverge(base, a, b, "aXYb");
        assertConverge(base, b, a, "aYXb");
    }

    @Test
    void concurrentDisjointDeletesConverge() {
        String base = "abcdef";
        TextOp a = TextOp.delete(1, 2, 0, "a");
        TextOp b = TextOp.delete(4, 2, 0, "b");

        assertConverge(base, a, b, "ad");
        assertConverge(base, b, a, "ad");
    }

    @Test
    void concurrentOverlappingDeletesConverge() {
        String base = "abcdef";
        TextOp a = TextOp.delete(1, 3, 0, "a");
        TextOp b = TextOp.delete(2, 3, 0, "b");

        assertConverge(base, a, b, "af");
        assertConverge(base, b, a, "af");
    }

    @Test
    void concurrentInsertAndDeleteConvergeForEachServerOrder() {
        String base = "abcdef";
        TextOp insert = TextOp.insert(3, "X", 0, "a");
        TextOp delete = TextOp.delete(2, 3, 0, "b");

        assertConverge(base, insert, delete, "abf");
        assertConverge(base, delete, insert, "abXf");
    }

    @Test
    void benchmarkFileRecordsGo() throws Exception {
        Path cwd = Path.of("").toAbsolutePath();
        Path report = Files.exists(cwd.resolve("docs/benchmarks"))
                ? cwd.resolve("docs/benchmarks/ot-convergence.md")
                : cwd.getParent().resolve("docs/benchmarks/ot-convergence.md");
        String text = Files.readString(report).strip();
        String verdict = text.substring(text.lastIndexOf('\n') + 1);
        assertEquals("go", verdict);
    }

    @Test
    void randomConcurrentPairsReplayToTheServerDocument() {
        String base = "abcdefghij";
        Random random = new Random(1);
        for (int i = 0; i < 30; i++) {
            TextOp a = randomOp(random, base, "a");
            TextOp b = randomOp(random, base, "b");
            ServerAuthority server = new ServerAuthority(base);
            server.receive(a);
            server.receive(b);
            String replayed = ServerAuthority.replay(base, server.log());
            assertEquals(server.document(), replayed, "pair " + i + " " + a + " then " + b);
            assertEquals(replayed, clientDocument(base, server.log()));
        }
    }

    private static void assertConverge(String base, TextOp first, TextOp second, String expected) {
        ServerAuthority server = new ServerAuthority(base);
        server.receive(first);
        server.receive(second);
        String fromServer = server.document();
        String fromLog = ServerAuthority.replay(server.base(), server.log());
        String clientA = clientDocument(base, server.log());
        String clientB = clientDocument(base, server.log());
        assertEquals(expected, fromServer);
        assertEquals(fromServer, fromLog);
        assertEquals(fromServer, clientA);
        assertEquals(clientA, clientB);
    }

    private static String clientDocument(String base, List<TextOp> log) {
        return ServerAuthority.replay(base, log);
    }

    private static TextOp randomOp(Random random, String base, String clientId) {
        if (random.nextBoolean() || base.isEmpty()) {
            int index = random.nextInt(base.length() + 1);
            return TextOp.insert(index, random.nextBoolean() ? "Q" : "xy", 0, clientId);
        }
        int index = random.nextInt(base.length());
        int length = 1 + random.nextInt(base.length() - index);
        return TextOp.delete(index, length, 0, clientId);
    }
}
