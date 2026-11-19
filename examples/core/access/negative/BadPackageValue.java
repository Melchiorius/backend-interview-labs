package client;

// Research: docs/plans/CORE-ACCESS.md

import api.Parent;

// INTENTIONALLY DOES NOT COMPILE: package-private field is not inherited here.
public class BadPackageValue extends Parent {
    int bad() {
        return packageValue;
    }
}
