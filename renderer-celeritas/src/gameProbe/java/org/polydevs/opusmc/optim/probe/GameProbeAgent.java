package org.polydevs.opusmc.optim.probe;
import java.awt.image.BufferedImage;import java.io.*;import java.lang.instrument.Instrumentation;import java.lang.reflect.*;import java.nio.*;import java.nio.file.*;import javax.imageio.ImageIO;import org.lwjgl.opengl.GL11;
/** Test-only agent: bounded offline workload in an explicitly supplied disposable directory. */
public final class GameProbeAgent {
    private GameProbeAgent() { }
    public static void premain(String expectedDirectory, Instrumentation ignored) {
        if(expectedDirectory==null || !expectedDirectory.contains("renderer-client"))throw new IllegalArgumentException("Explicit disposable renderer-client directory required");
        Thread t=new Thread(()->run(expectedDirectory),"O2 disposable gameplay probe");t.setDaemon(true);t.start();
    }
    private static void run(String expected) {
        Object minecraft=null;Class<?> mc=null;
        try {
            Class<?> launch=Class.forName("net.minecraft.launchwrapper.Launch");
            ClassLoader loader=null;long timeout=System.currentTimeMillis()+120000;
            while(loader==null && System.currentTimeMillis()<timeout){loader=(ClassLoader)launch.getField("classLoader").get(null);Thread.sleep(100);}
            if(loader==null)throw new IllegalStateException("LaunchWrapper unavailable");
            Class<?> hooks=Class.forName("org.polydevs.opusmc.optim.renderer.RendererHooks");
            while(!((Boolean)hooks.getMethod("ready").invoke(null)).booleanValue() && System.currentTimeMillis()<timeout)Thread.sleep(100);
            if(!((Boolean)hooks.getMethod("ready").invoke(null)).booleanValue())throw new IllegalStateException("O2 renderer was not ready");
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
            Field submitted=hooks.getDeclaredField("submitted");submitted.setAccessible(true);
            timeout=System.currentTimeMillis()+90000;
            while(!submitted.getBoolean(null) && System.currentTimeMillis()<timeout)Thread.sleep(200);
            if(!submitted.getBoolean(null))throw new IllegalStateException("No actual O2 terrain draw observed");
            Thread.sleep(5000);
            schedule(type,game,()->{
                try {
                    int width=type.getField("d").getInt(game),height=type.getField("e").getInt(game);
                    ByteBuffer pixels=ByteBuffer.allocateDirect(Math.multiplyExact(Math.multiplyExact(width,height),4));
                    GL11.glReadPixels(0,0,width,height,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
                    BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
                    for(int y=0;y<height;y++)for(int x=0;x<width;x++){int i=(y*width+x)*4;image.setRGB(x,height-1-y,((pixels.get(i+3)&255)<<24)|((pixels.get(i)&255)<<16)|((pixels.get(i+1)&255)<<8)|(pixels.get(i+2)&255));}
                    Path output=Paths.get(expected).resolve("o2-terrain.png");ImageIO.write(image,"png",output.toFile());
                    int error=GL11.glGetError();if(error!=0)throw new IllegalStateException("GL error="+error);
                    System.out.println("[O2/PROBE] actual terrain frame="+width+"x"+height+" regionsCreated="+hooks.getMethod("regionsCreated").invoke(null)+" glError="+error);
                }catch(Exception e){throw new IllegalStateException(e);}
            });
            schedule(type,game,()-> { try {Object world=type.getField("f").get(game);world.getClass().getMethod("H").invoke(world);}catch(Exception e){throw new IllegalStateException(e);} });
            // Stop packet production while the valid world drains already scheduled work.
            Thread.sleep(1000);
            schedule(type,game,()->{
                try {
                    type.getMethod("a",Class.forName("bdb",false,gameLoader)).invoke(game,new Object[]{null});
                    // Match normal quit-to-menu semantics before the next game tick.
                    type.getMethod("a",Class.forName("axu",false,gameLoader)).invoke(game,Class.forName("aya",true,gameLoader).getConstructor().newInstance());
                    int live=((Integer)hooks.getMethod("liveRegions").invoke(null)).intValue();
                    System.out.println("[O2/PROBE] unload regionsCreated="+hooks.getMethod("regionsCreated").invoke(null)+" regionsDeleted="+hooks.getMethod("regionsDeleted").invoke(null)+" liveRegions="+live);
                    if(live!=0)throw new IllegalStateException("Region leak after world unload");
                    type.getMethod("m").invoke(game);
                }catch(Exception e){throw new IllegalStateException(e);}
            });
        }catch(Throwable e){System.err.println("[O2/PROBE] FAILED "+e);e.printStackTrace();if(mc!=null&&minecraft!=null){final Class<?> type=mc;final Object game=minecraft;try{schedule(type,game,()->{try{type.getMethod("m").invoke(game);}catch(Exception ignored){ }});}catch(Exception ignored){ }}}
    }
    private static void schedule(Class<?> type,Object game,Runnable work)throws Exception {
        Object future=type.getMethod("a",Runnable.class).invoke(game,work);
        ((java.util.concurrent.Future<?>)future).get(120,java.util.concurrent.TimeUnit.SECONDS);
    }
}
