package org.polydevs.opusmc.optim.smoke;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.polydevs.opusmc.optim.api.*;
import org.polydevs.opusmc.optim.core.*;
import org.polydevs.opusmc.optim.platform.*;
import org.polydevs.opusmc.optim.platform.mc189.*;

/** Local vendor-bytecode verification and query microbenchmarks; never game/FPS evidence. */
public final class FrustumVerify {
    public interface BoxQuery {
        boolean test(float[][] planes, boolean disabled, double x0, double y0, double z0, double x1, double y1, double z1);
        boolean fully(float[][] planes, boolean disabled, double x0, double y0, double z0, double x1, double y1, double z1);
    }
    private static volatile long blackhole;
    private FrustumVerify() { }

    public static void main(String[] args) throws Exception {
        require(System.getProperty("java.version").startsWith("1.8."), "Use an actual Java 8 VM");
        Path minecraft = Paths.get(args[1]), optifine = Paths.get(args[2]);
        byte[] original = extract(minecraft, optifine);
        require(FrustumPatch.matchesTarget(original), "Local M5 target differs from supported pin");
        FrustumPatch patch = new FrustumPatch(); byte[] optimized = patch.transform("bid", original);
        require(patch.applicationState() == FrustumPatch.ApplicationState.APPLIED, "Patch not applied");
        JsonObject report = new JsonObject();
        report.addProperty("schemaVersion", 1); report.addProperty("scope", "LOCAL_QUERY_MICROBENCHMARK_NOT_GAME");
        report.addProperty("sessionId", UUID.randomUUID().toString());
        report.addProperty("java", System.getProperty("java.version")); report.addProperty("vm", System.getProperty("java.vm.name"));
        report.addProperty("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        report.addProperty("arch", System.getProperty("os.arch")); report.addProperty("targetSha256", sha(original));
        report.addProperty("minecraftJarSha256", sha(Files.readAllBytes(minecraft)));
        report.addProperty("optifineJarSha256", sha(Files.readAllBytes(optifine)));
        report.addProperty("helperSha256", sha(read(FrustumMath.class.getResourceAsStream("FrustumMath.class"))));
        if (args[0].equals("verify")) {
            FrustumOptimization.Candidate candidate = FrustumOptimization.candidate();
            require(candidate.applicationState() == FrustumPatch.ApplicationState.NOT_SEEN, "Factory status must start unseen");
            ProviderPlan selected = ProviderPlan.prepare(
                    new OptimConfig(true, Collections.singletonMap(FrustumOptimization.ID, true)),
                    new CapabilitySet(Arrays.asList(FrustumOptimization.TARGET_CAPABILITY, FrustumOptimization.ASM_CAPABILITY)),
                    CrashMarker.Recovery.clean(), Collections.singleton(candidate.binding()), Collections.emptyList(),
                    FrustumVerify.class.getClassLoader());
            byte[] viaFactory = selected.registrations().get(0).transformer().transform("bid", original);
            require(candidate.applicationState() == FrustumPatch.ApplicationState.APPLIED, "Factory status did not observe application");
            require(Arrays.equals(optimized, viaFactory), "Factory binding changed transformed bytes");
            report.addProperty("cases", parity(original, optimized)); report.addProperty("mismatches", 0);
            byte[] vanilla;
            try (ZipFile base = new ZipFile(minecraft.toFile())) { vanilla = read(base.getInputStream(base.getEntry("bid.class"))); }
            FrustumPatch rejected = new FrustumPatch();
            require(rejected.transform("bid", vanilla) == vanilla, "Vanilla pin was accepted");
            require(rejected.transform("bid", optimized) == optimized, "Already modified pin was accepted");
            require(rejected.applicationState() == FrustumPatch.ApplicationState.UNSUPPORTED_INPUT, "Missing rejection state");
            require(selected.registrations().get(0).transformer().transform("bid", optimized) == optimized,
                    "Factory binding accepted already modified input");
            require(candidate.applicationState() == FrustumPatch.ApplicationState.UNSUPPORTED_INPUT,
                    "Factory status did not observe rejection");
            report.addProperty("unknownPinsRetained", true); report.addProperty("disabledAndFullyVerified", true);
            report.addProperty("factoryStatusVerified", true);
            report.addProperty("transformedTargetSha256", sha(optimized));
        } else if (args[0].equals("trial")) {
            require(args[3].equals("control") || args[3].equals("treatment"), "Invalid arm");
            String workload = args[4]; double[] boxes = boxes(workload);
            ByteBuffer fixture = ByteBuffer.allocate(boxes.length * 8);
            for (double value : boxes) { fixture.putDouble(value); }
            report.addProperty("arm", args[3]); report.addProperty("workload", workload);
            report.addProperty("fixtureSha256", sha(fixture.array()));
            BoxQuery query = adapter(args[3].equals("control") ? original : optimized);
            float[][] planes = cameraPlanes();
            for (int warmup = 0; warmup < 5; warmup++) { blackhole = run(query, planes, boxes, 512); }
            JsonArray elapsed = new JsonArray(); JsonArray checksums = new JsonArray();
            for (int sample = 0; sample < 10; sample++) {
                long start = System.nanoTime(); long hits = run(query, planes, boxes, 256);
                long nanos = System.nanoTime() - start; blackhole = hits;
                require(nanos > 0, "Invalid clock sample"); elapsed.add(nanos); checksums.add(hits);
            }
            report.addProperty("queriesPerSample", 4096 * 256);
            report.addProperty("warmupQueries", 4096L * 512 * 5);
            report.add("sampleNanos", elapsed); report.add("visibleChecksums", checksums);
            report.addProperty("blackhole", blackhole);
        } else { throw new IllegalArgumentException("Expected verify or trial"); }
        System.out.println(new Gson().toJson(report));
    }

    private static long parity(byte[] original, byte[] optimized) throws Exception {
        BoxQuery base = adapter(original), changed = adapter(optimized); long cases = 0;
        Random random = new Random(20261013);
        for (int i = 0; i < 120_000; i++) {
            float[][] planes = new float[6][4];
            for (float[] p : planes) {
                for (int j = 0; j < 4; j++) {
                    p[j] = i < 100_000 ? random.nextFloat() * 2 - 1 : Float.intBitsToFloat(random.nextInt());
                }
            }
            double[] bounds = new double[6];
            for (int j = 0; j < 6; j++) {
                bounds[j] = i < 100_000 ? random.nextDouble() * 6e7 - 3e7 : Float.intBitsToFloat(random.nextInt());
            }
            if (i < 100_000) {
                for (int j = 0; j < 3; j++) {
                    double low = Math.min(bounds[j], bounds[j + 3]), high = Math.max(bounds[j], bounds[j + 3]);
                    bounds[j] = low; bounds[j + 3] = high;
                }
            }
            check(base, changed, planes, false, bounds); cases++;
        }
        double[] edges = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.MAX_VALUE, -Double.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, 0, -0d, 1, 16_777_217};
        float[] coefficients = {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                Float.MAX_VALUE, -Float.MAX_VALUE, 0, -0f, 1, -1, Float.MIN_VALUE};
        for (double lo : edges) {
            for (double hi : edges) {
                for (float a : coefficients) {
                    float[][] p = cameraPlanes(); p[0] = new float[]{a, -a, a, -a};
                    check(base, changed, p, false, new double[]{lo, lo, lo, hi, hi, hi}); cases++;
                }
            }
        }
        check(base, changed, null, true, new double[]{Double.NaN, 0, 0, 1, 1, 1}); cases++;
        return cases;
    }
    private static void check(BoxQuery base, BoxQuery changed, float[][] p, boolean disabled, double[] b) {
        require(base.test(p, disabled, b[0], b[1], b[2], b[3], b[4], b[5])
                == changed.test(p, disabled, b[0], b[1], b[2], b[3], b[4], b[5]), "Frustum parity mismatch");
        require(base.fully(p, disabled, b[0], b[1], b[2], b[3], b[4], b[5])
                == changed.fully(p, disabled, b[0], b[1], b[2], b[3], b[4], b[5]), "Full predicate changed");
    }
    private static long run(BoxQuery query, float[][] p, double[] b, int repeats) {
        long hits = 0;
        for (int repeat = 0; repeat < repeats; repeat++) {
            for (int i = 0; i < b.length; i += 6) {
                if (query.test(p, false, b[i], b[i + 1], b[i + 2], b[i + 3], b[i + 4], b[i + 5])) { hits++; }
            }
        }
        return hits;
    }
    private static double[] boxes(String scenario) {
        require(Arrays.asList("inside", "mixed-chunks", "outside").contains(scenario), "Unknown workload");
        Random r = new Random(20261013); double[] b = new double[4096 * 6];
        for (int i = 0; i < b.length; i += 6) {
            double x = scenario.equals("inside") ? r.nextDouble() * 16 - 8 : r.nextDouble() * 512 - 256;
            double y = scenario.equals("inside") ? r.nextDouble() * 16 - 8 : r.nextDouble() * 128 - 64;
            double z = scenario.equals("inside") ? -64 : -(r.nextDouble() * 256 + 1);
            if (scenario.equals("outside")) { x += 1024; }
            double size = scenario.equals("inside") ? 1 : 16;
            b[i] = x; b[i + 1] = y; b[i + 2] = z; b[i + 3] = x + size; b[i + 4] = y + size; b[i + 5] = z + size;
        }
        return b;
    }
    private static float[][] cameraPlanes() {
        return new float[][]{{1, 0, -1, 0}, {-1, 0, -1, 0}, {0, 1, -1, 0}, {0, -1, -1, 0},
                {0, 0, -1, -0.1f}, {0, 0, 1, 128}};
    }
    private static BoxQuery adapter(byte[] target) throws Exception {
        class GameLoader extends ClassLoader {
            GameLoader() { super(FrustumVerify.class.getClassLoader()); }
            Class<?> define(String name, byte[] bytes) { return defineClass(name, bytes, 0, bytes.length); }
        }
        GameLoader game = new GameLoader(); game.define("bid", target);
        ClassWriter w = new ClassWriter(0);
        w.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "O2FrustumAdapter", null, "bid",
                new String[]{Type.getInternalName(BoxQuery.class)});
        MethodVisitor ctor = w.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.visitCode(); ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "bid", "<init>", "()V", false);
        ctor.visitInsn(Opcodes.RETURN); ctor.visitMaxs(1, 1); ctor.visitEnd();
        for (String method : Arrays.asList("test", "fully")) {
            MethodVisitor out = w.visitMethod(Opcodes.ACC_PUBLIC, method, "([[FZDDDDDD)Z", null, null);
            out.visitCode(); out.visitVarInsn(Opcodes.ALOAD, 0); out.visitVarInsn(Opcodes.ALOAD, 1);
            out.visitFieldInsn(Opcodes.PUTFIELD, "bid", "a", "[[F");
            out.visitVarInsn(Opcodes.ALOAD, 0); out.visitVarInsn(Opcodes.ILOAD, 2);
            out.visitFieldInsn(Opcodes.PUTFIELD, "bid", "disabled", "Z");
            out.visitVarInsn(Opcodes.ALOAD, 0);
            for (int slot = 3; slot <= 13; slot += 2) { out.visitVarInsn(Opcodes.DLOAD, slot); }
            out.visitMethodInsn(Opcodes.INVOKESPECIAL, "bid", method.equals("test") ? "b" : "isBoxInFrustumFully", "(DDDDDD)Z", false);
            out.visitInsn(Opcodes.IRETURN); out.visitMaxs(13, 15); out.visitEnd();
        }
        w.visitEnd(); return (BoxQuery) game.define("O2FrustumAdapter", w.toByteArray()).getConstructor().newInstance();
    }
    private static byte[] extract(Path minecraft, Path optifine) throws Exception {
        try (ZipFile base = new ZipFile(minecraft.toFile()); ZipFile opti = new ZipFile(optifine.toFile());
                URLClassLoader tools = new URLClassLoader(new URL[]{optifine.toUri().toURL()}, FrustumVerify.class.getClassLoader())) {
            Class<?> patcher = Class.forName("optifine.Patcher", true, tools);
            Object map = patcher.getMethod("getConfigurationMap", ZipFile.class).invoke(null, opti);
            Object patterns = patcher.getMethod("getConfigurationPatterns", Map.class).invoke(null, map);
            Object provider = Class.forName("optifine.ZipResourceProvider", true, tools).getConstructor(ZipFile.class).newInstance(base);
            Class<?> resource = Class.forName("optifine.IResourceProvider", true, tools);
            byte[] delta = read(opti.getInputStream(opti.getEntry("patch/bid.class.xdelta")));
            return (byte[]) patcher.getMethod("applyPatch", String.class, byte[].class, Pattern[].class, Map.class, resource)
                    .invoke(null, "bid.class", delta, patterns, map, provider);
        }
    }
    private static byte[] read(InputStream source) throws IOException {
        if (source == null) { throw new IOException("Missing target input"); }
        try (InputStream in = source; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] block = new byte[4096]; int n;
            while ((n = in.read(block)) != -1) { out.write(block, 0, n); } return out.toByteArray();
        }
    }
    private static String sha(byte[] bytes) throws NoSuchAlgorithmException {
        StringBuilder hex = new StringBuilder(64);
        for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) {
            hex.append(Character.forDigit((b >>> 4) & 15, 16)).append(Character.forDigit(b & 15, 16));
        }
        return hex.toString();
    }
    private static void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
