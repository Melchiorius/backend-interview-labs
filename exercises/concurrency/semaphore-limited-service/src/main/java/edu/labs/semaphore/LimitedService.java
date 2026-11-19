package edu.labs.semaphore;

// Theory: knowledge/03-concurrency/semaphore.md
// Exercise: exercises/concurrency/semaphore-limited-service/README.md
public final class LimitedService {
    public LimitedService(int maxConcurrent, Gateway gateway) {
        // TODO: check constructor arguments; create Semaphore(maxConcurrent).
    }

    public String call() throws Exception {
        // TODO: acquire a permit, invoke gateway, release only if acquired.
        throw new UnsupportedOperationException("Implement LimitedService.call()");
    }
}
