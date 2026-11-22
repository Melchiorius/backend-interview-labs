// Theory: knowledge/01-java-core/syntax.md
// Theory: knowledge/01-java-core/operators.md
import static java.lang.Math.max;
import java.util.List;
import java.util.function.IntSupplier;

public class SyntaxProbe {
    static int value = 67;
    private int balance = 9;

    static String shadow() {
        int value = 7;
        int inside;
        {
            int inner = value + 1;
            inside = inner;
        }
        return value + ":" + SyntaxProbe.value + ":" + inside;
    }

    int fieldShadow(int balance) {
        return balance + this.balance;
    }

    static int capture() {
        int stable = 67;
        IntSupplier supplier = () -> stable + 1;
        return supplier.getAsInt();
    }

    static String blocks(boolean chosen) {
        String answer;
        if (chosen) {
            answer = "yes";
        } else {
            answer = "no";
        }
        return answer;
    }

    public static void main(String[] args) {
        System.out.println("SY01=" + shadow());
        System.out.println("SY02=" + new SyntaxProbe().fieldShadow(5));
        System.out.println("SY03=" + max(6, 7));
        System.out.println("SY04=" + List.of("x", "y").size());
        System.out.println("SY05=" + capture());
        System.out.println("SY06=" + blocks(false));
        System.out.println("SY07=" + blocks(true));
        var words = List.of("Java", "21");
        System.out.println("SY08=" + words.get(0) + words.get(1));
        System.out.println("SY09=" + java.util.Map.of("k", 67).get("k"));
        System.out.println("SY10=" + (args.length == 0));
        System.out.println("SY11=" + SyntaxProbe.class.getSimpleName());
        System.out.println("SY12=" + ("hello" instanceof String));
        int outer = 5;
        { int inner = outer + 2; System.out.println("SY13=" + inner); }
        System.out.println("SY14=" + outer);
    }
}
