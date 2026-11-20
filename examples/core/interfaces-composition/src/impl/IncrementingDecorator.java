package impl;

import api.Policy;
import java.util.Objects;

// Theory: knowledge/01-java-core/classes.md
// Research: docs/plans/CORE-INTERFACES-COMPOSITION.md
// A wrapper implements the same interface and explicitly forwards to its delegate.
public final class IncrementingDecorator implements Policy {
    private final Policy delegate;

    public IncrementingDecorator(Policy delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public int apply(int value) {
        return Math.addExact(delegate.apply(value), 1);
    }
}
