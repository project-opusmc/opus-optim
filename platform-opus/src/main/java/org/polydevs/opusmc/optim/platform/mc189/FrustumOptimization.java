package org.polydevs.opusmc.optim.platform.mc189;

import java.util.*;
import org.polydevs.opusmc.optim.api.*;
import org.polydevs.opusmc.optim.platform.TransformerBinding;

/** Explicit opt-in candidate; neither a microbenchmark nor this declaration promotes it. */
public final class FrustumOptimization {
    public static final String ID = "render.frustum.positive-vertex";
    public static final String TARGET_CAPABILITY = "mc189.frustum.optifine-m5.notch";
    public static final String ASM_CAPABILITY = "loader.asm5";
    private FrustumOptimization() { }

    public static OptimizationDescriptor descriptor() {
        return OptimizationDescriptor.builder(ID, ActivationKind.STARTUP_PATCH)
                .status(OptimizationStatus.EXPERIMENTAL)
                .requiredCapabilities(Arrays.asList(TARGET_CAPABILITY, ASM_CAPABILITY))
                .correctnessScope("Exact raw M5 input SHA256 " + FrustumPatch.TARGET_SHA256 + "; stable render-thread plane rows")
                .benchmarkScenario("frustum.query.local.v1; controlled game chunk-flight pending")
                .patchClaims(Collections.singleton(new PatchClaim("bid", "b(DDDDDD)Z", "method-body",
                        PatchClaim.Effect.BEHAVIOR, "", 400))).build();
    }

    public static TransformerBinding binding() {
        return new TransformerBinding(descriptor(), new FrustumPatch(), Collections.singleton(FrustumMath.class));
    }
}
