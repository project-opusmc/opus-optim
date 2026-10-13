package org.polydevs.opusmc.optim.bench;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.polydevs.opusmc.optim.api.Identifiers;

/** Declared evidence bound to the exact experiment, never a registry promotion. */
public final class ExperimentEvidence {
    private final String featureId, scopeFingerprint, controlVariantFingerprint, treatmentVariantFingerprint;
    private final EvidenceReceipt parity, mechanism, compatibility;
    public ExperimentEvidence(String featureId, String scopeFingerprint, String controlVariantFingerprint,
            String treatmentVariantFingerprint, EvidenceReceipt parity, EvidenceReceipt mechanism, EvidenceReceipt compatibility) {
        this.featureId = Identifiers.requireValid(featureId); this.scopeFingerprint = BenchValues.hash(scopeFingerprint);
        this.controlVariantFingerprint = BenchValues.hash(controlVariantFingerprint);
        this.treatmentVariantFingerprint = BenchValues.hash(treatmentVariantFingerprint);
        this.parity = Objects.requireNonNull(parity, "parity"); this.mechanism = Objects.requireNonNull(mechanism, "mechanism");
        this.compatibility = Objects.requireNonNull(compatibility, "compatibility");
    }
    public String featureId() { return featureId; }
    public String scopeFingerprint() { return scopeFingerprint; }
    public String controlVariantFingerprint() { return controlVariantFingerprint; }
    public String treatmentVariantFingerprint() { return treatmentVariantFingerprint; }
    public EvidenceReceipt parity() { return parity; }
    public EvidenceReceipt mechanism() { return mechanism; }
    public EvidenceReceipt compatibility() { return compatibility; }
    public static ExperimentEvidence read(Path path) throws IOException {
        if (Files.size(path) > RunReports.MAX_RECORD) { throw new IOException("Evidence size limit exceeded"); }
        try (InputStream input = Files.newInputStream(path)) { return read(input); }
    }
    static ExperimentEvidence read(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096]; int count;
        while ((count = input.read(chunk, 0, Math.min(chunk.length, RunReports.MAX_RECORD + 1 - bytes.size()))) != -1) {
            if (bytes.size() + count > RunReports.MAX_RECORD) { throw new IOException("Evidence size limit exceeded"); }
            bytes.write(chunk, 0, count);
        }
        try {
            String encoded = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
            JsonObject object = JsonSupport.parse(encoded);
            JsonSupport.fields(object, "schemaVersion", "featureId", "scopeFingerprint", "controlVariantFingerprint",
                    "treatmentVariantFingerprint", "parity", "mechanism", "compatibility");
            if (JsonSupport.integer(object, "schemaVersion") != 1) { throw new IOException("Unsupported evidence schema"); }
            return new ExperimentEvidence(JsonSupport.text(object, "featureId"), JsonSupport.text(object, "scopeFingerprint"),
                    JsonSupport.text(object, "controlVariantFingerprint"), JsonSupport.text(object, "treatmentVariantFingerprint"),
                    receipt(object, "parity"), receipt(object, "mechanism"), receipt(object, "compatibility"));
        } catch (IllegalArgumentException | ArithmeticException ex) { throw new IOException("Invalid evidence", ex); }
    }
    private static EvidenceReceipt receipt(JsonObject parent, String key) {
        JsonObject object = JsonSupport.object(parent.get(key));
        JsonSupport.fields(object, "status", "sha256");
        EvidenceReceipt.Status status = EvidenceReceipt.Status.valueOf(JsonSupport.text(object, "status"));
        String hash = object.get("sha256").isJsonNull() ? null : JsonSupport.text(object, "sha256");
        return new EvidenceReceipt(status, hash);
    }
}
