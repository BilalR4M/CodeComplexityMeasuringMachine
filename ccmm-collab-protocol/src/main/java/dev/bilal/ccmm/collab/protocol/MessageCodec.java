package dev.bilal.ccmm.collab.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One JSON object per WebSocket text frame. The IDE and the relay share this
 * encoding so a frame can be turned back into a {@link Message}.
 */
public final class MessageCodec {
    private MessageCodec() {}

    public static String encode(Message message) {
        return switch (message) {
            case Message.Join join -> object(
                    field("type", "join"),
                    field("displayName", join.displayName()),
                    field("inviteToken", join.inviteToken()));
            case Message.Presence presence -> object(
                    field("type", "presence"),
                    field("displayName", presence.displayName()),
                    raw("online", presence.online() ? "true" : "false"));
            case Message.Chat chat -> object(
                    field("type", "chat"),
                    field("displayName", chat.displayName()),
                    field("text", chat.text()));
            case Message.Insert insert -> object(
                    field("type", "insert"),
                    field("path", insert.path()),
                    raw("index", Integer.toString(insert.index())),
                    field("text", insert.text()),
                    raw("baseRevision", Integer.toString(insert.baseRevision())),
                    field("clientId", insert.clientId()));
            case Message.Delete delete -> object(
                    field("type", "delete"),
                    field("path", delete.path()),
                    raw("index", Integer.toString(delete.index())),
                    raw("length", Integer.toString(delete.length())),
                    raw("baseRevision", Integer.toString(delete.baseRevision())),
                    field("clientId", delete.clientId()));
        };
    }

    public static Message decode(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("message is blank");
        }
        Map<String, Object> fields = new Parser(json).object();
        String type = text(fields, "type");
        return switch (type) {
            case "join" -> new Message.Join(text(fields, "displayName"), text(fields, "inviteToken"));
            case "presence" -> new Message.Presence(text(fields, "displayName"), bool(fields, "online"));
            case "chat" -> new Message.Chat(text(fields, "displayName"), text(fields, "text"));
            case "insert" -> new Message.Insert(
                    text(fields, "path"),
                    integer(fields, "index"),
                    text(fields, "text"),
                    integer(fields, "baseRevision"),
                    text(fields, "clientId"));
            case "delete" -> new Message.Delete(
                    text(fields, "path"),
                    integer(fields, "index"),
                    integer(fields, "length"),
                    integer(fields, "baseRevision"),
                    text(fields, "clientId"));
            default -> throw new IllegalArgumentException("unknown message");
        };
    }

    private static String object(String... fields) {
        return "{" + String.join(",", fields) + "}";
    }

    private static String field(String name, String value) {
        return quote(name) + ":" + quote(value);
    }

    private static String raw(String name, String jsonValue) {
        return quote(name) + ":" + jsonValue;
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder(text.length() + 2);
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
        return out.toString();
    }

    private static String text(Map<String, Object> fields, String name) {
        Object value = fields.get(name);
        if (!(value instanceof String string)) {
            throw new IllegalArgumentException(name + " is missing");
        }
        return string;
    }

    private static int integer(Map<String, Object> fields, String name) {
        Object value = fields.get(name);
        if (!(value instanceof Integer number)) {
            throw new IllegalArgumentException(name + " is missing");
        }
        return number;
    }

    private static boolean bool(Map<String, Object> fields, String name) {
        Object value = fields.get(name);
        if (!(value instanceof Boolean flag)) {
            throw new IllegalArgumentException(name + " is missing");
        }
        return flag;
    }

    private static final class Parser {
        private final String json;
        private int index;

        private Parser(String json) {
            this.json = json;
        }

        private Map<String, Object> object() {
            skip();
            expect('{');
            Map<String, Object> fields = new LinkedHashMap<>();
            skip();
            if (peek('}')) {
                index++;
                end();
                return fields;
            }
            while (true) {
                skip();
                String key = string();
                skip();
                expect(':');
                skip();
                fields.put(key, value());
                skip();
                if (peek('}')) {
                    index++;
                    break;
                }
                expect(',');
            }
            end();
            return fields;
        }

        private Object value() {
            if (peek('"')) {
                return string();
            }
            if (peek('t')) {
                expectWord("true");
                return Boolean.TRUE;
            }
            if (peek('f')) {
                expectWord("false");
                return Boolean.FALSE;
            }
            if (peek('-') || digit()) {
                return number();
            }
            throw new IllegalArgumentException("value is invalid");
        }

        private String string() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (index < json.length()) {
                char c = json.charAt(index++);
                if (c == '"') {
                    return out.toString();
                }
                if (c == '\\') {
                    if (index >= json.length()) {
                        throw new IllegalArgumentException("string is not closed");
                    }
                    char escaped = json.charAt(index++);
                    switch (escaped) {
                        case '"', '\\', '/' -> out.append(escaped);
                        case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case 'u' -> out.append(unicode());
                        default -> throw new IllegalArgumentException("bad escape");
                    }
                } else if (c < 0x20) {
                    throw new IllegalArgumentException("control character");
                } else {
                    out.append(c);
                }
            }
            throw new IllegalArgumentException("string is not closed");
        }

        private char unicode() {
            if (index + 4 > json.length()) {
                throw new IllegalArgumentException("bad escape");
            }
            int code;
            try {
                code = Integer.parseInt(json.substring(index, index + 4), 16);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("bad escape");
            }
            index += 4;
            return (char) code;
        }

        private int number() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            if (!digit()) {
                throw new IllegalArgumentException("number is invalid");
            }
            while (digit()) {
                index++;
            }
            try {
                return Integer.parseInt(json.substring(start, index));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("number is invalid");
            }
        }

        private void expectWord(String word) {
            if (!json.startsWith(word, index)) {
                throw new IllegalArgumentException("value is invalid");
            }
            index += word.length();
        }

        private void expect(char c) {
            if (!peek(c)) {
                throw new IllegalArgumentException("message is invalid");
            }
            index++;
        }

        private boolean peek(char c) {
            return index < json.length() && json.charAt(index) == c;
        }

        private boolean digit() {
            return index < json.length() && Character.isDigit(json.charAt(index));
        }

        private void skip() {
            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }

        private void end() {
            skip();
            if (index != json.length()) {
                throw new IllegalArgumentException("trailing data");
            }
        }
    }
}
