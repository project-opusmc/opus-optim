package org.polydevs.opusmc.optim.bench;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.polydevs.opusmc.optim.api.Metrics;
import static org.junit.jupiter.api.Assertions.*;

class FrameCaptureTest {
    @Test void discardsWarmupAndCountsOverflowWithoutReplacingOldFrames() {
        FrameCapture capture = capture(2, 2);
        for (long nanos : new long[]{10, 20, 30, 40, 50}) { capture.record(nanos); }
        BenchmarkRun run = capture.finish(BenchmarkRun.Termination.COMPLETE, new Metrics(Collections.emptyMap()));
        assertEquals(2, run.warmupObserved());
        assertEquals(3, run.observedSamples());
        assertEquals(1, run.droppedSamples());
        assertArrayEquals(new long[]{30, 40}, run.frameNanos());
        long[] returned = run.frameNanos();
        returned[0] = 999;
        assertArrayEquals(new long[]{30, 40}, run.frameNanos());
        assertThrows(IllegalStateException.class, () -> capture.record(60));
        assertThrows(IllegalStateException.class, () -> capture.finish(BenchmarkRun.Termination.COMPLETE,
                new Metrics(Collections.emptyMap())));
    }
    @Test void retainsIncompleteWarmupAndInterruptedRuns() {
        FrameCapture capture = capture(2, 2);
        capture.record(10);
        BenchmarkRun run = capture.finish(BenchmarkRun.Termination.INTERRUPTED, new Metrics(Collections.emptyMap()));
        assertEquals(1, run.warmupObserved());
        assertEquals(0, run.frameNanos().length);
        assertEquals(BenchmarkRun.Termination.INTERRUPTED, run.termination());
    }
    @Test void rejectsInvalidDurationAndBoundsWithoutConsumingWarmup() {
        FrameCapture capture = capture(1, 1);
        assertThrows(IllegalArgumentException.class, () -> capture.record(0));
        capture.record(10); capture.record(20);
        assertArrayEquals(new long[]{20}, capture.finish(BenchmarkRun.Termination.COMPLETE,
                new Metrics(Collections.emptyMap())).frameNanos());
        assertThrows(IllegalArgumentException.class, () -> capture(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> capture(0, 0));
        assertThrows(IllegalArgumentException.class, () -> capture(0, 1_000_001));
    }
    @Test void refusesIncompleteScopeAndKeepsItsFingerprintImmutable() {
        Map<String, String> fields = Fixtures.scopeFields();
        BenchmarkScope scope = new BenchmarkScope(fields);
        String fingerprint = scope.fingerprint();
        fields.put("cpu", "different");
        assertEquals(fingerprint, scope.fingerprint());
        assertNotEquals(fingerprint, new BenchmarkScope(fields).fingerprint());
        fields.remove("cpu");
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkScope(fields));
        fields.put("cpu", "cpu"); fields.put("sampling", "cpu-guessed");
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkScope(fields));
    }
    @Test void rejectsInconsistentRunCountsAndInvalidArtifactIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkRun(
                new RunIdentity("run", "session", Fixtures.CONTROL, RunIdentity.Origin.RECORDED),
                Fixtures.scope(), 0, 0, 2, 0, new long[]{10}, BenchmarkRun.Termination.COMPLETE,
                new Metrics(Collections.emptyMap())));
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkVariant("x", Fixtures.REV,
                "unknown", Fixtures.HASH_A));
    }
    private FrameCapture capture(int warmup, int capacity) {
        return new FrameCapture(new RunIdentity("run", "session", Fixtures.CONTROL,
                RunIdentity.Origin.SYNTHETIC), Fixtures.scope(), warmup, capacity);
    }
}
