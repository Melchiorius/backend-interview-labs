public class PatternsProbe {
    record Point(int x, int y) {}
    record Envelope(Point point) {}
    sealed interface Answer permits Yes, No {}
    record Yes(Point point) implements Answer {}
    record No(String cause) implements Answer {}

    static String classify(Object input) {
        return switch (input) {
            case null -> "null";
            case String s when s.length() > 3 -> "long:" + s;
            case String s -> "short:" + s;
            case Integer n when n > 0 -> "positive:" + n;
            case Integer n -> "nonpositive:" + n;
            default -> "other";
        };
    }

    static String answer(Answer input) {
        return switch (input) {
            case Yes(Point(int x, int y)) when x > 0 -> "yes+:" + (x + y);
            case Yes(Point(int x, int y)) -> "yes:" + (x + y);
            case No(String cause) -> "no:" + cause;
        };
    }

    static String flow(Object input) {
        if (!(input instanceof String s)) return "not-string";
        return "string:" + s.length();
    }

    static String deconstruct(Object input) {
        if (input instanceof Envelope(Point(var x, var y)))
            return "point:" + (x + y);
        return "not-point";
    }

    static String countStatement(Object input) {
        StringBuilder b = new StringBuilder();
        switch (input) {
            case null -> b.append("null");
            case Integer n when n > 0 -> b.append("positive");
            case Integer n -> b.append("nonpositive");
            default -> b.append("other");
        }
        return b.toString();
    }

    static String nullOrDefault(Object value) {
        return switch (value) { case null, default -> "caught"; };
    }

    static boolean flowAnd(Object value) {
        return value instanceof String s && s.length() > 3;
    }

    static String failRecordAccessor(Object value) {
        return switch(value) {
            case Broken(int n) -> "ok:" + n;
            default -> "other";
        };
    }

    record Broken(int number) {
        @Override public int number() {
            throw new IllegalStateException("broken-accessor");
        }
    }

    public static void main(String[] args) {
        System.out.println("P01=" + classify(null));
        System.out.println("P02=" + classify("abcd"));
        System.out.println("P03=" + classify("ab"));
        System.out.println("P04=" + classify(67));
        System.out.println("P05=" + classify(-1));
        System.out.println("P06=" + classify(2.5));
        System.out.println("P07=" + answer(new Yes(new Point(4, 5))));
        System.out.println("P08=" + answer(new Yes(new Point(0, 5))));
        System.out.println("P09=" + answer(new No("down")));
        System.out.println("P10=" + flow("seven"));
        System.out.println("P11=" + flow(67));
        System.out.println("P12=" + deconstruct(new Envelope(new Point(6, 7))));
        System.out.println("P13=" + deconstruct(new Envelope(null)));
        System.out.println("P14=" + countStatement(67));
        System.out.println("P15=" + countStatement(null));
        try {
            System.out.println("P16=" + switch ((Object) null) {
                case String s -> s;
                default -> "default";
            });
        } catch (NullPointerException ex) {
            System.out.println("P16=NPE");
        }
        try { failRecordAccessor(new Broken(1)); }
        catch (MatchException ex) {
            System.out.println("P17=MatchException:" +
                ex.getCause().getClass().getSimpleName());
        }
        System.out.println("P18=" + nullOrDefault(null));
        System.out.println("P19=" + flowAnd("abcde"));
        try { answer(new Yes(null)); }
        catch (MatchException ex) {
            System.out.println("P20=MatchException");
        }
    }
}
