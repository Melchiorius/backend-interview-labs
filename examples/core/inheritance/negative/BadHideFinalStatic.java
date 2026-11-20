// Theory: knowledge/01-java-core/inheritance.md
// Research: docs/plans/CORE-INHERITANCE.md
package negative;
import base.Base;
// Cannot hide a final static method.
public class BadHideFinalStatic extends Base {
 public static String frozenKind() { return "changed"; }
}
