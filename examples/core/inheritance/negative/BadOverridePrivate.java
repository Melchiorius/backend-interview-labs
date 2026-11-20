// Research: docs/plans/CORE-INHERITANCE.md
package negative;
import base.Base;
// Base.privateNote() is private and is not overridden.
public class BadOverridePrivate extends Base {
 @Override public String privateNote() { return "wrong"; }
}
