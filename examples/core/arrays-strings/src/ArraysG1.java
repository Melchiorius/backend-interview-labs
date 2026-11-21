// Theory: knowledge/01-java-core/arrays.md
// Theory: knowledge/01-java-core/strings.md
// Research: docs/plans/CORE-ARRAYS-STRINGS.md (accepted G1 section 6.1)
import java.util.*;
import java.util.stream.*;

public class ArraysG1 {
    static void show(String id, Object value) { System.out.println(id + "=" + value); }
    static String fails(Runnable action) {
        try { action.run(); return "NO_ERROR"; }
        catch (RuntimeException ex) { return ex.getClass().getSimpleName(); }
    }
    @SafeVarargs // Deliberately false safety claim: demonstrates a runtime hazard, NOT recommended.
    static void unsafe(List<String>... lists) {
        Object[] alias = lists;
        alias[0] = List.of(67);
        String value = lists[0].get(0);
        show("A99", value); // never reached
    }
    public static void main(String[] args) {
        int[] ints = new int[2];
        String[] refs = new String[2];
        show("A01", Arrays.toString(ints) + "," + Arrays.toString(refs));
        show("A02", ints.length + "," + (ints instanceof Object));
        String[] strings = {"a", "b"};
        Object[] covariant = strings;
        show("A03", fails(() -> covariant[0] = 67));
        show("A04", covariant.getClass().getComponentType() == String.class);
        int[][] jagged = new int[2][];
        jagged[0] = new int[]{1, 2}; jagged[1] = new int[]{3};
        show("A05", jagged.length + "," + jagged[0].length + "," + jagged[1].length);
        int[][] copyOuter = jagged.clone();
        copyOuter[0][0] = 67;
        show("A06", (jagged != copyOuter) + "," + (jagged[0] == copyOuter[0]) + "," + jagged[0][0]);
        int[][] viaCopy = Arrays.copyOf(jagged, jagged.length);
        show("A07", (viaCopy[0] == jagged[0]) + "," + (viaCopy != jagged));
        int[] shortCopy = Arrays.copyOf(new int[]{4}, 3);
        show("A08", Arrays.toString(shortCopy));
        int[] overlapped = {1, 2, 3, 4};
        System.arraycopy(overlapped, 0, overlapped, 1, 3);
        show("A09", Arrays.toString(overlapped));
        show("A10", Arrays.equals(new int[]{1,2}, new int[]{1,2}));
        int[][] deepA={{1},{2}}, deepB={{1},{2}};
        show("A11", Arrays.equals(deepA, deepB) + "," + Arrays.deepEquals(deepA, deepB));
        show("A12", Arrays.deepToString(deepA));
        String[] source = {"x", "y"};
        List<String> fixed = Arrays.asList(source);
        fixed.set(0, "z"); source[1] = "q";
        show("A13", Arrays.toString(source) + "," + fixed);
        show("A14", fails(() -> fixed.add("t")));
        show("A15", Arrays.asList(new int[]{1,2}).size() + "," + Arrays.stream(new int[]{1,2}).sum());
        show("A16", Arrays.stream(new String[]{"p", "q"}).count());
        show("A17", fails(() -> { int n = ints[2]; }));
        show("A18", fails(() -> { int[] wrong = new int[-1]; }));
        show("A19", fails(() -> { int n = ((int[])null).length; }));
        show("A20", fails(() -> System.arraycopy(new Object[]{"ok", 67}, 0, new String[2], 0, 2)));
        show("A21", fails(() -> unsafe(List.of("ok"))));
        List<?>[] wildcard = new List<?>[1]; wildcard[0] = List.of(67);
        show("A22", wildcard[0].get(0));
        show("A23", fails(() -> List.of("x", null)));
        show("A24", new int[]{67}.clone().length);
    }
}
