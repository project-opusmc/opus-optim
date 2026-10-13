package org.polydevs.opusmc.optim.platform;

import org.polydevs.opusmc.optim.bench.*;

/** Optional diagnostic callback. The host supplies its already measured wall-frame interval. */
public final class FrameTimingBridge {
    private static final Object LOCK = new Object();
    private static CaptureSession active;
    private FrameTimingBridge() { }

    public static CaptureSession start(RunIdentity identity, BenchmarkScope scope, int warmup, int capacity,
            ClassLoader gameLoader) {
        TransformerBinding.verifyHelper(FrameTimingBridge.class, gameLoader);
        TransformerBinding.verifyHelper(CaptureSession.class, gameLoader);
        synchronized (LOCK) {
            if (active != null) { throw new IllegalStateException("An O2 capture is already attached"); }
            active = new CaptureSession(new FrameCapture(identity, scope, warmup, capacity));
            return active;
        }
    }

    /** No clock read, allocation, GL call, game-class load or file I/O on a valid frame. */
    public static void frameCompleted(long durationNanos) {
        CaptureSession session;
        synchronized (LOCK) { session = active; }
        if (session != null) { session.record(durationNanos); }
    }

    static void detach(CaptureSession session) {
        synchronized (LOCK) {
            if (active == session) { active = null; }
        }
    }
}
