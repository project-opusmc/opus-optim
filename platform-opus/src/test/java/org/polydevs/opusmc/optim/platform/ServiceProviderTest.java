package org.polydevs.opusmc.optim.platform;

import java.net.URLClassLoader;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.polydevs.opusmc.bootstrap.*;
import org.polydevs.opusmc.optim.core.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.polydevs.opusmc.optim.platform.ProviderPlanTest.*;

class ServiceProviderTest {
    @Test void actualSpiDiscoveryRoutesAnOutsideTargetAndExecutesTheSharedHelper() throws Exception {
        Fixtures.GameLoader game = new Fixtures.GameLoader(getClass().getClassLoader());
        TransformerBinding candidate = new TransformerBinding(descriptor("o2.probe", "fixture.FrameLoop", 0, ""),
                Fixtures.injectingTransformer(), Collections.singleton(Fixtures.Helper.class));
        ProviderPlan prepared = prepare(on("o2.probe"), CrashMarker.Recovery.clean(),
                Collections.singleton(candidate), Collections.emptyList(), game);
        try (URLClassLoader providers = Fixtures.isolatedProviderLoader()) {
            Class.forName(OptimBootstrap.class.getName(), true, providers)
                    .getMethod("install", ProviderPlan.class).invoke(null, prepared);
            TransformerPlan denied = TransformerPlan.discover(providers, Collections.emptySet(), Collections.emptyList());
            assertEquals(0, denied.transformerCount());
            TransformerPlan approved = TransformerPlan.discover(providers, Collections.singleton("opus.optim"), Collections.emptyList());
            assertTrue(approved.targets("fixture/FrameLoop"));
            assertFalse(approved.targets("fixture/Other"));
            assertEquals(1, approved.transformerCount());
            byte[] transformed = approved.chain().transform("fixture/FrameLoop", Fixtures.gameBytes());
            Fixtures.Helper.hits = 0;
            assertEquals(7, game.define(transformed).getMethod("run").invoke(null));
            assertEquals(1, Fixtures.Helper.hits);
        }
    }
    @Test void draftRejectsAnApprovedOffProviderAndOffDoesNotAddTransformsWhenUnapproved() throws Exception {
        try (URLClassLoader providers = Fixtures.isolatedProviderLoader()) {
            ClassTransformerProvider provider = ServiceLoader.load(ClassTransformerProvider.class, providers).iterator().next();
            assertEquals("opus.optim", provider.id());
            assertTrue(provider.registrations().isEmpty());
            assertEquals(0, TransformerPlan.discover(providers, Collections.emptySet(), Collections.emptyList()).transformerCount());
            assertThrows(IllegalArgumentException.class, () -> TransformerPlan.discover(
                    providers, Collections.singleton("opus.optim"), Collections.emptyList()));
        }
    }
}
