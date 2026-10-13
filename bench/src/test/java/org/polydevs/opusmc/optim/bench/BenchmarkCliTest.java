package org.polydevs.opusmc.optim.bench;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkCliTest {
    @TempDir Path directory;
    @Test void statsUsesTheWholeRawRunAndDoesNotInventUnsupportedLows() throws IOException {
        Path report = directory.resolve("run.jsonl");
        RunReports.writeNew(report, Fixtures.run("run", Fixtures.CONTROL, new long[]{10_000_000, 20_000_000}));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int code = BenchmarkCli.run(new String[]{"stats", report.toString()},
                new PrintStream(output), new PrintStream(new ByteArrayOutputStream()));
        assertEquals(0, code);
        JsonObject json = JsonParser.parseString(output.toString("UTF-8")).getAsJsonObject();
        assertEquals(15_000_000, json.get("meanFrameNanos").getAsDouble());
        assertEquals(20_000_000, json.get("p99Nanos").getAsLong());
        assertTrue(json.get("onePercentLowFps").isJsonNull());
        assertTrue(json.get("pointOnePercentLowFps").isJsonNull());
    }
    @Test void invalidInputProducesAnErrorExitAndNoSuccessJson() {
        ByteArrayOutputStream out = new ByteArrayOutputStream(), err = new ByteArrayOutputStream();
        assertEquals(2, BenchmarkCli.run(new String[]{"stats", directory.resolve("missing").toString()},
                new PrintStream(out), new PrintStream(err)));
        assertEquals(0, out.size()); assertTrue(err.size() > 0);
    }
}
