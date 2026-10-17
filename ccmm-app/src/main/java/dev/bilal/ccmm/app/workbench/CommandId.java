package dev.bilal.ccmm.app.workbench;

/** Stable command identifier. */
public record CommandId(String value) {
    public static final CommandId FILE_OPEN = new CommandId("file.open");
    public static final CommandId VIEW_TOGGLE_SIDE = new CommandId("view.toggleSide");
    public static final CommandId VIEW_TOGGLE_BOTTOM = new CommandId("view.toggleBottom");
    public static final CommandId VIEW_TOGGLE_ACTIVITY = new CommandId("view.toggleActivity");
    public static final CommandId VIEW_EXPLORER = new CommandId("view.explorer");
    public static final CommandId VIEW_SEARCH = new CommandId("view.search");
    public static final CommandId VIEW_OUTLINE = new CommandId("view.outline");
    public static final CommandId VIEW_SETTINGS = new CommandId("view.settings");
    public static final CommandId QUICK_OPEN = new CommandId("quick.open");
    public static final CommandId COMMAND_PALETTE = new CommandId("command.palette");
    public static final CommandId FIND = new CommandId("edit.find");
    public static final CommandId REPLACE = new CommandId("edit.replace");
    public static final CommandId GO_TO_LINE = new CommandId("edit.goToLine");
    public static final CommandId CLOSE_TAB = new CommandId("tab.close");
    public static final CommandId REOPEN_TAB = new CommandId("tab.reopen");
    public static final CommandId NAV_BACK = new CommandId("nav.back");
    public static final CommandId NAV_FORWARD = new CommandId("nav.forward");

    public CommandId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("command id is blank");
        }
    }
}
