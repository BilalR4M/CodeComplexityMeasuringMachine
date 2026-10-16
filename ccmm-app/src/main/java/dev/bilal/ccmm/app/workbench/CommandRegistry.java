package dev.bilal.ccmm.app.workbench;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Registers commands by id. Duplicate ids are rejected. */
public final class CommandRegistry {
    private final Map<String, Command> commands = new LinkedHashMap<>();

    public void register(Command command) {
        Objects.requireNonNull(command, "command");
        String id = command.id();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("command id is blank");
        }
        if (commands.containsKey(id)) {
            throw new IllegalStateException("duplicate command id: " + id);
        }
        commands.put(id, command);
    }

    public Optional<Command> find(String id) {
        return Optional.ofNullable(commands.get(id));
    }

    public boolean isRegistered(String id) {
        return commands.containsKey(id);
    }
}
