package api;

// Research: docs/plans/CORE-INTERFACES-COMPOSITION.md
public interface LeftOrigin {
    default String origin() {
        return "left";
    }
}
