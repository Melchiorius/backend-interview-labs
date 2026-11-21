import java.lang.reflect.*;
import java.util.*;

public class GenericsProbes {
    static final class Box<T> {
        private final T value;
        Box(T value) { this.value = value; }
        T get() { return value; }
    }
    static final class NumberBox<T extends Number> {
        private final T value;
        NumberBox(T value) { this.value = value; }
        T get() { return value; }
    }
    static <T extends Number & Comparable<T>> int compare(T a,T b) { return a.compareTo(b); }
    static <T> void copyOne(List<? super T> target, List<? extends T> source) {
        target.set(0,source.get(0));
    }
    static void captureFirst(List<?> items) { captureHelper(items); }
    private static <T> void captureHelper(List<T> items) { items.set(1,items.get(0)); }
    static <T> T identity(T value) { return value; }
    static class Parent<T> { T get() { return null; } }
    static final class Child extends Parent<String> {
        @Override String get() { return "child"; }
    }
    @SafeVarargs static String unsafeFirst(List<String>... lists) {
        Object[] alias = lists;
        alias[0] = List.of(67);
        return lists[0].get(0);
    }
    public static void main(String[] args) throws Exception {
        Box<Integer> box = new Box<>(67);
        System.out.println("P01=" + box.get());
        System.out.println("P02=" + compare(67,68));
        List<Integer> src = List.of(67,68);
        List<Number> dst = new ArrayList<>(List.of(0,0));
        Collections.copy(dst,src);
        System.out.println("P03=" + dst);
        copyOne(dst,src);
        System.out.println("P04=" + dst.get(0));
        List<? extends Number> producer = new ArrayList<Integer>(List.of(67));
        Number num = producer.get(0);
        producer.add(null);
        System.out.println("P05=" + num + "," + producer.size());
        List<? super Integer> consumer = new ArrayList<Number>();
        consumer.add(67);
        Object readBack = consumer.get(0);
        System.out.println("P06=" + readBack + "," + readBack.getClass().getSimpleName());
        List<String> items = new ArrayList<>(List.of("a","b"));
        captureFirst(items);
        System.out.println("P07=" + items);
        System.out.println("P08=" + (new ArrayList<String>().getClass()==new ArrayList<Integer>().getClass()));
        Object generic = new ArrayList<String>();
        System.out.println("P09=" + (generic instanceof List<?>));
        System.out.println("P10=" + NumberBox.class.getDeclaredMethod("get").getReturnType().getSimpleName());
        System.out.println("P11=" + GenericsProbes.class.getDeclaredMethod("identity",Object.class).getReturnType().getSimpleName());
        try {
            List<String> typed = new ArrayList<>();
            List raw = typed;
            raw.add(67);
            String mustBeString = typed.get(0);
            System.out.println("P12=" + mustBeString);
        } catch (ClassCastException ex) { System.out.println("P12=ClassCastException"); }
        try { unsafeFirst(List.of("a")); System.out.println("P13=no-error"); }
        catch (ClassCastException ex) { System.out.println("P13=ClassCastException"); }
        try {
            Number[] values = new Integer[1];
            values[0]=3.14;
            System.out.println("P14=no-error");
        } catch (ArrayStoreException ex) { System.out.println("P14=ArrayStoreException"); }
        List<String> words=List.of("x");
        List<?> anything=words; List<? extends Object> upper=words;
        System.out.println("P15=" + anything.get(0) + "," + upper.get(0));
        List<?>[] wildcardArray = new List<?>[1];
        wildcardArray[0] = List.of(67);
        System.out.println("P16=" + wildcardArray[0].get(0));
        System.out.println("P17=" + Child.class.getDeclaredMethods().length + "," +
           Arrays.stream(Child.class.getDeclaredMethods()).filter(Method::isBridge).count() + "," + new Child().get());
        List<Integer> empty = Collections.emptyList();
        List<Integer> made = new ArrayList<>(); made.add(67);
        System.out.println("P18=" + empty.size() + "," + made.get(0));
        List<Number> destination = new ArrayList<>(List.of(0,0));
        Collections.copy(destination,List.of(67,68));
        System.out.println("P19=" + destination.get(1));
        List<Integer> ints = List.of(67);
        List<?> general = ints;
        System.out.println("P20=" + (general instanceof List<?>) + "," + general.get(0));
    }
}
