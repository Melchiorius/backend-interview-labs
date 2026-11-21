// Theory: knowledge/01-java-core/types.md
// Theory: knowledge/01-java-core/inheritance.md
// Research: docs/plans/CORE-GENERICS-ERASURE.md
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A single Java 21 example of generic constraints and the runtime erasure boundary. */
public final class GenericsErasureDemo {
    private GenericsErasureDemo() {}

    private static final class NumberBox<T extends Number> {
        private final T value;
        NumberBox(T value) { this.value = Objects.requireNonNull(value); }
        T get() { return value; }
    }

    private static <T extends Number & Comparable<T>> int compare(T left, T right) {
        return left.compareTo(right);
    }

    private static <T> void copyFirst(List<? super T> target, List<? extends T> source) {
        target.set(0, source.get(0));
    }

    private static void repeatFirst(List<?> items) {
        repeatFirstCaptured(items);
    }

    private static <T> void repeatFirstCaptured(List<T> items) {
        items.set(1, items.get(0));
    }

    // Deliberately unsafe: a raw alias bypasses the generic List<String> contract.
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void polluteThroughRaw(List<String> typed) {
        List raw = typed;
        raw.add(67);
    }

    private static <T> T identity(T value) {
        return value;
    }

    private static class Parent<T> {
        T get() { return null; }
    }

    private static final class Child extends Parent<String> {
        @Override String get() { return "child"; }
    }

    private static void verify(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        NumberBox<Integer> box = new NumberBox<>(67);
        int comparison = compare(67, 68);
        List<Integer> producerValues = List.of(67, 68);
        List<Number> destination = new ArrayList<>(List.of(0, 0));
        Collections.copy(destination, producerValues);
        copyFirst(destination, producerValues);

        List<? extends Number> producer = new ArrayList<Integer>(List.of(67));
        Number first = producer.get(0);
        producer.add(null); // Allowed, but not an arbitrary non-null Number.

        List<? super Integer> consumer = new ArrayList<Number>();
        consumer.add(67);
        Object readBack = consumer.get(0); // Static read type is Object.

        List<String> captured = new ArrayList<>(List.of("a", "b"));
        repeatFirst(captured);

        List<String> strings = new ArrayList<>();
        List<Integer> integers = new ArrayList<>();
        boolean sameRuntimeClass = strings.getClass() == integers.getClass();
        boolean instanceofWildcard = strings instanceof List<?>;
        // Legal since Java 16: the narrowing conversion is checked-cast compatible.
        boolean instanceofParameterization = strings instanceof ArrayList<String>;

        Class<?> erasedBound = NumberBox.class.getDeclaredMethod("get").getReturnType();
        Method erasedIdentity = GenericsErasureDemo.class.getDeclaredMethod("identity", Object.class);

        List<String> polluted = new ArrayList<>();
        polluteThroughRaw(polluted);
        String rawFailure = "no-error";
        try {
            String value = polluted.get(0); // Compiler-added runtime cast to String.
        } catch (ClassCastException expected) {
            rawFailure = "ClassCastException";
        }

        String arrayFailure = "no-error";
        try {
            Number[] covariant = new Integer[1];
            covariant[0] = 3.14;
        } catch (ArrayStoreException expected) {
            arrayFailure = "ArrayStoreException";
        }

        List<?>[] reifiableArray = new List<?>[1];
        reifiableArray[0] = List.of(67);
        Parent<String> parentReference = new Child();
        String overrideValue = parentReference.get();
        String genericInference = identity("typed");

        verify(box.get() == 67 && comparison < 0, "bounded type variable");
        verify(destination.equals(List.of(67, 68)), "PECS copy into existing elements");
        verify(first.intValue() == 67 && producer.size() == 2, "extends read, null write");
        verify(readBack.equals(67), "super write, Object read");
        verify(captured.equals(List.of("a", "a")), "wildcard capture");
        verify(sameRuntimeClass && instanceofWildcard && instanceofParameterization, "erasure/runtime checks");
        verify(erasedBound == Number.class && erasedIdentity.getReturnType() == Object.class, "erased signatures");
        verify("ClassCastException".equals(rawFailure) && "ArrayStoreException".equals(arrayFailure), "runtime failures");
        verify(Objects.equals(reifiableArray[0].get(0), 67), "reifiable wildcard array");
        verify("child".equals(overrideValue) && "typed".equals(genericInference), "dispatch/inference");

        System.out.println("== Compile-time generic constraints ==");
        System.out.println("bounded NumberBox: " + box.get());
        System.out.println("bounded compare: " + comparison);
        System.out.println("PECS Collections.copy: " + destination);
        System.out.println("wildcard extends read/null: " + first + "/" + producer.size());
        System.out.println("wildcard super write/Object read: " + readBack);
        System.out.println("wildcard capture helper: " + captured);
        System.out.println("generic method inference: " + genericInference);
        System.out.println("== Erasure and runtime boundaries ==");
        System.out.println("same runtime List class: " + sameRuntimeClass);
        System.out.println("instanceof List<?>: " + instanceofWildcard);
        System.out.println("Java 21 checked instanceof: " + instanceofParameterization);
        System.out.println("erased bound/return: " + erasedBound.getSimpleName() + "/" + erasedIdentity.getReturnType().getSimpleName());
        System.out.println("raw heap pollution: " + rawFailure);
        System.out.println("covariant array store: " + arrayFailure);
        System.out.println("reifiable List<?>[] element: " + reifiableArray[0].get(0));
        System.out.println("override through Parent<String>: " + overrideValue);
        System.out.println("CHECK PASSED");
    }
}
