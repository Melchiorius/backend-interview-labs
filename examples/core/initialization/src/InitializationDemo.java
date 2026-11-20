/**
 * CORE-INIT: one Java SE 21 run through class and instance initialization.
 * Theory: knowledge/01-java-core/classes.md (fields/constructors) and
 * knowledge/01-java-core/inheritance.md (overriding/dynamic dispatch).
 */
public final class InitializationDemo {
    private InitializationDemo() {
    }

    private static void log(String event) {
        System.out.println(event);
    }

    private static int markInt(String event, int value) {
        log(event);
        return value;
    }

    private static Integer markInteger(String event, int value) {
        log(event);
        return value;
    }

    private static String markString(String event, String value) {
        log(event);
        return value;
    }

    interface RootDefault {
        int MARK = InitializationDemo.markInt("RootDefault.<clinit>", 1);

        default void rootDefault() {
        }
    }

    interface LeafDefault extends RootDefault {
        int MARK = InitializationDemo.markInt("LeafDefault.<clinit>", 1);

        default void leafDefault() {
        }
    }

    interface QuietParent {
        int MARK = InitializationDemo.markInt("QuietParent.<clinit>", 1);
    }

    interface QuietChild extends QuietParent {
        int MARK = InitializationDemo.markInt("QuietChild.<clinit>", 1);
    }

    static class Parent {
        static final int CONSTANT = 67;
        static int staticValue = markInt("Parent.static field", 67);
        static final Integer BOXED = markInteger("Parent.BOXED field", 67);

        static {
            log("Parent.static block");
        }

        int parentValue = markInt("Parent.instance field", 67);

        {
            log("Parent.instance block");
        }

        Parent() {
            log("Parent.constructor: before virtual call");
            observe();
            log("Parent.constructor: after virtual call");
        }

        void observe() {
        }
    }

    static final class Child extends Parent implements LeafDefault, QuietChild {
        static int OWN = markInt("Child.static field", 68);

        static {
            log("Child.static block");
        }

        int childValue = markInt("Child.instance field", 68);
        String childLabel = markString("Child.instance reference field", "ready");

        {
            log("Child.instance block");
        }

        Child() {
            this(67);
            log("Child() constructor body");
        }

        Child(int seed) {
            super();
            log("Child(int) constructor body: seed=" + seed);
        }

        @Override
        void observe() {
            log("Child.override: childValue=" + childValue + ", childLabel=" + childLabel);
        }
    }

    public static void main(String[] args) {
        log("== 1. Compile-time constant ==");
        log("Child.CONSTANT = " + Child.CONSTANT);

        log("== 2. Inherited non-constant static field ==");
        log("Child.BOXED = " + Child.BOXED);

        log("== 3. Child class initialization ==");
        log("Child.OWN = " + Child.OWN);

        log("== 3a. Direct interface initialization ==");
        log("QuietChild.MARK = " + QuietChild.MARK);

        log("== 4. Instance construction ==");
        Child child = new Child();

        log("== 5. Final object state ==");
        log("state=" + child.parentValue + "/" + child.childValue + "/" + child.childLabel);
    }
}
