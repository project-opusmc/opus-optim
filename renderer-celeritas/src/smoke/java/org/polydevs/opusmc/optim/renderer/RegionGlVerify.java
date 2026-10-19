package org.polydevs.opusmc.optim.renderer;
import java.nio.*;import java.util.*;import org.lwjgl.opengl.*;import org.embeddedt.embeddium.impl.common.util.NativeBuffer;
/** Real Java8/LWJGL2/Celeritas GL verification; no Minecraft FPS claim. */
public final class RegionGlVerify {
    private RegionGlVerify() { }
    public static void main(String[] args)throws Exception {
        require(System.getProperty("java.version").startsWith("1.8."),"Java8 required");
        Display.setDisplayMode(new DisplayMode(256,128));Display.setTitle("O2 renderer verification");Display.create();
        try {
            System.out.println("GL="+GL11.glGetString(GL11.GL_VERSION)+" renderer="+GL11.glGetString(GL11.GL_RENDERER));
            GL11.glViewport(0,0,256,128);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();
            GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            ByteBuffer a=quad(-0.9f,-0.8f,0.5f,0.8f,255,0),b=quad(-0.5f,-0.5f,0.9f,0.5f,0,255);
            ByteBuffer joined=bytes(224);joined.put(a.duplicate()).put(b.duplicate()).flip();
            int vanilla=GL15.glGenBuffers();GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vanilla);GL15.glBufferData(GL15.GL_ARRAY_BUFFER,joined,GL15.GL_STATIC_DRAW);pointers();clear();GL11.glDrawArrays(GL11.GL_QUADS,0,4);GL11.glDrawArrays(GL11.GL_QUADS,4,4);byte[] baseline=pixels();
            long before=NativeBuffer.getTotalAllocated();ObservedStorage storage=new ObservedStorage(28);RegionRenderer renderer=new RegionRenderer(28,storage);Object ra=new Object(),rb=new Object(),filler=new Object();
            renderer.upload(ra,a);renderer.upload(rb,b);
            // Force real arena relocation/growth, then free the filler before draw.
            ByteBuffer big=bytes(28*32768);renderer.upload(filler,big);renderer.upload(filler,bytes(0));
            renderer.queue(GL11.GL_QUADS,ra);renderer.queue(GL11.GL_QUADS,rb);clear();renderer.finish(RegionGlVerify::pointers);byte[] treatment=pixels();
            require(Arrays.equals(baseline,treatment),"Pixel mismatch after real arena growth");require(colored(treatment)>10000,"Fixture did not render geometry");
            require(storage.commands==2,"Reverse memory order must keep two draw commands");
            // Celeritas allocates from the free segment's end: rb then ra is
            // increasing physical order, including after real arena relocation.
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vanilla);pointers();clear();
            GL11.glDrawArrays(GL11.GL_QUADS,4,4);GL11.glDrawArrays(GL11.GL_QUADS,0,4);
            byte[] reverseBaseline=pixels();
            renderer.queue(GL11.GL_QUADS,rb);renderer.queue(GL11.GL_QUADS,ra);
            clear();renderer.finish(RegionGlVerify::pointers);
            require(storage.commands==1,"Adjacent complete quads were not coalesced");
            require(Arrays.equals(reverseBaseline,pixels()),"Coalesced blended quads changed pixels/order");
            renderer.upload(rb,a);renderer.queue(GL11.GL_QUADS,rb);renderer.finish(RegionGlVerify::pointers);require(renderer.uploads()==5&&renderer.draws()==3,"Counters wrong");
            int gpu=storage.bufferHandle();renderer.close();require(!GL15.glIsBuffer(gpu),"Region GPU buffer leaked");require(NativeBuffer.getTotalAllocated()==before,"Native upload buffer leaked");
            GL15.glDeleteBuffers(vanilla);require(GL11.glGetError()==GL11.GL_NO_ERROR,"GL error");
            System.out.println("PASS actual-Celeritas/LWJGL2: blended ordered/coalesced pixel parity (2 commands to 1), arena growth, replacement, empty/free, GPU/native lifecycle; coloredPixels="+colored(treatment));
        } finally {Display.destroy();}
    }
    private static final class ObservedStorage implements RegionRenderer.Storage {
        private final CeleritasStorage storage;
        int commands;
        ObservedStorage(int stride){storage=new CeleritasStorage(stride);}
        public RegionRenderer.Segment upload(ByteBuffer data){return storage.upload(data);}
        public void draw(int mode,IntBuffer first,IntBuffer count,Runnable pointers){commands=first.remaining();storage.draw(mode,first,count,pointers);}
        public void close(){storage.close();}
        int bufferHandle(){return storage.bufferHandle();}
    }
    private static ByteBuffer quad(float x0,float y0,float x1,float y1,int red,int green){ByteBuffer b=bytes(112);for(float[] p:new float[][]{{x0,y0},{x1,y0},{x1,y1},{x0,y1}}){b.putFloat(p[0]).putFloat(p[1]).putFloat(0).put((byte)red).put((byte)green).put((byte)0).put((byte)128).putFloat(0).putFloat(0).putInt(0);}b.flip();return b;}
    private static void pointers(){GL11.glVertexPointer(3,GL11.GL_FLOAT,28,0L);GL11.glColorPointer(4,GL11.GL_UNSIGNED_BYTE,28,12L);}
    private static void clear(){GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);}
    private static byte[] pixels(){ByteBuffer b=bytes(256*128*4);GL11.glReadPixels(0,0,256,128,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,b);byte[] a=new byte[b.capacity()];b.get(a);return a;}
    private static int colored(byte[] a){int n=0;for(int i=0;i<a.length;i+=4)if(a[i]!=0||a[i+1]!=0)n++;return n;}
    private static ByteBuffer bytes(int n){return ByteBuffer.allocateDirect(n).order(ByteOrder.nativeOrder());}
    private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
}
