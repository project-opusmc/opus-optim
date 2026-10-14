package org.polydevs.opusmc.optim.platform.mc189;

import java.security.*;
import org.objectweb.asm.*;
import org.polydevs.opusmc.bootstrap.ClassTransformer;

/** Retains the original miss body; duplicate cold races may compute equivalent strings. */
public final class ResourcePatch implements ClassTransformer {
    public enum ApplicationState { NOT_SEEN, APPLIED, UNSUPPORTED_INPUT }
    public static final String TARGET_SHA256="4dcb02692da8c3ae7d6137281b1a25e5f249514e2bc7d1efeb7b9400f857dd22";
    private static final String CACHE="o2$resourceText";
    private volatile ApplicationState state=ApplicationState.NOT_SEEN;
    @Override public String id() { return ResourceOptimization.ID; }
    @Override public int priority() { return 400; }
    public ApplicationState applicationState() { return state; }
    @Override public byte[] transform(String name,byte[] original) {
        if(!"jy".equals(name)){return original;}
        if(original==null||!TARGET_SHA256.equals(hash(original))){state=ApplicationState.UNSUPPORTED_INPUT;return original;}
        byte[] output=rewrite(original);state=ApplicationState.APPLIED;return output;
    }
    private byte[] rewrite(byte[] original) {
        ClassReader reader=new ClassReader(original);if(!reader.getClassName().equals("jy")){throw new IllegalArgumentException("Target identity differs");}
        ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);int[] methods={0},returns={0};
        reader.accept(new ClassVisitor(Opcodes.ASM5,writer) {
            @Override public FieldVisitor visitField(int a,String n,String d,String s,Object v) {
                if(n.equals(CACHE)){throw new IllegalArgumentException("Resource cache field already exists");}
                return super.visitField(a,n,d,s,v);
            }
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
                MethodVisitor out=super.visitMethod(access,name,desc,sig,ex);
                if(!name.equals("toString")||!desc.equals("()Ljava/lang/String;")){return out;}
                methods[0]++;
                return new MethodVisitor(Opcodes.ASM5,out) {
                    @Override public void visitCode() {
                        super.visitCode();out.visitVarInsn(Opcodes.ALOAD,0);out.visitFieldInsn(Opcodes.GETFIELD,"jy",CACHE,"Ljava/lang/String;");
                        out.visitInsn(Opcodes.DUP);Label miss=new Label();out.visitJumpInsn(Opcodes.IFNULL,miss);out.visitInsn(Opcodes.ARETURN);
                        out.visitLabel(miss);out.visitFrame(Opcodes.F_SAME1,0,null,1,new Object[]{"java/lang/String"});out.visitInsn(Opcodes.POP);
                    }
                    @Override public void visitInsn(int opcode) {
                        if(opcode==Opcodes.ARETURN){returns[0]++;out.visitInsn(Opcodes.DUP);out.visitVarInsn(Opcodes.ALOAD,0);out.visitInsn(Opcodes.SWAP);
                            out.visitFieldInsn(Opcodes.PUTFIELD,"jy",CACHE,"Ljava/lang/String;");}
                        super.visitInsn(opcode);
                    }
                };
            }
            @Override public void visitEnd() {
                writer.visitField(Opcodes.ACC_PRIVATE|Opcodes.ACC_VOLATILE|Opcodes.ACC_SYNTHETIC,CACHE,"Ljava/lang/String;",null,null).visitEnd();super.visitEnd();
            }
        },0);
        if(methods[0]!=1||returns[0]!=1){throw new IllegalStateException("Pinned text method shape changed");}
        return writer.toByteArray();
    }
    private static String hash(byte[] bytes) {
        try{StringBuilder out=new StringBuilder(64);for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes)){
            out.append(Character.forDigit((b>>>4)&15,16)).append(Character.forDigit(b&15,16));}return out.toString();
        }catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
