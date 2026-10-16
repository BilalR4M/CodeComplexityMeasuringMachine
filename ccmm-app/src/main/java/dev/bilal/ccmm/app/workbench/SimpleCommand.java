package dev.bilal.ccmm.app.workbench;

import java.util.Objects;
import java.util.function.Consumer;

/** Command backed by a Runnable against the workbench. */
public final class SimpleCommand implements Command {
    private final String id;
    private final String title;
    private final Consumer<Workbench> action;

    public SimpleCommand(CommandId id, String title, Consumer<Workbench> action) {
        this.id = Objects.requireNonNull(id).value();
        this.title = Objects.requireNonNull(title);
        this.action = Objects.requireNonNull(action);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public void run(Workbench workbench) {
        action.accept(workbench);
    }
}
