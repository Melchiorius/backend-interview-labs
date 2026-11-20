package api;

// Theory: knowledge/01-java-core/inheritance.md
// Research: docs/plans/CORE-INTERFACES-COMPOSITION.md
// Exactly one abstract method, even with a default and a private helper.
@FunctionalInterface
public interface Policy {
    int apply(int value);

    default String describe(int input) {
        int result = apply(input);
        return format(input, result);
    }

    private static String format(int input, int result) {
        return "input=" + input + " -> output=" + result;
    }
}
