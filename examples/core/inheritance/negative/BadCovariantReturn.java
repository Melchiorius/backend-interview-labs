// Theory: knowledge/01-java-core/inheritance.md
// Research: docs/plans/CORE-INHERITANCE.md
package negative;
import base.Base;
// Object is wider than Number.
public class BadCovariantReturn extends Base {
 @Override public Object result() { return 67; }
}
