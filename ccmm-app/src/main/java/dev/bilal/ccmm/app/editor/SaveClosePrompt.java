package dev.bilal.ccmm.app.editor;

import java.util.Objects;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** Save / Don't Save / Cancel when closing a dirty tab. */
@FunctionalInterface
public interface SaveClosePrompt {
    enum Choice {
        SAVE,
        DONT_SAVE,
        CANCEL
    }

    Choice ask(String fileName, Window owner);

    static SaveClosePrompt always(Choice choice) {
        return (fileName, owner) -> choice;
    }

    static SaveClosePrompt dialog() {
        return (fileName, owner) -> {
            Objects.requireNonNull(fileName);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Save");
            alert.setHeaderText(null);
            alert.setContentText("Save changes to " + fileName + "?");
            if (owner != null) {
                alert.initOwner(owner);
            }
            ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.YES);
            ButtonType dont = new ButtonType("Don't Save", ButtonBar.ButtonData.NO);
            ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(save, dont, cancel);
            Optional<ButtonType> answer = alert.showAndWait();
            if (answer.isEmpty() || answer.get() == cancel) {
                return Choice.CANCEL;
            }
            if (answer.get() == save) {
                return Choice.SAVE;
            }
            return Choice.DONT_SAVE;
        };
    }
}
