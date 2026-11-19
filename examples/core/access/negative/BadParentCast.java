package client;

// Theory: knowledge/01-java-core/access.md
// Research: docs/plans/CORE-ACCESS.md

import api.Parent;

// INTENTIONALLY DOES NOT COMPILE: a cast changes the static qualifier to Parent.
public class BadParentCast extends Parent {
    int bad(BadParentCast child) {
        return ((Parent) child).value;
    }
}
