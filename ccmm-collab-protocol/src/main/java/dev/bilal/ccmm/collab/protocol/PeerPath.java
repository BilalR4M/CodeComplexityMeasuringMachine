package dev.bilal.ccmm.collab.protocol;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Checks a path received from a peer before the IDE or the relay uses it.
 * The path must be relative, must not contain a {@code ..} segment, and its
 * real path must stay inside the project root.
 */
public final class PeerPath {
    private PeerPath() {}

    public static String require(Path projectRoot, String peerPath) {
        if (projectRoot == null || peerPath == null || peerPath.isBlank()) {
            throw new IllegalArgumentException("path is blank");
        }
        if (!Files.isDirectory(projectRoot)) {
            throw new IllegalArgumentException("project root is not a directory");
        }
        String relative = requireRelative(peerPath);
        Path rootReal = realRoot(projectRoot);
        Path cursor = rootReal;
        for (String segment : relative.split("/")) {
            cursor = cursor.resolve(segment);
            if (!Files.exists(cursor, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            Path real = real(cursor);
            if (!real.startsWith(rootReal)) {
                throw new IllegalArgumentException("path escapes the project root");
            }
            cursor = real;
        }
        if (!cursor.normalize().startsWith(rootReal)) {
            throw new IllegalArgumentException("path escapes the project root");
        }
        return relative;
    }

    /**
     * Checks the path text only. The relay calls this because it does not
     * have the project tree. {@link #require(Path, String)} also rejects a
     * symlink that leaves the project root.
     */
    public static String requireRelative(String peerPath) {
        if (peerPath == null || peerPath.isBlank()) {
            throw new IllegalArgumentException("path is blank");
        }
        String slashed = peerPath.replace('\\', '/');
        if (isAbsolute(slashed)) {
            throw new IllegalArgumentException("absolute path");
        }
        String[] raw = slashed.split("/");
        StringBuilder relative = new StringBuilder();
        for (String segment : raw) {
            if (segment.isEmpty() || segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                throw new IllegalArgumentException("parent segment");
            }
            if (relative.length() > 0) {
                relative.append('/');
            }
            relative.append(segment);
        }
        if (relative.isEmpty()) {
            throw new IllegalArgumentException("path is blank");
        }
        return relative.toString();
    }

    private static boolean isAbsolute(String slashed) {
        if (slashed.startsWith("/")) {
            return true;
        }
        return slashed.length() >= 2 && Character.isLetter(slashed.charAt(0)) && slashed.charAt(1) == ':';
    }

    private static Path realRoot(Path projectRoot) {
        try {
            return projectRoot.toRealPath();
        } catch (IOException exception) {
            throw new IllegalArgumentException("project root is not a directory");
        }
    }

    private static Path real(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException exception) {
            throw new IllegalArgumentException("path escapes the project root");
        }
    }
}
