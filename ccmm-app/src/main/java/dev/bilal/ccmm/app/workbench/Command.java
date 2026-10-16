package dev.bilal.ccmm.app.workbench;

/** A named action the menu, palette, and keymap can run. */
public interface Command {
    String id();

    String title();

    void run(Workbench workbench);
}
