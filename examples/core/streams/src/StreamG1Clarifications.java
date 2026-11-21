// Theory: knowledge/01-java-core/streams.md
// Theory: knowledge/01-java-core/stream-advanced.md
// Research: docs/plans/CORE-STREAMS-ADV.md (accepted G1, section 5a)
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class StreamG1Clarifications {
    public static void main(String[] args) {
        System.out.println("C01=" + Stream.of(1, 2, 3).reduce(Integer::sum));
        System.out.println("C02=" + Stream.<Integer>empty().reduce(Integer::sum));
        System.out.println("C03=" + Stream.of(1, 2, 3).reduce(0, Integer::sum));
        System.out.println("C04=" + Stream.of(1, 2, 3).reduce(0, Integer::sum, Integer::sum));
        System.out.println("C05=" + IntStream.of(1, 2, 3).reduce(Integer::sum));
        Optional<Integer> first = Stream.of(1, 2, 3).findFirst();
        OptionalInt primitive = IntStream.of(1, 2, 3).findFirst();
        System.out.println("C06=" + first + "," + primitive);
        System.out.println("C07=" + Stream.of(1, 2, 3).anyMatch(x -> x > 2));
        System.out.println("C08=" + Stream.<Integer>empty().anyMatch(x -> true));
        System.out.println("C09=" + Stream.<Integer>empty().allMatch(x -> false));
        System.out.println("C10=" + Stream.<Integer>empty().noneMatch(x -> true));
        System.out.println("C11=" + Stream.<Integer>empty().findFirst().orElse(67));
    }
}
