package org.polydevs.opusmc.optim.platform.mc189;

import java.util.*;
import org.polydevs.opusmc.optim.api.*;
import org.polydevs.opusmc.optim.core.render.*;
import org.polydevs.opusmc.optim.core.workload.DeadlineBudget;
import org.polydevs.opusmc.optim.platform.TransformerBinding;

/** Explicit candidates, not installation, game acceptance or automatic promotion. */
public final class PipelineOptimizations {
    public enum Kind {
        GLYPH_LOOKUP("text.glyph_lookup", "avn", "bf11b41d4cfa907e6cd3b517497cb8a82fd3ef21ef375b558dc6427c76e98c3e", 410,
                "mc189.font.optifine-m5.notch", GlyphIndex.class,
                "a(CZ)F", "a(Ljava/lang/String;Z)V", "getCharWidthFloat(C)F", "b(C)I", "<clinit>()V", "o2$glyphIndex:[S"),
        BUFFER_GROWTH("render.buffer_growth", "bfd", "5d231d8bc0b76de6072d24eab8f6f49d7bb84a6044ab6a33ae84aa1296c60a40", 420,
                "mc189.worldrenderer.optifine-m5.notch", BufferGrowth.class, "b(I)V"),
        UPLOAD_BUDGET("world.fixed_budget", "bho", "381976963d303e09ea0fba4fe4c0b0ec6db24db0b4da10e88fce999904e372c7", 430,
                "mc189.chunk-dispatcher.optifine-m5.notch", DeadlineBudget.class, "a(J)Z");

        final String id, target, digest, capability;
        final int priority;
        final Class<?> helper;
        final List<String> methods;
        Kind(String id, String target, String digest, int priority, String capability, Class<?> helper, String... methods) {
            this.id=id; this.target=target; this.digest=digest; this.priority=priority;
            this.capability=capability; this.helper=helper; this.methods=Collections.unmodifiableList(Arrays.asList(methods));
        }
        public String target() { return target; }
        public String targetSha256() { return digest; }
    }
    private PipelineOptimizations() { }
    public static Candidate candidate(Kind kind) { return new Candidate(Objects.requireNonNull(kind, "kind")); }
    public static List<Candidate> candidates() {
        List<Candidate> all=new ArrayList<>();
        for (Kind kind:Kind.values()) { all.add(candidate(kind)); }
        return Collections.unmodifiableList(all);
    }
    public static final class Candidate {
        private final Kind kind;
        private final OptimizationDescriptor descriptor;
        private final PipelinePatch patch;
        private final TransformerBinding binding;
        private Candidate(Kind kind) {
            this.kind=kind;
            List<PatchClaim> claims=new ArrayList<>();
            for (String method:kind.methods) { claims.add(new PatchClaim(kind.target, method, "method-body",
                    PatchClaim.Effect.BEHAVIOR, "", kind.priority)); }
            descriptor=OptimizationDescriptor.builder(kind.id, ActivationKind.STARTUP_PATCH)
                    .status(OptimizationStatus.EXPERIMENTAL)
                    .requiredCapabilities(Arrays.asList(kind.capability, "loader.asm5"))
                    .correctnessScope("Exact raw M5 SHA256 "+kind.digest+"; see PIPELINE_OPTIM_SPEC.md")
                    .benchmarkScenario("pipeline.kernel.local.v1; managed game pending")
                    .patchClaims(claims).build();
            patch=new PipelinePatch(kind);
            binding=new TransformerBinding(descriptor, patch, Collections.singleton(kind.helper));
        }
        public Kind kind() { return kind; }
        public OptimizationDescriptor descriptor() { return descriptor; }
        public TransformerBinding binding() { return binding; }
        /** Most recent target-bytecode outcome; APPLIED does not mean class definition or game use. */
        public PipelinePatch.ApplicationState applicationState() { return patch.applicationState(); }
    }
}
