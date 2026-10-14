package org.polydevs.opusmc.optim.renderer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.stream.Stream;
import org.embeddedt.embeddium.impl.common.util.NativeBuffer;
import org.embeddedt.embeddium.impl.gl.arena.GlBufferArena;
import org.embeddedt.embeddium.impl.gl.arena.GlBufferSegment;
import org.embeddedt.embeddium.impl.gl.arena.PendingUpload;
import org.embeddedt.embeddium.impl.gl.arena.staging.StagingBuffer;
import org.embeddedt.embeddium.impl.gl.buffer.GlBuffer;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferTarget;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;

/** Dynamically links the unmodified LGPL Celeritas arena and its LWJGL2 backend. */
public final class CeleritasStorage implements RegionRenderer.Storage {
    private final GlBufferArena arena;
    public CeleritasStorage(int stride) {
        RenderDevice.enterManagedCode();
        try (CommandList commands = RenderDevice.INSTANCE.createCommandList()) {
            arena = new GlBufferArena(commands, stride, 65536, new DirectStaging());
        } finally { RenderDevice.exitManagedCode(); }
    }
    @Override public RegionRenderer.Segment upload(ByteBuffer data) {
        NativeBuffer nativeData = NativeBuffer.copy(data);
        try {
            PendingUpload upload = PendingUpload.of(nativeData);
            RenderDevice.enterManagedCode();
            try (CommandList commands = RenderDevice.INSTANCE.createCommandList()) {
                arena.upload(commands, Stream.of(upload));
            } finally { RenderDevice.exitManagedCode(); }
            return new Allocation(upload.getResult());
        } finally { nativeData.free(); }
    }
    @Override public void draw(int mode, IntBuffer first, IntBuffer count, Runnable pointers) {
        RenderDevice.enterManagedCode();
        try (CommandList commands = RenderDevice.INSTANCE.createCommandList()) {
            commands.bindBuffer(GlBufferTarget.ARRAY_BUFFER, arena.getBufferObject());
            pointers.run();
            GL14.glMultiDrawArrays(mode, first, count);
        } finally { RenderDevice.exitManagedCode(); }
    }
    @Override public void close() {
        RenderDevice.enterManagedCode();
        try (CommandList commands = RenderDevice.INSTANCE.createCommandList()) { arena.delete(commands); }
        finally { RenderDevice.exitManagedCode(); }
    }
    int bufferHandle() { return arena.getBufferObject().handle(); }
    private static final class Allocation implements RegionRenderer.Segment {
        private final GlBufferSegment segment;
        Allocation(GlBufferSegment segment) { this.segment = segment; }
        public int offset() { return segment.getOffset(); }
        public int length() { return segment.getLength(); }
        public void free() { segment.delete(); }
    }
    /** Single direct upload; Celeritas still owns resize/compaction/copy fallback. */
    private static final class DirectStaging implements StagingBuffer {
        public void enqueueCopy(CommandList commands, ByteBuffer data, GlBuffer dst, long offset) {
            commands.bindBuffer(GlBufferTarget.ARRAY_BUFFER, dst);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, offset, data);
        }
        public void flush(CommandList commands) { }
        public void delete(CommandList commands) { }
        public void flip() { }
    }
}
