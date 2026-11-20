package base;

// Theory: knowledge/01-java-core/classes.md
// Theory: knowledge/01-java-core/access.md
// Theory: knowledge/01-java-core/types.md
// Theory: knowledge/01-java-core/inheritance.md
// Research: docs/plans/CORE-INHERITANCE.md
import java.io.IOException;

public class Base {
    public int code = 67;

    public String describe(Object value) { return "base:object"; }
    public static String kind() { return "base:static"; }
    public static final String frozenKind() { return "base:final-static"; }
    public final String contract() { return "base:final"; }
    public Number result() { return 67; }
    public String read() throws IOException { return "base:read"; }
    protected String source() { return "base:protected"; }

    private String privateNote() { return "base:private"; }
    String packageOnly() { return "base:package"; }
    public String callPrivate() { return privateNote(); }
    public String callPackageOnly() { return packageOnly(); }
}
