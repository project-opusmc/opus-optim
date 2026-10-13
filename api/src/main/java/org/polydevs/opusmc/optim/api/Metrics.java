package org.polydevs.opusmc.optim.api;

import java.util.*;

/** Measured counters only; missing counters remain absent rather than fabricated zeros. */
public final class Metrics {
    private final Map<String, Long> counters;

    public Metrics(Map<String, Long> counters) {
        TreeMap<String, Long> copy = new TreeMap<>();
        counters.forEach((id, value) -> {
            Identifiers.requireValid(id);
            if (value == null || value < 0) { throw new IllegalArgumentException("Invalid counter: " + id); }
            copy.put(id, value);
        });
        this.counters = Collections.unmodifiableMap(copy);
    }

    public Map<String, Long> counters() { return counters; }
    public OptionalLong counter(String id) {
        Long value = counters.get(id);
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }
}
