package dev.bilal.ccmm.spikes.e0t02;

/**
 * Records whether Eclipse tm4e can be measured without adding it to the parent POM.
 * Licence is EPL-2.0 — the owner must accept before any product dependency.
 */
final class Tm4eProbe {
    private Tm4eProbe() {}

    static String probe() {
        try {
            Class.forName("org.eclipse.tm4e.core.grammar.IGrammar");
            return "- tm4e classes found on the spike classpath. "
                    + "This run did not load TextMate grammars or time a full highlight. "
                    + "Licence: EPL-2.0 (owner must accept before adopting).";
        } catch (ClassNotFoundException missing) {
            return "- tm4e: not measured\n"
                    + "- Reason: `org.eclipse.tm4e` is not a spike dependency and must not enter "
                    + "`ccmm-app` without an EPL-2.0 acceptance. No side toolchain was applied "
                    + "to the parent POM.\n"
                    + "- Licence note: EPL-2.0\n"
                    + "- Result: skip. Regex remains the measured candidate.";
        }
    }
}
