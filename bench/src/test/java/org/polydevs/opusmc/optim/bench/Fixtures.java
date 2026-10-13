package org.polydevs.opusmc.optim.bench;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.polydevs.opusmc.optim.api.Metrics;

final class Fixtures {
    static final String HASH_A = String.join("", Collections.nCopies(64, "a"));
    static final String HASH_B = String.join("", Collections.nCopies(64, "b"));
    static final String REV = String.join("", Collections.nCopies(40, "a"));
    static final BenchmarkVariant CONTROL = new BenchmarkVariant("control", REV, HASH_A, HASH_A);
    static final BenchmarkVariant TREATMENT = new BenchmarkVariant("treatment", REV, HASH_B, HASH_B);
    private Fixtures() { }

    static Map<String, String> scopeFields() {
        Map<String, String> fields = new TreeMap<>();
        for (String key : Arrays.asList("scenario", "fixture", "minecraft", "loader", "client", "java",
                "os", "cpu", "gpu", "gl", "resource-pack", "settings", "instrumentation")) {
            fields.put(key, "fixture-" + key);
        }
        fields.put("sampling", "frame.wall.nanos.v1");
        return fields;
    }
    static BenchmarkScope scope() { return new BenchmarkScope(scopeFields()); }
    static long[] frames(int count, long nanos) {
        long[] values = new long[count];
        Arrays.fill(values, nanos);
        return values;
    }
    // RECORDED is a declared origin used to exercise gates, never a real game receipt.
    static BenchmarkRun run(String id, BenchmarkVariant variant, long[] frames) {
        return new BenchmarkRun(new RunIdentity(id, "session-" + id, variant, RunIdentity.Origin.RECORDED),
                scope(), 2, 2, frames.length, 0, frames, BenchmarkRun.Termination.COMPLETE,
                new Metrics(Collections.emptyMap()));
    }
}
