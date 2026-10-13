package org.polydevs.opusmc.optim.bench;

import java.util.Arrays;
import java.util.OptionalDouble;

/** Exact nearest-rank statistics of one raw run. No aggregation of window percentiles. */
public final class FrameStatistics {
    private final long[] sorted;
    private final double meanNanos;
    private FrameStatistics(long[] frames) {
        if (frames == null || frames.length == 0 || frames.length > BenchmarkRun.MAX_SAMPLES) {
            throw new IllegalArgumentException("Raw frame distribution required");
        }
        sorted = frames.clone();
        double sum = 0;
        for (long nanos : sorted) {
            if (nanos <= 0) { throw new IllegalArgumentException("Duration must be positive"); }
            sum += nanos;
        }
        meanNanos = sum / sorted.length;
        Arrays.sort(sorted);
    }
    public static FrameStatistics of(long[] frames) { return new FrameStatistics(frames); }
    public double meanNanos() { return meanNanos; }
    public long percentileNanos(double percentile) {
        if (!(percentile > 0 && percentile <= 1)) { throw new IllegalArgumentException("Percentile outside (0,1]"); }
        return sorted[(int) Math.ceil(percentile * sorted.length) - 1];
    }
    public OptionalDouble onePercentLowFps() { return low(100); }
    public OptionalDouble pointOnePercentLowFps() { return low(1000); }
    private OptionalDouble low(int divisor) {
        if (sorted.length < divisor) { return OptionalDouble.empty(); }
        int count = (sorted.length + divisor - 1) / divisor;
        double sum = 0;
        for (int i = sorted.length - count; i < sorted.length; i++) { sum += sorted[i]; }
        return OptionalDouble.of(1e9 / (sum / count));
    }
}
