// Theory: knowledge/01-java-core/inheritance.md
// Research: docs/plans/CORE-INHERITANCE.md
package negative;
import base.Base;
// Return type does not distinguish overloads.
public class BadReturnOnlyOverload {
 String convert(Object o) { return "x"; }
 int convert(Object o) { return 67; }
}
