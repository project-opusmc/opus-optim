package org.polydevs.opusmc.bootstrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

/** Immutable transformer chain plus exact target routing for managed providers. */
public final class TransformerPlan {
    private final TransformerChain chain;
    private final Set<String> targetClasses;

    private TransformerPlan(TransformerChain chain, Set<String> targetClasses) {
        this.chain = chain;
        this.targetClasses = Collections.unmodifiableSet(new HashSet<String>(targetClasses));
    }

    /**
     * Discovers only providers visible through the supplied loader and enabled by
     * the managed approval set. Built-ins are always included in the resulting chain.
     */
    public static TransformerPlan discover(
            ClassLoader providerLoader,
            Set<String> approvedProviderIds,
            List<TransformerRegistration> builtIns) {
        if (providerLoader == null) {
            throw new IllegalArgumentException("Provider class loader cannot be null");
        }
        if (approvedProviderIds == null) {
            throw new IllegalArgumentException("Approved provider ids cannot be null");
        }
        if (builtIns == null) {
            throw new IllegalArgumentException("Built-in registrations cannot be null");
        }

        Set<String> approvals = new HashSet<String>();
        for (String id : approvedProviderIds) {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Approved provider id cannot be empty");
            }
            approvals.add(id);
        }

        List<ClassTransformer> transformers = new ArrayList<ClassTransformer>();
        for (TransformerRegistration registration : builtIns) {
            if (registration == null) {
                throw new IllegalArgumentException("Built-in registration cannot be null");
            }
            transformers.add(registration.transformer());
        }

        Set<String> targets = new HashSet<String>();
        for (TransformerRegistration registration : builtIns) {
            if (registration == null) {
                throw new IllegalArgumentException("Built-in registration cannot be null");
            }
            targets.addAll(registration.targetClasses());
        }
        Set<String> providerIds = new HashSet<String>();
        ServiceLoader<ClassTransformerProvider> providers = ServiceLoader.load(
                ClassTransformerProvider.class, providerLoader);
        try {
            for (ClassTransformerProvider provider : providers) {
                if (provider == null) {
                    throw new IllegalArgumentException("Transformer provider cannot be null");
                }
                String providerId = provider.id();
                if (providerId == null || providerId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Transformer provider id cannot be empty");
                }
                if (!approvals.contains(providerId)) {
                    continue;
                }
                if (!providerIds.add(providerId)) {
                    throw new IllegalArgumentException("Duplicate transformer provider id: " + providerId);
                }
                List<TransformerRegistration> registrations = provider.registrations();
                if (registrations == null || registrations.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Approved transformer provider has no registrations: " + providerId);
                }
                for (TransformerRegistration registration : registrations) {
                    if (registration == null) {
                        throw new IllegalArgumentException(
                                "Provider " + providerId + " returned a null registration");
                    }
                    if (!providerId.equals(registration.providerId())) {
                        throw new IllegalArgumentException(
                                "Registration provider id does not match provider " + providerId);
                    }
                    transformers.add(registration.transformer());
                    targets.addAll(registration.targetClasses());
                }
            }
        } catch (ServiceConfigurationError failure) {
            throw new IllegalArgumentException("Could not discover managed transformer providers", failure);
        }
        return new TransformerPlan(new TransformerChain(transformers), targets);
    }

    public TransformerChain chain() {
        return chain;
    }

    /** Returns whether the exact raw class name is declared by a managed provider. */
    public boolean targets(String rawClassName) {
        return rawClassName != null
                && targetClasses.contains(TransformerRegistration.normalize(rawClassName));
    }

    /** Returns the immutable union of exact managed target classes. */
    public Set<String> targetClasses() {
        return targetClasses;
    }

    public int transformerCount() {
        return chain.size();
    }
}
