// Theory: knowledge/01-java-core/types.md
// Theory: knowledge/01-java-core/conversions.md
// Theory: knowledge/01-java-core/parameters.md
// From repository root: java examples/core/ValuesAndReferencesDemo.java
public class ValuesAndReferencesDemo {
    private static void changeNumber(int number) {
        number = 100;
        System.out.println("Inside changeNumber: " + number);
    }

    private static void append(StringBuilder text) {
        text.append("B");
    }

    private static void replace(StringBuilder text) {
        text = new StringBuilder("C");
        System.out.println("Inside replace: " + text);
    }

    public static void main(String[] args) {
        int number = 67;
        changeNumber(number);
        System.out.println("Outside changeNumber: " + number);

        StringBuilder first = new StringBuilder("A");
        StringBuilder second = first;
        append(second);
        System.out.println("After append: " + first);

        replace(second);
        System.out.println("After replace: " + first);

        second = new StringBuilder("D");
        System.out.println("After reassigning second: " + first);

        if (number != 67 || !"AB".contentEquals(first)) {
            throw new AssertionError("Unexpected Java value/reference behavior");
        }
        System.out.println("CHECK PASSED");
    }
}
