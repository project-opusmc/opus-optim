package org.polydevs.opusmc.optim.probe;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.objectweb.asm.*;

/** Test-only dispatch after the real game loop, outside Minecraft's task monitor. */
public final class ProbeFrameTransformer implements ClassFileTransformer {
    @Override public byte[] transform(ClassLoader loader, String name, Class<?> redefining,
            ProtectionDomain domain, byte[] bytes) {
        if ("org/polydevs/opusmc/client/EarlyClientRuntime".equals(name)
                && loader instanceof LaunchClassLoader) {
            ((LaunchClassLoader) loader).addClassLoaderExclusion("org.polydevs.opusmc.optim.probe.");
            ClassReader earlyReader = new ClassReader(bytes);
            ClassWriter earlyWriter = new ClassWriter(earlyReader, ClassWriter.COMPUTE_MAXS);
            earlyReader.accept(new ClassVisitor(Opcodes.ASM5, earlyWriter) {
                @Override public MethodVisitor visitMethod(int access, String method, String desc, String sig, String[] ex) {
                    MethodVisitor output = super.visitMethod(access, method, desc, sig, ex);
                    if (!"displayReady".equals(method) || !"()V".equals(desc)) return output;
                    return new MethodVisitor(Opcodes.ASM5, output) {
                        @Override public void visitInsn(int opcode) {
                            if (opcode == Opcodes.RETURN) super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                "org/polydevs/opusmc/optim/probe/GameProbeAgent", "onStartupFrame", "()V", false);
                            super.visitInsn(opcode);
                        }
                    };
                }
            }, 0);
            return earlyWriter.toByteArray();
        }
        if (!"ave".equals(name) || !(loader instanceof LaunchClassLoader)) return null;
        ((LaunchClassLoader) loader).addClassLoaderExclusion("org.polydevs.opusmc.optim.probe.");
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        int[] calls = {0};
        reader.accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override public MethodVisitor visitMethod(int access, String method, String desc, String sig, String[] ex) {
                MethodVisitor output = super.visitMethod(access, method, desc, sig, ex);
                if (!"av".equals(method) || !"()V".equals(desc)) return output;
                return new MethodVisitor(Opcodes.ASM5, output) {
                    @Override public void visitInsn(int opcode) {
                        if (opcode == Opcodes.RETURN) {
                            calls[0]++;
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                    "org/polydevs/opusmc/optim/probe/GameProbeAgent", "onFrame", "()V", false);
                        }
                        super.visitInsn(opcode);
                    }
                };
            }
        }, 0);
        if (calls[0] == 0) throw new IllegalStateException("Disposable game-loop anchor unavailable");
        System.out.println("[O2/PROBE] frame dispatch hooked after host transforms");
        return writer.toByteArray();
    }
}
