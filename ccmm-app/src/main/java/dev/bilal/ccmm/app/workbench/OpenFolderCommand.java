package dev.bilal.ccmm.app.workbench;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.stage.Window;

/** Opens a project folder via {@link DirectoryPrompt}. */
public final class OpenFolderCommand implements Command {
    private final DirectoryPrompt prompt;
    private final Consumer<Path> openDirectory;
    private final java.util.function.Supplier<Window> owner;

    public OpenFolderCommand(
            DirectoryPrompt prompt, Consumer<Path> openDirectory, java.util.function.Supplier<Window> owner) {
        this.prompt = Objects.requireNonNull(prompt);
        this.openDirectory = Objects.requireNonNull(openDirectory);
        this.owner = Objects.requireNonNull(owner);
    }

    @Override
    public String id() {
        return CommandId.FILE_OPEN.value();
    }

    @Override
    public String title() {
        return "Open Folder…";
    }

    @Override
    public void run(Workbench workbench) {
        prompt.choose(owner.get()).ifPresent(openDirectory);
    }
}
