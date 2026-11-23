// Theory: knowledge/01-java-core/utilities.md
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public class UtilProbe {
    record Person(String name, int age) { public String toString() { return name + ":" + age; } }
    public static void main(String[] args) {
        System.out.println("UT01=" + Objects.equals(null, null));
        int[] x = {67, 1}, y = {67, 1};
        System.out.println("UT02=" + Objects.deepEquals(x, y));
        System.out.println("UT03=" + Objects.equals(x, y));
        System.out.println("UT04=" + Objects.hashCode(67) + ":" + Objects.hash(67));
        System.out.println("UT05=" + Objects.toString(null, "fallback"));
        try { Objects.requireNonNull(null, "required"); }
        catch (NullPointerException ex) { System.out.println("UT06=NullPointerException:" + ex.getMessage()); }
        int[] calls={0};
        Objects.requireNonNull("ok", () -> { calls[0]++; return "lazy"; });
        System.out.println("UT07=" + calls[0]);
        try { Objects.requireNonNull(null, () -> { calls[0]++; return "lazy"; }); }
        catch (NullPointerException ex) { System.out.println("UT08=" + calls[0] + ":" + ex.getMessage()); }
        System.out.println("UT09=" + Objects.checkIndex(2, 3));
        try { Objects.checkIndex(3,3); }
        catch (IndexOutOfBoundsException ex) { System.out.println("UT10=IndexOutOfBoundsException"); }
        System.out.println("UT11=" + Objects.checkFromToIndex(1,3,3));
        try { Objects.checkFromIndexSize(1,3,3); }
        catch (IndexOutOfBoundsException ex) { System.out.println("UT12=IndexOutOfBoundsException"); }
        List<Person> people = new ArrayList<>(List.of(new Person("Bob",30),new Person("Ada",67),new Person("Ada",20)));
        Comparator<Person> cmp = Comparator.comparing(Person::name).thenComparingInt(Person::age);
        people.sort(cmp);
        System.out.println("UT13=" + people);
        people.sort(cmp.reversed());
        System.out.println("UT14=" + people);
        List<String> nullable = new ArrayList<>(Arrays.asList("B",null,"A"));
        nullable.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
        System.out.println("UT15=" + nullable);
        nullable.sort(Comparator.nullsLast(Comparator.naturalOrder()));
        System.out.println("UT16=" + nullable);
        nullable.sort(Comparator.nullsFirst(Comparator.<String>naturalOrder()).reversed());
        System.out.println("UT17=" + nullable);
        try { Comparator.comparing(Person::name).compare(new Person(null,1),new Person("Ada",2)); }
        catch (NullPointerException ex) { System.out.println("UT18=NullPointerException"); }
        TreeSet<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.add("A"); caseInsensitive.add("a");
        System.out.println("UT19=" + caseInsensitive.size());
        TreeSet<String> odd = new TreeSet<>(Comparator.nullsFirst((Comparator<String>) null));
        odd.add("A"); odd.add("B"); odd.add(null);
        System.out.println("UT20=" + odd.size());
        System.out.println("UT21=" + Arrays.equals(x,y));
        Object[] nestedA = {new int[]{1,67}}, nestedB = {new int[]{1,67}};
        System.out.println("UT22=" + Arrays.deepEquals(nestedA,nestedB));
        System.out.println("UT23=" + Arrays.equals(nestedA,nestedB));
        System.out.println("UT24=" + Arrays.mismatch(new int[]{67,1},new int[]{67,2}));
        System.out.println("UT25=" + Integer.signum(Arrays.compare(new int[]{67,1},new int[]{67,2})));
        int[] sorted = {99,10,67}; Arrays.sort(sorted);
        System.out.println("UT26=" + Arrays.toString(sorted));
        System.out.println("UT27=" + Arrays.binarySearch(sorted,67));
        System.out.println("UT28=" + Arrays.binarySearch(sorted,68));
        String[] words = {"A", "B"}; List<String> view = Arrays.asList(words);
        words[0]="Z"; System.out.println("UT29=" + view);
        view.set(1,"Y"); System.out.println("UT30=" + Arrays.toString(words));
        try { view.add("C"); }
        catch (UnsupportedOperationException ex) { System.out.println("UT31=UnsupportedOperationException"); }
        System.out.println("UT32=" + Arrays.asList(new int[]{1,67}).size());
        try { List.of("A",null); }
        catch (NullPointerException ex) { System.out.println("UT33=NullPointerException"); }
        try { List.of("A").set(0,"B"); }
        catch (UnsupportedOperationException ex) { System.out.println("UT34=UnsupportedOperationException"); }
        List<String> source = new ArrayList<>(List.of("A"));
        List<String> unmodView = Collections.unmodifiableList(source);
        List<String> snapshot = List.copyOf(source);
        source.set(0,"B");
        System.out.println("UT35=" + unmodView + ":" + snapshot);
        StringBuilder element = new StringBuilder("A");
        List<StringBuilder> copied = List.copyOf(List.of(element));
        element.append("67");
        System.out.println("UT36=" + copied.get(0));
        System.out.println("UT37=" + new HashSet<>(Arrays.asList("A","A")).size());
        String[] ordered = {"bbb","a","cc"};
        Arrays.sort(ordered, Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
        System.out.println("UT38=" + Arrays.toString(ordered));
    }
}
