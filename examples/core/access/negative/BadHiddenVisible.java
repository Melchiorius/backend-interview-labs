package client;

// Theory: knowledge/01-java-core/access.md
// Research: docs/plans/CORE-ACCESS.md
// INTENTIONALLY DOES NOT COMPILE: the qualifying type Hidden is inaccessible.
public class BadHiddenVisible {
    api.Hidden.Visible v;
}
