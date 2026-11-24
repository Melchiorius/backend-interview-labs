// Theory: knowledge/02-collections/sorted.md
// Theory: knowledge/02-collections/priority-queue.md
import java.math.BigDecimal;
import java.util.*;

public class SortedHeapProbe {
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
    static void comparisons() {
        var a = new BigDecimal("1.0");
        var b = new BigDecimal("1.00");
        check("decimal equals", false, a.equals(b));
        check("decimal compare sign", 0, Integer.signum(a.compareTo(b)));
        check("tree set size", 1, new TreeSet<>(List.of(a, b)).size());
        check("hash set size", 2, new HashSet<>(List.of(a, b)).size());
        // Broken comparator: demonstrate overflow, never sort with it.
        Comparator<Integer> broken = (x, y) -> x - y;
        check("overflow forward sign", -1, Integer.signum(broken.compare(Integer.MAX_VALUE, -1)));
        check("overflow backward sign", -1, Integer.signum(broken.compare(-1, Integer.MAX_VALUE)));
        check("safe forward sign", 1, Integer.signum(Integer.compare(Integer.MAX_VALUE, -1)));
        check("safe backward sign", -1, Integer.signum(Integer.compare(-1, Integer.MAX_VALUE)));
        Comparator<String> byLength = Comparator.comparingInt(String::length);
        var set = new TreeSet<>(byLength);
        set.add("ab");
        check("length comparator duplicate", false, set.add("cd"));
        check("length comparator contains", true, set.contains("xy"));
        var map = new TreeMap<String, Integer>(byLength);
        map.put("ab", 1);
        check("equivalent key replaces value", 1, map.put("cd", 67));
        check("equivalent key map size", 1, map.size());
        check("equivalent key lookup", 67, map.get("xy"));
        throwsType("natural tree null", NullPointerException.class, () -> new TreeSet<Integer>().add(null));
        var nullable = new TreeSet<Integer>(Comparator.nullsFirst(Comparator.naturalOrder()));
        nullable.add(67); nullable.add(null);
        check("null comparator first", null, nullable.first());
        check("null comparator contains", true, nullable.contains(null));
    }
    static void navigation() {
        var map = new TreeMap<Integer, String>();
        map.put(30, "c"); map.put(10, "a"); map.put(20, "b");
        check("sorted keys", List.of(10, 20, 30), new ArrayList<>(map.keySet()));
        check("natural comparator", null, map.comparator());
        check("lower", 10, map.lowerKey(20));
        check("floor", 20, map.floorKey(20));
        check("ceiling", 20, map.ceilingKey(20));
        check("higher", 30, map.higherKey(20));
        var range = map.subMap(10, true, 30, false);
        check("range keys", List.of(10, 20), new ArrayList<>(range.keySet()));
        range.put(15, "middle");
        check("range writes parent", "middle", map.get(15));
        map.put(20, "changed");
        check("parent writes range", "changed", range.get(20));
        throwsType("excluded upper bound", IllegalArgumentException.class, () -> range.put(30, "x"));
        var descending = map.descendingMap();
        check("descending keys", List.of(30, 20, 15, 10), new ArrayList<>(descending.keySet()));
        descending.remove(15);
        check("descending removes parent key", false, map.containsKey(15));
        var reversed = map.reversed();
        check("sequenced reverse keys", List.of(30, 20, 10), new ArrayList<>(reversed.keySet()));
        reversed.put(25, "new");
        check("reverse writes parent", "new", map.get(25));
        throwsType("tree putFirst", UnsupportedOperationException.class, () -> map.putFirst(1, "x"));
        throwsType("tree putLast", UnsupportedOperationException.class, () -> map.putLast(40, "x"));
        var snapshot = map.firstEntry();
        map.put(10, "updated");
        check("navigation entry snapshot", "a", snapshot.getValue());
        check("parent value updated", "updated", map.get(10));
        throwsType("navigation entry setValue", UnsupportedOperationException.class, () -> snapshot.setValue("x"));
    }
    record Task(String name, int priority) {}
    static void queues() {
        var q = new PriorityQueue<Integer>();
        check("empty peek", null, q.peek());
        check("empty poll", null, q.poll());
        throwsType("empty element", NoSuchElementException.class, q::element);
        throwsType("empty remove", NoSuchElementException.class, q::remove);
        throwsType("queue null", NullPointerException.class, () -> q.offer(null));
        q.addAll(List.of(67, 2, 5, 2));
        check("min head", 2, q.peek());
        check("duplicate size", 4, q.size());
        check("remove object", true, q.remove(Integer.valueOf(67)));
        check("head remove", 2, q.remove());
        check("remaining duplicate", 2, q.poll());
        check("remaining value", 5, q.poll());
        check("drained", true, q.isEmpty());
        var max = new PriorityQueue<Integer>(Comparator.reverseOrder());
        max.addAll(List.of(2, 67, 5));
        check("max poll order", List.of(67, 5, 2), List.of(max.poll(), max.poll(), max.poll()));
        var tasks = new PriorityQueue<Task>(Comparator.comparingInt(Task::priority));
        var a = new Task("a", 1); var b = new Task("b", 1); var c = new Task("c", 67);
        tasks.addAll(List.of(c, a, b));
        Task first = tasks.poll(), second = tasks.poll();
        check("tie priorities", List.of(1, 1), List.of(first.priority(), second.priority()));
        check("tie membership", true, Set.of(a, b).equals(new HashSet<>(List.of(first, second))));
        check("later priority", c, tasks.poll());
    }
    public static void main(String[] args) {
        comparisons(); navigation(); queues();
    }
}
