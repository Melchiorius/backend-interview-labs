// Theory: knowledge/01-java-core/modules.md
// Theory: knowledge/01-java-core/classloaders.md
import java.nio.file.Files;
import java.nio.file.Path;
public class LoaderProbe {
    static final class ChildFirstTokenLoader extends ClassLoader {
        private final byte[] bytes;
        ChildFirstTokenLoader(byte[] bytes) {
            super(ClassLoader.getSystemClassLoader());
            this.bytes = bytes.clone();
        }
        @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.equals("sample.Token")) return super.loadClass(name, resolve);
            synchronized (getClassLoadingLock(name)) {
                Class<?> defined = findLoadedClass(name);
                if (defined == null) defined = defineClass(name, bytes, 0, bytes.length);
                if (resolve) resolveClass(defined);
                return defined;
            }
        }
    }
    public static void main(String[] args) throws Exception {
        ClassLoader app = LoaderProbe.class.getClassLoader();
        System.out.println("CL01=" + (String.class.getClassLoader() == null));
        System.out.println("CL02=" + (app == ClassLoader.getSystemClassLoader()));
        System.out.println("CL03=" + (ClassLoader.getPlatformClassLoader() != null));
        Class<?> loaded = app.loadClass("sample.Lazy");
        System.out.println("CL04=" + loaded.getName());
        System.out.println("CL05=" + (Class.forName("sample.Lazy", false, app) == loaded));
        System.out.println("CL07=" + Class.forName("sample.Lazy", true, app).getSimpleName());
        System.out.println("CL08=" + (Class.forName("java.lang.String") == String.class));
        byte[] bytes = Files.readAllBytes(Path.of(args[0]));
        Class<?> a = new ChildFirstTokenLoader(bytes).loadClass("sample.Token");
        Class<?> b = new ChildFirstTokenLoader(bytes).loadClass("sample.Token");
        System.out.println("CL09=" + a.getName().equals(b.getName()));
        System.out.println("CL10=" + (a == b));
        System.out.println("CL11=" + (a.getClassLoader() != b.getClassLoader()));
        System.out.println("CL12=" + (a.getModule().isNamed()));
        System.out.println("CL13=" + (a.getConstructor().newInstance() instanceof sample.Token));
        try {
            b.cast(a.getConstructor().newInstance());
            System.out.println("CL14=unexpected");
        } catch (ClassCastException e) {
            System.out.println("CL14=ClassCastException");
        }
        System.out.println("CL15=" + (Thread.currentThread().getContextClassLoader() == app));
        Thread.currentThread().setContextClassLoader(a.getClassLoader());
        System.out.println("CL16=" + (Thread.currentThread().getContextClassLoader() == a.getClassLoader()));
        Thread.currentThread().setContextClassLoader(app);
        System.out.println("CL17=" + (a.getClassLoader().getParent() == app));
        System.out.println("CL18=" + (sample.Token.class != a));
        ClassLoader delegating = new ClassLoader(app) {};
        Class<?> shared = delegating.loadClass("sample.Token");
        System.out.println("CL19=" + (shared == sample.Token.class));
        System.out.println("CL20=" + (shared.getClassLoader() == app));
        System.out.println("CL21=" + (!LoaderProbe.class.getModule().isNamed()));
    }
}
