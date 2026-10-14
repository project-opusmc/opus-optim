package org.polydevs.opusmc.optim.renderer;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.concurrent.Future;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL15;

/** Display/context capability is established once on the actual render thread. */
public final class RendererHooks {
    private static final RegionLifetime LIFETIME = new RegionLifetime();
    private static volatile Class<?> config;
    private static volatile boolean ready;
    private static volatile long regionsCreated, regionsDeleted;
    private static Method shaders, multiTexture, settings;
    private static VertexPointers pointers;
    private static Field serverThread;
    private RendererHooks() { }
    public static void initialize(Class<?> configClass) {
        if (!"celeritas-region".equals(System.getProperty("opus.optim.renderer"))) return;
        if (ready || !Display.isCreated()) return;
        try {
            ClassLoader loader = configClass.getClassLoader();
            Class.forName("net.optifine.render.VboRegion", false, loader);
            Class.forName("bqs", false, loader);
            Class.forName("bfr", false, loader);
            Class.forName("ave", false, loader);
            Class.forName("bpo", false, loader);
            if (!RendererTransformer.allApplied()) throw new IllegalStateException("O2 renderer hooks are incomplete");
            if (!GLContext.getCapabilities().OpenGL21) throw new IllegalStateException("O2 region backend requires OpenGL2.1");
            config = configClass;
            shaders = configClass.getMethod("isShaders"); multiTexture = configClass.getMethod("isMultiTexture"); settings = configClass.getMethod("getGameSettings");
            pointers = new VertexPointers(Class.forName("bfy", false, loader));
            serverThread = Class.forName("net.minecraft.server.MinecraftServer", false, loader).getDeclaredField("aa");
            serverThread.setAccessible(true);
            // Force framework/service linkage while startup can still fail clearly.
            Class.forName("org.taumc.celeritas.lwjgl.LWJGLServiceProvider", true, RendererHooks.class.getClassLoader());
            ready = true;
            System.out.println("[O2] Celeritas Common 2.5.0-pre.1 / LWJGL2 region backend READY; frame/FPS acceptance pending");
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("O2 renderer initialization failed", e); }
    }
    public static boolean regions(boolean original) { return original || compatible(); }
    public static boolean vboRegions(boolean original) { return original || compatible(); }
    private static boolean compatible() {
        if (!ready) return false;
        try {
            Object options = settings.invoke(null);
            return options != null && options.getClass().getField("u").getBoolean(options)
                    && !((Boolean)shaders.invoke(null)).booleanValue() && !((Boolean)multiTexture.invoke(null)).booleanValue();
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("O2 renderer compatibility query failed", e); }
    }
    public static RegionRenderer create(int stride) {
        if (!compatible()) return null;
        RegionRenderer renderer = new RegionRenderer(stride, new CeleritasStorage(stride)); regionsCreated++; LIFETIME.add(renderer);
        if (regionsCreated == 1) System.out.println("[O2] custom region ATTACHED stride="+stride);
        return renderer;
    }
    public static int releaseOriginal(int original, RegionRenderer renderer) {
        if (renderer == null) return original;
        if (original >= 0) GL15.glDeleteBuffers(original);
        return -1;
    }
    public static void finish(RegionRenderer renderer, Object renderList) {
        long before = renderer.draws();
        pointers.finish(renderer, renderList);
        if (renderer.draws() > before && !submitted) { submitted = true; System.out.println("[O2] custom region DRAW_SUBMITTED vertices="+renderer.submittedVertices()); }
    }
    private static volatile boolean submitted;
    public static void delete(RegionRenderer renderer) { if (LIFETIME.release(renderer)) regionsDeleted++; }
    public static Future<?> prepareServerTaskWait(Future<?> future, Object server) {
        if(serverThread==null)return future;
        try {
            Future<?> prepared=ServerTaskWait.prepare(future, (Thread)serverThread.get(server));
            if(prepared!=future)System.out.println("[O2] abandoned shutdown future released after server thread TERMINATED");
            return prepared;
        }
        catch (IllegalAccessException error) { throw new IllegalStateException("O2 server lifetime unavailable", error); }
    }
    public static void worldChanging(Object world) { if (world == null) worldReset(); }
    public static void worldReset() { regionsDeleted += LIFETIME.reset(); }
    public static int liveRegions() { return LIFETIME.size(); }
    public static long regionsCreated() { return regionsCreated; }
    public static long regionsDeleted() { return regionsDeleted; }
    public static boolean ready() { return ready; }
}
