package org.polydevs.opusmc.optim.renderer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;

/** Links the host pointer setup once; one callback is reused by ordered batches. */
final class VertexPointers {
    private final Thread owner = Thread.currentThread();
    private final MethodHandle setup;
    private final Runnable callback = this::invoke;
    private Object current;

    VertexPointers(Class<?> renderListType) throws ReflectiveOperationException {
        setup = MethodHandles.publicLookup().findVirtual(renderListType, "a", MethodType.methodType(void.class))
                .asType(MethodType.methodType(void.class, Object.class));
    }

    void finish(RegionRenderer renderer, Object renderList) {
        if (Thread.currentThread() != owner) throw new IllegalStateException("Vertex setup accessed from another thread");
        if (current != null) throw new IllegalStateException("Nested vertex setup");
        current = Objects.requireNonNull(renderList, "renderList");
        try { renderer.finish(callback); } finally { current = null; }
    }

    private void invoke() {
        try { setup.invokeExact(current); }
        catch (Throwable error) { throw new IllegalStateException("O2 vertex-pointer setup failed", error); }
    }
}
