package org.polydevs.opusmc.bootstrap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Immutable managed transformer registration and its exact raw target classes. */
public final class TransformerRegistration {
    private final String providerId;
    private final ClassTransformer transformer;
    private final Set<String> targetClasses;

    public TransformerRegistration(
            String providerId,
            ClassTransformer transformer,
            Set<String> targetClasses) {
        if (providerId == null || providerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transformer provider id cannot be empty");
        }
        if (transformer == null) {
            throw new IllegalArgumentException("Transformer registration cannot contain null transformer");
        }
        if (targetClasses == null || targetClasses.isEmpty()) {
            throw new IllegalArgumentException(
                    "Transformer registration must declare at least one target class");
        }
        Set<String> normalized = new HashSet<String>();
        for (String targetClass : targetClasses) {
            if (targetClass == null || targetClass.trim().isEmpty()) {
                throw new IllegalArgumentException("Transformer target class cannot be empty");
            }
            normalized.add(normalize(targetClass));
        }
        this.providerId = providerId;
        this.transformer = transformer;
        this.targetClasses = Collections.unmodifiableSet(normalized);
    }

    public String providerId() {
        return providerId;
    }

    public ClassTransformer transformer() {
        return transformer;
    }

    public Set<String> targetClasses() {
        return targetClasses;
    }

    static String normalize(String className) {
        return className.replace('/', '.');
    }
}
