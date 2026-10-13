package org.polydevs.opusmc.optim.bench;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Screening result only. No verdict activates or promotes an optimization. */
public final class ComparisonResult {
    public enum Verdict { INVALID, REJECTED, INCONCLUSIVE, PERFORMANCE_SIGNAL }
    private final Verdict verdict;
    private final List<String> reasons;
    private final Map<String, ComparisonEstimate> estimates;
    private final Map<String, Integer> runCounts;
    private final ExperimentEvidence evidence;
    ComparisonResult(Verdict verdict, List<String> reasons, Map<String, ComparisonEstimate> estimates,
            Map<String, Integer> runCounts, ExperimentEvidence evidence) {
        this.verdict = verdict; this.reasons = Collections.unmodifiableList(new ArrayList<>(reasons));
        this.estimates = Collections.unmodifiableMap(new LinkedHashMap<>(estimates));
        this.runCounts = Collections.unmodifiableMap(new LinkedHashMap<>(runCounts)); this.evidence = evidence;
    }
    public Verdict verdict() { return verdict; }
    public List<String> reasons() { return reasons; }
    public Map<String, ComparisonEstimate> estimates() { return estimates; }
    public Map<String, Integer> runCounts() { return runCounts; }
    JsonObject toJson() {
        JsonObject result = new JsonObject(); result.addProperty("schemaVersion", 1);
        result.addProperty("verdict", verdict.name()); result.addProperty("method", "independent-run-t95-min-df30-v1");
        result.add("reasons", JsonSupport.GSON.toJsonTree(reasons)); result.add("runCounts", JsonSupport.GSON.toJsonTree(runCounts));
        if (evidence != null) {
            result.addProperty("featureId", evidence.featureId()); result.addProperty("scopeFingerprint", evidence.scopeFingerprint());
            result.addProperty("controlVariantFingerprint", evidence.controlVariantFingerprint());
            result.addProperty("treatmentVariantFingerprint", evidence.treatmentVariantFingerprint());
            JsonObject receipts = new JsonObject();
            addReceipt(receipts, "parity", evidence.parity()); addReceipt(receipts, "mechanism", evidence.mechanism());
            addReceipt(receipts, "compatibility", evidence.compatibility()); result.add("evidence", receipts);
        }
        JsonObject data = new JsonObject();
        estimates.forEach((key, value) -> {
            JsonObject estimate = new JsonObject(); estimate.addProperty("controlMeanNanos", value.controlMeanNanos());
            estimate.addProperty("treatmentMeanNanos", value.treatmentMeanNanos()); estimate.addProperty("effectPercent", value.effectPercent());
            estimate.addProperty("lowerPercent", value.lowerPercent()); estimate.addProperty("upperPercent", value.upperPercent());
            estimate.addProperty("aaNoiseFloorPercent", value.noiseFloorPercent()); data.add(key, estimate);
        });
        result.add("estimates", data); return result;
    }
    private static void addReceipt(JsonObject parent, String name, EvidenceReceipt receipt) {
        JsonObject value = new JsonObject(); value.addProperty("status", receipt.status().name());
        value.addProperty("sha256", receipt.sha256()); parent.add(name, value);
    }
}
