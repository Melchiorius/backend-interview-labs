// Theory: knowledge/01-java-core/parameters.md
// Theory: knowledge/01-java-core/types.md
// Theory: knowledge/01-java-core/conversions.md
// From repo root: java examples/core/ParametersDemo.java
public class ParametersDemo {
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static void changeNumber(int number) { number = 100; }
    private static void append(StringBuilder target) { target.append("B"); }
    private static void replace(StringBuilder target) { target = new StringBuilder("C"); }
    private static void changeArray(int[] values) { values[0] = 67; }
    private static void replaceArray(int[] values) { values = new int[] {100}; }
    private static void modifyText(String value) { value += "B"; }
    private static void modifyBox(Integer value) { value++; }
    private static void acceptNull(StringBuilder target) { target = new StringBuilder("N"); }
    private static String choose(Object value) { return "Object"; }
    private static String choose(String value) { return "String"; }
    private static void mutateVarargs(int... values) {
        if (values.length > 0) values[0] = 67;
    }
    private static void swap(StringBuilder left, StringBuilder right) {
        StringBuilder tmp = left;
        left = right;
        right = tmp;
    }
    private static int next(StringBuilder trace, int number) {
        trace.append(number);
        return number;
    }
    private static void two(int first, int second) { }

    public static void main(String[] args) {
        int number = 67;
        changeNumber(number);
        check(number == 67, "Primitive argument");

        StringBuilder text = new StringBuilder("A");
        append(text);
        check("AB".contentEquals(text), "Mutated shared object");
        replace(text);
        check("AB".contentEquals(text), "Reassigned parameter");

        int[] values = {1, 2};
        changeArray(values);
        check(values[0] == 67, "Mutated shared array");
        replaceArray(values);
        check(values[0] == 67 && values.length == 2, "Reassigned array parameter");

        String original = "A";
        modifyText(original);
        check("A".equals(original), "Immutable String");
        Integer boxed = 67;
        modifyBox(boxed);
        check(boxed == 67, "Reboxed Integer");
        acceptNull(null);

        int[] passed = {1, 2};
        mutateVarargs(passed);
        check(passed[0] == 67, "Explicit array to varargs");
        mutateVarargs(1, 2); // array created for this call

        StringBuilder first = new StringBuilder("X");
        StringBuilder second = new StringBuilder("Y");
        swap(first, second);
        check("X".contentEquals(first) && "Y".contentEquals(second),
                "Swapped only local parameters");

        Object arg = "Java";
        check("Object".equals(choose(arg)), "Static overload selection");
        check("String".equals(choose("Java")), "Specific overload");

        StringBuilder trace = new StringBuilder();
        two(next(trace, 1), next(trace, 2));
        check("12".contentEquals(trace), "Left-to-right argument evaluation");

        System.out.println("number=" + number + ", text=" + text + ", array[0]=" + values[0]);
        System.out.println("varargs original array[0]=" + passed[0] + ", overload=" + choose(arg));
        System.out.println("CHECK PASSED");
    }
}
