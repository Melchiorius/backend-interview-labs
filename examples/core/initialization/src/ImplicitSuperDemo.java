// Theory: knowledge/01-java-core/initialization.md
// Research: docs/plans/CORE-INIT.md
// Independent Java 21 contrast: zero actual arguments, not zero formal parameters.
public final class ImplicitSuperDemo {
    private ImplicitSuperDemo() {}

    static class Parent {
        final int count;

        Parent(int... xs) {
            count = xs.length;
        }
    }

    static final class Explicit extends Parent {
        Explicit() {} // Implicit super() in an explicitly declared constructor.
    }

    static final class Implicit extends Parent {} // Compiler-declared default constructor.

    public static void main(String[] args) {
        Explicit explicit = new Explicit();
        Implicit implicit = new Implicit();
        if (explicit.count != 0) throw new AssertionError("explicit constructor");
        if (implicit.count != 0) throw new AssertionError("default constructor");
        System.out.println(explicit.count);
        System.out.println(implicit.count);
    }
}
