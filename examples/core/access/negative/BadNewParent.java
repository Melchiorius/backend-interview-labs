package client;

// Theory: knowledge/01-java-core/access.md
// Research: docs/plans/CORE-ACCESS.md

import api.Parent;

// INTENTIONALLY DOES NOT COMPILE: protected ctor cannot be invoked via plain new.
public class BadNewParent extends Parent {
    Parent bad() {
        return new Parent();
    }
}
