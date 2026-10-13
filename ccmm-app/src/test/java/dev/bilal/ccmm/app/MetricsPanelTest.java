package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.bilal.ccmm.core.FileMetrics;
import dev.bilal.ccmm.core.Halstead;
import dev.bilal.ccmm.core.MethodMetrics;
import java.util.List;
import org.junit.jupiter.api.Test;

class MetricsPanelTest {
    @Test
    void theCaretSelectsTheLatestMethodThatHasStarted() {
        Halstead halstead = new Halstead(0, 0, 0, 0, 0, 0, 0, 0, 0);
        MethodMetrics first = new MethodMetrics("first", 2, 1, 1, 0, halstead, 1);
        MethodMetrics second = new MethodMetrics("second", 4, 2, 2, 1, halstead, 1);
        FileMetrics file = new FileMetrics(6, 4, 0, 2, 3, 1, halstead, 1, List.of(first, second));

        assertNull(MetricsPanel.methodAt(file, 1));
        assertEquals("first", MetricsPanel.methodAt(file, 2).name());
        assertEquals("first", MetricsPanel.methodAt(file, 3).name());
        assertEquals("second", MetricsPanel.methodAt(file, 5).name());
    }
}
