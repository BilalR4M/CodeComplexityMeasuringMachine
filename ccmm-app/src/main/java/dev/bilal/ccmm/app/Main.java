package dev.bilal.ccmm.app;

import java.nio.file.Path;
import java.util.List;
import javafx.application.Application;
import javafx.stage.Stage;

public final class Main extends Application {
    private IdeWindow window;

    @Override
    public void start(Stage stage) {
        window = new IdeWindow();
        stage.setTitle("CCMM IDE");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(window.scene());
        stage.setOnCloseRequest(event -> window.close());
        stage.show();
        List<String> args = getParameters().getRaw();
        if (args.size() == 1) {
            window.open(Path.of(args.get(0)));
        } else {
            window.restoreSession();
        }
    }
}
