package org.polydevs.opusmc.optim.renderer;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ServerTaskWaitTest {
    @Test void failureBetweenTimeoutAndOwnerTerminationRetainsOriginalFuture() throws Exception {
        assertCompletionRace(false);
    }
    @Test void cancellationBetweenTimeoutAndOwnerTerminationRetainsOriginalFuture() throws Exception {
        assertCompletionRace(true);
    }
    private static void assertCompletionRace(boolean cancelled) throws Exception {
        AtomicReference<FutureTask<Void>> pending = new AtomicReference<>();
        CountDownLatch timedOut = new CountDownLatch(1);
        Thread server = new Thread(() -> {
            try { timedOut.await(); }
            catch (InterruptedException error) { throw new AssertionError(error); }
            if (cancelled) pending.get().cancel(false);
            else pending.get().run();
        });
        FutureTask<Void> work = new FutureTask<Void>(() -> { throw new IllegalArgumentException("raced failure"); }) {
            @Override public Void get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                try { return super.get(1, TimeUnit.MILLISECONDS); }
                catch (TimeoutException expired) {
                    timedOut.countDown();
                    server.join();
                    throw expired;
                }
            }
        };
        pending.set(work);
        server.start();
        Future<?> prepared = ServerTaskWait.prepare(work, server);
        assertSame(work, prepared, "a lost cancellation race must preserve the original future");
        if (cancelled) assertThrows(CancellationException.class, prepared::get);
        else assertEquals("raced failure", assertThrows(ExecutionException.class, prepared::get).getCause().getMessage());
    }
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
