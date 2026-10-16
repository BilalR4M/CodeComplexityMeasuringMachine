package dev.bilal.ccmm.spikes.e0t01;

import java.util.Arrays;

/** Same nearest-rank rule as KeystrokeReport. Duplicated so the spike stays off the app classpath. */
final class Percentile {
    private Percentile() {}

    static double millis(long[] nanos, double percentile) {
        if (nanos.length == 0) {
            throw new IllegalArgumentException("no samples");
        }
        long[] sorted = nanos.clone();
        Arrays.sort(sorted);
        int index = (int) Math.ceil(percentile * sorted.length) - 1;
        index = Math.max(0, Math.min(sorted.length - 1, index));
        return sorted[index] / 1_000_000.0;
    }
}
