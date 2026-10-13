package org.polydevs.opusmc.optim.bench;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class JsonSupport {
    static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private JsonSupport() { }
    static JsonObject parse(String input) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(input))) {
            reader.setStrictness(Strictness.STRICT);
            JsonObject result = object(read(reader, 0));
            if (reader.peek() != JsonToken.END_DOCUMENT) { throw new IOException("Trailing JSON value"); }
            return result;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new IOException("Invalid JSON record", ex);
        }
    }
    private static JsonElement read(JsonReader reader, int depth) throws IOException {
        if (depth > 8) { throw new IOException("JSON nesting limit exceeded"); }
        switch (reader.peek()) {
            case BEGIN_OBJECT:
                JsonObject object = new JsonObject();
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (object.has(name)) { throw new IOException("Duplicate JSON key: " + name); }
                    object.add(name, read(reader, depth + 1));
                }
                reader.endObject(); return object;
            case STRING: return new JsonPrimitive(reader.nextString());
            case NUMBER: return new JsonPrimitive(new BigDecimal(reader.nextString()));
            case BOOLEAN: return new JsonPrimitive(reader.nextBoolean());
            case NULL: reader.nextNull(); return JsonNull.INSTANCE;
            default: throw new IOException("Unexpected JSON token");
        }
    }
    static JsonObject object(JsonElement value) {
        if (value == null || !value.isJsonObject()) { throw new IllegalArgumentException("Object required"); }
        return value.getAsJsonObject();
    }
    static void fields(JsonObject value, String... expected) {
        Set<String> keys = new HashSet<>();
        for (java.util.Map.Entry<String, JsonElement> entry : value.entrySet()) { keys.add(entry.getKey()); }
        if (!keys.equals(new HashSet<>(Arrays.asList(expected)))) {
            throw new IllegalArgumentException("Missing or unsupported record members");
        }
    }
    static String text(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("String required: " + key);
        }
        return value.getAsString();
    }
    static long integer(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Integer required: " + key);
        }
        return value.getAsBigDecimal().longValueExact();
    }
    static int smallInteger(JsonObject object, String key) { return Math.toIntExact(integer(object, key)); }
}
