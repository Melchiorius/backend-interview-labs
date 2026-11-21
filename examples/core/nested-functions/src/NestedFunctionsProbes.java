// Theory: knowledge/01-java-core/interfaces.md
// Research: docs/plans/CORE-NESTED-FUNCTIONS.md
// Java SE 21: one integrated example for nested classes, captures and lambdas.
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

public class NestedFunctionsProbes {
    private int value = 67;
    static class Nested { int get() { return 68; } }
    class Inner {
        static int count = 69;
        static { count++; }
        int getOuter() { return NestedFunctionsProbes.this.value; }
    }
    record Widget(int id) { }
    @FunctionalInterface interface Transform { int apply(int x); boolean equals(Object obj); }
    @FunctionalInterface interface Checked { void run() throws IOException; }
    static String choose(IntUnaryOperator op) { return "int"; }
    static String choose(LongUnaryOperator op) { return "long"; }
    static String receiver(AtomicInteger calls) { calls.incrementAndGet(); return "abc"; }

    void check() {
        System.out.println("P01=" + new Nested().get());
        Inner inner = this.new Inner();
        System.out.println("P02=" + inner.getOuter());
        System.out.println("P03=" + Inner.count);
        int captured = 67;
        class Local {
            static int marker = 70;
            static { marker++; }
            int read() { return captured + marker; }
        }
        System.out.println("P04=" + Local.marker);
        Runnable anonymousStatic = new Runnable() {
            static int marker = 72;
            static { marker++; }
            @Override public void run() { System.out.println("P05=" + marker); }
        };
        anonymousStatic.run();
        System.out.println("P06=" + new Local().read());
        Supplier<Boolean> anonymousThis = new Supplier<>() {
            @Override public Boolean get() { return (Object)this == NestedFunctionsProbes.this; }
        };
        Supplier<Boolean> lambdaThis = () -> this == NestedFunctionsProbes.this;
        System.out.println("P07=" + anonymousThis.get() + "," + lambdaThis.get());
        int[] box = {67};
        Supplier<Integer> capturedRef = () -> box[0];
        box[0] = 68;
        System.out.println("P08=" + capturedRef.get());
        IntUnaryOperator add = x -> x + 1;
        System.out.println("P09=" + add.applyAsInt(67));
        IntFunction<String> staticRef = String::valueOf;
        Supplier<String> boundRef = "hi"::toUpperCase;
        ToIntFunction<String> unboundRef = String::length;
        IntFunction<Widget> ctorRef = Widget::new;
        System.out.println("P10=" + staticRef.apply(67) + "," + boundRef.get() + "," + unboundRef.applyAsInt("ab") + "," + ctorRef.apply(67).id());
        String missing = null;
        boolean immediate = false, delayed = false;
        try { Supplier<String> badRef = missing::trim; }
        catch (NullPointerException e) { immediate = true; }
        Supplier<String> later = () -> missing.trim();
        try { later.get(); } catch (NullPointerException e) { delayed = true; }
        System.out.println("P11=" + immediate + "," + delayed);
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> eager = receiver(calls)::toUpperCase;
        eager.get(); eager.get(); int afterEager = calls.get();
        Supplier<String> deferred = () -> receiver(calls).toUpperCase();
        deferred.get(); deferred.get();
        System.out.println("P12=" + afterEager + "," + calls.get());
        System.out.println("P13=" + choose((IntUnaryOperator)(x -> x + 1)));
        Checked checked = () -> { throw new IOException("io"); };
        String result;
        try { checked.run(); result = "missed"; }
        catch (IOException e) { result = e.getMessage(); }
        System.out.println("P14=" + result);
        record LocalRecord(int n) { }
        System.out.println("P15=" + new LocalRecord(67).n());
        Supplier<Integer> anonymousCapture = new Supplier<>() {
            @Override public Integer get() { return captured; }
        };
        Supplier<Integer> lambdaCapture = () -> captured;
        System.out.println("P16=" + anonymousCapture.get() + "," + lambdaCapture.get());
        Transform transform = x -> x + 1;
        System.out.println("P17=" + transform.apply(67));
    }
    public static void main(String[] args) { new NestedFunctionsProbes().check(); }
}
