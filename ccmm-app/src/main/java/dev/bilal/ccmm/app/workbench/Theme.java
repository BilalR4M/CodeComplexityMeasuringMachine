package dev.bilal.ccmm.app.workbench;

/** Workbench theme. Style classes and sheets from ADR-0013. */
public enum Theme {
    DARK("theme-dark", "/theme/dark.css"),
    LIGHT("theme-light", "/theme/light.css");

    private final String styleClass;
    private final String stylesheet;

    Theme(String styleClass, String stylesheet) {
        this.styleClass = styleClass;
        this.stylesheet = stylesheet;
    }

    public String styleClass() {
        return styleClass;
    }

    public String stylesheetResource() {
        return stylesheet;
    }
}
