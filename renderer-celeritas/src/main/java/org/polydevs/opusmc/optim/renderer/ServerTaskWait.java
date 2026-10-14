package org.polydevs.opusmc.optim.renderer;

import java.util.Objects;
import java.util.concurrent.*;

/** Avoids waiting forever for work abandoned by a terminated integrated server. */
final class ServerTaskWait {
    private ServerTaskWait() { }
    static Future<?> prepare(Future<?> future, Thread server) {
        Objects.requireNonNull(future, "future");
        boolean interrupted = false;
        try {
            while (!future.isDone()) {
                try { future.get(50, TimeUnit.MILLISECONDS); }
                catch (InterruptedException error) { interrupted = true; }
                catch (ExecutionException | CancellationException error) { return future; }
                catch (TimeoutException error) {
                    if (server != null && server.getState() == Thread.State.TERMINATED) {
                        if (!future.cancel(false)) return future;
                        FutureTask<Void> completed = new FutureTask<>(() -> null);
                        completed.run();
                        return completed;
                    }
                }
            }
            return future;
        } finally { if (interrupted) Thread.currentThread().interrupt(); }
    }
}
