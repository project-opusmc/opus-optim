package org.polydevs.opusmc.optim.bench;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.polydevs.opusmc.optim.api.Identifiers;

/** All identities invariant across an experiment, including explicit absent/unprobed values. */
public final class BenchmarkScope {
    private final Map<String, String> fields;
    private final String fingerprint;
    public BenchmarkScope(Map<String, String> fields) {
        if (fields == null || fields.size() > 32) { throw new IllegalArgumentException("Invalid scope size"); }
        TreeMap<String, String> copy = new TreeMap<>();
        fields.forEach((key, value) -> copy.put(Identifiers.requireValid(key), BenchValues.text(value)));
        if (!copy.keySet().containsAll(Arrays.asList("scenario", "fixture", "minecraft", "loader", "client",
                "java", "os", "cpu", "gpu", "gl", "resource-pack", "settings", "instrumentation", "sampling"))) {
            throw new IllegalArgumentException("Incomplete benchmark scope");
        }
        if (!"frame.wall.nanos.v1".equals(copy.get("sampling"))) {
            throw new IllegalArgumentException("Unsupported sampling semantics");
        }
        this.fields = Collections.unmodifiableMap(copy);
        this.fingerprint = BenchValues.fingerprint(copy);
    }
    public Map<String, String> fields() { return fields; }
    public String fingerprint() { return fingerprint; }
}
