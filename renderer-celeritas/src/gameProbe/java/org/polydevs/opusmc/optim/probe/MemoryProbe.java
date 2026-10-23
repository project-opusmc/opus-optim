package org.polydevs.opusmc.optim.probe;

import java.lang.instrument.Instrumentation;
import java.lang.management.*;
import java.lang.reflect.Field;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

/** Test-only phase observations; no forced GC or per-frame profiler overhead. */
final class MemoryProbe {
    private MemoryProbe() { }

    static void snapshot(String phase, Instrumentation instrumentation) throws Exception {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        long direct = 0, directCapacity = 0, directCount = 0, gcCount = 0, gcMillis = 0;
        for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
            if (pool.getName().equals("direct")) {
                direct = pool.getMemoryUsed(); directCapacity = pool.getTotalCapacity(); directCount = pool.getCount();
            }
        }
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc.getCollectionCount() >= 0) gcCount += gc.getCollectionCount();
            if (gc.getCollectionTime() >= 0) gcMillis += gc.getCollectionTime();
        }
        long glyphBytes = 0, glyphPayload = 0; int glyphTables = 0;
        long regionGpuBytes = 0; int regionArenas = 0, emptyRegionArenas = 0;
        long chunkIdleBytes = 0; int chunkIdleBuilders = 0, chunkConfiguredBuilders = 0, chunkWorkers = 0;
        for (Class<?> type : instrumentation.getAllLoadedClasses()) {
            if (type.getName().equals("ave")) {
                Object game = type.getMethod("A").invoke(null), render = type.getField("g").get(game);
                for (Field field : render.getClass().getDeclaredFields()) {
                    if (!field.getType().getName().equals("bho")) continue;
                    field.setAccessible(true); Object dispatcher = field.get(render);
                    Field pool = dispatcher.getClass().getDeclaredField("e"); pool.setAccessible(true);
                    Field count = dispatcher.getClass().getDeclaredField("countRenderBuilders"); count.setAccessible(true);
                    Field workers = dispatcher.getClass().getDeclaredField("c"); workers.setAccessible(true);
                    chunkConfiguredBuilders = count.getInt(dispatcher);
                    chunkWorkers = ((java.util.List<?>) workers.get(dispatcher)).size();
                    for (Object builder : (java.util.Collection<?>) pool.get(dispatcher)) {
                        chunkIdleBuilders++;
                        Field layers = builder.getClass().getDeclaredField("a"); layers.setAccessible(true);
                        for (Object layer : (Object[]) layers.get(builder)) {
                            Field bytes = layer.getClass().getDeclaredField("a"); bytes.setAccessible(true);
                            chunkIdleBytes += ((java.nio.ByteBuffer) bytes.get(layer)).capacity();
                        }
                    }
                }
            }
            if (type.getName().equals("org.polydevs.opusmc.optim.renderer.RendererHooks")) {
                Field lifetimeField = type.getDeclaredField("LIFETIME"); lifetimeField.setAccessible(true);
                Object lifetime = lifetimeField.get(null);
                Field liveField = lifetime.getClass().getDeclaredField("live"); liveField.setAccessible(true);
                int previous = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
                try {
                    for (Object region : (java.util.Set<?>) liveField.get(lifetime)) {
                        Field storageField = region.getClass().getDeclaredField("storage"); storageField.setAccessible(true);
                        Object storage = storageField.get(region);
                        Field arenaField = storage.getClass().getDeclaredField("arena"); arenaField.setAccessible(true);
                        if (arenaField.get(storage) == null) continue;
                        java.lang.reflect.Method handle = storage.getClass().getDeclaredMethod("bufferHandle"); handle.setAccessible(true);
                        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, ((Integer) handle.invoke(storage)).intValue());
                        regionGpuBytes += GL15.glGetBufferParameteri(GL15.GL_ARRAY_BUFFER, GL15.GL_BUFFER_SIZE);
                        regionArenas++;
                        if (((Integer) region.getClass().getMethod("meshCount").invoke(region)).intValue() == 0) emptyRegionArenas++;
                    }
                } finally { GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previous); }
            }
            if (!type.getName().equals("avn") && !type.getName().equals("net.minecraft.client.gui.FontRenderer")) continue;
            for (Field field : type.getDeclaredFields()) {
                if (!field.getName().equals("o2$glyphIndex")) continue;
                field.setAccessible(true); Object value = field.get(null);
                if (value instanceof short[]) {
                    glyphTables++; glyphBytes += instrumentation.getObjectSize(value);
                    glyphPayload += ((short[]) value).length * 2L;
                }
            }
        }
        // macOS ps reports KiB. RSS includes the JVM, native libraries and mapped pages;
        // it is not a live-heap measurement and cannot be attributed to this small table.
        long rssBytes = -1;
        if (System.getProperty("os.name").startsWith("Mac")) {
            String pid = ManagementFactory.getRuntimeMXBean().getName().split("@", 2)[0];
            if (!pid.matches("[0-9]+")) throw new IllegalStateException("Non-numeric process identity");
            Process ps = new ProcessBuilder("/bin/ps", "-o", "rss=", "-p", pid).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(ps.getInputStream(), "UTF-8"))) {
                String line = reader.readLine();
                if (ps.waitFor() != 0 || line == null) throw new IllegalStateException("RSS observation failed");
                rssBytes = Long.parseLong(line.trim()) * 1024L;
            }
        }
        System.out.println("[O2/MEMORY] phase=" + phase + " heapUsed=" + memory.getHeapMemoryUsage().getUsed()
                + " heapCommitted=" + memory.getHeapMemoryUsage().getCommitted()
                + " nonHeapUsed=" + memory.getNonHeapMemoryUsage().getUsed()
                + " directUsed=" + direct + " directCapacity=" + directCapacity + " directCount=" + directCount
                + " gcCount=" + gcCount + " gcMillis=" + gcMillis + " rssBytes=" + rssBytes
                + " glyphTables=" + glyphTables + " glyphArrayBytes=" + glyphBytes + " glyphPayloadBytes=" + glyphPayload
                + " regionGpuBytes=" + regionGpuBytes + " regionArenas=" + regionArenas + " emptyRegionArenas=" + emptyRegionArenas);
        System.out.println("[O2/MEMORY_POOL] phase=" + phase + " configuredBuilders=" + chunkConfiguredBuilders
                + " workers=" + chunkWorkers + " idleBuilders=" + chunkIdleBuilders + " idleDirectCapacity=" + chunkIdleBytes);
    }
}
