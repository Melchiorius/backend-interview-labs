// Research: docs/plans/CORE-INHERITANCE.md
package negative;
import base.Base;
// Cannot override a final instance method.
public class BadOverrideFinal extends Base {
 @Override public String contract() { return "changed"; }
}
