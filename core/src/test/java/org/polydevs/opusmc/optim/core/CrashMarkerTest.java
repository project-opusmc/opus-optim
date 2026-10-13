package org.polydevs.opusmc.optim.core;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.polydevs.opusmc.optim.api.*;

final class CrashMarkerTest {
    @TempDir Path directory;

    @Test
    void unfinishedLaunchDisablesExperimentsUntilASubsequentCleanRun() throws Exception {
        CrashMarker marker = new CrashMarker(directory.resolve("crash.json"));
        assertFalse(marker.inspect().disableExperimental());
        marker.begin("run-1", Collections.singleton("trial"));
        assertTrue(new CrashMarker(directory.resolve("crash.json")).inspect().disableExperimental());
        OptimizationDescriptor trial = OptimizationDescriptor.builder("trial", ActivationKind.EXPERIMENTAL_BACKEND)
                .correctnessScope("trial-parity").benchmarkScenario("trial-workload").build();
        OptimizationDescriptor safe = OptimizationDescriptor.builder("safe", ActivationKind.RUNTIME_POLICY)
                .correctnessScope("safe-parity").benchmarkScenario("safe-workload").build();
        Map<String, Boolean> flags = new HashMap<>();
        flags.put("trial", true); flags.put("safe", true);
        PatchPlan result = new PatchPlanner(new OptimizationRegistry(Arrays.asList(trial, safe)))
                .plan(new OptimConfig(true, flags), CapabilitySet.empty(), marker.inspect());
        assertEquals(Collections.singletonList("safe"), result.selectedIds());
        assertEquals(ExclusionReason.CRASH_RECOVERY, result.excluded().get("trial").code());
        marker.begin("safe-run-2", Collections.emptySet());
        marker.markCleanShutdown("safe-run-2");
        assertFalse(marker.inspect().disableExperimental());
    }

    @Test
    void wrongRunCannotClearAnotherLaunchMarker() throws Exception {
        CrashMarker marker = new CrashMarker(directory.resolve("crash.json"));
        marker.begin("run-1", Collections.emptySet());
        assertThrows(IllegalStateException.class, () -> marker.markCleanShutdown("another-run"));
        assertTrue(marker.inspect().disableExperimental());
    }

    @Test
    void corruptMarkerKeepsBaseModeAndIsNotDeletedByInspection() throws Exception {
        Path path = directory.resolve("crash.json");
        Files.write(path, "broken".getBytes(StandardCharsets.UTF_8));
        CrashMarker marker = new CrashMarker(path);
        assertTrue(marker.inspect().isSafe());
        assertTrue(marker.inspect().disableExperimental());
        assertFalse(marker.inspect().reason().isEmpty());
        OptimizationDescriptor feature = OptimizationDescriptor.builder("safe", ActivationKind.RUNTIME_POLICY)
                .correctnessScope("safe-parity").benchmarkScenario("safe-workload").build();
        PatchPlan result = new PatchPlanner(new OptimizationRegistry(Collections.singletonList(feature)))
                .plan(new OptimConfig(true, Collections.singletonMap("safe", true)), CapabilitySet.empty(), marker.inspect());
        assertTrue(result.selectedIds().isEmpty());
        assertEquals(ExclusionReason.CRASH_RECOVERY, result.excluded().get("safe").code());
        assertEquals("broken", new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    }
}
