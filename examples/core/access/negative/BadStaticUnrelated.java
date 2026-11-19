package client;

// Research: docs/plans/CORE-ACCESS.md

import api.Parent;

// INTENTIONALLY DOES NOT COMPILE: protected static still requires permitted context.
public class BadStaticUnrelated {
    int bad() {
        return Parent.total;
    }
}
