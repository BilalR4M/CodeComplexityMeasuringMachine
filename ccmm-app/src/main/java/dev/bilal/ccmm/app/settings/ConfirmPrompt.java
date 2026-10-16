package dev.bilal.ccmm.app.settings;

import java.util.Objects;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** Confirms destructive settings actions. Tests inject a fixed answer. */
@FunctionalInterface
public interface ConfirmPrompt {
    boolean confirm(String message, Window owner);

    static ConfirmPrompt always(boolean value) {
        return (message, owner) -> value;
    }

    static ConfirmPrompt dialog() {
        return (message, owner) -> {
            Objects.requireNonNull(message);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Settings");
            alert.setHeaderText(null);
            alert.setContentText(message);
            if (owner != null) {
                alert.initOwner(owner);
            }
            ButtonType reset = new ButtonType("Reset", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(reset, cancel);
            Optional<ButtonType> answer = alert.showAndWait();
            return answer.isPresent() && answer.get() == reset;
        };
    }
}
