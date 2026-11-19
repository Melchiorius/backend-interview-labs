package api;

// Theory: knowledge/01-java-core/classes.md
// Research: docs/plans/CORE-ACCESS.md
// Same-package code does NOT need to extend Parent to access protected/package members.
public final class Neighbor {
    private Neighbor() {}

    public static int readFromSamePackage() {
        Parent parent = new Parent(); // Protected constructor is accessible in api.
        return parent.value + parent.packageValue + Parent.total;
    }
}
