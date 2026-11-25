package client;

// Theory: knowledge/01-java-core/interfaces.md
// Theory: knowledge/01-java-core/composition.md
// Theory: knowledge/01-java-core/inheritance.md
// Theory: knowledge/01-java-core/access.md
// Research: docs/plans/CORE-INTERFACES-COMPOSITION.md
import api.LeftOrigin;
import api.Policy;
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

    private static final class RecordingPolicyFake implements Policy {
        int calls;
        int lastInput;

        @Override
        public int apply(int value) {
            calls++;
            lastInput = value;
            return 900;
        }
    }

    private static final class CustomPolicy implements Policy {
        int applyCalls;
        int describeCalls;
        int lastInput;

        @Override
        public int apply(int value) {
            applyCalls++;
            lastInput = value;
            return value;
        }

        @Override
        public String describe(int input) {
            describeCalls++;
            lastInput = input;
            return "custom";
        }
    }

    // Same transformation as IncrementingDecorator, different description policy.
    private static final class ForwardingDescription implements Policy {
        private final Policy delegate;

        ForwardingDescription(Policy delegate) {
            this.delegate = delegate;
        }

        @Override
        public int apply(int value) {
            return Math.addExact(delegate.apply(value), 1);
        }

        @Override
        public String describe(int input) {
            return delegate.describe(input);
        }
    }

    private static void verifyRecording() {
        RecordingPolicyFake executeFake = new RecordingPolicyFake();
        int result = new Service(executeFake).execute(67);
        check(result == 900, "Service.execute must return delegate result");
        check(executeFake.lastInput == 67, "Service.execute must forward input");
        check(executeFake.calls == 1, "Service.execute must call apply exactly once");

        RecordingPolicyFake describeFake = new RecordingPolicyFake();
        String description = new Service(describeFake).explain(67);
        check("input=67 -> output=900".equals(description), "Policy.describe formatted result");
        check(describeFake.lastInput == 67, "Policy.describe must forward input");
        check(describeFake.calls == 1, "Policy.describe must call apply exactly once");
    }

    private static void verifyDescriptionPolicies() {
        CustomPolicy direct = new CustomPolicy();
        check("custom".equals(direct.describe(67)), "Custom description");
        check("custom".equals(new Service(direct).explain(67)), "Service forwards custom describe");
        check(direct.describeCalls == 2, "Direct and service description calls");
        check(direct.applyCalls == 0, "Custom description does not apply");
        check(direct.lastInput == 67, "Custom description input");

        CustomPolicy recomputed = new CustomPolicy();
        Policy decorator = new IncrementingDecorator(recomputed);
        check("input=67 -> output=68".equals(new Service(decorator).explain(67)), "Decorator recomputes description");
        check(recomputed.applyCalls == 1, "Recomputed description calls delegate apply once");
        check(recomputed.lastInput == 67, "Recomputed description input");
        check(recomputed.describeCalls == 0, "Inherited default does not forward describe");

        CustomPolicy forwarded = new CustomPolicy();
        Policy wrapper = new ForwardingDescription(forwarded);
        check("custom".equals(new Service(wrapper).explain(67)), "Explicit forwarding preserves custom description");
        check(forwarded.describeCalls == 1, "Forwarded description calls delegate describe once");
        check(forwarded.applyCalls == 0, "Forwarded description does not recompute");
        check(forwarded.lastInput == 67, "Forwarded description input");
    }

    private static void expectArithmetic(Runnable action, String message) {
        try {
            action.run();
        } catch (ArithmeticException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    private static void expectSameFailure(Runnable action, IllegalStateException failure) {
        try {
            action.run();
        } catch (IllegalStateException actual) {
            check(actual == failure, "Delegate failure identity must be preserved");
            return;
        }
        throw new AssertionError("Delegate failure must propagate");
    }

    private static void verifyArithmeticAndFailures() {
        int max = Integer.MAX_VALUE;
        check(max + 1 == Integer.MIN_VALUE, "Ordinary int addition wraps");
        expectArithmetic(() -> new Service(new AddTenPolicy()).execute(max), "AddTenPolicy must reject overflow");
        expectArithmetic(() -> new Service(new DoublePolicy()).execute(max), "DoublePolicy must reject overflow");
        expectArithmetic(() -> new Service(new IncrementingDecorator(value -> value)).execute(max), "Decorator must reject overflow");

        IllegalStateException failure = new IllegalStateException("delegate failure");
        int[] calls = {0};
        Policy throwing = value -> { calls[0]++; throw failure; };
        expectSameFailure(() -> new Service(throwing).execute(67), failure);
        check(calls[0] == 1, "Service must not retry failed delegate");
        expectSameFailure(() -> new Service(new IncrementingDecorator(throwing)).execute(67), failure);
        check(calls[0] == 2, "Decorator must not retry failed delegate");

        boolean nullRejected = false;
        try {
            new IncrementingDecorator(null);
        } catch (NullPointerException expected) {
            nullRejected = true;
        }
        check(nullRejected, "Decorator must reject null dependency");
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
        check("input=67 -> output=77".equals(description), "Policy default formatted description");
        check(nullRejected, "Service must reject null dependency");
        check("left|right".equals(resolved), "Explicit InterfaceName.super resolution");
        check("class".equals(classWins), "Concrete class method takes precedence");

        verifyRecording();
        verifyDescriptionPolicies();
        verifyArithmeticAndFailures();

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
