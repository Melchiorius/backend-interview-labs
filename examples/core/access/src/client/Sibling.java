package client;

import api.Parent;

// Research: docs/plans/CORE-ACCESS.md
// This is a separate subclass of Parent, NOT a subclass of Child.
public final class Sibling extends Parent {
    public Sibling() {
        super();
    }

    public int ownValue() {
        return this.value;
    }
}
