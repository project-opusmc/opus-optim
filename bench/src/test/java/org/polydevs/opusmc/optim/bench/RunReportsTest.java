package org.polydevs.opusmc.optim.bench;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.polydevs.opusmc.optim.api.Metrics;
import static org.junit.jupiter.api.Assertions.*;

class RunReportsTest {
    @TempDir Path directory;
    @Test void roundTripsRawSamplesIdentityAndOnlySuppliedCounters() throws IOException {
        Path file = directory.resolve("run.jsonl");
        BenchmarkRun original = new BenchmarkRun(new RunIdentity("run", "session", Fixtures.CONTROL,
                RunIdentity.Origin.SYNTHETIC), Fixtures.scope(), 2, 2, 2, 0, new long[]{10, 20},
                BenchmarkRun.Termination.COMPLETE, new Metrics(Collections.singletonMap("draw.calls", 7L)));
        RunReports.writeNew(file, original);
        BenchmarkRun loaded = RunReports.read(file);
        assertArrayEquals(new long[]{10, 20}, loaded.frameNanos());
        assertEquals(original.scope().fingerprint(), loaded.scope().fingerprint());
        assertEquals(Fixtures.CONTROL.fingerprint(), loaded.identity().variant().fingerprint());
        assertEquals(RunIdentity.Origin.SYNTHETIC, loaded.identity().origin());
        assertEquals(7, loaded.counters().counter("draw.calls").getAsLong());
        assertFalse(loaded.counters().counter("gpu.nanos").isPresent());
        assertThrows(FileAlreadyExistsException.class, () -> RunReports.writeNew(file, original));
        assertArrayEquals(new long[]{10, 20}, RunReports.read(file).frameNanos());
    }
    @Test void rejectsTruncatedAndTrailingReportsRatherThanReturningPartialSuccess() throws IOException {
        List<String> lines = Files.readAllLines(valid(), StandardCharsets.UTF_8);
        for (List<String> broken : java.util.Arrays.asList(lines.subList(0, lines.size() - 1),
                java.util.Arrays.asList(lines.get(0), lines.get(1), lines.get(2), lines.get(3), lines.get(1)))) {
            Path file = directory.resolve("broken.jsonl");
            Files.write(file, broken, StandardCharsets.UTF_8);
            assertThrows(IOException.class, () -> RunReports.read(file));
        }
    }
    @Test void refusesFutureSchemaCountMismatchDuplicateKeysAndUnknownMembers() throws IOException {
        String good = new String(Files.readAllBytes(valid()), StandardCharsets.UTF_8);
        for (String bad : new String[]{good.replace("\"schemaVersion\":1", "\"schemaVersion\":2"),
                good.replace("\"sampleCount\":2", "\"sampleCount\":3"),
                good.replace("\"type\":\"run\"", "\"type\":\"run\",\"type\":\"run\""),
                good.replace("\"type\":\"frame\"", "\"extra\":true,\"type\":\"frame\""),
                good.replace("\"droppedSamples\":0", "\"droppedSamples\":1"),
                good.replace("\"warmupObserved\":2", "\"warmupObserved\":3")}) {
            Path file = directory.resolve("invalid.jsonl");
            Files.write(file, bad.getBytes(StandardCharsets.UTF_8));
            assertThrows(IOException.class, () -> RunReports.read(file));
        }
    }
    @Test void rejectsNonIntegralNonNumericAndNonPositiveDurations() throws IOException {
        String good = new String(Files.readAllBytes(valid()), StandardCharsets.UTF_8);
        for (String value : new String[]{"0", "-1", "1.5", "9223372036854775808", "\"10\"", "true", "null"}) {
            Path file = directory.resolve("number.jsonl");
            Files.write(file, good.replace("\"nanos\":10", "\"nanos\":" + value).getBytes(StandardCharsets.UTF_8));
            assertThrows(IOException.class, () -> RunReports.read(file), value);
        }
    }
    @Test void boundsFileAndRecordSizesBeforeParsing() throws IOException {
        Path file = directory.resolve("large.jsonl");
        try (RandomAccessFile sparse = new RandomAccessFile(file.toFile(), "rw")) {
            sparse.setLength(64L * 1024 * 1024 + 1);
        }
        assertThrows(IOException.class, () -> RunReports.read(file));
        Files.write(file, Collections.singletonList(String.join("", Collections.nCopies(16_385, "x"))),
                StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> RunReports.read(file));
    }
    @Test void rejectsEscapedLoneSurrogatesInScopeAndVariantIdentities() throws IOException {
        String good = new String(Files.readAllBytes(valid()), StandardCharsets.UTF_8);
        for (String bad : new String[]{good.replace("fixture-cpu", "\\ud800"), good.replace(Fixtures.REV, "\\udc00")}) {
            Path file = directory.resolve("unicode.jsonl");
            Files.write(file, bad.getBytes(StandardCharsets.UTF_8));
            assertThrows(IOException.class, () -> RunReports.read(file));
        }
    }
    private Path valid() throws IOException {
        Path file = directory.resolve("valid.jsonl");
        RunReports.writeNew(file, Fixtures.run("run", Fixtures.CONTROL, new long[]{10, 20}));
        return file;
    }
}
