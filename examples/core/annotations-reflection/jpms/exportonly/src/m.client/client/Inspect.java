// Theory: knowledge/01-java-core/annotations.md
// Theory: knowledge/01-java-core/reflection.md
package client;
import api.Vault;
import java.lang.reflect.Field;
public class Inspect {
    public static void main(String[] args) throws Exception {
        Vault vault = new Vault();
        System.out.println("JM01=" + Vault.class.getMethod("read").invoke(vault));
        Field field = Vault.class.getDeclaredField("secret");
        boolean enabled = field.trySetAccessible();
        System.out.println("JM02=" + enabled);
        try { System.out.println("JM03=" + field.getInt(vault)); }
        catch (IllegalAccessException e) { System.out.println("JM03=IllegalAccessException"); }
    }
}
