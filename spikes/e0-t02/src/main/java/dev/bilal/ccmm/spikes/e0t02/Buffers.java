package dev.bilal.ccmm.spikes.e0t02;

final class Buffers {
    static final int LINES = 10_000;
    static final int SAMPLES = 200;
    static final int FIVE_MB = 5 * 1024 * 1024;

    private Buffers() {}

    static String tenThousandLines() {
        StringBuilder text = new StringBuilder(LINES * 48);
        text.append("package sample;\n");
        text.append("public class Sample {\n");
        for (int i = 2; i < LINES - 1; i++) {
            text.append("    int value").append(i).append(" = ").append(i).append(" + 1; // kw\n");
        }
        text.append("}\n");
        return text.toString();
    }

    static String fiveMegabytes() {
        StringBuilder text = new StringBuilder(FIVE_MB + 64);
        text.append("package big;\npublic class Big {\n");
        int n = 0;
        while (text.length() < FIVE_MB) {
            text.append("  /** comment ").append(n).append(" */\n");
            text.append("  public int m").append(n).append("() { return ").append(n).append("; }\n");
            n++;
        }
        text.append("}\n");
        return text.toString();
    }
}
