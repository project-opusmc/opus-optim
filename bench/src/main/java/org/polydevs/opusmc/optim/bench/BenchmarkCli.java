package org.polydevs.opusmc.optim.bench;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.OptionalDouble;

/** Local offline report inspection; never uploads or modifies a report. */
public final class BenchmarkCli {
    private BenchmarkCli() { }
    public static void main(String[] args) { System.exit(run(args, System.out, System.err)); }
    public static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            if (args.length != 2 || !"stats".equals(args[0])) {
                throw new IllegalArgumentException("Usage: stats RUN.jsonl");
            }
            BenchmarkRun run = RunReports.read(Paths.get(args[1]));
            FrameStatistics stats = FrameStatistics.of(run.frameNanos());
            JsonObject result = new JsonObject();
            result.addProperty("runId", run.identity().runId()); result.addProperty("sessionId", run.identity().sessionId());
            result.addProperty("origin", run.identity().origin().name()); result.addProperty("termination", run.termination().name());
            result.addProperty("scopeFingerprint", run.scope().fingerprint());
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
    private static void optional(JsonObject object, String key, OptionalDouble value) {
        object.addProperty(key, value.isPresent() ? value.getAsDouble() : null);
    }
}
