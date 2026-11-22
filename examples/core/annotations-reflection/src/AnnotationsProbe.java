// Theory: knowledge/01-java-core/annotations.md
// Theory: knowledge/01-java-core/reflection.md
import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AnnotationsProbe {
    enum Importance { HIGH, LOW }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    @interface Family { String value(); }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Repeatable(Tags.class)
    @interface Tag { String value(); }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface Tags { Tag[] value(); }

    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.TYPE)
    @interface SourceMark {}

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE)
    @interface ClassMark {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Details {
        String value() default "default";
        int[] numbers() default {6, 7};
        Class<?> kind() default String.class;
        Importance importance() default Importance.HIGH;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    @interface TypeMark {}

    @Family("parent")
    @Tag("alpha")
    @Tag("beta")
    @Details
    @SourceMark
    @ClassMark
    static class Parent {
        @Tag("method")
        public String work() { return "work"; }
        @Override public String toString() { return "parent"; }
    }
    static class Child extends Parent {}

    @Family("interface")
    interface FamilyInterface {}
    static class Implementor implements FamilyInterface {}

    static class FieldHolder { @TypeMark String text; }

    @FunctionalInterface
    interface Op { int apply(int a); }

    public static void main(String[] args) throws ReflectiveOperationException {
        System.out.println("AN01=" + Parent.class.getAnnotation(Family.class).value());
        System.out.println("AN02=" + Child.class.getAnnotation(Family.class).value());
        System.out.println("AN03=" + (Child.class.getDeclaredAnnotation(Family.class) == null));
        System.out.println("AN04=" + (Implementor.class.getAnnotation(Family.class) == null));
        System.out.println("AN05=" + (Child.class.getAnnotation(Tag.class) == null));
        System.out.println("AN06=" + Parent.class.getDeclaredAnnotationsByType(Tag.class).length);
        System.out.println("AN07=" + Parent.class.getAnnotation(Tags.class).value().length);
        System.out.println("AN08=" + (Parent.class.getDeclaredAnnotation(Tag.class) == null));
        System.out.println("AN09=" + (Parent.class.getAnnotation(SourceMark.class) == null));
        System.out.println("AN10=" + (Parent.class.getAnnotation(ClassMark.class) == null));
        Details details = Parent.class.getAnnotation(Details.class);
        System.out.println("AN11=" + details.value());
        System.out.println("AN12=" + details.numbers()[0] + ":" + details.numbers()[1]);
        System.out.println("AN13=" + details.kind().getSimpleName());
        System.out.println("AN14=" + details.importance());
        Method work = Parent.class.getDeclaredMethod("work");
        System.out.println("AN15=" + work.getDeclaredAnnotation(Tag.class).value());
        Method toStringMethod = Parent.class.getDeclaredMethod("toString");
        System.out.println("AN16=" + (toStringMethod.getDeclaredAnnotation(Override.class) == null));
        Field field = FieldHolder.class.getDeclaredField("text");
        System.out.println("AN17=" + field.getAnnotatedType().isAnnotationPresent(TypeMark.class));
        System.out.println("AN18=" + (field.getAnnotation(TypeMark.class) == null));
        Op op = value -> value + 1;
        System.out.println("AN19=" + op.apply(67));
        System.out.println("AN20=" + Parent.class.isAnnotationPresent(Details.class));
        System.out.println("AN21=" + (Child.class.getDeclaredAnnotationsByType(Tag.class).length == 0));
        System.out.println("AN22=" + Parent.class.getAnnotationsByType(Tag.class)[0].value());
        System.out.println("AN23=" + (Parent.class.getAnnotation(Details.class).annotationType() == Details.class));
    }
}
