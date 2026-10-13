package org.polydevs.opusmc.optim.api;

/** Declared evidence status. Constructing a descriptor does not validate or promote a feature. */
public enum OptimizationStatus {
    DISCOVERED, PRICED, EXPERIMENTAL, VALIDATED, DEFAULT, REJECTED, DISABLED_CONFLICT
}
