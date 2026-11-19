package client;

// Theory: knowledge/01-java-core/access.md
// Research: docs/plans/CORE-ACCESS.md

import api.Parent;

// INTENTIONALLY DOES NOT COMPILE: package-private field is not inherited here.
public class BadPackageValue extends Parent {
    int bad() {
        return packageValue;
    }
}
