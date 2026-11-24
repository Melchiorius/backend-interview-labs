// Theory: knowledge/02-collections/contracts.md
// Theory: knowledge/02-collections/hash-collections.md
// Theory: knowledge/01-java-core/equality.md
// Theory: knowledge/01-java-core/immutability.md
import java.util.*;

public class ContractsHashProbe {
    // Equal ids define equality; deliberately colliding hashes do not define equality.
    record Key(int id) {
        @Override public int hashCode() { return 67; }
    }

    static final class MutableKey {
        int id;
        MutableKey(int id) { this.id = id; }
        @Override public int hashCode() { return id; }
        @Override public boolean equals(Object other) {
            return other instanceof MutableKey key && id == key.id;
        }
    }

    private static void expect(String label, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
        System.out.println(label + " = " + actual);
    }

    private static void expectThrows(String label, Class<? extends RuntimeException> type, Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException e) {
            if (!type.isInstance(e)) throw e;
            System.out.println(label + " = " + type.getSimpleName());
            return;
        }
        throw new AssertionError(label + ": expected " + type.getSimpleName());
    }

    private static void contracts() {
        List<Integer> list = new ArrayList<>(List.of(67, 67));
        Set<Integer> set = new HashSet<>(list);
        expect("CC01 list size with duplicates", 2, list.size());
        expect("CC02 set size with duplicates", 1, set.size());
        expect("CC03 duplicate set add", false, set.add(67));
        expect("CC04 list order matters", false, List.of(1, 2).equals(List.of(2, 1)));
        expect("CC05 set order irrelevant", true, Set.of(1, 2).equals(Set.of(2, 1)));
        expect("CC06 map equality across implementations", true,
                Map.of("x", 67).equals(new HashMap<>(Map.of("x", 67))));
        expectThrows("CC07 unmodifiable list add", UnsupportedOperationException.class, () -> List.of(67).add(68));
        expectThrows("CC08 factory null", NullPointerException.class, () -> Map.of("x", (Integer) null));

        Map<String, Integer> map = new HashMap<>();
        map.put("x", null);
        expect("CC09 mapped null get", null, map.get("x"));
        expect("CC10 absent get", null, map.get("missing"));
        expect("CC11 mapped null present", true, map.containsKey("x"));
        expect("CC12 absent present", false, map.containsKey("missing"));
        map.put(null, null);
        expect("CC13 null key present", true, map.containsKey(null));
        Set<String> keys = map.keySet();
        Collection<Integer> values = map.values();
        map.put("y", 67);
        expect("CC14 keys see map insertion", true, keys.contains("y"));
        expect("CC15 values see map insertion", true, values.contains(67));
        expect("CC16 key removal", true, keys.remove("x"));
        expect("CC17 map sees key removal", false, map.containsKey("x"));
        // Select by key, never by unspecified HashMap encounter order.
        Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
        Map.Entry<String, Integer> entry;
        do { entry = iterator.next(); } while (!"y".equals(entry.getKey()));
        expect("CC18 entry old value", 67, entry.setValue(68));
        expect("CC19 map sees entry update", 68, map.get("y"));
        expect("CC20 values see entry update", true, values.contains(68));
        expectThrows("CC21 key view cannot add", UnsupportedOperationException.class, () -> keys.add("z"));

        List<StringBuilder> backing = new ArrayList<>();
        StringBuilder item = new StringBuilder("a");
        backing.add(item);
        List<StringBuilder> view = Collections.unmodifiableList(backing);
        List<StringBuilder> copy = List.copyOf(backing);
        backing.add(new StringBuilder("b"));
        expect("CC22 view size after backing add", 2, view.size());
        expect("CC23 copy size after backing add", 1, copy.size());
        item.append("67");
        expect("CC24 copy shares mutable element", "a67", copy.getFirst().toString());
        expectThrows("CC25 view add", UnsupportedOperationException.class, () -> view.add(item));
        expectThrows("CC26 copy add", UnsupportedOperationException.class, () -> copy.add(item));

        SequencedMap<String, Integer> ordered = new LinkedHashMap<>();
        ordered.put("a", 1);
        ordered.put("b", 2);
        SequencedMap<String, Integer> reverse = ordered.reversed();
        expect("CC27 reversed keys", List.of("b", "a"), new ArrayList<>(reverse.sequencedKeySet()));
        ordered.putLast("c", 3);
        expect("CC28 reverse sees original addition", "c", reverse.firstEntry().getKey());
        reverse.putFirst("d", 67);
        expect("CC29 original sees reverse addition", "d", ordered.lastEntry().getKey());
    }

    private static void hashAndUpdates() {
        Key one = new Key(1), two = new Key(2);
        expect("CH01 equal hashes", true, one.hashCode() == two.hashCode());
        expect("CH02 unequal keys", false, one.equals(two));
        Map<Key, String> collisions = new HashMap<>();
        collisions.put(one, "one");
        collisions.put(two, "two");
        expect("CH03 collision map size", 2, collisions.size());
        expect("CH04 lookup equal key one", "one", collisions.get(new Key(1)));
        expect("CH05 lookup equal key two", "two", collisions.get(new Key(2)));
        expect("CH06 replacement old value", "one", collisions.put(new Key(1), "updated"));
        expect("CH07 replacement preserves size", 2, collisions.size());
        expect("CH08 replacement value", "updated", collisions.get(one));
        Set<Key> distinct = new HashSet<>(List.of(one, two));
        expect("CH09 collision set size", 2, distinct.size());
        expect("CH10 equal key duplicate", false, distinct.add(new Key(1)));

        // Specific OpenJDK 21 observation, not a Map guarantee after key mutation.
        // Initial capacity 16, one insertion: spread(1)=1, spread(67)=67;
        // indices 1 and 3 in the 16-slot table. No resize or other keys.
        MutableKey key = new MutableKey(1);
        Map<MutableKey, String> unstable = new HashMap<>(16);
        unstable.put(key, "saved");
        expect("CH11 before key mutation", "saved", unstable.get(key));
        key.id = 67;
        expect("CH12 OpenJDK lookup after mutation", null, unstable.get(key));
        expect("CH13 OpenJDK contains after mutation", false, unstable.containsKey(key));
        expect("CH14 OpenJDK size after mutation", 1, unstable.size());
        key.id = 1;
        expect("CH15 OpenJDK lookup after restoration", "saved", unstable.get(key));

        Map<String, Integer> counts = new HashMap<>();
        counts.put("null", null);
        counts.put("existing", 1);
        expect("CH16 compute absent", 67, counts.computeIfAbsent("absent", k -> 67));
        expect("CH17 compute mapped null", 67, counts.computeIfAbsent("null", k -> 67));
        expect("CH18 compute existing", 1, counts.computeIfAbsent("existing", k -> {
            throw new AssertionError("callback must not run for non-null mapping");
        }));
        expect("CH19 compute null result", null, counts.computeIfAbsent("missing", k -> null));
        expect("CH20 null result no insertion", false, counts.containsKey("missing"));
        counts.put("still-null", null);
        expect("CH21 compute existing null result", null, counts.computeIfAbsent("still-null", k -> null));
        expect("CH22 existing null mapping remains", true, counts.containsKey("still-null"));
        expect("CH23 merge absent", 67, counts.merge("new", 67, Integer::sum));
        expect("CH24 merge mapped null", 67, counts.merge("still-null", 67, Integer::sum));
        expect("CH25 merge existing", 68, counts.merge("existing", 67, Integer::sum));
        expect("CH26 merge null result", null, counts.merge("existing", 67, (a, b) -> null));
        expect("CH27 merge removes mapping", false, counts.containsKey("existing"));
        expectThrows("CH28 merge null argument", NullPointerException.class,
                () -> counts.merge("new", null, Integer::sum));
    }


    // The counter belongs to the query, not to the identity of the key.
    static final class CountingKey {
        final int id;
        int equalsCalls;
        CountingKey(int id) { this.id = id; }
        @Override public int hashCode() { return 67; }
        @Override public boolean equals(Object other) {
            equalsCalls++;
            return other instanceof CountingKey key && id == key.id;
        }
    }

    private static Map<CountingKey, Integer> sameHashMap(int size) {
        Map<CountingKey, Integer> map = new HashMap<>(128);
        for (int i = 0; i < size; i++) map.put(new CountingKey(i), i);
        return map;
    }

    private static void sameHashLookup() {
        var map = sameHashMap(67);
        expect("B1 same-hash size", 67, map.size());
        expect("B1 equal key lookup", 66, map.get(new CountingKey(66)));
        expect("B1 absent key lookup", null, map.get(new CountingKey(-1)));
    }

    private static void observeCollisions() {
        for (int n : new int[]{16, 32, 67, 128}) {
            var map = sameHashMap(n);
            var query = new CountingKey(-1);
            if (map.get(query) != null || map.size() != n) throw new AssertionError("lookup");
            // I observation only: no assertion on the count or internal tree shape.
            System.out.println("same-hash n=" + n + " absent equals calls=" + query.equalsCalls);
        }
    }

    private static void orderedHash() {
        var insertion = new LinkedHashMap<String, Integer>();
        var access = new LinkedHashMap<String, Integer>(16, 0.75f, true);
        for (var map : List.of(insertion, access)) {
            map.put("a", 1);
            map.put("b", 2);
            expect(map == insertion ? "B2 insertion get" : "B2 access get", 1, map.get("a"));
        }
        expect("B2 insertion order", List.of("a", "b"), new ArrayList<>(insertion.keySet()));
        expect("B2 access order", List.of("b", "a"), new ArrayList<>(access.keySet()));
        var lru = new LinkedHashMap<String, Integer>(16, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                return size() > 2;
            }
        };
        lru.put("a", 1);
        lru.put("b", 2);
        expect("B2 LRU touch", 1, lru.get("a"));
        lru.put("c", 67);
        expect("B2 LRU keys", List.of("a", "c"), new ArrayList<>(lru.keySet()));
        expect("B2 LRU evicted b", false, lru.containsKey("b"));
        expect("B2 LRU size", 2, lru.size());
        var set = new LinkedHashSet<>(List.of(1, 67, 2));
        expect("B2 set repeated add", false, set.add(1));
        expect("B2 set unchanged order", List.of(1, 67, 2), new ArrayList<>(set));
        set.addLast(1);
        expect("B2 set addLast order", List.of(67, 2, 1), new ArrayList<>(set));
    }

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--observe-collisions")) {
            observeCollisions();
            return;
        }
        if (args.length != 0) throw new IllegalArgumentException("unknown arguments");
        contracts();
        hashAndUpdates();
        sameHashLookup();
        orderedHash();
    }
}
