package org.polydevs.opusmc.bootstrap;

import java.util.List;

/** Provides explicitly approved, managed transformer registrations. */
public interface ClassTransformerProvider {
    String id();

    /** Called once for an approved provider before its startup registrations are read. */
    default void initialize(ClassLoader gameLoader) { }

    List<TransformerRegistration> registrations();
}
