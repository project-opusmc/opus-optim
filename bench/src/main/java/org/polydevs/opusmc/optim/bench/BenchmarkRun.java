package org.polydevs.opusmc.optim.bench;

import java.util.Objects;
import org.polydevs.opusmc.optim.api.Metrics;

/** Immutable whole-session raw distribution, including incomplete runs. */
public final class BenchmarkRun {
    public enum Termination { COMPLETE, INTERRUPTED }
    public static final int MAX_SAMPLES = 1_000_000;
    private final RunIdentity identity;
    private final BenchmarkScope scope;
    private final int warmupRequested, warmupObserved;
    private final long observedSamples, droppedSamples;
    private final long[] frameNanos;
    private final Termination termination;
    private final Metrics counters;
    public BenchmarkRun(RunIdentity identity, BenchmarkScope scope, int warmupRequested, int warmupObserved,
            long observedSamples, long droppedSamples, long[] frameNanos, Termination termination, Metrics counters) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.termination = Objects.requireNonNull(termination, "termination");
        this.counters = Objects.requireNonNull(counters, "counters");
        Objects.requireNonNull(frameNanos, "raw frames");
        if (warmupRequested < 0 || warmupObserved < 0 || warmupObserved > warmupRequested
                || droppedSamples < 0 || observedSamples < droppedSamples
                || observedSamples - droppedSamples != frameNanos.length || frameNanos.length > MAX_SAMPLES
                || counters.counters().size() > 64) {
            throw new IllegalArgumentException("Invalid run counts/bounds");
        }
        this.frameNanos = frameNanos.clone();
        for (long nanos : this.frameNanos) {
            if (nanos <= 0) { throw new IllegalArgumentException("Frame durations must be positive"); }
        }
        this.warmupRequested = warmupRequested; this.warmupObserved = warmupObserved;
        this.observedSamples = observedSamples; this.droppedSamples = droppedSamples;
    }
    public RunIdentity identity() { return identity; }
    public BenchmarkScope scope() { return scope; }
    public int warmupRequested() { return warmupRequested; }
    public int warmupObserved() { return warmupObserved; }
    public long observedSamples() { return observedSamples; }
    public long droppedSamples() { return droppedSamples; }
    public long[] frameNanos() { return frameNanos.clone(); }
    public int sampleCount() { return frameNanos.length; }
    public Termination termination() { return termination; }
    public Metrics counters() { return counters; }
}
