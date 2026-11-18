package labs.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoundedBlockingQueueTest {

    @Test
    void preservesFifoOrderAndDoesNotExceedCapacity() throws InterruptedException {
        BoundedBlockingQueue<Integer> queue = new BoundedBlockingQueue<>(2);

        queue.put(67);
        queue.put(7);

        assertEquals(2, queue.size());
        assertEquals(2, queue.capacity());
        assertEquals(67, queue.take().intValue());
        assertEquals(7, queue.take().intValue());
        assertEquals(0, queue.size());
    }

    @Test
    void consumerWaitsWhenQueueIsEmptyAndWakesAfterPut() throws Exception {
        BoundedBlockingQueue<Integer> queue = new BoundedBlockingQueue<>(1);

        try (var executor = Executors.newSingleThreadExecutor()) {
            var consumer = executor.submit(queue::take);

            assertThrows(
                    TimeoutException.class,
                    () -> consumer.get(100, TimeUnit.MILLISECONDS),
                    "take() must wait while the queue is empty"
            );

            queue.put(67);
            assertEquals(67, consumer.get(1, TimeUnit.SECONDS).intValue());
        }
    }

    @Test
    void producerWaitsWhenQueueIsFullAndWakesAfterTake() throws Exception {
        BoundedBlockingQueue<Integer> queue = new BoundedBlockingQueue<>(1);
        queue.put(7);

        try (var executor = Executors.newSingleThreadExecutor()) {
            var producer = executor.submit(() -> queue.put(67));

            assertThrows(
                    TimeoutException.class,
                    () -> producer.get(100, TimeUnit.MILLISECONDS),
                    "put() must wait while the queue is full"
            );

            assertEquals(7, queue.take());
            producer.get(1, TimeUnit.SECONDS);
            assertEquals(67, queue.take());
        }
    }
}
