package edu.labs.semaphore;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Theory: knowledge/03-concurrency/semaphore.md
// Exercise: exercises/concurrency/semaphore-limited-service/README.md
// Testing concepts: JUnit, Mockito, ExecutorService, CountDownLatch, interruption.
@ExtendWith(MockitoExtension.class)
class LimitedServiceTest {
    @Mock Gateway gateway;

    @Test
    void validatesArguments() {
        assertThrows(IllegalArgumentException.class, () -> new LimitedService(0, gateway));
        assertThrows(IllegalArgumentException.class, () -> new LimitedService(-1, gateway));
        assertThrows(NullPointerException.class, () -> new LimitedService(1, null));
    }

    @Test
    void delegatesAndReturnsResult() throws Exception {
        when(gateway.execute()).thenReturn("67");
        assertEquals("67", new LimitedService(2, gateway).call());
        verify(gateway).execute();
    }

    @Test
    void releasesPermitWhenGatewayThrows() throws Exception {
        when(gateway.execute()).thenThrow(new IllegalStateException("failed")).thenReturn("ok");
        LimitedService service = new LimitedService(1, gateway);
        assertThrows(IllegalStateException.class, service::call);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> secondCall = executor.submit(service::call);
            assertEquals("ok", secondCall.get(2, TimeUnit.SECONDS),
                    "Second call blocks: the first call probably leaked its permit");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void neverRunsMoreThanTwoGatewayCallsConcurrently() throws Exception {
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        CountDownLatch twoEntered = new CountDownLatch(2);
        CountDownLatch thirdEntered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch ready = new CountDownLatch(3);
        CountDownLatch start = new CountDownLatch(1);

        when(gateway.execute()).thenAnswer(invocation -> {
            int now = active.incrementAndGet();
            maximum.accumulateAndGet(now, Math::max);
            if (now == 3) thirdEntered.countDown();
            twoEntered.countDown();
            try {
                if (!release.await(3, TimeUnit.SECONDS)) throw new AssertionError("Gateway was never released");
                return "ok";
            } finally {
                active.decrementAndGet();
            }
        });

        LimitedService service = new LimitedService(2, gateway);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            @SuppressWarnings("unchecked")
            Future<String>[] tasks = new Future[3];
            for (int i = 0; i < 3; i++) {
                tasks[i] = executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return service.call();
                });
            }
            assertTrue(ready.await(2, TimeUnit.SECONDS), "Workers did not start");
            start.countDown();
            assertTrue(twoEntered.await(2, TimeUnit.SECONDS), "Two calls should enter immediately");
            assertFalse(thirdEntered.await(250, TimeUnit.MILLISECONDS),
                    "Third call entered while two permits are occupied");
            release.countDown();
            for (Future<String> task : tasks) assertEquals("ok", task.get(3, TimeUnit.SECONDS));
            assertTrue(maximum.get() <= 2, "Concurrency limit was exceeded: " + maximum.get());
            verify(gateway, times(3)).execute();
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void interruptionOfWaitingCallerDoesNotConsumePermit() throws Exception {
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        when(gateway.execute()).thenAnswer(invocation -> {
            firstEntered.countDown();
            if (!release.await(3, TimeUnit.SECONDS)) {
                throw new AssertionError("First gateway call was not released");
            }
            return "ok";
        });

        LimitedService service = new LimitedService(1, gateway);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicReference<Throwable> waitingFailure = new AtomicReference<>();
        CountDownLatch waitingStarted = new CountDownLatch(1);

        Thread waiting = new Thread(() -> {
            waitingStarted.countDown();
            try {
                service.call();
                waitingFailure.set(new AssertionError("Interrupted call unexpectedly succeeded"));
            } catch (InterruptedException expected) {
                // Correct: no permit was acquired, so no release is needed.
            } catch (Throwable unexpected) {
                waitingFailure.set(unexpected);
            }
        }, "semaphore-interrupted-waiter");

        try {
            Future<String> first = executor.submit(service::call);
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS), "First call did not occupy the permit");
            waiting.start();
            assertTrue(waitingStarted.await(2, TimeUnit.SECONDS));

            // Wait until the second thread actually blocks inside acquire().
            // A latch that only signals entry into the lambda would not prove this.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (waiting.getState() != Thread.State.WAITING
                    && waiting.isAlive() && System.nanoTime() < deadline) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
            }
            assertEquals(Thread.State.WAITING, waiting.getState(),
                    "Second call did not block while the permit was held");

            waiting.interrupt();
            waiting.join(2_000);
            assertFalse(waiting.isAlive(), "Interrupted waiter failed to terminate");
            assertNull(waitingFailure.get(), "Interrupted caller did not propagate InterruptedException");

            // Still holding the first permit: a leaked/extra release would let
            // another gateway.execute() slip through.
            verify(gateway, times(1)).execute();

            release.countDown();
            assertEquals("ok", first.get(3, TimeUnit.SECONDS));
        } finally {
            waiting.interrupt();
            waiting.join(2_000);
            release.countDown();
            executor.shutdownNow();
        }
    }
}
