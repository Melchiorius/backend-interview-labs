package api;

// Research: docs/plans/CORE-INTERFACES-COMPOSITION.md
public interface RightOrigin {
    default String origin() {
        return "right";
    }
}
