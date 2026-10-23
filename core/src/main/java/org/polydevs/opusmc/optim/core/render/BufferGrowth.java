package org.polydevs.opusmc.optim.core.render;

/** Bounded geometric headroom; required capacity above the ceiling is always retained. */
public final class BufferGrowth {
    private static final int HEADROOM_CEILING = 32 * 1024 * 1024;
    private BufferGrowth() { }
    /** Chunk builders start small; their existing growth path still serves any required size. */
    public static int initialChunkInts(int requested) { return Math.min(requested, 8192); }
    public static int capacity(int required, int previous) {
        if (previous <= 0 || required <= previous || previous >= HEADROOM_CEILING) { return required; }
        int geometric = (int) Math.min((long) previous * 2, HEADROOM_CEILING);
        return Math.max(required, geometric);
    }
}
