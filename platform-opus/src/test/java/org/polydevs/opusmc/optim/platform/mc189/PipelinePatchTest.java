package org.polydevs.opusmc.optim.platform.mc189;

import java.lang.reflect.Method;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.polydevs.opusmc.optim.api.*;
import org.polydevs.opusmc.optim.core.*;
import org.polydevs.opusmc.optim.platform.*;
import static org.junit.jupiter.api.Assertions.*;

class PipelinePatchTest {
    @Test void chunkBuilderLimitsEachInitialLayerTo32KiB() throws Exception {
        ClassWriter buffer=type("bfd");
        buffer.visitField(Opcodes.ACC_PUBLIC,"capacity","I",null,null).visitEnd();
        MethodVisitor c=buffer.visitMethod(Opcodes.ACC_PUBLIC,"<init>","(I)V",null,null);c.visitCode();
        c.visitVarInsn(Opcodes.ALOAD,0);c.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);
        c.visitVarInsn(Opcodes.ALOAD,0);c.visitVarInsn(Opcodes.ILOAD,1);c.visitInsn(Opcodes.ICONST_4);c.visitInsn(Opcodes.IMUL);
        c.visitFieldInsn(Opcodes.PUTFIELD,"bfd","capacity","I");c.visitInsn(Opcodes.RETURN);c.visitMaxs(3,2);c.visitEnd();buffer.visitEnd();
        ClassWriter builder=type("bfg");builder.visitField(Opcodes.ACC_PUBLIC,"layers","[Lbfd;",null,null).visitEnd();
        MethodVisitor m=builder.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);m.visitCode();
        m.visitVarInsn(Opcodes.ALOAD,0);m.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);
        m.visitVarInsn(Opcodes.ALOAD,0);m.visitInsn(Opcodes.ICONST_4);m.visitTypeInsn(Opcodes.ANEWARRAY,"bfd");m.visitFieldInsn(Opcodes.PUTFIELD,"bfg","layers","[Lbfd;");
        int[] capacities={2097152,131072,131072,262144};
        for(int i=0;i<4;i++) {
            m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,"bfg","layers","[Lbfd;");m.visitLdcInsn(i);
            m.visitTypeInsn(Opcodes.NEW,"bfd");m.visitInsn(Opcodes.DUP);m.visitLdcInsn(capacities[i]);
            m.visitMethodInsn(Opcodes.INVOKESPECIAL,"bfd","<init>","(I)V",false);m.visitInsn(Opcodes.AASTORE);
        }
        m.visitInsn(Opcodes.RETURN);m.visitMaxs(5,1);m.visitEnd();builder.visitEnd();
        byte[] changed=rewrite(PipelineOptimizations.Kind.valueOf("CHUNK_BUFFERS"),builder.toByteArray());
        class Loader extends ClassLoader {Loader(){super(PipelinePatchTest.class.getClassLoader());}Class<?> load(String n,byte[] b){return defineClass(n,b,0,b.length);}}
        Loader loader=new Loader();Class<?> bufferType=loader.load("bfd",buffer.toByteArray()),builderType=loader.load("bfg",changed);
        Object instance=builderType.getConstructor().newInstance();Object[] layers=(Object[])builderType.getField("layers").get(instance);
        assertEquals(4,layers.length);
        for(Object layer:layers)assertEquals(32768,bufferType.getField("capacity").getInt(layer));
    }
    @Test void allIndependentCandidatesCanBePreparedTogether() {
        List<TransformerBinding> bindings=new ArrayList<>();Map<String,Boolean> flags=new HashMap<>();Set<String> capabilities=new HashSet<>();
        bindings.add(FrustumOptimization.candidate().binding());flags.put(FrustumOptimization.ID,true);
        capabilities.addAll(FrustumOptimization.descriptor().requiredCapabilities());
        for(PipelineOptimizations.Candidate c:PipelineOptimizations.candidates()) {
            bindings.add(c.binding());flags.put(c.descriptor().id(),true);capabilities.addAll(c.descriptor().requiredCapabilities());
        }
        ResourceOptimization.Candidate resource=ResourceOptimization.candidate();bindings.add(resource.binding());
        flags.put(resource.descriptor().id(),true);capabilities.addAll(resource.descriptor().requiredCapabilities());
        ProviderPlan all=ProviderPlan.prepare(new OptimConfig(true,flags),new CapabilitySet(capabilities),CrashMarker.Recovery.clean(),
                bindings,Collections.emptyList(),getClass().getClassLoader());
        assertEquals(Arrays.asList("render.buffer_growth","render.frustum.positive-vertex","runtime.resource_string_cache","text.glyph_lookup","world.chunk_buffer_capacity","world.fixed_budget"),all.patchPlan().selectedIds());
        assertEquals(6,all.registrations().size());
    }
    @Test void hostRoutingAliasIsDeclaredAndRefusalStillVisibleAtTheAlias() throws Exception {
        String mapped="net.minecraft.client.gui.FontRenderer";
        PipelineOptimizations.Candidate candidate=PipelineOptimizations.candidate(PipelineOptimizations.Kind.GLYPH_LOOKUP,
                Collections.singleton(mapped));
        ProviderPlan selected=plan(Collections.singletonMap(candidate.descriptor().id(),true),caps(),Collections.singleton(candidate));
        assertEquals(new HashSet<>(Arrays.asList("avn",mapped)),selected.registrations().get(0).targetClasses());
        byte[] wrong={1,2,3};
        assertSame(wrong,selected.registrations().get(0).transformer().transform(mapped.replace('.','/'),wrong));
        assertEquals(PipelinePatch.ApplicationState.UNSUPPORTED_INPUT,candidate.applicationState());
        assertThrows(IllegalArgumentException.class,()->PipelineOptimizations.candidate(PipelineOptimizations.Kind.GLYPH_LOOKUP,Collections.singleton(" ")));
    }
    @Test void publicFactoryReportsRefusedPinsIndependentlyAfterSelectedRegistration() throws Exception {
        for (PipelineOptimizations.Candidate c : PipelineOptimizations.candidates()) {
            PipelineOptimizations.Candidate other = PipelineOptimizations.candidate(c.kind());
            ProviderPlan selected = plan(Collections.singletonMap(c.descriptor().id(), true), caps(), Collections.singleton(c));
            assertEquals(PipelinePatch.ApplicationState.NOT_SEEN, c.applicationState());
            byte[] wrong = {1,2,3};
            assertSame(wrong, selected.registrations().get(0).transformer().transform(c.kind().target(), wrong));
            assertEquals(PipelinePatch.ApplicationState.UNSUPPORTED_INPUT, c.applicationState());
            assertEquals(PipelinePatch.ApplicationState.NOT_SEEN, other.applicationState());
        }
    }
    @Test void flagsAreIndependentAndMissingCapabilityAndOffContributeNoTransforms() {
        List<PipelineOptimizations.Candidate> all = PipelineOptimizations.candidates();
        assertTrue(plan(Collections.emptyMap(), caps(), all).registrations().isEmpty());
        for (PipelineOptimizations.Candidate c : all) {
            Map<String,Boolean> flag = Collections.singletonMap(c.descriptor().id(), true);
            ProviderPlan selected = plan(flag, caps(), all);
            assertEquals(Collections.singletonList(c.descriptor().id()), selected.patchPlan().selectedIds());
            assertEquals(Collections.singleton(c.kind().target()), selected.registrations().get(0).targetClasses());
            assertTrue(plan(flag, new CapabilitySet(Collections.emptySet()), all).registrations().isEmpty());
            List<TransformerBinding> bindings = new ArrayList<>(); for (PipelineOptimizations.Candidate x : all) { bindings.add(x.binding()); }
            assertTrue(ProviderPlan.prepare(new OptimConfig(false, flag), caps(), CrashMarker.Recovery.clean(), bindings,
                    Collections.emptyList(), getClass().getClassLoader()).registrations().isEmpty());
        }
    }
    @Test void glyphRewriteExecutesOwnedAlphabetAndPreservesSeparateFormattingSearch() throws Exception {
        StringBuilder a = new StringBuilder(); for (int i=0;i<256;i++) { a.append((char)(i%128)); }
        String alphabet = a.toString();
        ClassWriter w = type("avn"); constructor(w);
        w.visitField(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"alphabetLength","I",null,null).visitEnd();
        w.visitField(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"obfuscatedChar","I",null,null).visitEnd();
        glyphMethod(w, "a", "(CZ)F", alphabet, Opcodes.FRETURN);
        glyphMethod(w, "getCharWidthFloat", "(C)F", alphabet, Opcodes.FRETURN);
        glyphMethod(w, "b", "(C)I", "abc", Opcodes.IRETURN);
        MethodVisitor draw=w.visitMethod(Opcodes.ACC_PUBLIC,"a","(Ljava/lang/String;Z)V",null,null);
        draw.visitCode();draw.visitLdcInsn(alphabet);draw.visitInsn(Opcodes.ICONST_0);
        draw.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/String","indexOf","(I)I",false);draw.visitInsn(Opcodes.POP);
        draw.visitLdcInsn(alphabet);draw.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/String","length","()I",false);
        draw.visitFieldInsn(Opcodes.PUTSTATIC,"avn","alphabetLength","I");
        draw.visitLdcInsn(alphabet);draw.visitIntInsn(Opcodes.BIPUSH,66);draw.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/String","charAt","(I)C",false);
        draw.visitFieldInsn(Opcodes.PUTSTATIC,"avn","obfuscatedChar","I");
        draw.visitInsn(Opcodes.RETURN);draw.visitMaxs(2,3);draw.visitEnd();
        MethodVisitor format=w.visitMethod(Opcodes.ACC_PUBLIC,"format","(I)I",null,null);
        format.visitCode();format.visitLdcInsn("abc");format.visitVarInsn(Opcodes.ILOAD,1);
        format.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/String","indexOf","(I)I",false);
        format.visitInsn(Opcodes.IRETURN);format.visitMaxs(2,2);format.visitEnd();w.visitEnd();
        Class<?> t=define("avn",rewrite(PipelineOptimizations.Kind.GLYPH_LOOKUP,w.toByteArray()));Object o=t.getConstructor().newInstance();
        Method index=t.getMethod("getCharWidthFloat",char.class);
        for(int c=0;c<65536;c++){assertEquals((float)alphabet.indexOf(c),index.invoke(o,(char)c));}
        assertEquals(1,t.getMethod("b",char.class).invoke(o,'b'));
        t.getMethod("a",String.class,boolean.class).invoke(o,"owned",true);
        assertEquals(256,t.getField("alphabetLength").getInt(null));
        assertEquals(66,t.getField("obfuscatedChar").getInt(null));
        assertEquals(1,t.getMethod("format",int.class).invoke(o,(int)'b'));
    }
    @Test void bufferRewriteAppliesCapacityBeforeTheOriginalStore() throws Exception {
        ClassWriter w=type("bfd");w.visitField(Opcodes.ACC_PUBLIC,"capacity","I",null,null).visitEnd();constructor(w);
        MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"b","(I)V",null,null);m.visitCode();
        m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,"bfd","capacity","I");m.visitVarInsn(Opcodes.ISTORE,2);
        m.visitVarInsn(Opcodes.ILOAD,1);m.visitVarInsn(Opcodes.ISTORE,4);
        m.visitVarInsn(Opcodes.ALOAD,0);m.visitVarInsn(Opcodes.ILOAD,4);m.visitFieldInsn(Opcodes.PUTFIELD,"bfd","capacity","I");
        m.visitInsn(Opcodes.RETURN);m.visitMaxs(2,5);m.visitEnd();w.visitEnd();
        Class<?> t=define("bfd",rewrite(PipelineOptimizations.Kind.BUFFER_GROWTH,w.toByteArray()));Object o=t.getConstructor().newInstance();
        t.getField("capacity").setInt(o,4*1024*1024);t.getMethod("b",int.class).invoke(o,6*1024*1024);
        assertEquals(8*1024*1024,t.getField("capacity").getInt(o));
    }
    @Test void uploadRewriteCapsFutureDeadlineButRetainsZeroAndExpiredInput() throws Exception {
        ClassWriter w=type("bho");w.visitField(Opcodes.ACC_PUBLIC,"deadline","J",null,null).visitEnd();constructor(w);
        MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"a","(J)Z",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);
        m.visitVarInsn(Opcodes.LLOAD,1);m.visitFieldInsn(Opcodes.PUTFIELD,"bho","deadline","J");
        m.visitInsn(Opcodes.ICONST_0);m.visitInsn(Opcodes.IRETURN);m.visitMaxs(3,3);m.visitEnd();w.visitEnd();
        Class<?> t=define("bho",rewrite(PipelineOptimizations.Kind.UPLOAD_BUDGET,w.toByteArray()));Object o=t.getConstructor().newInstance();
        Method call=t.getMethod("a",long.class);long before=System.nanoTime();call.invoke(o,before+1_000_000_000L);long after=System.nanoTime();
        long deadline=t.getField("deadline").getLong(o);
        assertTrue(deadline-before>=2_000_000L);assertTrue(deadline-before<=after-before+2_000_000L);
        call.invoke(o,0L);assertEquals(0L,t.getField("deadline").getLong(o));
        call.invoke(o,before-1);assertEquals(before-1,t.getField("deadline").getLong(o));
    }
    private ProviderPlan plan(Map<String,Boolean> flags,CapabilitySet caps,Collection<PipelineOptimizations.Candidate> candidates) {
        List<TransformerBinding> bindings=new ArrayList<>();for(PipelineOptimizations.Candidate c:candidates){bindings.add(c.binding());}
        return ProviderPlan.prepare(new OptimConfig(true,flags),caps,CrashMarker.Recovery.clean(),bindings,Collections.emptyList(),getClass().getClassLoader());
    }
    private CapabilitySet caps(){Set<String> caps=new HashSet<>();for(PipelineOptimizations.Candidate c:PipelineOptimizations.candidates()){caps.addAll(c.descriptor().requiredCapabilities());}return new CapabilitySet(caps);}
    private byte[] rewrite(PipelineOptimizations.Kind kind,byte[] source)throws Exception {
        Method m=PipelinePatch.class.getDeclaredMethod("rewrite",byte[].class);m.setAccessible(true);return(byte[])m.invoke(new PipelinePatch(kind),(Object)source);
    }
    private Class<?> define(String name,byte[] bytes){class L extends ClassLoader {L(){super(PipelinePatchTest.class.getClassLoader());}Class<?> d(){return defineClass(name,bytes,0,bytes.length);}}return new L().d();}
    private ClassWriter type(String name){ClassWriter w=new ClassWriter(0);w.visit(Opcodes.V1_8,Opcodes.ACC_PUBLIC,name,null,"java/lang/Object",null);return w;}
    private void constructor(ClassWriter w){MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(1,1);m.visitEnd();}
    private void glyphMethod(ClassWriter w,String name,String desc,String alphabet,int ret){MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,name,desc,null,null);m.visitCode();m.visitLdcInsn(alphabet);m.visitVarInsn(Opcodes.ILOAD,1);m.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/String","indexOf","(I)I",false);if(ret==Opcodes.FRETURN)m.visitInsn(Opcodes.I2F);m.visitInsn(ret);m.visitMaxs(2,3);m.visitEnd();}
}
