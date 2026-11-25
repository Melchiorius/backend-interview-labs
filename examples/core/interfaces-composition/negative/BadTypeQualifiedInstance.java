package client;

// Theory: knowledge/01-java-core/interfaces.md
// Research: docs/plans/CORE-INTERFACES-COMPOSITION.md
// INTENTIONALLY DOES NOT COMPILE: the private member is accessible here, but needs a receiver.
public class BadTypeQualifiedInstance {
    public interface Policy {
        private String helper() { return "policy"; }
    }

    String read() {
        return Policy.helper();
    }
}
