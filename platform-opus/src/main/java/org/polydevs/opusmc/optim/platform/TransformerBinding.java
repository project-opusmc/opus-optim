package org.polydevs.opusmc.optim.platform;

import java.util.*;
import org.polydevs.opusmc.bootstrap.ClassTransformer;
import org.polydevs.opusmc.optim.api.*;

/** Trusted startup code plus exact host-supplied claims; not proof of bytecode parity. */
public final class TransformerBinding {
    final OptimizationDescriptor descriptor;
    final ClassTransformer transformer;
    final Set<String> targets;
    private final List<Class<?>> helpers;

    public TransformerBinding(OptimizationDescriptor descriptor, ClassTransformer delegate,
            Collection<Class<?>> injectedHelpers) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(delegate, "transformer");
        if (descriptor.activation() != ActivationKind.STARTUP_PATCH || descriptor.patchClaims().isEmpty()) {
            throw new IllegalArgumentException("Binding needs a STARTUP_PATCH with exact claims: " + descriptor.id());
        }
        String id = delegate.id();
        int priority = delegate.priority();
        if (!descriptor.id().equals(id)) {
            throw new IllegalArgumentException("Transformer id differs from descriptor: " + descriptor.id());
        }
        TreeSet<String> declared = new TreeSet<>();
        for (PatchClaim claim : descriptor.patchClaims()) { declared.add(claim.targetClass()); }
        targets = Collections.unmodifiableSet(declared);
        ArrayList<Class<?>> copy = new ArrayList<>();
        for (Class<?> helper : injectedHelpers) { copy.add(Objects.requireNonNull(helper, "helper")); }
        helpers = Collections.unmodifiableList(copy);
        transformer = new ClassTransformer() {
            @Override public String id() { return id; }
            @Override public int priority() { return priority; }
            @Override public byte[] transform(String className, byte[] original) throws Exception {
                if (className == null || !targets.contains(className.replace('/', '.'))) { return original; }
                return delegate.transform(className, original);
            }
        };
    }

    void verifyHelpers(ClassLoader gameLoader) {
        for (Class<?> helper : helpers) { verifyHelper(helper, gameLoader); }
    }

    static void verifyHelper(Class<?> helper, ClassLoader gameLoader) {
        Objects.requireNonNull(gameLoader, "gameLoader");
        try {
            if (Class.forName(helper.getName(), false, gameLoader) != helper) {
                throw new IllegalArgumentException("Different injected helper class identity: " + helper.getName());
            }
        } catch (ClassNotFoundException | LinkageError failure) {
            throw new IllegalArgumentException("Injected helper is not visible: " + helper.getName(), failure);
        }
    }
}
