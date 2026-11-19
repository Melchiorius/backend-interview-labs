// Theory: knowledge/01-java-core/classes.md
// Theory: knowledge/01-java-core/types.md
// Theory: knowledge/01-java-core/parameters.md
// From repository root: java examples/core/ClassesAndObjectsDemo.java
public class ClassesAndObjectsDemo {
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static void expectInvalid(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError("Expected IllegalArgumentException: " + label);
        } catch (IllegalArgumentException expected) {
            // Correct: the object or operation rejected invalid data.
        }
    }

    public static void main(String[] args) {
        int createdBefore = CreditAccount.createdCount();
        CreditAccount first = new CreditAccount("А", 67);
        CreditAccount second = new CreditAccount("Б");
        CreditAccount alias = first;

        check(first != second, "Different instances");
        check(first == alias, "Alias refers to the same object");
        check(first.balance() == 67 && second.balance() == 0, "Independent fields");
        check(CreditAccount.createdCount() == createdBefore + 2, "Shared static count");

        alias.deposit(10);
        check(first.balance() == 77 && second.balance() == 0, "Shared instance mutation");
        second.deposit(4);
        second.withdraw(3);
        check(second.balance() == 1, "Second instance changes separately");

        expectInvalid(() -> new CreditAccount("", 1), "Empty owner");
        expectInvalid(() -> new CreditAccount("А", -1), "Negative starting balance");
        expectInvalid(() -> first.deposit(0), "Nonpositive deposit");
        check(CreditAccount.createdCount() == createdBefore + 2, "Rejected objects not counted");

        boolean refused = false;
        try {
            second.withdraw(67);
        } catch (IllegalStateException expected) {
            refused = true;
        }
        check(refused && second.balance() == 1, "Overdraw was refused");

        CreditAccount huge = new CreditAccount("В", Long.MAX_VALUE);
        boolean overflow = false;
        try {
            huge.deposit(1);
        } catch (ArithmeticException expected) {
            overflow = true;
        }
        check(overflow && huge.balance() == Long.MAX_VALUE, "Overflow was refused");
        check(CreditAccount.createdCount() == createdBefore + 3, "Successful construction counted");

        System.out.println("first=" + first.balance() + ", second=" + second.balance());
        System.out.println("created=" + CreditAccount.createdCount());
        System.out.println("CHECK PASSED");
    }
}

// A second top-level class in the same file is legal if it is not public.
final class CreditAccount {
    private static int createdCount;
    private final String owner;
    private long balance;

    CreditAccount(String owner) {
        this(owner, 0);
    }

    CreditAccount(String owner, long openingBalance) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Owner name required");
        }
        if (openingBalance < 0) {
            throw new IllegalArgumentException("Negative opening balance");
        }
        this.owner = owner;
        this.balance = openingBalance;
        createdCount++;
    }

    String owner() { return owner; }
    long balance() { return balance; }
    static int createdCount() { return createdCount; }

    void deposit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        balance = Math.addExact(balance, amount);
    }

    void withdraw(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (amount > balance) throw new IllegalStateException("Insufficient credits");
        balance -= amount;
    }
}
