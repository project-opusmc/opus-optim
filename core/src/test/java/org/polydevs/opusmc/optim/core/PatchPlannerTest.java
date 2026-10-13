package org.polydevs.opusmc.optim.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.polydevs.opusmc.optim.api.*;

final class PatchPlannerTest {
    private static OptimizationDescriptor.Builder feature(String id) {
        return OptimizationDescriptor.builder(id, ActivationKind.STARTUP_PATCH)
                .correctnessScope("fixture-parity-v1").benchmarkScenario("fixture-workload-v1");
    }

    private static OptimConfig enabled(String... ids) {
        Map<String, Boolean> flags = new HashMap<>();
        for (String id : ids) { flags.put(id, true); }
        return new OptimConfig(true, flags);
    }

    private static PatchPlan plan(OptimConfig config, OptimizationDescriptor... descriptors) {
        return new PatchPlanner(new OptimizationRegistry(Arrays.asList(descriptors)))
                .plan(config, CapabilitySet.empty(), CrashMarker.Recovery.clean());
    }

    private static PatchClaim claim(PatchClaim.Effect effect, String composition, int order) {
        return new PatchClaim("ave", "runGameLoop()V", "frame-return-v1", effect, composition, order);
    }

    @Test
    void duplicateIdsAreRejectedInsteadOfReplacingFeatures() {
        assertThrows(IllegalArgumentException.class, () -> new OptimizationRegistry(
                Arrays.asList(feature("a").build(), feature("a").build())));
    }

    @Test
    void offAndSafeModesSelectNothingIncludingDeclaredDefaults() {
        OptimizationDescriptor a = feature("a").status(OptimizationStatus.DEFAULT).build();
        PatchPlan off = plan(new OptimConfig(false, Collections.emptyMap()), a);
        assertTrue(off.selectedIds().isEmpty());
        assertEquals(ExclusionReason.OPTIM_OFF, off.excluded().get("a").code());
        assertEquals(ExclusionReason.SAFE_CONFIG,
                plan(OptimConfig.safe("invalid json"), a).excluded().get("a").code());
    }

    @Test
    void experimentalFeaturesRequireExplicitOptInEvenWithDefaultStatus() {
        OptimizationDescriptor backend = OptimizationDescriptor.builder("gpu", ActivationKind.EXPERIMENTAL_BACKEND)
                .status(OptimizationStatus.DEFAULT).correctnessScope("gpu-parity").benchmarkScenario("gpu-workload").build();
        assertEquals(ExclusionReason.EXPERIMENTAL_NOT_ENABLED,
                plan(new OptimConfig(true, Collections.emptyMap()), backend).excluded().get("gpu").code());
        assertEquals(Collections.singletonList("gpu"), plan(enabled("gpu"), backend).selectedIds());
        OptimizationDescriptor trial = feature("trial").status(OptimizationStatus.EXPERIMENTAL).build();
        assertTrue(plan(new OptimConfig(true, Collections.emptyMap()), trial).selectedIds().isEmpty());
    }

    @Test
    void selectedDependenciesAreOrderedBeforeTheirConsumersRegardlessOfInputOrder() {
        OptimizationDescriptor a = feature("a").requires(Collections.singleton("z")).build();
        PatchPlan result = plan(enabled("a", "z"), a, feature("z").build());
        assertEquals(Arrays.asList("z", "a"), result.selectedIds());
        assertThrows(UnsupportedOperationException.class, () -> result.selectedIds().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.excluded().clear());
    }

    @Test
    void missingAndDisabledDependenciesExplainWhyAFeatureCannotRun() {
        OptimizationDescriptor a = feature("a").requires(Collections.singleton("missing")).build();
        assertEquals(ExclusionReason.DEPENDENCY_MISSING, plan(enabled("a"), a).excluded().get("a").code());
        a = feature("a").requires(Collections.singleton("b")).build();
        PatchPlan result = plan(enabled("a"), a, feature("b").build());
        assertEquals(ExclusionReason.DEPENDENCY_DISABLED, result.excluded().get("a").code());
        assertFalse(result.excluded().get("a").detail().isEmpty());
    }

    @Test
    void cyclesAreExcludedAndTheirConsumersCannotSneakIntoThePlan() {
        PatchPlan result = plan(enabled("a", "b", "consumer"),
                feature("a").requires(Collections.singleton("b")).build(),
                feature("b").requires(Collections.singleton("a")).build(),
                feature("consumer").requires(Collections.singleton("a")).build());
        assertTrue(result.selectedIds().isEmpty());
        assertEquals(ExclusionReason.DEPENDENCY_CYCLE, result.excluded().get("a").code());
        assertEquals(ExclusionReason.DEPENDENCY_CYCLE, result.excluded().get("b").code());
        assertEquals(ExclusionReason.DEPENDENCY_DISABLED, result.excluded().get("consumer").code());
    }

    @Test
    void missingCapabilitiesDoNotInventSupportOrEnableTheFallback() {
        OptimizationDescriptor a = feature("fast").requiredCapabilities(Collections.singleton("gl.timer-query"))
                .fallbackId("safe").build();
        PatchPlan result = plan(enabled("fast"), a, feature("safe").build());
        assertTrue(result.selectedIds().isEmpty());
        assertEquals(ExclusionReason.CAPABILITY_MISSING, result.excluded().get("fast").code());
        assertEquals("safe", result.excluded().get("fast").fallbackId().get());
    }

    @Test
    void declaredConflictsDisableBothAndPropagateToDependents() {
        PatchPlan result = plan(enabled("a", "b", "dependent"),
                feature("a").conflicts(Collections.singleton("b")).build(), feature("b").build(),
                feature("dependent").requires(Collections.singleton("a")).build());
        assertTrue(result.selectedIds().isEmpty());
        assertEquals(ExclusionReason.CONFLICT, result.excluded().get("a").code());
        assertEquals(ExclusionReason.CONFLICT, result.excluded().get("b").code());
        assertEquals(ExclusionReason.DEPENDENCY_DISABLED, result.excluded().get("dependent").code());
    }

    @Test
    void offFeaturesDoNotConflictWithEnabledFeatures() {
        PatchPlan result = plan(enabled("a"), feature("a").build(),
                feature("b").conflicts(Collections.singleton("a")).build());
        assertEquals(Collections.singletonList("a"), result.selectedIds());
    }

    @Test
    void instrumentationAndBehaviorComposeOnlyWithMatchingEvidenceAndOrder() {
        OptimizationDescriptor timing = feature("z-timing").patchClaims(Collections.singletonList(
                claim(PatchClaim.Effect.INSTRUMENTATION, "frame-composition-v1", 10))).build();
        OptimizationDescriptor behavior = feature("a-behavior").patchClaims(Collections.singletonList(
                claim(PatchClaim.Effect.BEHAVIOR, "frame-composition-v1", 20))).build();
        assertEquals(Arrays.asList("z-timing", "a-behavior"),
                plan(enabled("z-timing", "a-behavior"), timing, behavior).selectedIds());
        OptimizationDescriptor unknown = feature("unknown").patchClaims(Collections.singletonList(
                claim(PatchClaim.Effect.BEHAVIOR, "", 20))).build();
        PatchPlan denied = plan(enabled("z-timing", "unknown"), timing, unknown);
        assertTrue(denied.selectedIds().isEmpty());
        assertEquals(ExclusionReason.CONFLICT, denied.excluded().get("unknown").code());
    }

    @Test
    void competingBehaviorEditsCannotUseACompositionLabelToBypassConflict() {
        PatchPlan result = plan(enabled("a", "b"),
                feature("a").patchClaims(Collections.singletonList(claim(PatchClaim.Effect.BEHAVIOR, "proof", 10))).build(),
                feature("b").patchClaims(Collections.singletonList(claim(PatchClaim.Effect.BEHAVIOR, "proof", 20))).build());
        assertTrue(result.selectedIds().isEmpty());
    }

    @Test
    void baselineClaimsArePreservedWhenAnOptimPatchCompetesWithThem() {
        CompatibilityResolver resolver = new CompatibilityResolver(Collections.singletonList(
                claim(PatchClaim.Effect.BEHAVIOR, "loader-fixture", 1)));
        PatchPlan result = new PatchPlanner(new OptimizationRegistry(Collections.singletonList(
                feature("a").patchClaims(Collections.singletonList(claim(PatchClaim.Effect.BEHAVIOR, "", 2))).build())), resolver)
                .plan(enabled("a"), CapabilitySet.empty(), CrashMarker.Recovery.clean());
        assertTrue(result.selectedIds().isEmpty());
        assertEquals(ExclusionReason.CONFLICT, result.excluded().get("a").code());
    }

    @Test
    void rejectedFeaturesStayRejectedEvenWhenExplicitlyEnabled() {
        assertEquals(ExclusionReason.REJECTED,
                plan(enabled("a"), feature("a").status(OptimizationStatus.REJECTED).build()).excluded().get("a").code());
    }

    @Test
    void startupPlanChangesRequireRestartButLivePoliciesDoNot() {
        OptimizationDescriptor patch = feature("patch").build();
        OptimizationDescriptor policy = OptimizationDescriptor.builder("policy", ActivationKind.RUNTIME_POLICY)
                .correctnessScope("policy-parity").benchmarkScenario("policy-workload").build();
        PatchPlan off = plan(new OptimConfig(false, Collections.emptyMap()), patch, policy);
        assertTrue(plan(enabled("patch"), patch, policy).restartRequiredComparedTo(off));
        assertFalse(plan(enabled("policy"), patch, policy).restartRequiredComparedTo(off));
    }

    @Test
    void reorderedStartupPatchesRequireRestartEvenWithTheSameFeatureIds() {
        OptimizationDescriptor timingBefore = feature("timing").patchClaims(Collections.singletonList(
                claim(PatchClaim.Effect.INSTRUMENTATION, "receipt", 10))).build();
        OptimizationDescriptor timingAfter = feature("timing").patchClaims(Collections.singletonList(
                claim(PatchClaim.Effect.INSTRUMENTATION, "receipt", 30))).build();
        OptimizationDescriptor behavior = feature("behavior").patchClaims(Collections.singletonList(
                claim(PatchClaim.Effect.BEHAVIOR, "receipt", 20))).build();
        PatchPlan previous = plan(enabled("timing", "behavior"), timingBefore, behavior);
        PatchPlan next = plan(enabled("timing", "behavior"), timingAfter, behavior);
        assertEquals(Arrays.asList("timing", "behavior"), previous.selectedIds());
        assertEquals(Arrays.asList("behavior", "timing"), next.selectedIds());
        assertTrue(next.restartRequiredComparedTo(previous));
    }

    @Test
    void contradictoryDependencyAndCompositionOrderCannotBeInstalled() {
        OptimizationDescriptor early = feature("early").requires(Collections.singleton("late"))
                .patchClaims(Collections.singletonList(claim(PatchClaim.Effect.INSTRUMENTATION, "receipt", 10))).build();
        OptimizationDescriptor late = feature("late").patchClaims(Collections.singletonList(
                claim(PatchClaim.Effect.BEHAVIOR, "receipt", 20))).build();
        PatchPlan result = plan(enabled("early", "late"), early, late);
        assertTrue(result.selectedIds().isEmpty());
        assertEquals(ExclusionReason.ORDER_CYCLE, result.excluded().get("early").code());
    }

    @Test
    void aSingleFeatureCannotContainItsOwnUnresolvedCompetingEdits() {
        OptimizationDescriptor self = feature("self").patchClaims(Arrays.asList(
                claim(PatchClaim.Effect.BEHAVIOR, "receipt", 10),
                claim(PatchClaim.Effect.BEHAVIOR, "receipt", 20))).build();
        PatchPlan result = plan(enabled("self"), self);
        assertTrue(result.selectedIds().isEmpty());
        assertEquals(ExclusionReason.CONFLICT, result.excluded().get("self").code());
    }

    @Test
    void changedStartupAnchorRequiresRestartEvenWhenTheSelectedIdIsUnchanged() {
        OptimizationDescriptor before = feature("patch").patchClaims(Collections.singletonList(
                claim(PatchClaim.Effect.BEHAVIOR, "receipt-v1", 10))).build();
        OptimizationDescriptor after = feature("patch").patchClaims(Collections.singletonList(
                new PatchClaim("ave", "runGameLoop()V", "different-anchor-v2", PatchClaim.Effect.BEHAVIOR, "receipt-v2", 10))).build();
        assertTrue(plan(enabled("patch"), after).restartRequiredComparedTo(plan(enabled("patch"), before)));
    }

    @Test
    void selfDeclaredConflictCannotAppearInASelectedPlan() {
        PatchPlan result = plan(enabled("a"), feature("a").conflicts(Collections.singleton("a")).build());
        assertTrue(result.selectedIds().isEmpty());
        assertEquals(ExclusionReason.CONFLICT, result.excluded().get("a").code());
    }
}
