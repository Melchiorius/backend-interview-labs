// Research: docs/plans/CORE-INHERITANCE.md
package negative;
import base.Base;
// protected cannot be reduced to package access.
public class BadNarrowAccess extends Base {
 @Override String source() { return "wrong"; }
}
