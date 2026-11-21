// Theory: knowledge/01-java-core/records.md
// Theory: knowledge/01-java-core/immutability.md
// Research: docs/plans/CORE-RECORDS-IMMUTABLE.md (accepted G1 section 5.2)
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ImmutableProbe {
    record Unsafe(List<String> names) {}
    record Safe(List<String> names) {
        Safe { names = List.copyOf(names); }
    }
    static final class Numbers {
        private final int[] data;
        Numbers(int[] data) {
            if (data.length == 0) throw new IllegalArgumentException("empty");
            this.data = data.clone();
        }
        int[] data() { return data.clone(); }
        int first() { return data[0]; }
    }
    static final class MutableElement { int n; MutableElement(int n) { this.n = n; } }
    public static void main(String[] args) {
        List<String> source = new ArrayList<>(List.of("A"));
        Unsafe unsafe = new Unsafe(source);
        Safe safe = new Safe(source);
        List<String> view = Collections.unmodifiableList(source);
        source.add("B");
        System.out.println("I01=" + unsafe.names());
        System.out.println("I02=" + safe.names());
        System.out.println("I03=" + view);
        try { safe.names().add("C"); } catch (UnsupportedOperationException e) { System.out.println("I04=UOE"); }
        try { List.copyOf(Arrays.asList("A", null)); } catch (NullPointerException e) { System.out.println("I05=NPE"); }
        int[] input = {67};
        Numbers numbers = new Numbers(input);
        input[0] = 1;
        numbers.data()[0] = 2;
        System.out.println("I06=" + numbers.first());
        MutableElement element = new MutableElement(1);
        List<MutableElement> shallow = List.copyOf(List.of(element));
        element.n = 67;
        System.out.println("I07=" + shallow.get(0).n);
        System.out.println("I08=" + (source == safe.names())); // наблюдение, не универсальная гарантия identity
        System.out.println("I09=" + Arrays.toString(Arrays.copyOf(new int[]{1}, 3)));
        try { new Numbers(new int[0]); } catch (IllegalArgumentException e) { System.out.println("I10=" + e.getMessage()); }
    }
}
