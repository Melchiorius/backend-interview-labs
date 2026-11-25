package client;

import api.Exported;
import api.InterfaceTypes;
import api.Neighbor;
import api.Parent;

// Theory: knowledge/01-java-core/access.md
// Theory: knowledge/01-java-core/classes.md
// Theory: knowledge/01-java-core/types.md
// Research: docs/plans/CORE-ACCESS.md
// From repository root:
// javac --release 21 -d examples/core/access/build/classes -sourcepath examples/core/access/src examples/core/access/src/client/AccessDemo.java
// java -cp examples/core/access/build/classes client.AccessDemo
public final class AccessDemo {
    private AccessDemo() {}

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        Child first = new Child();
        Child second = new Child();
        check(first.ownValue() == 67, "this.value within Child");
        check(first.otherChildValue(second) == 67, "otherChild.value within Child");
        check(first.touchUsingSuper() == 68, "super.touch() within Child");
        check(second.ownValue() == 67, "instances keep independent state");

        check(Child.inheritedTotal() == 67, "Parent.total from a subclass");
        check(Neighbor.readFromSamePackage() == 141,
                "same-package non-subclass reads protected and package-private");
        check(new Sibling().ownValue() == 67, "sibling accesses its own inherited member");

        // This class does NOT extend Parent: the anonymous subclass is the special
        // protected-constructor case under JLS 21 §6.6.2.2.
        Parent anonymous = new Parent() {};
        check(anonymous.publicValue() == 67, "anonymous subclass construction");

        InterfaceTypes.Member member = new InterfaceTypes.Member();
        check(member != null, "interface member class is implicitly public static");
        // Public inherited members can be selected through an accessible subclass.
        Exported e = new Exported();
        check(e.value == 67, "public inherited field");
        check(e.read() == 67, "public inherited method");
        Exported.Visible visible = new Exported.Visible();
        check(visible instanceof Exported.Visible && "Visible".equals(visible.label()),
                "public inherited member type");
        System.out.println("public_inherited=" + e.value + ":" + e.read() + ":"
                + visible.label());
        System.out.println("CHECK PASSED");
    }
}
