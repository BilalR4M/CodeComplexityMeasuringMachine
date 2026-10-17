package dev.bilal.ccmm.app.document;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Strict UTF-8 file reads — no Latin-1 fallback. */
public final class Utf8Text {
    public static final String ENCODING_LABEL = "UTF-8";

    private Utf8Text() {}

    public static String couldNotReadMessage(String fileName) {
        return "Could not read " + fileName + " as UTF-8";
    }

    /**
     * Decodes {@code bytes} as UTF-8, reporting malformed input.
     *
     * @throws CharacterCodingException when the bytes are not valid UTF-8
     */
    public static String decode(byte[] bytes) throws CharacterCodingException {
        CharsetDecoder decoder = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        return decoder.decode(ByteBuffer.wrap(bytes == null ? new byte[0] : bytes)).toString();
    }

    public static String read(Path file) throws IOException, CharacterCodingException {
        return decode(Files.readAllBytes(file));
    }
}
