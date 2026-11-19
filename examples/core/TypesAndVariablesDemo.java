// Theory: knowledge/01-java-core/types.md
// From repository root: java examples/core/TypesAndVariablesDemo.java
public class TypesAndVariablesDemo {
    static int classCounter;
    int instanceCounter;
    String name;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        int original = 67;
        int copy = original;
        copy = 68;
        check(original == 67 && copy == 68, "Primitive copy");

        StringBuilder first = new StringBuilder("A");
        StringBuilder second = first;
        second.append("B");
        check(first == second && "AB".contentEquals(first), "Reference alias");
        second = new StringBuilder("C");
        check(first != second && "AB".contentEquals(first), "Reference reassignment");

        TypesAndVariablesDemo object = new TypesAndVariablesDemo();
        int[] numbers = new int[2];
        String[] names = new String[2];
        check(classCounter == 0 && object.instanceCounter == 0 && object.name == null,
                "Default field values");
        check(numbers[0] == 0 && names[0] == null, "Default array values");

        var builder = new StringBuilder("Java");
        builder.append(" 21");
        check("Java 21".contentEquals(builder), "Inferred local type");

        int wrapped = Integer.MAX_VALUE + 1;
        long widenedBeforeAddition = (long) Integer.MAX_VALUE + 1;
        check(wrapped == Integer.MIN_VALUE && widenedBeforeAddition == 2_147_483_648L,
                "Overflow and promotion");

        int codePoint = 0x1F600;
        String symbol = new String(Character.toChars(codePoint));
        check(symbol.length() == 2 && symbol.codePointCount(0, symbol.length()) == 1,
                "UTF-16 code units");

        final StringBuilder fixedReference = new StringBuilder("A");
        fixedReference.append("B");
        check("AB".contentEquals(fixedReference), "final is not immutable");

        System.out.println("primitive: " + original + ", reference: " + first);
        System.out.println("overflow: " + wrapped + ", widened: " + widenedBeforeAddition);
        System.out.println("UTF-16 length: " + symbol.length() + ", code points: "
                + symbol.codePointCount(0, symbol.length()));
        System.out.println("CHECK PASSED");
    }
}
