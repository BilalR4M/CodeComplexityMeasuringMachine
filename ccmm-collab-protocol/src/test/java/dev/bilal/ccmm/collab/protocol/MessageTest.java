package dev.bilal.ccmm.collab.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MessageTest {
    @Test
    void joinCarriesTheInviteToken() {
        Message.Join join = new Message.Join("Ada", "secret");

        assertEquals("Ada", join.displayName());
        assertEquals("secret", join.inviteToken());
        assertThrows(IllegalArgumentException.class, () -> new Message.Join("Ada", " "));
    }

    @Test
    void presenceAndChatCarryADisplayName() {
        Message.Presence presence = new Message.Presence("Ada", true);
        Message.Chat chat = new Message.Chat("Ada", "hello");

        assertEquals(true, presence.online());
        assertEquals("hello", chat.text());
        assertThrows(IllegalArgumentException.class, () -> new Message.Chat(" ", "hello"));
        assertThrows(IllegalArgumentException.class, () -> new Message.Chat("Ada", null));
    }

    @Test
    void textOpsCarryTheRevisionTheyWereBasedOn() {
        Message.Insert insert = new Message.Insert("src/A.java", 1, "X", 3, "client-a");
        Message.Delete delete = new Message.Delete("src/A.java", 2, 4, 3, "client-b");

        assertEquals(3, insert.baseRevision());
        assertEquals(3, delete.baseRevision());
        assertEquals(insert.baseRevision(), insert.op().baseRevision());
        assertEquals(delete.baseRevision(), delete.op().baseRevision());
        assertEquals("src/A.java", insert.path());
        assertEquals(TextOp.Kind.INSERT, insert.op().kind());
        assertEquals(TextOp.Kind.DELETE, delete.op().kind());
    }
}
