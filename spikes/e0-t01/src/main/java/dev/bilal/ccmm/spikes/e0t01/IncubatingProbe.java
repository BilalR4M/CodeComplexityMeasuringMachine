package dev.bilal.ccmm.spikes.e0t01;

/**
 * Records whether the incubating JavaFX editor can be measured on this JDK.
 * Does not change the parent POM. A real measurement needs a side toolchain
 * with JavaFX 25+ and is recorded as not available when the class is absent.
 */
final class IncubatingProbe {
    private IncubatingProbe() {}

    static String probe() {
        try {
            Class.forName("jfx.incubator.scene.control.richtext.CodeArea");
            return "- JavaFX incubating `CodeArea` is on the classpath. "
                    + "This spike run did not switch the parent POM to load it; "
                    + "measure it with a side JavaFX 25+ toolchain before adopting it.";
        } catch (ClassNotFoundException missing) {
            return "- JavaFX: not measured on this run\n"
                    + "- Reason: `jfx.incubator.scene.control.richtext.CodeArea` is not on the "
                    + "JavaFX 21.0.5 classpath used by this repository (ADR-0012 recommends staying on 21). "
                    + "The incubating module appears in JavaFX 25/26 and may change or be removed. "
                    + "No side toolchain was applied to the parent POM.\n"
                    + "- Result: skip. RichTextFX remains the measured candidate.";
        }
    }
}
