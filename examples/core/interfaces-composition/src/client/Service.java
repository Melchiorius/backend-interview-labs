package client;

import api.Policy;
import java.util.Objects;

// Theory: knowledge/01-java-core/classes.md
// Research: docs/plans/CORE-INTERFACES-COMPOSITION.md
// Composition: Service HAS a Policy, but does not extend an implementation.
public final class Service {
    private final Policy policy;

    public Service(Policy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public int execute(int value) {
        return policy.apply(value);
    }

    public String explain(int value) {
        return policy.describe(value);
    }
}
