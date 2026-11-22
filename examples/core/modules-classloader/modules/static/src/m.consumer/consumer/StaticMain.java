// Theory: knowledge/01-java-core/modules.md
// Theory: knowledge/01-java-core/classloaders.md
package consumer;
public class StaticMain {
    public static void main(String[] args) {
        System.out.println("JS01=" + ModuleLayer.boot().findModule("m.opt").isPresent());
        System.out.println("JS02=" + StaticMain.class.getModule().isNamed());
    }
}
