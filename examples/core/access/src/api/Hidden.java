package api;

// Theory: knowledge/01-java-core/access.md
// Research: docs/plans/CORE-ACCESS.md
// Package-private declaring class; its public members are inherited by Exported.
class Hidden {
    public int value = 67;

    public int read() {
        return value;
    }

    public static class Visible {
        public String label() {
            return "Visible";
        }
    }
}
