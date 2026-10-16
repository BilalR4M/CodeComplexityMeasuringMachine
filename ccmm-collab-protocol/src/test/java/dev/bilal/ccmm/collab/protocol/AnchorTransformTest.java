package dev.bilal.ccmm.collab.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnchorTransformTest {
    @Test
    void insertAboveShiftsBothEnds() {
        ThreadAnchor anchor = new ThreadAnchor(10, 20);
        TextOp insert = TextOp.insert(0, "xxxxx", 0, "a");
        Optional<ThreadAnchor> next = AnchorTransformer.next(anchor, insert);
        assertEquals(Optional.of(new ThreadAnchor(15, 25)), next);
    }

    @Test
    void bothClientsSeeTheSameShiftAfterConcurrentInsertsAbove() {
        String base = "0123456789ABCDEFGHIJ"; // length 20; anchor on 10..20 is "ABCDEFGHIJ"
        ThreadAnchor anchor = new ThreadAnchor(10, 20);
        TextOp left = TextOp.insert(0, "LEFT-", 0, "a");
        TextOp right = TextOp.insert(0, "RIGHT", 0, "b");

        assertClientsConverge(base, anchor, left, right);
        assertClientsConverge(base, anchor, right, left);
    }

    @Test
    void insertInsideGrowsTheEnd() {
        ThreadAnchor anchor = new ThreadAnchor(10, 20);
        Optional<ThreadAnchor> next = AnchorTransformer.next(anchor, TextOp.insert(15, "IN", 0, "a"));
        assertEquals(Optional.of(new ThreadAnchor(10, 22)), next);
    }

    @Test
    void deleteShortensTheRange() {
        ThreadAnchor anchor = new ThreadAnchor(10, 20);
        Optional<ThreadAnchor> next = AnchorTransformer.next(anchor, TextOp.delete(12, 3, 0, "a"));
        assertEquals(Optional.of(new ThreadAnchor(10, 17)), next);
    }

    @Test
    void deleteThatCoversTheAnchorRemovesIt() {
        ThreadAnchor anchor = new ThreadAnchor(10, 20);
        Optional<ThreadAnchor> next = AnchorTransformer.next(anchor, TextOp.delete(5, 20, 0, "a"));
        assertTrue(next.isEmpty());
    }

    @Test
    void deleteExactlyMatchingTheAnchorRemovesIt() {
        ThreadAnchor anchor = new ThreadAnchor(10, 20);
        Optional<ThreadAnchor> next = AnchorTransformer.next(anchor, TextOp.delete(10, 10, 0, "a"));
        assertTrue(next.isEmpty());
    }

    @Test
    void concurrentInsertAndDeleteAboveStillConvergeOnAnchor() {
        String base = "0123456789ABCDEFGHIJ";
        ThreadAnchor anchor = new ThreadAnchor(10, 20);
        TextOp insert = TextOp.insert(0, "XX", 0, "a");
        TextOp delete = TextOp.delete(2, 3, 0, "b");
        assertClientsConverge(base, anchor, insert, delete);
        assertClientsConverge(base, anchor, delete, insert);
    }

    @Test
    void textStillConvergesWhenAnchorsAreTracked() {
        String base = "hello world!!!!!";
        ThreadAnchor anchor = new ThreadAnchor(6, 11); // "world"
        TextOp a = TextOp.insert(0, "X", 0, "a");
        TextOp b = TextOp.insert(5, "Y", 0, "b");
        ServerAuthority server = new ServerAuthority(base);
        server.receive(a);
        server.receive(b);
        assertEquals(server.document(), ServerAuthority.replay(base, server.log()));
        Optional<ThreadAnchor> fromLog = AnchorTransformer.applyAll(anchor, server.log());
        // X at 0 then Y before "world" → "world" starts at 8
        assertEquals(Optional.of(new ThreadAnchor(8, 13)), fromLog);
        assertEquals("world", server.document().substring(fromLog.get().start(), fromLog.get().end()));
    }

    @Test
    void benchmarkFileRecordsGo() throws Exception {
        Path cwd = Path.of("").toAbsolutePath();
        Path report = Files.exists(cwd.resolve("docs/benchmarks"))
                ? cwd.resolve("docs/benchmarks/anchor-ot.md")
                : cwd.getParent().resolve("docs/benchmarks/anchor-ot.md");
        String text = Files.readString(report).strip();
        String verdict = text.substring(text.lastIndexOf('\n') + 1);
        assertEquals("go", verdict);
    }

    private static void assertClientsConverge(String base, ThreadAnchor anchor, TextOp first, TextOp second) {
        ServerAuthority server = new ServerAuthority(base);
        server.receive(first);
        server.receive(second);
        List<TextOp> log = server.log();
        String fromServer = server.document();
        assertEquals(fromServer, ServerAuthority.replay(base, log));

        Optional<ThreadAnchor> clientA = AnchorTransformer.applyAll(anchor, log);
        Optional<ThreadAnchor> clientB = AnchorTransformer.applyAll(anchor, log);
        assertEquals(clientA, clientB);
        assertEquals(fromServer, ServerAuthority.replay(base, log));
    }
}
