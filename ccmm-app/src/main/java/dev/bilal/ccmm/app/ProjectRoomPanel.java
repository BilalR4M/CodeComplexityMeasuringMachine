package dev.bilal.ccmm.app;

import dev.bilal.ccmm.collab.protocol.Message;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

final class ProjectRoomPanel extends VBox {
    private final ProjectRoom room = new ProjectRoom();
    private final Label body = new Label();

    ProjectRoomPanel() {
        setPrefWidth(240);
        Label title = new Label("Project");
        body.setWrapText(true);
        getChildren().addAll(title, body);
        body.setText(room.text());
    }

    void acceptAll(List<Message> messages) {
        if (messages == null) {
            return;
        }
        for (Message message : messages) {
            room.accept(message);
        }
        body.setText(room.text());
    }

    String text() {
        return body.getText();
    }
}
