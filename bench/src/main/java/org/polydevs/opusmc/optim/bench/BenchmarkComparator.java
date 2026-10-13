package org.polydevs.opusmc.optim.bench;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Conservative screening over independent whole-run aggregates, never windows or pooled frames. */
public final class BenchmarkComparator {
    public static final int MAX_RUNS_PER_ARM = 1000;
    public static final long MAX_COMPARISON_SAMPLES = 20_000_000;
    private BenchmarkComparator() { }
    public static ComparisonResult compare(List<BenchmarkRun> aaControl, List<BenchmarkRun> aaRepeat,
            List<BenchmarkRun> control, List<BenchmarkRun> treatment, ExperimentEvidence evidence) {
        Set<String> failures = new LinkedHashSet<>();
        Map<String, List<BenchmarkRun>> arms = new LinkedHashMap<>();
        arms.put("aaControl", copy(aaControl, failures)); arms.put("aaRepeat", copy(aaRepeat, failures));
        arms.put("control", copy(control, failures)); arms.put("treatment", copy(treatment, failures));
        Map<String, Integer> counts = new LinkedHashMap<>();
        long samples = 0;
        for (Map.Entry<String, List<BenchmarkRun>> arm : arms.entrySet()) {
            counts.put(arm.getKey(), arm.getValue().size());
            if (arm.getValue().size() < 5) { failures.add("INSUFFICIENT_RUNS"); }
            for (BenchmarkRun run : arm.getValue()) {
                if (run == null) { failures.add("NULL_RUN"); } else { samples += run.sampleCount(); }
            }
        }
        if (samples > MAX_COMPARISON_SAMPLES) { failures.add("COMPARISON_SAMPLE_LIMIT"); }
        if (evidence == null) { failures.add("MISSING_EVIDENCE"); }
        if (!failures.isEmpty()) { return invalid(failures, counts, evidence); }
        BenchmarkRun reference = arms.get("aaControl").get(0);
        BenchmarkVariant baseline = reference.identity().variant(), delta = arms.get("treatment").get(0).identity().variant();
        if (baseline.sourceRevision().equals(delta.sourceRevision()) && baseline.artifactSha256().equals(delta.artifactSha256())
                && baseline.configSha256().equals(delta.configSha256())) { failures.add("NO_VARIANT_DELTA"); }
        Set<String> sessions = new HashSet<>(), ids = new HashSet<>(); boolean synthetic = false;
        for (Map.Entry<String, List<BenchmarkRun>> arm : arms.entrySet()) {
            String variant = arm.getKey().equals("treatment") ? delta.fingerprint() : baseline.fingerprint();
            for (BenchmarkRun run : arm.getValue()) {
                if (!sessions.add(run.identity().sessionId())) { failures.add("REUSED_SESSION"); }
                if (!ids.add(run.identity().runId())) { failures.add("REUSED_RUN_ID"); }
                if (!reference.scope().fingerprint().equals(run.scope().fingerprint())) { failures.add("SCOPE_MISMATCH"); }
                if (!variant.equals(run.identity().variant().fingerprint())) { failures.add("VARIANT_MISMATCH"); }
                if (run.warmupRequested() != reference.warmupRequested()) { failures.add("WARMUP_POLICY_MISMATCH"); }
                if (run.warmupObserved() != run.warmupRequested()) { failures.add("WARMUP_INCOMPLETE"); }
                if (run.droppedSamples() != 0) { failures.add("DROPPED_SAMPLES"); }
                if (run.termination() != BenchmarkRun.Termination.COMPLETE) { failures.add("INTERRUPTED_RUN"); }
                if (run.sampleCount() < 1000) { failures.add("INSUFFICIENT_SAMPLES"); }
                synthetic |= run.identity().origin() == RunIdentity.Origin.SYNTHETIC;
            }
        }
        if (!evidence.scopeFingerprint().equals(reference.scope().fingerprint())) { failures.add("EVIDENCE_SCOPE_MISMATCH"); }
        if (!evidence.controlVariantFingerprint().equals(baseline.fingerprint())
                || !evidence.treatmentVariantFingerprint().equals(delta.fingerprint())) { failures.add("EVIDENCE_VARIANT_MISMATCH"); }
        if (!failures.isEmpty()) { return invalid(failures, counts, evidence); }
        Map<String, double[][]> aggregates = new LinkedHashMap<>();
        arms.forEach((name, runs) -> aggregates.put(name, aggregate(runs)));
        Map<String, ComparisonEstimate> estimates = new LinkedHashMap<>();
        String[] metrics = {"meanFrameNanos", "runP95Nanos", "runP99Nanos"};
        for (int i = 0; i < metrics.length; i++) {
            estimates.put(metrics[i], new ComparisonEstimate(aggregates.get("aaControl")[i], aggregates.get("aaRepeat")[i],
                    aggregates.get("control")[i], aggregates.get("treatment")[i]));
        }
        Set<String> reasons = new LinkedHashSet<>();
        boolean rejected = receipt(evidence.parity(), "PARITY", reasons) | receipt(evidence.mechanism(), "MECHANISM", reasons)
                | receipt(evidence.compatibility(), "COMPATIBILITY", reasons);
        if (synthetic) { reasons.add("SYNTHETIC_DATA"); }
        ComparisonEstimate mean = estimates.get("meanFrameNanos");
        if (mean.lowerPercent() <= mean.noiseFloorPercent()) { reasons.add("MEAN_NOT_BEYOND_NOISE"); }
        if (mean.upperPercent() < -mean.noiseFloorPercent()) { reasons.add("MEAN_REGRESSION"); rejected = true; }
        for (int i = 1; i < metrics.length; i++) {
            ComparisonEstimate tail = estimates.get(metrics[i]); String name = i == 1 ? "P95" : "P99";
            if (tail.upperPercent() < -tail.noiseFloorPercent()) { reasons.add(name + "_REGRESSION"); rejected = true; }
            else if (tail.lowerPercent() < -tail.noiseFloorPercent()) { reasons.add(name + "_UNCERTAIN"); }
        }
        ComparisonResult.Verdict verdict = rejected ? ComparisonResult.Verdict.REJECTED
                : reasons.isEmpty() ? ComparisonResult.Verdict.PERFORMANCE_SIGNAL : ComparisonResult.Verdict.INCONCLUSIVE;
        return new ComparisonResult(verdict, new ArrayList<>(reasons), estimates, counts, evidence);
    }
    private static List<BenchmarkRun> copy(List<BenchmarkRun> runs, Set<String> failures) {
        if (runs == null) { failures.add("MISSING_ARM"); return Collections.emptyList(); }
        if (runs.size() > MAX_RUNS_PER_ARM) { failures.add("RUN_LIMIT"); return Collections.emptyList(); }
        return new ArrayList<>(runs);
    }
    private static boolean receipt(EvidenceReceipt receipt, String name, Set<String> reasons) {
        if (receipt.status() != EvidenceReceipt.Status.PASS) { reasons.add(name + "_" + receipt.status().name()); }
        return receipt.status() == EvidenceReceipt.Status.FAIL;
    }
    private static ComparisonResult invalid(Set<String> reasons, Map<String, Integer> counts, ExperimentEvidence evidence) {
        return new ComparisonResult(ComparisonResult.Verdict.INVALID, new ArrayList<>(reasons), Collections.emptyMap(), counts, evidence);
    }
    private static double[][] aggregate(List<BenchmarkRun> runs) {
        double[][] values = new double[3][runs.size()];
        for (int i = 0; i < runs.size(); i++) {
            FrameStatistics stats = FrameStatistics.of(runs.get(i).frameNanos());
            values[0][i] = stats.meanNanos(); values[1][i] = stats.percentileNanos(.95); values[2][i] = stats.percentileNanos(.99);
        }
        return values;
    }
}
