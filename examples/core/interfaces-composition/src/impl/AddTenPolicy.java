package impl;

// Theory: knowledge/01-java-core/composition.md
import api.Policy;

// Research: docs/plans/CORE-INTERFACES-COMPOSITION.md
public final class AddTenPolicy implements Policy {
    @Override
    public int apply(int value) {
        return Math.addExact(value, 10);
    }
}
