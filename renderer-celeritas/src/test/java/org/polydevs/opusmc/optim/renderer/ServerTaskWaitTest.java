package org.polydevs.opusmc.optim.renderer;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ServerTaskWaitTest {
    @Test void abandonedTaskOnTerminatedServerIsCancelledWithoutRunningIt() throws Exception {
        Thread server = new Thread(() -> { }); server.start(); server.join();
        FutureTask<Void> abandoned = new FutureTask<>(() -> fail("task must not run on another thread"), null);
        Future<?> prepared = ServerTaskWait.prepare(abandoned, server);
        assertTrue(abandoned.isCancelled()); assertNotSame(abandoned, prepared);
        assertTrue(prepared.isDone()); assertNull(prepared.get());
    }
    @Test void liveServerResultsAndFailuresRetainOriginalFuture() throws Exception {
        FutureTask<String> work = new FutureTask<>(() -> "original result");
        Thread server = new Thread(() -> { try { Thread.sleep(20); } catch (InterruptedException e) { throw new AssertionError(e); } work.run(); });
        server.start(); assertSame(work, ServerTaskWait.prepare(work, server)); assertEquals("original result", work.get()); server.join();
        FutureTask<Void> failed = new FutureTask<>(() -> { throw new IllegalArgumentException("original failure"); }); failed.run();
        assertSame(failed, ServerTaskWait.prepare(failed, server));
        assertInstanceOf(IllegalArgumentException.class, assertThrows(ExecutionException.class, failed::get).getCause());
    }
    @Test void uninterruptibleWaitRestoresCallerInterruptStatus() throws Exception {
        FutureTask<Void> work = new FutureTask<>(() -> null);
        Thread server = new Thread(() -> { try { Thread.sleep(20); } catch (InterruptedException e) { throw new AssertionError(e); } work.run(); });
        server.start(); Thread.currentThread().interrupt();
        try { assertSame(work, ServerTaskWait.prepare(work, server)); assertTrue(Thread.currentThread().isInterrupted()); }
        finally { Thread.interrupted(); server.join(); }
    }
}
