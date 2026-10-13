package org.polydevs.opusmc.optim.bench;

import java.util.Map;
import java.util.TreeMap;
import org.polydevs.opusmc.optim.api.Identifiers;

/** Optim-specific experimental delta; base client/loader identities belong to scope. */
public final class BenchmarkVariant {
    private final String id, sourceRevision, artifactSha256, configSha256, fingerprint;
    public BenchmarkVariant(String id, String sourceRevision, String artifactSha256, String configSha256) {
        this.id = Identifiers.requireValid(id);
        this.sourceRevision = BenchValues.text(sourceRevision);
        this.artifactSha256 = "absent".equals(artifactSha256) ? artifactSha256 : BenchValues.hash(artifactSha256);
        this.configSha256 = BenchValues.hash(configSha256);
        Map<String, String> fields = new TreeMap<>();
        fields.put("id", this.id); fields.put("source", this.sourceRevision);
        fields.put("artifact", this.artifactSha256); fields.put("config", this.configSha256);
        this.fingerprint = BenchValues.fingerprint(fields);
    }
    public String id() { return id; }
    public String sourceRevision() { return sourceRevision; }
    public String artifactSha256() { return artifactSha256; }
    public String configSha256() { return configSha256; }
    public String fingerprint() { return fingerprint; }
}
