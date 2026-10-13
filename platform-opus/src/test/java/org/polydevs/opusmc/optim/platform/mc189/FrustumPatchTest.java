package org.polydevs.opusmc.optim.platform.mc189;

import java.lang.reflect.Method;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.polydevs.opusmc.optim.api.*;
import org.polydevs.opusmc.optim.core.*;
import org.polydevs.opusmc.optim.platform.*;
import static org.junit.jupiter.api.Assertions.*;

class FrustumPatchTest {
    @Test void unknownBytesAndNonTargetClassesRemainUnchangedWithObservableState() throws Exception {
        FrustumPatch transformer = new FrustumPatch(); byte[] other = {1, 2, 3};
        assertSame(other, transformer.transform("other.Class", other));
        assertEquals(FrustumPatch.ApplicationState.NOT_SEEN, transformer.applicationState());
        assertFalse(FrustumPatch.matchesTarget(other));
        assertSame(other, transformer.transform("bid", other));
        assertEquals(FrustumPatch.ApplicationState.UNSUPPORTED_INPUT, transformer.applicationState());
    }
    @Test void syntheticRewriteCallsMathPreservesDisabledBypassAndLeavesFullPredicateAlone() throws Exception {
        byte[] input = fixtureBytes();
        assertFalse(FrustumPatch.matchesTarget(input));
        // Test the private rewrite with an owned fixture; public exact-pin path is
        // separately executed against real local M5 bytes by frustumVerify.
        Method rewrite = FrustumPatch.class.getDeclaredMethod("rewrite", byte[].class);
        rewrite.setAccessible(true);
        byte[] rewritten = (byte[]) rewrite.invoke(new FrustumPatch(), (Object) input);
        Class<?> type = new GameLoader().define(rewritten); Object instance = type.getConstructor().newInstance();
        Method visible = type.getMethod("b", double.class, double.class, double.class, double.class, double.class, double.class);
        Method fully = type.getMethod("isBoxInFrustumFully", double.class, double.class, double.class, double.class, double.class, double.class);
        type.getField("a").set(instance, FrustumMathTest.acceptingPlanes());
        assertEquals(true, visible.invoke(instance, 0d, 0d, 0d, 1d, 1d, 1d));
        assertEquals(false, fully.invoke(instance, 0d, 0d, 0d, 1d, 1d, 1d));
        float[][] outside = FrustumMathTest.acceptingPlanes(); outside[0][3] = -1;
        type.getField("a").set(instance, outside);
        assertEquals(false, visible.invoke(instance, 0d, 0d, 0d, 1d, 1d, 1d));
        type.getField("disabled").setBoolean(instance, true); type.getField("a").set(instance, null);
        assertEquals(true, visible.invoke(instance, Double.NaN, 0d, 0d, 1d, 1d, 1d));
    }
    @Test void independentFlagAndCapabilitiesAreRequiredAndStatusRemainsExperimental() {
        TransformerBinding binding = FrustumOptimization.binding();
        ProviderPlan off = plan(new OptimConfig(false, Collections.singletonMap(FrustumOptimization.ID, true)), allCapabilities(), binding);
        assertTrue(off.registrations().isEmpty());
        assertEquals(ExclusionReason.OPTIM_OFF, off.patchPlan().excluded().get(FrustumOptimization.ID).code());
        ProviderPlan absentFlag = plan(new OptimConfig(true, Collections.emptyMap()), allCapabilities(), binding);
        assertTrue(absentFlag.registrations().isEmpty());
        assertEquals(ExclusionReason.EXPERIMENTAL_NOT_ENABLED, absentFlag.patchPlan().excluded().get(FrustumOptimization.ID).code());
        OptimConfig enabled = new OptimConfig(true, Collections.singletonMap(FrustumOptimization.ID, true));
        ProviderPlan missing = plan(enabled, new CapabilitySet(Collections.emptySet()), binding);
        assertEquals(ExclusionReason.CAPABILITY_MISSING, missing.patchPlan().excluded().get(FrustumOptimization.ID).code());
        ProviderPlan active = plan(enabled, allCapabilities(), binding);
        assertEquals(Collections.singletonList(FrustumOptimization.ID), active.patchPlan().selectedIds());
        assertEquals(Collections.singleton("bid"), active.registrations().get(0).targetClasses());
        assertEquals(OptimizationStatus.EXPERIMENTAL, FrustumOptimization.descriptor().status());
    }
    private ProviderPlan plan(OptimConfig config, CapabilitySet caps, TransformerBinding binding) {
        return ProviderPlan.prepare(config, caps, CrashMarker.Recovery.clean(), Collections.singleton(binding),
                Collections.emptyList(), getClass().getClassLoader());
    }
    private CapabilitySet allCapabilities() {
        return new CapabilitySet(Arrays.asList(FrustumOptimization.TARGET_CAPABILITY, FrustumOptimization.ASM_CAPABILITY));
    }
    private static final class GameLoader extends ClassLoader {
        GameLoader() { super(FrustumPatchTest.class.getClassLoader()); }
        Class<?> define(byte[] bytes) { return defineClass("bid", bytes, 0, bytes.length); }
    }
    private byte[] fixtureBytes() {
        ClassWriter out = new ClassWriter(0);
        out.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "bid", null, "java/lang/Object", null);
        out.visitField(Opcodes.ACC_PUBLIC, "a", "[[F", null, null).visitEnd();
        out.visitField(Opcodes.ACC_PUBLIC, "disabled", "Z", null, null).visitEnd();
        MethodVisitor ctor = out.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.visitCode(); ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(Opcodes.RETURN); ctor.visitMaxs(1, 1); ctor.visitEnd();
        for (String name : Arrays.asList("b", "isBoxInFrustumFully")) {
            MethodVisitor method = out.visitMethod(Opcodes.ACC_PUBLIC, name, "(DDDDDD)Z", null, null);
            method.visitCode(); method.visitInsn(Opcodes.ICONST_0); method.visitInsn(Opcodes.IRETURN);
            method.visitMaxs(1, 13); method.visitEnd();
        }
        out.visitEnd(); return out.toByteArray();
    }
}
