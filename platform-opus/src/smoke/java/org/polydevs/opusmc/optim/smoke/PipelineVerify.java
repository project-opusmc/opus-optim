package org.polydevs.opusmc.optim.smoke;

import com.google.gson.*;
import java.lang.reflect.Method;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.objectweb.asm.*;
import org.polydevs.opusmc.optim.api.*;
import org.polydevs.opusmc.optim.core.*;
import org.polydevs.opusmc.optim.core.render.GlyphIndex;
import org.polydevs.opusmc.optim.platform.*;
import org.polydevs.opusmc.optim.platform.mc189.*;

/** Real method bodies in owned kernel fixtures. No game, GL, installed session or FPS proof. */
public final class PipelineVerify {
    public interface WidthQuery { float width(char c); }
    public static final class Sprite { }
    public static final class VertexFormat { public int f() { return 32; } }
    public static final class QuietLog {
        public static QuietLog getLogger() { return new QuietLog(); }
        public void warn(String ignored) { }
    }
    public static final class Allocator {
        public static ByteBuffer allocate(int bytes) { return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder()); }
    }
    public static final class UploadTask {
        private final int id; private final long delay; private final List<Integer> completed;
        UploadTask(int id,long delay,List<Integer> completed) { this.id=id;this.delay=delay;this.completed=completed; }
        public void run() { long start=System.nanoTime();while(System.nanoTime()-start<delay){ }completed.add(id); }
    }
    private static volatile long blackhole;
    private PipelineVerify() { }
    public static void main(String[] args) throws Exception {
        require(System.getProperty("java.version").startsWith("1.8."),"Actual Java8 required");
        Path mc=Paths.get(args[1]),of=Paths.get(args[2]);
        JsonObject report=new JsonObject();
        report.addProperty("schemaVersion",1);report.addProperty("scope","REAL_METHOD_KERNELS_WITH_OWNED_SUBSTITUTES_NOT_GAME");
        report.addProperty("sessionId",UUID.randomUUID().toString());report.addProperty("java",System.getProperty("java.version"));
        report.addProperty("arch",System.getProperty("os.arch"));report.addProperty("minecraftSha256",hash(Files.readAllBytes(mc)));
        report.addProperty("optifineSha256",hash(Files.readAllBytes(of)));
        JsonObject targets=new JsonObject();Map<PipelineOptimizations.Kind,byte[][]> data=new EnumMap<>(PipelineOptimizations.Kind.class);
        for(PipelineOptimizations.Candidate c:PipelineOptimizations.candidates()) {
            byte[] original=FrustumVerify.extract(mc,of,c.kind().target());
            require(hash(original).equals(c.kind().targetSha256()),"Unexpected target pin");
            ProviderPlan plan=ProviderPlan.prepare(new OptimConfig(true,Collections.singletonMap(c.descriptor().id(),true)),
                    new CapabilitySet(c.descriptor().requiredCapabilities()),CrashMarker.Recovery.clean(),Collections.singleton(c.binding()),
                    Collections.emptyList(),PipelineVerify.class.getClassLoader());
            require(c.applicationState()==PipelinePatch.ApplicationState.NOT_SEEN,"Candidate reused");
            byte[] patched=plan.registrations().get(0).transformer().transform(c.kind().target(),original);
            require(c.applicationState()==PipelinePatch.ApplicationState.APPLIED,"Pin not applied");
            byte[] wrong={1,2,3};require(plan.registrations().get(0).transformer().transform(c.kind().target(),wrong)==wrong,"Unknown input changed");
            require(c.applicationState()==PipelinePatch.ApplicationState.UNSUPPORTED_INPUT,"Refusal invisible");
            verifyUntouchedMethods(original,patched,c.kind());
            JsonObject target=new JsonObject();target.addProperty("originalSha256",hash(original));target.addProperty("patchedSha256",hash(patched));
            target.addProperty("factoryStatesVerified",true);target.addProperty("unchangedMethodsVerified",true);
            targets.add(c.descriptor().id(),target);data.put(c.kind(),new byte[][]{original,patched});
        }
        report.add("targets",targets);
        JsonObject helpers=new JsonObject();for(Class<?> type:Arrays.asList(GlyphIndex.class,
                org.polydevs.opusmc.optim.core.render.BufferGrowth.class,org.polydevs.opusmc.optim.core.workload.DeadlineBudget.class)) {
            helpers.addProperty(type.getSimpleName(),hash(readResource(type))); }
        report.add("helperSha256",helpers);
        byte[][] font=data.get(PipelineOptimizations.Kind.GLYPH_LOOKUP);
        if(args[0].equals("trial")) { fontTrial(report,font,args[3],args[4]); }
        else if(args[0].equals("verify")) {
            PipelineOptimizations.Candidate alias=PipelineOptimizations.candidate(PipelineOptimizations.Kind.GLYPH_LOOKUP,
                    Collections.singleton("net.minecraft.client.gui.FontRenderer"));
            ProviderPlan aliasPlan=ProviderPlan.prepare(new OptimConfig(true,Collections.singletonMap(alias.descriptor().id(),true)),
                    new CapabilitySet(alias.descriptor().requiredCapabilities()),CrashMarker.Recovery.clean(),Collections.singleton(alias.binding()),
                    Collections.emptyList(),PipelineVerify.class.getClassLoader());
            byte[] aliasBytes=aliasPlan.registrations().get(0).transformer().transform("net.minecraft.client.gui.FontRenderer",font[0]);
            require(Arrays.equals(aliasBytes,font[1])&&alias.applicationState()==PipelinePatch.ApplicationState.APPLIED,"Host routing alias did not apply same pinned bytes");
            report.addProperty("fontMappedRoutingAliasVerified",true);
            report.addProperty("fontCases",fontParity(font));report.addProperty("fontMismatches",0);
            report.add("buffer",bufferVerification(data.get(PipelineOptimizations.Kind.BUFFER_GROWTH)));
            report.add("uploads",uploadVerification(data.get(PipelineOptimizations.Kind.UPLOAD_BUDGET)));
        } else { throw new IllegalArgumentException("Expected verify/trial"); }
        System.out.println(new Gson().toJson(report));
    }
    private static byte[] readResource(Class<?> type) throws Exception {
        try(java.io.InputStream in=type.getResourceAsStream(type.getSimpleName()+".class");java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){out.write(buffer,0,n);}return out.toByteArray();
        }
    }
    private static long fontParity(byte[][] source) throws Exception {
        WidthQuery base=font(source[0]),changed=font(source[1]);
        for(boolean unicode:new boolean[]{false,true}) {
            base.getClass().getField("k").setBoolean(base,unicode);changed.getClass().getField("k").setBoolean(changed,unicode);
            for(int c=0;c<65536;c++) {require(Float.floatToIntBits(base.width((char)c))==Float.floatToIntBits(changed.width((char)c)),"Width mismatch "+c);}
        }
        return 131072;
    }
    private static WidthQuery font(byte[] source) throws Exception {
        Class<?> type=kernel(source,"font");WidthQuery query=(WidthQuery)type.getConstructor().newInstance();
        float[] widths=new float[256];byte[] unicode=new byte[65536];
        for(int i=0;i<widths.length;i++){widths[i]=(i%19)/4f;}for(int i=0;i<unicode.length;i++){unicode[i]=(byte)(i*31);}
        type.getField("charWidthFloat").set(query,widths);type.getField("e").set(query,unicode);return query;
    }
    private static void fontTrial(JsonObject report,byte[][] font,String arm,String workload) throws Exception {
        require(arm.equals("control")||arm.equals("treatment"),"Unknown arm");
        require(workload.equals("ascii")||workload.equals("unicode"),"Unknown workload");
        WidthQuery query=font(font[arm.equals("control")?0:1]);char[] chars=new char[4096];Random random=new Random(20261014);
        for(int i=0;i<chars.length;i++){chars[i]=(char)(workload.equals("ascii")?33+random.nextInt(94):256+random.nextInt(65000));}
        report.addProperty("arm",arm);report.addProperty("workload",workload);
        for(int i=0;i<5;i++){blackhole=fontRun(query,chars,512);}
        JsonArray elapsed=new JsonArray(),sums=new JsonArray();
        for(int i=0;i<10;i++){long start=System.nanoTime(),sum=fontRun(query,chars,256);elapsed.add(System.nanoTime()-start);blackhole=sum;sums.add(sum);}
        report.addProperty("queriesPerSample",1048576);report.addProperty("warmupQueries",10485760);
        report.add("sampleNanos",elapsed);report.add("checksums",sums);report.addProperty("blackhole",blackhole);
    }
    private static long fontRun(WidthQuery query,char[] chars,int repeats) {
        long sum=0;for(int r=0;r<repeats;r++){for(char c:chars){sum+=Float.floatToIntBits(query.width(c));}}return sum;
    }
    private static JsonObject bufferVerification(byte[][] bytes) throws Exception {
        JsonObject report=new JsonObject();
        for(int arm=0;arm<2;arm++) {
            Class<?> t=kernel(bytes[arm],"buffer");Object o=t.getConstructor().newInstance();
            ByteBuffer initial=Allocator.allocate(2*1024*1024);for(int i=0;i<1024;i++){initial.put(i,(byte)i);}
            t.getField("a").set(o,initial);t.getField("b").set(o,initial.asIntBuffer());t.getField("c").set(o,initial.asShortBuffer());t.getField("d").set(o,initial.asFloatBuffer());
            t.getField("m").set(o,new VertexFormat());Sprite first=new Sprite();Sprite[] sprites=new Sprite[16384];sprites[0]=first;
            t.getField("quadSprites").set(o,sprites);t.getField("quadSpritesPrev").set(o,sprites);
            Method grow=t.getMethod("b",int.class);long copied=0;int allocations=0,used=0;
            for(int step=0;step<20;step++) {
                ByteBuffer before=(ByteBuffer)t.getField("a").get(o);IntBuffer ints=(IntBuffer)t.getField("b").get(o);ints.position(used/4);
                grow.invoke(o,262144);ByteBuffer after=(ByteBuffer)t.getField("a").get(o);
                if(after!=before){allocations++;copied+=before.capacity();require(after.order()==ByteOrder.nativeOrder(),"Byte order lost");
                    require(((IntBuffer)t.getField("b").get(o)).position()==used/4,"Int position lost");
                    require(((ShortBuffer)t.getField("c").get(o)).position()==used/2,"Short position lost");
                    require(((FloatBuffer)t.getField("d").get(o)).position()==0,"Float position changed");
                    require(t.getField("quadSpritesPrev").get(o)==null,"Previous sprite array retained");}
                for(int i=0;i<1024;i++){require(after.get(i)==(byte)i,"Copied bytes changed");}
                require(((Sprite[])t.getField("quadSprites").get(o))[0]==first,"Sprite prefix lost");used+=1024*1024;
            }
            JsonObject row=new JsonObject();row.addProperty("directReallocations",allocations);row.addProperty("bytesCopied",copied);
            row.addProperty("peakCapacity",((ByteBuffer)t.getField("a").get(o)).capacity());row.addProperty("usedBytes",used);
            row.addProperty("bytesViewsSpritesVerified",true);report.add(arm==0?"control":"treatment",row);
        }
        return report;
    }
    private static JsonObject uploadVerification(byte[][] bytes) throws Exception {
        JsonObject report=new JsonObject();
        for(int arm=0;arm<2;arm++) {
            Class<?> t=kernel(bytes[arm],"upload");Object o=t.getConstructor().newInstance();Queue<UploadTask> tasks=new ArrayDeque<>();List<Integer> completed=new ArrayList<>();
            t.getField("h").set(o,tasks);Method drain=t.getMethod("a",long.class);
            tasks.add(new UploadTask(0,0,completed));tasks.add(new UploadTask(1,0,completed));drain.invoke(o,0L);require(completed.equals(Collections.singletonList(0)),"Zero did not drain one");
            drain.invoke(o,System.nanoTime()-1);require(completed.equals(Arrays.asList(0,1)),"Expired deadline did not preserve one-task progress");
            require(Boolean.FALSE.equals(drain.invoke(o,0L)),"Empty queue reported work");completed.clear();
            for(int i=0;i<64;i++){tasks.add(new UploadTask(i,250000,completed));}
            long start=System.nanoTime();drain.invoke(o,start+100_000_000L);long elapsed=System.nanoTime()-start;int firstFrame=completed.size(),frames=1;
            require(firstFrame>0,"No progress");if(arm==1){require(firstFrame<64,"Upload cap did not defer work");}
            while(!tasks.isEmpty()){drain.invoke(o,System.nanoTime()+100_000_000L);frames++;require(frames<=64,"Upload starvation");}
            for(int i=0;i<64;i++){require(completed.get(i)==i,"Upload order changed");}
            completed.clear();tasks.add(new UploadTask(0,3_000_000L,completed));tasks.add(new UploadTask(1,0,completed));
            drain.invoke(o,System.nanoTime()+100_000_000L);if(arm==1){require(completed.size()==1&&tasks.size()==1,"Slow upload did not leave next task queued");}
            JsonObject row=new JsonObject();row.addProperty("firstDrainTasks",firstFrame);row.addProperty("firstDrainNanos",elapsed);
            row.addProperty("drainsToFinish",frames);row.addProperty("fifoAndProgressVerified",true);row.addProperty("zeroExpiredEmptyVerified",true);
            row.addProperty("slowTaskCanExceedCap",true);report.add(arm==0?"control":"treatment",row);
        }
        return report;
    }
    private static Class<?> kernel(byte[] source,String mode) {
        ClassReader reader=new ClassReader(source);String owner=reader.getClassName();ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);
        w.visit(Opcodes.V1_8,Opcodes.ACC_PUBLIC,owner,null,"java/lang/Object",mode.equals("font")?new String[]{Type.getInternalName(WidthQuery.class)}:null);
        String[] alphabet={null};boolean[] table={false};
        reader.accept(new ClassVisitor(Opcodes.ASM5) {
            @Override public FieldVisitor visitField(int a,String n,String d,String s,Object v) {
                if(n.equals("o2$glyphIndex")){table[0]=true;w.visitField(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC|Opcodes.ACC_FINAL,n,d,null,null).visitEnd();}
                if(mode.equals("font")&&Arrays.asList("k","e","charWidthFloat").contains(n)
                        ||mode.equals("buffer")&&Arrays.asList("a","b","c","d","m","quadSprites","quadSpritesPrev").contains(n)
                        ||mode.equals("upload")&&n.equals("h")){w.visitField(Opcodes.ACC_PUBLIC,n,redirectDesc(d),null,null).visitEnd();}
                return null;
            }
            @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex) {
                boolean keep=mode.equals("font")&&n.equals("getCharWidthFloat")&&d.equals("(C)F")
                        ||mode.equals("buffer")&&(n.equals("b")&&d.equals("(I)V")||n.equals("getBufferQuadSize"))
                        ||mode.equals("upload")&&n.equals("a")&&d.equals("(J)Z");
                MethodVisitor out=keep?w.visitMethod(Opcodes.ACC_PUBLIC,mode.equals("font")?"width":n,d,null,ex):null;
                return new MethodVisitor(Opcodes.ASM5,out) {
                    @Override public void visitLdcInsn(Object value){if(value instanceof String&&((String)value).length()==256){alphabet[0]=(String)value;}super.visitLdcInsn(value);}
                    @Override public void visitFieldInsn(int op,String o,String name,String desc){super.visitFieldInsn(op,o,name,redirectDesc(desc));}
                    @Override public void visitTypeInsn(int op,String type){super.visitTypeInsn(op,redirect(type));}
                    @Override public void visitFrame(int type,int nLocal,Object[] local,int nStack,Object[] stack) {
                        super.visitFrame(type,nLocal,redirectFrame(local,nLocal),nStack,redirectFrame(stack,nStack));
                    }
                    @Override public void visitMethodInsn(int op,String o,String name,String desc,boolean iface) {
                        if(o.equals("avd")&&name.equals("c")){super.visitMethodInsn(Opcodes.INVOKESTATIC,Type.getInternalName(Allocator.class),"allocate",desc,false);}
                        else if(o.equals("org/apache/logging/log4j/LogManager")){super.visitMethodInsn(Opcodes.INVOKESTATIC,Type.getInternalName(QuietLog.class),name,"()L"+Type.getInternalName(QuietLog.class)+";",false);}
                        else if(o.equals("org/apache/logging/log4j/Logger")){super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,Type.getInternalName(QuietLog.class),name,desc,false);}
                        else{super.visitMethodInsn(op,redirect(o),name,desc,iface);}
                    }
                };
            }
        },0);
        MethodVisitor ctor=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);ctor.visitCode();ctor.visitVarInsn(Opcodes.ALOAD,0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);ctor.visitInsn(Opcodes.RETURN);ctor.visitMaxs(0,0);ctor.visitEnd();
        if(table[0]) {require(alphabet[0]!=null,"Missing table alphabet");MethodVisitor init=w.visitMethod(Opcodes.ACC_STATIC,"<clinit>","()V",null,null);
            init.visitCode();init.visitLdcInsn(alphabet[0]);init.visitMethodInsn(Opcodes.INVOKESTATIC,Type.getInternalName(GlyphIndex.class),"create","(Ljava/lang/String;)[S",false);
            init.visitFieldInsn(Opcodes.PUTSTATIC,owner,"o2$glyphIndex","[S");init.visitInsn(Opcodes.RETURN);init.visitMaxs(0,0);init.visitEnd();}
        w.visitEnd();byte[] bytes=w.toByteArray();class L extends ClassLoader {L(){super(PipelineVerify.class.getClassLoader());}Class<?> define(){return defineClass(owner,bytes,0,bytes.length);}}
        return new L().define();
    }
    private static String redirect(String name) {
        if(name.equals("bmi")){return Type.getInternalName(Sprite.class);}if(name.equals("bmu")){return Type.getInternalName(VertexFormat.class);}
        if(name.equals("com/google/common/util/concurrent/ListenableFutureTask")){return Type.getInternalName(UploadTask.class);}return name;
    }
    private static String redirectDesc(String descriptor) {
        return descriptor.replace("Lbmi;","L"+Type.getInternalName(Sprite.class)+";").replace("Lbmu;","L"+Type.getInternalName(VertexFormat.class)+";");
    }
    private static Object[] redirectFrame(Object[] frame,int count) {
        if(frame==null){return null;}Object[] copy=Arrays.copyOf(frame,count);
        for(int i=0;i<count;i++){if(copy[i] instanceof String){String type=(String)copy[i];copy[i]=type.startsWith("[")?redirectDesc(type):redirect(type);}}
        return copy;
    }
    private static void verifyUntouchedMethods(byte[] before,byte[] after,PipelineOptimizations.Kind kind) throws Exception {
        Map<String,String> a=methods(before),b=methods(after);Set<String> allowed=new HashSet<>();
        if(kind==PipelineOptimizations.Kind.GLYPH_LOOKUP){allowed.addAll(Arrays.asList("a(CZ)F","a(Ljava/lang/String;Z)V","getCharWidthFloat(C)F","<clinit>()V"));}
        else{allowed.add(kind==PipelineOptimizations.Kind.BUFFER_GROWTH?"b(I)V":"a(J)Z");}
        for(String method:a.keySet()){if(!allowed.contains(method)){require(a.get(method).equals(b.get(method)),"Unrelated method changed: "+method);}}
    }
    static Map<String,String> methods(byte[] bytes) throws Exception {
        Map<String,String> result=new TreeMap<>();ClassReader reader=new ClassReader(bytes);
        reader.accept(new ClassVisitor(Opcodes.ASM5) {
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                ClassWriter single=new ClassWriter(0);single.visit(Opcodes.V1_8,Opcodes.ACC_PUBLIC,reader.getClassName(),null,"java/lang/Object",null);
                MethodVisitor m=single.visitMethod(access,name,desc,sig,ex);
                return new MethodVisitor(Opcodes.ASM5,m){@Override public void visitEnd(){super.visitEnd();single.visitEnd();try{result.put(name+desc,hash(single.toByteArray()));}catch(Exception failure){throw new IllegalStateException(failure);}}};
            }
        },0);return result;
    }
    private static String hash(byte[] bytes) throws Exception {StringBuilder out=new StringBuilder();for(byte v:MessageDigest.getInstance("SHA-256").digest(bytes)){out.append(String.format(Locale.ROOT,"%02x",v));}return out.toString();}
    private static void require(boolean ok,String message){if(!ok){throw new AssertionError(message);}}
}
