// Theory: knowledge/01-java-core/enums.md
// Theory: knowledge/01-java-core/control-flow.md
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;

public class EnumProbe {
    enum Status {
        NEW(67), PAID(80), SENT(90), DONE(100);
        private final int code;
        private int visits;
        Status(int code) { this.code = code; }
        int code() { return code; }
        void visit() { visits++; }
        int visits() { return visits; }
        boolean canMoveTo(Status next) {
            return switch (this) {
                case NEW -> next == PAID;
                case PAID -> next == SENT;
                case SENT -> next == DONE;
                case DONE -> false;
            };
        }
        @Override public String toString() { return "status-" + name().toLowerCase(Locale.ROOT); }
    }
    interface Action { int apply(int a, int b); }
    enum Operation implements Action {
        PLUS { public int apply(int a, int b) { return a + b; } },
        TIMES { public int apply(int a, int b) { return a * b; } };
    }
    enum Simple { FIRST, SECOND }
    static String operation(Operation op) { return switch(op) { case PLUS -> "+"; case TIMES -> "*"; }; }
    static String qualified(Status status) {
        return switch (status) {
            case Status.NEW -> "new";
            case Status.PAID -> "paid";
            case Status.SENT -> "sent";
            case Status.DONE -> "done";
        };
    }
    public static void main(String[] args) {
        System.out.println("E01=" + Arrays.toString(Status.values()));
        System.out.println("E02=" + Status.NEW.name());
        System.out.println("E03=" + Status.NEW.ordinal());
        System.out.println("E04=" + Status.NEW.toString());
        System.out.println("E05=" + (Status.valueOf("PAID") == Status.PAID));
        System.out.println("E06=" + Status.NEW.code());
        System.out.println("E07=" + Status.NEW.canMoveTo(Status.PAID));
        System.out.println("E08=" + Status.NEW.canMoveTo(Status.DONE));
        System.out.println("E09=" + Operation.PLUS.apply(6, 7));
        System.out.println("E10=" + operation(Operation.TIMES));
        EnumSet<Status> set = EnumSet.of(Status.DONE, Status.NEW, Status.SENT);
        System.out.println("E11=" + set);
        EnumMap<Status, String> map = new EnumMap<>(Status.class);
        map.put(Status.SENT, "sent");
        map.put(Status.NEW, "new");
        map.put(Status.DONE, null);
        System.out.println("E12=" + map);
        System.out.println("E13=" + map.containsKey(Status.DONE));
        System.out.println("E14=" + (map.get(Status.DONE) == null));
        System.out.println("E15=" + set.contains(null));
        try { set.add(null); } catch (NullPointerException ex) { System.out.println("E16=NPE"); }
        try { map.put(null, "bad"); } catch (NullPointerException ex) { System.out.println("E17=NPE"); }
        try { Status.valueOf("MISSING"); } catch (IllegalArgumentException ex) { System.out.println("E18=IAE"); }
        try { Status.valueOf(null); } catch (NullPointerException ex) { System.out.println("E19=NPE"); }
        Status.NEW.visit();
        System.out.println("E20=" + Status.NEW.visits());
        System.out.println("E21=" + (Status.NEW.getClass() == Status.NEW.getDeclaringClass()));
        System.out.println("E22=" + (Operation.PLUS.getClass() == Operation.PLUS.getDeclaringClass()));
        System.out.println("E23=" + Simple.class.isSealed());
        System.out.println("E24=" + Operation.class.isSealed());
        System.out.println("E25=" + (Status.NEW.equals(Status.valueOf("NEW"))));
        System.out.println("E26=" + qualified(Status.PAID));
        Status[] snapshot = Status.values();
        snapshot[0] = Status.DONE;
        System.out.println("E27=" + (Status.values()[0] == Status.NEW));
        System.out.println("E28=" + map.containsKey(null));
        System.out.println("E29=" + set.remove(null));
        System.out.println("E30=" + Status.NEW.name().equals(Status.NEW.toString()));
    }
}
