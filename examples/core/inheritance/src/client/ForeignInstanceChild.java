package client;

// Theory: knowledge/01-java-core/inheritance.md
// Research: docs/plans/CORE-INHERITANCE.md
import base.ForeignStaticBase;

// Direct subclass in a different package: the package-private method is inaccessible.
public final class ForeignInstanceChild extends ForeignStaticBase {
    public int code() { return 68; } // Independent declaration, not override/hide.
}
