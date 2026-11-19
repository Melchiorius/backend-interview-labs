package client;

// Theory: knowledge/01-java-core/access.md
// Research: docs/plans/CORE-ACCESS.md

import api.Parent;

// INTENTIONALLY DOES NOT COMPILE: Parent is not this subclass or its subclass.
public class BadParentValue extends Parent {
    int bad(Parent parent) {
        return parent.value;
    }
}
