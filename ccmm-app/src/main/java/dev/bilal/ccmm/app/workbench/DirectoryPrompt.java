package dev.bilal.ccmm.app.workbench;

import java.nio.file.Path;
import java.util.Optional;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

/** Chooses a project directory. Tests supply a fixed path. */
@FunctionalInterface
public interface DirectoryPrompt {
    Optional<Path> choose(Window owner);

    static DirectoryPrompt chooser() {
        return owner -> {
            DirectoryChooser dialog = new DirectoryChooser();
            dialog.setTitle("Open project");
            var selected = dialog.showDialog(owner);
            return selected == null ? Optional.empty() : Optional.of(selected.toPath());
        };
    }

    static DirectoryPrompt fixed(Path path) {
        return owner -> Optional.ofNullable(path);
    }
}
