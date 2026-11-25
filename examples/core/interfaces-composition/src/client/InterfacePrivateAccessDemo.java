package client;

// Theory: knowledge/01-java-core/interfaces.md
// Research: docs/plans/CORE-INTERFACES-COMPOSITION.md
public final class InterfacePrivateAccessDemo {
    private InterfacePrivateAccessDemo() {}

    public interface Policy {
        int apply(int value);
        private String helper() { return "policy"; }
    }

    public static void main(String[] args) {
        Policy policy = value -> value;
        // Access is permitted from the enclosing top-level type, with a receiver.
        String result = policy.helper();
        if (!"policy".equals(result)) throw new AssertionError("nested private helper");
        System.out.println("nestPrivate=" + result);
    }
}
