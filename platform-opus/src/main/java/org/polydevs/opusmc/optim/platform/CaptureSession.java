package org.polydevs.opusmc.optim.platform;

import java.util.*;
import org.polydevs.opusmc.optim.api.Metrics;
import org.polydevs.opusmc.optim.bench.*;

/** Owns one capture's writer and seal. No callback is evidence of clean process shutdown. */
public final class CaptureSession {
    private final FrameCapture capture;
    private Thread writer;
    private String failure;
    private boolean finished;

    CaptureSession(FrameCapture capture) { this.capture = capture; }

    synchronized void record(long durationNanos) {
        if (finished || failure != null) { return; }
        if (durationNanos <= 0) { failure = "INVALID_FRAME_DURATION"; return; }
        Thread current = Thread.currentThread();
        if (writer == null) { writer = current; }
        else if (writer != current) { failure = "MULTIPLE_FRAME_THREADS"; return; }
        capture.record(durationNanos);
    }

    public synchronized BenchmarkRun finish(BenchmarkRun.Termination termination, Metrics counters) {
        if (finished) { throw new IllegalStateException("O2 capture is already sealed"); }
        Objects.requireNonNull(termination, "termination");
        BenchmarkRun run = capture.finish(failure == null ? termination : BenchmarkRun.Termination.INTERRUPTED, counters);
        finished = true;
        writer = null;
        FrameTimingBridge.detach(this);
        return run;
    }

    public synchronized Optional<String> failureReason() { return Optional.ofNullable(failure); }
}
