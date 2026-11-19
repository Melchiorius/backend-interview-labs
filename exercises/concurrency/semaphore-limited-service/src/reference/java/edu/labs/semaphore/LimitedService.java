package edu.labs.semaphore;

import java.util.Objects;
import java.util.concurrent.Semaphore;

// Theory: knowledge/03-concurrency/semaphore.md
// Exercise: exercises/concurrency/semaphore-limited-service/README.md
public final class LimitedService {
    private final Semaphore permits;
    private final Gateway gateway;

    public LimitedService(int maxConcurrent, Gateway gateway) {
        if (maxConcurrent <= 0) {
            throw new IllegalArgumentException("maxConcurrent must be positive");
        }
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.permits = new Semaphore(maxConcurrent);
    }

    public String call() throws Exception {
        permits.acquire(); // If interrupted here, no permit has been acquired.
        try {
            return gateway.execute();
        } finally {
            permits.release();
        }
    }
}
