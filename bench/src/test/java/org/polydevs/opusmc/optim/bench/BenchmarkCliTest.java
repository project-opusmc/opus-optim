package org.polydevs.opusmc.optim.bench;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkCliTest {
    @TempDir Path directory;
    @Test void statsUsesTheWholeRawRunAndDoesNotInventUnsupportedLows() throws IOException {
        Path report = directory.resolve("run.jsonl");
        RunReports.writeNew(report, Fixtures.run("run", Fixtures.CONTROL, new long[]{10_000_000, 20_000_000}));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int code = BenchmarkCli.run(new String[]{"stats", report.toString()},
                new PrintStream(output), new PrintStream(new ByteArrayOutputStream()));
        assertEquals(0, code);
        JsonObject json = JsonParser.parseString(output.toString("UTF-8")).getAsJsonObject();
        assertEquals(15_000_000, json.get("meanFrameNanos").getAsDouble());
        assertEquals(20_000_000, json.get("p99Nanos").getAsLong());
        assertTrue(json.get("onePercentLowFps").isJsonNull());
        assertTrue(json.get("pointOnePercentLowFps").isJsonNull());
    }
    @Test void invalidInputProducesAnErrorExitAndNoSuccessJson() {
        ByteArrayOutputStream out = new ByteArrayOutputStream(), err = new ByteArrayOutputStream();
        assertEquals(2, BenchmarkCli.run(new String[]{"stats", directory.resolve("missing").toString()},
                new PrintStream(out), new PrintStream(err)));
        assertEquals(0, out.size()); assertTrue(err.size() > 0);
    }
    @Test void compareLoadsFourActualRunDirectoriesAndBoundEvidence() throws IOException {
        String[] args = comparisonFiles();
        Path evidence = java.nio.file.Paths.get(args[5]);
        Files.write(evidence, evidenceJson("PASS").getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertEquals(0, BenchmarkCli.run(args, new PrintStream(output), new PrintStream(new ByteArrayOutputStream())));
        JsonObject result = JsonParser.parseString(output.toString("UTF-8")).getAsJsonObject();
        assertEquals("PERFORMANCE_SIGNAL", result.get("verdict").getAsString());
        assertEquals(20, result.getAsJsonObject("estimates").getAsJsonObject("meanFrameNanos").get("effectPercent").getAsDouble());
        Files.write(evidence, evidenceJson("MISSING").getBytes(StandardCharsets.UTF_8));
        assertEquals(3, BenchmarkCli.run(args, new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream())));
        Files.write(evidence, evidenceJson("FAIL").getBytes(StandardCharsets.UTF_8));
        assertEquals(4, BenchmarkCli.run(args, new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream())));
    }
    @Test void evidenceSchemaAndReceiptReferencesFailClosed() throws IOException {
        Path file = directory.resolve("evidence.json");
        String valid = evidenceJson("PASS");
        Files.write(file, valid.getBytes(StandardCharsets.UTF_8));
        assertEquals(EvidenceReceipt.Status.PASS, ExperimentEvidence.read(file).parity().status());
        for (String invalid : new String[]{valid.replace("\"schemaVersion\":1", "\"schemaVersion\":2"),
                valid.replace("\"schemaVersion\":1", "\"schemaVersion\":\"1\""),
                valid.replace("\"status\":\"PASS\"", "\"status\":\"PASS\",\"status\":\"PASS\""),
                valid.replace(Fixtures.HASH_A, "unknown"), valid + "{}"}) {
            Files.write(file, invalid.getBytes(StandardCharsets.UTF_8));
            assertThrows(IOException.class, () -> ExperimentEvidence.read(file));
        }
    }
    @Test void evidenceByteLimitIsEnforcedOnTheStreamRatherThanOnlyFileMetadata() throws IOException {
        byte[] content = evidenceJson("PASS").getBytes(StandardCharsets.UTF_8);
        byte[] limit = new byte[16_384], overflow = new byte[16_385];
        java.util.Arrays.fill(limit, (byte) ' '); java.util.Arrays.fill(overflow, (byte) ' ');
        System.arraycopy(content, 0, limit, 0, content.length); System.arraycopy(content, 0, overflow, 0, content.length);
        assertEquals(EvidenceReceipt.Status.PASS, ExperimentEvidence.read(new ByteArrayInputStream(limit)).parity().status());
        assertThrows(IOException.class, () -> ExperimentEvidence.read(new ByteArrayInputStream(overflow)));
    }
    private String[] comparisonFiles() throws IOException {
        String[] args = new String[6]; args[0] = "compare";
        String[] arms = {"aa1", "aa2", "a", "b"};
        for (int i = 0; i < arms.length; i++) {
            Path folder = Files.createDirectory(directory.resolve(arms[i])); args[i + 1] = folder.toString();
            List<BenchmarkRun> runs = BenchmarkComparatorTest.arm(arms[i], i == 3 ? Fixtures.TREATMENT : Fixtures.CONTROL, i == 3 ? 8 : 10);
            for (BenchmarkRun run : runs) { RunReports.writeNew(folder.resolve(run.identity().runId() + ".jsonl"), run); }
        }
        args[5] = directory.resolve("evidence.json").toString(); return args;
    }
    private String evidenceJson(String parityStatus) {
        String receipt = "{\"status\":\"PASS\",\"sha256\":\"" + Fixtures.HASH_A + "\"}";
        String parity = "{\"status\":\"" + parityStatus + "\",\"sha256\":"
                + (parityStatus.equals("MISSING") ? "null" : "\"" + Fixtures.HASH_A + "\"") + "}";
        return "{\"schemaVersion\":1,\"featureId\":\"candidate\",\"scopeFingerprint\":\"" + Fixtures.scope().fingerprint()
                + "\",\"controlVariantFingerprint\":\"" + Fixtures.CONTROL.fingerprint()
                + "\",\"treatmentVariantFingerprint\":\"" + Fixtures.TREATMENT.fingerprint()
                + "\",\"parity\":" + parity + ",\"mechanism\":" + receipt + ",\"compatibility\":" + receipt + "}";
    }
}
