# CORE-STREAMS — Stream API в Java 21: как собирается и выполняется pipeline

**Версия:** Java SE 21, без preview. **Контракт темы:** принятый [G1 CORE-STREAMS + CORE-STREAM-ADV](../../docs/plans/CORE-STREAMS-ADV.md), [Issue #114](https://github.com/Melchiorius/backend-interview-labs/issues/114). Эта статья объясняет, **какие участники создают поток вычислений, когда вызываются функции и какой тип результата можно обещать**. Отдельная [CORE-STREAM-ADV](stream-advanced.md) раскрывает редукции, коллекторы, порядок при параллельном выполнении и ресурсы. Предпосылки: [лямбды и функциональные интерфейсы](functions.md), [обобщённые типы](generics.md), [исключения](exceptions.md), [equals/hashCode](equality.md).

## 1. Stream — описание обработки данных, а не ещё одна коллекция (ST-01)

`List<Integer>` хранит элементы и предоставляет операции доступа к ним. `Stream<Integer>` представляет **последовательность вычислений над элементами из источника**, которую можно обработать **один раз**. Поток не добавляет элементы в исходный список и не хранит автоматически копию всех результатов.

Участники одного pipeline:

- **Источник**: `List`, массив, генератор, диапазон `IntStream` или другой источник элементов.
- **Промежуточная стадия**: возвращает новый stream, описывая дополнительное преобразование или фильтрацию, например `filter`, `map`, `flatMap`.
- **Терминальная операция**: запрашивает итог `List`, `Optional`, число, логическое значение либо выполняет действие, например `toList`, `count`, `findFirst`, `forEach`.

~~~text
List<Integer> source: [1, 2, 3, 4]
                 │
                 ▼
Stream<Integer> -- filter(x -> x % 2 == 0) --> Stream<Integer>
                                                       │
                                                       ▼
                              map(x -> x * 10) --> Stream<Integer>
                                                       │
                                                       ▼
                                      toList() --> List<Integer> [20, 40]
~~~

Стрелки означают **логические стадии преобразования элементов**, не отдельные потоки ОС, задачи executor или гарантированный цикл для каждой стрелки. Источник поставляет элементы, операция terminal запрашивает результат, реализация Stream решает, как выполнить этапы с учётом допустимых оптимизаций. [Stream API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html), [package description](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html).

~~~java
import java.util.List;

class FirstPipeline {
    public static void main(String[] args) {
        List<Integer> source = List.of(1, 2, 3, 4);
        List<Integer> result = source.stream()
            .filter(x -> x % 2 == 0)
            .map(x -> x * 10)
            .toList();
        System.out.println(result); // [20, 40]
        System.out.println(source); // [1, 2, 3, 4] — исходный List не изменён
    }
}
~~~

**Зачем:** когда в сервисе нужно получить преобразованный список из DTO, `stream()` описывает вычисление результата без изменения исходной коллекции. Для обновления самих DTO или выполнения внешнего побочного действия API Stream не даёт автоматически транзакционности, потокобезопасности или повторяемости.

## 2. Отложенное выполнение: map не вызывается при построении (ST-02, ST-09)

Вызов `map` возвращает новый объект Stream, но сама функция ещё не обязана обрабатывать элементы. Терминальная операция обычно запускает обработку. Однако из этого **не** следует «каждая lambda обязательно выполняется один раз для каждого элемента»: спецификация допускает пропуск стадии, если он не меняет результат, а short-circuit прекращает дальнейшее потребление.

~~~java
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

class LazyExample {
    public static void main(String[] args) {
        AtomicInteger calls = new AtomicInteger();
        Stream<Integer> pipeline = Stream.of(1, 2, 3)
            .map(x -> {
                calls.incrementAndGet();
                return x * 2;
            });
        System.out.println(calls.get()); // 0: пока только построили pipeline
        System.out.println(pipeline.toList()); // [2, 4, 6]
        System.out.println(calls.get()); // 3: наблюдение этого конкретного выполнения
    }
}
~~~

`AtomicInteger` здесь позволяет **наблюдать** вызовы, а не является обязательным механизмом Stream. Не превращайте значение счётчика в переносимую гарантию для произвольной цепочки. Спецификация прямо позволяет устранять вызовы behavioral parameters, если вычисленный результат не меняется.

Контрпример против «`peek` гарантированно проверяет все записи»:

~~~java
import java.util.List;

class PeekCount {
    static long count() {
        return List.of("a", "b", "c").stream()
            .peek(System.out::println)
            .count();
    }
}
~~~

Для источника с известным размером `count()` **может** вычислить 3 без обхода и вообще не вызвать `peek`; так ведёт себя исследованный JDK 21. Но это **не обещание**, что `peek` не сработает на любом источнике или при любой цепочке. Аналогично `map(x -> sendEmail(x))` — ошибочная попытка обеспечивать обязательное действие через промежуточную стадию: обработка может быть сокращена или оптимизирована. Если требуются эффекты, нужно отдельно проектировать их семантику; в обычной бизнес-логике часто лучше явно выраженный цикл.

Норма: [Stream.count() API note](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#count()), [stream package — side effects](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html).

## 3. filter, map и flatMap: разные типы функций (ST-03, ST-04)

`filter(Predicate<T>)` оставляет элемент при `true`, не меняя тип элемента; `map(Function<T,R>)` преобразует элемент типа `T` в элемент типа `R`. Это два различных SAM-контракта из [CORE-FUNCTIONS](functions.md).

~~~java
import java.util.List;

class Transform {
    static List<String> names() {
        return List.of("Al", "Boris", "Vi").stream()
            .filter(s -> s.length() > 2)    // Predicate<String> -> boolean
            .map(String::toUpperCase)       // Function<String,String> -> String
            .toList();                       // [BORIS]
    }
}
~~~

Слово «типизированный» здесь важно: `Stream<Integer>.map((String s) -> ...)` не компилируется — функция должна принимать Integer. А `filter(s -> s.length())` не компилируется, потому что `Predicate.test` возвращает `boolean`, не `int`. Такие compile-negative исходники N01/N11 зафиксированы в G1 §7.

Если один внешний элемент содержит **несколько** внутренних, обычный `map(List::stream)` создаёт `Stream<Stream<T>>`. Чтобы получить единый плоский поток элементов, используют `flatMap`, чей mapper возвращает Stream:

~~~java
import java.util.List;

class Flat {
    public static void main(String[] args) {
        List<List<String>> groups = List.of(
            List.of("a", "b"), List.of("c"));
        System.out.println(groups.stream()
            .flatMap(List::stream)
            .toList());                    // [a, b, c]
        // .flatMap(group -> group)        // НЕ компилируется: требуется Stream
    }
}
~~~

`flatMap` последовательно передаёт элементы вложенных streams в итоговый pipeline; созданный mapper'ом stream закрывается по завершении использования согласно API. Не обещайте, что это автоматически закроет **внешний** stream или произвольные ресурсы, захваченные lambda. Дополнительный разбор закрытия — в [CORE-STREAM-ADV](stream-advanced.md). [Stream.flatMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#flatMap(java.util.function.Function)).

## 4. distinct, sorted и limit: состояние, цена и сокращение (ST-05, ST-06)

У `filter` для решения об одном элементе достаточно его текущего значения. `distinct` должен различать ранее встреченные элементы, а `sorted` — упорядочивать множество элементов; это **stateful** intermediate operations. Упорядоченному `sorted` обычно нужно получить все конечные элементы, прежде чем начать выдавать отсортированный результат, поэтому нельзя утверждать, что любая цепочка «обязательно работает с O(1) памяти». `distinct` опирается на согласованный контракт `equals/hashCode` объектов, а не на сравнение по ссылке.

~~~java
import java.util.List;

class Stateful {
    public static void main(String[] args) {
        System.out.println(List.of(3, 1, 3, 2, 2).stream()
            .distinct()
            .sorted()
            .limit(2)
            .toList());                    // [1, 2]
    }
}
~~~

Здесь `distinct` устраняет дубликаты, `sorted` устанавливает порядок, `limit(2)` ограничивает итог двумя значениями. **Порядок операций меняет смысл**: `limit(2).sorted()` сперва ограничивает исходную последовательность, а затем сортирует только её первые два значения. При parallel/ordered `distinct` и `limit` могут быть заметно дороже из-за необходимости соблюдать encounter order; гарантии асимптотики и speedup по одному примеру не выводятся.

~~~java
import java.util.stream.Stream;

class ShortCircuit {
    static long firstThreeCounted() {
        return Stream.iterate(0, x -> x + 1)
            .limit(3)
            .count();                       // 3
    }
}
~~~

`limit` — short-circuiting *intermediate* операция: downstream может перестать требовать элементы после границы. Для **бесконечного** источника такой предел часто делает запрос конечным, но не обещает завершение *любой* pipeline: если перед `limit` поставить операцию, которой требуется бесконечно много данных (например, полную сортировку неограниченной последовательности), вычисление может не завершиться. [Stream.limit](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#limit(long)), [Stream.sorted](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#sorted()).

## 5. findFirst, findAny и *Match: Optional — не boolean (ST-07, ST-19)

Терминальная `findFirst()` возвращает `Optional<T>`: он либо содержит найденный объект, либо пуст. Если **есть encounter order**, первый означает первый по этому порядку. `findAny()` вправе выбрать **любой** элемент: даже если в конкретном sequential запуске он совпал с первым, не надо записывать это как гарантированный выбор. Для `IntStream` результат аналогично имеет тип `OptionalInt`; он предоставляет `getAsInt()`/`orElse(int)`, не `Optional<T>.get()`.

~~~java
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class FindExamples {
    public static void main(String[] args) {
        Optional<String> first = Stream.of("first", "second").findFirst();
        OptionalInt number = IntStream.of(67, 68).findFirst();
        System.out.println(first.orElse("missing"));       // first
        System.out.println(number.orElse(-1));              // 67
        System.out.println(Stream.<String>empty().findFirst()); // Optional.empty
    }
}
~~~

Важно не смешивать `Optional<Integer>` и `OptionalInt`: первый оборачивает Integer, второй специализирован для `int`. Попытка присвоить `IntStream.of(1).findFirst()` в `Optional<Integer>` — ошибка типов. При выбранном `null` `findFirst/findAny` могут завершиться `NullPointerException`, поэтому такие операции не следует представлять как универсальную замену проверке null.

Другие три terminal-операции возвращают **примитивный `boolean`**, не Optional:

~~~java
import java.util.stream.Stream;

class Match {
    public static void main(String[] args) {
        System.out.println(Stream.of(1, 2, 3).anyMatch(x -> x > 2)); // true
        System.out.println(Stream.<Integer>empty().anyMatch(x -> true));  // false
        System.out.println(Stream.<Integer>empty().allMatch(x -> false)); // true
        System.out.println(Stream.<Integer>empty().noneMatch(x -> true)); // true
    }
}
~~~

Для пустого множества `anyMatch` ложен, а `allMatch` и `noneMatch` истинны: **нет контрпримера** к «все подходят» или «никто не подходит». Это вакуумная истинность, а не вызов `Predicate` на каком-то «пустом» элементе. `Optional.orElse` применим только к Optional, не к `boolean`. [Stream.findFirst](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#findFirst()), [Stream.anyMatch](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#anyMatch(java.util.function.Predicate)), [OptionalInt](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/OptionalInt.html).

## 6. Stream — одноразовый; терминальная операция не обязана его закрывать (ST-08)

~~~java
import java.util.stream.Stream;

class UseOnce {
    public static void main(String[] args) {
        Stream<Integer> source = Stream.of(1, 2, 3);
        System.out.println(source.count()); // 3
        // source.toList(); // запрещено: повторное использование pipeline
    }
}
~~~

API требует, чтобы stream **использовался только однажды**. После терминальной операции дальнейшее использование того же pipeline может приводить к `IllegalStateException`; реализация обнаруживает некоторые случаи повторного использования уже раньше. Если нужно два разных результата, создайте **два новых stream из повторно доступного источника**; не пытайтесь «перезапустить» старый объект. Генератор или сетевой источник может быть неповторяемым — это не лечится переприсваиванием ссылки.

И отдельно: «terminal запущена» **не равно** «`close()` вызван». Обычные streams из List не нуждаются во внешней очистке, но stream из IO-источника может удерживать ресурс. Правильные `try-with-resources` и `onClose` разобраны в [CORE-STREAM-ADV](stream-advanced.md) и [CORE-RESOURCES](resources.md). [Stream overview](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html).

## 7. Три похожие операции — три разных контракта List (ST-11, ST-12, ST-13)

| Операция | Что именно гарантировано | Null и изменяемость |
|---|---|---|
| `stream.toList()`, с Java 16 | Итоговый List **unmodifiable**, encounter order сохраняется при наличии; конкретный класс не обещан | На OpenJDK 21 в обычной пробе `Stream.of((String)null).toList()` содержит null; API этой операции не устанавливает запрет на null |
| `stream.collect(Collectors.toList())` | List без гарантий типа, изменяемости, сериализуемости и потокобезопасности | Нельзя считать результат гарантированно изменяемым или гарантированно null-отвергающим |
| `stream.collect(Collectors.toUnmodifiableList())` | **Unmodifiable** List; null запрещён | При встрече с null — `NullPointerException` |

~~~java
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ListResults {
    public static void main(String[] args) {
        List<Integer> fixed = Stream.of(1, 2, 3).toList();
        try {
            fixed.add(67);
        } catch (UnsupportedOperationException e) {
            System.out.println("unmodifiable");
        }
        List<Integer> unspecified = Stream.of(1, 2, 3)
            .collect(Collectors.toList());
        System.out.println(unspecified);   // [1, 2, 3]
        try {
            Stream.of("x", (String) null)
                .collect(Collectors.toUnmodifiableList());
        } catch (NullPointerException e) {
            System.out.println("null rejected");
        }
    }
}
~~~

Старайтесь проектировать **нужный контракт**, а не угадывать текущую реализацию `ArrayList`. Если нужен гарантированно изменяемый `ArrayList`, используйте `collect(Collectors.toCollection(java.util.ArrayList::new))`. `unmodifiable` означает запрет структурных изменений **этого списка**, а не глубокой неизменяемости объектов внутри. Не утверждайте, что `Collections.unmodifiableList`, `List.copyOf` и `Stream.toList` имеют одинаковую политику null. [Stream.toList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#toList()), [Collectors API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collectors.html).

## 8. IntStream — не Stream<Integer> (ST-18)

`Stream<Integer>` переносит ссылки на boxed Integer; `IntStream` работает с примитивными `int` и предлагает специальные терминальные операции `sum()`, `average()` (`OptionalDouble`), `summaryStatistics()`. Автобоксинг может возникать при переходе обратно к объектной форме, но Stream API не обещает конкретного выигрыша по времени в любой программе.

~~~java
import java.util.List;
import java.util.stream.IntStream;

class Primitive {
    public static void main(String[] args) {
        int total = IntStream.rangeClosed(1, 3).sum(); // 6
        List<Integer> values = IntStream.range(1, 4).boxed().toList();
        int charCount = List.of("a", "bb", "ccc").stream()
            .mapToInt(String::length)
            .sum();                              // 6
        System.out.println(total + "," + values + "," + charCount);
    }
}
~~~

`Stream<Integer>.sum()` **не существует**; нужно `mapToInt(Integer::intValue).sum()`, `collect(summingInt(...))` или `reduce(0, Integer::sum)`. И `IntStream.collect(Collector)` не совпадает с `Stream.collect(Collector)`: у IntStream другая перегрузка `collect(Supplier, ObjIntConsumer, BiConsumer)`. Аналоги `LongStream` и `DoubleStream` имеют собственные примитивные операции/Optional-специализации. [IntStream API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/IntStream.html).

## 9. Префикс, бесконечные источники и mapMulti (ST-26–ST-28, P1)

**`takeWhile` и `dropWhile`** на *ordered* stream работают с префиксом: первая возвращает наибольший начальный участок подходящих элементов, вторая удаляет такой начальный участок. Они **не** равны `filter`, который отбирает все подходящие элементы где бы они ни находились:

~~~java
import java.util.List;

class Prefix {
    public static void main(String[] args) {
        List<Integer> v = List.of(1, 2, 8, 3, 4);
        System.out.println(v.stream().takeWhile(x -> x < 5).toList()); // [1, 2]
        System.out.println(v.stream().dropWhile(x -> x < 5).toList()); // [8, 3, 4]
        System.out.println(v.stream().filter(x -> x < 5).toList());    // [1, 2, 3, 4]
    }
}
~~~

Для unordered stream понятие «первого» элемента отсутствует: API допускает иные варианты допустимого подмножества; не строить на этом детерминированную бизнес-логику. `takeWhile/dropWhile` доступны с Java 9.

**Источники:** `Stream.iterate(seed, next)` и `Stream.generate(supplier)` могут давать потенциально бесконечный поток, а `Stream.ofNullable(value)` — ноль или один элемент. Эти источники не обязаны завершаться сами. Для `generate` особенно важно ограничить потребление, если нужен конечный итог:

~~~java
import java.util.stream.Stream;

class Sources {
    public static void main(String[] args) {
        System.out.println(Stream.ofNullable(null).count()); // 0
        System.out.println(Stream.iterate(1, x -> x + 1)
            .limit(3).toList());                             // [1, 2, 3]
        System.out.println(Stream.generate(() -> 67)
            .limit(2).toList());                             // [67, 67]
    }
}
~~~

**`mapMulti`** (Java 16+) позволяет для одного входного элемента передать **ноль, один или несколько** выходных элементов непосредственно в предоставленный `Consumer`, без отдельного stream на каждый вход:

~~~java
import java.util.List;
import java.util.function.Consumer;

class MapMulti {
    static List<String> words() {
        return List.of("a,b", "c").stream()
            .<String>mapMulti((s, out) -> {
                for (String word : s.split(",")) out.accept(word);
            })
            .toList();                           // [a, b, c]
    }
}
~~~

В этом примере это альтернатива `flatMap(s -> java.util.Arrays.stream(s.split(",")))`, а не обещание, что любое `mapMulti` быстрее. Сигнатура, вывод типов и правила работы с `Consumer` указаны в [Stream.mapMulti](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#mapMulti(java.util.function.BiConsumer)).

## 10. Checked IOException в map(Function) и ошибки во время terminal (ST-31, P1)

`Stream.map` принимает `Function<T,R>`; метод `Function.apply` **не** объявляет checked `IOException`. Поэтому объявление `throws IOException` у **внешнего** метода не делает следующую lambda допустимой:

~~~java
import java.io.IOException;
import java.util.stream.Stream;

class CheckedMapperWrong {
    // static void broken() throws IOException {
    //     Stream.of("x").map(s -> { throw new IOException("IO"); }).toList();
    // }
    // НЕ компилируется: checked IOException не разрешён контрактом Function.apply
}
~~~

Это **самостоятельный compile-negative**: если раскрыть строки без комментариев, `javac --release 21` отвергнет checked exception в lambda. Это не ошибка `onClose(Runnable)` и не тождественно N15 из исследовательского G1. Контракт SAM и правила lambda — [Function API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/Function.html), [JLS §15.27](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27), [CORE-FUNCTIONS](functions.md).

Если преобразование действительно может завершиться IO-сбоем, можно обработать checked исключение **внутри** lambda и выразить результат в согласованном контракте, например через `UncheckedIOException`:

~~~java
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;

class CheckedMapperHandled {
    static String read(String name) throws IOException {
        if (name.isEmpty()) throw new IOException("empty");
        return name.toUpperCase();
    }
    public static void main(String[] args) {
        Stream<String> pipeline = Stream.of("", "ok").map(name -> {
            try {
                return read(name);
            } catch (IOException e) {
                throw new UncheckedIOException("read failed", e);
            }
        });
        // Создание pipeline ещё не требует вызова read().
        try {
            pipeline.toList();
        } catch (UncheckedIOException e) {
            System.out.println(e.getCause().getMessage()); // empty
        }
    }
}
~~~

В этом заданном случае ошибка проявляется во время terminal, когда mapper реально потреблён. **Не** гарантируется, что любой mapper с побочным эффектом будет выполнен перед каждым terminal (`count` может оптимизировать), и обёртка unchecked сама по себе не решает retry/transaction semantics. [CORE-EXCEPTIONS](exceptions.md), [CORE-RESOURCES](resources.md).

## 11. Как выбрать инструмент для задачи и чего статья не обещает

Обычно начинают с вопроса об итоговом результате: нужен список — `map/filter/toList`; нужно установить существование — `anyMatch`; найти значение или зафиксировать отсутствие — `findFirst`/`Optional`; сумма по `int` — `mapToInt().sum()`; группировка и политика дублирующихся ключей — [CORE-STREAM-ADV](stream-advanced.md). Для существенных побочных эффектов, внешнего IO или мутации общего состояния **цикл нередко яснее и безопаснее**, чем скрытая цепочка lambda.

**Не обещается:** что Stream «быстрее любого цикла», что каждое `map/peek` обязательно вызовется, что `findAny` вернёт конкретный элемент, что parallel изменит только скорость, что терминальная операция закроет ресурсы или что `Stream.toList` вернёт ArrayList. Всё это либо противоречит контракту, либо зависит от конкретной реализации и workload.

## Карта ST-01–ST-36 для самостоятельного чтения

| ID / уровень | Где объяснён и где граница |
|---|---|
| ST-01–ST-02 · P0 | §§1–2: source/intermediate/terminal, deferred pipeline и оптимизация |
| ST-03–ST-04 · P0 | §3: filter/map/flatMap, Predicate/Function и отрицательные типовые примеры |
| ST-05–ST-06 · P0 | §4: stateful distinct/sorted, short-circuit limit |
| ST-07–ST-09 · P0 | §§5–6 и §2: findFirst/findAny/Optional, одно использование, peek/count |
| ST-10 · P0 | [CORE-STREAM-ADV](stream-advanced.md): encounter order и forEachOrdered |
| ST-11–ST-13 · P0 | §7: три контракта List, null/mutability |
| ST-14–ST-17 · P0 | [CORE-STREAM-ADV](stream-advanced.md): toMap и grouping |
| ST-18–ST-19 · P0 | §§8, 5: IntStream, OptionalInt, *Match boolean, пустая последовательность |
| ST-20–ST-22 · P0 | [CORE-STREAM-ADV](stream-advanced.md): reduce, parallel, close/onClose |
| ST-23–ST-25 · P1 | [CORE-STREAM-ADV](stream-advanced.md): Collector, Spliterator, onClose errors |
| ST-26–ST-28 · P1 | §9: takeWhile/dropWhile, generators и mapMulti |
| ST-29–ST-30 · P1 | [CORE-STREAM-ADV](stream-advanced.md): mutable reduction и изменение источника |
| ST-31 · P1 | §10: checked Function и deferred unchecked failure |
| ST-32 · P1 | [CORE-STREAM-ADV](stream-advanced.md): concurrent grouping |
| ST-33–ST-36 · P2 | [CORE-STREAM-ADV](stream-advanced.md): внешние IO, scheduler, custom Collector, API после 21 — только границы |

## Источники и границы воспроизводимости

**Официальные нормы:** [Stream API 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html), [java.util.stream package](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html), [Collectors](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collectors.html), [IntStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/IntStream.html), [Optional](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html), [JLS §15.27](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27). Независимые интервью-источники и матрица — [G1-план](../../docs/plans/CORE-STREAMS-ADV.md), [реестр источников](../../docs/SOURCE_REGISTRY.md). **JLS не задаёт внутренний планировщик Stream**; библиотечные правила — в Java SE API.

**Статус авторских inline-примеров:** демонстрационные классы предназначены для отдельного копирования в собственные файлы, а закомментированные строки специально показывают compile-negative. Приведённые комментарии с результатами описывают ожидаемые результаты для показанных входов, а не доказательство запуска **всех** snippets из Markdown. Фактические проверки автора и будущий единый runnable фиксируются в авторском PR по Issue #114 и техническом [Issue #115](https://github.com/Melchiorius/backend-interview-labs/issues/115); ссылку на исполняемый пример добавим **только после** его включения в это же дерево. G1 §6/§5a содержат отдельно проверенные 33/11 наблюдений; 17 compile-negative N01–N17 не становятся проверенными в CI от наличия этой статьи. **NOT RUN до отдельной проверки:** автоматическое извлечение/компиляция всех блоков Markdown, GUI Obsidian/IntelliJ, другие JDK/JVM, JMH, IO, параллельные гонки.

**Дальше:** [CORE-STREAM-ADV — reduce/Collector/parallel/resources](stream-advanced.md); при необходимости вернитесь к [CORE-FUNCTIONS](functions.md), [CORE-EQUALITY](equality.md) и [CORE-EXCEPTIONS](exceptions.md).
