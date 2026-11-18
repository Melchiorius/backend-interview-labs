package labs.concurrency;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SynchronizedCounterTest {

    @Test
    void preservesAllIncrementsFromConcurrentTasks() throws Exception {
        int workers = 8;
        int incrementsPerWorker = 10_000;
        SynchronizedCounter counter = new SynchronizedCounter();

        try (var executor = Executors.newFixedThreadPool(workers)) {
            List<Callable<Void>> tasks = new ArrayList<>();

            for (int worker = 0; worker < workers; worker++) {
                tasks.add(() -> {
                    for (int i = 0; i < incrementsPerWorker; i++) {
                        counter.increment();
                    }
                    return null;
                });
            }

            var results = executor.invokeAll(tasks);
            for (var result : results) {
                result.get();
            }
        }

        assertEquals((long) workers * incrementsPerWorker, counter.get());
    }
}
