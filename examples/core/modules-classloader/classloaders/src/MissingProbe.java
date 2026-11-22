// Theory: knowledge/01-java-core/modules.md
// Theory: knowledge/01-java-core/classloaders.md
public class MissingProbe {
    public static void main(String[] args) {
        try { Class.forName("absent.DoesNotExist"); }
        catch (ClassNotFoundException e) { System.out.println("CE01=ClassNotFoundException"); }
        try { Class.forName("sample.Dependent", false, MissingProbe.class.getClassLoader()); }
        catch (NoClassDefFoundError e) { System.out.println("CE02=NoClassDefFoundError"); }
        catch (ClassNotFoundException e) { System.out.println("CE02=unexpected-CNFE"); }
    }
}
