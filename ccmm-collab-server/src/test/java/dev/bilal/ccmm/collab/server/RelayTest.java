package dev.bilal.ccmm.collab.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import dev.bilal.ccmm.collab.protocol.Message;
import dev.bilal.ccmm.collab.protocol.MessageCodec;
import dev.bilal.ccmm.collab.protocol.ServerAuthority;
import dev.bilal.ccmm.collab.protocol.TextOp;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class RelayTest {
    @Test
    void aWrongInviteTokenIsDisconnected() throws Exception {
        try (Relay relay = Relay.start("secret", new InetSocketAddress("127.0.0.1", 0));
                Peer ada = Peer.connect(relay.port());
                Peer eve = Peer.connect(relay.port())) {
            ada.send(new Message.Join("Ada", "secret"));
            ada.await(Message.Presence.class, "Ada");
            eve.send(new Message.Join("Eve", "nope"));

            eve.closed().get(3, TimeUnit.SECONDS);
            assertAbsent(ada, Message.Presence.class, "Eve");
        }
    }

    @Test
    void aMissingInviteTokenIsDisconnected() throws Exception {
        try (Relay relay = Relay.start("secret", new InetSocketAddress("127.0.0.1", 0));
                Peer eve = Peer.connect(relay.port())) {
            eve.sendRaw("{\"type\":\"join\",\"displayName\":\"Eve\"}");

            eve.closed().get(3, TimeUnit.SECONDS);
        }
    }

    @Test
    void presenceAndChatStayOnTheProjectRoom() throws Exception {
        try (Relay relay = Relay.start("secret", new InetSocketAddress("127.0.0.1", 0));
                Peer ada = Peer.connect(relay.port());
                Peer bob = Peer.connect(relay.port())) {
            ada.send(new Message.Join("Ada", "secret"));
            bob.send(new Message.Join("Bob", "secret"));

            ada.await(Message.Presence.class, "Bob");
            bob.await(Message.Presence.class, "Ada");
            ada.send(new Message.Chat("nope", "hi"));
            Message.Chat chat = bob.await(Message.Chat.class, 1);

            assertEquals("Ada", chat.displayName());
            assertEquals("hi", chat.text());
        }
    }

    @Test
    void aStaleInsertIsTransformedBeforeItIsBroadcast() throws Exception {
        try (Relay relay = Relay.start("secret", new InetSocketAddress("127.0.0.1", 0));
                Peer ada = Peer.connect(relay.port());
                Peer bob = Peer.connect(relay.port())) {
            ada.send(new Message.Join("Ada", "secret"));
            bob.send(new Message.Join("Bob", "secret"));
            ada.await(Message.Presence.class, "Ada");
            ada.send(new Message.Insert("A.java", 0, "hello", 0, "ada"));
            bob.await(Message.Insert.class, 1);
            bob.send(new Message.Insert("A.java", 0, "X", 0, "bob"));

            Message.Insert shifted = ada.await(Message.Insert.class, 2);
            assertEquals("A.java", shifted.path());
            assertEquals(5, shifted.index());
            assertEquals("X", shifted.text());
            assertEquals("bob", shifted.clientId());
            assertEquals("helloX", ServerAuthority.replay("", ops(ada)));
            assertEquals("helloX", ServerAuthority.replay("", ops(bob)));
            bob.send(new Message.Insert("B.java", 0, "Z", 0, "bob"));
            Message.Insert otherFile = ada.await(Message.Insert.class, 3);
            assertEquals("B.java", otherFile.path());
            assertEquals(0, otherFile.index());
        }
    }

    @Test
    void aParentSegmentIsNotBroadcast() throws Exception {
        try (Relay relay = Relay.start("secret", new InetSocketAddress("127.0.0.1", 0));
                Peer ada = Peer.connect(relay.port());
                Peer bob = Peer.connect(relay.port())) {
            ada.send(new Message.Join("Ada", "secret"));
            bob.send(new Message.Join("Bob", "secret"));
            ada.await(Message.Presence.class, "Ada");
            bob.await(Message.Presence.class, "Bob");
            ada.send(new Message.Insert("../A.java", 0, "X", 0, "ada"));

            ada.closed().get(3, TimeUnit.SECONDS);
            assertAbsent(bob, Message.Insert.class, null);
        }
    }

    private static List<TextOp> ops(Peer peer) {
        List<TextOp> ops = new ArrayList<>();
        for (Message message : peer.snapshot()) {
            if (message instanceof Message.Insert insert) {
                ops.add(insert.op());
            } else if (message instanceof Message.Delete delete) {
                ops.add(delete.op());
            }
        }
        return ops;
    }

    private static void assertAbsent(Peer peer, Class<? extends Message> type, String name) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(400);
        while (System.nanoTime() < deadline) {
            for (Message message : peer.snapshot()) {
                if (type.isInstance(message) && (name == null || name.equals(displayName(message)))) {
                    fail(type.getSimpleName() + " was broadcast");
                }
            }
            Thread.sleep(20);
        }
    }

    private static String displayName(Message message) {
        return switch (message) {
            case Message.Join join -> join.displayName();
            case Message.Presence presence -> presence.displayName();
            case Message.Chat chat -> chat.displayName();
            case Message.Insert ignored -> "";
            case Message.Delete ignored -> "";
        };
    }

    private static final class Peer implements WebSocket.Listener, AutoCloseable {
        private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        private final List<Message> messages = new ArrayList<>();
        private final StringBuilder partial = new StringBuilder();
        private final CompletableFuture<Integer> closed = new CompletableFuture<>();
        private WebSocket socket;

        static Peer connect(int port) {
            Peer peer = new Peer();
            peer.socket = peer.client.newWebSocketBuilder()
                    .buildAsync(URI.create("ws://127.0.0.1:" + port + "/"), peer)
                    .orTimeout(3, TimeUnit.SECONDS)
                    .join();
            return peer;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                Message message = MessageCodec.decode(partial.toString());
                partial.setLength(0);
                synchronized (messages) {
                    messages.add(message);
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            closed.complete(statusCode);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            closed.complete(-1);
        }

        void send(Message message) {
            sendRaw(MessageCodec.encode(message));
        }

        void sendRaw(String json) {
            socket.sendText(json, true).join();
        }

        CompletableFuture<Integer> closed() {
            return closed;
        }

        List<Message> snapshot() {
            synchronized (messages) {
                return List.copyOf(messages);
            }
        }

        <T extends Message> T await(Class<T> type, int count) {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (System.nanoTime() < deadline) {
                int seen = 0;
                for (Message message : snapshot()) {
                    if (type.isInstance(message)) {
                        seen++;
                        if (seen == count) {
                            return type.cast(message);
                        }
                    }
                }
                pause();
            }
            return fail("timed out waiting for " + type.getSimpleName() + " #" + count + " in " + snapshot());
        }

        <T extends Message> T await(Class<T> type, String name) {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (System.nanoTime() < deadline) {
                for (Message message : snapshot()) {
                    if (type.isInstance(message) && name.equals(displayName(message))) {
                        return type.cast(message);
                    }
                }
                pause();
            }
            return fail("timed out waiting for " + name + " in " + snapshot());
        }

        @Override
        public void close() {
            try {
                if (socket != null && !closed.isDone()) {
                    socket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").orTimeout(1, TimeUnit.SECONDS).join();
                }
            } catch (Exception ignored) {
                // the relay may already have closed this socket
            }
            client.close();
        }

        private static void pause() {
            try {
                Thread.sleep(10);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        }
    }
}
