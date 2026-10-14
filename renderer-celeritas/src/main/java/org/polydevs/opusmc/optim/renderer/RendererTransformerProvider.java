package org.polydevs.opusmc.optim.renderer;

import java.util.*;
import org.polydevs.opusmc.bootstrap.*;

/** Loader-owned managed provider; no second tweaker or classloader. */
public final class RendererTransformerProvider implements ClassTransformerProvider {
    public String id() { return "opus.optim.renderer"; }
    public void initialize(ClassLoader gameLoader) {
        try {
            if (Class.forName(RendererHooks.class.getName(),false,gameLoader)!=RendererHooks.class)
                throw new IllegalArgumentException("O2 renderer helper has a different game-loader identity");
        } catch (ClassNotFoundException error) { throw new IllegalArgumentException("O2 renderer helper is not visible",error); }
    }
    public List<TransformerRegistration> registrations() {
        final RendererTransformer renderer=new RendererTransformer();
        ClassTransformer bridge=new ClassTransformer() {
            public String id() { return "render.celeritas-region"; }
            public int priority() { return 0; }
            public byte[] transform(String name,byte[] bytes) { return renderer.transform(name,name,bytes); }
        };
        return Collections.singletonList(new TransformerRegistration(id(),bridge,RendererTransformer.targetClasses()));
    }
}
