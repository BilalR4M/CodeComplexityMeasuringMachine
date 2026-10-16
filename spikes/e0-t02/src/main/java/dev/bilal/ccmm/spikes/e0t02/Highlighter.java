package dev.bilal.ccmm.spikes.e0t02;

import java.util.List;

/** One lexer interface for the three spike engines. */
public interface Highlighter {
    List<Span> highlight(String text, String extension);
}
