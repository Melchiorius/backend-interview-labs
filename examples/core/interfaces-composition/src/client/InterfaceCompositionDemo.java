package client;

// Theory: knowledge/01-java-core/inheritance.md
// Theory: knowledge/01-java-core/access.md
// Research: docs/plans/CORE-INTERFACES-COMPOSITION.md
import api.LeftOrigin;
import api.RightOrigin;
import impl.AddTenPolicy;
import impl.DoublePolicy;
import impl.IncrementingDecorator;

public final class InterfaceCompositionDemo {
    private InterfaceCompositionDemo() {}

    // Two unrelated defaults: the implementing class must resolve the conflict.
    private static final class Resolved implements LeftOrigin, RightOrigin {
        @Override
        public String origin() {
            return LeftOrigin.super.origin() + "|" + RightOrigin.super.origin();
        }
    }

    private static class OriginBase {
        public String origin() {
            return "class";
        }
    }

    // The inherited concrete class method takes precedence over the default.
    private static final class ClassWins extends OriginBase implements LeftOrigin {}

    private static void check(boolean valid, String message) {
        if (!valid) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        int input = 67;
        Service add = new Service(new AddTenPolicy());
        Service twice = new Service(new DoublePolicy());
        Service decorated = new Service(new IncrementingDecorator(new DoublePolicy()));

        int addResult = add.execute(input);
        int doubleResult = twice.execute(input);
        int decoratedResult = decorated.execute(input);
        String description = add.explain(input);

        boolean nullRejected = false;
        try {
            new Service(null);
        } catch (NullPointerException expected) {
            nullRejected = true;
        }

        String resolved = new Resolved().origin();
        String classWins = new ClassWins().origin();

        check(addResult == 77, "AddTenPolicy must add 10");
        check(doubleResult == 134, "DoublePolicy must multiply by 2");
        check(decoratedResult == 135, "Decorator must forward then add 1");
        check("input=67 -> output=77".equals(description), "Policy default must use apply once");
        check(nullRejected, "Service must reject null dependency");
        check("left|right".equals(resolved), "Explicit InterfaceName.super resolution");
        check("class".equals(classWins), "Concrete class method takes precedence");

        System.out.println("== Service with interchangeable policies ==");
        System.out.println("add ten: " + addResult);
        System.out.println("double: " + doubleResult);
        System.out.println("decorated double: " + decoratedResult);
        System.out.println("default describe: " + description);
        System.out.println("null dependency rejected: " + nullRejected);
        System.out.println("== Interface default resolution ==");
        System.out.println("two defaults: " + resolved);
        System.out.println("class wins: " + classWins);
        System.out.println("CHECK PASSED");
    }
}
