import java.util.Arrays;
import java.util.List;

public class ControlProbe {
    enum Stage { NEW, RUNNING, DONE }
    @SuppressWarnings("fallthrough") // демонстрируем намеренный fall-through
    static String legacy(int n) {
        StringBuilder out = new StringBuilder();
        switch (n) {
            case 1: out.append("one");
            case 2: out.append("two"); break;
            default: out.append("other");
        }
        return out.toString();
    }
    static String arrow(int n) {
        StringBuilder out = new StringBuilder();
        switch (n) {
            case 1 -> out.append("one");
            case 2 -> out.append("two");
            default -> out.append("other");
        }
        return out.toString();
    }
    static String evaluate(Stage s) {
        return switch (s) {
            case NEW -> "new";
            case RUNNING -> {
                int local = 67;
                yield "run:" + local;
            }
            case DONE -> "done";
        };
    }
    static int traced() {
        int count = 0;
        outer: for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (j == 1) continue;
                if (i == 2) break outer;
                count++;
            }
        }
        return count;
    }
    static String statementWithoutDefault(int x) {
        String s = "unchanged";
        switch (x) { case 1: s = "one"; break; }
        return s;
    }
    static String danglingElse(boolean a, boolean b) {
        String value = "none";
        if (a)
            if (b) value = "inner-true";
            else value = "inner-false";
        return value;
    }
    static int sideEffects() {
        int x = 0;
        int y = 0;
        if (++x > 0 && ++y > 0) {}
        if (x > 2 && ++y > 0) {}
        return x * 10 + y;
    }
    static String nullSwitch(String input) {
        return switch (input) {
            case null -> "null-explicit";
            case "A" -> "A";
            default -> "other";
        };
    }
    public static void main(String[] args) {
        System.out.println("C01=" + danglingElse(false, true));
        System.out.println("C02=" + danglingElse(true, false));
        System.out.println("C03=" + legacy(1));
        System.out.println("C04=" + arrow(1));
        System.out.println("C05=" + legacy(2));
        System.out.println("C06=" + statementWithoutDefault(67));
        System.out.println("C07=" + evaluate(Stage.RUNNING));
        System.out.println("C08=" + evaluate(Stage.DONE));
        System.out.println("C09=" + traced());
        int w = 0;
        while (w < 3) w++;
        System.out.println("C10=" + w);
        int d = 0;
        do { d++; } while (d < 0);
        System.out.println("C11=" + d);
        int sum = 0;
        for (int v : List.of(1, 2, 3)) sum += v;
        System.out.println("C12=" + sum);
        System.out.println("C13=" + sideEffects());
        System.out.println("C14=" + nullSwitch(null));
        try {
            Stage s = null;
            String value = switch (s) {
                case NEW -> "new";
                case RUNNING -> "run";
                case DONE -> "done";
            };
            System.out.println(value);
        } catch (NullPointerException ex) { System.out.println("C15=NPE"); }
        int colonValue = switch (1) {
            case 1:
                yield 67;
            default:
                yield 0;
        };
        System.out.println("C16=" + colonValue);
        int hit = 0;
        for (int i = 0; i < 3; i++) { if (i == 1) continue; hit++; }
        System.out.println("C17=" + hit);
        int[] xs = {1, 2, 3};
        for (int item : xs) item += 67;
        System.out.println("C18=" + Arrays.toString(xs));
        int[] selector = {0};
        String once = switch (++selector[0]) { case 1 -> "first"; default -> "other"; };
        System.out.println("C19=" + once + ":" + selector[0]);
        int total = 0;
        next: for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (j == 1) continue next;
                total++;
            }
        }
        System.out.println("C20=" + total);
        int[] shortCircuit = {0};
        if (true || ++shortCircuit[0] > 0) { }
        System.out.println("C21=" + shortCircuit[0]);
    }
}
