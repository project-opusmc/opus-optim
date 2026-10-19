package org.polydevs.opusmc.optim.renderer;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/** CPU queue/packing cost only. No GL driver, game frame or FPS measurement. */
public final class RegionBatchBench {
    private static final Runnable POINTERS = () -> { };
    private static volatile long blackhole;
    private RegionBatchBench() { }
    private static final class Allocation implements RegionRenderer.Segment {
        final int offset;
        Allocation(int offset) { this.offset = offset; }
        public int offset() { return offset; }
        public int length() { return 4; }
        public void free() { }
    }
    private static final class Sink implements RegionRenderer.Storage {
        final int step;
        int offset;
        long commands, checksum;
        Sink(int step) { this.step = step; }
        public RegionRenderer.Segment upload(ByteBuffer data) {
            Allocation allocation = new Allocation(offset); offset += step;
            return allocation;
        }
        public void draw(int mode, IntBuffer first, IntBuffer count, Runnable pointers) {
            commands += count.remaining();
            checksum += first.get(first.position()) + count.get(count.position()) + count.remaining();
        }
        public void close() { }
    }
    public static void main(String[] args) {
        if (!System.getProperty("java.version").startsWith("1.8.")) throw new IllegalStateException("Java8 required");
        String layout = args[0];
        if (!layout.equals("contiguous") && !layout.equals("fragmented") && !layout.equals("reverse")) {
            throw new IllegalArgumentException("Unknown layout");
        }
        int warmup = Integer.parseInt(args[1]), batches = Integer.parseInt(args[2]);
        final int ranges = 128;
        Sink sink = new Sink(layout.equals("fragmented") ? 8 : 4);
        try (RegionRenderer renderer = new RegionRenderer(28, sink)) {
            Object[] keys = new Object[ranges];
            ByteBuffer mesh = ByteBuffer.allocate(112);
            for (int i = 0; i < ranges; i++) { keys[i] = new Object(); renderer.upload(keys[i], mesh); }
            if (layout.equals("reverse")) for (int i = 0; i < ranges / 2; i++) {
                Object key = keys[i]; keys[i] = keys[ranges - 1 - i]; keys[ranges - 1 - i] = key;
            }
            for (int i = 0; i < warmup; i++) batch(renderer, keys);
            ThreadMXBean allocations = (ThreadMXBean) ManagementFactory.getThreadMXBean();
            if (!allocations.isThreadAllocatedMemorySupported()) throw new IllegalStateException("Allocation counter unavailable");
            allocations.setThreadAllocatedMemoryEnabled(true);
            long thread = Thread.currentThread().getId();
            long beforeBytes = allocations.getThreadAllocatedBytes(thread), beforeCommands = sink.commands;
            long start = System.nanoTime();
            for (int i = 0; i < batches; i++) batch(renderer, keys);
            long elapsed = System.nanoTime() - start;
            long allocated = allocations.getThreadAllocatedBytes(thread) - beforeBytes;
            long expectedVertices = ((long) warmup + batches) * ranges * 4;
            if (renderer.submittedVertices() != expectedVertices || renderer.draws() != (long) warmup + batches) {
                throw new AssertionError("Workload did not complete");
            }
            blackhole = sink.checksum + renderer.submittedVertices();
            System.out.println("{\"layout\":\"" + layout + "\",\"warmupBatches\":" + warmup
                    + ",\"measuredBatches\":" + batches + ",\"rangesPerBatch\":" + ranges
                    + ",\"nanosPerBatch\":" + ((double) elapsed / batches)
                    + ",\"bytesPerBatch\":" + ((double) allocated / batches)
                    + ",\"commandsPerBatch\":" + ((sink.commands - beforeCommands) / batches)
                    + ",\"submittedVertices\":" + expectedVertices + ",\"checksum\":" + blackhole + "}");
        }
    }
    private static void batch(RegionRenderer renderer, Object[] keys) {
        for (Object key : keys) renderer.queue(7, key);
        renderer.finish(POINTERS);
    }
}
