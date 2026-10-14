package org.polydevs.opusmc.optim.renderer;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public final class VertexPointersTest {
    public static final class Pointers {
        int calls;
        boolean fail;
        public void a() { calls++; if (fail) throw new IllegalArgumentException("fixture setup failure"); }
    }
    public static final class WrongShape { public int a() { return 1; } }

    @Test void oneLinkedSetupServesDifferentListsAndSkipsEmptyBatch() throws Exception {
        VertexPointers setup = new VertexPointers(Pointers.class);
        RegionRendererTest.Storage storage = new RegionRendererTest.Storage();
        RegionRenderer region = new RegionRenderer(28, storage);
        Pointers first = new Pointers(), second = new Pointers();
        Object mesh = new Object();
        region.upload(mesh, ByteBuffer.allocate(112));
        region.queue(7, mesh); setup.finish(region, first);
        region.queue(7, mesh); setup.finish(region, second);
        setup.finish(region, second);
        assertEquals(1, first.calls); assertEquals(1, second.calls);
        assertEquals(2, region.draws());
        region.close();
    }

    @Test void failedSetupDoesNotRetainBatchOrPoisonNextInvocation() throws Exception {
        VertexPointers setup = new VertexPointers(Pointers.class);
        RegionRenderer region = new RegionRenderer(28, new RegionRendererTest.Storage());
        Object mesh = new Object(); region.upload(mesh, ByteBuffer.allocate(112));
        Pointers failed = new Pointers(); failed.fail = true;
        region.queue(7, mesh);
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> setup.finish(region, failed));
        assertInstanceOf(IllegalArgumentException.class, error.getCause());
        assertEquals(0, region.draws());
        Pointers next = new Pointers(); region.queue(7, mesh); setup.finish(region, next);
        assertEquals(1, next.calls); assertEquals(1, region.draws()); region.close();
    }

    @Test void missingVoidSetupFailsBeforeRendering() {
        assertThrows(NoSuchMethodException.class, () -> new VertexPointers(WrongShape.class));
    }

    @Test void foreignThreadCannotChangeReusableCallback() throws Exception {
        VertexPointers setup = new VertexPointers(Pointers.class);
        RegionRenderer region = new RegionRenderer(28, new RegionRendererTest.Storage());
        Throwable[] error = {null};
        Thread foreign = new Thread(() -> { try { setup.finish(region, new Pointers()); } catch (Throwable e) { error[0] = e; } });
        foreign.start(); foreign.join();
        assertInstanceOf(IllegalStateException.class, error[0]);
        region.close();
    }
}
