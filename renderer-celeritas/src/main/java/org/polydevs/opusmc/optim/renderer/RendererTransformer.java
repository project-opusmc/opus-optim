package org.polydevs.opusmc.optim.renderer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

/** Exact M5 inputs only; explicit unsupported selection fails before gameplay. */
public final class RendererTransformer implements IClassTransformer {
    private static final String REGION = "net/optifine/render/VboRegion", FIELD = "o2$region";
    private static final String PREFIX = "org/polydevs/opusmc/optim/renderer/", TYPE = PREFIX + "RegionRenderer", HOOK = PREFIX + "RendererHooks";
    private static final Map<String,String> PINS;
    private static final Set<String> APPLIED = Collections.synchronizedSet(new HashSet<String>());
    static {
        Map<String,String> pins = new LinkedHashMap<>();
        pins.put("Config", "8530b8c8f9690c07f881dbf32a9ad579b0639f2701f5eff8a673c101ce3627aa");
        pins.put("ave", "430a17aa1911bcd0251aa4dcecb013b6c5b251bb5d0122c7305de0e36dc33e82");
        pins.put("bfr", "a60c44bbc5535e985b1c4b3549bb298b126e3039074aa910f99da97378250619");
        pins.put("bpo", "869d345907c9e49396f1214dcfe52f14509106b7f04fa80367cbc74914e10cbc");
        pins.put("net.minecraft.server.MinecraftServer", "63b4c32e947e2f83ea6db33ab685518603350455ee598a4efff180fc43045283");
        pins.put("bqs", "6e51da6383889c601142ab90c8f48fb3d5418eb6c3431458651d2f8389560ff3");
        pins.put("net.optifine.render.VboRegion", "38f2e59fa54ecc815e7ba8a1db398ecf79685b0bab1a0ebe143de8bc263da441");
        PINS = Collections.unmodifiableMap(pins);
    }
    private final boolean enabled;
    public RendererTransformer() { this("celeritas-region".equals(System.getProperty("opus.optim.renderer"))); }
    RendererTransformer(boolean enabled) { this.enabled = enabled; }
    static boolean allApplied() { return APPLIED.containsAll(PINS.keySet()); }
    @Override public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (!enabled || bytes == null) return bytes;
        String target = PINS.containsKey(name) ? name : transformedName;
        if (!PINS.containsKey(target)) return bytes;
        if (!PINS.get(target).equals(hash(bytes))) throw new IllegalStateException("O2 unsupported renderer target: " + target + " sha256=" + hash(bytes));
        byte[] output = patchKnown(target, bytes); APPLIED.add(target);
        System.out.println("[O2] renderer hook APPLIED " + target + " input=" + PINS.get(target));
        return output;
    }
    static byte[] patchKnown(String target, byte[] bytes) {
        if (target.equals("net.minecraft.server.MinecraftServer")) {
            int[] fields={0};
            new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM5){
                @Override public FieldVisitor visitField(int access,String name,String desc,String sig,Object value){if(name.equals("aa")&&desc.equals("Ljava/lang/Thread;"))fields[0]++;return null;}
            },ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
            if(fields[0]!=1)throw new IllegalStateException("O2 server-thread field anchor changed");
            return bytes;
        }
        ClassReader reader = new ClassReader(bytes); ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        int[] hooked = {0};
        reader.accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                MethodVisitor out = super.visitMethod(access,name,desc,sig,ex);
                return new MethodVisitor(Opcodes.ASM5,out) {
                    @Override public void visitCode() {
                        super.visitCode();
                        if (target.equals("bfr") && name.equals("a") && (desc.equals("()V") || desc.equals("(Lbdb;)V"))) {
                            hooked[0]++;out.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"worldReset","()V",false);
                        }
                        if (!target.equals("net.optifine.render.VboRegion")) return;
                        String call = null;
                        if (name.equals("bufferData") && desc.equals("(Ljava/nio/ByteBuffer;Lnet/optifine/render/VboRange;)V")) call = "upload";
                        if (name.equals("drawArrays") && desc.equals("(ILnet/optifine/render/VboRange;)V")) call = "queue";
                        if (name.equals("finishDraw") && desc.equals("(Lbfy;)V")) call = "finish";
                        if (name.equals("deleteGlBuffers") && desc.equals("()V")) call = "delete";
                        if (name.equals("getPositionTop") && desc.equals("()I")) call = "positionTop";
                        if (name.equals("bindBuffer") && desc.equals("()V")) call = "bind";
                        if (call == null) return;
                        hooked[0]++;
                        out.visitVarInsn(Opcodes.ALOAD,0); out.visitFieldInsn(Opcodes.GETFIELD,REGION,FIELD,"L"+TYPE+";");
                        out.visitInsn(Opcodes.DUP); Label original = new Label();out.visitJumpInsn(Opcodes.IFNULL,original);
                        switch (call) {
                            case "upload": out.visitVarInsn(Opcodes.ALOAD,2);out.visitVarInsn(Opcodes.ALOAD,1);out.visitMethodInsn(Opcodes.INVOKEVIRTUAL,TYPE,"upload","(Ljava/lang/Object;Ljava/nio/ByteBuffer;)V",false);break;
                            case "queue": out.visitVarInsn(Opcodes.ILOAD,1);out.visitVarInsn(Opcodes.ALOAD,2);out.visitMethodInsn(Opcodes.INVOKEVIRTUAL,TYPE,"queue","(ILjava/lang/Object;)V",false);break;
                            case "finish": out.visitVarInsn(Opcodes.ALOAD,1);out.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"finish","(L"+TYPE+";Ljava/lang/Object;)V",false);break;
                            case "delete": out.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"delete","(L"+TYPE+";)V",false);break;
                            case "positionTop": out.visitMethodInsn(Opcodes.INVOKEVIRTUAL,TYPE,"positionTop","()I",false);break;
                            default: out.visitMethodInsn(Opcodes.INVOKEVIRTUAL,TYPE,"bind","()V",false);
                        }
                        out.visitInsn(call.equals("positionTop") ? Opcodes.IRETURN : Opcodes.RETURN);
                        out.visitLabel(original);out.visitFrame(Opcodes.F_SAME1,0,null,1,new Object[]{TYPE});out.visitInsn(Opcodes.POP);
                    }
                    @Override public void visitFieldInsn(int op,String owner,String field,String fieldDesc) {
                        super.visitFieldInsn(op,owner,field,fieldDesc);
                        if (target.equals("bqs") && name.equals("f") && desc.equals("()Z") && op==Opcodes.GETSTATIC && field.equals("vboRegions")) {
                            hooked[0]++;out.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"vboRegions","(Z)Z",false);
                        }
                    }
                    @Override public void visitMethodInsn(int op,String owner,String method,String methodDesc,boolean itf) {
                        if(target.equals("bpo")&&name.equals("w")&&desc.equals("()V")&&op==Opcodes.INVOKESTATIC&&owner.equals("com/google/common/util/concurrent/Futures")&&method.equals("getUnchecked")&&methodDesc.equals("(Ljava/util/concurrent/Future;)Ljava/lang/Object;")) {
                            hooked[0]++;out.visitVarInsn(Opcodes.ALOAD,0);
                            out.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"prepareServerTaskWait","(Ljava/util/concurrent/Future;Ljava/lang/Object;)Ljava/util/concurrent/Future;",false);
                        }
                        super.visitMethodInsn(op,owner,method,methodDesc,itf);
                    }
                    @Override public void visitInsn(int op) {
                        if (target.equals("ave") && name.equals("a") && desc.equals("(Lbdb;Ljava/lang/String;)V") && op==Opcodes.RETURN) {
                            // Preserve host disconnect/shutdown ordering before bulk GPU deletion.
                            hooked[0]++;out.visitVarInsn(Opcodes.ALOAD,1);out.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"worldChanging","(Ljava/lang/Object;)V",false);
                        }
                        if (target.equals("Config") && name.equals("isRenderRegions") && desc.equals("()Z") && op==Opcodes.IRETURN) {
                            hooked[0]++;out.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"regions","(Z)Z",false);
                        }
                        if (target.equals("Config") && name.equals("initDisplay") && desc.equals("()V") && op==Opcodes.RETURN) {
                            hooked[0]++;out.visitLdcInsn(Type.getObjectType("Config"));out.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"initialize","(Ljava/lang/Class;)V",false);
                        }
                        if (target.equals("net.optifine.render.VboRegion") && name.equals("<init>") && op==Opcodes.RETURN) {
                            hooked[0]++;
                            out.visitVarInsn(Opcodes.ALOAD,0);out.visitVarInsn(Opcodes.ALOAD,0);out.visitFieldInsn(Opcodes.GETFIELD,REGION,"vertexBytes","I");out.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"create","(I)L"+TYPE+";",false);out.visitFieldInsn(Opcodes.PUTFIELD,REGION,FIELD,"L"+TYPE+";");
                            out.visitVarInsn(Opcodes.ALOAD,0);out.visitVarInsn(Opcodes.ALOAD,0);out.visitFieldInsn(Opcodes.GETFIELD,REGION,"glBufferId","I");out.visitVarInsn(Opcodes.ALOAD,0);out.visitFieldInsn(Opcodes.GETFIELD,REGION,FIELD,"L"+TYPE+";");out.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"releaseOriginal","(IL"+TYPE+";)I",false);out.visitFieldInsn(Opcodes.PUTFIELD,REGION,"glBufferId","I");
                        }
                        super.visitInsn(op);
                    }
                };
            }
            @Override public void visitEnd() {
                if (target.equals("net.optifine.render.VboRegion")) writer.visitField(Opcodes.ACC_PRIVATE|Opcodes.ACC_SYNTHETIC,FIELD,"L"+TYPE+";",null,null).visitEnd();
                super.visitEnd();
            }
        },0);
        int minimum = target.equals("net.optifine.render.VboRegion") ? 7 : target.equals("Config") || target.equals("bfr") ? 2 : 1;
        if (hooked[0] < minimum) throw new IllegalStateException("O2 renderer method anchors changed: " + target);
        return writer.toByteArray();
    }
    private static String hash(byte[] bytes) {
        try {StringBuilder s=new StringBuilder(64);for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes)){s.append(Character.forDigit((b>>>4)&15,16));s.append(Character.forDigit(b&15,16));}return s.toString();}
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
