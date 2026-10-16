package dev.bilal.ccmm.spikes.e0t01;

final class Buffers {
    static final int LINES = 10_000;
    static final int SAMPLES = 200;
    static final int FIVE_MB = 5 * 1024 * 1024;

    private Buffers() {}

    static String tenThousandLines() {
        StringBuilder text = new StringBuilder(LINES * 32);
        for (int i = 0; i < LINES; i++) {
            if (i > 0) {
                text.append('\n');
            }
            text.append("    int value").append(i).append(" = ").append(i).append(" + 1;");
        }
        return text.toString();
    }

    static String fiveMegabytes() {
        StringBuilder text = new StringBuilder(FIVE_MB + 64);
        int line = 0;
        while (text.length() < FIVE_MB) {
            text.append("line ").append(line++).append(" abcdefghijklmnopqrstuvwxyz0123456789\n");
        }
        return text.toString();
    }
}
