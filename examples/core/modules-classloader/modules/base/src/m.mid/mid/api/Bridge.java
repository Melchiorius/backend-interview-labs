// Theory: knowledge/01-java-core/modules.md
// Theory: knowledge/01-java-core/classloaders.md
package mid.api;
import lib.api.Greeter;
public final class Bridge {
    private Bridge() {}
    public static String bridge() { return "bridge:" + Greeter.hello(); }
}
