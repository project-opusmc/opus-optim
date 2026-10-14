package org.polydevs.opusmc.optim.renderer;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.polydevs.opusmc.bootstrap.*;
import static org.junit.jupiter.api.Assertions.*;
final class RendererProviderTest {
    @Test void rendererProviderDeclaresEarlyPinnedTargetsAndRetainsOffBytes() throws Exception {
        String previous=System.getProperty("opus.optim.renderer");
        System.setProperty("opus.optim.renderer","off");
        try {
            RendererTransformerProvider provider=new RendererTransformerProvider();
            provider.initialize(getClass().getClassLoader());
            assertEquals("opus.optim.renderer",provider.id());
            List<TransformerRegistration> registrations=provider.registrations();
            assertEquals(1,registrations.size());
            assertEquals(7,registrations.get(0).targetClasses().size());
            assertEquals(0,registrations.get(0).transformer().priority());
            byte[] bytes={1,2,3};
            assertSame(bytes,registrations.get(0).transformer().transform("ave",bytes));
        } finally { if(previous==null)System.clearProperty("opus.optim.renderer");else System.setProperty("opus.optim.renderer",previous); }
    }
}
