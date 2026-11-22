// Theory: knowledge/01-java-core/records.md
// Theory: knowledge/01-java-core/immutability.md
// Research: docs/plans/CORE-RECORDS-IMMUTABLE.md (accepted G1 section 5.1)
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RecordsProbe {
    record Person(String name, int age) {
        static final String KIND = "person";
        Person(String name) { this(name, 0); }
        Person {
            name = Objects.requireNonNull(name).strip();
            if (age < 0) throw new IllegalArgumentException("age");
        }
        Person withName(String value) { return new Person(value, age); }
    }
    record ArrayBox(int[] values) {}
    record DoubleBox(double value) {}
    record GenericBox<T>(T value) implements java.io.Serializable {}
    record BadAccessor(int[] values) {
        @Override public int[] values() { return values.clone(); }
    }
    public static void main(String[] args) {
        Person p = new Person(" Ada ", 67);
        Person q = new Person("Ada", 67);
        System.out.println("R01=" + p.name() + ":" + p.age());
        System.out.println("R02=" + p.equals(q));
        System.out.println("R03=" + (p.hashCode() == q.hashCode()));
        System.out.println("R04=" + p.toString().contains("name=Ada"));
        System.out.println("R05=" + p.withName("Bob").name());
        try { new Person("X", -1); } catch (IllegalArgumentException ex) { System.out.println("R06=" + ex.getMessage()); }
        int[] first = {1, 2};
        int[] second = {1, 2};
        System.out.println("R07=" + new ArrayBox(first).equals(new ArrayBox(second)));
        System.out.println("R08=" + Arrays.equals(first, second));
        System.out.println("R09=" + new ArrayBox(first).equals(new ArrayBox(first)));
        System.out.println("R10=" + new DoubleBox(Double.NaN).equals(new DoubleBox(Double.NaN)));
        System.out.println("R11=" + new DoubleBox(-0.0).equals(new DoubleBox(+0.0)));
        GenericBox<String> generic = new GenericBox<>("ok");
        System.out.println("R12=" + generic.value());
        BadAccessor b = new BadAccessor(first);
        BadAccessor copy = new BadAccessor(b.values());
        System.out.println("R13=" + b.equals(copy)); // нарушен Record.copy invariant
        System.out.println("R14=" + (new GenericBox<String>(null).value() == null));
        System.out.println("R15=" + Person.KIND);
        System.out.println("R16=" + new Person(" Ed ").age());
    }
}
