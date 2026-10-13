package org.polydevs.opusmc.optim.platform;

import java.util.List;
import org.polydevs.opusmc.bootstrap.*;

/** Loader-owned SPI; construction does no config I/O or game/GL initialization. */
public final class OptimTransformerProvider implements ClassTransformerProvider {
    public static final String ID = "opus.optim";
    public OptimTransformerProvider() { }
    @Override public String id() { return ID; }
    @Override public List<TransformerRegistration> registrations() { return OptimBootstrap.STATE.registrations(); }
}
