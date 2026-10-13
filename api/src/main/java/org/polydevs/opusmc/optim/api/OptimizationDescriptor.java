package org.polydevs.opusmc.optim.api;

import java.util.*;

public final class OptimizationDescriptor {
    private final String id;
    private final ActivationKind activation;
    private final OptimizationStatus status;
    private final Set<String> requiredCapabilities;
    private final Set<String> requires;
    private final Set<String> conflicts;
    private final String fallbackId;
    private final String correctnessScope;
    private final String benchmarkScenario;
    private final List<PatchClaim> patchClaims;

    private OptimizationDescriptor(Builder builder) {
        id = builder.id;
        activation = builder.activation;
        status = builder.status;
        requiredCapabilities = ids(builder.requiredCapabilities);
        requires = ids(builder.requires);
        conflicts = ids(builder.conflicts);
        fallbackId = builder.fallbackId == null ? null : Identifiers.requireValid(builder.fallbackId);
        correctnessScope = Identifiers.requireText(builder.correctnessScope, "correctnessScope");
        benchmarkScenario = Identifiers.requireText(builder.benchmarkScenario, "benchmarkScenario");
        ArrayList<PatchClaim> claims = new ArrayList<>();
        for (PatchClaim claim : builder.patchClaims) { claims.add(Objects.requireNonNull(claim, "patchClaim")); }
        patchClaims = Collections.unmodifiableList(claims);
    }

    private static Set<String> ids(Collection<String> values) {
        TreeSet<String> copy = new TreeSet<>();
        for (String value : values) { copy.add(Identifiers.requireValid(value)); }
        return Collections.unmodifiableSet(copy);
    }

    public static Builder builder(String id, ActivationKind activation) { return new Builder(id, activation); }
    public String id() { return id; }
    public ActivationKind activation() { return activation; }
    public OptimizationStatus status() { return status; }
    public Set<String> requiredCapabilities() { return requiredCapabilities; }
    public Set<String> requires() { return requires; }
    public Set<String> conflicts() { return conflicts; }
    public Optional<String> fallbackId() { return Optional.ofNullable(fallbackId); }
    public String correctnessScope() { return correctnessScope; }
    public String benchmarkScenario() { return benchmarkScenario; }
    public List<PatchClaim> patchClaims() { return patchClaims; }
    public boolean experimental() {
        return activation == ActivationKind.EXPERIMENTAL_BACKEND || status == OptimizationStatus.EXPERIMENTAL;
    }

    public static final class Builder {
        private final String id;
        private final ActivationKind activation;
        private OptimizationStatus status = OptimizationStatus.DISCOVERED;
        private Collection<String> requiredCapabilities = Collections.emptySet();
        private Collection<String> requires = Collections.emptySet();
        private Collection<String> conflicts = Collections.emptySet();
        private String fallbackId;
        private String correctnessScope = "";
        private String benchmarkScenario = "";
        private Collection<PatchClaim> patchClaims = Collections.emptyList();

        private Builder(String id, ActivationKind activation) {
            this.id = Identifiers.requireValid(id);
            this.activation = Objects.requireNonNull(activation, "activation");
        }
        public Builder status(OptimizationStatus value) { status = Objects.requireNonNull(value); return this; }
        public Builder requiredCapabilities(Collection<String> value) { requiredCapabilities = Objects.requireNonNull(value); return this; }
        public Builder requires(Collection<String> value) { requires = Objects.requireNonNull(value); return this; }
        public Builder conflicts(Collection<String> value) { conflicts = Objects.requireNonNull(value); return this; }
        public Builder fallbackId(String value) { fallbackId = Identifiers.requireValid(value); return this; }
        public Builder correctnessScope(String value) { correctnessScope = Objects.requireNonNull(value); return this; }
        public Builder benchmarkScenario(String value) { benchmarkScenario = Objects.requireNonNull(value); return this; }
        public Builder patchClaims(Collection<PatchClaim> value) { patchClaims = Objects.requireNonNull(value); return this; }
        public OptimizationDescriptor build() { return new OptimizationDescriptor(this); }
    }
}
