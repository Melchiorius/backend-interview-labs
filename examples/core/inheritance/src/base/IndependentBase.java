package base;

// Theory: knowledge/01-java-core/inheritance.md
// Research: docs/plans/CORE-INHERITANCE.md
public class IndependentBase {
    private static final int privateStatic() { return 67; }
    private final int privateInstance() { return 67; }
    static final int foreignStatic() { return 67; }

    public int callPrivateStatic() { return privateStatic(); }
    public int callPrivateInstance() { return privateInstance(); }
    public int callForeignStatic() { return foreignStatic(); }
}
