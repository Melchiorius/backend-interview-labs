package client;

// Research: docs/plans/CORE-ACCESS.md

import api.Parent;

// INTENTIONALLY DOES NOT COMPILE: Sibling does not extend BadSibling.
public class BadSibling extends Parent {
    int bad(Sibling sibling) {
        return sibling.value;
    }
}
