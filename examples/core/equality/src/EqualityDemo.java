// Theory: knowledge/01-java-core/equality.md
// Theory: knowledge/01-java-core/object.md
// Theory: knowledge/01-java-core/types.md
// Theory: knowledge/01-java-core/classes.md
// Theory: knowledge/01-java-core/inheritance.md
// Research: docs/plans/CORE-OBJECT-EQUALITY.md (G1 ACCEPTED, PR #61 merged in main)

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class EqualityDemo {
    private EqualityDemo() {}

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // Immutable value key: the same fields participate in equals and hashCode.
    private static final class ValueKey {
        private final int id;
        private final String code;

        private ValueKey(int id, String code) {
            this.id = id;
            this.code = Objects.requireNonNull(code);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof ValueKey key)) return false;
            return id == key.id && code.equals(key.code);
        }

        @Override
        public int hashCode() {
            return 31 * Integer.hashCode(id) + code.hashCode();
        }
    }

    // Deliberately broken: equals ignores salt but hashCode uses it.
    private static final class BadHashKey {
        private final int id;
        private final int salt;

        private BadHashKey(int id, int salt) {
            this.id = id;
            this.salt = salt;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof BadHashKey key && id == key.id;
        }

        @Override
        public int hashCode() {
            return salt;
        }
    }

    // Deliberately broken: mutating a field used by hashCode/equals after insertion.
    private static final class MutableKey {
        private int id;

        private MutableKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof MutableKey key && id == key.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }

        private void changeId(int newId) {
            id = newId;
        }
    }

    // Deliberately broken: subclass adds a restriction parent does not apply.
    private static class ParentKey {
        private final int id;

        private ParentKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ParentKey key && id == key.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    private static final class ChildKey extends ParentKey {
        private final String label;

        private ChildKey(int id, String label) {
            super(id);
            this.label = label;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ChildKey key
                    && super.equals(key)
                    && label.equals(key.label);
        }

        @Override
        public int hashCode() {
            return super.hashCode(); // Not a fix for asymmetry; keeps equal hashes in this case.
        }
    }

    public static void main(String[] args) {
        ValueKey original = new ValueKey(67, "FB");
        ValueKey copy = new ValueKey(67, "FB");
        ValueKey collision = new ValueKey(67, "Ea");
        boolean differentReferences = original != copy;
        boolean equalValues = original.equals(copy);
        boolean matchingHashes = original.hashCode() == copy.hashCode();
        boolean collisionButNotEqual = original.hashCode() == collision.hashCode()
                && !original.equals(collision);
        Set<ValueKey> collidingSet = new HashSet<>();
        collidingSet.add(original);
        collidingSet.add(collision);
        Map<ValueKey, String> goodMap = new HashMap<>();
        goodMap.put(original, "left");
        goodMap.put(collision, "right");

        check(differentReferences && equalValues && matchingHashes, "value equality vs identity");
        ValueKey third = new ValueKey(67, "FB");
        check(original.equals(original) && copy.equals(original), "reflexivity/symmetry");
        check(copy.equals(third) && original.equals(third), "transitivity");
        check(!original.equals(null), "non-null value key must differ from null");
        check(collisionButNotEqual && collidingSet.size() == 2, "hash collision is not equality");
        check("left".equals(goodMap.get(copy)), "equal key must retrieve value");
        check("right".equals(goodMap.get(collision)), "distinct colliding key retained");
        System.out.println("== Identity and correct immutable key ==");
        System.out.println("Different references: " + differentReferences);
        System.out.println("Equal values and matching hashes: " + equalValues + "/" + matchingHashes);
        System.out.println("Unequal hash-collision keys in HashSet: " + collidingSet.size());
        System.out.println("HashMap retrieves colliding keys: " + goodMap.get(copy) + "/" + goodMap.get(collision));

        BadHashKey badStored = new BadHashKey(67, 1);
        BadHashKey badLookup = new BadHashKey(67, 2);
        Map<BadHashKey, String> badMap = new HashMap<>();
        badMap.put(badStored, "stored");
        boolean badEqual = badStored.equals(badLookup);
        boolean badHashesEqual = badStored.hashCode() == badLookup.hashCode();
        String brokenLookupResult = badMap.get(badLookup);
        check(badEqual && !badHashesEqual, "deliberate hash contract violation");
        check(brokenLookupResult == null, "observed OpenJDK 21 HashMap case");

        ParentKey parent = new ParentKey(67);
        ChildKey child = new ChildKey(67, "zone");
        boolean parentToChild = parent.equals(child);
        boolean childToParent = child.equals(parent);
        check(parentToChild && !childToParent, "deliberate symmetry violation");

        MutableKey mutable = new MutableKey(67);
        Map<MutableKey, String> mutableMap = new HashMap<>();
        mutableMap.put(mutable, "stored");
        String beforeMutation = mutableMap.get(mutable);
        mutable.changeId(68);
        String afterMutation = mutableMap.get(mutable);
        check("stored".equals(beforeMutation) && afterMutation == null,
                "observed OpenJDK 21 lookup after key mutation");

        System.out.println("== Deliberately broken keys (OpenJDK 21 observation) ==");
        System.out.println("Broken hash: equal/hash-equal=" + badEqual + "/" + badHashesEqual);
        System.out.println("Broken hash: HashMap lookup=" + brokenLookupResult);
        System.out.println("Asymmetric equals: parent->child/child->parent=" + parentToChild + "/" + childToParent);
        System.out.println("Mutable key: lookup before/after=" + beforeMutation + "/" + afterMutation);

        BigDecimal twoWithOneZero = new BigDecimal("2.0");
        BigDecimal twoWithTwoZeros = new BigDecimal("2.00");
        boolean decimalsEqual = twoWithOneZero.equals(twoWithTwoZeros);
        boolean decimalsCompareAsEqual = twoWithOneZero.compareTo(twoWithTwoZeros) == 0;
        Set<BigDecimal> decimalHashSet = new HashSet<>();
        Set<BigDecimal> decimalTreeSet = new TreeSet<>();
        decimalHashSet.add(twoWithOneZero);
        decimalHashSet.add(twoWithTwoZeros);
        decimalTreeSet.add(twoWithOneZero);
        decimalTreeSet.add(twoWithTwoZeros);
        check(!decimalsEqual && decimalsCompareAsEqual, "BigDecimal scale vs value");
        check(decimalHashSet.size() == 2 && decimalTreeSet.size() == 1, "hash vs sorted set");

        Set<String> stringHashSet = new HashSet<>();
        Set<String> stringTreeSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        stringHashSet.add("a");
        stringHashSet.add("A");
        stringTreeSet.add("a");
        stringTreeSet.add("A");
        check(stringHashSet.size() == 2 && stringTreeSet.size() == 1, "comparator vs equals");

        int[][] nestedA = {{67}, {68}};
        int[][] nestedB = {{67}, {68}};
        boolean shallowArraysEqual = Arrays.equals(nestedA, nestedB);
        boolean deepArraysEqual = Arrays.deepEquals(nestedA, nestedB);
        check(!shallowArraysEqual && deepArraysEqual, "arrays shallow vs deep");
        check(Objects.equals(null, null) && !Objects.equals(null, original), "null safe equals");

        System.out.println("== Hash equality vs sorted equality ==");
        System.out.println("BigDecimal equals/compareToZero=" + decimalsEqual + "/" + decimalsCompareAsEqual);
        System.out.println("BigDecimal HashSet/TreeSet=" + decimalHashSet.size() + "/" + decimalTreeSet.size());
        System.out.println("Case-insensitive String HashSet/TreeSet=" + stringHashSet.size() + "/" + stringTreeSet.size());
        System.out.println("Nested arrays shallow/deep=" + shallowArraysEqual + "/" + deepArraysEqual);
        System.out.println("Objects.equals(null, null)=" + Objects.equals(null, null));
        System.out.println("CHECK PASSED");
    }
}
