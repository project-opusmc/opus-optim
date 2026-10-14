package org.polydevs.opusmc.optim.platform;

import java.util.List;
import org.polydevs.opusmc.bootstrap.*;

/** Loader-owned SPI; construction does no config I/O or game/GL initialization. */
public final class OptimTransformerProvider implements ClassTransformerProvider {
    public static final String ID = "opus.optim";
    public OptimTransformerProvider() { }
    @Override public String id() { return ID; }
    @Override public void initialize(ClassLoader gameLoader) {
        if (OptimBootstrap.STATE.installed()) return;
        String selection=System.getProperty("opus.optim.patches", "");
        if (selection.isEmpty()) return;
        ProviderPlan plan=ManagedOptimPlan.prepare(selection,System.getProperty("opus.optim.runtime", ""),gameLoader);
        OptimBootstrap.install(plan);
        System.out.println("[O2] managed patches SELECTED " + plan.patchPlan().selectedIds());
    }
    @Override public List<TransformerRegistration> registrations() { return OptimBootstrap.STATE.registrations(); }
}
