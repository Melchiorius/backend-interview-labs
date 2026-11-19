// Theory: knowledge/01-java-core/conversions.md
// Theory: knowledge/01-java-core/types.md
// From repo root: java examples/core/ConversionsDemo.java
public class ConversionsDemo {
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static String select(long value) { return "long"; }
    private static String select(Integer value) { return "Integer"; }
    private static void takeByte(byte value) { }
    private static void accept(String value) { }
    private static void accept(Integer value) { }

    public static void main(String[] args) {
        byte literal = 67;
        final int constant = 67;
        byte fromConstant = constant;
        check(literal == fromConstant, "Constant narrowing in assignment");
        // takeByte(67); // compile error: int argument is not narrowed to byte
        takeByte((byte) 67);

        byte small = 60;
        small += 7;
        check(small == 67, "Compound assignment");
        // small = small + 1; // compile error: addition yields int

        Integer cachedA = 127;
        Integer cachedB = 127;
        check(cachedA == cachedB, "Boxing guarantee");
        Integer big = 1000;
        Integer anotherBig = 1000;
        check(big.equals(anotherBig), "Value comparison");
        check(big == 1000, "Unboxing on mixed primitive comparison");

        boolean npe = false;
        Integer absent = null;
        try {
            boolean unused = absent == 0;
        } catch (NullPointerException expected) {
            npe = true;
        }
        check(npe, "Null unboxing");

        check("long".equals(select(67)), "Widening before boxing");
        // accept(null); // compile error: String / Integer overload ambiguity
        accept((String) null); // cast disambiguates

        long wrong = Integer.MAX_VALUE + 1;
        long correct = (long) Integer.MAX_VALUE + 1;
        check(wrong == Integer.MIN_VALUE && correct == 2_147_483_648L,
                "Overflow occurs before assignment");
        check(3 / 2 == 1 && 3 / 2.0 == 1.5, "Numeric promotion");
        check((int) Double.NaN == 0, "NaN to int");
        check((int) Double.POSITIVE_INFINITY == Integer.MAX_VALUE,
                "Positive infinity to int");

        String interned = "Java";
        String literalConcat = "Ja" + "va";
        String different = new String("Java");
        check(interned == literalConcat && interned != different
                && interned == different.intern(), "String interning");

        System.out.println("cache 127: " + (cachedA == cachedB));
        System.out.println("mixed Integer/int: " + (big == 1000));
        System.out.println("overflow: " + wrong + ", correct: " + correct);
        System.out.println("CHECK PASSED");
    }
}
