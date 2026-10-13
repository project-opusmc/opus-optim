package org.polydevs.opusmc.optim.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

final class ContractTest {
    @Test
    void descriptorRejectsEmptyIdentityAndMissingEvidenceScope() {
        assertThrows(IllegalArgumentException.class,
                () -> OptimizationDescriptor.builder(" ", ActivationKind.STARTUP_PATCH));
        assertThrows(IllegalArgumentException.class,
                () -> OptimizationDescriptor.builder("test", ActivationKind.STARTUP_PATCH).build());
    }

    @Test
    void descriptorCopiesInputsAndExposesImmutableMetadata() {
        Set<String> dependencies = new HashSet<>(Collections.singleton("base"));
        OptimizationDescriptor descriptor = OptimizationDescriptor.builder("text", ActivationKind.STARTUP_PATCH)
                .requires(dependencies).requiredCapabilities(Collections.singleton("cpu.clock"))
                .correctnessScope("text-parity-v1").benchmarkScenario("text-dense-v1").build();
        dependencies.clear();
        assertEquals(Collections.singleton("base"), descriptor.requires());
        assertThrows(UnsupportedOperationException.class, () -> descriptor.requires().clear());
        assertEquals(OptimizationStatus.DISCOVERED, descriptor.status());
    }

    @Test
    void glCapabilitiesCannotBeReportedBeforeAProbe() {
        assertThrows(IllegalArgumentException.class, () -> new RuntimeDescriptor("standalone", "1.8.9", "8",
                "absent", Collections.singletonMap("client", "fixture-hash"), false,
                new CapabilitySet(Collections.singleton("gl.timer-query"))));
        RuntimeDescriptor runtime = new RuntimeDescriptor("standalone", "1.8.9", "8", "absent",
                Collections.singletonMap("client", "fixture-hash"), false, CapabilitySet.empty());
        assertFalse(runtime.glProbed());
        assertFalse(runtime.capabilities().contains("gl.timer-query"));
    }

    @Test
    void metricsDistinguishUnmeasuredFromZeroAndRejectNegativeCounters() {
        Metrics metrics = new Metrics(Collections.singletonMap("draw.calls", 0L));
        assertEquals(0L, metrics.counter("draw.calls").getAsLong());
        assertFalse(metrics.counter("gpu.ns").isPresent());
        assertThrows(IllegalArgumentException.class,
                () -> new Metrics(Collections.singletonMap("draw.calls", -1L)));
        assertThrows(UnsupportedOperationException.class, () -> metrics.counters().put("gpu.ns", 0L));
    }
}
