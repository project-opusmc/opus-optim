package org.polydevs.opusmc.optim.api;

import java.util.*;

/** Environment identity supplied by a platform. This value object performs no capability probes. */
public final class RuntimeDescriptor {
    private final String loader;
    private final String minecraftVersion;
    private final String javaVersion;
    private final String optiFine;
    private final Map<String, String> artifacts;
    private final boolean glProbed;
    private final CapabilitySet capabilities;

    public RuntimeDescriptor(String loader, String minecraftVersion, String javaVersion, String optiFine,
            Map<String, String> artifacts, boolean glProbed, CapabilitySet capabilities) {
        this.loader = Identifiers.requireText(loader, "loader");
        this.minecraftVersion = Identifiers.requireText(minecraftVersion, "minecraftVersion");
        this.javaVersion = Identifiers.requireText(javaVersion, "javaVersion");
        this.optiFine = Identifiers.requireText(optiFine, "optiFine (use absent explicitly)");
        TreeMap<String, String> copy = new TreeMap<>();
        artifacts.forEach((key, value) -> copy.put(Identifiers.requireValid(key), Identifiers.requireText(value, "artifact identity")));
        this.artifacts = Collections.unmodifiableMap(copy);
        this.glProbed = glProbed;
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        if (!glProbed && capabilities.values().stream().anyMatch(value -> value.startsWith("gl."))) {
            throw new IllegalArgumentException("GL capabilities require an explicit completed platform probe");
        }
    }

    public String loader() { return loader; }
    public String minecraftVersion() { return minecraftVersion; }
    public String javaVersion() { return javaVersion; }
    public String optiFine() { return optiFine; }
    public Map<String, String> artifacts() { return artifacts; }
    public boolean glProbed() { return glProbed; }
    public CapabilitySet capabilities() { return capabilities; }
}
