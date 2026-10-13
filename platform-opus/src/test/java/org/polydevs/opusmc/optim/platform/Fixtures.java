package org.polydevs.opusmc.optim.platform;

import java.io.*;
import java.net.*;
import java.util.*;
import org.objectweb.asm.*;
import org.polydevs.opusmc.bootstrap.ClassTransformer;

public final class Fixtures {
    static boolean initialized;
    public static final class InitializationWitness {
        static { initialized = true; }
    }
    public static final class Helper {
        static int hits;
        public static void called() { hits++; }
    }
    static final class Probe implements ClassTransformer {
        String name; int rank, calls;
        Probe(String name, int rank) { this.name = name; this.rank = rank; }
        @Override public String id() { return name; }
        @Override public int priority() { return rank; }
        @Override public byte[] transform(String className, byte[] bytes) { calls++; return bytes; }
    }
    static byte[] bytes(Class<?> type) throws IOException {
        try (InputStream in = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
            if (in == null) { throw new IOException("Missing class fixture"); }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] block = new byte[1024]; int read;
            while ((read = in.read(block)) != -1) { out.write(block, 0, read); }
            return out.toByteArray();
        }
    }
    static ClassLoader duplicateLoader(Class<?> type) throws IOException {
        byte[] duplicate = bytes(type);
        return new ClassLoader(Fixtures.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.equals(type.getName())) { return super.loadClass(name, resolve); }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) { loaded = defineClass(name, duplicate, 0, duplicate.length); }
                    if (resolve) { resolveClass(loaded); }
                    return loaded;
                }
            }
        };
    }
    static URLClassLoader isolatedProviderLoader() {
        URL output = OptimTransformerProvider.class.getProtectionDomain().getCodeSource().getLocation();
        Set<String> isolated = new HashSet<>(Arrays.asList(
                OptimTransformerProvider.class.getName(), OptimBootstrap.class.getName(), ProviderState.class.getName()));
        return new URLClassLoader(new URL[]{output}, Fixtures.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!isolated.contains(name)) { return super.loadClass(name, resolve); }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) { loaded = findClass(name); }
                    if (resolve) { resolveClass(loaded); }
                    return loaded;
                }
            }
        };
    }
    static byte[] gameBytes() {
        ClassWriter out = new ClassWriter(0);
        out.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "fixture/FrameLoop", null, "java/lang/Object", null);
        MethodVisitor method = out.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", "()I", null, null);
        method.visitCode(); method.visitIntInsn(Opcodes.BIPUSH, 7); method.visitInsn(Opcodes.IRETURN);
        method.visitMaxs(1, 0); method.visitEnd(); out.visitEnd(); return out.toByteArray();
    }
    static ClassTransformer injectingTransformer() {
        return new ClassTransformer() {
            @Override public String id() { return "o2.probe"; }
            @Override public int priority() { return 200; }
            @Override public byte[] transform(String name, byte[] original) {
                ClassReader reader = new ClassReader(original);
                ClassWriter out = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                reader.accept(new ClassVisitor(Opcodes.ASM5, out) {
                    @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                        MethodVisitor delegate = super.visitMethod(access, name, desc, sig, exceptions);
                        return new MethodVisitor(Opcodes.ASM5, delegate) {
                            @Override public void visitInsn(int opcode) {
                                if (opcode == Opcodes.IRETURN) {
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                            Helper.class.getName().replace('.', '/'), "called", "()V", false);
                                }
                                super.visitInsn(opcode);
                            }
                        };
                    }
                }, 0);
                return out.toByteArray();
            }
        };
    }
    static final class GameLoader extends ClassLoader {
        GameLoader(ClassLoader parent) { super(parent); }
        Class<?> define(byte[] bytes) { return defineClass("fixture.FrameLoop", bytes, 0, bytes.length); }
    }
}
