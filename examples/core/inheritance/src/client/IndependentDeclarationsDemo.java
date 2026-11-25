package client;

// Theory: knowledge/01-java-core/inheritance.md
// Research: docs/plans/CORE-INHERITANCE.md
import base.PrivateFinalBase;
import base.ForeignStaticBase;

public final class IndependentDeclarationsDemo extends PrivateFinalBase {
    // Neither declaration overrides or hides a private superclass method.
    public static int instanceCode() { return 68; }
    public int staticCode() { return 68; }

    private static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }

    public static void verify() {
        IndependentDeclarationsDemo child = new IndependentDeclarationsDemo();
        PrivateFinalBase parent = child;
        int parentInstance = parent.callInstanceCode();
        int childStatic = IndependentDeclarationsDemo.instanceCode();
        int parentStatic = parent.callStaticCode();
        int childInstance = child.staticCode();
        check(parentInstance == 67, "private final instance in parent");
        check(childStatic == 68, "independent static in child");
        check(parentStatic == 67, "private static final in parent");
        check(childInstance == 68, "independent instance in child");
        System.out.println("private-final=" + parentInstance + ":" + childStatic + ":"
                + parentStatic + ":" + childInstance);

        ForeignInstanceChild foreignChild = new ForeignInstanceChild();
        ForeignStaticBase foreignParent = foreignChild;
        int parentCode = foreignParent.run();
        int childCode = foreignChild.code();
        check(parentCode == 67, "direct foreign-package base static");
        check(childCode == 68, "independent foreign-package child instance");
        System.out.println("foreign_static=" + parentCode + ":" + childCode);
    }

    public static void main(String[] args) {
        verify();
    }
}
