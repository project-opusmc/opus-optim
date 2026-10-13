package org.polydevs.opusmc.optim.bench;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Local offline report inspection; never uploads or modifies a report. */
public final class BenchmarkCli {
    private BenchmarkCli() { }
    public static void main(String[] args) { System.exit(run(args, System.out, System.err)); }
    public static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            if (args.length == 6 && "compare".equals(args[0])) { return compare(args, out); }
            if (args.length != 2 || !"stats".equals(args[0])) {
                throw new IllegalArgumentException("Usage: stats RUN.jsonl | compare AA_CONTROL_DIR AA_REPEAT_DIR CONTROL_DIR TREATMENT_DIR EVIDENCE.json");
            }
            BenchmarkRun run = RunReports.read(Paths.get(args[1]));
            FrameStatistics stats = FrameStatistics.of(run.frameNanos());
            JsonObject result = new JsonObject();
            result.addProperty("runId", run.identity().runId()); result.addProperty("sessionId", run.identity().sessionId());
            result.addProperty("origin", run.identity().origin().name()); result.addProperty("termination", run.termination().name());
            result.addProperty("scopeFingerprint", run.scope().fingerprint());
            result.addProperty("variantFingerprint", run.identity().variant().fingerprint());
            result.addProperty("sampleCount", run.sampleCount()); result.addProperty("droppedSamples", run.droppedSamples());
            result.addProperty("warmupRequested", run.warmupRequested()); result.addProperty("warmupObserved", run.warmupObserved());
            result.addProperty("meanFrameNanos", stats.meanNanos());
            result.addProperty("p50Nanos", stats.percentileNanos(.5)); result.addProperty("p95Nanos", stats.percentileNanos(.95));
            result.addProperty("p99Nanos", stats.percentileNanos(.99));
            optional(result, "onePercentLowFps", stats.onePercentLowFps());
            optional(result, "pointOnePercentLowFps", stats.pointOnePercentLowFps());
            out.println(JsonSupport.GSON.toJson(result)); return 0;
        } catch (IOException | IllegalArgumentException ex) { err.println(ex.getMessage()); return 2; }
    }
    private static int compare(String[] args, PrintStream out) throws IOException {
        List<List<BenchmarkRun>> arms = new ArrayList<>(); long totalSamples = 0;
        for (int i = 1; i <= 4; i++) {
            List<Path> files;
            try (Stream<Path> paths = Files.list(Paths.get(args[i]))) {
                files = paths.filter(path -> path.getFileName().toString().endsWith(".jsonl") && Files.isRegularFile(path))
                        .limit(BenchmarkComparator.MAX_RUNS_PER_ARM + 1L).sorted().collect(Collectors.toList());
            }
            if (files.size() > BenchmarkComparator.MAX_RUNS_PER_ARM) { throw new IOException("Run count limit exceeded"); }
            List<BenchmarkRun> runs = new ArrayList<>();
            for (Path file : files) {
                BenchmarkRun run = RunReports.read(file); totalSamples += run.sampleCount();
                if (totalSamples > BenchmarkComparator.MAX_COMPARISON_SAMPLES) { throw new IOException("Comparison sample limit exceeded"); }
                runs.add(run);
            }
            arms.add(runs);
        }
        ComparisonResult result = BenchmarkComparator.compare(arms.get(0), arms.get(1), arms.get(2), arms.get(3),
                ExperimentEvidence.read(Paths.get(args[5])));
        out.println(JsonSupport.GSON.toJson(result.toJson()));
        switch (result.verdict()) {
            case PERFORMANCE_SIGNAL: return 0;
            case INCONCLUSIVE: return 3;
            case REJECTED: return 4;
            default: return 2;
        }
    }
    private static void optional(JsonObject object, String key, OptionalDouble value) {
        object.addProperty(key, value.isPresent() ? value.getAsDouble() : null);
    }
}
