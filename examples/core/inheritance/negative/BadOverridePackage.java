// Research: docs/plans/CORE-INHERITANCE.md
package negative;
import base.Base;
// Base.packageOnly() is package-private in a different package.
public class BadOverridePackage extends Base {
 @Override public String packageOnly() { return "wrong"; }
}
