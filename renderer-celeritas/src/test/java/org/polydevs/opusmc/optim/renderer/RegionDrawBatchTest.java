package org.polydevs.opusmc.optim.renderer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class RegionDrawBatchTest {
    private static final class Segment implements RegionRenderer.Segment {
        int offset, length;
        Segment(int offset, int length) { this.offset = offset; this.length = length; }
        public int offset() { return offset; }
        public int length() { return length; }
        public void free() { }
    }
    private static final class DrawSink implements RegionRenderer.Storage {
        final List<Segment> segments = new ArrayList<>();
        final List<Integer> commands = new ArrayList<>();
        int nextOffset;
        boolean fail;
        public RegionRenderer.Segment upload(ByteBuffer data) {
            Segment segment = new Segment(nextOffset, data.remaining() / 28);
            nextOffset += segment.length;
            segments.add(segment);
            return segment;
        }
        public void draw(int mode, IntBuffer first, IntBuffer count, Runnable pointers) {
            pointers.run();
            if (fail) throw new IllegalStateException("draw failed");
            assertEquals(first.remaining(), count.remaining());
            while (first.hasRemaining()) { commands.add(first.get()); commands.add(count.get()); }
        }
        public void close() { }
    }
    private static Object upload(RegionRenderer renderer, int vertices) {
        Object key = new Object();
        renderer.upload(key, ByteBuffer.allocate(vertices * 28));
        return key;
    }
    @Test void contiguousCompletePrimitivesUseOneCommandWithoutDroppingVertices() {
        for (int[] primitive : new int[][]{{0, 1}, {1, 2}, {4, 3}, {7, 4}}) {
            DrawSink sink = new DrawSink();
            try (RegionRenderer renderer = new RegionRenderer(28, sink)) {
                sink.nextOffset = 12;
                Object a = upload(renderer, primitive[1]), b = upload(renderer, primitive[1] * 2);
                renderer.queue(primitive[0], a); renderer.queue(primitive[0], b);
                renderer.finish(() -> { });
                assertEquals(Arrays.asList(12, primitive[1] * 3), sink.commands);
                assertEquals(primitive[1] * 3, renderer.submittedVertices());
            }
        }
    }
    @Test void incompletePrimitivesCannotCreateNewGeometryAcrossChunkBoundaries() {
        DrawSink sink = new DrawSink();
        try (RegionRenderer renderer = new RegionRenderer(28, sink)) {
            Object a = upload(renderer, 3), b = upload(renderer, 1);
            renderer.queue(7, a); renderer.queue(7, b); renderer.finish(() -> { });
            assertEquals(Arrays.asList(0, 3, 3, 1), sink.commands);
        }
    }
    @Test void connectedPrimitivesAndUnknownModesKeepTheirRestartBoundaries() {
        for (int mode : new int[]{2, 3, 5, 6, 8, 9, -1}) {
            DrawSink sink = new DrawSink();
            try (RegionRenderer renderer = new RegionRenderer(28, sink)) {
                Object a = upload(renderer, 4), b = upload(renderer, 4);
                renderer.queue(mode, a); renderer.queue(mode, b); renderer.finish(() -> { });
                assertEquals(Arrays.asList(0, 4, 4, 4), sink.commands, "mode " + mode);
            }
        }
    }
    @Test void gapsReverseOrderAndRepeatedMeshesKeepTheExactSubmissionOrder() {
        DrawSink sink = new DrawSink();
        try (RegionRenderer renderer = new RegionRenderer(28, sink)) {
            Object a = upload(renderer, 4);
            sink.nextOffset = 12;
            Object b = upload(renderer, 4);
            renderer.queue(7, b); renderer.queue(7, a); renderer.queue(7, a); renderer.queue(7, b);
            renderer.finish(() -> { });
            assertEquals(Arrays.asList(12, 4, 0, 4, 0, 4, 12, 4), sink.commands);
        }
    }
    @Test void arenaRelocationIsResolvedAtFinishBeforeDecidingAdjacency() {
        DrawSink sink = new DrawSink();
        try (RegionRenderer renderer = new RegionRenderer(28, sink)) {
            Object a = upload(renderer, 4), b = upload(renderer, 4);
            renderer.queue(7, a); renderer.queue(7, b);
            sink.segments.get(0).offset = 80; sink.segments.get(1).offset = 84;
            renderer.finish(() -> { });
            assertEquals(Arrays.asList(80, 8), sink.commands);
        }
    }
    @Test void growingQueueCoalescesAcrossThePreviousCapacityBoundary() {
        DrawSink sink = new DrawSink();
        try (RegionRenderer renderer = new RegionRenderer(28, sink)) {
            Object[] keys = new Object[1024];
            for (int i = 0; i < keys.length; i++) keys[i] = upload(renderer, 4);
            for (Object key : keys) renderer.queue(7, key);
            renderer.finish(() -> { });
            assertEquals(Arrays.asList(0, 4096), sink.commands);
        }
    }
    @Test void coalescingCannotOverflowTheGlSignedCount() {
        DrawSink sink = new DrawSink();
        try (RegionRenderer renderer = new RegionRenderer(28, sink)) {
            Object a = upload(renderer, 4), b = upload(renderer, 4);
            sink.segments.get(0).length = 2147483644;
            sink.segments.get(1).offset = 2147483644;
            renderer.queue(7, a); renderer.queue(7, b); renderer.finish(() -> { });
            assertEquals(Arrays.asList(0, 2147483644, 2147483644, 4), sink.commands);
        }
    }
    @Test void failedDrawClearsTheBatchSoReplacementCanProceed() {
        DrawSink sink = new DrawSink();
        try (RegionRenderer renderer = new RegionRenderer(28, sink)) {
            Object a = upload(renderer, 4), b = upload(renderer, 4);
            renderer.queue(7, a); renderer.queue(7, b);
            sink.fail = true;
            assertThrows(IllegalStateException.class, () -> renderer.finish(() -> { }));
            assertEquals(0, renderer.draws());
            assertEquals(0, renderer.submittedVertices());
            renderer.upload(a, ByteBuffer.allocate(112));
            sink.fail = false;
            renderer.finish(() -> fail("failed batch must be cleared"));
            renderer.queue(7, a); renderer.finish(() -> { });
            assertEquals(Arrays.asList(8, 4), sink.commands);
        }
    }
}
