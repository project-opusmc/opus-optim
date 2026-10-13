package org.polydevs.opusmc.optim.bench;

import java.util.Arrays;
import java.util.Objects;
import org.polydevs.opusmc.optim.api.Metrics;

/** Single-writer benchmark capture. record() allocates no objects and performs no I/O. */
public final class FrameCapture {
    private final RunIdentity identity;
    private final BenchmarkScope scope;
    private final int warmupRequested;
    private final long[] frames;
    private int warmupObserved, count;
    private long observedSamples;
    private boolean finished;
    public FrameCapture(RunIdentity identity, BenchmarkScope scope, int warmupFrames, int capacity) {
        if (warmupFrames < 0 || capacity < 1 || capacity > BenchmarkRun.MAX_SAMPLES) {
            throw new IllegalArgumentException("Invalid capture bounds");
        }
        this.identity = Objects.requireNonNull(identity, "identity");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.warmupRequested = warmupFrames;
        this.frames = new long[capacity];
    }
    public void record(long durationNanos) {
        if (finished) { throw new IllegalStateException("Capture is sealed"); }
        if (durationNanos <= 0) { throw new IllegalArgumentException("Duration must be positive"); }
        if (warmupObserved < warmupRequested) { warmupObserved++; return; }
        observedSamples = Math.addExact(observedSamples, 1);
        if (count < frames.length) { frames[count++] = durationNanos; }
    }
    public BenchmarkRun finish(BenchmarkRun.Termination termination, Metrics counters) {
        if (finished) { throw new IllegalStateException("Capture is sealed"); }
        BenchmarkRun run = new BenchmarkRun(identity, scope, warmupRequested, warmupObserved,
                observedSamples, observedSamples - count, Arrays.copyOf(frames, count), termination, counters);
        finished = true;
        return run;
    }
}
