// Theory: knowledge/01-java-core/assertions.md
// Theory: knowledge/01-java-core/optional.md
import java.util.Objects;

public class AssertProbe {
    private static int conditionCalls;
    private static int detailCalls;
    private static int textCalls;
    private static boolean condition(boolean value) {
        conditionCalls++;
        return value;
    }
    private static String detail() {
        detailCalls++;
        return "expected-67";
    }
    private static String text() {
        textCalls++;
        return "required-message";
    }
    private static String legal(String value) {
        return Objects.requireNonNull(value, "value");
    }
    private static int age(int value) {
        if (value < 0) throw new IllegalArgumentException("negative-age");
        return value;
    }
    private static String state(boolean ready) {
        if (!ready) throw new IllegalStateException("not-ready");
        return "ready";
    }
    public static void main(String[] args) {
        boolean assertionEnabled = false;
        assert assertionEnabled = true;
        System.out.println("AS01=" + assertionEnabled);
        assert condition(true) : detail();
        System.out.println("AS02=" + conditionCalls + ":" + detailCalls);
        try {
            assert condition(false) : detail();
            System.out.println("AS03=skipped");
        } catch (AssertionError error) {
            System.out.println("AS03=AssertionError:" + error.getMessage());
        }
        System.out.println("AS04=" + conditionCalls + ":" + detailCalls);
        int sideEffects = 0;
        assert ++sideEffects == 1;
        System.out.println("AS05=" + sideEffects);
        String good = "ok";
        Objects.requireNonNull(good, text());
        System.out.println("AS06=" + textCalls);
        Objects.requireNonNull(good, AssertProbe::text);
        System.out.println("AS07=" + textCalls);
        try {
            Objects.requireNonNull(null, (java.util.function.Supplier<String>) AssertProbe::text);
        } catch (NullPointerException error) {
            System.out.println("AS08=NPE:" + error.getMessage() + ":" + textCalls);
        }
        try { legal(null); } catch (NullPointerException error) { System.out.println("AS09=NPE:" + error.getMessage()); }
        try { age(-1); } catch (IllegalArgumentException error) { System.out.println("AS10=IAE:" + error.getMessage()); }
        try { state(false); } catch (IllegalStateException error) { System.out.println("AS11=ISE:" + error.getMessage()); }
        try {
            assert false;
            System.out.println("AS12=skipped");
        } catch (AssertionError error) {
            System.out.println("AS12=AssertionError:" + (error.getMessage() == null));
        }
        System.out.println("AS13=" + age(67));
        System.out.println("AS14=" + state(true));
        System.out.println("AS15=" + AssertProbe.class.desiredAssertionStatus());
    }
}
