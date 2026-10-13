package org.polydevs.opusmc.optim.smoke;

import java.nio.file.*;
import java.util.*;
import org.polydevs.opusmc.bootstrap.*;
import org.polydevs.opusmc.optim.api.*;
import org.polydevs.opusmc.optim.bench.*;
import org.polydevs.opusmc.optim.core.*;
import org.polydevs.opusmc.optim.platform.*;

/** Executable artifact check on an actual Java 8 VM. All durations/identities are synthetic. */
public final class BridgeSmoke {
    private BridgeSmoke() { }
    public static void main(String[] args) throws Exception {
        require(System.getProperty("java.version").startsWith("1.8."), "actual Java 8 VM required");
        OptimizationDescriptor descriptor = OptimizationDescriptor.builder("o2.fixture", ActivationKind.STARTUP_PATCH)
                .correctnessScope("synthetic").benchmarkScenario("synthetic")
                .patchClaims(Collections.singleton(new PatchClaim("fixture.Outside", "run()V", "entry",
                        PatchClaim.Effect.INSTRUMENTATION, "", 0))).build();
        ClassTransformer code = new ClassTransformer() {
            @Override public String id() { return "o2.fixture"; }
            @Override public int priority() { return 200; }
            @Override public byte[] transform(String name, byte[] original) { return new byte[]{1, 2}; }
        };
        ProviderPlan plan = ProviderPlan.prepare(new OptimConfig(true, Collections.singletonMap("o2.fixture", true)),
                new CapabilitySet(Collections.emptySet()), CrashMarker.Recovery.clean(),
                Collections.singleton(new TransformerBinding(descriptor, code, Collections.singleton(FrameTimingBridge.class))),
                Collections.emptyList(), BridgeSmoke.class.getClassLoader());
        OptimBootstrap.install(plan);
        TransformerPlan denied = TransformerPlan.discover(BridgeSmoke.class.getClassLoader(), Collections.emptySet(), Collections.emptyList());
        require(denied.transformerCount() == 0, "unapproved provider ran");
        TransformerPlan approved = TransformerPlan.discover(BridgeSmoke.class.getClassLoader(), Collections.singleton("opus.optim"), Collections.emptyList());
        require(approved.targets("fixture/Outside") && approved.transformerCount() == 1, "exact target routing");
        require(Arrays.equals(new byte[]{1, 2}, approved.chain().transform("fixture.Outside", new byte[]{1})), "transform dispatch");
        byte[] unrelated = {3};
        require(approved.chain().transform("fixture.Unrelated", unrelated) == unrelated, "target filtering");
        try { OptimBootstrap.install(plan); throw new AssertionError("late installation allowed"); }
        catch (IllegalStateException expected) { /* sealed */ }
        Map<String, String> fields = new TreeMap<>();
        for (String key : Arrays.asList("scenario", "fixture", "minecraft", "loader", "client", "java", "os",
                "cpu", "gpu", "gl", "resource-pack", "settings", "instrumentation")) { fields.put(key, "synthetic-" + key); }
        fields.put("sampling", "frame.wall.nanos.v1");
        String hash = String.join("", Collections.nCopies(64, "a"));
        RunIdentity identity = new RunIdentity("smoke", "synthetic-process", new BenchmarkVariant("synthetic",
                String.join("", Collections.nCopies(40, "a")), hash, hash), RunIdentity.Origin.SYNTHETIC);
        BenchmarkScope scope = new BenchmarkScope(fields);
        Metrics none = new Metrics(Collections.emptyMap());
        CaptureSession capture = FrameTimingBridge.start(identity, scope, 2, 2, BridgeSmoke.class.getClassLoader());
        for (long duration : new long[]{10, 20, 30, 40}) { FrameTimingBridge.frameCompleted(duration); }
        BenchmarkRun run = capture.finish(BenchmarkRun.Termination.COMPLETE, none);
        require(Arrays.equals(new long[]{30, 40}, run.frameNanos()), "warmup/raw frames");
        Path output = Files.createTempFile(Paths.get(args[0]), "synthetic-smoke-", ".jsonl");
        Files.delete(output);
        try {
            RunReports.writeNew(output, run);
            require(Arrays.equals(new long[]{30, 40}, RunReports.read(output).frameNanos()), "report roundtrip");
        } finally { Files.deleteIfExists(output); }
        CaptureSession invalid = FrameTimingBridge.start(identity, scope, 0, 2, BridgeSmoke.class.getClassLoader());
        FrameTimingBridge.frameCompleted(10);
        Thread foreign = new Thread(() -> FrameTimingBridge.frameCompleted(20)); foreign.start(); foreign.join();
        require(invalid.finish(BenchmarkRun.Termination.COMPLETE, none).termination() == BenchmarkRun.Termination.INTERRUPTED,
                "foreign writer accepted as complete");
        System.out.println("O2_JAVA8_BRIDGE_SMOKE_PASS java=" + System.getProperty("java.version") + " origin=SYNTHETIC");
    }
    private static void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
