// Research: docs/plans/CORE-INHERITANCE.md
package negative;
import base.Base;
// Exception is broader than IOException.
public class BadCheckedThrows extends Base {
 @Override public String read() throws Exception { return "wrong"; }
}
