package org.polydevs.opusmc.optim.core;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ConfigStoreTest {
    @TempDir Path directory;

    private Path configPath() { return directory.resolve("optim.json"); }
    private void write(String json) throws Exception {
        Files.write(configPath(), json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void absentConfigIsOffAndDoesNotCreateAFile() throws Exception {
        ConfigStore.Snapshot snapshot = new ConfigStore(configPath()).load();
        assertFalse(snapshot.config().enabled());
        assertFalse(snapshot.config().isSafe());
        assertFalse(Files.exists(configPath()));
    }

    @Test
    void scopedUpdatesPreserveUnknownFieldsAndUnknownFeatureFlags() throws Exception {
        write("{\"schemaVersion\":1,\"enabled\":true,\"future\":{\"mode\":\"keep\"},"
                + "\"features\":{\"future-feature\":true,\"text\":false}}");
        ConfigStore store = new ConfigStore(configPath());
        store.updateFeature("text", true);
        store.updateEnabled(false);
        String json = new String(Files.readAllBytes(configPath()), StandardCharsets.UTF_8);
        assertEquals("keep", JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("future").get("mode").getAsString());
        assertTrue(store.load().config().features().get("future-feature"));
        assertTrue(store.load().config().features().get("text"));
        assertFalse(store.load().config().enabled());
    }

    @Test
    void malformedConfigRecoversToSafeWithoutOverwritingTheFile() throws Exception {
        String corrupt = "{broken";
        write(corrupt);
        ConfigStore store = new ConfigStore(configPath());
        assertTrue(store.load().config().isSafe());
        assertFalse(store.load().config().enabled());
        assertFalse(store.load().reason().isEmpty());
        assertThrows(IllegalStateException.class, () -> store.updateFeature("text", true));
        assertEquals(corrupt, new String(Files.readAllBytes(configPath()), StandardCharsets.UTF_8));
    }

    @Test
    void staleSchemaAndWrongTypedFlagsStaySafeAndUntouched() throws Exception {
        for (String json : Arrays.asList("{\"schemaVersion\":2,\"enabled\":true}",
                "{\"schemaVersion\":1,\"enabled\":\"true\"}",
                "{\"schemaVersion\":1,\"features\":{\"text\":1}}", "null", "{} true")) {
            write(json);
            ConfigStore store = new ConfigStore(configPath());
            assertTrue(store.load().config().isSafe(), json);
            assertThrows(IllegalStateException.class, () -> store.updateEnabled(true));
            assertEquals(json, new String(Files.readAllBytes(configPath()), StandardCharsets.UTF_8));
        }
    }

    @Test
    void partialConfigNeverEnablesOptimAndScopedUpdatesReloadCurrentData() throws Exception {
        write("{\"schemaVersion\":1,\"features\":{\"text\":true}}");
        ConfigStore store = new ConfigStore(configPath());
        assertFalse(store.load().config().enabled());
        write("{\"schemaVersion\":1,\"enabled\":false,\"new-field\":42}");
        store.updateFeature("text", true);
        assertEquals(42, JsonParser.parseString(new String(Files.readAllBytes(configPath()), StandardCharsets.UTF_8))
                .getAsJsonObject().get("new-field").getAsInt());
        assertThrows(UnsupportedOperationException.class, () -> store.load().config().features().clear());
    }
}
