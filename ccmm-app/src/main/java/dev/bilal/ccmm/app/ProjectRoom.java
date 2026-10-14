package dev.bilal.ccmm.app;

import dev.bilal.ccmm.collab.protocol.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * Who is in the project room, and what they have said.
 * Presence and chat never name a file.
 */
final class ProjectRoom {
    private final List<String> names = new ArrayList<>();
    private final List<String> chat = new ArrayList<>();

    void accept(Message message) {
        switch (message) {
            case Message.Presence presence -> {
                if (presence.online()) {
                    names.add(presence.displayName());
                } else {
                    names.remove(presence.displayName());
                }
            }
            case Message.Chat messageChat -> chat.add(messageChat.displayName() + ": " + messageChat.text());
            default -> {
                // join and text ops are not part of the project room
            }
        }
    }

    String text() {
        StringBuilder out = new StringBuilder("Here");
        for (String name : names) {
            out.append('\n').append(name);
        }
        for (String line : chat) {
            out.append('\n').append(line);
        }
        return out.toString();
    }
}
