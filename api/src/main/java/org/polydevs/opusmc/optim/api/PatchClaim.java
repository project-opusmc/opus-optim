package org.polydevs.opusmc.optim.api;

import java.util.Objects;

/** Exact patch surface plus an externally reviewed composition receipt id and relative order. */
public final class PatchClaim {
    public enum Effect { INSTRUMENTATION, BEHAVIOR }

    private final String targetClass;
    private final String method;
    private final String anchor;
    private final Effect effect;
    private final String compositionEvidence;
    private final int order;

    public PatchClaim(String targetClass, String method, String anchor, Effect effect,
            String compositionEvidence, int order) {
        this.targetClass = Identifiers.requireText(targetClass, "targetClass").replace('/', '.');
        this.method = Identifiers.requireText(method, "method (including descriptor)");
        this.anchor = Identifiers.requireText(anchor, "anchor");
        this.effect = Objects.requireNonNull(effect, "effect");
        this.compositionEvidence = Objects.requireNonNull(compositionEvidence, "compositionEvidence").trim();
        this.order = order;
    }

    public String targetClass() { return targetClass; }
    public String method() { return method; }
    public String anchor() { return anchor; }
    public Effect effect() { return effect; }
    public String compositionEvidence() { return compositionEvidence; }
    public int order() { return order; }
    public boolean sharesMethod(PatchClaim other) {
        return targetClass.equals(other.targetClass) && method.equals(other.method);
    }
}
