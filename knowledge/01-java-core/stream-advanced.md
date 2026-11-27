# CORE-STREAM-ADV — reduce, Collector, порядок и параллельные Stream в Java 21

**Версия:** Java SE 21 без preview. **Исходная спецификация:** [принятый G1 CORE-STREAMS/STREAM-ADV](../../docs/plans/CORE-STREAMS-ADV.md), [G2 Issue #114](https://github.com/Melchiorius/backend-interview-labs/issues/114). Перед этой статьёй прочитайте [CORE-STREAMS](streams.md): источник, промежуточные операции, отложенное выполнение, short-circuit и Optional. Здесь цель другая — объяснить, **как элементы собираются в результат, что можно объединять параллельно и какие побочные действия запрещают корректное преобразование**.

**Сквозной пример:** [CORE-STREAMS + CORE-STREAM-ADV — Java 21 runnable с проверяемым stdout](../../examples/core/streams/README.md). В P01–P33 наблюдаются обычные операции и основные ограничения, а C01–C11 отдельно демонстрируют формы `reduce` с/без identity и типы результатов `find*`/`*Match`. **44 строки — два Java-процесса, не 44 независимых теста**. Для понимания правил параллельного исполнения используйте API-ограничения из статьи, а не единственную трассу.

**Как запускать примеры.** Из корня репозитория с JDK 21 и Bash: `bash examples/core/streams/verify.sh`. Эта команда компилирует два исходника из `examples/core/streams/src/`, запускает оба класса и сравнивает объединённый вывод с эталоном. Новые наблюдения ниже относятся к отдельной временной программе Probe автора 02, а не к этому runner: [полный фактически исполненный исходник, команды, вывод и хеши](https://github.com/Melchiorius/backend-interview-labs/issues/351#issuecomment-7223616905). Короткие фрагменты с результатами объясняют этот опыт; полную программу можно воспроизвести по приложению. Отдельно выполнена [независимая временная проба 03 ClosePartitionCheck — полный исходник, команды, вывод и хеши](https://github.com/Melchiorius/backend-interview-labs/pull/352#issuecomment-7223848030). Это другая программа; не склеивайте фрагменты двух проб или блоки статьи. Ни одна из временных программ не входит в tracked runner или CI.

Ключевая идея: Stream описывает вычисление, но не обещает, что все lambdas выполнятся в одном потоке или в ожидаемом порядке. Для правильного результата важны **ассоциативность редукции, нейтральный элемент, контракт контейнеров, encounter order, non-interference и реальное владение ресурсом**. В Java SE 21 это контракты [Stream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html), [Collector](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collector.html) и [java.util.stream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html).

## 1. reduce: результат из нескольких элементов (ST-20, P0)

`reduce` сворачивает последовательность элементов в одно значение. Например, для [2, 3, 4] сумма вычисляется как `((2 + 3) + 4) = 9`. Для параллельного выполнения реализация вправе сначала получить результаты отдельных частей и затем объединить их; значит, операция редукции должна быть **ассоциативной**: `(a ⊕ b) ⊕ c = a ⊕ (b ⊕ c)`. Указанный закон — не то же самое, что коммутативность `a ⊕ b = b ⊕ a`: конкатенация строк ассоциативна, но не коммутативна.

### 1.1. Без identity → `Optional<T>`

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

В новой пробе на входе 1, 2, 3 последовательное и параллельное исполнение `reduce(10, Integer::sum)` дали 16 и 36, а `reduce(0, (a, b) -> a - b)` — −6 и −2. Это **наблюдения некорректных редукций**, не гарантированные результаты parallel. Ассоциативность математической операции также не означает ассоциативность её реализации с округлением: для double a=1e16, b=−1e16, c=1 выражения `(a+b)+c` и `a+(b+c)` фактически дали 1.0 и 0.0. См. [числа](numbers.md). Вызов `Stream.<Integer>empty().reduce(67, Integer::sum)` вернул 67 как переданный identity пустого потока (67 не является корректным identity суммы непустых данных; для неё используйте 0). `Stream.of(1, 2).reduce((u,t) -> null)` бросил NullPointerException: Optional-возвращающая перегрузка запрещает null-результат редукции.

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

Точная сигнатура трёхаргументной перегрузки: `<U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner)`. Identity должен быть нейтральным для combiner: `combiner(identity, u).equals(u)` для любого допустимого u; условие совместимости выше действует для всех допустимых u и t, а не только для примера. Accumulator и combiner должны быть stateless и non-interfering, combiner — ассоциативным. Например, если accumulator прибавляет длину строки, а combiner выбирает Math.max, max ассоциативен, но `max(2, 3) != 2+3`; кроме того 0 не нейтрален для max на произвольных отрицательных u (даже ограничение u неотрицательными не спасает совместимость). На ["a","bb","ccc"] фактически получены 6 и 3 в последовательном/параллельном режимах; с Integer::sum — 6 и корректный контракт. Последовательный успех не доказывает совместимость: combiner может вообще не вызываться. См. [функции](functions.md) и [обобщения](generics.md).

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

Три участника перегрузки `collect(Supplier<R>, BiConsumer<R,? super T>, BiConsumer<R,R>)`: `supplier` создаёт контейнер; `accumulator` добавляет очередной элемент; `combiner` соединяет два частичных контейнера. Важно: мы **не** передали один заранее созданный глобальный `ArrayList`, куда все потоки обязаны писать одновременно.

У supplier должен получаться новый пустой накопитель при каждом вызове. Два прямых вызова ArrayList::new в пробе дали разные объекты; два вызова supplier, возвращающего заранее созданный shared, дали один объект. Второй supplier не запускался в параллельной редукции. Для трёхаргументного collect combiner имеет тип BiConsumer с void-результатом и обязан перенести правый накопитель в левый. `List::addAll` это делает; `(left, right) -> right.addAll(left)` делает наоборот, и полученный в новой пробе результат [1] вместо [1, 2, 3, 4] лишь показывает нарушение контракта в конкретном разбиении. В Collector combiner — BinaryOperator с возвращаемым накопителем: он может вернуть допустимый объединённый контейнер, а не обязательно левый. Последовательная проверка не гарантирует корректность combiner. Не переносите мутацию общего identity в reduce.

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

Эквивалентность для ordered collector сравнивают после применения finisher (через equals); для UNORDERED допускается отличие порядка, если в остальном результаты эквивалентны. UNORDERED снимает требование порядка, но не обещает перемешивания. Для non-concurrent collector промежуточные контейнеры изолированы по потокам на время накопления: один и тот же контейнер обрабатывается последовательно, объединение происходит после накопления соответствующих частей. Контейнер, переданный в combiner или finisher и не возвращённый им, больше не используется реализацией редукции; после передачи в combiner или finisher этот контейнер не передаётся обратно accumulator. Это контракт жизненного цикла, а не обещание, что любой внешний ArrayList станет потокобезопасным.

Concurrent reduction для collect(Collector) выполняется, если поток parallel, collector объявил CONCURRENT и (поток unordered или collector UNORDERED). IDENTITY_FINISH требует, чтобы пропуск finisher был корректен: должно быть допустимо unchecked-приведение A к R. Сам факт, что типы похожи, недостаточен.

~~~java
// Фрагмент Probe.main, imports java.util.stream.*
Collector<String, StringBuilder, String> fin = Collector.of(
    StringBuilder::new, StringBuilder::append,
    (left, right) -> left.append(right), StringBuilder::toString);
String result = Stream.of("a", "b", "c").parallel().collect(fin);
~~~

Фактическая проверка получила abc и отсутствие IDENTITY_FINISH у fin: накопитель StringBuilder не является возвращаемой строкой. Этот collector не объявляет CONCURRENT. Полный пользовательский concurrent collector и его безопасность под нагрузкой остаются за пределами опыта.

Пример обычной безопасной сборки: `stream.parallel().collect(Collectors.toList())`. `Collectors.toList()` не обещает изменяемость или класс итогового списка, но терминальная операция реализует редукцию в соответствии с контрактом collector. Это **не** то же самое, что несколько потоков вручную вызывают `shared.add(x)`. Норма: [Collector API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collector.html), [Stream.collect](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#collect(java.util.stream.Collector)).

## 3. toMap: дубликат ключа — не «последнее значение победит» (ST-14, ST-15)

При сборке карты первый аргумент `toMap` преобразует элемент в ключ, второй — в значение. Если несколько элементов имеют ключи, равные по Object.equals, двухаргументная перегрузка **без** merge-функции бросает `IllegalStateException`.

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

При merge-функции **вы сами выбираете** политику конфликта: сохранить прежнее, заменить, объединить значения или выбросить свою ошибку. Для ordered stream и обычного non-concurrent collector ассоциативная, но некоммутативная merge-функция (например, конкатенация) допустима даже при parallel; порядок склеивания значений соответствует encounter order. При unordered/concurrent редукции порядокозависимая политика не гарантирует тот же результат. Порядок объединения значений одного ключа и порядок обхода ключей Map — разные свойства. У `Collectors.toMap(...)` нет общего обещания, что результат является `HashMap`, что он сохраняет порядок ключей или имеет гарантированную изменяемость. Если нужен определённый тип контейнера, у `toMap` есть перегрузка с `mapFactory` (например, `LinkedHashMap::new`), но требования к key/value/merge остаются.

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

Map.merge требует ненулевое входное значение; если remapping function возвращает null, существующая запись удаляется. Это не способ хранить null. Политика должна оставаться совместимой с разбиением на частичные карты: произвольное удаление при каждой коллизии не обязательно ассоциативно для более длинного входа. В новой пробе ordered parallel toMap для ["a","b","c"] с общим ключом 67 и String::concat дал значение abc; последовательная сборка двух элементов с merge, возвращающим null, — пустую карту. В TreeMap со String.CASE_INSENSITIVE_ORDER ключи "a" и "A" дали одну запись со значением 2, хотя equals у строк false: mapFactory задаёт правила сравнения/эквивалентности ключей и порядок контейнера. См. [equals](equality.md), [контракты коллекций](../02-collections/contracts.md), [сортированные коллекции](../02-collections/sorted.md).

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

Различайте три независимых null-сценария: (1) null-ключ в обычном двухаргументном toMap принят на проверенной реализации (значение 67 доступно по null), но произвольная Map может запрещать null-ключ; (2) null от valueMapper в перегрузке с merge нарушает требование ненулевого входного значения Map.merge — фактически NPE даже с HashMap::new; (3) null как результат merge удаляет запись (§3). Обе перегрузки toUnmodifiableMap — с merge и без неё — запрещают null от keyMapper и valueMapper; в новых четырёх отдельных вызовах получены NPE для каждого сочетания. Это не общая политика всех Map.

## 5. groupingBy, partitioningBy и concurrent grouping (ST-17, ST-32)

`groupingBy(classifier)` собирает элементы по ключу классификации, в базовой перегрузке в значениях — списки элементов по контракту API. `partitioningBy(predicate)` группирует по **Boolean** в две категории; при необходимости downstream collector считает количество или суммирует значения внутри групп.

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

PartitioningBy гарантирует наличие **обоих** ключей true и false, даже при пустом источнике. Для пустой части базовая перегрузка возвращает пустой List, перегрузка с downstream — результат finishing пустого downstream-контейнера (например, counting даёт 0). GroupingBy создаёт группы только для встретившихся ключей. В Probe пустой stream проверен по ключам и Map.equals, а не строковому порядку: оба списка пусты, оба счётчика 0. Для ["a","bb"] downstream filtering по length>1 после groupingBy(String::length) оставляет ключ 1 с пустым списком и ключ 2 с [bb]; upstream filter до groupingBy оставляет только ключ 2. Null от classifier в проверенном groupingBy дал NPE, не универсальное правило Map о null-ключе.

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

Для обычного groupingBy collector не concurrent: параллельная сборка должна сливать ключи частичных карт, что может быть дорогим. Встроенный groupingByConcurrent допускает не-concurrent downstream, например toList; collector сам обеспечивает необходимый режим накопления. Возвращаемые списки в ConcurrentMap не обязаны быть thread-safe для последующего конкурентного доступа. Контракт ConcurrentMap также не делает произвольные составные действия над картой атомарными. Проба с 0..99 проверила оба флага CONCURRENT/UNORDERED и размеры двух списков 50/50, не порядок, расписание или ускорение.

## 6. Encounter order: результат и вызовы lambda — разные вещи (ST-10)

**Encounter order** — определённый источником/стадиями порядок элементов (например, порядок элементов List). Он не равен очереди выполнения mapper'ов на реальных потоках.

~~~java
import java.util.List;

class OrderedTerminal {
    static void ordered() {
        List.of(1, 2, 3).parallelStream()
            .map(x -> x * 10)
            .forEachOrdered(System.out::println);
        // forEachOrdered выводит 10, 20, 30 по порядку элементов исходного List;
        // map при параллельной обработке не обязан выполняться в этом порядке
    }
}
~~~

Для ordered stream `forEachOrdered` обязуется выполнять действие в encounter order; `forEach` такого обязательства не имеет. Это **не** обещает, что `map` выполнялся ровно в том же порядке, на одном worker thread или только после предыдущего `forEachOrdered`. Не стройте на порядке вызовов lambda протокол записи во внешний ресурс.

У forEachOrdered есть более сильная гарантия: действия выполняются по одному; выполнение действия для одного элемента happens-before действия для следующего, даже если их выполняют разные потоки. Это не распространяется на mapper или произвольного внешнего читателя контейнера. В Probe изолированный ArrayList наполнялся только terminal action `IntStream.range(0,5).parallel().forEachOrdered(actions::add)` и был прочитан после возврата terminal: [0, 1, 2, 3, 4]. Внешних конкурентных читателей не было. Для parallel forEach действия могут выполняться одновременно и в недетерминированном порядке; не переносите эту гарантию на него. Это контракт API, не эксперимент по всей JMM.

`Stream.unordered()` может явно снять ограничение encounter order, когда оно не нужно результату. У некоторых источников, например `HashSet`, порядок элементов может не быть задан; `findFirst` на unordered stream не означает «первый в визуальной печати». Для операций, которым важен порядок (`limit`, `distinct` в parallel), снятие ограничения иногда меняет производительность, но **не гарантирует ускорения**. [Stream.forEachOrdered](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#forEachOrdered(java.util.function.Consumer)), [Stream.forEach](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#forEach(java.util.function.Consumer)).

## 7. parallel() не исправляет гонку и не гарантирует ускорение (ST-21, ST-29)

`parallel()` меняет режим выполнения stream, но не превращает переданные lambdas в thread-safe. Стандартные требования к behavioral parameters — **non-interfering** (не изменяют источник данных во время обхода, кроме специально разрешённых случаев) и обычно **stateless** (не зависят от изменяемого внешнего состояния, способного меняться при обработке).

Последний вызов sequential() или parallel() задаёт режим исполнения **всего pipeline**, а не только участка после вызова; методы могут вернуть тот же объект stream. `Stream.of(1,2).parallel().map(x -> x).sequential()` фактически имел isParallel=false перед терминальной операцией и дал [1,2]. Обычная терминальная операция синхронна для вызывающего кода: возвращает результат (или завершает void-действие/бросает исключение), а не Future; это не обещание, что все внутренние действия исполняются вызывающим потоком. Non-interference нужен и для sequential.

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

Наблюдаемый контраст до начала terminal: ArrayList с [1,2], созданный stream и последующий add(67) дали при toList [1,2,67]; CopyOnWriteArrayList с теми же действиями дал [1,2], потому что его spliterator работает со снимком. Проба не изменяла источник во время обхода и не демонстрирует гарантированный CME. См. [итерацию и источники](../02-collections/iteration.md).

`parallel()` также не обещает, сколько потоков будет создано, какой пул будет использован, сколько будет задач и произойдёт ли ускорение. Часто параллельная обработка мелкой коллекции дороже последовательной из-за разбиения и объединения. Зависимость от IO делает оценку сложнее. Нормы: [java.util.stream package — non-interference](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html), [Stream.parallel](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/BaseStream.html#parallel()).

## 8. Spliterator и характеристики источника (ST-24)

Путь от источника к stream может строиться на **Spliterator**, который предоставляет элементы, может делить работу на части (`trySplit`) и сообщает свойства источника. Важные флаги:

| Характеристика | Значение | Чего не обещает |
|---|---|---|
| `ORDERED` | Есть определённый encounter order | Что каждый mapper выполняется в этом порядке в parallel |
| `SIZED` | До частичного обхода/разбиения estimateSize — точное число элементов полного обхода при отсутствии структурных изменений источника | Что любые последующие `filter`/`flatMap` сохранят известный размер |
| `SUBSIZED` | Все spliterator, полученные через trySplit (в том числе потомки), имеют SIZED и SUBSIZED | Что деление одинаково эффективно для всех источников |
| `CONCURRENT` / `IMMUTABLE` | Особенности доступа к источнику | Что любой пользовательский side effect безопасен |

SORTED подразумевает ORDERED; DISTINCT означает отсутствие пары элементов, равных по equals, NONNULL — отсутствие null-элементов. CONCURRENT разрешает безопасные структурные изменения источника по его документированной политике; IMMUTABLE означает отсутствие структурных изменений элементов источника (не глубокую неизменяемость самих объектов). Характеристики spliterator источника и результата pipeline не тождественны: стадии могут сохранять лишь часть флагов, например filter не гарантирует точного размера. Проба List.of(1,2,3,4).spliterator().trySplit() получила размеры 2,2 и обе части имели SIZED+SUBSIZED; после filter getExactSizeIfKnown() вернул −1. Это не гарантия баланса каждого trySplit или алгоритма исполнения.

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

Если зарегистрировано несколько обработчиков через `onClose(Runnable)`, они выполняются **в порядке регистрации**. Если один бросает исключение, остальные всё равно будут вызваны в порядке регистрации. Первое выброшенное исключение распространяется как исходный объект; последующие отличные объекты добавляются как **suppressed**, если подавление у первого включено. Если поздний обработчик выбросил тот же объект, самоподавление пропускается; при отключённом подавлении вторичная ошибка не сохраняется. Это относится и к Error. Это не `getCause()` и не общая политика бизнес-транзакции.

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

### 9.1. Тот же Throwable, отключённое подавление и TWR

Различайте экземпляры исключений, а не только их тип и текст. Если два обработчика бросают одну и ту же переменную e, BaseStream.onClose не вызывает самоподавление этого объекта. [Throwable.addSuppressed](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html#addSuppressed(java.lang.Throwable)) в общем случае запрещает подавление самого себя, а при отключённом suppression (четырёхаргументный конструктор Throwable с enableSuppression=false) не сохраняет вторичный объект. Первый throwable может быть RuntimeException или Error; поздний Error не становится автоматически первичным.

В полной [пробе 02](https://github.com/Melchiorius/backend-interview-labs/issues/351#issuecomment-7223616905) четыре сценария используют одинаковую структуру: первый handler записывает first и бросает заранее созданный first; второй записывает second и бросает заданный second; третий записывает last. Проверки после close сравнивают **идентичность** пойманного first, null cause, точный список событий и массив suppressed с ожидаемыми объектами. Disabled — вложенный вспомогательный класс Probe, наследующий RuntimeException; его конструктор вызывает `super("disabled", null, false, true)`. Четыре вызова вспомогательного метода closeCase находятся в Probe.main; полный исходник по ссылке содержит также его реализацию и необходимые импорты. Для каждого сценария фактически получено:

| first / second | Первичный объект | suppressed | Порядок |
|---|---|---|---|
| Разные RuntimeException | тот же first | 1, именно second | first, second, last |
| Один объект RuntimeException | тот же first | 0 | first, second, last |
| Disabled / другой RuntimeException | тот же first | 0 | first, second, last |
| RuntimeException / AssertionError | тот же first | 1, именно Error | first, second, last |

Cause во всех этих пробах null. Повторный close в этих четырёх случаях не добавил событий; не обобщайте это наблюдение до обещания о любых пользовательских ресурсах/реализациях Stream.

Для сравнения, TWR при одном объекте, выброшенном из тела и close(), по переводу JLS пытается вызвать addSuppressed на самом объекте; это приводит к IllegalArgumentException (самоподавление запрещено). В Probe ресурс — AutoCloseable, тело и close бросили один RuntimeException, пойман IAE, cause совпал с исходным throwable и suppressed был пуст (форма cause — наблюдение данной JVM). Это **не эквивалентная трансляция onClose**, где тот же объект исключён из подавления. См. [TWR](resources.md), [JLS §14.20.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.3). Никакого сбоя исходного runner не обнаружено: прежний пример с двумя разными объектами корректен.

**Независимая проверка 03.** В [ClosePartitionCheck](https://github.com/Melchiorius/backend-interview-labs/pull/352#issuecomment-7223848030) метод scenario принимает Throwable first и second, вспомогательный fail выбрасывает RuntimeException или Error, а вызов close перехватывает `RuntimeException | Error`. Повторены все четыре сценария выше и добавлен пятый: AssertionError от первого обработчика остался первичным объектом, а поздний IllegalStateException — единственным подавленным объектом. Во всех пяти случаях проверены идентичность первичного объекта, null cause, точное число и идентичность suppressed, порядок [1, 2, 3]; повторный close оставил этот список неизменным именно в этих опытах. Отдельная проба TWR у 03 проверила IllegalArgumentException с cause, совпадающим с shared, пустые suppressed-массивы как у IAE, так и у исходного shared, и порядок [body, close]. Форма cause — наблюдение этой JVM; проверка обоих массивов относится именно к 03, а не к предыдущей пробе 02.

`onClose` принимает `Runnable`; его метод `run()` не объявляет checked `IOException`. Поэтому `onClose(() -> { throw new IOException(); })` **не компилируется** без обработки/обёртки; в G1 это отрицательная проба N15. **Не путать с checked `IOException` в `map(Function)`** — это другой SAM и отдельно разобрано в [CORE-STREAMS §10](streams.md). [BaseStream.onClose](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/BaseStream.html#onClose(java.lang.Runnable)), [BaseStream.close](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/BaseStream.html#close()).

## 10. P2: что сознательно остаётся за пределами материала (ST-33–ST-36)

**ST-33 / IO:** в реальном коде `Files.lines(path)` может удерживать открытый файловый ресурс. Типичный шаблон — `try (Stream<String> lines = Files.lines(path)) { ... }`, но здесь не открывается настоящий файл и не исследуется `UncheckedIOException` во время потребления, системная кодировка, файловые ошибки и закрытие при внешнем сбое. Это отдельный материал **IO-FILES**; подробный разбор оставлен для этой темы. [Files.lines API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html#lines(java.nio.file.Path)).

**ST-34 / планировщик и измерения:** параллельный Stream связан с возможностью разбиения работы, но API не нормирует конкретную универсальную схему ForkJoinPool, число workers, CPU/IO schedule или speedup. Для достоверного benchmark нужны дизайн измерений, прогрев, конфигурация JVM и отдельная работа с JMH; здесь такого эксперимента **нет**.

**ST-35 / собственный Collector:** `Collector.of(supplier,accumulator,combiner,finisher,characteristics)` позволяет объявлять свои редукции. Ошибка в identity/associativity или ложное `CONCURRENT` может сделать результат некорректным, особенно parallel. §2 демонстрирует простой non-concurrent collector с finisher; полноценная разработка concurrent collector и независимая стресс-проверка здесь не выполнялись.

**ST-36 / версии:** примеры ограничены **Java 21**. `Stream.toList` и `mapMulti` появились в Java 16, `takeWhile/dropWhile` — Java 9; методы Stream, появившиеся **после Java 21**, не приписываем JDK 21. Новые JDK-версии здесь не проверялись и не служат базой для утверждений об этом коде.

## Карта ST-01–ST-36 по приоритетам и объяснениям

Карта 36 идентификаторов — навигация к объяснениям, а не итоговый вердикт независимой проверки.

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

Часть Java-блоков — отдельные классы, часть — фрагменты методов или демонстрации. Для полного запуска новых проб используйте отдельные исходники Probe (02) и ClosePartitionCheck (03) и команды из прямых приложений выше; закомментированные антипримеры предназначены для объяснения, не для исполнения. Предполагаемый stdout основан на правилах API и исследованных фиксированных входах, но **не является свидетельством**, что все встроенные snippets данной статьи уже извлечены и успешно скомпилированы из Markdown. Исторические сведения о воспроизводимости исходных материалов собраны в [Issue #114](https://github.com/Melchiorius/backend-interview-labs/issues/114) и [авторском PR #116](https://github.com/Melchiorius/backend-interview-labs/pull/116): отдельно различаются локальные положительные пробы, отдельные ожидаемые ошибки компиляции и результаты CI. В [общем запускаемом примере](../../examples/core/streams/README.md) представлены две программы Java 21 на основе G1 §6 (P01–P33) и §5a (C01–C11) и один строго проверяемый эталон `expected.stdout`. При первоначальной проверке исходных программ роль 03 подтвердила локальный PASS `javac --release 21` / `java` / `diff -u` на Debian OpenJDK 21.0.12.1. Исторические 17/17 G1 negative и дополнительный `CheckedFunctionProbe` автор 02 отдельно проверил по диагностическим категориям, **но они не запускаются в положительном verify.sh и CI**. [GitHub Actions #44940716537](https://github.com/Melchiorius/backend-interview-labs/actions/runs/44940716537) подтвердил успешный запуск обоих первоначальных исходников на тогдашнем зафиксированном коммите; эта историческая проверка не включает отрицательные примеры и не доказывает компиляцию всех Java-блоков в статьях. Текущий исходник P01–P33 отличается от исходного варианта представлением P19 при печати; текущие исходник и README проверены отдельно в отчёте 03 ниже, а не этим старым CI.

**Фактическая временная проба 02.** [Приложение 02](https://github.com/Melchiorius/backend-interview-labs/issues/351#issuecomment-7223616905) содержит полный временный Probe.java, точные команды, stdout и SHA-256. OpenJDK/javac 21.0.12.1 Debian: одна строгая компиляция `javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d classes Probe.java` exit 0, stdout/stderr пусты. Три запуска `LC_ALL=C.utf8 java -cp classes Probe` (default en/empty и отдельно JVM-флаги -Duser.language=en -Duser.country=US, tr/TR) exit 0, stderr пуст, stdout charset UTF-8 проверен программой. По 41 строке (1023/1025/1025 байт); это наблюдения и проверки условий, не 41 JUnit-тест. Недопустимые редукции и неверный combiner печатают локальные результаты, не утверждают их как контракт. Shared supplier проверен двумя прямыми вызовами, не запускался в parallel.

**Независимая временная проба 03.** [Полный отчёт ClosePartitionCheck](https://github.com/Melchiorius/backend-interview-labs/pull/352#issuecomment-7223848030): на OpenJDK/javac 21.0.12.1 одна строгая компиляция `javac --release 21 -encoding UTF-8 -XDrawDiagnostics -Xlint:all -Werror ClosePartitionCheck.java` exit 0, stdout/stderr пусты; три JVM exit 0, stderr пуст. При явных `-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8` фактически получены default en/US и отдельные JVM en/US, tr/TR через `-Duser.language` и `-Duser.country`. Программа проверила default charset, file.encoding и stdout charset UTF-8. Каждый вывод — 9 строк/774 байта; полный исходник, команды и хеши в отчёте 03. Это другой режим окружения, чем default en/empty у 02 с C.utf8, а не противоречие. В обеих проверках новые отрицательные компиляции не выполнялись; временные программы не включены в CI.

**Runner на исходных входах (02).** Два фактических прогона неизменённого runner на четырёх восстановленных входах с совпадающими Git blob SHA и mode, не полном клоне: default POSIX и LC_ALL=C.utf8. Одна компиляция двух Java-файлов, два JVM-процесса, diff exit 0; actual.stdout 44 строки/650 байт в каждом, оба совпали с fixture (SHA-256 42c1854ebf93d1d3b6e79571a8086c9e6d96dfb81a2daeaa4e8ffece21f77f82), внешние stdout 45 строк/695 байт (44 строки и marker CHECK PASSED, SHA-256 f97f7d5661d666a7bb63a67a6cb56b569bf18bfdd894ece3e0287dec6869dfec), оба запуска exit 0, stderr пуст, cmp exit 0. Начальная опечатка P28 при ручном восстановлении временного fixture привела к несовпадению Git blob SHA и двум неуспешным diff; она исправлена до двух точных успешных запусков (подробности в приложении 02). Это не дефект репозитория или исходного runner.

**Runner на обновлённых входах (03).** В [отчёте 03](https://github.com/Melchiorius/backend-interview-labs/pull/352#issuecomment-7223848030) восстановлены четыре точных входных файла, не полный клон; текущий StreamPipelineProbes.java имеет Git blob SHA `963e2e5080ad7531b87dc7b2de19f573c709841b`. Отдельная строгая компиляция обоих tracked Java-исходников с `--release 21 -encoding UTF-8 -XDrawDiagnostics -Xlint:all -Werror` завершилась с exit 0 и пустыми stdout/stderr. Это отдельная команда, а не флаги runner. Два запуска неизменённого скрипта на обновлённых входах, default POSIX и LC_ALL=C.utf8, дали exit 0 и пустой stderr. Для каждого запуска три сравнения cmp (actual.stdout с fixture, внешний stdout с fixture + marker, перечень путей/Git blob SHA/режимов четырёх входных файлов до и после) дали exit 0: всего шесть cmp. Оба actual.stdout — 44 строки/650 байт, SHA-256 42c1854ebf93d1d3b6e79571a8086c9e6d96dfb81a2daeaa4e8ffece21f77f82; оба внешних stdout — 45 строк/695 байт, SHA-256 f97f7d5661d666a7bb63a67a6cb56b569bf18bfdd894ece3e0287dec6869dfec. Байты совпали с результатами 02.

Сам runner делает один вызов javac --release 21 для двух исходников, запускает две JVM, объединяет 44 строки наблюдений, выполняет diff -u, выводит actual.stdout через cat и отдельный marker; это не 44 теста. В скрипте нет собственных флагов lint/Werror/явной encoding, проверки пустоты stderr или хешей. В обеих локальных средах доступны OS locales C/C.utf8/POSIX; en_US/tr_TR не установлены, полный runner в этих системных локалях не выполнялся. Отдельные JVM-флаги проверены во временных пробах. Исторические 17 compiler-negative и CheckedFunctionProbe, новые временные пробы 02/03 и 24 EXPECTED REJECTION всей CI-сборки на HEAD `e193d5ec7cf005e99bfba0f991b0315516976c22` ([run 46037509467](https://github.com/Melchiorius/backend-interview-labs/actions/runs/46037509467)) — разные наборы доказательств. Новых локальных запусков в этой редакционной правке не проводилось.

**Граница переносимости эталона.** В историческом [G1, строки 216–217](https://github.com/Melchiorius/backend-interview-labs/blob/472b5ce970f9d61c96e9a1bdaf8a0b5c9375d08e/docs/plans/CORE-STREAMS-ADV.md#L216-L217) P19 печатал Map напрямую; наблюдение 02 дало `{false=[1, 3], true=[2, 4]}`. В текущем [StreamPipelineProbes.java](../../examples/core/streams/src/StreamPipelineProbes.java), L38, результат partitioningBy перед печатью помещается в отдельный TreeMap только для стабильного представления. API partitioningBy гарантирует обе категории, но не класс Map и не порядок обхода ключей возвращённой карты. Независимая проба 03 проверила наличие обоих ключей, списки [1, 3] и [2, 4] в encounter order по отдельным ключам, оба пустых списка на пустом входе, а также сортированное представление. Байты fixture не изменились. P09: в пробе 03 проверено условие count == 3, а `peek=0` только наблюдалось. Строгий эталон намеренно сохраняет это реализационно-зависимое наблюдение: на иной допустимой реализации сверка может не пройти даже при правильном count; фактического отказа на другой JVM не наблюдали. Исходный код P09, runner и fixture сохранены, а текущий README (L3/83/85/105, ссылка выше) уточняет историческую атрибуцию, границы P09/P19 и статус материала.

**Что не доказывают разовые запуски:** детерминизм `findAny` и параллельных lambda, обязательное возникновение гонки в shared List, ускорение parallel, конкретное число JVM workers, поведение произвольных источников в `peek/count`, стабильность layout/классов возвращённых Map/List. **NOT RUN до отдельной проверки:** автоматическое извлечение всех Markdown Java blocks, внешний IO/JDBC, GUI Obsidian/IDE, альтернативные JDK/JVM/OS, JMH и реальное конкурентное стресс-тестирование.

**Дальше:** повторите [pipeline/Optional/коллекторы List](streams.md) и разберите [функциональные интерфейсы](functions.md), [изменяемость и типы](generics.md), [исключения](exceptions.md), [TWR](resources.md).
