package client;

// Theory: knowledge/01-java-core/classes.md
// Theory: knowledge/01-java-core/types.md
// Theory: knowledge/01-java-core/conversions.md
// Theory: knowledge/01-java-core/parameters.md
// Research: docs/plans/CORE-INHERITANCE.md
// From repository root: bash examples/core/inheritance/verify.sh
import base.Base;
import java.io.IOException;

public final class InheritanceDemo {
    private InheritanceDemo() {}

    private static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }

    public static void main(String[] args) throws IOException {
        Base ref = new Derived();
        Derived derived = (Derived) ref;

        String byBaseType = ref.describe("67");
        String byDerivedType = derived.describe("67");
        check(byBaseType.equals("derived:object"), "selected Base.describe(Object), dispatched to Derived");
        check(byDerivedType.equals("derived:string"), "selected Derived.describe(String)");
        check(derived.callSuper().equals("base:object"), "super does not dispatch to Derived");
        check(derived.callThroughCast().equals("derived:object"), "cast does not suppress dispatch");
        check(ref.code == 67 && derived.code == 68 && derived.parentCode() == 67, "field hiding");
        check(Base.kind().equals("base:static") && Derived.kind().equals("derived:static"), "static hiding");
        check(ref.kind().equals("base:static"), "static call through reference uses declared type");
        check(Base.frozenKind().equals("base:final-static"), "final static method inherited");
        check(ref.contract().equals("base:final"), "final instance method inherited");
        check(ref.result().equals(68) && ref.result() instanceof Integer, "covariant override");
        check(ref.read().equals("derived:read"), "override with narrower checked throws");
        check(derived.callProtected().equals("base:protected"), "super protected call");
        check(ref.callPrivate().equals("base:private"), "private method not overridden");
        check(derived.privateNote().equals("derived:private"), "different private-name declaration");
        check(ref.callPackageOnly().equals("base:package"), "package method not overridden across packages");
        check(derived.packageOnly().equals("derived:package"), "new method in another package");

        System.out.println("Base reference + String argument: " + byBaseType);
        System.out.println("Derived reference + String argument: " + byDerivedType);
        System.out.println("super vs cast: " + derived.callSuper() + " / " + derived.callThroughCast());
        System.out.println("static method: " + Base.kind() + " / " + Derived.kind());
        System.out.println("field: " + ref.code + " / " + derived.code + " / " + derived.parentCode());
        System.out.println("covariant + throws: " + ref.result() + " / " + ref.read());
        System.out.println("private/package: " + ref.callPrivate() + " / " + ref.callPackageOnly());
        System.out.println("CHECK PASSED");
    }
}
