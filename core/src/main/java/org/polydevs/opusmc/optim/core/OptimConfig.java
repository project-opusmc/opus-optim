package org.polydevs.opusmc.optim.core;

import java.util.*;
import org.polydevs.opusmc.optim.api.Identifiers;

public final class OptimConfig {
    private final boolean enabled;
    private final Map<String, Boolean> features;
    private final String safeReason;

    public OptimConfig(boolean enabled, Map<String, Boolean> features) { this(enabled, features, ""); }

    private OptimConfig(boolean enabled, Map<String, Boolean> features, String safeReason) {
        TreeMap<String, Boolean> copy = new TreeMap<>();
        features.forEach((id, flag) -> copy.put(Identifiers.requireValid(id), Objects.requireNonNull(flag, "feature flag")));
        this.enabled = enabled;
        this.features = Collections.unmodifiableMap(copy);
        this.safeReason = safeReason;
    }

    public static OptimConfig safe(String reason) {
        if (reason == null || reason.trim().isEmpty()) { throw new IllegalArgumentException("SAFE needs a reason"); }
        return new OptimConfig(false, Collections.emptyMap(), reason);
    }

    public boolean enabled() { return enabled; }
    public Map<String, Boolean> features() { return features; }
    public boolean isSafe() { return !safeReason.isEmpty(); }
    public String safeReason() { return safeReason; }
}
