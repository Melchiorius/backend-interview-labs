package labs.concurrency;

import java.util.ArrayDeque;
import java.util.Objects;

/**
 * A bounded FIFO queue implemented with an intrinsic monitor.
 *
 * <p>Producers wait while the queue is full; consumers wait while it is empty.
 * Both categories wait on the same monitor, so state changes use notifyAll():
 * notify() could wake a thread whose condition is still false while leaving
 * the thread that can make progress asleep.</p>
 *
 * <p>The while loops are intentional. A thread must re-check its condition
 * after waking because wakeups may be spurious, and another thread may change
 * the queue before the awakened thread reacquires the monitor.</p>
 *
 * <p>This class guarantees FIFO order for elements, but does not guarantee
 * fairness among waiting producer or consumer threads.</p>
 */
public final class BoundedBlockingQueue<E> {

    private final int capacity;
    private final ArrayDeque<E> elements = new ArrayDeque<>();

    public BoundedBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    public synchronized void put(E element) throws InterruptedException {
        Objects.requireNonNull(element, "element");

        while (elements.size() == capacity) {
            wait();
        }

        elements.addLast(element);
        notifyAll();
    }

    public synchronized E take() throws InterruptedException {
        while (elements.isEmpty()) {
            wait();
        }

        E element = elements.removeFirst();
        notifyAll();
        return element;
    }

    public synchronized int size() {
        return elements.size();
    }

    public int capacity() {
        return capacity;
    }
}
