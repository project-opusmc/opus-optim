package org.polydevs.opusmc.optim.probe;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ProbeTaskDrainTest {
    @Test void drainsPacketsInOrderWithoutHoldingProducerMonitor() throws Exception {
        Queue<FutureTask<?>> queue = new ArrayDeque<>();
        StringBuilder applied = new StringBuilder();
        CountDownLatch enqueued = new CountDownLatch(1);
        FutureTask<Void> first = new FutureTask<>(() -> {
            assertFalse(Thread.holdsLock(queue)); applied.append('A');
            Thread producer = new Thread(() -> {
                synchronized (queue) { queue.add(new FutureTask<Void>(() -> applied.append('B'), null)); }
                enqueued.countDown();
            });
            producer.setDaemon(true); producer.start();
            assertTrue(enqueued.await(2, TimeUnit.SECONDS)); return null;
        });
        queue.add(first);
        assertEquals(2, GameProbeAgent.drainTasks(queue));
        assertEquals("AB", applied.toString()); assertTrue(queue.isEmpty());
    }
}
