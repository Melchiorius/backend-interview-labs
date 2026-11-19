package edu.labs.semaphore;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
            if (!release.await(3, TimeUnit.SECONDS)) throw new AssertionError("Blocked gateway");
            return "ok";
        });

        LimitedService service = new LimitedService(1, gateway);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(service::call);
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
            CountDownLatch secondStarted = new CountDownLatch(1);
            Future<String> waiting = executor.submit(() -> {
                secondStarted.countDown();
                return service.call();
            });
            assertTrue(secondStarted.await(2, TimeUnit.SECONDS));
            waiting.cancel(true); // Interrupt a caller waiting for its permit.
            release.countDown();
            assertEquals("ok", first.get(3, TimeUnit.SECONDS));
            assertTrue(waiting.isCancelled());
            // The second call must never enter gateway while the permit was occupied.
            verify(gateway, times(1)).execute();
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }
}
