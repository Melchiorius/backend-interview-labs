package api;

// Theory: knowledge/01-java-core/classes.md
// Research: docs/plans/CORE-ACCESS.md
// Base class for the package-access and protected-access examples.
public class Parent {
    protected int value = 67;          // State of this particular object.
    protected static int total = 67;   // One field belonging to the class.
    int packageValue = 7;              // Only code in package api may use it directly.

    protected Parent() {}

    protected void touch() {
        value++;
    }

    public int publicValue() {
        return value;
    }
}
