package org.polydevs.opusmc.bootstrap;

import java.util.List;

/** Provides explicitly approved, managed transformer registrations. */
public interface ClassTransformerProvider {
    String id();

    List<TransformerRegistration> registrations();
}
