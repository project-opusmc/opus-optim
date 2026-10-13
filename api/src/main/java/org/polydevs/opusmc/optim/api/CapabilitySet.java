package org.polydevs.opusmc.optim.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/** Known capabilities only. Absence means unavailable or unprobed, never inferred support. */
public final class CapabilitySet {
    private final Set<String> values;

    public CapabilitySet(Collection<String> capabilities) {
        TreeSet<String> copy = new TreeSet<>();
        for (String capability : capabilities) { copy.add(Identifiers.requireValid(capability)); }
        values = Collections.unmodifiableSet(copy);
    }

    public static CapabilitySet empty() { return new CapabilitySet(Collections.emptySet()); }
    public boolean contains(String capability) { return values.contains(capability); }
    public Set<String> values() { return values; }
}
