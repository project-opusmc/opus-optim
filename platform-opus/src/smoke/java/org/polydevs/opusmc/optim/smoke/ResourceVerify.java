package org.polydevs.opusmc.optim.smoke;

import com.google.gson.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.polydevs.opusmc.optim.api.*;
import org.polydevs.opusmc.optim.core.*;
import org.polydevs.opusmc.optim.platform.*;
import org.polydevs.opusmc.optim.platform.mc189.*;

/** Full raw ResourceLocation classes with the actual library dependency; never a game/FPS run. */
public final class ResourceVerify {
    public interface Factory { Object create(String domain,String path); }
    private static volatile long blackhole;
    private ResourceVerify() { }
    public static void main(String[] args) throws Exception {
        require(System.getProperty("java.version").startsWith("1.8."),"Actual Java8 required");
        Path mc=Paths.get(args[1]),of=Paths.get(args[2]),commons=Paths.get(args[3]);
        byte[] original=FrustumVerify.extract(mc,of,"jy");ResourceOptimization.Candidate c=ResourceOptimization.candidate();
        ProviderPlan plan=ProviderPlan.prepare(new OptimConfig(true,Collections.singletonMap(c.descriptor().id(),true)),
                new CapabilitySet(c.descriptor().requiredCapabilities()),CrashMarker.Recovery.clean(),Collections.singleton(c.binding()),
                Collections.emptyList(),ResourceVerify.class.getClassLoader());
        require(c.applicationState()==ResourcePatch.ApplicationState.NOT_SEEN,"Already used");
        byte[] patched=plan.registrations().get(0).transformer().transform("jy",original);
        require(c.applicationState()==ResourcePatch.ApplicationState.APPLIED,"Pin did not apply");
        require(plan.registrations().get(0).transformer().transform("jy",patched)==patched,"Modified input accepted");
        require(c.applicationState()==ResourcePatch.ApplicationState.UNSUPPORTED_INPUT,"Refusal hidden");
        Map<String,String> before=PipelineVerify.methods(original),after=PipelineVerify.methods(patched);
        for(String method:before.keySet()){if(!method.equals("toString()Ljava/lang/String;")){require(before.get(method).equals(after.get(method)),"Unrelated method changed");}}
        JsonObject report=new JsonObject();report.addProperty("schemaVersion",1);report.addProperty("scope","FULL_RAW_RESOURCE_CLASS_NOT_GAME");
        report.addProperty("sessionId",UUID.randomUUID().toString());report.addProperty("java",System.getProperty("java.version"));
        report.addProperty("arch",System.getProperty("os.arch"));report.addProperty("minecraftSha256",hash(Files.readAllBytes(mc)));
        report.addProperty("optifineSha256",hash(Files.readAllBytes(of)));report.addProperty("commonsLangSha256",hash(Files.readAllBytes(commons)));
        report.addProperty("originalSha256",hash(original));report.addProperty("patchedSha256",hash(patched));
        report.addProperty("factoryStatesVerified",true);report.addProperty("unchangedMethodsVerified",true);
        try(Loader base=new Loader(original,commons);Loader changed=new Loader(patched,commons)) {
            Factory a=base.factory(),b=changed.factory();
            if(args[0].equals("verify")){verify(report,base,changed,a,b);}
            else if(args[0].equals("trial")){trial(report,args[4],args[5],args[4].equals("control")?a:b);}
            else{throw new IllegalArgumentException("Expected verify/trial");}
        }
        System.out.println(new Gson().toJson(report));
    }
    private static void verify(JsonObject report,Loader base,Loader changed,Factory a,Factory b) throws Exception {
        Random random=new Random(20261014);int cases=0;
        String[] domains={null,"","minecraft","OPUS","x","ë","资源"};String[] paths={"","font/default.png","a:b","Ω/纹理",null};
        for(String domain:domains){for(String path:paths){compare(a,b,domain,path);cases++;}}
        for(int i=0;i<20000;i++){compare(a,b,"opus"+random.nextInt(17),"assets/"+random.nextInt()+"/texture.png");cases++;}
        Constructor<?> ctorA=base.raw.getConstructor(String.class),ctorB=changed.raw.getConstructor(String.class);
        for(String input:new String[]{null,"","name","x:path","Opus:Textures/Case",":path","namespace:","a:b:c"}) {
            Object x=construct(ctorA,input),y=construct(ctorB,input);compareResult(x,y);cases++;
        }
        Object x=a.create("OPUS","item"),y=b.create("OPUS","item");require(x.toString().equals("opus:item")&&y.toString().equals("opus:item"),"Normalization changed");
        require(x.toString()!=x.toString(),"Unexpected fresh baseline identity");require(y.toString()==y.toString(),"Warm cache not reused");
        require(x.equals(a.create("opus","item"))&&y.equals(b.create("opus","item")),"Equality changed");
        require(x.hashCode()==y.hashCode(),"Hash changed");
        Class<?> subA=base.subclass(),subB=changed.subclass();Object sa=subA.getConstructor(String.class,String.class).newInstance("OPUS","item");
        Object sb=subB.getConstructor(String.class,String.class).newInstance("OPUS","item");require(sa.toString().equals("opus:item#variant")&&sb.toString().equals(sa.toString()),"Subclass dispatch changed");
        Object shared=b.create("OPUS","shared");ExecutorService pool=Executors.newFixedThreadPool(8);List<Future<?>> jobs=new ArrayList<>();
        try{for(int worker=0;worker<8;worker++){jobs.add(pool.submit(()->{for(int i=0;i<10000;i++){require(shared.toString().equals("opus:shared"),"Concurrent content changed");}}));}
            for(Future<?> job:jobs){job.get();}
        }finally{pool.shutdownNow();}
        report.addProperty("contentCases",cases);report.addProperty("mismatches",0);report.addProperty("concurrentReads",80000);
        report.addProperty("constructorsEqualsHashSubclassVerified",true);report.addProperty("warmIdentityAndInstanceIsolationVerified",true);
        report.addProperty("allocationContract","warm repeats retain one String; cold races may allocate duplicates; one reference field and retained String per queried instance");
    }
    private static Object construct(Constructor<?> ctor,String input) throws Exception {
        try{return ctor.newInstance(input);}catch(InvocationTargetException failure){return failure.getCause();}
    }
    private static void compare(Factory a,Factory b,String domain,String path) {
        Object x,y;try{x=a.create(domain,path);}catch(RuntimeException failure){x=failure;}try{y=b.create(domain,path);}catch(RuntimeException failure){y=failure;}compareResult(x,y);
    }
    private static void compareResult(Object x,Object y) {
        if(x instanceof Throwable||y instanceof Throwable){require(x.getClass().equals(y.getClass()),"Constructor error changed");}
        else{require(x.toString().equals(y.toString()),"Content changed");require(x.hashCode()==y.hashCode(),"Hash content changed");}
    }
    private static void trial(JsonObject report,String arm,String workload,Factory factory) throws Exception {
        require(arm.equals("control")||arm.equals("treatment"),"Unknown arm");require(workload.equals("warm")||workload.equals("cold"),"Unknown workload");
        String[] domains=new String[4096],paths=new String[4096];Object[] resources=new Object[4096];
        for(int i=0;i<4096;i++){domains[i]="opus"+(i%17);paths[i]="textures/"+i+"/resource.png";resources[i]=factory.create(domains[i],paths[i]);resources[i].toString();}
        report.addProperty("arm",arm);report.addProperty("workload",workload);report.addProperty("fixture","4096 opus(0..16)/textures(i)/resource.png; seed recipe v1");
        boolean cold=workload.equals("cold");for(int i=0;i<5;i++){blackhole=run(factory,resources,domains,paths,cold,128);}
        Object bean=java.lang.management.ManagementFactory.getThreadMXBean();Class<?> api=Class.forName("com.sun.management.ThreadMXBean");
        require(api.isInstance(bean)&&(Boolean)api.getMethod("isThreadAllocatedMemorySupported").invoke(bean),"Allocation counter unavailable");
        api.getMethod("setThreadAllocatedMemoryEnabled",boolean.class).invoke(bean,true);Method allocated=api.getMethod("getThreadAllocatedBytes",long.class);long thread=Thread.currentThread().getId();
        JsonArray elapsed=new JsonArray(),sums=new JsonArray(),allocs=new JsonArray();
        for(int i=0;i<8;i++){long allocBefore=(Long)allocated.invoke(bean,thread);long start=System.nanoTime();long sum=run(factory,resources,domains,paths,cold,64);
            long nanos=System.nanoTime()-start,bytes=(Long)allocated.invoke(bean,thread)-allocBefore;require(bytes>=0&&nanos>0,"Invalid samples");
            blackhole=sum;elapsed.add(nanos);sums.add(sum);allocs.add(bytes);}
        report.addProperty("queriesPerSample",262144);report.addProperty("warmupQueries",2621440);report.add("sampleNanos",elapsed);report.add("checksums",sums);report.add("sampleAllocatedBytes",allocs);
        report.addProperty("allocationCounterOverheadIncluded",true);report.addProperty("blackhole",blackhole);
    }
    private static long run(Factory factory,Object[] resources,String[] domains,String[] paths,boolean cold,int repeats) {
        long sum=0;for(int r=0;r<repeats;r++){for(int i=0;i<resources.length;i++){String text=(cold?factory.create(domains[i],paths[i]):resources[i]).toString();sum+=text.length()*31+text.charAt(0)+text.charAt(text.length()-1);}}return sum;
    }
    private static final class Loader extends URLClassLoader {
        final Class<?> raw;
        Loader(byte[] bytes,Path commons){super(new URL[]{url(commons)},ResourceVerify.class.getClassLoader());raw=defineClass("jy",bytes,0,bytes.length);}
        Factory factory() throws Exception {
            ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(Opcodes.V1_8,Opcodes.ACC_PUBLIC,"ResourceFactory",null,"java/lang/Object",new String[]{Type.getInternalName(Factory.class)});
            constructor(w,"java/lang/Object","()V");MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"create","(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;",null,null);
            m.visitCode();m.visitTypeInsn(Opcodes.NEW,"jy");m.visitInsn(Opcodes.DUP);m.visitVarInsn(Opcodes.ALOAD,1);m.visitVarInsn(Opcodes.ALOAD,2);
            m.visitMethodInsn(Opcodes.INVOKESPECIAL,"jy","<init>","(Ljava/lang/String;Ljava/lang/String;)V",false);m.visitInsn(Opcodes.ARETURN);m.visitMaxs(0,0);m.visitEnd();w.visitEnd();byte[] bytes=w.toByteArray();
            return(Factory)defineClass("ResourceFactory",bytes,0,bytes.length).getConstructor().newInstance();
        }
        Class<?> subclass() {
            ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(Opcodes.V1_8,Opcodes.ACC_PUBLIC,"ResourceSubclass",null,"jy",null);constructor(w,"jy","(Ljava/lang/String;Ljava/lang/String;)V");
            MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"toString","()Ljava/lang/String;",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);
            m.visitMethodInsn(Opcodes.INVOKESPECIAL,"jy","toString","()Ljava/lang/String;",false);m.visitLdcInsn("#variant");m.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/String","concat","(Ljava/lang/String;)Ljava/lang/String;",false);
            m.visitInsn(Opcodes.ARETURN);m.visitMaxs(0,0);m.visitEnd();w.visitEnd();byte[] bytes=w.toByteArray();return defineClass("ResourceSubclass",bytes,0,bytes.length);
        }
    }
    private static void constructor(ClassWriter w,String parent,String descriptor) {
        MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>",descriptor,null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);
        if(!descriptor.equals("()V")){m.visitVarInsn(Opcodes.ALOAD,1);m.visitVarInsn(Opcodes.ALOAD,2);}
        m.visitMethodInsn(Opcodes.INVOKESPECIAL,parent,"<init>",descriptor,false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();
    }
    private static URL url(Path p){try{return p.toUri().toURL();}catch(MalformedURLException failure){throw new IllegalArgumentException(failure);}}
    private static String hash(byte[] bytes) throws Exception{StringBuilder out=new StringBuilder();for(byte v:MessageDigest.getInstance("SHA-256").digest(bytes)){out.append(String.format(Locale.ROOT,"%02x",v));}return out.toString();}
    private static void require(boolean ok,String reason){if(!ok){throw new AssertionError(reason);}}
}
