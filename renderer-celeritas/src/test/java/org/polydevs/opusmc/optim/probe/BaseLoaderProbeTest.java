package org.polydevs.opusmc.optim.probe;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class BaseLoaderProbeTest {
    @Test void absentRendererControlNeverLoadsRendererHelpers() throws Exception {
        ClassLoader absent=new ClassLoader(getClass().getClassLoader()) {
            protected Class<?> loadClass(String name,boolean resolve) throws ClassNotFoundException {
                if(name.startsWith("org.polydevs.opusmc.optim.renderer."))throw new ClassNotFoundException(name);
                return super.loadClass(name,resolve);
            }
        };
        assertNull(GameProbeAgent.rendererHooks(true,absent));
        assertThrows(ClassNotFoundException.class,()->GameProbeAgent.rendererHooks(false,absent));
    }
}
