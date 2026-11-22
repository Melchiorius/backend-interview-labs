// Theory: knowledge/01-java-core/modules.md
// Theory: knowledge/01-java-core/classloaders.md
module m.lib { exports lib.api; opens lib.internal to m.client; }
