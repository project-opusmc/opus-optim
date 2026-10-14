package org.polydevs.opusmc.optim.probe;

import java.net.URL;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import static org.junit.jupiter.api.Assertions.*;

final class ProbeFrameTransformerTest {
    @Test void frameDispatchRunsAfterTaskMonitorAndUnrelatedClassesRemainUntouched() throws Exception {
        ClassWriter fixture = new ClassWriter(0);
        fixture.visit(52, Opcodes.ACC_PUBLIC, "ave", null, "java/lang/Object", null);
        MethodVisitor body = fixture.visitMethod(Opcodes.ACC_PRIVATE, "av", "()V", null, null);
        body.visitCode(); body.visitVarInsn(Opcodes.ALOAD, 0); body.visitInsn(Opcodes.MONITORENTER);
        body.visitVarInsn(Opcodes.ALOAD, 0); body.visitInsn(Opcodes.MONITOREXIT);
        body.visitInsn(Opcodes.RETURN); body.visitMaxs(1, 1); body.visitEnd(); fixture.visitEnd();
        ProbeFrameTransformer transformer = new ProbeFrameTransformer();
        try (LaunchClassLoader loader = new LaunchClassLoader(new URL[0])) {
            byte[] bytes = fixture.toByteArray();
            assertNull(transformer.transform(loader, "other", null, null, bytes));
            byte[] patched = transformer.transform(loader, "ave", null, null, bytes);
            int[] monitor = {0}, calls = {0};
            new ClassReader(patched).accept(new ClassVisitor(Opcodes.ASM5) {
                @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                    return new MethodVisitor(Opcodes.ASM5) {
                        @Override public void visitInsn(int op) { if (op == Opcodes.MONITORENTER) monitor[0]++; if (op == Opcodes.MONITOREXIT) monitor[0]--; }
                        @Override public void visitMethodInsn(int op, String owner, String method, String desc, boolean itf) {
                            if (method.equals("onFrame")) { assertEquals(0, monitor[0]); calls[0]++; }
                        }
                    };
                }
            }, 0);
            assertEquals(1, calls[0]);
        }
    }
}
