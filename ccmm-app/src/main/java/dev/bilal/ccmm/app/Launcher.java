package dev.bilal.ccmm.app;

import javafx.application.Application;

/** Non-Application entry point so JavaFX works from a classpath-based jpackage image. */
public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}
