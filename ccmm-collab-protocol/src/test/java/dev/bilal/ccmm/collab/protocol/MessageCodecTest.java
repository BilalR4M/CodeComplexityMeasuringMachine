package dev.bilal.ccmm.collab.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MessageCodecTest {
    @Test
    void eachMessageRoundTrips() {
        assertRoundTrip(new Message.Join("Ada", "secret"));
        assertRoundTrip(new Message.Presence("Ada", true));
        assertRoundTrip(new Message.Presence("Ada", false));
        assertRoundTrip(new Message.Chat("Ada", "hello"));
        assertRoundTrip(new Message.Insert("src/A.java", 2, "xy", 1, "ada"));
        assertRoundTrip(new Message.Delete("src/A.java", 2, 4, 1, "ada"));
    }

    @Test
    void quotesAndNewlinesStayInsideTheString() {
        Message.Chat chat = new Message.Chat("Ada", "say \"hi\"\n");

        Message decoded = MessageCodec.decode(MessageCodec.encode(chat));

        assertEquals(chat, decoded);
    }

    @Test
    void aMissingTokenAndAnUnknownTypeAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> MessageCodec.decode("{\"type\":\"join\",\"displayName\":\"Ada\"}"));
        assertThrows(IllegalArgumentException.class, () -> MessageCodec.decode("{\"type\":\"tree\"}"));
    }

    private static void assertRoundTrip(Message message) {
        assertEquals(message, MessageCodec.decode(MessageCodec.encode(message)));
    }
}
