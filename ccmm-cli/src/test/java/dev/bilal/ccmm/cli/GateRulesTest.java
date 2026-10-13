package dev.bilal.ccmm.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.cli.GateRules.GateRuleException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GateRulesTest {
    @TempDir
    Path root;

    @Test
    void aValidFileSetsMaximumsAndAMinimum() throws Exception {
        Files.writeString(root.resolve(".ccmm.yml"), """
                # thresholds
                rules:
                  nesting: 3
                  cyclomatic: 10
                  maintainability: 20.5
                """);

        GateRules rules = GateRules.load(root);

        assertEquals(10, rules.cyclomatic());
        assertEquals(3, rules.nesting());
        assertEquals(20.5, rules.maintainability());
        assertFalse(rules.exceedsCyclomatic(10));
        assertTrue(rules.exceedsCyclomatic(11));
        assertFalse(rules.exceedsNesting(3));
        assertTrue(rules.exceedsNesting(4));
        assertFalse(rules.belowMaintainability(20.5));
        assertTrue(rules.belowMaintainability(20.4));
    }

    @Test
    void aMissingFileIsAnError() {
        GateRuleException exception = assertThrows(GateRuleException.class, () -> GateRules.load(root));

        assertTrue(exception.getMessage().contains("missing .ccmm.yml"));
    }

    @Test
    void aMalformedFileIsAnError() throws Exception {
        Files.writeString(root.resolve(".ccmm.yml"), """
                rules:
                  cyclomatic: lots
                  nesting: 3
                  maintainability: 20
                """);

        GateRuleException exception = assertThrows(GateRuleException.class, () -> GateRules.load(root));

        assertTrue(exception.getMessage().contains("malformed"));
    }
}
