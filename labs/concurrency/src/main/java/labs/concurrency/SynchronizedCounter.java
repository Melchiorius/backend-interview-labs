package labs.concurrency;

/**
 * A minimal thread-safe counter.
 *
 * <p>Both increment() and get() use the same intrinsic monitor (this).
 * The increment is therefore an indivisible critical section with respect
 * to other calls guarded by this monitor, and get() observes the value
 * under the same synchronization discipline.</p>
 */
public final class SynchronizedCounter {

    private long value;

    public synchronized void increment() {
        value++;
    }

    public synchronized long get() {
        return value;
    }
}
