package org.polydevs.opusmc.optim.core;

import java.util.*;
import org.polydevs.opusmc.optim.api.*;

/** Pure pre-installation decisions. No transforms, class loading, probes or live unpatching. */
public final class PatchPlanner {
    private final OptimizationRegistry registry;
    private final CompatibilityResolver compatibility;

    public PatchPlanner(OptimizationRegistry registry) { this(registry, new CompatibilityResolver()); }
    public PatchPlanner(OptimizationRegistry registry, CompatibilityResolver compatibility) {
        this.registry = Objects.requireNonNull(registry); this.compatibility = Objects.requireNonNull(compatibility);
    }

    public PatchPlan plan(OptimConfig config, CapabilitySet capabilities, CrashMarker.Recovery recovery) {
        Objects.requireNonNull(config); Objects.requireNonNull(capabilities); Objects.requireNonNull(recovery);
        Map<String, OptimizationDescriptor> all = registry.descriptors();
        Map<String, PatchPlan.Exclusion> excluded = new TreeMap<>();
        for (OptimizationDescriptor descriptor : all.values()) {
            ExclusionReason code = eligibility(descriptor, config, capabilities, recovery);
            if (code != null) { exclude(excluded, descriptor, code, eligibilityDetail(descriptor, code, config, recovery)); }
            else {
                compatibility.internalConflict(descriptor).ifPresent(detail ->
                        exclude(excluded, descriptor, ExclusionReason.CONFLICT, "Within feature: " + detail));
            }
        }
        for (OptimizationDescriptor descriptor : all.values()) {
            if (excluded.containsKey(descriptor.id())) { continue; }
            for (String dependency : descriptor.requires()) {
                if (!all.containsKey(dependency)) {
                    exclude(excluded, descriptor, ExclusionReason.DEPENDENCY_MISSING, "Missing dependency: " + dependency);
                    break;
                }
            }
        }
        Set<String> visited = new HashSet<>();
        for (String id : all.keySet()) { markCycles(id, all, excluded, visited, new ArrayList<>()); }
        propagateDependencies(all, excluded);

        for (OptimizationDescriptor descriptor : all.values()) {
            if (!excluded.containsKey(descriptor.id())) {
                Optional<String> conflict = compatibility.baselineConflict(descriptor);
                conflict.ifPresent(detail -> exclude(excluded, descriptor, ExclusionReason.CONFLICT, "Baseline: " + detail));
            }
        }
        propagateDependencies(all, excluded);
        List<OptimizationDescriptor> candidates = candidates(all, excluded);
        for (int i = 0; i < candidates.size(); i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                OptimizationDescriptor left = candidates.get(i), right = candidates.get(j);
                Optional<String> conflict = compatibility.conflict(left, right);
                if (conflict.isPresent()) {
                    exclude(excluded, left, ExclusionReason.CONFLICT, right.id() + ": " + conflict.get());
                    exclude(excluded, right, ExclusionReason.CONFLICT, left.id() + ": " + conflict.get());
                }
            }
        }
        propagateDependencies(all, excluded);
        List<String> ordered = order(all, excluded);
        return new PatchPlan(ordered, excluded, all);
    }

    private static ExclusionReason eligibility(OptimizationDescriptor descriptor, OptimConfig config,
            CapabilitySet capabilities, CrashMarker.Recovery recovery) {
        if (config.isSafe()) { return ExclusionReason.SAFE_CONFIG; }
        if (!config.enabled()) { return ExclusionReason.OPTIM_OFF; }
        if (recovery.isSafe() || (recovery.disableExperimental() && descriptor.experimental())) {
            return ExclusionReason.CRASH_RECOVERY;
        }
        if (descriptor.status() == OptimizationStatus.REJECTED) { return ExclusionReason.REJECTED; }
        if (descriptor.status() == OptimizationStatus.DISABLED_CONFLICT) { return ExclusionReason.CONFLICT; }
        Boolean explicit = config.features().get(descriptor.id());
        if (descriptor.experimental() && explicit == null) { return ExclusionReason.EXPERIMENTAL_NOT_ENABLED; }
        boolean enabled = explicit != null ? explicit : descriptor.status() == OptimizationStatus.DEFAULT;
        if (!enabled) { return ExclusionReason.FEATURE_OFF; }
        if (!descriptor.requiredCapabilities().stream().allMatch(capabilities::contains)) { return ExclusionReason.CAPABILITY_MISSING; }
        return null;
    }

    private static String eligibilityDetail(OptimizationDescriptor descriptor, ExclusionReason code,
            OptimConfig config, CrashMarker.Recovery recovery) {
        if (code == ExclusionReason.SAFE_CONFIG) { return config.safeReason(); }
        if (code == ExclusionReason.CRASH_RECOVERY) { return recovery.reason(); }
        if (code == ExclusionReason.CAPABILITY_MISSING) { return "Required capabilities: " + descriptor.requiredCapabilities(); }
        return code.name() + ": " + descriptor.id();
    }

    private static void exclude(Map<String, PatchPlan.Exclusion> excluded, OptimizationDescriptor descriptor,
            ExclusionReason code, String detail) {
        excluded.putIfAbsent(descriptor.id(), new PatchPlan.Exclusion(code, detail, descriptor));
    }

    private static void markCycles(String id, Map<String, OptimizationDescriptor> all,
            Map<String, PatchPlan.Exclusion> excluded, Set<String> visited, List<String> path) {
        int cycleStart = path.indexOf(id);
        if (cycleStart >= 0) {
            for (String member : path.subList(cycleStart, path.size())) {
                exclude(excluded, all.get(member), ExclusionReason.DEPENDENCY_CYCLE, "Dependency cycle: " + path.subList(cycleStart, path.size()));
            }
            return;
        }
        if (excluded.containsKey(id) || !visited.add(id)) { return; }
        path.add(id);
        for (String dependency : all.get(id).requires()) {
            if (all.containsKey(dependency)) { markCycles(dependency, all, excluded, visited, path); }
        }
        path.remove(path.size() - 1);
    }

    private static void propagateDependencies(Map<String, OptimizationDescriptor> all, Map<String, PatchPlan.Exclusion> excluded) {
        boolean changed;
        do {
            changed = false;
            for (OptimizationDescriptor descriptor : all.values()) {
                if (excluded.containsKey(descriptor.id())) { continue; }
                for (String dependency : descriptor.requires()) {
                    if (excluded.containsKey(dependency)) {
                        exclude(excluded, descriptor, ExclusionReason.DEPENDENCY_DISABLED, "Disabled dependency: " + dependency);
                        changed = true; break;
                    }
                }
            }
        } while (changed);
    }

    private static List<OptimizationDescriptor> candidates(Map<String, OptimizationDescriptor> all,
            Map<String, PatchPlan.Exclusion> excluded) {
        List<OptimizationDescriptor> result = new ArrayList<>();
        for (OptimizationDescriptor descriptor : all.values()) {
            if (!excluded.containsKey(descriptor.id())) { result.add(descriptor); }
        }
        return result;
    }

    private static List<String> order(Map<String, OptimizationDescriptor> all, Map<String, PatchPlan.Exclusion> excluded) {
        List<OptimizationDescriptor> candidates = candidates(all, excluded);
        Map<String, Set<String>> prerequisites = new TreeMap<>();
        for (OptimizationDescriptor descriptor : candidates) { prerequisites.put(descriptor.id(), new TreeSet<>(descriptor.requires())); }
        for (OptimizationDescriptor left : candidates) {
            for (OptimizationDescriptor right : candidates) {
                if (left == right) { continue; }
                for (PatchClaim a : left.patchClaims()) {
                    for (PatchClaim b : right.patchClaims()) {
                        if (a.sharesMethod(b) && a.order() < b.order()) { prerequisites.get(right.id()).add(left.id()); }
                    }
                }
            }
        }
        List<String> result = new ArrayList<>();
        while (!prerequisites.isEmpty()) {
            String ready = null;
            for (Map.Entry<String, Set<String>> entry : prerequisites.entrySet()) {
                if (entry.getValue().isEmpty()) { ready = entry.getKey(); break; }
            }
            if (ready == null) {
                for (String id : prerequisites.keySet()) {
                    exclude(excluded, all.get(id), ExclusionReason.ORDER_CYCLE, "Contradictory dependency/composition order: " + prerequisites.keySet());
                }
                break;
            }
            prerequisites.remove(ready);
            for (Set<String> dependencies : prerequisites.values()) { dependencies.remove(ready); }
            result.add(ready);
        }
        return result;
    }
}
