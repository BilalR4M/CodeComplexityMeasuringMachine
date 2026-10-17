package dev.bilal.ccmm.app.workbench;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    /** Registration order. */
    public List<Command> all() {
        return List.copyOf(commands.values());
    }

    /**
     * Empty query: recently used ids first (when present), then the rest.
     * Non-empty: case-insensitive substring match on {@link Command#title()}.
     */
    public List<Command> filter(String query, List<String> recentIds) {
        List<Command> registered = all();
        if (query != null && !query.isBlank()) {
            String needle = query.strip().toLowerCase();
            List<Command> matched = new ArrayList<>();
            for (Command command : registered) {
                if (command.title().toLowerCase().contains(needle)) {
                    matched.add(command);
                }
            }
            return List.copyOf(matched);
        }
        List<Command> ordered = new ArrayList<>();
        if (recentIds != null) {
            for (String id : recentIds) {
                find(id).ifPresent(ordered::add);
            }
        }
        for (Command command : registered) {
            if (ordered.stream().noneMatch(c -> c.id().equals(command.id()))) {
                ordered.add(command);
            }
        }
        return List.copyOf(ordered);
    }
}
