package org.polydevs.opusmc.optim.bench;

import java.util.Objects;

/** A supplied receipt reference. The engine does not authenticate its contents. */
public final class EvidenceReceipt {
    public enum Status { PASS, FAIL, MISSING }
    private final Status status;
    private final String sha256;
    public EvidenceReceipt(Status status, String sha256) {
        this.status = Objects.requireNonNull(status, "status");
        if (status == Status.MISSING && sha256 != null) { throw new IllegalArgumentException("Missing receipt has no hash"); }
        this.sha256 = status == Status.MISSING ? null : BenchValues.hash(sha256);
    }
    public Status status() { return status; }
    public String sha256() { return sha256; }
}
