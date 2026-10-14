package org.polydevs.opusmc.optim.probe;
import java.awt.image.BufferedImage;import java.io.*;import java.lang.instrument.Instrumentation;import java.lang.reflect.*;import java.nio.*;import java.nio.file.*;import javax.imageio.ImageIO;import org.lwjgl.opengl.GL11;
/** Test-only agent: bounded offline workload in an explicitly supplied disposable directory. */
public final class GameProbeAgent {
    private static final java.util.concurrent.ConcurrentLinkedQueue<java.util.concurrent.FutureTask<?>> WORK = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static volatile boolean frameStarted;
    private GameProbeAgent() { }
    public static void premain(String expectedDirectory, Instrumentation ignored) {
        if(expectedDirectory==null || !expectedDirectory.contains("renderer-client"))throw new IllegalArgumentException("Explicit disposable renderer-client directory required");
        ignored.addTransformer(new ProbeFrameTransformer());
        Thread t=new Thread(()->run(expectedDirectory),"O2 disposable gameplay probe");t.setDaemon(true);t.start();
    }
    private static void run(String expected) {
        Object minecraft=null;Class<?> mc=null;
        try {
            final boolean baseline=Boolean.getBoolean("opus.optim.probe.baselineQuit");
            Class<?> launch=Class.forName("net.minecraft.launchwrapper.Launch");
            ClassLoader loader=null;long timeout=System.currentTimeMillis()+120000;
            while(loader==null && System.currentTimeMillis()<timeout){loader=(ClassLoader)launch.getField("classLoader").get(null);Thread.sleep(100);}
            if(loader==null)throw new IllegalStateException("LaunchWrapper unavailable");
            Class<?> hooks=Class.forName("org.polydevs.opusmc.optim.renderer.RendererHooks");
            while(!(baseline?frameStarted:((Boolean)hooks.getMethod("ready").invoke(null)).booleanValue()) && System.currentTimeMillis()<timeout)Thread.sleep(100);
            if(!(baseline?frameStarted:((Boolean)hooks.getMethod("ready").invoke(null)).booleanValue()))throw new IllegalStateException("Game renderer was not ready");
            if(baseline)System.out.println("[O2/PROBE] baseline quit control backend=off");
            mc=Class.forName("ave",true,loader);minecraft=mc.getMethod("A").invoke(null);
            File directory=(File)mc.getField("v").get(minecraft);
            if(!directory.getCanonicalPath().equals(new File(expected).getCanonicalPath()))throw new IllegalStateException("Refusing to touch another game instance");
            final Object game=minecraft;final Class<?> type=mc;final ClassLoader gameLoader=loader;
            schedule(type,game,()->{
                try {
                    Object options=type.getField("t").get(game); options.getClass().getField("z").setBoolean(options,false);
                    System.out.println("[O2/PROBE] disposable pauseOnLostFocus="+options.getClass().getField("z").getBoolean(options));
                    Class<?> settings=Class.forName("adp",true,gameLoader),mode=Class.forName("adp$a",true,gameLoader),worldType=Class.forName("adr",true,gameLoader);
                    Object creative=mode.getMethod("a",int.class).invoke(null,1);
                    Object worldSettings=settings.getConstructor(long.class,mode,boolean.class,boolean.class,worldType).newInstance(20261014L,creative,false,false,worldType.getField("c").get(null));
                    settings.getMethod("a",String.class).invoke(worldSettings,"3;minecraft:bedrock,3*minecraft:dirt,minecraft:grass;1;");
                    type.getMethod("a",String.class,String.class,settings).invoke(game,"O2RendererProbe","O2 renderer probe",worldSettings);
                    System.out.println("[O2/PROBE] offline world launch requested");
                }catch(Exception e){throw new IllegalStateException(e);}
            });
            Thread.sleep(6000);
            schedule(type,game,()->{
                try {Object screen=type.getField("m").get(game); Object world=type.getField("f").get(game);Object render=type.getField("g").get(game);System.out.println("[O2/PROBE] world="+(world!=null)+" screen="+(screen==null?"none":screen.getClass().getName())+" chunks="+render.getClass().getMethod("f").invoke(render));}
                catch(Exception e){throw new IllegalStateException(e);}
            });
            if(!baseline){
                Field submitted=hooks.getDeclaredField("submitted");submitted.setAccessible(true);
                timeout=System.currentTimeMillis()+90000;
                while(!submitted.getBoolean(null) && System.currentTimeMillis()<timeout)Thread.sleep(200);
                if(!submitted.getBoolean(null))throw new IllegalStateException("No actual O2 terrain draw observed");
            }
            Thread.sleep(5000);
            schedule(type,game,()->{
                try {
                    int width=type.getField("d").getInt(game),height=type.getField("e").getInt(game);
                    ByteBuffer pixels=ByteBuffer.allocateDirect(Math.multiplyExact(Math.multiplyExact(width,height),4));
                    GL11.glReadPixels(0,0,width,height,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
                    BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
                    for(int y=0;y<height;y++)for(int x=0;x<width;x++){int i=(y*width+x)*4;image.setRGB(x,height-1-y,((pixels.get(i+3)&255)<<24)|((pixels.get(i)&255)<<16)|((pixels.get(i+1)&255)<<8)|(pixels.get(i+2)&255));}
                    Path output=Paths.get(expected).resolve(baseline?"baseline-terrain.png":"o2-terrain.png");ImageIO.write(image,"png",output.toFile());
                    int error=GL11.glGetError();if(error!=0)throw new IllegalStateException("GL error="+error);
                    System.out.println("[O2/PROBE] actual terrain frame="+width+"x"+height+" regionsCreated="+hooks.getMethod("regionsCreated").invoke(null)+" glError="+error);
                }catch(Exception e){throw new IllegalStateException(e);}
            });
            if(!baseline){
            reload(type,game,hooks,true,"renderer-reload");
            awaitActiveDraws(type,game,hooks,"renderer-reload");
            reload(type,game,hooks,false,"vbo-off");
            Thread.sleep(3000);
            schedule(type,game,()->{
                try {if(((Integer)hooks.getMethod("liveRegions").invoke(null)).intValue()!=0)throw new IllegalStateException("O2 remained active with VBO off");if(GL11.glGetError()!=0)throw new IllegalStateException("GL error in VBO fallback");System.out.println("[O2/PROBE] vbo-off fallback liveRegions=0 glError=0");}
                catch(Exception e){throw new IllegalStateException(e);}
            });
            reload(type,game,hooks,true,"vbo-on");
            awaitActiveDraws(type,game,hooks,"vbo-on");
            }
            schedule(type,game,()->{
                try {
                    quitPackets(type,game);
                    System.out.println("[O2/PROBE] integrated world API unload BEGIN");
                    System.out.println("[O2/PROBE] unload BEGIN liveRegions="+hooks.getMethod("liveRegions").invoke(null));
                    type.getMethod("a",Class.forName("bdb",false,gameLoader)).invoke(game,new Object[]{null});
                    System.out.println("[O2/PROBE] unload END liveRegions="+hooks.getMethod("liveRegions").invoke(null));
                    // Match normal quit-to-menu semantics before the next game tick.
                    type.getMethod("a",Class.forName("axu",false,gameLoader)).invoke(game,Class.forName("aya",true,gameLoader).getConstructor().newInstance());
                    int live=((Integer)hooks.getMethod("liveRegions").invoke(null)).intValue();
                    System.out.println("[O2/PROBE] unload regionsCreated="+hooks.getMethod("regionsCreated").invoke(null)+" regionsDeleted="+hooks.getMethod("regionsDeleted").invoke(null)+" liveRegions="+live);
                    if(live!=0)throw new IllegalStateException("Region leak after world unload");
                    if(((Long)hooks.getMethod("regionsCreated").invoke(null)).longValue()!=((Long)hooks.getMethod("regionsDeleted").invoke(null)).longValue())throw new IllegalStateException("Region lifecycle count mismatch");
                }catch(Exception e){throw new IllegalStateException(e);}
            });
            Thread.sleep(2000);
            schedule(type,game,()->{
                try {
                    // The saved disposable world supplies its own WorldSettings.
                    type.getMethod("a",String.class,String.class,Class.forName("adp",false,gameLoader)).invoke(game,"O2RendererProbe","O2 renderer probe",null);
                    System.out.println("[O2/PROBE] offline world reopen requested");
                }catch(Exception e){throw new IllegalStateException(e);}
            });
            if(baseline)Thread.sleep(6000);else awaitActiveDraws(type,game,hooks,"world-reopen");
            if(!baseline)Thread.sleep(6000);
            schedule(type,game,()->{
                try {
                    quitPackets(type,game);
                    System.out.println("[O2/PROBE] second integrated world API unload BEGIN");
                    type.getMethod("a",Class.forName("bdb",false,gameLoader)).invoke(game,new Object[]{null});
                    type.getMethod("a",Class.forName("axu",false,gameLoader)).invoke(game,Class.forName("aya",true,gameLoader).getConstructor().newInstance());
                    int live=((Integer)hooks.getMethod("liveRegions").invoke(null)).intValue();
                    long created=((Long)hooks.getMethod("regionsCreated").invoke(null)).longValue(),deleted=((Long)hooks.getMethod("regionsDeleted").invoke(null)).longValue();
                    if(live!=0||created!=deleted)throw new IllegalStateException("Region leak after second world unload");
                    if(GL11.glGetError()!=0)throw new IllegalStateException("GL error after second world unload");
                    System.out.println("[O2/PROBE] "+(baseline?"baseline quit":"lifecycle")+" workload PASS regionsCreated="+created+" regionsDeleted="+deleted+" liveRegions="+live+" glError=0; shutdown REQUEST");
                    type.getMethod("m").invoke(game);
                }catch(Exception e){throw new IllegalStateException(e);}
            });
        }catch(Throwable e){System.err.println("[O2/PROBE] FAILED "+e);e.printStackTrace();if(mc!=null&&minecraft!=null){final Class<?> type=mc;final Object game=minecraft;try{schedule(type,game,()->{try{type.getMethod("m").invoke(game);}catch(Exception ignored){ }});}catch(Exception ignored){ }}}
    }
    private static void schedule(Class<?> type,Object game,Runnable work)throws Exception {
        java.util.concurrent.FutureTask<?> future=new java.util.concurrent.FutureTask<Void>(work,null);
        WORK.add(future);
        try {future.get(120,java.util.concurrent.TimeUnit.SECONDS);}
        finally {if(!future.isDone()){future.cancel(false);WORK.remove(future);}}
    }
    public static void onFrame() {
        frameStarted=true;
        for(int i=0;i<8;i++){java.util.concurrent.FutureTask<?> work=WORK.poll();if(work==null)return;work.run();}
    }
    private static void quitPackets(Class<?> type,Object game)throws Exception {
        drainMinecraftTasks(type,game);
        Object world=type.getField("f").get(game);
        if(world!=null)world.getClass().getMethod("H").invoke(world);
        // Apply already scheduled packets before loadWorld clears their handler's world.
        drainMinecraftTasks(type,game);
    }
    private static void drainMinecraftTasks(Class<?> type,Object game)throws Exception {
        Field selected=null;
        for(Field f:type.getDeclaredFields())if(java.util.Queue.class.isAssignableFrom(f.getType())){
            if(selected!=null)throw new IllegalStateException("Ambiguous disposable task queue");selected=f;
        }
        if(selected==null)throw new IllegalStateException("Disposable task queue unavailable");
        selected.setAccessible(true);
        int drained=drainTasks((java.util.Queue<?>)selected.get(game));
        System.out.println("[O2/PROBE] applied pending Minecraft tasks="+drained);
    }
    static int drainTasks(java.util.Queue<?> queue)throws Exception {
        for(int drained=0;drained<4096;drained++){
            Object work;
            synchronized(queue){work=queue.poll();}
            if(work==null)return drained;
            if(!(work instanceof java.util.concurrent.FutureTask<?>))throw new IllegalStateException("Unsupported disposable task type");
            java.util.concurrent.FutureTask<?> task=(java.util.concurrent.FutureTask<?>)work;
            task.run();task.get();
        }
        throw new IllegalStateException("Disposable task drain exceeded 4096 entries");
    }
    private static void reload(Class<?> type,Object game,Class<?> hooks,boolean useVbo,String phase)throws Exception {
        schedule(type,game,()->{
            try {
                int oldLive=((Integer)hooks.getMethod("liveRegions").invoke(null)).intValue();
                long oldDeleted=((Long)hooks.getMethod("regionsDeleted").invoke(null)).longValue();
                Object options=type.getField("t").get(game);options.getClass().getField("u").setBoolean(options,useVbo);
                Object render=type.getField("g").get(game);render.getClass().getMethod("a").invoke(render);
                int live=((Integer)hooks.getMethod("liveRegions").invoke(null)).intValue();
                long deleted=((Long)hooks.getMethod("regionsDeleted").invoke(null)).longValue()-oldDeleted;
                if(deleted<oldLive||(useVbo?live==0:live!=0))throw new IllegalStateException("Reload ownership/selection failed in "+phase);
                if(GL11.glGetError()!=0)throw new IllegalStateException("GL error in "+phase);
                System.out.println("[O2/PROBE] "+phase+" useVbo="+useVbo+" oldLive="+oldLive+" deleted="+deleted+" liveRegions="+live+" glError=0");
            }catch(Exception e){throw new IllegalStateException(e);}
        });
    }
    // Read our owned lifetime on the main thread; no production per-frame telemetry.
    private static void awaitActiveDraws(Class<?> type,Object game,Class<?> hooks,String phase)throws Exception {
        long[] draws={0};long timeout=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(90);
        while(draws[0]==0&&System.nanoTime()<timeout){
            schedule(type,game,()->{
                try {
                    Field f=hooks.getDeclaredField("LIFETIME");f.setAccessible(true);Object lifetime=f.get(null);
                    Field live=lifetime.getClass().getDeclaredField("live");live.setAccessible(true);
                    for(Object region:(java.util.Set<?>)live.get(lifetime))draws[0]+=((Long)region.getClass().getMethod("draws").invoke(region)).longValue();
                    if(draws[0]>0)System.out.println("[O2/PROBE] "+phase+" actualDraws="+draws[0]+" liveRegions="+hooks.getMethod("liveRegions").invoke(null));
                }catch(Exception e){throw new IllegalStateException(e);}
            });
            if(draws[0]==0)Thread.sleep(200);
        }
        if(draws[0]==0)throw new IllegalStateException("No actual O2 terrain draw in "+phase);
    }
}
