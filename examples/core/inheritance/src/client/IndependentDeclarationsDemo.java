package client;

// Theory: knowledge/01-java-core/inheritance.md
// Research: docs/plans/CORE-INHERITANCE.md
import base.IndependentBase;

public final class IndependentDeclarationsDemo extends IndependentBase {
    // Independent declarations: neither override nor hide the inaccessible methods.
    public int privateStatic() { return 68; }
    public static int privateInstance() { return 68; }
    public int foreignStatic() { return 68; }

    private static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }

    public static void verify() {
        IndependentDeclarationsDemo child = new IndependentDeclarationsDemo();
        IndependentBase parent = child;
        int parentStatic = parent.callPrivateStatic();
        int childInstance = child.privateStatic();
        int parentInstance = parent.callPrivateInstance();
        int childStatic = IndependentDeclarationsDemo.privateInstance();
        check(parentStatic == 67, "private static final in parent");
        check(childInstance == 68, "independent instance in child");
        check(parentInstance == 67, "private final instance in parent");
        check(childStatic == 68, "independent static in child");
        System.out.println("private-final=" + parentStatic + ":" + childInstance + ":"
                + parentInstance + ":" + childStatic);

        // Direct cross-package subclass, not a transitive override example.
        int parentCode = parent.callForeignStatic();
        int childCode = child.foreignStatic();
        check(parentCode == 67, "direct foreign-package base static");
        check(childCode == 68, "independent foreign-package child instance");
        System.out.println("foreign_static=" + parentCode + ":" + childCode);
    }

    public static void main(String[] args) {
        verify();
    }
}
