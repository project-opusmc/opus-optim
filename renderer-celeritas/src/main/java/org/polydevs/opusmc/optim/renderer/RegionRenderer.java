package org.polydevs.opusmc.optim.renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/** Owns one region; ordered draws resolve live arena offsets after relocation. */
public final class RegionRenderer implements AutoCloseable {
    public interface Segment { int offset(); int length(); void free(); }
    public interface Storage extends AutoCloseable {
        Segment upload(ByteBuffer data);
        default void bind() { throw new UnsupportedOperationException("External binding unavailable"); }
        void draw(int mode, IntBuffer first, IntBuffer count, Runnable pointers);
        @Override void close();
    }
    private final Thread owner = Thread.currentThread();
    private final int stride;
    private final Storage storage;
    private final Map<Object, Segment> meshes = new IdentityHashMap<>();
    private Segment[] queued = new Segment[256];
    private IntBuffer first = ints(256), count = ints(256);
    private int pending, mode;
    private boolean closed;
    private long uploads, draws, submittedVertices;

    public RegionRenderer(int stride, Storage storage) {
        if (stride <= 0) throw new IllegalArgumentException("Positive stride required");
        this.stride = stride;
        this.storage = Objects.requireNonNull(storage, "storage");
    }
    public void upload(Object range, ByteBuffer data) {
        check(); Objects.requireNonNull(range, "range"); Objects.requireNonNull(data, "data");
        if (pending != 0) throw new IllegalStateException("Upload during a queued region draw");
        if (data.remaining() % stride != 0) throw new IllegalArgumentException("Incomplete vertex");
        Segment old = meshes.remove(range);
        if (old != null) old.free();
        if (data.hasRemaining()) meshes.put(range, storage.upload(data.duplicate().order(data.order())));
        uploads++;
    }
    public void queue(int drawMode, Object range) {
        check();
        if (pending != 0 && mode != drawMode) throw new IllegalArgumentException("Mixed region draw modes");
        Segment segment = meshes.get(range);
        if (segment == null) return;
        if (pending == 65536) throw new IllegalStateException("Region draw queue exceeds 65536 entries");
        if (pending == queued.length) {
            Segment[] grown = new Segment[queued.length * 2];
            System.arraycopy(queued, 0, grown, 0, pending);
            queued = grown; first = ints(grown.length); count = ints(grown.length);
        }
        mode = drawMode; queued[pending++] = segment;
    }
    public void finish(Runnable pointers) {
        check(); Objects.requireNonNull(pointers, "pointers");
        if (pending == 0) return;
        first.clear(); count.clear(); long vertices = 0;
        try {
            // Only independent, complete primitives can cross a draw boundary.
            // Strips/fans/unknown modes retain their original restart semantics.
            int primitive = primitiveSize(mode);
            int start = 0, length = 0;
            for (int i = 0; i < pending; i++) {
                Segment segment = queued[i];
                int nextStart = segment.offset(), nextLength = segment.length();
                vertices += nextLength;
                if (i != 0 && primitive != 0 && (long) start + length == nextStart
                        && (long) length + nextLength <= Integer.MAX_VALUE
                        && length % primitive == 0 && nextLength % primitive == 0) {
                    length += nextLength;
                } else {
                    if (i != 0) { first.put(start); count.put(length); }
                    start = nextStart; length = nextLength;
                }
            }
            first.put(start); count.put(length);
            first.flip(); count.flip();
            storage.draw(mode, first, count, pointers);
            draws++; submittedVertices += vertices;
        } finally {
            for (int i = 0; i < pending; i++) queued[i] = null;
            pending = 0;
        }
    }
    public void bind() { check(); storage.bind(); }
    public int positionTop() { check(); int top = 0; for (Segment s : meshes.values()) top = Math.max(top, Math.addExact(s.offset(), s.length())); return top; }
    public int meshCount() { check(); return meshes.size(); }
    public boolean isClosed() { return closed; }
    public long uploads() { return uploads; }
    public long draws() { return draws; }
    public long submittedVertices() { return submittedVertices; }
    @Override public void close() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("Region accessed from another thread");
        if (closed) return;
        for (Segment s : meshes.values()) s.free();
        meshes.clear();
        for (int i = 0; i < pending; i++) queued[i] = null;
        pending = 0; storage.close(); closed = true;
    }
    private void check() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("Region accessed from another thread");
        if (closed) throw new IllegalStateException("Region deleted");
    }
    private static int primitiveSize(int mode) {
        switch (mode) {
            case 0: return 1; // GL_POINTS
            case 1: return 2; // GL_LINES
            case 4: return 3; // GL_TRIANGLES
            case 7: return 4; // GL_QUADS
            default: return 0;
        }
    }
    private static IntBuffer ints(int n) { return ByteBuffer.allocateDirect(n * 4).order(ByteOrder.nativeOrder()).asIntBuffer(); }
}
