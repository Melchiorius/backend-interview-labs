// Theory: knowledge/01-java-core/annotations.md
// Theory: knowledge/01-java-core/reflection.md
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;

public class ReflectionProbe {
    static class Parent {
        public int inheritedField = 7;
        public String inheritedMethod() { return "parent"; }
    }
    static class Lazy {
        static { System.out.println("RF18=static-init"); }
    }
    static boolean containsMethod(Method[] methods, String name) {
        return Arrays.stream(methods).anyMatch(method -> method.getName().equals(name));
    }
    static boolean containsField(Field[] fields, String name) {
        return Arrays.stream(fields).anyMatch(field -> field.getName().equals(name));
    }
    public static void main(String[] args) throws ReflectiveOperationException {
        Class<?> type = ReflectTarget.class;
        ReflectTarget obj = new ReflectTarget(67);
        System.out.println("RF01=" + (type == obj.getClass()));
        System.out.println("RF02=" + containsMethod(type.getMethods(), "inheritedMethod"));
        System.out.println("RF03=" + containsMethod(type.getDeclaredMethods(), "inheritedMethod"));
        System.out.println("RF04=" + containsMethod(type.getDeclaredMethods(), "hidden"));
        System.out.println("RF05=" + containsField(type.getFields(), "inheritedField"));
        System.out.println("RF06=" + containsField(type.getDeclaredFields(), "inheritedField"));
        System.out.println("RF07=" + containsField(type.getDeclaredFields(), "secret"));
        System.out.println("RF08=" + type.getConstructors().length + ":" + type.getDeclaredConstructors().length);
        Field privateField = type.getDeclaredField("secret");
        System.out.println("RF09=" + privateField.canAccess(obj));
        System.out.println("RF10=" + privateField.trySetAccessible());
        System.out.println("RF11=" + privateField.getInt(obj));
        Method add = type.getDeclaredMethod("add", int.class);
        System.out.println("RF12=" + add.invoke(obj, 5));
        Constructor<?> publicConstructor = type.getConstructor(int.class);
        System.out.println("RF13=" + ((ReflectTarget) publicConstructor.newInstance(67)).add(1));
        Constructor<?> privateConstructor = type.getDeclaredConstructor();
        System.out.println("RF14=" + privateConstructor.trySetAccessible());
        System.out.println("RF15=" + ((ReflectTarget) privateConstructor.newInstance()).add(1));
        try {
            type.getMethod("fail").invoke(obj);
        } catch (InvocationTargetException error) {
            System.out.println("RF16=" + error.getCause().getClass().getSimpleName() + ":" + error.getCause().getMessage());
        }
        try {
            type.getMethod("missing");
        } catch (NoSuchMethodException error) {
            System.out.println("RF17=NoSuchMethodException");
        }
        Class<?> unloaded = Class.forName(Lazy.class.getName(), false, ReflectionProbe.class.getClassLoader());
        System.out.println("RF17b=" + (unloaded == Lazy.class));
        System.out.println("RF17c=" + Class.forName(Lazy.class.getName()).getSimpleName());
        Method hidden = type.getDeclaredMethod("hidden");
        try { hidden.invoke(obj); }
        catch (IllegalAccessException error) { System.out.println("RF19=IllegalAccessException"); }
        System.out.println("RF20=" + hidden.trySetAccessible());
        System.out.println("RF21=" + hidden.invoke(obj));
        Method names = type.getDeclaredMethod("names");
        System.out.println("RF22=" + names.getReturnType().getSimpleName());
        Type generic = names.getGenericReturnType();
        System.out.println("RF23=" + (generic instanceof ParameterizedType));
        System.out.println("RF24=" + ((ParameterizedType) generic).getActualTypeArguments()[0].getTypeName());
        System.out.println("RF25=" + int.class.isPrimitive());
        System.out.println("RF26=" + String[].class.isArray());
        System.out.println("RF27=" + String[].class.getComponentType().getSimpleName());
        System.out.println("RF28=" + (Class.forName("java.lang.String") == String.class));
        System.out.println("RF29=" + Modifier.isPrivate(privateField.getModifiers()));
        Field jdkPrivate = String.class.getDeclaredField("value");
        System.out.println("RF30=" + jdkPrivate.trySetAccessible());
        try { jdkPrivate.setAccessible(true); }
        catch (java.lang.reflect.InaccessibleObjectException error) {
            System.out.println("RF31=InaccessibleObjectException");
        }
        System.out.println("RF32=" + Arrays.stream(type.getDeclaredMethods()).map(Method::getName).sorted().toList());
    }
}

class ReflectTarget extends ReflectionProbe.Parent {
    private int secret;
    public String label = "Java";
    public ReflectTarget(int value) { secret = value; }
    private ReflectTarget() { secret = 0; }
    public int add(int number) { return secret + number; }
    private String hidden() { return "hidden:" + secret; }
    public void fail() { throw new IllegalArgumentException("boom"); }
    public List<String> names() { return List.of("A", "B"); }
}
