// Theory: knowledge/01-java-core/syntax.md
// Theory: knowledge/01-java-core/operators.md
public class OperatorsProbe {
    static boolean mark(int[] counter, boolean result) {
        counter[0]++;
        return result;
    }
    static int trace(StringBuilder order, String symbol, int value) {
        order.append(symbol);
        return value;
    }
    static int two(int a, int b) { return a * 10 + b; }

    public static void main(String[] args) {
        System.out.println("OP01=" + (1 + 2 * 3));
        System.out.println("OP02=" + ((1 + 2) * 3));
        int x = 67;
        int old = x++;
        System.out.println("OP03=" + old + ":" + x);
        int now = ++x;
        System.out.println("OP04=" + now + ":" + x);
        x = 4;
        x = x++;
        System.out.println("OP05=" + x);
        short s = Short.MAX_VALUE;
        s += 1;
        System.out.println("OP06=" + s);
        System.out.println("OP07=" + (Integer.MAX_VALUE + 1));
        System.out.println("OP08=" + (-7 / 3) + ":" + (-7 % 3));
        System.out.println("OP09=" + (1 << 33));
        System.out.println("OP10=" + (-1 >>> 1));
        System.out.println("OP11=" + (1L << 65));
        int[] c1 = {0};
        boolean sc = false && mark(c1, true);
        System.out.println("OP12=" + sc + ":" + c1[0]);
        int[] c2 = {0};
        boolean eager = false & mark(c2, true);
        System.out.println("OP13=" + eager + ":" + c2[0]);
        int[] c3 = {0};
        boolean or = true || mark(c3, false);
        System.out.println("OP14=" + or + ":" + c3[0]);
        int[] c4 = {0};
        boolean eagerOr = true | mark(c4, false);
        System.out.println("OP15=" + eagerOr + ":" + c4[0]);
        System.out.println("OP16=" + (1 + 2 + "x"));
        System.out.println("OP17=" + ("x" + 1 + 2));
        byte small = true ? 1 : 2;
        System.out.println("OP18=" + small);
        Number numeric = true ? 1 : 2.5;
        System.out.println("OP19=" + numeric + ":" + numeric.getClass().getSimpleName());
        int assign = 1;
        assign += (assign = 3);
        System.out.println("OP20=" + assign);
        StringBuilder order = new StringBuilder();
        int result = two(trace(order, "L", 6), trace(order, "R", 7));
        System.out.println("OP21=" + result + ":" + order);
        System.out.println("OP22=" + (Double.NaN == Double.NaN));
        System.out.println("OP23=" + Double.compare(-0.0, 0.0));
        int[] c5 = {0};
        int choose = true ? 67 : (c5[0]++);
        System.out.println("OP24=" + choose + ":" + c5[0]);
        int a = 1, b = 2, d = 3;
        a = b = d;
        System.out.println("OP25=" + a + ":" + b);
        System.out.println("OP26=" + (7 / 3) + ":" + (7 % 3));
        int[] side = {0};
        try {
            int zero = Integer.parseInt("0");
            int notValue = side[0]++ / zero;
            System.out.println(notValue);
        } catch (ArithmeticException ex) {
            System.out.println("OP27=ArithmeticException:" + side[0]);
        }
    }
}
