package org.polydevs.opusmc.optim.platform;

import java.util.*;
import org.polydevs.opusmc.bootstrap.TransformerRegistration;
import org.polydevs.opusmc.optim.api.*;
import org.polydevs.opusmc.optim.core.*;

/** Immutable prepared startup snapshot; preparation does not install or define classes. */
public final class ProviderPlan {
    private final PatchPlan patchPlan;
    private final List<TransformerRegistration> registrations;

    private ProviderPlan(PatchPlan patchPlan, List<TransformerRegistration> registrations) {
        this.patchPlan = patchPlan;
        this.registrations = Collections.unmodifiableList(new ArrayList<>(registrations));
    }

    public static ProviderPlan prepare(OptimConfig config, CapabilitySet capabilities, CrashMarker.Recovery recovery,
            Collection<TransformerBinding> candidates, Collection<TransformerBinding> baseline, ClassLoader gameLoader) {
        Objects.requireNonNull(gameLoader, "gameLoader");
        Map<String, TransformerBinding> bound = bindings(candidates);
        Map<String, TransformerBinding> base = bindings(baseline);
        for (String id : bound.keySet()) {
            if (base.containsKey(id)) { throw new IllegalArgumentException("Candidate duplicates baseline id: " + id); }
        }
        List<OptimizationDescriptor> catalogue = new ArrayList<>();
        for (TransformerBinding binding : bound.values()) { catalogue.add(binding.descriptor); }
        List<PatchClaim> baseClaims = new ArrayList<>();
        for (TransformerBinding binding : base.values()) { baseClaims.addAll(binding.descriptor.patchClaims()); }
        PatchPlan plan = new PatchPlanner(new OptimizationRegistry(catalogue), new CompatibilityResolver(baseClaims))
                .plan(config, capabilities, recovery);

        List<TransformerBinding> selected = new ArrayList<>();
        for (String id : plan.selectedIds()) { selected.add(bound.get(id)); }
        List<TransformerBinding> actual = new ArrayList<>(base.values());
        actual.addAll(selected);
        actual.sort(Comparator.comparingInt((TransformerBinding b) -> b.transformer.priority())
                .thenComparing(b -> b.transformer.id()));
        List<String> actualSelected = new ArrayList<>();
        for (TransformerBinding binding : actual) {
            if (bound.containsKey(binding.descriptor.id())) { actualSelected.add(binding.descriptor.id()); }
        }
        if (!actualSelected.equals(plan.selectedIds())) {
            throw new IllegalArgumentException("Loader priority/id order differs from the selected startup plan");
        }
        for (int i = 0; i < actual.size(); i++) {
            for (int j = i + 1; j < actual.size(); j++) {
                TransformerBinding first = actual.get(i), second = actual.get(j);
                if (base.containsKey(first.descriptor.id()) && base.containsKey(second.descriptor.id())) { continue; }
                for (PatchClaim a : first.descriptor.patchClaims()) {
                    for (PatchClaim b : second.descriptor.patchClaims()) {
                        if (a.sharesMethod(b) && a.order() >= b.order()) {
                            throw new IllegalArgumentException("Loader order reverses composition at "
                                    + a.targetClass() + "#" + a.method());
                        }
                    }
                }
            }
        }
        List<TransformerRegistration> registrations = new ArrayList<>();
        for (TransformerBinding binding : selected) {
            binding.verifyHelpers(gameLoader);
            registrations.add(new TransformerRegistration(OptimTransformerProvider.ID, binding.transformer, binding.targets));
        }
        return new ProviderPlan(plan, registrations);
    }

    private static Map<String, TransformerBinding> bindings(Collection<TransformerBinding> supplied) {
        Map<String, TransformerBinding> result = new TreeMap<>();
        for (TransformerBinding binding : supplied) {
            Objects.requireNonNull(binding, "binding");
            if (result.putIfAbsent(binding.descriptor.id(), binding) != null) {
                throw new IllegalArgumentException("Duplicate transformer binding: " + binding.descriptor.id());
            }
        }
        return result;
    }

    public PatchPlan patchPlan() { return patchPlan; }
    public List<TransformerRegistration> registrations() { return registrations; }
}
