package dev.bilal.ccmm.app.workbench;

/** Stable command identifier. */
public record CommandId(String value) {
    public static final CommandId FILE_OPEN = new CommandId("file.open");

    public CommandId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("command id is blank");
        }
    }
}
