package org.polydevs.opusmc.optim.platform;

import java.util.*;
import org.polydevs.opusmc.bootstrap.TransformerRegistration;

/** One classloader's immutable startup lifecycle, shared by its service provider. */
final class ProviderState {
    private boolean installed, sealed;
    private List<TransformerRegistration> snapshot = Collections.emptyList();

    synchronized void install(ProviderPlan plan) {
        Objects.requireNonNull(plan, "plan");
        if (installed || sealed) { throw new IllegalStateException("O2 startup is already installed or sealed; restart required"); }
        snapshot = plan.registrations();
        installed = true;
    }

    synchronized List<TransformerRegistration> registrations() {
        sealed = true;
        return snapshot;
    }
}
