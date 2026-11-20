// Theory: knowledge/01-java-core/inheritance.md
// Research: docs/plans/CORE-INHERITANCE.md
package negative;
import base.Base;
// Overload does not override.
public class BadOverrideOverload extends Base {
 @Override public String describe(String value) { return "wrong"; }
}
