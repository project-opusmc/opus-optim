package org.polydevs.opusmc.optim.renderer;
import java.nio.*;import java.util.*;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
final class RegionRendererTest {
    static final class Segment implements RegionRenderer.Segment {int offset,size;boolean freed;Segment(int n,int s){offset=n;size=s;}public int offset(){return offset;}public int length(){return size;}public void free(){assertFalse(freed);freed=true;}}
    static final class Storage implements RegionRenderer.Storage {
        final List<Segment> live=new ArrayList<>();final List<Integer> draws=new ArrayList<>();boolean closed;int uploads;
        public RegionRenderer.Segment upload(ByteBuffer data){uploads++;Segment s=new Segment(uploads*10,data.remaining()/28);live.add(s);return s;}
        public void draw(int mode,IntBuffer first,IntBuffer count,Runnable pointers){pointers.run();while(first.hasRemaining()){draws.add(first.get());draws.add(count.get());}}
        public void close(){closed=true;}
    }
    @Test void queuedOffsetsResolveAfterRelocationAndPreserveOrder(){Storage s=new Storage();RegionRenderer r=new RegionRenderer(28,s);Object a=new Object(),b=new Object();r.upload(a,ByteBuffer.allocate(112));r.upload(b,ByteBuffer.allocate(224));r.queue(7,b);r.queue(7,a);s.live.get(0).offset=96;s.live.get(1).offset=83;int[] pointers={0};r.finish(()->pointers[0]++);assertEquals(Arrays.asList(83,8,96,4),s.draws);assertEquals(1,pointers[0]);r.finish(()->fail("empty batch"));}
    @Test void replacementEmptyAndCloseFreeAllSegments(){Storage s=new Storage();RegionRenderer r=new RegionRenderer(28,s);Object a=new Object();r.upload(a,ByteBuffer.allocate(112));r.upload(a,ByteBuffer.allocate(224));assertTrue(s.live.get(0).freed);r.upload(a,ByteBuffer.allocate(0));assertTrue(s.live.get(1).freed);r.upload(a,ByteBuffer.allocate(112));r.close();r.close();assertTrue(s.closed);assertTrue(s.live.get(2).freed);assertThrows(IllegalStateException.class,()->r.upload(a,ByteBuffer.allocate(112)));}
    @Test void partialInputAndInvalidStrideNeverReachStorage(){Storage s=new Storage();RegionRenderer r=new RegionRenderer(28,s);ByteBuffer b=ByteBuffer.allocate(140);b.position(28);b.limit(112);r.upload(new Object(),b);assertEquals(28,b.position());assertEquals(112,b.limit());assertEquals(3,s.live.get(0).size);assertThrows(IllegalArgumentException.class,()->r.upload(new Object(),ByteBuffer.allocate(29)));assertEquals(1,s.uploads);}
    @Test void mixedModesAndUploadDuringBatchFailBeforeMutation(){Storage s=new Storage();RegionRenderer r=new RegionRenderer(28,s);Object a=new Object();r.upload(a,ByteBuffer.allocate(112));r.queue(7,a);assertThrows(IllegalArgumentException.class,()->r.queue(4,a));assertThrows(IllegalStateException.class,()->r.upload(a,ByteBuffer.allocate(224)));assertEquals(1,s.uploads);}
    @Test void callsFromOtherThreadAreRejected()throws Exception{RegionRenderer r=new RegionRenderer(28,new Storage());Throwable[] seen={null};Thread t=new Thread(()->{try{r.close();}catch(Throwable e){seen[0]=e;}});t.start();t.join();assertInstanceOf(IllegalStateException.class,seen[0]);r.close();}
}
