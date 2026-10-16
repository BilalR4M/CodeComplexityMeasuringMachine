package dev.bilal.ccmm.spikes.e0t02;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RegexJavaHighlighterTest {
    private final RegexJavaHighlighter highlighter = new RegexJavaHighlighter();

    @Test
    void keywordsAndStringsAreTokenized() {
        List<Span> spans = highlighter.highlight("public class A { String s = \"hi\"; }", "java");
        assertTrue(spans.stream().anyMatch(s -> s.token().equals("keyword") && "public".equals(slice(s))));
        assertTrue(spans.stream().anyMatch(s -> s.token().equals("string")));
    }

    @Test
    void commentsAreTokenized() {
        List<Span> spans = highlighter.highlight("// hello\nint x;", "java");
        assertEquals("comment", spans.get(0).token());
    }

    @Test
    void nonJavaExtensionYieldsNoSpans() {
        assertTrue(highlighter.highlight("public int x;", "md").isEmpty());
    }

    private static String slice(Span span) {
        return "public class A { String s = \"hi\"; }".substring(span.start(), span.end());
    }
}
