package org.polydevs.opusmc.optim.platform;

import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.polydevs.opusmc.bootstrap.*;
import org.polydevs.opusmc.optim.api.*;
import org.polydevs.opusmc.optim.core.*;
import static org.junit.jupiter.api.Assertions.*;

class ProviderPlanTest {
    @TempDir Path root;
    private final ClassLoader gameLoader = getClass().getClassLoader();

    @Test void missingConfigIsOffAndDoesNotResolveHelpers() {
        TransformerBinding binding = binding(descriptor("o2.probe", "fixture.Other", 0, ""), 200,
                Collections.singleton(Fixtures.Helper.class));
        ClassLoader blind = new ClassLoader(null) { };
        ProviderPlan prepared = prepare(new ConfigStore(root.resolve("missing.json")).load().config(),
                CrashMarker.Recovery.clean(), Collections.singleton(binding), Collections.emptyList(), blind);
        assertTrue(prepared.registrations().isEmpty());
        assertEquals(ExclusionReason.OPTIM_OFF, prepared.patchPlan().excluded().get("o2.probe").code());
        assertFalse(java.nio.file.Files.exists(root.resolve("missing.json")));
    }
    @Test void safeConfigRetainsItsReasonAndZeroRegistrations() {
        ProviderPlan prepared = prepare(OptimConfig.safe("fixture-corruption"), CrashMarker.Recovery.clean(),
                Collections.singleton(binding(descriptor("o2.probe", "fixture.Other", 0, ""), 200,
                        Collections.emptyList())), Collections.emptyList(), gameLoader);
        assertTrue(prepared.registrations().isEmpty());
        assertEquals("fixture-corruption", prepared.patchPlan().excluded().get("o2.probe").detail());
    }
    @Test void recoveryExcludesExperimentalBinding() throws Exception {
        CrashMarker marker = new CrashMarker(root.resolve("marker.json"));
        marker.begin("previous", Collections.singleton("o2.probe"));
        OptimizationDescriptor experimental = OptimizationDescriptor.builder("o2.probe", ActivationKind.STARTUP_PATCH)
                .status(OptimizationStatus.EXPERIMENTAL).correctnessScope("fixture").benchmarkScenario("fixture")
                .patchClaims(Collections.singleton(claim("fixture.Other", 0, ""))).build();
        ProviderPlan prepared = prepare(on("o2.probe"), marker.inspect(),
                Collections.singleton(binding(experimental, 200, Collections.emptyList())),
                Collections.emptyList(), gameLoader);
        assertTrue(prepared.registrations().isEmpty());
        assertEquals(ExclusionReason.CRASH_RECOVERY, prepared.patchPlan().excluded().get("o2.probe").code());
        assertTrue(java.nio.file.Files.exists(root.resolve("marker.json")));
    }
    @Test void refusesUnsupportedBindingsAndMissingClaimsOrMismatchedIds() {
        OptimizationDescriptor policy = OptimizationDescriptor.builder("o2.probe", ActivationKind.RUNTIME_POLICY)
                .correctnessScope("fixture").benchmarkScenario("fixture").build();
        assertThrows(IllegalArgumentException.class, () -> binding(policy, 200, Collections.emptyList()));
        OptimizationDescriptor noClaims = OptimizationDescriptor.builder("o2.probe", ActivationKind.STARTUP_PATCH)
                .correctnessScope("fixture").benchmarkScenario("fixture").build();
        assertThrows(IllegalArgumentException.class, () -> binding(noClaims, 200, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new TransformerBinding(
                descriptor("o2.probe", "fixture.Other", 0, ""), new Fixtures.Probe("other", 200),
                Collections.emptyList()));
    }
    @Test void exposesImmutableExactTargetsAndFiltersNonTargetTransformCalls() throws Exception {
        Fixtures.Probe delegate = new Fixtures.Probe("o2.probe", 200);
        TransformerBinding bound = new TransformerBinding(descriptor("o2.probe", "fixture/Other", 0, ""),
                delegate, Collections.emptyList());
        ProviderPlan prepared = prepare(on("o2.probe"), CrashMarker.Recovery.clean(),
                Collections.singleton(bound), Collections.emptyList(), gameLoader);
        TransformerRegistration registration = prepared.registrations().get(0);
        assertEquals("opus.optim", registration.providerId());
        assertEquals(Collections.singleton("fixture.Other"), registration.targetClasses());
        assertThrows(UnsupportedOperationException.class, () -> prepared.registrations().clear());
        assertThrows(UnsupportedOperationException.class, () -> registration.targetClasses().clear());
        byte[] bytes = new byte[]{1};
        assertSame(bytes, registration.transformer().transform("unrelated.Other", bytes));
        assertEquals(0, delegate.calls);
        registration.transformer().transform("fixture/Other", bytes);
        assertEquals(1, delegate.calls);
        delegate.name = "changed"; delegate.rank = -99;
        assertEquals("o2.probe", registration.transformer().id());
        assertEquals(200, registration.transformer().priority());
    }
    @Test void rejectsLoaderOrderThatReversesADependency() {
        OptimizationDescriptor dependent = OptimizationDescriptor.builder("o2.a", ActivationKind.STARTUP_PATCH)
                .requires(Collections.singleton("o2.b")).correctnessScope("fixture").benchmarkScenario("fixture")
                .patchClaims(Collections.singleton(claim("fixture.A", 0, ""))).build();
        List<TransformerBinding> candidates = Arrays.asList(binding(dependent, 100, Collections.emptyList()),
                binding(descriptor("o2.b", "fixture.B", 0, ""), 200, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> prepare(on("o2.a", "o2.b"),
                CrashMarker.Recovery.clean(), candidates, Collections.emptyList(), gameLoader));
    }
    @Test void rejectsCompositionOrderThatReversesBaselinePriority() {
        TransformerBinding candidate = binding(descriptor("o2.probe", "fixture.Other", 2, "receipt"),
                100, Collections.emptyList());
        TransformerBinding base = binding(descriptor("opus.base", "fixture.Other", 1, "receipt"),
                200, Collections.emptyList());
        assertThrows(IllegalArgumentException.class, () -> prepare(on("o2.probe"),
                CrashMarker.Recovery.clean(), Collections.singleton(candidate), Collections.singleton(base), gameLoader));
    }
    @Test void retainsACompatibleBaselineAndExcludesUnprovenComposition() {
        TransformerBinding base = binding(descriptor("opus.base", "fixture.Other", 1, "receipt"),
                100, Collections.emptyList());
        TransformerBinding good = binding(descriptor("o2.probe", "fixture.Other", 2, "receipt"),
                200, Collections.emptyList());
        assertEquals(1, prepare(on("o2.probe"), CrashMarker.Recovery.clean(), Collections.singleton(good),
                Collections.singleton(base), gameLoader).registrations().size());
        TransformerBinding unproven = binding(descriptor("o2.probe", "fixture.Other", 2, ""),
                200, Collections.emptyList());
        ProviderPlan excluded = prepare(on("o2.probe"), CrashMarker.Recovery.clean(), Collections.singleton(unproven),
                Collections.singleton(base), gameLoader);
        assertTrue(excluded.registrations().isEmpty());
        assertEquals(ExclusionReason.CONFLICT, excluded.patchPlan().excluded().get("o2.probe").code());
    }
    @Test void refusesDuplicateBindingsOrIdsCollidingWithBaseline() {
        TransformerBinding probe = binding(descriptor("o2.probe", "fixture.Other", 0, ""),
                200, Collections.emptyList());
        assertThrows(IllegalArgumentException.class, () -> prepare(on("o2.probe"), CrashMarker.Recovery.clean(),
                Arrays.asList(probe, probe), Collections.emptyList(), gameLoader));
        assertThrows(IllegalArgumentException.class, () -> prepare(on("o2.probe"), CrashMarker.Recovery.clean(),
                Collections.singleton(probe), Collections.singleton(probe), gameLoader));
    }
    @Test void candidateBaselineConflictExcludesItsDependentsAndPreservesTheBaseline() {
        OptimizationDescriptor incompatible = OptimizationDescriptor.builder("o2.probe", ActivationKind.STARTUP_PATCH)
                .conflicts(Collections.singleton("opus.base")).correctnessScope("fixture").benchmarkScenario("fixture")
                .patchClaims(Collections.singleton(claim("fixture.Candidate", 0, ""))).build();
        OptimizationDescriptor dependent = OptimizationDescriptor.builder("o2.dependent", ActivationKind.STARTUP_PATCH)
                .requires(Collections.singleton("o2.probe")).correctnessScope("fixture").benchmarkScenario("fixture")
                .patchClaims(Collections.singleton(claim("fixture.Dependent", 0, ""))).build();
        TransformerBinding base = binding(descriptor("opus.base", "fixture.Base", 0, ""), 100, Collections.emptyList());
        ProviderPlan plan = prepare(on("o2.probe", "o2.dependent"), CrashMarker.Recovery.clean(), Arrays.asList(
                binding(incompatible, 200, Collections.emptyList()), binding(dependent, 300, Collections.emptyList())),
                Collections.singleton(base), gameLoader);
        assertTrue(plan.registrations().isEmpty());
        assertEquals(ExclusionReason.CONFLICT, plan.patchPlan().excluded().get("o2.probe").code());
        assertTrue(plan.patchPlan().excluded().get("o2.probe").detail().contains("opus.base"));
        assertEquals(ExclusionReason.DEPENDENCY_DISABLED, plan.patchPlan().excluded().get("o2.dependent").code());
    }
    @Test void baselineCanDeclareTheConflictInTheOtherDirection() {
        OptimizationDescriptor baseline = OptimizationDescriptor.builder("opus.base", ActivationKind.STARTUP_PATCH)
                .conflicts(Collections.singleton("o2.probe")).correctnessScope("fixture").benchmarkScenario("fixture")
                .patchClaims(Collections.singleton(claim("fixture.Base", 0, ""))).build();
        ProviderPlan plan = prepare(on("o2.probe"), CrashMarker.Recovery.clean(), Collections.singleton(
                binding(descriptor("o2.probe", "fixture.Candidate", 0, ""), 200, Collections.emptyList())),
                Collections.singleton(binding(baseline, 100, Collections.emptyList())), gameLoader);
        assertTrue(plan.registrations().isEmpty());
        assertEquals(ExclusionReason.CONFLICT, plan.patchPlan().excluded().get("o2.probe").code());
    }
    @Test void refusesInvisibleAndDuplicateHelpersWithoutInitializingThem() throws Exception {
        Fixtures.initialized = false;
        TransformerBinding candidate = binding(descriptor("o2.probe", "fixture.Other", 0, ""), 200,
                Collections.singleton(Fixtures.InitializationWitness.class));
        prepare(on("o2.probe"), CrashMarker.Recovery.clean(), Collections.singleton(candidate),
                Collections.emptyList(), gameLoader);
        assertFalse(Fixtures.initialized);
        assertThrows(IllegalArgumentException.class, () -> prepare(on("o2.probe"), CrashMarker.Recovery.clean(),
                Collections.singleton(candidate), Collections.emptyList(), new ClassLoader(null) { }));
        ClassLoader duplicate = Fixtures.duplicateLoader(Fixtures.InitializationWitness.class);
        assertNotSame(Fixtures.InitializationWitness.class,
                Class.forName(Fixtures.InitializationWitness.class.getName(), false, duplicate));
        assertThrows(IllegalArgumentException.class, () -> prepare(on("o2.probe"), CrashMarker.Recovery.clean(),
                Collections.singleton(candidate), Collections.emptyList(), duplicate));
    }
    @Test void discoverySealsOffAndRejectsLateOrRepeatedInstall() {
        ProviderState off = new ProviderState();
        assertTrue(off.registrations().isEmpty());
        ProviderPlan plan = prepare(on("o2.probe"), CrashMarker.Recovery.clean(), Collections.singleton(
                binding(descriptor("o2.probe", "fixture.Other", 0, ""), 200, Collections.emptyList())),
                Collections.emptyList(), gameLoader);
        assertThrows(IllegalStateException.class, () -> off.install(plan));
        ProviderState active = new ProviderState();
        active.install(plan);
        assertThrows(IllegalStateException.class, () -> active.install(plan));
        assertSame(active.registrations(), active.registrations());
        assertEquals(1, active.registrations().size());
        assertThrows(IllegalStateException.class, () -> active.install(plan));
    }
    static OptimConfig on(String... ids) {
        Map<String, Boolean> flags = new HashMap<>();
        for (String id : ids) { flags.put(id, true); }
        return new OptimConfig(true, flags);
    }
    static PatchClaim claim(String target, int order, String receipt) {
        return new PatchClaim(target, "run()I", "return", PatchClaim.Effect.INSTRUMENTATION, receipt, order);
    }
    static OptimizationDescriptor descriptor(String id, String target, int order, String receipt) {
        return OptimizationDescriptor.builder(id, ActivationKind.STARTUP_PATCH).correctnessScope("fixture")
                .benchmarkScenario("fixture").patchClaims(Collections.singleton(claim(target, order, receipt))).build();
    }
    static TransformerBinding binding(OptimizationDescriptor descriptor, int priority, Collection<Class<?>> helpers) {
        return new TransformerBinding(descriptor, new Fixtures.Probe(descriptor.id(), priority), helpers);
    }
    static ProviderPlan prepare(OptimConfig config, CrashMarker.Recovery recovery,
            Collection<TransformerBinding> candidates, Collection<TransformerBinding> baseline, ClassLoader loader) {
        return ProviderPlan.prepare(config, new CapabilitySet(Collections.emptySet()), recovery, candidates, baseline, loader);
    }
}
