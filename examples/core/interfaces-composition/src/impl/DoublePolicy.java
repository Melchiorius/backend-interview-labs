package impl;

// Theory: knowledge/01-java-core/composition.md
import api.Policy;

// Research: docs/plans/CORE-INTERFACES-COMPOSITION.md
public final class DoublePolicy implements Policy {
    @Override
    public int apply(int value) {
        return Math.multiplyExact(value, 2);
    }
}
