package client;

// Theory: knowledge/01-java-core/access.md
// Research: docs/plans/CORE-ACCESS.md

// INTENTIONALLY DOES NOT COMPILE: member types of interfaces cannot be protected.
public interface BadProtectedInterfaceMember {
    protected interface Nested {}
}
