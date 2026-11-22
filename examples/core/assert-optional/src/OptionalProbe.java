// Theory: knowledge/01-java-core/assertions.md
// Theory: knowledge/01-java-core/optional.md
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class OptionalProbe {
    private static String fallback(AtomicInteger counter) {
        counter.incrementAndGet();
        return "fallback";
    }
    private static Optional<String> lookup(String value) {
        return Optional.ofNullable(value);
    }
    public static void main(String[] args) {
        Optional<String> present = Optional.of("Java");
        Optional<String> absent = Optional.empty();
        System.out.println("OT01=" + present.isPresent() + ":" + present.isEmpty());
        System.out.println("OT02=" + absent.isPresent() + ":" + absent.isEmpty());
        System.out.println("OT03=" + Optional.ofNullable(null).isEmpty());
        try { Optional.of(null); } catch (NullPointerException ex) { System.out.println("OT04=NPE"); }
        System.out.println("OT05=" + present.get());
        try { absent.get(); } catch (NoSuchElementException ex) { System.out.println("OT06=NSEE"); }
        try { absent.orElseThrow(); } catch (NoSuchElementException ex) { System.out.println("OT07=NSEE"); }
        try { absent.orElseThrow(() -> new IllegalStateException("missing")); }
        catch (IllegalStateException ex) { System.out.println("OT08=ISE:" + ex.getMessage()); }
        AtomicInteger counter = new AtomicInteger();
        System.out.println("OT09=" + present.orElse(fallback(counter)) + ":" + counter.get());
        System.out.println("OT10=" + present.orElseGet(() -> fallback(counter)) + ":" + counter.get());
        System.out.println("OT11=" + absent.orElseGet(() -> fallback(counter)) + ":" + counter.get());
        System.out.println("OT12=" + present.map(String::length).orElse(-1));
        System.out.println("OT13=" + present.<String>map(s -> null).isEmpty());
        System.out.println("OT14=" + present.flatMap(OptionalProbe::lookup).orElse("none"));
        System.out.println("OT15=" + present.filter(s -> s.length() > 4).isEmpty());
        System.out.println("OT16=" + present.filter(s -> s.length() == 4).orElse("none"));
        System.out.println("OT17=" + absent.map(String::length).isEmpty());
        System.out.println("OT18=" + present.or(() -> Optional.of("other")).get());
        System.out.println("OT19=" + absent.or(() -> Optional.of("other")).get());
        try { present.flatMap(s -> null); } catch (NullPointerException ex) { System.out.println("OT20=NPE:flatMap-null"); }
        try { absent.or(() -> null); } catch (NullPointerException ex) { System.out.println("OT21=NPE:or-null"); }
        try { absent.orElseGet(null); } catch (NullPointerException ex) { System.out.println("OT22=NPE:missing-supplier"); }
        System.out.println("OT23=" + present.orElseGet(null));
        System.out.println("OT24=" + absent.orElse(null));
        System.out.println("OT25=" + Stream.of(Optional.of("A"), Optional.<String>empty(), Optional.of("B"))
                .flatMap(Optional::stream).toList());
        System.out.println("OT26=" + present.stream().count() + ":" + absent.stream().count());
        StringBuilder actions = new StringBuilder();
        present.ifPresent(actions::append);
        absent.ifPresentOrElse(actions::append, () -> actions.append("-empty"));
        System.out.println("OT27=" + actions);
        System.out.println("OT28=" + OptionalInt.of(67).getAsInt());
        System.out.println("OT29=" + OptionalLong.of(67L).stream().sum());
        System.out.println("OT30=" + OptionalDouble.of(2.5).getAsDouble());
        System.out.println("OT31=" + OptionalInt.empty().orElse(67));
        System.out.println("OT32=" + Optional.of("same").equals(Optional.of("same")));
        System.out.println("OT33=" + Optional.ofNullable("value").map(String::length).orElse(0));
        try { OptionalInt.empty().getAsInt(); } catch (NoSuchElementException ex) { System.out.println("OT34=NSEE:int"); }
        System.out.println("OT35=" + OptionalLong.empty().isEmpty() + ":" + OptionalDouble.empty().isEmpty());
        System.out.println("OT36=" + present.orElseThrow());
    }
}
