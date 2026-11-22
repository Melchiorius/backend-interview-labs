public class SealedProbe {
    sealed interface Result permits Ok, Err, Pending {}
    record Ok(int value) implements Result {}
    record Err(String message) implements Result {}
    non-sealed static class Pending implements Result {
        int count;
        void increment() { count++; }
    }
    static final class Later extends Pending {}

    sealed interface AutoPermit {}
    record Token(String text) implements AutoPermit {}
    final static class Marker implements AutoPermit {}

    sealed abstract static class State permits Ready, Stopped {}
    static final class Ready extends State {}
    static final class Stopped extends State {}

    sealed interface GenericResult<T> permits GenericOk {}
    record GenericOk<T>(T value) implements GenericResult<T> {}

    static String describe(Result result) {
        return switch (result) {
            case Ok(int value) -> "ok:" + value;
            case Err(String message) -> "err:" + message;
            case Pending p -> "pending";
        };
    }

    static String implicit(AutoPermit value) {
        return switch (value) {
            case Token(String text) -> "token:" + text;
            case Marker marker -> "marker";
        };
    }

    public static void main(String[] args) {
        System.out.println("S01=" + describe(new Ok(67)));
        System.out.println("S02=" + describe(new Err("bad")));
        System.out.println("S03=" + describe(new Later()));
        System.out.println("S04=" + implicit(new Token("A")));
        System.out.println("S05=" + implicit(new Marker()));
        System.out.println("S06=" + (new Ready() instanceof State));
        System.out.println("S07=" + Result.class.isSealed());
        System.out.println("S08=" + (new Ok(1) instanceof java.lang.Record));
        System.out.println("S09=" + (new Later() instanceof Pending));
        GenericResult<String> g = new GenericOk<>("generic");
        System.out.println("S10=" + ((GenericOk<?>) g).value());
        Pending mutable = new Pending();
        mutable.increment();
        System.out.println("S11=" + mutable.count);
    }
}
