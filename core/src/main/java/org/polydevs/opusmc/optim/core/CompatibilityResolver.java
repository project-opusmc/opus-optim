package org.polydevs.opusmc.optim.core;

import java.util.*;
import org.polydevs.opusmc.optim.api.*;

public final class CompatibilityResolver {
    private final List<PatchClaim> baselineClaims;

    public CompatibilityResolver() { this(Collections.emptyList()); }
    public CompatibilityResolver(Collection<PatchClaim> baselineClaims) {
        ArrayList<PatchClaim> copy = new ArrayList<>();
        for (PatchClaim claim : baselineClaims) { copy.add(Objects.requireNonNull(claim)); }
        this.baselineClaims = Collections.unmodifiableList(copy);
    }

    public Optional<String> baselineConflict(OptimizationDescriptor descriptor) {
        return claimConflict(descriptor.patchClaims(), baselineClaims);
    }

    public Optional<String> internalConflict(OptimizationDescriptor descriptor) {
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
