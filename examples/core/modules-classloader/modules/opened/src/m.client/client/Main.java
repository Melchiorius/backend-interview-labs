// Theory: knowledge/01-java-core/modules.md
// Theory: knowledge/01-java-core/classloaders.md
package client;
import lib.api.Greeter;
import mid.api.Bridge;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
public class Main {
    public static void main(String[] args) throws Exception {
        Module self = Main.class.getModule();
        Module library = Greeter.class.getModule();
        System.out.println("JM01=" + Bridge.bridge());
        System.out.println("JM02=" + Greeter.hello());
        System.out.println("JM03=" + self.canRead(library));
        System.out.println("JM04=" + library.isExported("lib.api", self));
        System.out.println("JM05=" + library.isExported("lib.internal", self));
        System.out.println("JM06=" + library.isOpen("lib.internal", self));
        Class<?> hidden = Class.forName("lib.internal.Hidden");
        Constructor<?> constructor = hidden.getConstructor();
        Field field = hidden.getDeclaredField("secret");
        boolean ctorOpen = constructor.trySetAccessible();
        boolean fieldOpen = field.trySetAccessible();
        System.out.println("JM07=" + ctorOpen);
        System.out.println("JM08=" + fieldOpen);
        if (ctorOpen && fieldOpen) System.out.println("JM09=" + field.getInt(constructor.newInstance()));
        else System.out.println("JM09=blocked");
        System.out.println("JM10=" + (Main.class.getClassLoader() == Greeter.class.getClassLoader()));
        System.out.println("JM11=" + self.isNamed());
    }
}
