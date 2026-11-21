# CORE-STREAM-ADV — reduce, Collector, порядок и параллельные Stream в Java 21

**Версия:** Java SE 21 без preview. **Исходная спецификация:** [принятый G1 CORE-STREAMS/STREAM-ADV](../../docs/plans/CORE-STREAMS-ADV.md), [G2 Issue #114](https://github.com/Melchiorius/backend-interview-labs/issues/114). Перед этой статьёй прочитайте [CORE-STREAMS](streams.md): источник, промежуточные операции, отложенное выполнение, short-circuit и Optional. Здесь цель другая — объяснить, **как элементы собираются в результат, что можно объединять параллельно и какие побочные действия запрещают корректное преобразование**.

Ключевая идея: Stream описывает вычисление, но не обещает, что все lambdas выполнятся в одном потоке или в ожидаемом порядке. Для правильного результата важны **ассоциативность редукции, нейтральный элемент, контракт контейнеров, encounter order, non-interference и реальное владение ресурсом**. В Java SE 21 это контракты [Stream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html), [Collector](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collector.html) и [java.util.stream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html).

## 1. reduce: результат из нескольких элементов (ST-20, P0)

`reduce` сворачивает последовательность элементов в одно значение. Например, для [2, 3, 4] сумма вычисляется как `((2 + 3) + 4) = 9`. Для параллельного выполнения реализация вправе сначала получить результаты отдельных частей и затем объединить их; значит, операция редукции должна быть **ассоциативной**: `(a ⊕ b) ⊕ c = a ⊕ (b ⊕ c)`. Указанный закон — не то же самое, что коммутативность `a ⊕ b = b ⊕ a`: конкатенация строк ассоциативна, но не коммутативна.

### 1.1. Без identity → Optional<T>

~~~java
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class ReduceOverloads {
    public static void main(String[] args) {
        Optional<Integer> sum = Stream.of(2, 3, 4)
            .reduce(Integer::sum);
        Optional<Integer> empty = Stream.<Integer>empty()
            .reduce(Integer::sum);
        System.out.println(sum);                // Optional[9]
        System.out.println(empty);              // Optional.empty

        int withIdentity = Stream.of(2, 3, 4)
            .reduce(0, Integer::sum);
        System.out.println(withIdentity);       // 9

        int byIntStream = IntStream.of(2, 3, 4)
            .reduce(Integer::sum).orElse(0);
        System.out.println(byIntStream);        // 9
    }
}
~~~

Без нейтрального значения пустую последовательность нечем свернуть, поэтому `Stream<T>.reduce(BinaryOperator<T>)` возвращает **`Optional<T>`**, а `IntStream.reduce(IntBinaryOperator)` — **`OptionalInt`**. Напротив, `Stream<T>.reduce(T identity, BinaryOperator<T>)` возвращает само `T`; пустая последовательность даёт identity. Нельзя называть все три перегрузки «`reduce всегда возвращает T`».

### 1.2. С identity: нужен нейтральный элемент, а не просто стартовое число

Для сложения корректный identity = 0, потому что `0 + x = x + 0 = x`. Для умножения — 1. Для склеивания строк — пустая строка. Identity должен быть нейтральным и для логики объединения частичных результатов.

~~~java
import java.util.stream.Stream;

class Identity {
    static int good() {
        return Stream.of(1, 2, 3).reduce(0, Integer::sum); // 6
    }
    static int badContract() {
        return Stream.of(1, 2, 3).reduce(10, Integer::sum);
        // sequential может дать 16, но 10 НЕ нейтрален для сложения
    }
}
~~~

Почему `reduce(10, Integer::sum)` опасен: последовательная реализация складывает 10 с элементами; параллельная может начать **несколько** частичных редукций с identity=10 и объединить их. Для такой некорректной операции API не обещает стабильную семантику при разных стратегиях разбиения. Число 10 здесь **не универсальный «начальный seed»**, который просто однократно прибавится в любом исполнении. Сложение чисел Java `int` ассоциативно в смысле операции с переполнением по модулю 2^32; вычитание не подходит под нужный закон.

### 1.3. Трёхаргументный reduce: тип результата может отличаться от типа элемента

~~~java
import java.util.stream.Stream;

class HeterogeneousReduce {
    public static void main(String[] args) {
        int totalLetters = Stream.of("a", "bb", "ccc")
            .reduce(0,
                (length, word) -> length + word.length(), // BiFunction<U,T,U>
                Integer::sum);                            // BinaryOperator<U>
        System.out.println(totalLetters);                 // 6
    }
}
~~~

Здесь элементы имеют тип `String`, результат — `Integer`. `identity` и `combiner` работают с Integer, `accumulator` добавляет в Integer вклад одного String. Контракт требует не только ассоциативности combiner, но и согласованности `combiner(u, accumulator(identity, t)) = accumulator(u, t)` для допустимых данных и identity. В параллельном pipeline реализация может разбить источник и слить результаты; нельзя просто «использовать свой аккумулятор» произвольным образом. Если в результат надо **мутировать коллекцию**, правильнее `collect`, а не `reduce` с мутируемым identity.

Нормы: [Stream.reduce(BinaryOperator)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#reduce(java.util.function.BinaryOperator)), [reduce(identity,accumulator)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#reduce(T,java.util.function.BinaryOperator)), [reduce(U,BiFunction,BinaryOperator)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#reduce(U,java.util.function.BiFunction,java.util.function.BinaryOperator)).

## 2. collect: управляемая сборка с изменяемым накопителем (ST-23, ST-29)

В случае `collect` частичный результат — **накопитель**: например, отдельный `ArrayList`. Реализация может создавать несколько накопителей и затем объединять их. Это отличается от `reduce`: здесь разрешена локальная мутация **контейнера, созданного для накопления**, если функции соблюдают правила Collector и не пишут в общий посторонний объект.

~~~java
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class CollectExample {
    static List<Integer> build() {
        return Stream.of(1, 2, 3)
            .collect(ArrayList::new,
                ArrayList::add,
                ArrayList::addAll); // [1, 2, 3]
    }
}
~~~

Три участника перегрузки `collect(Supplier<R>, BiConsumer<R,T>, BiConsumer<R,R>)`: `supplier` создаёт контейнер; `accumulator` добавляет очередной элемент; `combiner` соединяет два частичных контейнера. Важно: мы **не** передали один заранее созданный глобальный `ArrayList`, куда все потоки обязаны писать одновременно.

Обобщённый `Collector<T,A,R>` дополняет эту модель четвёртой функцией **finisher**, превращающей промежуточный накопитель `A` в окончательный результат `R`:

~~~text
elements:           A    B      C    D
                       │           │
supplier() ->      [ ]          [ ]
accumulator ->     [A,B]        [C,D]
                       └────┬──────┘
                         combiner
                           [A,B,C,D]    (соблюдая контракт порядка)
                               │
                            finisher
                               │
                           result R
~~~

Схема иллюстрирует **одну допустимую стратегию**, а не обещанный JVM порядок разбиения. Параллельный collector может использовать другой способ объединения, если он соответствует identity/associativity и характеристикам.

У Collector есть требования:

- **Identity**: объединение накопленного результата с пустым контейнером не должно менять эквивалентный итог.
- **Associativity**: произвольная допустимая группировка накопления/объединения должна давать эквивалентный результат.
- **Characteristics**: `IDENTITY_FINISH` говорит, что finisher можно опустить; `UNORDERED` — результат не зависит от encounter order; `CONCURRENT` разрешает конкурентный вызов accumulator **на одном накопителе** при выполнении остальных правил API. Нельзя присваивать их «на глаз».

Пример обычной безопасной сборки: `stream.parallel().collect(Collectors.toList())`. `Collectors.toList()` не обещает изменяемость или класс итогового списка, но терминальная операция реализует редукцию в соответствии с контрактом collector. Это **не** то же самое, что несколько потоков вручную вызывают `shared.add(x)`. Норма: [Collector API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collector.html), [Stream.collect](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#collect(java.util.stream.Collector)).

## 3. toMap: дубликат ключа — не «последнее значение победит» (ST-14, ST-15)

При сборке карты первый аргумент `toMap` преобразует элемент в ключ, второй — в значение. Если несколько элементов имеют один и тот же ключ, перегрузка **без** merge-функции бросает `IllegalStateException`.

~~~java
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DuplicateKeys {
    public static void main(String[] args) {
        try {
            Stream.of("A", "a").collect(Collectors.toMap(
                String::toLowerCase,
                s -> s));
        } catch (IllegalStateException e) {
            System.out.println("duplicate key"); // ожидаемый перехват
        }

        Map<String, String> chosen = Stream.of("A", "a")
            .collect(Collectors.toMap(
                String::toLowerCase,
                s -> s,
                (oldValue, newValue) -> oldValue + newValue));
        System.out.println(chosen.get("a"));     // Aa
    }
}
~~~

При merge-функции **вы сами выбираете** политику конфликта: сохранить прежнее, заменить, объединить значения или выбросить свою ошибку. Не полагайтесь на случайный порядок входа в **unordered/parallel** stream, если политика зависит от «кто первый». У `Collectors.toMap(...)` нет общего обещания, что результат является `HashMap`, что он сохраняет порядок ключей или имеет гарантированную изменяемость. Если нужен определённый тип контейнера, у `toMap` есть перегрузка с `mapFactory` (например, `LinkedHashMap::new`), но требования к key/value/merge остаются.

~~~java
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ExplicitMapType {
    static Map<Integer,String> map() {
        return Stream.of("a", "bb").collect(Collectors.toMap(
            String::length,
            s -> s,
            (first, second) -> first,
            LinkedHashMap::new));
    }
}
~~~

Здесь мы выбрали реализацию контейнера, но не сделали любую сборку «автоматически отсортированной» или потокобезопасной. Нормы: [Collectors.toMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collectors.html), [Map.merge](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html#merge(K,V,java.util.function.BiFunction)).

## 4. Null в toMap/toUnmodifiableMap: контракт конкретной перегрузки (ST-16)

Опасное обобщение: «любая Map отвергает null» или «все toMap одинаково работают с null». Это неверно. Правила определяет **конкретный Collector** и участвующая Map/merge-функция.

~~~java
import java.util.stream.Collectors;
import java.util.stream.Stream;

class MapNullCases {
    static void rejectNullValue() {
        try {
            Stream.of("x").collect(Collectors.toUnmodifiableMap(
                s -> s, s -> null));
        } catch (NullPointerException e) {
            System.out.println("unmodifiable map rejects null");
        }
    }

    static void observedToMap() {
        try {
            Stream.of("x").collect(Collectors.toMap(
                s -> s, s -> null));
        } catch (NullPointerException e) {
            System.out.println("toMap mapper null rejected here");
        }
    }
}
~~~

Для `toUnmodifiableMap` API **явно запрещает null ключи и значения**. В исследованном OpenJDK 21 обычный `Collectors.toMap(keyMapper,valueMapper)` с null-значением также бросает NPE (класс исключения — зафиксированное наблюдение конкретного сценария). Не переносите это утверждение на **все overloads**, произвольную `mapFactory`, `Map.put` или правила `Stream.toList()`. В иных перегрузках и collector'ах задействованы другие контракты и шаги `Map.merge`. В проекте лучше заранее определить политику: отбрасывать null, заменять значением по умолчанию или явно сигнализировать об ошибке. [Collectors.toUnmodifiableMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collectors.html), [Collectors.toMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collectors.html).

## 5. groupingBy, partitioningBy и concurrent grouping (ST-17, ST-32)

`groupingBy(classifier)` собирает элементы по ключу классификации, обычно в значениях — списки элементов. `partitioningBy(predicate)` группирует по **Boolean** в две категории; при необходимости downstream collector считает количество или суммирует значения внутри групп.

~~~java
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class Grouping {
    static void example() {
        Map<Integer, List<String>> byLength =
            List.of("a", "bb", "c").stream()
                .collect(Collectors.groupingBy(String::length));
        System.out.println(byLength.get(1)); // [a, c]

        Map<Boolean, List<Integer>> byParity =
            List.of(1, 2, 3).stream()
                .collect(Collectors.partitioningBy(x -> x % 2 == 0));
        System.out.println(byParity.get(true)); // [2]

        Map<Integer, Long> counts = List.of("a", "bb", "c").stream()
            .collect(Collectors.groupingBy(
                String::length, Collectors.counting()));
        System.out.println(counts.get(1)); // 2
    }
}
~~~

Результат `groupingBy` — не обязательно `HashMap` или карта с сортированными ключами. У перегрузок есть возможность задать mapFactory и downstream collector; не утверждайте, что произвольный downstream возвращает List.

`groupingByConcurrent` возвращает **concurrent, unordered Collector** (для соответствующих перегрузок). Его применяют, если цель — корректная конкурентная группировка и порядок элементов в списках не является частью бизнес-контракта. Это **не** синоним «всегда быстрее» и не гарантия, что элементы каждого списка отсортированы по encounter order.

~~~java
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

class ConcurrentGroups {
    static ConcurrentMap<Integer,List<Integer>> group() {
        return List.of(1, 2, 3, 4, 5).parallelStream()
            .collect(Collectors.groupingByConcurrent(x -> x % 2));
    }
}
~~~

**Почему так допустимо:** Streams/Collector API управляет накоплением в соответствии с заявленными характеристиками. Но это не обещание, что произвольная пользовательская `ArrayList`, разделяемая между потоками, автоматически станет безопасной. Не полагайтесь на фиксированный порядок значений в каждой группе. [Collectors.groupingByConcurrent](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collectors.html#groupingByConcurrent(java.util.function.Function)).

## 6. Encounter order: результат и вызовы lambda — разные вещи (ST-10)

**Encounter order** — определённый источником/стадиями порядок элементов (например, порядок элементов List). Он не равен очереди выполнения mapper'ов на реальных потоках.

~~~java
import java.util.List;

class OrderedTerminal {
    static void ordered() {
        List.of(1, 2, 3).parallelStream()
            .map(x -> x * 10)
            .forEachOrdered(System.out::println);
        // для этого ordered source terminal effects: 10, 20, 30
    }
}
~~~

Для ordered stream `forEachOrdered` обязуется выполнять действие в encounter order; `forEach` такого обязательства не имеет. Это **не** обещает, что `map` выполнялся ровно в том же порядке, на одном worker thread или только после предыдущего `forEachOrdered`. Не стройте на порядке вызовов lambda протокол записи во внешний ресурс.

`Stream.unordered()` может явно снять ограничение encounter order, когда оно не нужно результату. У некоторых источников, например `HashSet`, порядок элементов может не быть задан; `findFirst` на unordered stream не означает «первый в визуальной печати». Для операций, которым важен порядок (`limit`, `distinct` в parallel), снятие ограничения иногда меняет производительность, но **не гарантирует ускорения**. [Stream.forEachOrdered](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#forEachOrdered(java.util.function.Consumer)), [Stream.forEach](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#forEach(java.util.function.Consumer)).

## 7. parallel() не исправляет гонку и не гарантирует ускорение (ST-21, ST-29)

`parallel()` меняет режим выполнения stream, но не превращает переданные lambdas в thread-safe. Стандартные требования к behavioral parameters — **non-interfering** (не изменяют источник данных во время обхода, кроме специально разрешённых случаев) и обычно **stateless** (не зависят от изменяемого внешнего состояния, способного меняться при обработке).

~~~java
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

class SharedStateAntiPattern {
    static void bad() {
        List<Integer> shared = new ArrayList<>();
        // НЕКОРРЕКТНЫЙ ПРИЁМ, ВЫПОЛНЯТЬ НЕ НАДО:
        // IntStream.range(0, 10_000).parallel().forEach(shared::add);
        // Несинхронизированный ArrayList не становится потокобезопасным.
    }

    static List<Integer> good() {
        return IntStream.range(0, 1_000).parallel()
            .boxed()
            .toList(); // корректная сборка без shared mutable lambda
    }
}
~~~

Плохой вариант не обязан каждый раз «упасть» или даже заметно потерять элементы; **гонка существует как нарушение контракта доступа**, но конкретный симптом и исход очередного запуска недетерминированы. Даже если использовать синхронизированную коллекцию, это не всегда хорошая архитектура: конкурентная запись может убрать пользу от распараллеливания и мешать рассуждать о порядке.

Изменение исходного `List` внутри `map/filter/forEach` — другой опасный случай. Например, добавление элементов в `ArrayList` во время его обхода stream не гарантированно корректно и может вызвать `ConcurrentModificationException`, но нельзя обещать обязательное исключение для каждого источника. Граница *до начала терминального обхода* и **во время** работы отличаются: некоторые spliterators связываются с источником поздно, другие раньше. **Не** обобщайте поведение одного List на все Stream sources.

`parallel()` также не обещает, сколько потоков будет создано, какой пул будет использован, сколько будет задач и произойдёт ли ускорение. Часто параллельная обработка мелкой коллекции дороже последовательной из-за разбиения и объединения. Зависимость от IO делает оценку сложнее. Нормы: [java.util.stream package — non-interference](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html), [Stream.parallel](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/BaseStream.html#parallel()).

## 8. Spliterator и характеристики источника (ST-24)

Путь от источника к stream может строиться на **Spliterator**, который предоставляет элементы, может делить работу на части (`trySplit`) и сообщает свойства источника. Важные флаги:

| Характеристика | Значение | Чего не обещает |
|---|---|---|
| `ORDERED` | Есть определённый encounter order | Что каждый mapper выполняется в этом порядке в parallel |
| `SIZED` | Известный точный размер на рассматриваемом этапе обхода | Что любые последующие `filter`/`flatMap` сохранят известный размер |
| `SUBSIZED` | Все получающиеся после разбиения части тоже `SIZED` | Что деление одинаково эффективно для всех источников |
| `CONCURRENT` / `IMMUTABLE` | Особенности доступа к источнику | Что любой пользовательский side effect безопасен |

~~~java
import java.util.List;
import java.util.Spliterator;

class SourceProperties {
    public static void main(String[] args) {
        Spliterator<Integer> s = List.of(1, 2, 3).spliterator();
        System.out.println(s.hasCharacteristics(Spliterator.ORDERED)); // true
        System.out.println(s.hasCharacteristics(Spliterator.SIZED));   // true
        System.out.println(s.estimateSize());                          // 3
    }
}
~~~

Это **наблюдение характеристик конкретного List**, не «все Stream имеют SIZED». Именно наличие достоверного размера при некоторых pipelines позволяет `count()` получить итог, не вызывая `peek/map`. Позднее связывание (*late binding*) зависит от конкретного spliterator; нельзя делать blanket-утверждение «источник всегда снимается только при terminal». [Spliterator API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Spliterator.html), [Stream.count](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#count()).

## 9. close/onClose: у stream может быть ресурс (ST-22, ST-25)

Большинство streams из обычных коллекций не держит открытый файл или сокет. Но `Stream` реализует `AutoCloseable`, и некоторые фабрики (например, `Files.lines`) возвращают stream, чей внешний источник нужно **явно закрыть**. Терминальная операция **не вызывает автоматически** `close()` — это отдельное действие. [CORE-RESOURCES](resources.md) объясняет контракт TWR в целом.

~~~java
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class CloseHandlers {
    public static void main(String[] args) {
        List<String> events = new ArrayList<>();
        Stream<Integer> stream = Stream.of(1, 2)
            .onClose(() -> events.add("first"))
            .onClose(() -> events.add("second"));
        System.out.println(stream.count()); // 2
        System.out.println(events);         // [] — terminal не вызвал close()
        stream.close();
        System.out.println(events);         // [first, second]
    }
}
~~~

Если зарегистрировано несколько обработчиков через `onClose(Runnable)`, они выполняются **в порядке регистрации**. Если один бросает исключение, остальные всё равно будут запущены, первая ошибка распространится, остальные добавятся к ней как **suppressed**. Это не `getCause()` и не общая политика бизнес-транзакции.

~~~java
import java.util.stream.Stream;

class CloseWithErrors {
    public static void main(String[] args) {
        Stream<Integer> s = Stream.of(1)
            .onClose(() -> { throw new IllegalStateException("first"); })
            .onClose(() -> { throw new IllegalArgumentException("second"); });
        try {
            s.close();
        } catch (IllegalStateException primary) {
            System.out.println(primary.getMessage());            // first
            System.out.println(primary.getSuppressed()[0].getMessage()); // second
        }
    }
}
~~~

`onClose` принимает `Runnable`; его метод `run()` не объявляет checked `IOException`. Поэтому `onClose(() -> { throw new IOException(); })` **не компилируется** без обработки/обёртки; в G1 это отрицательная проба N15. **Не путать с checked `IOException` в `map(Function)`** — это другой SAM и отдельно разобрано в [CORE-STREAMS §10](streams.md). [BaseStream.onClose](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/BaseStream.html#onClose(java.lang.Runnable)), [BaseStream.close](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/BaseStream.html#close()).

## 10. P2: что сознательно остаётся за пределами материала (ST-33–ST-36)

**ST-33 / IO:** в реальном коде `Files.lines(path)` может удерживать открытый файловый ресурс. Типичный шаблон — `try (Stream<String> lines = Files.lines(path)) { ... }`, но здесь не открывается настоящий файл и не исследуется `UncheckedIOException` во время потребления, системная кодировка, файловые ошибки и закрытие при внешнем сбое. Это отдельный материал **IO-FILES**; не добавляем несуществующую локальную ссылку. [Files.lines API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html#lines(java.nio.file.Path)).

**ST-34 / планировщик и измерения:** параллельный Stream связан с возможностью разбиения работы, но API не нормирует конкретную универсальную схему ForkJoinPool, число workers, CPU/IO schedule или speedup. Для достоверного benchmark нужны дизайн измерений, прогрев, конфигурация JVM и отдельная работа с JMH; здесь такого эксперимента **нет**.

**ST-35 / собственный Collector:** `Collector.of(supplier,accumulator,combiner,finisher,characteristics)` позволяет объявлять свои редукции. Ошибка в identity/associativity или ложное `CONCURRENT` может сделать результат некорректным, особенно parallel. Здесь объясняется **контракт**, но не реализован и не прошёл независимую конкурентную проверку пользовательский collector. Не присваиваем ему фиктивный PASS.

**ST-36 / версии:** примеры ограничены **Java 21**. `Stream.toList` и `mapMulti` появились в Java 16, `takeWhile/dropWhile` — Java 9; методы Stream, появившиеся **после Java 21**, не приписываем JDK 21. Проверка новых JDK-версий вне G2 и не служит базой для утверждений об этом коде.

## Карта ST-01–ST-36 по приоритетам и объяснениям

| ID / уровень | Раздел и проверяемое ограничение |
|---|---|
| ST-01–ST-09 · P0 | [CORE-STREAMS](streams.md): pipeline, laziness, map/filter/flatMap, distinct/sorted/limit, find*, single-use, peek/count |
| ST-10 · P0 | §6: encounter order, forEach vs forEachOrdered, порядок действий vs mapper |
| ST-11–ST-13 · P0 | [CORE-STREAMS](streams.md) §7: три способа получения List, null и mutability |
| ST-14–ST-15 · P0 | §3: toMap duplicate/merge, выбор mapFactory и отсутствие гарантии класса Map |
| ST-16 · P0 | §4: ограничения null по конкретному Collector |
| ST-17 · P0 | §5: groupingBy/partitioningBy, порядок/тип карты |
| ST-18–ST-19 · P0 | [CORE-STREAMS](streams.md) §§5,8: IntStream/OptionalInt, find* vs *Match |
| ST-20 · P0 | §1: три overloads reduce, identity/associativity/combiner |
| ST-21 · P0 | §7: parallel и non-interference/statelessness |
| ST-22 · P0 | §9: close/onClose vs terminal |
| ST-23 · P1 | §2: Collector 4 функции, Characteristics, identity/associativity |
| ST-24 · P1 | §8: Spliterator ORDERED/SIZED/SUBSIZED, late binding |
| ST-25 · P1 | §9: onClose handlers, first exception и suppressed |
| ST-26 / ST-27 / ST-28 · P1 | [CORE-STREAMS](streams.md) §9: takeWhile/dropWhile, infinite sources, mapMulti |
| ST-29–ST-30 · P1 | §§2,7: mutable reduction, внешний shared state, изменение источника |
| ST-31 · P1 | [CORE-STREAMS](streams.md) §10: checked map(Function), deferred runtime failure |
| ST-32 · P1 | §5: groupingByConcurrent и unordered |
| ST-33–ST-36 · P2 | §10: IO-FILES, scheduler/JMH, custom Collector, новые API — **обозначены**, не реализованы |

## Источники, исполнение и проверяемость

**Нормативные источники:** [Stream API Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html), [Collectors](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collectors.html), [Collector](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collector.html), [BaseStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/BaseStream.html), [Spliterator](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Spliterator.html), [Stream package](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html), [JLS §15.27 по checked lambda](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27). Сопоставление 22 P0/10 P1/4 P2 и независимые вопросы — [G1-матрица](../../docs/plans/CORE-STREAMS-ADV.md), [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md).

Приведённые Java-блоки — **отдельные классы для самостоятельного копирования и запуска**; закомментированные антипримеры специально демонстрируют неверное API-предположение. Предполагаемый stdout основан на правилах API и исследованных фиксированных входах, но **не является свидетельством**, что все встроенные snippets данной статьи уже извлечены и успешно скомпилированы из Markdown. Фактические локальные прогоны автора, separate negative javac и CI фиксируются в PR [Issue #114](https://github.com/Melchiorius/backend-interview-labs/issues/114). Общий runnable роли 03 согласовывается в [Issue #115](https://github.com/Melchiorius/backend-interview-labs/issues/115); относительная ссылка на него будет добавлена **после** физического появления файла в едином дереве.

**Что не доказывают разовые запуски:** детерминизм `findAny` и параллельных lambda, обязательное возникновение гонки в shared List, ускорение parallel, конкретное число JVM workers, поведение произвольных источников в `peek/count`, стабильность layout/классов возвращённых Map/List. **NOT RUN до отдельной проверки:** автоматическое извлечение всех Markdown Java blocks, внешний IO/JDBC, GUI Obsidian/IDE, альтернативные JDK/JVM/OS, JMH и реальное конкурентное стресс-тестирование.

**Дальше:** повторите [pipeline/Optional/коллекторы List](streams.md) и разберите [функциональные интерфейсы](functions.md), [изменяемость и типы](generics.md), [исключения](exceptions.md), [TWR](resources.md).
