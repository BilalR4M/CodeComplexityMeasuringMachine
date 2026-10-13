package dev.bilal.ccmm.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record GateRules(int cyclomatic, double maintainability, int nesting) {
    public static final String FILE_NAME = ".ccmm.yml";

    public static GateRules load(Path projectRoot) throws GateRuleException {
        if (projectRoot == null || !Files.isDirectory(projectRoot)) {
            throw new GateRuleException("project root is not a directory");
        }
        Path file = projectRoot.resolve(FILE_NAME);
        if (!Files.isRegularFile(file)) {
            throw new GateRuleException("missing .ccmm.yml");
        }
        try {
            return parse(Files.readString(file));
        } catch (IOException exception) {
            throw new GateRuleException("cannot read .ccmm.yml: " + exception.getMessage());
        }
    }

    public boolean exceedsCyclomatic(int value) {
        return value > cyclomatic;
    }

    public boolean exceedsNesting(int value) {
        return value > nesting;
    }

    public boolean belowMaintainability(double value) {
        return value < maintainability;
    }

    static GateRules parse(String text) throws GateRuleException {
        Integer cyclomatic = null;
        Double maintainability = null;
        Integer nesting = null;
        boolean inRules = false;
        for (String raw : text.split("\n", -1)) {
            String line = stripComment(raw.endsWith("\r") ? raw.substring(0, raw.length() - 1) : raw);
            if (line.isBlank()) {
                continue;
            }
            if (!inRules) {
                if (!"rules:".equals(line)) {
                    throw malformed("expected rules:");
                }
                inRules = true;
                continue;
            }
            if (line.charAt(0) != ' ') {
                throw malformed("expected a rule under rules:");
            }
            String body = line.stripLeading();
            if (body.length() == line.length() || line.substring(0, line.length() - body.length()).indexOf('\t') >= 0) {
                throw malformed("rules are indented with spaces");
            }
            int colon = body.indexOf(':');
            if (colon <= 0) {
                throw malformed("expected key: value");
            }
            String key = body.substring(0, colon).strip();
            String value = body.substring(colon + 1).strip();
            if (value.isEmpty()) {
                throw malformed("missing value for " + key);
            }
            switch (key) {
                case "cyclomatic" -> cyclomatic = assign(cyclomatic, integer(value, key), key);
                case "nesting" -> nesting = assign(nesting, integer(value, key), key);
                case "maintainability" -> maintainability = assign(maintainability, decimal(value), key);
                default -> throw malformed("unknown rule " + key);
            }
        }
        if (!inRules || cyclomatic == null || nesting == null || maintainability == null) {
            throw malformed("rules need cyclomatic, nesting, and maintainability");
        }
        return new GateRules(cyclomatic, maintainability, nesting);
    }

    private static <T> T assign(T current, T value, String key) throws GateRuleException {
        if (current != null) {
            throw malformed("duplicate rule " + key);
        }
        return value;
    }

    private static int integer(String value, String key) throws GateRuleException {
        if (!value.chars().allMatch(Character::isDigit) || value.length() > 9) {
            throw malformed(key + " must be a non-negative integer");
        }
        return Integer.parseInt(value);
    }

    private static double decimal(String value) throws GateRuleException {
        int dot = value.indexOf('.');
        String whole = dot < 0 ? value : value.substring(0, dot);
        String fraction = dot < 0 ? "" : value.substring(dot + 1);
        if (whole.isEmpty() || !whole.chars().allMatch(Character::isDigit) || !fraction.chars().allMatch(Character::isDigit)) {
            throw malformed("maintainability must be a non-negative number");
        }
        return Double.parseDouble(value);
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash < 0 ? line.stripTrailing() : line.substring(0, hash).stripTrailing();
    }

    private static GateRuleException malformed(String detail) {
        return new GateRuleException(".ccmm.yml is malformed: " + detail);
    }

    public static final class GateRuleException extends Exception {
        GateRuleException(String message) {
            super(message);
        }
    }
}
