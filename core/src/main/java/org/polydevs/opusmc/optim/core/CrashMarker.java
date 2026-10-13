package org.polydevs.opusmc.optim.core;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.polydevs.opusmc.optim.api.Identifiers;

/** A launch marker, not a crash attribution system. An incomplete run disables experiments. */
public final class CrashMarker {
    private final Path path;

    public CrashMarker(Path path) { this.path = Objects.requireNonNull(path).toAbsolutePath().normalize(); }

    public synchronized Recovery inspect() {
        try {
            JsonObject document = readDocument();
            return new Recovery(false, true, "UNCLEAN_TERMINATION", JsonFiles.stringValue(document.get("runId")));
        } catch (NoSuchFileException absent) {
            return Recovery.clean();
        } catch (IOException | RuntimeException failure) {
            return new Recovery(true, true, "INVALID_CRASH_MARKER: " + failure.getClass().getSimpleName(), "");
        }
    }

    /** Call after reading recovery and planning, before entering the launched runtime. */
    public synchronized void begin(String runId, Collection<String> experimentalIds) throws IOException {
        JsonObject document = new JsonObject();
        document.addProperty("schemaVersion", 1);
        document.addProperty("runId", Identifiers.requireValid(runId));
        JsonArray experiments = new JsonArray();
        TreeSet<String> ids = new TreeSet<>();
        for (String id : experimentalIds) { ids.add(Identifiers.requireValid(id)); }
        for (String id : ids) { experiments.add(id); }
        document.add("experimentalIds", experiments);
        JsonFiles.writeAtomic(path, document);
    }

    /** The platform must establish its documented clean-termination criterion before calling. */
    public synchronized void markCleanShutdown(String runId) throws IOException {
        Identifiers.requireValid(runId);
        JsonObject document = readDocument();
        if (!runId.equals(JsonFiles.stringValue(document.get("runId")))) {
            throw new IllegalStateException("Cannot clear a different Optim launch marker");
        }
        Files.delete(path);
    }

    private JsonObject readDocument() throws IOException {
        JsonObject document = JsonFiles.parse(JsonFiles.read(path));
        JsonFiles.requireSchema(document);
        Identifiers.requireValid(JsonFiles.stringValue(document.get("runId")));
        JsonElement experiments = document.get("experimentalIds");
        if (experiments == null || !experiments.isJsonArray()) {
            throw new IllegalArgumentException("Crash marker needs experimentalIds");
        }
        for (JsonElement id : experiments.getAsJsonArray()) { Identifiers.requireValid(JsonFiles.stringValue(id)); }
        return document;
    }

    public static final class Recovery {
        private final boolean safe;
        private final boolean disableExperimental;
        private final String reason;
        private final String runId;

        private Recovery(boolean safe, boolean disableExperimental, String reason, String runId) {
            this.safe = safe; this.disableExperimental = disableExperimental; this.reason = reason; this.runId = runId;
        }
        public static Recovery clean() { return new Recovery(false, false, "", ""); }
        public boolean isSafe() { return safe; }
        public boolean disableExperimental() { return disableExperimental; }
        public String reason() { return reason; }
        public Optional<String> previousRunId() { return runId.isEmpty() ? Optional.empty() : Optional.of(runId); }
    }
}
