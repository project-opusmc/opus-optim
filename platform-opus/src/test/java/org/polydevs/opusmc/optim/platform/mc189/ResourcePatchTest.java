package org.polydevs.opusmc.optim.platform.mc189;

import java.lang.reflect.Method;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.polydevs.opusmc.optim.api.*;
import org.polydevs.opusmc.optim.core.*;
import org.polydevs.opusmc.optim.platform.*;
import static org.junit.jupiter.api.Assertions.*;

class ResourcePatchTest {
    @Test void coldBodyRunsOncePerInstanceAndWarmCallsReuseImmutableContent() throws Exception {
        byte[] source=fixture();Method rewrite=ResourcePatch.class.getDeclaredMethod("rewrite",byte[].class);rewrite.setAccessible(true);
        byte[] bytes=(byte[])rewrite.invoke(new ResourcePatch(),(Object)source);
        class L extends ClassLoader {L(){super(ResourcePatchTest.class.getClassLoader());}Class<?> define(){return defineClass("jy",bytes,0,bytes.length);}}
        Class<?> type=new L().define();Object first=type.getConstructor(String.class).newInstance("one"),second=type.getConstructor(String.class).newInstance("two");
        String a=first.toString();assertEquals("one",a);assertSame(a,first.toString());assertEquals(1,type.getField("calls").getInt(first));
        assertEquals("two",second.toString());assertEquals(1,type.getField("calls").getInt(second));
        assertNotSame(a,second.toString());assertTrue(java.lang.reflect.Modifier.isVolatile(type.getDeclaredField("o2$resourceText").getModifiers()));
    }
    @Test void factoryFlagsAndPublicRejectedPinReportActualOutcome() throws Exception {
        ResourceOptimization.Candidate c=ResourceOptimization.candidate();CapabilitySet caps=new CapabilitySet(c.descriptor().requiredCapabilities());
        ProviderPlan absent=plan(new OptimConfig(true,Collections.emptyMap()),caps,c);assertTrue(absent.registrations().isEmpty());
        OptimConfig enable=new OptimConfig(true,Collections.singletonMap(c.descriptor().id(),true));
        assertTrue(plan(enable,new CapabilitySet(Collections.emptySet()),c).registrations().isEmpty());
        assertTrue(plan(new OptimConfig(false,enable.features()),caps,c).registrations().isEmpty());
        ProviderPlan selected=plan(enable,caps,c);byte[] wrong=fixture();
        assertEquals(ResourcePatch.ApplicationState.NOT_SEEN,c.applicationState());
        assertSame(wrong,selected.registrations().get(0).transformer().transform("other",wrong));
        assertEquals(ResourcePatch.ApplicationState.NOT_SEEN,c.applicationState());
        assertSame(wrong,selected.registrations().get(0).transformer().transform("jy",wrong));
        assertEquals(ResourcePatch.ApplicationState.UNSUPPORTED_INPUT,c.applicationState());
        assertEquals(ResourcePatch.ApplicationState.NOT_SEEN,ResourceOptimization.candidate().applicationState());
    }
    private ProviderPlan plan(OptimConfig config,CapabilitySet caps,ResourceOptimization.Candidate c) {
        return ProviderPlan.prepare(config,caps,CrashMarker.Recovery.clean(),Collections.singleton(c.binding()),Collections.emptyList(),getClass().getClassLoader());
    }
    private byte[] fixture() {
        ClassWriter w=new ClassWriter(0);w.visit(Opcodes.V1_8,Opcodes.ACC_PUBLIC,"jy",null,"java/lang/Object",null);
        w.visitField(Opcodes.ACC_PRIVATE|Opcodes.ACC_FINAL,"value","Ljava/lang/String;",null,null).visitEnd();w.visitField(Opcodes.ACC_PUBLIC,"calls","I",null,null).visitEnd();
        MethodVisitor ctor=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","(Ljava/lang/String;)V",null,null);ctor.visitCode();ctor.visitVarInsn(Opcodes.ALOAD,0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);ctor.visitVarInsn(Opcodes.ALOAD,0);ctor.visitVarInsn(Opcodes.ALOAD,1);
        ctor.visitFieldInsn(Opcodes.PUTFIELD,"jy","value","Ljava/lang/String;");ctor.visitInsn(Opcodes.RETURN);ctor.visitMaxs(2,2);ctor.visitEnd();
        MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"toString","()Ljava/lang/String;",null,null);m.visitCode();
        m.visitVarInsn(Opcodes.ALOAD,0);m.visitInsn(Opcodes.DUP);m.visitFieldInsn(Opcodes.GETFIELD,"jy","calls","I");m.visitInsn(Opcodes.ICONST_1);m.visitInsn(Opcodes.IADD);m.visitFieldInsn(Opcodes.PUTFIELD,"jy","calls","I");
        m.visitTypeInsn(Opcodes.NEW,"java/lang/String");m.visitInsn(Opcodes.DUP);m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,"jy","value","Ljava/lang/String;");
        m.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/String","<init>","(Ljava/lang/String;)V",false);m.visitInsn(Opcodes.ARETURN);m.visitMaxs(3,1);m.visitEnd();w.visitEnd();return w.toByteArray();
    }
}
