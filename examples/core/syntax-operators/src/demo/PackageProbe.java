// Theory: knowledge/01-java-core/syntax.md
// Theory: knowledge/01-java-core/operators.md
package demo;
import p.Helper;
import static p.Helper.greet;
import static p.Helper.VALUE;
public class PackageProbe {
    public static void main(String[] args) {
        System.out.println("PK01=" + greet() + ":" + VALUE);
        System.out.println("PK02=" + Helper.VALUE);
        System.out.println("PK03=" + java.util.List.of(6, 7).size());
    }
}
