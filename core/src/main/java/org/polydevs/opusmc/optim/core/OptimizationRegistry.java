package org.polydevs.opusmc.optim.core;

import java.util.*;
import org.polydevs.opusmc.optim.api.OptimizationDescriptor;

public final class OptimizationRegistry {
    private final Map<String, OptimizationDescriptor> descriptors;

    public OptimizationRegistry(Collection<OptimizationDescriptor> descriptors) {
        TreeMap<String, OptimizationDescriptor> copy = new TreeMap<>();
        for (OptimizationDescriptor descriptor : descriptors) {
            Objects.requireNonNull(descriptor, "descriptor");
            if (copy.putIfAbsent(descriptor.id(), descriptor) != null) {
                throw new IllegalArgumentException("Duplicate Optim id: " + descriptor.id());
            }
        }
        this.descriptors = Collections.unmodifiableMap(copy);
    }

    public Map<String, OptimizationDescriptor> descriptors() { return descriptors; }
}
