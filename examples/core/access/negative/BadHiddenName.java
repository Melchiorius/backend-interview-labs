package client;

// Theory: knowledge/01-java-core/access.md
// Research: docs/plans/CORE-ACCESS.md
// INTENTIONALLY DOES NOT COMPILE: Hidden is package-private in api.
public class BadHiddenName {
    api.Hidden h;
}
