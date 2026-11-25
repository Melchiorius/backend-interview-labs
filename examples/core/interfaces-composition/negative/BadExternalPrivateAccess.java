package client;

// Theory: knowledge/01-java-core/interfaces.md
// Research: docs/plans/CORE-INTERFACES-COMPOSITION.md
// INTENTIONALLY DOES NOT COMPILE: a different top-level type cannot access helper().
public class BadExternalPrivateAccess {
    static String read(InterfacePrivateAccessDemo.Policy policy) {
        return policy.helper();
    }
}
