package base;

// Theory: knowledge/01-java-core/inheritance.md
// Research: docs/plans/CORE-INHERITANCE.md
public class PrivateFinalBase {
    private final int instanceCode() { return 67; }
    private static final int staticCode() { return 67; }

    public int callInstanceCode() { return instanceCode(); }
    public int callStaticCode() { return staticCode(); }
}
