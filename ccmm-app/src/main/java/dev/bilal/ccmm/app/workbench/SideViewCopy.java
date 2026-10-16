package dev.bilal.ccmm.app.workbench;

/** Empty-state copy for side views. No JavaFX types. */
public final class SideViewCopy {
    private SideViewCopy() {}

    public static String empty(ActivityView view, String folderName) {
        return switch (view) {
            case EXPLORER -> "No folder opened";
            case SEARCH -> folderName == null || folderName.isBlank()
                    ? "No folder opened"
                    : "Type to search in " + folderName;
            case OUTLINE -> "Open a file to see its outline";
            case COLLABORATION -> "Not sharing";
            case SETTINGS -> "Settings";
        };
    }
}
