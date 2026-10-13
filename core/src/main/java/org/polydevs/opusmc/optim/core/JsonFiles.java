package org.polydevs.opusmc.optim.core;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

final class JsonFiles {
    private static final int MAX_BYTES = 1024 * 1024;
    private static final Gson WRITER = new GsonBuilder().setPrettyPrinting().create();

    private JsonFiles() { }

    static byte[] read(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path); ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (result.size() + count > MAX_BYTES) { throw new IOException("Optim JSON exceeds 1 MiB"); }
                result.write(buffer, 0, count);
            }
            return result.toByteArray();
        }
    }

    static JsonObject parse(byte[] bytes) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(new String(bytes, StandardCharsets.UTF_8)))) {
            reader.setStrictness(Strictness.STRICT);
            JsonElement value = JsonParser.parseReader(reader);
            if (!value.isJsonObject() || reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IllegalArgumentException("Optim JSON must be one object");
            }
            return value.getAsJsonObject();
        }
    }

    static void requireSchema(JsonObject document) {
        JsonElement schema = document.get("schemaVersion");
        if (schema == null || !schema.isJsonPrimitive() || !schema.getAsJsonPrimitive().isNumber()
                || schema.getAsBigDecimal().compareTo(java.math.BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("Unsupported or missing Optim schemaVersion");
        }
    }

    static boolean booleanValue(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("Optim flag must be a boolean");
        }
        return value.getAsBoolean();
    }

    static String stringValue(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Optim identity must be a string");
        }
        return value.getAsString();
    }

    /** No non-atomic fallback: a failed write must retain the existing document. */
    static void writeAtomic(Path path, JsonObject document) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "opus-optim-", ".tmp");
        try {
            Files.write(temporary, (WRITER.toJson(document) + "\n").getBytes(StandardCharsets.UTF_8));
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
