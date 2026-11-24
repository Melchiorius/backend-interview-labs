// Theory: knowledge/02-collections/sequential.md
// Theory: knowledge/02-collections/iteration.md
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class SequentialIteratorProbe {
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
    static void lists() {
        Integer[] array = {1, 2, 3};
        var list = new ArrayList<>(Arrays.asList(array));
        list.ensureCapacity(67);
        check("capacity is not size", 3, list.size());
        list.add(1, 67);
        check("insertion shifts positions", List.of(1, 67, 2, 3), list);
        check("array length unchanged", 3, array.length);
        var view = list.subList(1, 3);
        view.set(0, 68);
        check("subList set changes parent", List.of(1, 68, 2, 3), list);
        list.set(2, 69);
        check("parent set visible in view", List.of(68, 69), view);
        view.clear();
        check("subList clear changes parent", List.of(1, 3), list);
        var fixed = Arrays.asList(array);
        var copy = new ArrayList<>(fixed);
        fixed.set(0, 67);
        check("fixed set changes array", 67, array[0]);
        array[1] = 68;
        check("array change visible in fixed", List.of(67, 68, 3), fixed);
        throwsType("fixed add", UnsupportedOperationException.class, () -> fixed.add(4));
        copy.add(4);
        check("independent mutable copy", List.of(1, 2, 3, 4), copy);
    }
    static void deque() {
        var deque = new ArrayDeque<Integer>();
        check("empty poll", null, deque.pollFirst());
        check("empty peek", null, deque.peekLast());
        throwsType("empty remove", NoSuchElementException.class, deque::removeFirst);
        throwsType("empty element", NoSuchElementException.class, deque::element);
        throwsType("null add", NullPointerException.class, () -> deque.add(null));
        deque.addLast(1); deque.addLast(67); deque.addFirst(2);
        check("first end", 2, deque.removeFirst());
        check("last end", 67, deque.pollLast());
        deque.offer(3);
        check("FIFO", 1, deque.poll());
        deque.push(67);
        check("LIFO", 67, deque.pop());
        check("remaining deque", List.of(3), new ArrayList<>(deque));
    }
    static void iterators() {
        var list = new ArrayList<>(List.of(1, 67, 3));
        var it = list.iterator();
        throwsType("remove before next", IllegalStateException.class, it::remove);
        check("hasNext", true, it.hasNext());
        check("hasNext again", true, it.hasNext());
        check("next", 1, it.next());
        it.remove();
        check("iterator removes from list", List.of(67, 3), list);
        throwsType("remove twice", IllegalStateException.class, it::remove);
        check("next after remove", 67, it.next());
        var remaining = new ArrayList<Integer>();
        it.forEachRemaining(remaining::add);
        check("forEachRemaining", List.of(3), remaining);
        check("exhausted", false, it.hasNext());
        throwsType("next exhausted", NoSuchElementException.class, it::next);
        var immutable = List.of(67).iterator();
        immutable.next();
        throwsType("unsupported remove", UnsupportedOperationException.class, immutable::remove);
        var linked = new LinkedList<>(List.of(1, 2));
        var cursor = linked.listIterator();
        check("initial nextIndex", 0, cursor.nextIndex());
        check("initial previousIndex", -1, cursor.previousIndex());
        throwsType("previous at start", NoSuchElementException.class, cursor::previous);
        throwsType("set before traversal", IllegalStateException.class, () -> cursor.set(67));
        check("cursor next", 1, cursor.next());
        cursor.add(67);
        check("add at cursor", List.of(1, 67, 2), linked);
        check("nextIndex after add", 2, cursor.nextIndex());
        check("previousIndex after add", 1, cursor.previousIndex());
        throwsType("set after add", IllegalStateException.class, () -> cursor.set(68));
        throwsType("remove after add", IllegalStateException.class, cursor::remove);
        check("previous after add", 67, cursor.previous());
        cursor.set(68);
        check("set last returned", List.of(1, 68, 2), linked);
        cursor.remove();
        check("remove last returned", List.of(1, 2), linked);
        check("nextIndex after remove", 1, cursor.nextIndex());
        check("next after cursor remove", 2, cursor.next());
        int sum = 0;
        for (int x : new int[]{1, 67}) sum += x;
        check("enhanced for array", 68, sum);
        sum = 0;
        for (int x : List.of(1, 67)) sum += x;
        check("enhanced for Iterable", 68, sum);
    }
    static void traversalModels() {
        var cow = new CopyOnWriteArrayList<>(List.of(1, 2));
        var snapshot = cow.iterator();
        cow.add(67); cow.set(0, 68);
        var seen = new ArrayList<Integer>();
        snapshot.forEachRemaining(seen::add);
        check("COW snapshot", List.of(1, 2), seen);
        check("COW current list", List.of(68, 2, 67), cow);
        throwsType("COW remove", UnsupportedOperationException.class, snapshot::remove);
        // Specific OpenJDK 21 observation; fail-fast detection is best effort.
        var list = new ArrayList<>(List.of(1, 2));
        var stale = list.iterator();
        list.add(67);
        throwsType("OpenJDK21 observed fail-fast", ConcurrentModificationException.class, stale::next);
    }
    public static void main(String[] args) {
        lists(); deque(); iterators(); traversalModels();
    }
}
