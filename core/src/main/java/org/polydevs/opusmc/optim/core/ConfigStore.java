package org.polydevs.opusmc.optim.core;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.polydevs.opusmc.optim.api.Identifiers;

/** Explicit local config path. Loading is read-only; corrupt/future documents cannot be updated. */
public final class ConfigStore {
    private final Path path;

    public ConfigStore(Path path) { this.path = Objects.requireNonNull(path).toAbsolutePath().normalize(); }

    public synchronized Snapshot load() {
        try {
            byte[] bytes = JsonFiles.read(path);
            JsonObject document = JsonFiles.parse(bytes);
            JsonFiles.requireSchema(document);
            boolean enabled = document.has("enabled") && JsonFiles.booleanValue(document.get("enabled"));
            Map<String, Boolean> flags = new TreeMap<>();
            if (document.has("features")) {
                JsonElement features = document.get("features");
                if (!features.isJsonObject()) { throw new IllegalArgumentException("features must be an object"); }
                for (Map.Entry<String, JsonElement> entry : features.getAsJsonObject().entrySet()) {
                    flags.put(Identifiers.requireValid(entry.getKey()), JsonFiles.booleanValue(entry.getValue()));
                }
            }
            return new Snapshot(new OptimConfig(enabled, flags), "", document, bytes);
        } catch (NoSuchFileException absent) {
            JsonObject empty = new JsonObject();
            empty.addProperty("schemaVersion", 1);
            empty.addProperty("enabled", false);
            empty.add("features", new JsonObject());
            return new Snapshot(new OptimConfig(false, Collections.emptyMap()), "MISSING_CONFIG", empty, null);
        } catch (IOException | RuntimeException failure) {
            String reason = "INVALID_CONFIG: " + failure.getClass().getSimpleName();
            return new Snapshot(OptimConfig.safe(reason), reason, null, null);
        }
    }

    public synchronized void updateFeature(String id, boolean enabled) throws IOException {
        Identifiers.requireValid(id);
        Snapshot snapshot = writableSnapshot();
        JsonObject document = snapshot.document.deepCopy();
        if (!document.has("features")) { document.add("features", new JsonObject()); }
        document.getAsJsonObject("features").addProperty(id, enabled);
        save(snapshot, document);
    }

    public synchronized void updateEnabled(boolean enabled) throws IOException {
        Snapshot snapshot = writableSnapshot();
        JsonObject document = snapshot.document.deepCopy();
        document.addProperty("enabled", enabled);
        save(snapshot, document);
    }

    private Snapshot writableSnapshot() {
        Snapshot snapshot = load();
        if (snapshot.config.isSafe()) { throw new IllegalStateException(snapshot.reason); }
        return snapshot;
    }

    private void save(Snapshot snapshot, JsonObject document) throws IOException {
        byte[] current;
        try { current = JsonFiles.read(path); }
        catch (NoSuchFileException absent) { current = null; }
        if (!Arrays.equals(current, snapshot.bytes)) {
            throw new IOException("Optim config changed during scoped update; reload before retrying");
        }
        JsonFiles.writeAtomic(path, document);
    }

    public static final class Snapshot {
        private final OptimConfig config;
        private final String reason;
        private final JsonObject document;
        private final byte[] bytes;

        private Snapshot(OptimConfig config, String reason, JsonObject document, byte[] bytes) {
            this.config = config; this.reason = reason; this.document = document; this.bytes = bytes;
        }
        public OptimConfig config() { return config; }
        public String reason() { return reason; }
    }
}
