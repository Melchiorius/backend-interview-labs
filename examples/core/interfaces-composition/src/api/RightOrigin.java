package api;

// Theory: knowledge/01-java-core/interfaces.md
// Research: docs/plans/CORE-INTERFACES-COMPOSITION.md
public interface RightOrigin {
    default String origin() {
        return "right";
    }
}
