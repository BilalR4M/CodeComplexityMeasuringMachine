package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.editor.BufferFinder;
import java.util.List;
import org.junit.jupiter.api.Test;

class BufferFinderTest {
    @Test
    void emptyQueryYieldsNoMatches() {
        assertTrue(BufferFinder.findAll("aaa", "", true).isEmpty());
        assertTrue(BufferFinder.findAll("aaa", null, false).isEmpty());
    }

    @Test
    void findAllIsLiteralAndNonOverlapping() {
        List<BufferFinder.Match> matches = BufferFinder.findAll("aaaa", "aa", true);
        assertEquals(2, matches.size());
        assertEquals(0, matches.get(0).start());
        assertEquals(2, matches.get(0).end());
        assertEquals(2, matches.get(1).start());
        assertEquals(4, matches.get(1).end());
    }

    @Test
    void matchCaseIsOptional() {
        assertEquals(1, BufferFinder.findAll("Parse", "parse", false).size());
        assertTrue(BufferFinder.findAll("Parse", "parse", true).isEmpty());
    }

    @Test
    void nextAndPreviousWrap() {
        List<BufferFinder.Match> matches = BufferFinder.findAll("parse x parse", "parse", true);
        assertEquals(0, BufferFinder.nextIndex(matches, 0));
        assertEquals(1, BufferFinder.nextIndex(matches, 5));
        assertEquals(0, BufferFinder.nextIndex(matches, 13));
        assertEquals(1, BufferFinder.previousIndex(matches, 13));
        assertEquals(0, BufferFinder.previousIndex(matches, 5));
        assertEquals(1, BufferFinder.previousIndex(matches, 0));
    }

    @Test
    void replaceAllSubstitutesEveryMatch() {
        assertEquals("b b", BufferFinder.replaceAll("a a", "a", "b", true));
        assertEquals("a a", BufferFinder.replaceAll("a a", "", "b", true));
    }
}
