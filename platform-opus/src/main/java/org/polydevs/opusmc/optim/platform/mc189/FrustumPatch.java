package org.polydevs.opusmc.optim.platform.mc189;

import java.security.*;
import org.objectweb.asm.*;
import org.polydevs.opusmc.bootstrap.ClassTransformer;

/** One pinned M5 raw-notch class only; unknown input retains the host baseline. */
public final class FrustumPatch implements ClassTransformer {
    public enum ApplicationState { NOT_SEEN, APPLIED, UNSUPPORTED_INPUT }
    public static final String TARGET_SHA256 = "85b5a3893dfa826880a9e6e5a1221ac4443c1d778e38c384ef715911ababa7c4";
    private volatile ApplicationState state = ApplicationState.NOT_SEEN;

    @Override public String id() { return FrustumOptimization.ID; }
    @Override public int priority() { return 400; }
    public ApplicationState applicationState() { return state; }

    public static boolean matchesTarget(byte[] bytes) {
        if (bytes == null) { return false; }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest) {
                hex.append(Character.forDigit((value >>> 4) & 15, 16)).append(Character.forDigit(value & 15, 16));
            }
            return TARGET_SHA256.contentEquals(hex);
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable", impossible); }
    }

    @Override public byte[] transform(String name, byte[] original) {
        if (!"bid".equals(name)) { return original; }
        if (!matchesTarget(original)) { state = ApplicationState.UNSUPPORTED_INPUT; return original; }
        byte[] rewritten = rewrite(original);
        state = ApplicationState.APPLIED;
        return rewritten;
    }

    private static byte[] rewrite(byte[] original) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        int[] matched = {0};
        reader.accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                MethodVisitor out = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!name.equals("b") || !descriptor.equals("(DDDDDD)Z")) { return out; }
                matched[0]++;
                out.visitCode();
                out.visitVarInsn(Opcodes.ALOAD, 0);
                out.visitFieldInsn(Opcodes.GETFIELD, "bid", "disabled", "Z");
                Label enabled = new Label();
                out.visitJumpInsn(Opcodes.IFEQ, enabled);
                out.visitInsn(Opcodes.ICONST_1); out.visitInsn(Opcodes.IRETURN);
                out.visitLabel(enabled); out.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                out.visitVarInsn(Opcodes.ALOAD, 0);
                out.visitFieldInsn(Opcodes.GETFIELD, "bid", "a", "[[F");
                for (int slot = 1; slot <= 11; slot += 2) { out.visitVarInsn(Opcodes.DLOAD, slot); }
                out.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "org/polydevs/opusmc/optim/platform/mc189/FrustumMath", "visible", "([[FDDDDDD)Z", false);
                out.visitInsn(Opcodes.IRETURN); out.visitMaxs(0, 0); out.visitEnd();
                return null;
            }
        }, 0);
        if (matched[0] != 1) { throw new IllegalStateException("Pinned frustum method count changed"); }
        return writer.toByteArray();
    }
}
