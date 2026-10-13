package org.polydevs.opusmc.optim.bench;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import org.polydevs.opusmc.optim.api.Metrics;

/** Strict bounded local JSONL. Incomplete writes remain invalid reports, never partial successes. */
public final class RunReports {
    static final int MAX_RECORD = 16_384;
    static final long MAX_BYTES = 64L * 1024 * 1024;
    private RunReports() { }
    public static void writeNew(Path path, BenchmarkRun run) throws IOException {
        JsonObject header = new JsonObject();
        header.addProperty("type", "run"); header.addProperty("schemaVersion", 1);
        header.addProperty("runId", run.identity().runId()); header.addProperty("sessionId", run.identity().sessionId());
        header.addProperty("origin", run.identity().origin().name());
        BenchmarkVariant variant = run.identity().variant();
        JsonObject delta = new JsonObject();
        delta.addProperty("id", variant.id()); delta.addProperty("sourceRevision", variant.sourceRevision());
        delta.addProperty("artifactSha256", variant.artifactSha256()); delta.addProperty("configSha256", variant.configSha256());
        header.add("variant", delta); header.add("scope", JsonSupport.GSON.toJsonTree(run.scope().fields()));
        header.addProperty("warmupRequested", run.warmupRequested()); header.addProperty("warmupObserved", run.warmupObserved());
        JsonObject end = new JsonObject();
        end.addProperty("type", "end"); end.addProperty("observedSamples", run.observedSamples());
        end.addProperty("droppedSamples", run.droppedSamples()); end.addProperty("sampleCount", run.sampleCount());
        end.addProperty("termination", run.termination().name());
        end.add("counters", JsonSupport.GSON.toJsonTree(run.counters().counters()));
        String first = record(header), last = record(end);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            writer.write(first); writer.newLine();
            for (long nanos : run.frameNanos()) {
                writer.write("{\"type\":\"frame\",\"nanos\":" + nanos + "}"); writer.newLine();
            }
            writer.write(last); writer.newLine();
        }
    }
    private static String record(JsonObject value) throws IOException {
        String encoded = JsonSupport.GSON.toJson(value);
        if (encoded.length() > MAX_RECORD) { throw new IOException("Record limit exceeded"); }
        return encoded;
    }
    public static BenchmarkRun read(Path path) throws IOException {
        if (Files.size(path) > MAX_BYTES) { throw new IOException("Report limit exceeded"); }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject header = JsonSupport.parse(requiredLine(reader));
            JsonSupport.fields(header, "type", "schemaVersion", "runId", "sessionId", "origin", "variant",
                    "scope", "warmupRequested", "warmupObserved");
            if (!"run".equals(JsonSupport.text(header, "type")) || JsonSupport.integer(header, "schemaVersion") != 1) {
                throw new IOException("Unsupported report header/version");
            }
            JsonObject delta = JsonSupport.object(header.get("variant"));
            JsonSupport.fields(delta, "id", "sourceRevision", "artifactSha256", "configSha256");
            BenchmarkVariant variant = new BenchmarkVariant(JsonSupport.text(delta, "id"),
                    JsonSupport.text(delta, "sourceRevision"), JsonSupport.text(delta, "artifactSha256"),
                    JsonSupport.text(delta, "configSha256"));
            RunIdentity identity = new RunIdentity(JsonSupport.text(header, "runId"), JsonSupport.text(header, "sessionId"),
                    variant, RunIdentity.Origin.valueOf(JsonSupport.text(header, "origin")));
            JsonObject scopeObject = JsonSupport.object(header.get("scope"));
            Map<String, String> scopeFields = new TreeMap<>();
            for (Map.Entry<String, JsonElement> entry : scopeObject.entrySet()) {
                scopeFields.put(entry.getKey(), JsonSupport.text(scopeObject, entry.getKey()));
            }
            BenchmarkScope scope = new BenchmarkScope(scopeFields);
            long[] samples = new long[1024]; int count = 0;
            long consumed = 0;
            while (true) {
                String line = requiredLine(reader); consumed += line.length() + 1;
                if (consumed > MAX_BYTES) { throw new IOException("Report limit exceeded while reading"); }
                JsonObject row = JsonSupport.parse(line);
                String type = JsonSupport.text(row, "type");
                if ("frame".equals(type)) {
                    JsonSupport.fields(row, "type", "nanos");
                    if (count == BenchmarkRun.MAX_SAMPLES) { throw new IOException("Sample limit exceeded"); }
                    if (count == samples.length) { samples = Arrays.copyOf(samples, Math.min(samples.length * 2, BenchmarkRun.MAX_SAMPLES)); }
                    samples[count++] = JsonSupport.integer(row, "nanos");
                } else if ("end".equals(type)) {
                    JsonSupport.fields(row, "type", "observedSamples", "droppedSamples", "sampleCount", "termination", "counters");
                    if (JsonSupport.integer(row, "sampleCount") != count || boundedLine(reader) != null) {
                        throw new IOException("Count mismatch or trailing report records");
                    }
                    JsonObject counterObject = JsonSupport.object(row.get("counters"));
                    Map<String, Long> counters = new TreeMap<>();
                    for (Map.Entry<String, JsonElement> entry : counterObject.entrySet()) {
                        counters.put(entry.getKey(), JsonSupport.integer(counterObject, entry.getKey()));
                    }
                    return new BenchmarkRun(identity, scope, JsonSupport.smallInteger(header, "warmupRequested"),
                            JsonSupport.smallInteger(header, "warmupObserved"), JsonSupport.integer(row, "observedSamples"),
                            JsonSupport.integer(row, "droppedSamples"), Arrays.copyOf(samples, count),
                            BenchmarkRun.Termination.valueOf(JsonSupport.text(row, "termination")), new Metrics(counters));
                } else { throw new IOException("Unexpected report record: " + type); }
            }
        } catch (IllegalArgumentException | ArithmeticException ex) {
            throw new IOException("Invalid benchmark run: " + ex.getMessage(), ex);
        }
    }
    static String requiredLine(BufferedReader reader) throws IOException {
        String line = boundedLine(reader);
        if (line == null || line.isEmpty()) { throw new IOException("Truncated/empty report record"); }
        return line;
    }
    static String boundedLine(BufferedReader reader) throws IOException {
        StringBuilder line = new StringBuilder();
        int next;
        while ((next = reader.read()) != -1) {
            if (next == '\n') { return line.toString(); }
            if (line.length() == MAX_RECORD) { throw new IOException("Record limit exceeded"); }
            line.append((char) next);
        }
        return line.length() == 0 ? null : line.toString();
    }
}
