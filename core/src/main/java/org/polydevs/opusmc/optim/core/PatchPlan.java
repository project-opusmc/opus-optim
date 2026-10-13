package org.polydevs.opusmc.optim.core;

import java.util.*;
import org.polydevs.opusmc.optim.api.*;

public final class PatchPlan {
    private final List<String> selectedIds;
    private final Map<String, Exclusion> excluded;
    private final List<List<Object>> startupSignature;

    PatchPlan(List<String> ids, Map<String, Exclusion> excluded, Map<String, OptimizationDescriptor> descriptors) {
        selectedIds = Collections.unmodifiableList(new ArrayList<>(ids));
        this.excluded = Collections.unmodifiableMap(new TreeMap<>(excluded));
        List<List<Object>> startup = new ArrayList<>();
        for (String id : ids) {
            OptimizationDescriptor descriptor = descriptors.get(id);
            if (descriptor.activation() == ActivationKind.STARTUP_PATCH) {
                List<List<Object>> claims = new ArrayList<>();
                for (PatchClaim claim : descriptor.patchClaims()) {
                    claims.add(Arrays.asList(claim.targetClass(), claim.method(), claim.anchor(),
                            claim.effect(), claim.compositionEvidence(), claim.order()));
                }
                startup.add(Arrays.asList(id, descriptor.correctnessScope(), descriptor.benchmarkScenario(),
                        descriptor.requiredCapabilities(), descriptor.requires(), descriptor.conflicts(),
                        descriptor.fallbackId().orElse(""), claims));
            }
        }
        startupSignature = Collections.unmodifiableList(startup);
    }

    public List<String> selectedIds() { return selectedIds; }
    public Map<String, Exclusion> excluded() { return excluded; }
    public boolean restartRequiredComparedTo(PatchPlan previous) { return !startupSignature.equals(previous.startupSignature); }

    public static final class Exclusion {
        private final ExclusionReason code;
        private final String detail;
        private final String fallbackId;

        Exclusion(ExclusionReason code, String detail, OptimizationDescriptor descriptor) {
            this.code = code; this.detail = detail; fallbackId = descriptor.fallbackId().orElse(null);
        }
        public ExclusionReason code() { return code; }
        public String detail() { return detail; }
        public Optional<String> fallbackId() { return Optional.ofNullable(fallbackId); }
    }
}
