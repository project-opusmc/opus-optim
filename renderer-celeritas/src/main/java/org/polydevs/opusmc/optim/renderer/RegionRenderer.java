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
    private Object[] queued = new Object[256];
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
        if (!meshes.containsKey(range)) return;
        if (pending == 65536) throw new IllegalStateException("Region draw queue exceeds 65536 entries");
        if (pending == queued.length) {
            Object[] grown = new Object[queued.length * 2];
            System.arraycopy(queued, 0, grown, 0, pending);
            queued = grown; first = ints(grown.length); count = ints(grown.length);
        }
        mode = drawMode; queued[pending++] = range;
    }
    public void finish(Runnable pointers) {
        check(); Objects.requireNonNull(pointers, "pointers");
        if (pending == 0) return;
        first.clear(); count.clear(); long vertices = 0;
        try {
            for (int i = 0; i < pending; i++) {
                Segment segment = meshes.get(queued[i]);
                first.put(segment.offset()); count.put(segment.length()); vertices += segment.length();
            }
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
    private static IntBuffer ints(int n) { return ByteBuffer.allocateDirect(n * 4).order(ByteOrder.nativeOrder()).asIntBuffer(); }
}
