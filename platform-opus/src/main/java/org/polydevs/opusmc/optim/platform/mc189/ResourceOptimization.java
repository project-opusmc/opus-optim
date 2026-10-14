package org.polydevs.opusmc.optim.platform.mc189;

import java.util.*;
import org.polydevs.opusmc.optim.api.*;
import org.polydevs.opusmc.optim.platform.TransformerBinding;

/** Lazy immutable resource text; extra retained heap must be measured before promotion. */
public final class ResourceOptimization {
    public static final String ID="runtime.resource_string_cache";
    private ResourceOptimization() { }
    public static Candidate candidate() { return new Candidate(); }
    public static final class Candidate {
        private final ResourcePatch patch=new ResourcePatch();
        private final OptimizationDescriptor descriptor=OptimizationDescriptor.builder(ID,ActivationKind.STARTUP_PATCH)
                .status(OptimizationStatus.EXPERIMENTAL)
                .requiredCapabilities(Arrays.asList("mc189.resource-location.optifine-m5.notch","loader.asm5"))
                .correctnessScope("Exact raw jy SHA256 "+ResourcePatch.TARGET_SHA256+"; immutable final domain/path; content parity, cached identity intentional")
                .benchmarkScenario("resource.string.local.v1; real heap/game workload pending")
                .patchClaims(Arrays.asList(
                        new PatchClaim("jy","toString()Ljava/lang/String;","method-body",PatchClaim.Effect.BEHAVIOR,"",400),
                        new PatchClaim("jy","o2$resourceText:Ljava/lang/String;","instance-field",PatchClaim.Effect.BEHAVIOR,"",400))).build();
        private final TransformerBinding binding=new TransformerBinding(descriptor,patch,Collections.emptyList());
        private Candidate() { }
        public OptimizationDescriptor descriptor() { return descriptor; }
        public TransformerBinding binding() { return binding; }
        public ResourcePatch.ApplicationState applicationState() { return patch.applicationState(); }
    }
}
