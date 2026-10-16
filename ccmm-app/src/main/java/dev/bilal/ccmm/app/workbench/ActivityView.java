package dev.bilal.ccmm.app.workbench;

/** Side-bar views switched by the activity bar. */
public enum ActivityView {
    EXPLORER("Explorer", "Explorer (Ctrl+Shift+E)", "Ex"),
    SEARCH("Search", "Search (Ctrl+Shift+F)", "Se"),
    OUTLINE("Outline", "Outline (Ctrl+Shift+O)", "Ou"),
    COLLABORATION("Session", "Collaboration", "Co"),
    SETTINGS("Settings", "Settings (Ctrl+,)", "St");

    private final String title;
    private final String accessibleName;
    private final String glyph;

    ActivityView(String title, String accessibleName, String glyph) {
        this.title = title;
        this.accessibleName = accessibleName;
        this.glyph = glyph;
    }

    /** Side-bar title row. Collaboration shows as Session. */
    public String title() {
        return title;
    }

    public String accessibleName() {
        return accessibleName;
    }

    public String glyph() {
        return glyph;
    }
}
