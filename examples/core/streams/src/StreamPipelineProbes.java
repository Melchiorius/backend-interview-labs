// Theory: knowledge/01-java-core/streams.md
// Theory: knowledge/01-java-core/stream-advanced.md
// Research: docs/plans/CORE-STREAMS-ADV.md (accepted G1, section 6)
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.*;

public class StreamPipelineProbes {
    static void show(String id, Object value) { System.out.println(id + "=" + value); }
    static String failure(Runnable action) {
        try { action.run(); return "NO_ERROR"; }
        catch (RuntimeException e) { return e.getClass().getSimpleName(); }
    }
    public static void main(String[] args) {
        AtomicInteger mapperCalls = new AtomicInteger();
        Stream<Integer> lazy = Stream.of(1, 2, 3).map(x -> { mapperCalls.incrementAndGet(); return x * 2; });
        show("P01", mapperCalls.get());
        show("P02", lazy.toList());
        show("P03", mapperCalls.get());
        show("P04", Stream.of(List.of("a", "b"), List.of("c")).flatMap(List::stream).toList());
        show("P05", Stream.of(3, 1, 2, 3).distinct().sorted().toList());
        show("P06", Stream.of(2, 4, 6, 8).filter(x -> x > 3).limit(2).toList());
        show("P07", IntStream.range(1, 30).parallel().filter(x -> x % 7 == 0).findFirst().orElse(-1));
        show("P08", IntStream.range(1, 30).parallel().filter(x -> x % 7 == 0).findAny().isPresent());
        AtomicInteger peekCalls = new AtomicInteger();
        show("P09", Stream.of(1, 2, 3).peek(x -> peekCalls.incrementAndGet()).count() + ",peek=" + peekCalls.get());
        show("P10", Stream.<String>of("a", null).toList());
        show("P11", failure(() -> Stream.of(1, 2).toList().add(3)));
        show("P12", Stream.of(1, 2).collect(Collectors.toList()).size());
        show("P13", failure(() -> Stream.<String>of("a", null).collect(Collectors.toUnmodifiableList())));
        show("P14", failure(() -> Stream.of("a", "a").collect(Collectors.toMap(x -> x, String::length))));
        Map<String, Integer> counts = Stream.of("a", "b", "a").collect(Collectors.toMap(x -> x, x -> 1, Integer::sum));
        show("P15", new TreeMap<>(counts));
        show("P16", failure(() -> Stream.of("a").collect(Collectors.toUnmodifiableMap(x -> null, x -> 1))));
        show("P17", failure(() -> Stream.of("a").collect(Collectors.toMap(x -> x, x -> null))));
        Map<String, List<Integer>> groups = Stream.of(1, 2, 3, 4).collect(Collectors.groupingBy(x -> x % 2 == 0 ? "even" : "odd"));
        show("P18", new TreeMap<>(groups));
        show("P19", new TreeMap<>(Stream.of(1, 2, 3, 4).collect(Collectors.partitioningBy(x -> x % 2 == 0))));
        show("P20", IntStream.of(2, 3, 5).sum());
        show("P21", IntStream.rangeClosed(1, 3).boxed().toList());
        Stream<Integer> consumed = Stream.of(1, 2);
        consumed.count();
        show("P22", failure(consumed::count));
        StringBuilder closeLog = new StringBuilder();
        Stream<Integer> closable = Stream.of(1, 2).onClose(() -> closeLog.append('A')).onClose(() -> closeLog.append('B'));
        closable.count();
        show("P23", "before=" + closeLog);
        closable.close();
        show("P24", "after=" + closeLog);
        StringBuffer ordered = new StringBuffer();
        IntStream.rangeClosed(1, 4).parallel().forEachOrdered(ordered::append);
        show("P25", ordered.toString());
        show("P26", IntStream.rangeClosed(1, 1000).parallel().sum());
        show("P27", Stream.<String>empty().findFirst().orElse("fallback"));
        show("P28", Stream.of(1, 2, 3, 4).takeWhile(x -> x < 3).toList());
        show("P29", Stream.of(1, 2, 3, 4).dropWhile(x -> x < 3).toList());
        show("P30", Stream.of("a", "b").collect(Collectors.joining(",")));
        Stream<Integer> throwing = Stream.of(1).map(x -> { throw new IllegalStateException("in-map"); });
        show("P31", failure(throwing::toList));
        show("P32", Stream.ofNullable(null).count());
        show("P33", Stream.iterate(1, x -> x < 5, x -> x + 1).toList());
    }
}
