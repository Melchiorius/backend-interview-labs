// Theory: knowledge/01-java-core/inheritance.md
// Research: docs/plans/CORE-INHERITANCE.md
package negative;
import base.Base;
// Cannot use static for an inherited instance method.
public class BadStaticForInstance extends Base {
 public static String describe(Object value) { return "wrong"; }
}
