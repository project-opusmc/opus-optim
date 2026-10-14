package org.polydevs.opusmc.optim.platform;

import java.util.*;
import org.polydevs.opusmc.optim.api.*;
import org.polydevs.opusmc.optim.core.*;
import org.polydevs.opusmc.optim.platform.mc189.*;

/** Explicit managed startup snapshot; never reads or overwrites user preferences. */
public final class ManagedOptimPlan {
    private ManagedOptimPlan() { }
    public static ProviderPlan prepare(String selection, String runtime, ClassLoader gameLoader) {
        Objects.requireNonNull(selection,"selection");
        List<TransformerBinding> candidates=new ArrayList<>();
        candidates.add(FrustumOptimization.candidate().binding());
        for (PipelineOptimizations.Candidate candidate:PipelineOptimizations.candidates()) candidates.add(candidate.binding());
        candidates.add(ResourceOptimization.candidate().binding());
        Set<String> known=new HashSet<>(), capabilities=new HashSet<>();
        for (TransformerBinding binding:candidates) {
            known.add(binding.descriptor.id()); capabilities.addAll(binding.descriptor.requiredCapabilities());
        }
        Map<String,Boolean> flags=new LinkedHashMap<>();
        if (!selection.isEmpty()) {
            if (!"mc189-optifine-m5".equals(runtime)) throw new IllegalArgumentException("Verified O2 runtime selection required");
            for(String id:selection.split(",",-1)) {
                if (!known.contains(id) || flags.put(id,true)!=null) throw new IllegalArgumentException("Unknown or duplicate O2 patch: "+id);
            }
        }
        return ProviderPlan.prepare(new OptimConfig(!flags.isEmpty(),flags),new CapabilitySet(capabilities),
                CrashMarker.Recovery.clean(),candidates,Collections.emptyList(),gameLoader);
    }
}
