package org.polydevs.opusmc.optim.bench;

import java.util.Objects;
import org.polydevs.opusmc.optim.api.Identifiers;

/** Session independence and origin are supplied declarations, not authenticated evidence. */
public final class RunIdentity {
    public enum Origin { RECORDED, SYNTHETIC }
    private final String runId, sessionId;
    private final BenchmarkVariant variant;
    private final Origin origin;
    public RunIdentity(String runId, String sessionId, BenchmarkVariant variant, Origin origin) {
        this.runId = Identifiers.requireValid(runId);
        this.sessionId = Identifiers.requireValid(sessionId);
        this.variant = Objects.requireNonNull(variant, "variant");
        this.origin = Objects.requireNonNull(origin, "origin");
    }
    public String runId() { return runId; }
    public String sessionId() { return sessionId; }
    public BenchmarkVariant variant() { return variant; }
    public Origin origin() { return origin; }
}
