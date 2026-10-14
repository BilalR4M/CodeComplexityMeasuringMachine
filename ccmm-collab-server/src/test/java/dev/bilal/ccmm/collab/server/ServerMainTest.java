package dev.bilal.ccmm.collab.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ServerMainTest {
    @Test
    void anExplicitTokenWinsAndABlankEnvironmentIsGeneratedLater() {
        assertEquals("secret", ServerMain.token(new String[] {"--token", "secret"}, "from-env"));
        assertEquals("from-env", ServerMain.token(new String[0], "from-env"));
        assertNull(ServerMain.token(new String[0], " "));
    }

    @Test
    void thePortComesFromTheArgumentOrDefaultsTo8080() {
        assertEquals(9000, ServerMain.port(new String[] {"--port", "9000"}, null));
        assertEquals(8080, ServerMain.port(new String[0], null));
        assertThrows(IllegalArgumentException.class, () -> ServerMain.port(new String[] {"--port", "nope"}, null));
    }
}
