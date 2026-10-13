package org.polydevs.opusmc.optim.platform;

/** Managed host entry: invoke before approved provider discovery and game class definition. */
public final class OptimBootstrap {
    static final ProviderState STATE = new ProviderState();
    private OptimBootstrap() { }

    public static void install(ProviderPlan plan) { STATE.install(plan); }
}
