package dev.bilal.ccmm.app.workbench;

/** Side-bar views switched by the activity bar. */
public enum ActivityView {
    EXPLORER("Explorer (Ctrl+Shift+E)", "Ex"),
    SEARCH("Search (Ctrl+Shift+F)", "Se"),
    OUTLINE("Outline (Ctrl+Shift+O)", "Ou"),
    COLLABORATION("Collaboration", "Co"),
    SETTINGS("Settings (Ctrl+,)", "St");

    private final String accessibleName;
    private final String glyph;

    ActivityView(String accessibleName, String glyph) {
        this.accessibleName = accessibleName;
        this.glyph = glyph;
    }

    public String accessibleName() {
        return accessibleName;
    }

    public String glyph() {
        return glyph;
    }
}
