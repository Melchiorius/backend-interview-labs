package api;

// Theory: knowledge/01-java-core/access.md
// Research: docs/plans/CORE-ACCESS.md
// Member types declared in an interface are implicitly public static (JLS 21 §9.5).
public interface InterfaceTypes {
    class Member {}
    interface Nested {}
}
