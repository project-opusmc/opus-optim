package org.polydevs.opusmc.optim.platform;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.polydevs.opusmc.optim.api.Metrics;
import org.polydevs.opusmc.optim.bench.*;
import static org.junit.jupiter.api.Assertions.*;

class FrameTimingBridgeTest {
    @TempDir Path root;
    private final ClassLoader game = getClass().getClassLoader();
    private final Metrics noCounters = new Metrics(Collections.emptyMap());

    @Test void noCaptureIsNoOpAndASecondCaptureCannotReplaceTheFirst() {
        FrameTimingBridge.frameCompleted(0);
        CaptureSession session = start(0, 2);
        try {
            assertThrows(IllegalStateException.class, () -> start(0, 2));
            FrameTimingBridge.frameCompleted(10);
        } finally {
            assertArrayEquals(new long[]{10}, session.finish(BenchmarkRun.Termination.COMPLETE, noCounters).frameNanos());
        }
    }
    @Test void retainsWarmupOverflowAndSyntheticIdentityInAReportRoundtrip() throws Exception {
        CaptureSession session = start(2, 2);
        for (long frame : new long[]{10, 20, 30, 40, 50}) { FrameTimingBridge.frameCompleted(frame); }
        BenchmarkRun run = session.finish(BenchmarkRun.Termination.COMPLETE, noCounters);
        assertArrayEquals(new long[]{30, 40}, run.frameNanos());
        assertEquals(2, run.warmupObserved()); assertEquals(3, run.observedSamples()); assertEquals(1, run.droppedSamples());
        assertEquals(RunIdentity.Origin.SYNTHETIC, run.identity().origin());
        assertFalse(session.failureReason().isPresent());
        Path report = root.resolve("synthetic.jsonl");
        RunReports.writeNew(report, run);
        assertArrayEquals(new long[]{30, 40}, RunReports.read(report).frameNanos());
    }
    @Test void invalidDurationStopsCaptureAndDowngradesCompletion() {
        CaptureSession session = start(0, 2);
        FrameTimingBridge.frameCompleted(10); FrameTimingBridge.frameCompleted(0); FrameTimingBridge.frameCompleted(30);
        BenchmarkRun run = session.finish(BenchmarkRun.Termination.COMPLETE, noCounters);
        assertArrayEquals(new long[]{10}, run.frameNanos());
        assertEquals(BenchmarkRun.Termination.INTERRUPTED, run.termination());
        assertEquals(Optional.of("INVALID_FRAME_DURATION"), session.failureReason());
    }
    @Test void aDifferentWriterInvalidatesWithoutThrowingIntoTelemetry() throws Exception {
        CaptureSession session = start(0, 2);
        FrameTimingBridge.frameCompleted(10);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread other = new Thread(() -> {
            try { FrameTimingBridge.frameCompleted(20); } catch (Throwable thrown) { failure.set(thrown); }
        });
        other.start(); other.join();
        FrameTimingBridge.frameCompleted(30);
        BenchmarkRun run = session.finish(BenchmarkRun.Termination.COMPLETE, noCounters);
        assertNull(failure.get()); assertArrayEquals(new long[]{10}, run.frameNanos());
        assertEquals(BenchmarkRun.Termination.INTERRUPTED, run.termination());
        assertEquals(Optional.of("MULTIPLE_FRAME_THREADS"), session.failureReason());
    }
    @Test void concurrentFinishAndStaleCallbacksCannotMutateASealedReport() throws Exception {
        CaptureSession session = start(0, 100);
        CountDownLatch first = new CountDownLatch(1), continueFrames = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread renderer = new Thread(() -> {
            try {
                FrameTimingBridge.frameCompleted(10); first.countDown(); continueFrames.await();
                for (int i = 0; i < 1000; i++) { FrameTimingBridge.frameCompleted(10); }
            } catch (Throwable thrown) { failure.set(thrown); }
        });
        renderer.start(); first.await(); continueFrames.countDown();
        BenchmarkRun run = session.finish(BenchmarkRun.Termination.COMPLETE, noCounters);
        renderer.join();
        long[] sealed = run.frameNanos();
        FrameTimingBridge.frameCompleted(999);
        assertNull(failure.get()); assertArrayEquals(sealed, run.frameNanos());
        assertTrue(sealed.length >= 1 && sealed.length <= 100);
        for (long value : sealed) { assertEquals(10, value); }
        assertEquals(run.observedSamples() - sealed.length, run.droppedSamples());
    }
    @Test void finishIsOnceOnlyAndTheNextSessionCanStart() {
        CaptureSession first = start(0, 2);
        BenchmarkRun report = first.finish(BenchmarkRun.Termination.INTERRUPTED, noCounters);
        assertEquals(BenchmarkRun.Termination.INTERRUPTED, report.termination());
        assertThrows(IllegalStateException.class, () -> first.finish(BenchmarkRun.Termination.COMPLETE, noCounters));
        CaptureSession second = start(0, 2);
        FrameTimingBridge.frameCompleted(20);
        assertArrayEquals(new long[]{20}, second.finish(BenchmarkRun.Termination.COMPLETE, noCounters).frameNanos());
    }
    @Test void helpersMustBeVisibleAsTheExactSameClassesBeforeAttaching() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> FrameTimingBridge.start(identity(), scope(), 0, 2,
                new ClassLoader(null) { }));
        assertThrows(IllegalArgumentException.class, () -> FrameTimingBridge.start(identity(), scope(), 0, 2,
                Fixtures.duplicateLoader(FrameTimingBridge.class)));
        CaptureSession session = start(0, 2);
        assertEquals(0, session.finish(BenchmarkRun.Termination.INTERRUPTED, noCounters).frameNanos().length);
    }
    private CaptureSession start(int warmup, int capacity) {
        return FrameTimingBridge.start(identity(), scope(), warmup, capacity, game);
    }
    static RunIdentity identity() {
        String sha = String.join("", Collections.nCopies(64, "a"));
        return new RunIdentity("fixture-run", "fixture-process", new BenchmarkVariant("fixture-control",
                String.join("", Collections.nCopies(40, "a")), sha, sha), RunIdentity.Origin.SYNTHETIC);
    }
    static BenchmarkScope scope() {
        Map<String, String> fields = new TreeMap<>();
        for (String key : Arrays.asList("scenario", "fixture", "minecraft", "loader", "client", "java",
                "os", "cpu", "gpu", "gl", "resource-pack", "settings", "instrumentation")) {
            fields.put(key, "synthetic-" + key);
        }
        fields.put("sampling", "frame.wall.nanos.v1");
        return new BenchmarkScope(fields);
    }
}
