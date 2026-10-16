package dev.bilal.ccmm.spikes.e0t02;

/**
 * Records whether a tree-sitter Java binding builds and runs on JDK 21.
 * Native libraries must stay off the FX thread if adopted.
 */
final class TreeSitterProbe {
    private TreeSitterProbe() {}

    static String probe() {
        String[] candidates = {
            "io.github.bonede.tree_sitter.TreeSitter",
            "org.treesitter.TSParser",
            "com.github.javaparser.treesitter.Parser"
        };
        for (String name : candidates) {
            try {
                Class.forName(name);
                return "- tree-sitter class `" + name + "` is on the classpath. "
                        + "Measure only with a side native toolchain; do not load it on the FX thread.";
            } catch (ClassNotFoundException ignored) {
                // try next
            }
        }
        return "- tree-sitter: not measured\n"
                + "- JDK: " + System.getProperty("java.version") + "\n"
                + "- Reason: no tree-sitter Java binding was on the spike classpath. "
                + "Common candidates were absent; native libraries were not loaded.\n"
                + "- Result: skip. Do not block the regex path.";
    }
}
