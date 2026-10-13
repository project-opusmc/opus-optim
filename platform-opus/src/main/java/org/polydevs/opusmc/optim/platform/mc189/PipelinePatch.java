package org.polydevs.opusmc.optim.platform.mc189;

import java.security.*;
import java.util.Objects;
import org.objectweb.asm.*;
import org.polydevs.opusmc.bootstrap.ClassTransformer;
import org.polydevs.opusmc.optim.core.render.*;
import org.polydevs.opusmc.optim.core.workload.DeadlineBudget;

/** One exact input per candidate. Unknown or previously modified bytes retain the host baseline. */
public final class PipelinePatch implements ClassTransformer {
    public enum ApplicationState { NOT_SEEN, APPLIED, UNSUPPORTED_INPUT }
    private static final String TABLE="o2$glyphIndex";
    private final PipelineOptimizations.Kind kind;
    private volatile ApplicationState state=ApplicationState.NOT_SEEN;
    public PipelinePatch(PipelineOptimizations.Kind kind) { this.kind=Objects.requireNonNull(kind, "kind"); }
    @Override public String id() { return kind.id; }
    @Override public int priority() { return kind.priority; }
    public ApplicationState applicationState() { return state; }
    @Override public byte[] transform(String name, byte[] original) {
        if (!kind.target.equals(name)) { return original; }
        if (original==null || !kind.digest.equals(sha256(original))) {
            state=ApplicationState.UNSUPPORTED_INPUT; return original;
        }
        byte[] output=rewrite(original);
        state=ApplicationState.APPLIED;
        return output;
    }
    private byte[] rewrite(byte[] original) {
        ClassReader reader=new ClassReader(original);
        if (!reader.getClassName().equals(kind.target)) { throw new IllegalArgumentException("Target identity differs"); }
        String alphabet=kind==PipelineOptimizations.Kind.GLYPH_LOOKUP ? alphabet(reader) : null;
        ClassWriter writer=new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        int[] edits={0}; boolean[] clinit={false};
        reader.accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                if (kind==PipelineOptimizations.Kind.GLYPH_LOOKUP && name.equals(TABLE)) {
                    throw new IllegalArgumentException("Glyph field already exists");
                }
                return super.visitField(access,name,desc,signature,value);
            }
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor out=super.visitMethod(access,name,desc,signature,exceptions);
                if (kind==PipelineOptimizations.Kind.GLYPH_LOOKUP && name.equals("<clinit>")) {
                    clinit[0]=true;
                    return new MethodVisitor(Opcodes.ASM5,out) {
                        @Override public void visitCode() { super.visitCode(); initialize(out,alphabet); }
                    };
                }
                if (!kind.methods.contains(name+desc)) { return out; }
                return new MethodVisitor(Opcodes.ASM5,out) {
                    private boolean glyphPending;
                    @Override public void visitCode() {
                        super.visitCode();
                        if (kind==PipelineOptimizations.Kind.UPLOAD_BUDGET) {
                            out.visitVarInsn(Opcodes.LLOAD,1);
                            out.visitMethodInsn(Opcodes.INVOKESTATIC,"java/lang/System","nanoTime","()J",false);
                            out.visitLdcInsn(2_000_000L);
                            out.visitMethodInsn(Opcodes.INVOKESTATIC,Type.getInternalName(DeadlineBudget.class),"cap","(JJJ)J",false);
                            out.visitVarInsn(Opcodes.LSTORE,1); edits[0]++;
                        }
                    }
                    @Override public void visitLdcInsn(Object value) {
                        if (alphabet!=null && alphabet.equals(value)) {
                            out.visitFieldInsn(Opcodes.GETSTATIC,kind.target,TABLE,"[S"); glyphPending=true;
                        } else { super.visitLdcInsn(value); }
                    }
                    @Override public void visitMethodInsn(int opcode,String owner,String method,String descriptor,boolean iface) {
                        if (glyphPending && owner.equals("java/lang/String") && method.equals("indexOf") && descriptor.equals("(I)I")) {
                            out.visitMethodInsn(Opcodes.INVOKESTATIC,Type.getInternalName(GlyphIndex.class),"lookup","([SI)I",false);
                            glyphPending=false; edits[0]++;
                        } else { super.visitMethodInsn(opcode,owner,method,descriptor,iface); }
                    }
                    @Override public void visitVarInsn(int opcode,int slot) {
                        if (kind==PipelineOptimizations.Kind.BUFFER_GROWTH && opcode==Opcodes.ISTORE && slot==4) {
                            out.visitVarInsn(Opcodes.ILOAD,2);
                            out.visitMethodInsn(Opcodes.INVOKESTATIC,Type.getInternalName(BufferGrowth.class),"capacity","(II)I",false);
                            edits[0]++;
                        }
                        super.visitVarInsn(opcode,slot);
                    }
                };
            }
            @Override public void visitEnd() {
                if (alphabet!=null) {
                    writer.visitField(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC|Opcodes.ACC_FINAL|Opcodes.ACC_SYNTHETIC,TABLE,"[S",null,null).visitEnd();
                    if (!clinit[0]) {
                        MethodVisitor out=writer.visitMethod(Opcodes.ACC_STATIC,"<clinit>","()V",null,null);
                        out.visitCode(); initialize(out,alphabet); out.visitInsn(Opcodes.RETURN); out.visitMaxs(0,0); out.visitEnd();
                    }
                }
                super.visitEnd();
            }
        },0);
        if (edits[0]!=(alphabet==null ? 1 : 4)) { throw new IllegalStateException("Pinned anchor count changed: "+edits[0]); }
        return writer.toByteArray();
    }
    private void initialize(MethodVisitor out,String alphabet) {
        out.visitLdcInsn(alphabet);
        out.visitMethodInsn(Opcodes.INVOKESTATIC,Type.getInternalName(GlyphIndex.class),"create","(Ljava/lang/String;)[S",false);
        out.visitFieldInsn(Opcodes.PUTSTATIC,kind.target,TABLE,"[S");
    }
    private String alphabet(ClassReader reader) {
        String[] found={null};
        reader.accept(new ClassVisitor(Opcodes.ASM5) {
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions) {
                if (!kind.methods.contains(name+desc)) { return null; }
                return new MethodVisitor(Opcodes.ASM5) {
                    @Override public void visitLdcInsn(Object value) {
                        if (value instanceof String && ((String)value).length()==256) {
                            if (found[0]!=null && !found[0].equals(value)) { throw new IllegalStateException("Different glyph alphabets"); }
                            found[0]=(String)value;
                        }
                    }
                };
            }
        },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        if (found[0]==null) { throw new IllegalStateException("Missing glyph alphabet"); }
        return found[0];
    }
    private static String sha256(byte[] bytes) {
        try {
            StringBuilder hex=new StringBuilder(64);
            for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes)) {
                hex.append(Character.forDigit((b>>>4)&15,16)).append(Character.forDigit(b&15,16));
            }
            return hex.toString();
        } catch(NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
