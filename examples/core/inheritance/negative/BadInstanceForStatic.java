// Research: docs/plans/CORE-INHERITANCE.md
package negative;
import base.Base;
// Cannot replace a static method with an instance method.
public class BadInstanceForStatic extends Base {
 public String kind() { return "wrong"; }
}
