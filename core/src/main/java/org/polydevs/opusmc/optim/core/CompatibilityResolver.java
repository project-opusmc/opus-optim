package org.polydevs.opusmc.optim.core;

import java.util.*;
import org.polydevs.opusmc.optim.api.*;

public final class CompatibilityResolver {
    private final List<PatchClaim> baselineClaims;
    private final List<OptimizationDescriptor> baselineDescriptors;

    public CompatibilityResolver() { this(Collections.emptyList()); }
    public CompatibilityResolver(Collection<PatchClaim> baselineClaims) {
        this(baselineClaims, Collections.emptyList());
    }
    private CompatibilityResolver(Collection<PatchClaim> baselineClaims, Collection<OptimizationDescriptor> baselineDescriptors) {
        ArrayList<PatchClaim> copy = new ArrayList<>();
        for (PatchClaim claim : baselineClaims) { copy.add(Objects.requireNonNull(claim)); }
        this.baselineClaims = Collections.unmodifiableList(copy);
        ArrayList<OptimizationDescriptor> descriptors = new ArrayList<>();
        for (OptimizationDescriptor descriptor : baselineDescriptors) { descriptors.add(Objects.requireNonNull(descriptor)); }
        this.baselineDescriptors = Collections.unmodifiableList(descriptors);
    }

    /** Active host baseline metadata; declarations and claims both constrain candidates. */
    public static CompatibilityResolver withBaselineDescriptors(Collection<OptimizationDescriptor> baseline) {
        return new CompatibilityResolver(Collections.emptyList(), baseline);
    }

    public Optional<String> baselineConflict(OptimizationDescriptor descriptor) {
        for (OptimizationDescriptor baseline : baselineDescriptors) {
            Optional<String> conflict = conflict(descriptor, baseline);
            if (conflict.isPresent()) { return conflict; }
        }
        return claimConflict(descriptor.patchClaims(), baselineClaims);
    }

    public Optional<String> internalConflict(OptimizationDescriptor descriptor) {
        if (descriptor.conflicts().contains(descriptor.id())) {
            return Optional.of("Self-declared conflict: " + descriptor.id());
        }
        List<PatchClaim> claims = descriptor.patchClaims();
        for (int i = 0; i < claims.size(); i++) {
            for (int j = i + 1; j < claims.size(); j++) {
                Optional<String> conflict = claimConflict(Collections.singleton(claims.get(i)), Collections.singleton(claims.get(j)));
                if (conflict.isPresent()) { return conflict; }
            }
        }
        return Optional.empty();
    }

    public Optional<String> conflict(OptimizationDescriptor left, OptimizationDescriptor right) {
        if (left.conflicts().contains(right.id()) || right.conflicts().contains(left.id())) {
            return Optional.of("Declared conflict between " + left.id() + " and " + right.id());
        }
        return claimConflict(left.patchClaims(), right.patchClaims());
    }

    private static Optional<String> claimConflict(Collection<PatchClaim> left, Collection<PatchClaim> right) {
        for (PatchClaim a : left) {
            for (PatchClaim b : right) {
                if (!a.sharesMethod(b)) { continue; }
                if (a.effect() == PatchClaim.Effect.BEHAVIOR && b.effect() == PatchClaim.Effect.BEHAVIOR
                        && a.anchor().equals(b.anchor())) {
                    return Optional.of("Competing behavior edits at " + a.targetClass() + "#" + a.method() + ":" + a.anchor());
                }
                if (a.compositionEvidence().isEmpty() || !a.compositionEvidence().equals(b.compositionEvidence())
                        || a.order() == b.order()) {
                    return Optional.of("Unproven composition/order at " + a.targetClass() + "#" + a.method());
                }
            }
        }
        return Optional.empty();
    }
}
