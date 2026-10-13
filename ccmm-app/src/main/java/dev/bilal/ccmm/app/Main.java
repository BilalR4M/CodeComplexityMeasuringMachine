package dev.bilal.ccmm.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class Main extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("CCMM IDE");
        stage.setScene(new Scene(new StackPane(new Label("CCMM IDE")), 1100, 700));
        stage.show();
    }
}
