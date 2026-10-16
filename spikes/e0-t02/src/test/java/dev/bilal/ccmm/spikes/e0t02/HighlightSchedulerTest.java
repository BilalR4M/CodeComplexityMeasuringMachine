package dev.bilal.ccmm.spikes.e0t02;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HighlightSchedulerTest {
    @Test
    void newerRevisionDiscardsStaleSpans() throws Exception {
        assertTrue(HighlightingBakeoff.measureCancel(new RegexJavaHighlighter()));
    }
}
