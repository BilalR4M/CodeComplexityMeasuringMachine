package dev.bilal.ccmm.collab.server;

import dev.bilal.ccmm.collab.protocol.Message;
import dev.bilal.ccmm.collab.protocol.MessageCodec;
import dev.bilal.ccmm.collab.protocol.PeerPath;
import dev.bilal.ccmm.collab.protocol.ServerAuthority;
import dev.bilal.ccmm.collab.protocol.TextOp;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket relay for one project room and one operational-transform log per file.
 * A connection that has not presented the invite token is closed.
 */
public final class Relay implements AutoCloseable {
    static final int MAX_MESSAGE_BYTES = 1024 * 1024;
    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final byte[] POLICY_CLOSE = new byte[] {(byte) 0x03, (byte) 0xF0};

    private final String inviteToken;
    private final ServerSocket server;
    private final Thread acceptThread;
    private final AtomicBoolean closing = new AtomicBoolean();
    private final Object lock = new Object();
    private final List<Session> sessions = new ArrayList<>();
    private final Map<String, ServerAuthority> files = new HashMap<>();

    private Relay(String inviteToken, ServerSocket server) {
        this.inviteToken = inviteToken;
        this.server = server;
        this.acceptThread = new Thread(this::acceptLoop, "ccmm-relay-accept");
        this.acceptThread.start();
    }

    public static Relay start(String inviteToken, InetSocketAddress address) throws IOException {
        if (inviteToken == null || inviteToken.isBlank()) {
            throw new IllegalArgumentException("invite token is blank");
        }
        if (address == null) {
            throw new IllegalArgumentException("address is null");
        }
        ServerSocket server = new ServerSocket();
        server.bind(address);
        return new Relay(inviteToken, server);
    }

    public int port() {
        return server.getLocalPort();
    }

    public void await() throws InterruptedException {
        acceptThread.join();
    }

    @Override
    public void close() {
        if (!closing.compareAndSet(false, true)) {
            return;
        }
        try {
            server.close();
        } catch (IOException ignored) {
            // the accept loop exits when the socket closes
        }
        List<Session> copy;
        synchronized (lock) {
            copy = List.copyOf(sessions);
        }
        for (Session session : copy) {
            session.finish();
        }
        try {
            acceptThread.join(2000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void acceptLoop() {
        while (!closing.get()) {
            try {
                Socket socket = server.accept();
                socket.setTcpNoDelay(true);
                Session session = new Session(socket);
                synchronized (lock) {
                    if (closing.get()) {
                        socket.close();
                        return;
                    }
                    sessions.add(session);
                }
                Thread worker = new Thread(session, "ccmm-relay");
                worker.setDaemon(true);
                worker.start();
            } catch (IOException exception) {
                if (closing.get()) {
                    return;
                }
            }
        }
    }

    private List<Outbound> detach(Session self) {
        synchronized (lock) {
            sessions.remove(self);
            if (!self.joined) {
                return List.of();
            }
            self.joined = false;
            String json = MessageCodec.encode(new Message.Presence(self.name, false));
            List<Outbound> outbound = new ArrayList<>();
            for (Session other : sessions) {
                if (other.joined) {
                    outbound.add(new Outbound(other, json));
                }
            }
            return outbound;
        }
    }

    private boolean dispatch(Session self, Message message, List<Outbound> outbound) {
        return switch (message) {
            case Message.Join join -> join(self, join, outbound);
            case Message.Presence ignored -> false;
            case Message.Chat chat -> chat(self, chat, outbound);
            case Message.Insert insert -> edit(self, insert.path(), insert.op(), outbound, true);
            case Message.Delete delete -> edit(self, delete.path(), delete.op(), outbound, false);
        };
    }

    private boolean join(Session self, Message.Join join, List<Outbound> outbound) {
        if (!inviteToken.equals(join.inviteToken())) {
            return true;
        }
        if (self.joined) {
            return false;
        }
        self.name = join.displayName();
        self.joined = true;
        for (Session other : sessions) {
            if (other.joined && other != self) {
                outbound.add(new Outbound(self, MessageCodec.encode(new Message.Presence(other.name, true))));
            }
        }
        String hello = MessageCodec.encode(new Message.Presence(self.name, true));
        for (Session session : sessions) {
            if (session.joined) {
                outbound.add(new Outbound(session, hello));
            }
        }
        return false;
    }

    private boolean chat(Session self, Message.Chat chat, List<Outbound> outbound) {
        if (!self.joined) {
            return true;
        }
        String json = MessageCodec.encode(new Message.Chat(self.name, chat.text()));
        for (Session session : sessions) {
            if (session.joined) {
                outbound.add(new Outbound(session, json));
            }
        }
        return false;
    }

    private boolean edit(Session self, String peerPath, TextOp op, List<Outbound> outbound, boolean insert) {
        if (!self.joined) {
            return true;
        }
        String path;
        try {
            path = PeerPath.requireRelative(peerPath);
        } catch (IllegalArgumentException exception) {
            return true;
        }
        TextOp transformed;
        try {
            transformed = files.computeIfAbsent(path, ignored -> new ServerAuthority("")).receive(op);
        } catch (RuntimeException exception) {
            return true;
        }
        Message broadcast = insert
                ? new Message.Insert(path, transformed.index(), transformed.text(), transformed.baseRevision(), transformed.clientId())
                : new Message.Delete(path, transformed.index(), transformed.length(), transformed.baseRevision(), transformed.clientId());
        String json = MessageCodec.encode(broadcast);
        for (Session session : sessions) {
            if (session.joined) {
                outbound.add(new Outbound(session, json));
            }
        }
        return false;
    }

    private record Outbound(Session session, String json) {}

    private final class Session implements Runnable {
        private final Socket socket;
        private final InputStream in;
        private final OutputStream out;
        private final AtomicBoolean ended = new AtomicBoolean();
        private volatile boolean closeSent;
        private String name;
        private boolean joined;

        private Session(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedInputStream(socket.getInputStream());
            this.out = new BufferedOutputStream(socket.getOutputStream());
        }

        @Override
        public void run() {
            boolean upgraded = false;
            try {
                upgraded = handshake();
                if (upgraded) {
                    readFrames();
                }
            } catch (IOException exception) {
                // the peer left, or close() shut the socket
            } finally {
                if (upgraded) {
                    finish();
                } else {
                    drop();
                }
            }
        }

        private void handle(String text) {
            Message message;
            try {
                message = MessageCodec.decode(text);
            } catch (IllegalArgumentException exception) {
                reject();
                return;
            }
            List<Outbound> outbound = new ArrayList<>();
            boolean drop;
            synchronized (lock) {
                drop = dispatch(this, message, outbound);
            }
            for (Outbound item : outbound) {
                item.session.send(item.json);
            }
            if (drop) {
                reject();
            }
        }

        private void send(String json) {
            try {
                writeFrame(0x1, json.getBytes(StandardCharsets.UTF_8));
            } catch (IOException exception) {
                finish();
            }
        }

        private void reject() {
            end(POLICY_CLOSE);
        }

        private void finish() {
            end(new byte[0]);
        }

        private void drop() {
            if (!ended.compareAndSet(false, true)) {
                return;
            }
            try {
                socket.close();
            } catch (IOException ignored) {
                // the handshake never became a WebSocket
            }
            detach(this);
        }

        private void end(byte[] closePayload) {
            if (!ended.compareAndSet(false, true)) {
                return;
            }
            if (!closeSent) {
                closeSent = true;
                try {
                    writeFrame(0x8, closePayload);
                } catch (IOException ignored) {
                    // the peer may already be gone
                }
            }
            try {
                socket.close();
            } catch (IOException ignored) {
                // closing is enough
            }
            for (Outbound outbound : detach(this)) {
                outbound.session.send(outbound.json);
            }
        }

        private boolean handshake() throws IOException {
            String request = readHeader();
            if (request == null || !request.startsWith("GET ")) {
                rejectHandshake();
                return false;
            }
            String key = header(request, "Sec-WebSocket-Key");
            String upgrade = header(request, "Upgrade");
            if (key == null || key.isBlank()
                    || upgrade == null || !upgrade.toLowerCase(Locale.ROOT).contains("websocket")) {
                rejectHandshake();
                return false;
            }
            String response = "HTTP/1.1 101 Switching Protocols\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + acceptKey(key.trim()) + "\r\n\r\n";
            out.write(response.getBytes(StandardCharsets.US_ASCII));
            out.flush();
            return true;
        }

        private void rejectHandshake() throws IOException {
            byte[] body = "bad handshake\n".getBytes(StandardCharsets.UTF_8);
            String response = "HTTP/1.1 400 Bad Request\r\nConnection: close\r\nContent-Length: "
                    + body.length + "\r\n\r\n";
            out.write(response.getBytes(StandardCharsets.US_ASCII));
            out.write(body);
            out.flush();
        }

        private void readFrames() throws IOException {
            ByteArrayOutputStream payload = new ByteArrayOutputStream();
            while (!ended.get()) {
                int first = in.read();
                if (first < 0) {
                    return;
                }
                int second = in.read();
                if (second < 0) {
                    return;
                }
                boolean fin = (first & 0x80) != 0;
                int opcode = first & 0x0F;
                boolean masked = (second & 0x80) != 0;
                long length = second & 0x7F;
                if (length == 126) {
                    length = readUnsignedShort();
                } else if (length == 127) {
                    length = readUnsignedLong();
                }
                if (!masked || length < 0 || length > MAX_MESSAGE_BYTES || payload.size() + length > MAX_MESSAGE_BYTES) {
                    reject();
                    return;
                }
                byte[] mask = readFully(4);
                byte[] data = readFully((int) length);
                for (int i = 0; i < data.length; i++) {
                    data[i] = (byte) (data[i] ^ mask[i & 3]);
                }
                if (opcode == 0x8) {
                    closeSent = true;
                    writeFrame(0x8, new byte[0]);
                    return;
                }
                if (opcode == 0x9) {
                    if (length > 125) {
                        reject();
                        return;
                    }
                    writeFrame(0xA, data);
                    continue;
                }
                if (opcode == 0xA) {
                    continue;
                }
                if (opcode == 0x1) {
                    payload.reset();
                } else if (opcode != 0x0) {
                    reject();
                    return;
                }
                payload.write(data);
                if (fin) {
                    handle(payload.toString(StandardCharsets.UTF_8));
                    payload.reset();
                    if (ended.get()) {
                        return;
                    }
                }
            }
        }

        private String readHeader() throws IOException {
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            while (header.size() < 8192) {
                int b = in.read();
                if (b < 0) {
                    throw new EOFException();
                }
                header.write(b);
                byte[] bytes = header.toByteArray();
                int n = bytes.length;
                if (n >= 4 && bytes[n - 4] == '\r' && bytes[n - 3] == '\n' && bytes[n - 2] == '\r' && bytes[n - 1] == '\n') {
                    return header.toString(StandardCharsets.ISO_8859_1);
                }
            }
            return null;
        }

        private int readUnsignedShort() throws IOException {
            int hi = in.read();
            int lo = in.read();
            if (hi < 0 || lo < 0) {
                throw new EOFException();
            }
            return (hi << 8) | lo;
        }

        private long readUnsignedLong() throws IOException {
            long value = 0;
            for (int i = 0; i < 8; i++) {
                int b = in.read();
                if (b < 0) {
                    throw new EOFException();
                }
                value = (value << 8) | b;
            }
            return value;
        }

        private byte[] readFully(int length) throws IOException {
            byte[] data = new byte[length];
            int offset = 0;
            while (offset < length) {
                int read = in.read(data, offset, length - offset);
                if (read < 0) {
                    throw new EOFException();
                }
                offset += read;
            }
            return data;
        }

        private void writeFrame(int opcode, byte[] data) throws IOException {
            synchronized (out) {
                out.write(0x80 | opcode);
                long length = data.length;
                if (length < 126) {
                    out.write((int) length);
                } else if (length <= 0xFFFF) {
                    out.write(126);
                    out.write((int) (length >>> 8));
                    out.write((int) length);
                } else {
                    out.write(127);
                    for (int shift = 56; shift >= 0; shift -= 8) {
                        out.write((int) ((length >>> shift) & 0xFF));
                    }
                }
                out.write(data);
                out.flush();
            }
        }
    }

    private static String header(String request, String name) {
        String[] lines = request.split("\r\n");
        String prefix = name.toLowerCase(Locale.ROOT) + ":";
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    private static String acceptKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest((key + WEBSOCKET_GUID).getBytes(StandardCharsets.US_ASCII));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
