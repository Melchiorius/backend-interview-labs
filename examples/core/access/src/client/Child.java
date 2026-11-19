package client;

import api.Parent;

// Theory: knowledge/01-java-core/classes.md
// Theory: knowledge/01-java-core/types.md
// Research: docs/plans/CORE-ACCESS.md
// Cross-package subclass: instance access depends on the qualifying STATIC type.
public final class Child extends Parent {
    public Child() {
        super(); // Legal use of Parent's protected constructor from a subclass.
    }

    public int ownValue() {
        return this.value;
    }

    public int otherChildValue(Child otherChild) {
        return otherChild.value; // Qualifier has static type Child: legal.
    }

    public int touchUsingSuper() {
        super.touch(); // A protected instance method is accessible here.
        return this.value;
    }

    public static int inheritedTotal() {
        return Parent.total; // Protected STATIC access has no instance-qualifier restriction.
    }
}
