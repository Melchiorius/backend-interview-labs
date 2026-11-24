// Theory: knowledge/02-collections/concurrent.md
// Theory: knowledge/02-collections/tradeoffs.md
import java.util.*;
import java.util.concurrent.*;

public class ConcurrentCollectionsProbe {
    static void check(String label, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) throw new AssertionError(label + ": " + actual);
        System.out.println(label + " = " + actual);
    }
    static void throwsType(String label, Class<? extends RuntimeException> type, Runnable action) {
        try { action.run(); } catch (RuntimeException e) {
            if (!type.isInstance(e)) throw e;
            System.out.println(label + " = " + type.getSimpleName());
            return;
        }
        throw new AssertionError(label + ": missing " + type.getSimpleName());
    }
    static void transitions() {
        var m = new ConcurrentHashMap<String, Integer>();
        check("absent", null, m.get("x"));
        throwsType("null key", NullPointerException.class, () -> m.put(null, 67));
        throwsType("null value", NullPointerException.class, () -> m.put("x", null));
        check("putIfAbsent absent", null, m.putIfAbsent("x", 1));
        check("putIfAbsent present", 1, m.putIfAbsent("x", 67));
        check("conditional replace mismatch", false, m.replace("x", 2, 67));
        check("conditional replace match", true, m.replace("x", 1, 67));
        check("replace absent", null, m.replace("missing", 67));
        check("replace present", 67, m.replace("x", 2));
        check("conditional remove mismatch", false, m.remove("x", 67));
        check("conditional remove match", true, m.remove("x", 2));
        check("computeIfAbsent absent", 67, m.computeIfAbsent("x", k -> 67));
        check("computeIfAbsent existing", 67, m.computeIfAbsent("x", k -> { throw new AssertionError(); }));
        check("computeIfAbsent null", null, m.computeIfAbsent("missing", k -> null));
        check("null result not installed", false, m.containsKey("missing"));
        throwsType("computeIfAbsent exception", IllegalArgumentException.class, () -> m.computeIfAbsent("missing", k -> { throw new IllegalArgumentException(); }));
        check("failed absent callback leaves absent", false, m.containsKey("missing"));
        check("computeIfPresent absent", null, m.computeIfPresent("missing", (k,v) -> { throw new AssertionError(); }));
        check("computeIfPresent present", 68, m.computeIfPresent("x", (k,v) -> v + 1));
        throwsType("computeIfPresent exception", IllegalArgumentException.class, () -> m.computeIfPresent("x", (k,v) -> { throw new IllegalArgumentException(); }));
        check("failed present callback unchanged", 68, m.get("x"));
        check("computeIfPresent removes", null, m.computeIfPresent("x", (k,v) -> null));
        check("removed mapping", false, m.containsKey("x"));
        check("compute absent", 67, m.compute("x", (k,v) -> { if (v != null) throw new AssertionError(); return 67; }));
        check("compute present", 68, m.compute("x", (k,v) -> v + 1));
        throwsType("compute exception", IllegalArgumentException.class, () -> m.compute("x", (k,v) -> { throw new IllegalArgumentException(); }));
        check("failed compute unchanged", 68, m.get("x"));
        check("compute removes", null, m.compute("x", (k,v) -> null));
        check("merge absent", 67, m.merge("x", 67, (a,b) -> { throw new AssertionError(); }));
        check("merge present", 68, m.merge("x", 1, Integer::sum));
        throwsType("merge exception", IllegalArgumentException.class, () -> m.merge("x", 1, (a,b) -> { throw new IllegalArgumentException(); }));
        check("failed merge unchanged", 68, m.get("x"));
        check("merge removes", null, m.merge("x", 1, (a,b) -> null));
        check("merge removed mapping", false, m.containsKey("x"));
        throwsType("merge null value", NullPointerException.class, () -> m.merge("x", null, Integer::sum));
    }
    static void queues() throws InterruptedException {
        var q = new ArrayBlockingQueue<Integer>(1);
        check("empty poll", null, q.poll());
        throwsType("empty remove", NoSuchElementException.class, q::remove);
        check("empty timed poll", null, q.poll(0, TimeUnit.MILLISECONDS));
        throwsType("queue null", NullPointerException.class, () -> q.offer(null));
        check("add", true, q.add(67));
        check("full capacity", 0, q.remainingCapacity());
        check("full offer", false, q.offer(68));
        throwsType("full add", IllegalStateException.class, () -> q.add(68));
        check("full timed offer", false, q.offer(68, 0, TimeUnit.MILLISECONDS));
        check("remove head", 67, q.remove());
        check("available timed offer", true, q.offer(68, 0, TimeUnit.MILLISECONDS));
        check("available timed poll", 68, q.poll(0, TimeUnit.MILLISECONDS));
    }
    static void snapshot() {
        var payload = new StringBuilder("a");
        var list = new CopyOnWriteArrayList<>(List.of(payload, new StringBuilder("b")));
        var it = list.iterator();
        list.add(new StringBuilder("c"));
        list.set(1, new StringBuilder("d"));
        payload.append(67);
        var seen = new ArrayList<String>();
        it.forEachRemaining(x -> seen.add(x.toString()));
        check("shallow snapshot", List.of("a67", "b"), seen);
        check("current list", "[a67, d, c]", list.toString());
        throwsType("snapshot remove", UnsupportedOperationException.class, it::remove);
    }

    @FunctionalInterface
    interface CheckedAction {
        void run() throws InterruptedException, ExecutionException, TimeoutException;
    }

    // Preserve the primary exception even if the bounded cleanup itself fails.
    static void runWithCleanup(CheckedAction work, CheckedAction cleanup)
            throws InterruptedException, ExecutionException, TimeoutException {
        Throwable primary = null;
        try {
            work.run();
        } catch (InterruptedException | ExecutionException | TimeoutException | RuntimeException | Error failure) {
            primary = failure;
            throw failure;
        } finally {
            try {
                cleanup.run();
            } catch (InterruptedException | ExecutionException | TimeoutException | RuntimeException | Error secondary) {
                if (primary == null) throw secondary;
                if (primary != secondary) primary.addSuppressed(secondary);
                // The checked interruption cannot replace the already propagating primary.
                if (secondary instanceof InterruptedException) Thread.currentThread().interrupt();
            }
        }
    }

    static Throwable captureFailure(CheckedAction action) {
        try {
            action.run();
        } catch (InterruptedException | ExecutionException | TimeoutException | RuntimeException | Error failure) {
            return failure;
        }
        throw new AssertionError("expected failure");
    }

    static void cleanupDiagnostics() throws InterruptedException, ExecutionException, TimeoutException {
        // Synchronous injected failures, not a blocked-worker or scheduling experiment.
        var cleaned = new boolean[1];
        runWithCleanup(() -> {}, () -> cleaned[0] = true);
        check("N8 happy cleanup executed", true, cleaned[0]);
        var primary = new ExecutionException(new IllegalStateException("worker"));
        var secondary = new IllegalStateException("cleanup");
        check("N8 primary only identity", true,
                captureFailure(() -> runWithCleanup(() -> { throw primary; }, () -> {})) == primary);
        check("N8 cleanup only identity", true,
                captureFailure(() -> runWithCleanup(() -> {}, () -> { throw secondary; })) == secondary);
        var both = captureFailure(() -> runWithCleanup(() -> { throw primary; }, () -> { throw secondary; }));
        check("N8 primary preserved", true, both == primary);
        check("N8 secondary suppressed", true,
                both.getSuppressed().length == 1 && both.getSuppressed()[0] == secondary);
        var interruption = new InterruptedException("injected");
        check("N8 primary interruption propagated", true,
                captureFailure(() -> runWithCleanup(() -> { throw interruption; }, () -> {})) == interruption);
        check("N8 cleanup interruption propagated", true,
                captureFailure(() -> runWithCleanup(() -> {}, () -> { throw interruption; })) == interruption);
        var timeout = new TimeoutException("primary");
        try {
            var failure = captureFailure(() -> runWithCleanup(() -> { throw timeout; }, () -> { throw interruption; }));
            check("N8 interrupted cleanup primary", true, failure == timeout);
            check("N8 interrupted cleanup suppressed", true,
                    failure.getSuppressed().length == 1 && failure.getSuppressed()[0] == interruption);
            check("N8 interrupt flag restored", true, Thread.currentThread().isInterrupted());
        } finally {
            // Clear only the flag injected by this diagnostic in the standalone main thread.
            Thread.interrupted();
        }
    }

    static void atomicity() throws InterruptedException, ExecutionException, TimeoutException {
        var m = new ConcurrentHashMap<String, Integer>();
        m.put("x", 0);
        // Logical interleaving, deliberately executed sequentially.
        int a = m.get("x"), b = m.get("x");
        m.put("x", a + 1); m.put("x", b + 1);
        check("logical lost update", 1, m.get("x"));
        m.put("a", 67); m.put("b", 0);
        m.compute("a", (k,v) -> v - 1);
        check("intermediate total", 66, m.get("a") + m.get("b"));
        m.merge("b", 1, Integer::sum);
        check("final total", 67, m.get("a") + m.get("b"));
        var counts = new ConcurrentHashMap<String, Integer>();
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2, task -> {
            var t = new Thread(task); t.setDaemon(true); return t;
        });
        var futures = new ArrayList<Future<?>>();
        runWithCleanup(() -> {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            for (int i = 0; i < 2; i++) futures.add(pool.submit(() -> {
                start.await();
                for (int n = 0; n < 1000; n++) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                    counts.merge("x", 1, Integer::sum);
                }
                return null;
            }));
            start.countDown();
            for (var f : futures) f.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            check("completed per-key increments", 2000, counts.get("x"));
        }, () -> {
            start.countDown();
            for (var f : futures) f.cancel(true);
            pool.shutdownNow();
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) throw new IllegalStateException("worker cleanup timeout");
        });
    }
    static void views() {
        Map<String, Integer> wrapper = Collections.synchronizedMap(new HashMap<>());
        synchronized (wrapper) {
            wrapper.put("x", wrapper.getOrDefault("x", 0) + 67);
            check("wrapper traversal", Set.of("x"), new HashSet<>(wrapper.keySet()));
            check("wrapper value", 67, wrapper.get("x"));
        }
        var m = new ConcurrentHashMap<String, Integer>();
        m.put("a", 1); m.put("b", 2);
        throwsType("ordinary keySet add", UnsupportedOperationException.class, () -> m.keySet().add("c"));
        check("mapped keySet add", true, m.keySet(67).add("c"));
        check("mapped keySet value", 67, m.get("c"));
        check("stable traversal membership", true, Set.of("a", "b", "c").equals(new HashSet<>(m.keySet())));
        check("stable size", 3, m.size());
    }
    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        transitions(); queues(); snapshot(); atomicity(); views(); cleanupDiagnostics();
    }
}
