package client;

// Theory: knowledge/01-java-core/access.md
// Theory: knowledge/01-java-core/conversions.md
// Theory: knowledge/01-java-core/inheritance.md
// Research: docs/plans/CORE-INHERITANCE.md
import base.Base;
import java.io.FileNotFoundException;

public final class Derived extends Base {
    public int code = 68; // Hides Base.code; not overriding a field.

    @Override public String describe(Object value) { return "derived:object"; }
    public String describe(String value) { return "derived:string"; } // New overload.
    public static String kind() { return "derived:static"; } // Hides Base.kind().
    @Override public Integer result() { return 68; } // Covariant return.
    @Override public String read() throws FileNotFoundException { return "derived:read"; }

    public String callSuper() { return super.describe("67"); }
    public String callThroughCast() { return ((Base) this).describe("67"); }
    public int parentCode() { return super.code; }
    public String callProtected() { return super.source(); }

    // These methods do NOT override the inaccessible Base declarations.
    public String privateNote() { return "derived:private"; }
    public String packageOnly() { return "derived:package"; }
}
