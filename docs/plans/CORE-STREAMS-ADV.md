# CORE-STREAMS + CORE-STREAM-ADV — исследование Java Stream API 21 (G1)

**Статус:** исследовательская спецификация до написания статей. Роль **01 — Research & Coverage**, [Issue #108](https://github.com/Melchiorius/backend-interview-labs/issues/108) / [поручение 00](https://github.com/Melchiorius/backend-interview-labs/issues/108#issuecomment-7064660328). **Нормативная версия:** Java SE **21** (без preview), Stream API; при необходимости JLS 21 по lambda. **BASE рабочего PR:** `e9c8c1b1764e6bb92cb099a29e9a37ef948df8a5` — новый фактический `main` после G6 [PR #109](https://github.com/Melchiorius/backend-interview-labs/pull/109); при выдаче поручения 00 база была `64dfbabdc76995ae289a7cd5f8ea5d917b757564`. Между назначением и созданием ветки PR #109 изменил только `docs/COVERAGE.md` и `docs/CURRICULUM.md`. HEAD и GitHub CI фиксируются в PR **после** коммита. **G1 не принят, G2 не начат.**

## 1. Обещание будущего материала и рекомендуемое разделение

**Цель:** читатель должен по коду предсказать результат, понять, когда и какие стадии выполняются, отличить последовательный encounter order от порядка вызова lambda, выбрать корректный Collector, оценить single-use/close/null/duplicate-key риски и не применять shared mutable state в параллельном pipeline.

**Предлагаю две самостоятельные, но согласованные статьи** с единым исследовательским планом:
1. **CORE-STREAMS — «Построение Stream и вычисление pipeline»**: source→intermediate→terminal, lazy, `filter/map/flatMap`, short-circuit, `limit/findFirst/findAny`, `Optional` как результат терминального вызова, single-use, `distinct/sorted`, примитивные streams, `Stream.toList()` и базовые Collectors. Должны быть явные предупреждения, что `peek`/side effects могут не выполняться.
2. **CORE-STREAM-ADV — «Контракты Collector, порядок, side effects и параллельное выполнение»**: mutable reduction, `groupingBy/toMap` duplicate/null, `Collector` identity/associativity, Spliterator, encounter order, `forEachOrdered`, non-interference/statelessness, `parallel`, `close/onClose`, пределы performance и concurrent grouping.

**Почему две:** основные операции и методические вопросы о запуске pipeline сами по себе представляют цельную P0-основу, но advanced-контракты mutable reduction/parallel/Collector требуют отдельного механизма и значительно большего количества контрпримеров. Это **рекомендация 01**, не утверждённое решение 00: допустима одна связная статья с двумя навигационными разделами, если 00/02/04 сочтут её удобнее. **Нельзя** объявлять `CORE-STREAMS` и `CORE-STREAM-ADV` готовыми из факта существования данного плана.

**Рекомендация по runnable:** один короткий автономный Java 21 source с ожидаемым stdout полезен, потому что позволяет сверить разнообразные результаты и ошибки в связной демонстрации; сделать единый `examples/core/streams/` с `verify.sh` **только если роль 03 после G1 согласует этот объём и проверит сценарий чистого клона**. 17 compile-negative probes можно оставить как источники в плане или разделить на runnable negatives, если pedagogical benefit доказана. Отдельное упражнение с JUnit/Mockito/Gradle-модулем **не требуется по формальному признаку**: см. [EXERCISE_AND_TESTING_STANDARD](../EXERCISE_AND_TESTING_STANDARD.md).

## 2. Сверка опубликованного main и границы

[CURRICULUM](../CURRICULUM.md) и фактический [COVERAGE](../COVERAGE.md) после G6 [PR #109](https://github.com/Melchiorius/backend-interview-labs/pull/109) фиксируют **17 опубликованных Java Core статей**; STREAMS/STREAM-ADV остались **только curricular ID**. [JAVA_CORE_PLAN](../JAVA_CORE_PLAN.md) §«Функциональный стиль» включает оба ID, а таблица больших примеров рекомендует интегрированную демонстрацию для Stream. Ни отсутствие файла, ни уже существующее краткое упоминание не означают исчерпывающего покрытия всех связанных вопросов.

- [CORE-FUNCTIONS](../../knowledge/01-java-core/functions.md) — уже разобраны `Function/Predicate`, SAM, lambda, capture и checked exceptions: в новой статье нужно **применять**, не повторять целиком.
- [CORE-GENERICS](../../knowledge/01-java-core/generics.md) и [CORE-ERASURE](../../knowledge/01-java-core/erasure.md) — типы `Stream<T>`, `Optional<T>`, invariance и type inference; не пересказывать PECS/bridge.
- [CORE-EXCEPTIONS](../../knowledge/01-java-core/exceptions.md) и [CORE-RESOURCES](../../knowledge/01-java-core/resources.md) — исключения в lambda, `AutoCloseable`, `try-with-resources`; показать только специфическую границу stream `close()`/deferred errors.
- [CORE-EQUALITY](../../knowledge/01-java-core/equality.md) — требования к `equals/hashCode` при `distinct`/key collisions.
- [Общий индекс runnable](../../examples/core/README.md), [план Java Core](../JAVA_CORE_PLAN.md), [реестр источников](../SOURCE_REGISTRY.md).

**Будущие темы, без фиктивных локальных ссылок:** `COL-CONTRACTS`, `COL-ITER`, `COL-CONCURRENT`, `CON-FORKJOIN`, `CORE-OPTIONAL`, `IO-FILES`, `JVM-JIT`, `ALG-COMPLEXITY`. На данный момент полноценный самостоятельный блок Collections в Java Core ещё не должен автоматически считаться опубликованным. Не смешивать `java.util.stream.Stream` с `java.io.InputStream` или с Reactor `Flux` / Spring WebFlux.

## 3. Источники A — нормативные спецификации Java SE 21

Первичный реестровый источник — [OFF-API21](../SOURCE_REGISTRY.md). **Для семантики Stream API первичен Java 21 Javadoc**, не JLS и не произвольное поведение конкретного OpenJDK.

| API Java 21 | Что гарантируется / что НЕ гарантируется |
|---|---|
| [java.util.stream package](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html) и [Stream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html) | Source→intermediate→terminal, laziness, возможность elide whole stages, non-interfering/stateless behavioral parameters; side effects не обещают порядок, видимость и обязательный вызов (кроме специально документированных terminal операций). |
| [Stream.map/filter/flatMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html) | `map` преобразует, `filter` отбирает, `flatMap` разворачивает вложенные streams и закрывает mapped stream после переноса его элементов; `peek` в оптимизированных pipelines не гарантирован как instrumentation. |
| [Stream.findFirst/findAny/limit](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html) | `findFirst` соблюдает encounter order при его наличии; `findAny` может вернуть любой элемент и недетерминирован; short-circuit может пропускать обход и вызовы функций. |
| [Stream.toList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#toList()) | Начиная с Java 16, unmodifiable List, encounter order если задан; implementation class и identity не гарантируются. В Java 21 OpenJDK наблюдается поддержка null (P10). |
| [Collectors](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collectors.html) | `toList()` **не гарантирует** mutability/type/thread-safety; `toUnmodifiableList()` запрещает null; `toMap` без merge отвергает duplicate keys; `toUnmodifiableMap` запрещает null keys/values. Не распространять одно правило null на все overloads/collectors. |
| [Collector](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collector.html) | `supplier/accumulator/combiner/finisher`, identity/associativity и Characteristics; корректный parallel collector не требует записи в один shared ArrayList из lambda. |
| [BaseStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/BaseStream.html) | `close/onClose`, `sequential/parallel/unordered`; **terminal сам по себе не вызывает stream.close()**. `onClose` handlers запускаются при close в порядке регистрации. |
| [IntStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/IntStream.html) | Специализация примитивного `int`; `sum`, `boxed`, `collect(Supplier,ObjIntConsumer,BiConsumer)` — не те же методы, что `Stream<Integer>`. |
| [Spliterator](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Spliterator.html) | ORDERED/SIZED/SUBSIZED, binding и splitting; характеристики конкретного источника, не всех Stream; use `SIZED` для понимания некоторых оптимизаций `count`, но не утверждать, что любой источник SIZED. |
| [Optional](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html) и [OptionalInt](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/OptionalInt.html) | Terminal `find*` возвращает Optional/primitive optional, а не nullable-значение; полная тема `CORE-OPTIONAL` не поглощается STREAMS. |
| [JLS 21 §15.27](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27) | Только правила lambda и проверяемых исключений относительно SAM; спецификация Stream pipeline — в библиотечном API. |

**Оговорка о производительности:** API не гарантирует конкретный пул, число потоков, speedup или поток исполнения отдельной lambda. Пробные `parallel()`/sequential `time` не доказывают преимуществ для всех CPU/JVM/source; требуются отдельная методика и JMH/стабилизированные условия.

## 4. Источники C — реально найденные независимые вопросы

Записи уже есть в [SOURCE_REGISTRY](../SOURCE_REGISTRY.md), новых ID не требуется:

- **Q-HRUDAY:** [Core-Java-Interview-Questions-List](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md#chapter-17---java-stream-api), **Chapter 17 «Java Stream API» (в оглавлении указано 186 вопросов)**: fundamentals, creating, intermediate/terminal, parallel, collectors, `toList` vs `Collectors.toList`, `Spliterator`. Проверены оглавление и список prompts, а не все ответы или заявленная частотность.
- **Q-CHERNYSHOV:** [русскоязычные вопросы Java Developer](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/master/ru/README.md), **§8 «Stream API» (29 вопросов)**: lazy, `reduce/collect/groupingBy`, reuse, parallel pitfalls, Optional. Повторы/уровни не нормативны.
- **Q-TEAMLEAD:** [java-interview-questions](https://github.com/teamlead/java-interview-questions), явные формулировки `What is Stream`, `Stream begins its execution`, `parallel stream`, `how to create stream`, `mapToInt/flatMap`, `intermediate/final operations`. **Есть также IO-stream questions — не путать** с Stream API.
- **Q-IN28:** [JavaInterviewQuestionsAndAnswers](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/master/readme.md) — раздел **Functional Programming - Lambda expressions and Streams**, вопросы 210–212 (Stream, пример, intermediate/terminal). Узкая подборка: не заявлять, что она содержит все современные `Stream.toList`/Collector pitfalls.
- **Q-BAELDUNG-CORE:** [Baeldung Java Interview Questions](https://www.baeldung.com/java-interview-questions) — есть в реестре, но **все Stream-ответы этой страницы самостоятельно не проходились**; не приписывать ей проверку специфических Java 21 гарантий.

**Вывод по полноте:** банки подсказывают обычные вопросы про pipeline/reuse, а важные corner cases null/duplicate/short-circuit/Collector contracts проверены отдельно через API и Java 21 эксперименты. Независимые вопросы не являются нормативным свидетельством и не дают объективного процента встречаемости на интервью.

## 5. Матрица вопросов: 36 ID, 22 P0 / 10 P1 / 4 P2

`P01–P33` — **33 строки stdout одного Java-процесса (§6)**, НЕ 33 JUnit-теста и не 33 отдельных компиляции. `N01–N17` — **17 независимых compile-negative files (§7)**, каждый проверяется отдельно. В матрице **P0** должен превратиться в полное объяснение с кодом либо явно обозначенную проектную границу, **P1** — в объяснение/переход, **P2** — в честный хвост, не объявляемый реализованным.

| Вопрос | Проверяемый смысл | Норма | Проверка / соседство | Статья |
|---|---|---|---|---|
| ST-01 **P0** | Поток вычислений, а не хранилище; source + intermediate + terminal. | Stream package, Stream API | P01/P02; CORE-FUNCTIONS | STREAMS |
| ST-02 **P0** | Ленивая композиция: промежуточная стадия не обходит источник до terminal; вызовы функций могут быть оптимизированы. | Stream, package side-effects | P01–P03/P09 | STREAMS |
| ST-03 **P0** | Типизированные map/filter: преобразование против отбора; не путать Predicate<T> и Function<T,R>. | Stream.map/filter | P02/P06, N01/N11 | STREAMS |
| ST-04 **P0** | flatMap: каждый элемент преобразуется во внутренний Stream и разворачивается; вернёт Stream, не List. | Stream.flatMap | P04, N04 | STREAMS |
| ST-05 **P0** | distinct/sorted — stateful intermediate operations; цена, буферизация и порядок в зависимости от источника. | Stream.distinct/sorted | P05 | STREAMS |
| ST-06 **P0** | limit и краткое вычисление; терминальный результат не требует посещения всех элементов; бесконечные источники — отдельные условия. | Stream.limit, package laziness | P06, P28–P29 | STREAMS |
| ST-07 **P0** | findFirst — первый в encounter order, если он есть; findAny может выбрать любой; нельзя фиксировать значение parallel findAny. | Stream.findFirst/findAny | P07/P08, Optional API | STREAMS |
| ST-08 **P0** | Один stream pipeline — одна употребляемая последовательность; после terminal повтор не разрешён и может обнаруживаться раньше. | Stream overview, IllegalStateException | P22 | STREAMS |
| ST-09 **P0** | Терминальная операция инициирует обработку, но map/peek/side-effects не обязаны вызываться для всех элементов; оптимизация count. | Stream.count, package side-effects | P09/P31; наблюдение vs гарантия | STREAMS |
| ST-10 **P0** | Encounter order результата не означает порядка/потока исполнения пользовательских функций; forEach и forEachOrdered различны. | Stream.forEach/forEachOrdered | P25 | STREAM-ADV |
| ST-11 **P0** | Stream.toList() (с Java 16) возвращает unmodifiable List; порядок по encounter; тест null на конкретном Java 21. | Stream.toList | P10/P11; N06 | STREAMS |
| ST-12 **P0** | Collectors.toList() НЕ обещает изменяемость, тип, потокобезопасность или сериализуемость; не приравнивать к Stream.toList(). | Collectors.toList | P12 | STREAMS |
| ST-13 **P0** | Collectors.toUnmodifiableList() отвергает null; сравнить с наблюдением Stream.toList() для null. | Collectors.toUnmodifiableList | P10/P13 | STREAMS |
| ST-14 **P0** | Collectors.toMap без merge-функции выбрасывает IllegalStateException на дубликате ключей. | Collectors.toMap(Function,Function) | P14 | STREAM-ADV |
| ST-15 **P0** | toMap с mergeFunction решает collision явной политикой; класс Map и порядок отображения не гарантируются. | Collectors.toMap(Function,Function,BinaryOperator) | P15/N08/N09 | STREAM-ADV |
| ST-16 **P0** | Контракты null зависят от метода: toUnmodifiableMap запрещает null key/value; toMap с null mapped value — конкретный диагностический опыт. | Collectors.toUnmodifiableMap, toMap | P16/P17; различать API и OpenJDK | STREAM-ADV |
| ST-17 **P0** | groupingBy и partitioningBy: группировка по classifier или boolean, не обещать тип конкретной Map и порядок ключей. | Collectors.groupingBy/partitioningBy | P18/P19, N17 | STREAM-ADV |
| ST-18 **P0** | IntStream/LongStream/DoubleStream: примитивные специализации, mapToInt, boxed; не путать Stream<Integer> и IntStream. | IntStream, Stream.mapToInt | P20/P21, N02/N03/N05/N12 | STREAMS |
| ST-19 **P0** | `Stream.findFirst/findAny` → `Optional<T>`, `IntStream.findFirst/findAny` → `OptionalInt` (аналоги `OptionalLong/OptionalDouble` для других primitive streams). `anyMatch/allMatch/noneMatch` возвращают `boolean`; для пустого Stream соответственно `false/true/true`. `Optional<T>.orElse(T)` — метод Optional, не boolean. | Stream.findFirst/findAny/anyMatch/allMatch/noneMatch, Optional/OptionalInt API | P27, N07/N13/N16; **C06–C11** из §5a | STREAMS |
| ST-20 **P0** | `Stream.reduce(BinaryOperator)` и `IntStream.reduce(IntBinaryOperator)` **без** identity → `Optional<T>` / `OptionalInt`; overload `reduce(identity, accumulator)` — **с** нейтральным identity, трёхаргументный `Stream.reduce(identity, accumulator, combiner)` требует совместимости accumulator/combiner. Редукция должна соблюдать ассоциативность; `reduce(10, Integer::sum)` не задаёт нейтральный элемент для суммы. Отделить от mutable `collect`. | Stream.reduce overloads, IntStream.reduce, Collector | **C01–C05** из §5a; P26/N14 не доказывают различие overloads | STREAM-ADV |
| ST-21 **P0** | parallel() не означает асинхронность или ускорение, и не защищает побочные изменения общего ArrayList; stateless/non-interfering. | Stream package, Collector | P26; unsafe shared state не запускать как гарантированный fail | STREAM-ADV |
| ST-22 **P0** | BaseStream.close/onClose: terminal не обязан закрывать источник; try-with-resources и закрытие обработчиков в порядке регистрации. | BaseStream.close/onClose, Stream | P23/P24, N15 | STREAM-ADV |
| ST-23 **P1** | Collector: supplier/accumulator/combiner/finisher, identity/associativity, CONCURRENT/UNORDERED/IDENTITY_FINISH. | Collector API | Схема partitions -> combine, отдельно без готового custom collector | STREAM-ADV |
| ST-24 **P1** | Spliterator.ORDERED/SIZED/SUBSIZED, late binding и splitting объясняют допустимые оптимизации; не обобщать на любой источник. | Spliterator, Stream package | P09/parallel; теоретическая граница | STREAM-ADV |
| ST-25 **P1** | onClose с несколькими обработчиками и исключениями: порядок, подавленные ошибки; не равно автоматическому закрытию. | BaseStream.onClose API | P23/P24; дополнительная проба при G2 | STREAM-ADV |
| ST-26 **P1** | takeWhile/dropWhile: префикс упорядоченного потока, иные границы для unordered/parallel. | Stream.takeWhile/dropWhile | P28/P29 | STREAMS |
| ST-27 **P1** | iterate/generate/ofNullable, infinite pipelines; short-circuit не гарантирует termination при неправильно упорядоченных stateful стадиях. | Stream.iterate/generate/ofNullable | P32/P33; не запускать заведомо зависающие случаи | STREAMS |
| ST-28 **P1** | mapMulti и плоское отображение, где полезнее flatMap; новые методы Java 16+ и граница версий. | Stream.mapMulti | Встроенный фрагмент только после проверки | STREAMS |
| ST-29 **P1** | Mutable reduction и ошибки совместного состояния: collect(Supplier,Accumulator,Combiner), почему collect(Collectors.toList()) уместнее side effects. | Stream.collect, Collector | Схема и контрпример опасного shared ArrayList (not run) | STREAM-ADV |
| ST-30 **P1** | Источник из Collection, мутация до/после terminal и условия non-interference; view/late binding не универсальны. | Stream package, Spliterator | Граничный сценарий, не объявлять deterministic ConcurrentModificationException | STREAM-ADV |
| ST-31 **P1** | Checked exception в `Stream.map(Function)` / `filter(Predicate)` нельзя выбросить из lambda без обработки, так как SAM не объявляет такой `throws`; unchecked ошибка из mapper может проявиться лишь на terminal. | CORE-FUNCTIONS, JLS 15.27, Function/Predicate API | P31 (отложенная runtime-ошибка), **проверенный `CheckedFunctionProbe` из §5a** (checked); N15 касается только `onClose(Runnable)`, не `map/filter` | STREAMS |
| ST-32 **P1** | groupingByConcurrent и collect с CONCURRENT/UNORDERED: не обещать encounter order в concurrent grouping; trade-off. | Collectors.groupingByConcurrent, Collector | Нормативный разбор, без perf claim | STREAM-ADV |
| ST-33 **P2** | Потоки из Files.lines и иных IO источников: закрытие AutoCloseable, исключения чтения/IO. | Files API, CORE-RESOURCES | Граница будущего IO-FILES | STREAM-ADV |
| ST-34 **P2** | Параллельные стримы и ForkJoinPool, планирование/производительность/CPU-bound vs IO-bound, реальные бенчмарки. | Stream, Spliterator, CON-FORKJOIN | Отдельный эксперимент, не JDK scheduler guarantee | STREAM-ADV |
| ST-35 **P2** | Собственный Collector, нарушения associativity/identity, concurrency characteristics — самостоятельная проверка. | Collector API | Контрпример/тест при G2 если нужен | STREAM-ADV |
| ST-36 **P2** | API новых JDK после 21, Stream Gatherers и чужие версии; не переносить новые методы задним числом. | Version scope Java SE 21 | Только граница совместимости | STREAM-ADV |

## 5a. Точечные нормативные проверки ST-20 / ST-19 / ST-31 (после G1-review 04)

**B1 / ST-20.** Официальные контракты Java SE 21: [Stream.reduce(BinaryOperator)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#reduce(java.util.function.BinaryOperator)), [Stream.reduce(T, BinaryOperator)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#reduce(T,java.util.function.BinaryOperator)), [Stream.reduce(U, BiFunction, BinaryOperator)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#reduce(U,java.util.function.BiFunction,java.util.function.BinaryOperator)), [IntStream.reduce(IntBinaryOperator)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/IntStream.html#reduce(java.util.function.IntBinaryOperator)). Без identity результат выражается через `Optional` / `OptionalInt`, пустой Stream даёт пустой Optional. С явным identity он должен быть нейтральным; для трёхаргументной формы дополнительно важна совместимость accumulator и combiner. Редукция основана на ассоциативной операции. P26 (sum) и N14 (ошибка типов) **не являются прямым доказательством** этих перегрузок.

**B2 / ST-19.** [Stream.findFirst](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#findFirst()), [Stream.anyMatch](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#anyMatch(java.util.function.Predicate)), [Stream.allMatch](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#allMatch(java.util.function.Predicate)), [Stream.noneMatch](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#noneMatch(java.util.function.Predicate)), [IntStream.findFirst](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/IntStream.html#findFirst()), [Optional.orElse](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html#orElse(T)): `find*` возвращают Optional-объект, `*Match` — **примитивный boolean**. Для пустого Stream: `anyMatch = false`, `allMatch = true`, `noneMatch = true` (vacuous truth). `orElse` применяется к Optional, не к boolean.

**Отдельный positive probe**, не часть сохранённого источника P01–P33. Полный файл `StreamG1Clarifications.java`:

```java
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
```

**Фактически проверено отдельно ролью 01:** OpenJDK/javac **21.0.12.1**, `javac --release 21 -d out StreamG1Clarifications.java` — exit 0; `java -cp out StreamG1Clarifications` — exit 0, **11/11 строк** совпали с ожидаемыми (одна компиляция + один запуск, не 11 независимых тестов):

```text
C01=Optional[6]
C02=Optional.empty
C03=6
C04=6
C05=OptionalInt[6]
C06=Optional[1],OptionalInt[1]
C07=true
C08=false
C09=true
C10=true
C11=67
```

**N1 / ST-31.** Прямой отдельный отрицательный пример checked в `Stream.map(Function)`, **не** переименовывает, не заменяет и не увеличивает список N01–N17:

```java
import java.io.IOException;
import java.util.stream.Stream;
class CheckedFunctionProbe {
    void work() { Stream.of(1).map(x -> { throw new IOException("io"); }); }
}
```

`javac --release 21 -d out-negative CheckedFunctionProbe.java` на OpenJDK 21.0.12.1: **exit 1**, категория `unreported exception IOException; must be caught or declared to be thrown`, не parser/EOF error. Причина — `Function.apply` не объявляет checked `throws IOException`; обработка возможна внутри lambda либо через другой SAM-контракт. **N15** из §7 — иное ограничение `onClose(Runnable)` и не используется как прямое доказательство для `map`.

**Границы новых проверок:** выполнены только эти две точечные Java 21 компиляции и один запуск C01–C11. **Сохранённый** source §6 с P01–P33, исходники N01–N17 и их ранее проверенные результаты **не менялись и заново не запускались** в рамках данной дельты. GitHub CI не извлекает пробы из Markdown; Obsidian/IntelliJ GUI, другие JVM, benchmark/JMH, реальный IO/JDBC/Spring — not run.

## 6. Positive и runtime-ловушки — полная программа для Java 21

Создать **один** файл `StreamPipelineProbes.java`, в отдельном временном каталоге выполнить:

```bash
mkdir -p out
javac --release 21 -d out StreamPipelineProbes.java
java -cp out StreamPipelineProbes > actual.stdout
# Скопировать нижний ожидаемый блок в expected.stdout
diff -u expected.stdout actual.stdout
```

**Авторские фактические проверки (OpenJDK/javac 21.0.12.1):** одна успешная компиляция `--release 21`, один успешный запуск, **33 stdout-строки**, приведённые ниже. В выводе **P11/P13/P14/P16/P17/P22/P31 — перехваченные runtime failures**, они не являются случаями «не компилируется». Program stdout содержит только class simple name для этих исключений; текст сообщения/runtime stack trace не фиксируется.

```java
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
        show("P19", Stream.of(1, 2, 3, 4).collect(Collectors.partitioningBy(x -> x % 2 == 0)));
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
```

**Фактический stdout для этого исходника, OpenJDK 21.0.12.1:**

```text
P01=0
P02=[2, 4, 6]
P03=3
P04=[a, b, c]
P05=[1, 2, 3]
P06=[4, 6]
P07=7
P08=true
P09=3,peek=0
P10=[a, null]
P11=UnsupportedOperationException
P12=2
P13=NullPointerException
P14=IllegalStateException
P15={a=2, b=1}
P16=NullPointerException
P17=NullPointerException
P18={even=[2, 4], odd=[1, 3]}
P19={false=[1, 3], true=[2, 4]}
P20=10
P21=[1, 2, 3]
P22=IllegalStateException
P23=before=
P24=after=AB
P25=1234
P26=500500
P27=fallback
P28=[1, 2]
P29=[3, 4]
P30=a,b
P31=IllegalStateException
P32=0
P33=[1, 2, 3, 4]
```

**Обязательно объяснить, что означает каждая строка, а не только показать её:**
- **P01–P03:** построение lazy stage не вызывает mapper; `toList()` материализует три выходных значения (поэтому mapper для каждого вызван). Это не разрешает полагаться на side effects для любого terminal.
- **P04–P06:** flatMap, distinct/sorted и ограничение числа выходных значений после фильтра.
- **P07/P08:** parallel ordered `findFirst` гарантирует первый подходящий (=7); для `findAny` проверяется лишь **наличие** результата. Не фиксировать конкретное число от parallel findAny.
- **P09:** в этой реализации `count()` на SIZED source дал `peek=0`. **Это законная оптимизация, а не контракт «peek всегда пропускается»**.
- **P10–P13:** `Stream.toList` допускает null в наблюдаемом JDK 21, но возвращает unmodifiable List; `Collectors.toList()` имеет лишь нефиксированный контракт mutability, `toUnmodifiableList` отвергает null.
- **P14–P17:** duplicate key без merge → ISE; merge `Integer::sum` → `{a=2,b=1}` (для печати создан `TreeMap`, **не** гарантия `toMap`-порядка); `toUnmodifiableMap` rejects null; mapped null value в `toMap` вызвал NPE в данном OpenJDK. Не объявлять null-правило любых Map collector перегрузок одинаковым.
- **P18/P19:** group keys и partitioning; `TreeMap` применяется лишь для стабилизации вывода P18.
- **P20/P21:** `IntStream.sum()` и явный `boxed()` дают разные типы операций; N03/N12 доказывают ограничения типов.
- **P22:** повторный terminal на уже употреблённом stream → ISE в проверенном коде; спецификация предупреждает, что обнаружение reuse не гарантируется для всех форм intermediate reuse.
- **P23/P24:** terminal `count()` не закрыл stream; только явный `close()` вызвал оба close handlers в регистрации A→B. Успех terminal не означает release файлового дескриптора.
- **P25/P26:** `forEachOrdered` обеспечивает encounter order, параллельная `sum` корректна для ассоциативного сложения; **не** обещает порядок вызова mapper или число threads.
- **P27–P33:** пустой Optional terminal, ordered prefixes `takeWhile/dropWhile`, `joining`, отложенная ошибка mapper при terminal, `Stream.ofNullable(null)` и ограниченный iterate.

## 7. Negative javac — полные отдельные исходники, 17/17 категорий

Каждый ряд — **полный независимый source** в файле `negative/<ID>.java`; нельзя склеивать в один Java-файл. Внутри Markdown-таблицы вертикальные черты при необходимости экранированы `\\|`, при восстановлении файла нужно удалить только Markdown-escape, оставив Java `|`. Команды для каждого ID:

```bash
mkdir -p out-negative
javac --release 21 -d out-negative negative/N01_MismatchedMapper.java
# ожидается nonzero именно из-за типов Function в map
# затем отдельно: javac --release 21 -d out-negative negative/N02_MapToIntResult.java
# аналогично N03...N17; проверять категорию каждого diagnostic, не только exit code
```

| Файл (ID) | Полный отдельный Java source | Ожидаемая и фактически полученная категория отказа javac |
|---|---|---|
| `N01_MismatchedMapper` | `import java.util.stream.*; class N01_MismatchedMapper {void f(){Stream.of(1,2).map((String x)->x.length());}}` | map обрабатывает Integer, не String; несовместимый Function. |
| `N02_MapToIntResult` | `import java.util.stream.*; class N02_MapToIntResult {void f(){Stream.of("a").mapToInt(String::toUpperCase);}}` | mapToInt требует int-результат, ссылка возвращает String. |
| `N03_IntStreamCollector` | `import java.util.stream.*; class N03_IntStreamCollector {void f(){IntStream.of(1,2).collect(Collectors.toList());}}` | IntStream.collect принимает supplier/ObjIntConsumer/combiner, не Collector. |
| `N04_FlatMapRequiresStream` | `import java.util.*; import java.util.stream.*; class N04_FlatMapRequiresStream {void f(){Stream.of(1).flatMap(x->List.of(x));}}` | flatMap mapper возвращает Stream, не List. |
| `N05_SumOnObjectStream` | `import java.util.stream.*; class N05_SumOnObjectStream {void f(){int n=Stream.of(1,2).sum();}}` | sum есть у примитивного IntStream, не у Stream<Integer>. |
| `N06_ToListBadType` | `import java.util.*;import java.util.stream.*;class N06_ToListBadType {void f(){List<String> s=Stream.of(1,2).toList();}}` | List<Integer> не может быть присвоен List<String>. |
| `N07_OrElseType` | `import java.util.stream.*;class N07_OrElseType {void f(){int n=Stream.of(1,2).findFirst().orElse("fallback");}}` | Optional<Integer>.orElse ожидает Integer, не String. |
| `N08_CollectorWrongArity` | `import java.util.stream.*;class N08_CollectorWrongArity {void f(){Stream.of(1).collect(Collectors.toMap(x->x));}}` | toMap(Function) не существует: нужны keyMapper и valueMapper. |
| `N09_ToMapKeyWrongType` | `import java.util.*;import java.util.stream.*;class N09_ToMapKeyWrongType {void f(){Map<String,Integer> m=Stream.of(1).collect(Collectors.toMap(x->x,x->x));}}` | Inference даёт Map<Integer,Integer>, несовместимый ключ. |
| `N10_ForEachBiConsumer` | `import java.util.stream.*;class N10_ForEachBiConsumer {void f(){Stream.of(1).forEach((a,b)->{});}}` | forEach принимает Consumer с одним аргументом. |
| `N11_FilterReturnsInt` | `import java.util.stream.*;class N11_FilterReturnsInt {void f(){Stream.of(1).filter(x->x+1);}}` | filter ожидает boolean predicate, не int. |
| `N12_PrimitiveBoxed` | `import java.util.stream.*;class N12_PrimitiveBoxed {void f(){Stream<Integer> s=IntStream.range(0,3);}}` | IntStream не присваивается Stream<Integer> без boxed(). |
| `N13_OptionalIntType` | `import java.util.*;import java.util.stream.*;class N13_OptionalIntType {void f(){OptionalInt i=IntStream.of(1).findFirst();int n=i.orElse("x");}}` | OptionalInt.orElse ожидает int, не String. |
| `N14_ReduceType` | `import java.util.stream.*;class N14_ReduceType {void f(){int n=Stream.of("a").reduce(0,Integer::sum);}}` | reduce identity/accumulator несовместимы с Stream<String>. |
| `N15_OnCloseChecked` | `import java.io.*;import java.util.stream.*;class N15_OnCloseChecked {void f(){Stream.of(1).onClose(()->{throw new IOException();});}}` | Runnable close-handler не объявляет checked IOException. |
| `N16_OptionalGetAsInt` | `import java.util.*;import java.util.stream.*;class N16_OptionalGetAsInt {void f(){Optional<Integer> i=Stream.of(1).findFirst();int n=i.getAsInt();}}` | Optional<Integer> не имеет getAsInt(); это метод OptionalInt. |
| `N17_GroupingByType` | `import java.util.*;import java.util.stream.*;class N17_GroupingByType {void f(){Map<Integer,Integer> m=Stream.of(1).collect(Collectors.groupingBy(x->x));}}` | groupingBy даёт Map<Integer,List<Integer>>, а не Map<Integer,Integer>. |

**Фактически запущено автором 01:** OpenJDK/javac **21.0.12.1**, на временных восстановленных `negative/N01...N17.java` командой `javac --release 21 -d <изолированный каталог> <файл>`. **17/17 nonzero exit и 17/17 нужных смысловых error categories** (не accidental syntax/EOF error); они не входят в обычную CI, если остаются только внутри этого Markdown.

**Runtime-negative (НЕ compile-negative)** уже воспроизведены в §6: P11 (`UnsupportedOperationException` при изменении `toList`), P13/P16/P17 (`NullPointerException` от конкретных коллектора/mapper), P14 (duplicate key → `IllegalStateException`), P22 (stream reuse → `IllegalStateException`), P31 (mapper throws при terminal). Некорректный параллельный side effect **не воспроизводить как обязательный deterministic failure**: пример `ArrayList<Integer> shared = new ArrayList<>(); IntStream.range(0, 1000).parallel().forEach(shared::add);` допускает потерю значений/гонки и нарушает контракт behavioral parameters, но каждый конкретный запуск может случайно показать ожидаемый размер.

## 8. Гарантии, отрицательные границы и непроверенные предположения

1. **Laziness и оптимизация:** `count()` может исключить вызовы `peek`/mapper; `map(x -> log())` — недостоверный способ проверить весь pipeline. Внедрять диагностику через тестируемый результат или отдельные явно контролируемые тесты, не через предположение «вся lambda обязательно сработает».
2. **Encounter order ≠ порядок выполнения:** `forEachOrdered` гарантирует порядок эффекта терминального действия для ordered stream, но из этого не следует, что прочие mapper будут выполняться на том же потоке, последовательно либо в том же порядке.
3. **Parallel correctness:** non-interference/statelessness важны даже для sequential; у `parallel()` нет обещаний speedup. `collect(Collectors.toList())` может использовать отдельные локальные контейнеры и merge; самодельный общий `ArrayList` в `forEach` не становится безопасным. `groupingByConcurrent` и `CONCURRENT` — отдельные условия API, не «магическое ускорение».
4. **Reduction algebra:** в перегрузках **с identity** он должен быть нейтральным; `reduce(BinaryOperator)` **без identity** возвращает `Optional<T>` и не требует передавать нейтральный элемент. Для трёхаргументного `reduce(identity, accumulator, combiner)` нужны совместимость функций и требования ассоциативности по Stream API. Сложение с identity=0 — корректный пример; `reduce(10, Integer::sum)` не имеет нейтрального элемента для сложения, поэтому не стоит ожидать одинаковой суммы от sequential и parallel. Проверка перегрузок и пустого потока — C01–C05 из §5a.
5. **Null и duplicate:** правила `Stream.toList`, `Collectors.toList`, `toUnmodifiableList`, `toMap` и `toUnmodifiableMap` **различны**. Не использовать наблюдение P17 для выводов о null keys во всех `toMap` overloads. Не обещать `HashMap`, `ArrayList` или их mutability как типовой контракт результата Collector.
6. **Закрытие:** Stream является AutoCloseable, но обычные коллекционные streams чаще не удерживают внешних ресурсов; `Files.lines()` и подобные I/O streams требуют реального close. Terminal `count()` не подменяет `try-with-resources`; детальные контракты IO относятся к будущему IO-FILES.
7. **Версии:** `Stream.toList` — с Java 16, `takeWhile/dropWhile` с Java 9, `mapMulti` с Java 16. Новые API после 21 не приписывать Java SE 21. JDK implementation и публичный API не смешивать.
8. **Обещание полноты:** P2 (`Files.lines`, scheduler, custom Collector, JDK newer methods) — обозначенные границы, а не написанная статья и не закрытые вопросы. Реальные Col/IO/CON темы — отдельные curricular ID.

## 9. Протокол G1, проверки и следующие gates

**Выполнено 01:**
- Проверен фактический `main` после изменения G6 #97/#109; 17 Java Core статей опубликованы, STREAMS/STREAM-ADV отсутствуют как статьи. Никаких изменений `CURRICULUM` или `COVERAGE`.
- Сверены официальные страницы Java SE 21 и независимые prompt banks Q-HRUDAY, Q-CHERNYSHOV, Q-TEAMLEAD, Q-IN28 по конкретным перечисленным темам (ограничение проверки отдельных ответов оговорено).
- Локально на OpenJDK **21.0.12.1**: одна компиляция + один запуск `StreamPipelineProbes.java` с **33/33 строками** ожидаемого stdout, **17/17 отдельных отрицательных `javac --release 21`** по правильным причинам. Исходные файлы находятся во временном рабочем каталоге автора, **в GitHub PR входят только Markdown listing**, не `.java` и не test.
- Для списка из 36 вопросов проверены численность и P0/P1/P2: **22 / 10 / 4**. Явно выделены output/runtime/compile-negative, отдельные неисполненные границы.

**Не запускалось / нельзя считать доказанным:** локальный `./gradlew build` / JUnit проекта, IDE/Obsidian preview и переходы по ссылкам в интерфейсе, автоматически извлечённая компиляция Java непосредственно из GitHub Markdown, альтернативные JDK/JVM, живой JDBC/IO/Spring, `Files.lines` с закрытием файловой системы, гонки с shared ArrayList как обязательный провал, custom Collector concurrency, сравнение benchmark/JMH, scheduler/ForkJoinPool internals. **CI GitHub** отдельно проверяется только на **точном HEAD после открытия PR**, и даже успешный CI не компилирует embedded probes.

**Обязательные вопросы для независимого G1-review роли 04:**
- [ ] Проверить source code §§6–7, **точную** категорию компиляционных отказов N01–N17, особенно N03/N04/N08/N14/N15; не принимать nonzero за доказательство без diagnostic.
- [ ] Пересчитать 36 ID, 22 P0 / 10 P1 / 4 P2, установить нормативность каждого P0.
- [ ] Проверить P09 `peek/count` (наблюдение только о данной реализации), P10–P17 null/mutability/duplicate contracts и запрет ложного обещания `HashMap`/любого modifiable List.
- [ ] Проверить P07–P08/25–P26, encounter order, findAny и parallel non-interference/Collector identity/associativity; не путать deterministic output с deterministic callback scheduling.
- [ ] Проверить P22–P24 reuse/close; P31 deferred exception; актуальные и рабочие локальные Markdown-ссылки, отсутствие будущих `CORE-STREAMS.md` как опубликованного файла.
- [ ] Оценить разделение на две статьи/опциональный runnable и отсутствие автоматического G2.

**После review 04** координатор **00** принимает отдельное `G1 ACCEPTED/BLOCKED`, утверждает форму материалов и только затем даёт G2 ролям 02/03. Роли 01 и 04 **не** делают merge, не объявляют G1 принятым и не меняют `COVERAGE` до публикации.

### Передача эстафеты
Кому: 04 — Independent Reviewer
Что сделать: провести независимое G1 review данного плана `docs/plans/CORE-STREAMS-ADV.md` в отдельном PR по новому точному HEAD после коммита, проверить Java 21 API контракт и 17 negative sources, вынести PASS/CHANGES и при PASS передать 00 для самостоятельного решения.
Передать: оператор пересылает прямую ссылку на опубликованный адресный GitHub-комментарий после публикации.
