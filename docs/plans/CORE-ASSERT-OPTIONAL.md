# CORE-ASSERT + CORE-OPTIONAL — предварительное исследование G1, Java SE 21

**G1 PROPOSED / NOT ACCEPTED.** Исследователь: 01 — Research & Coverage. Основание: [Issue #159](https://github.com/Melchiorius/backend-interview-labs/issues/159), [адресное поручение 00 #7077526980](https://github.com/Melchiorius/backend-interview-labs/issues/159#issuecomment-7077526980), программа #33.
**BASE/main:** `b096790bdde1af99b91ee7bbac84721f69993d3f` — опубликованный G5 [PR #156](https://github.com/Melchiorius/backend-interview-labs/pull/156); параллельный документарный G6 [#158](https://github.com/Melchiorius/backend-interview-labs/issues/158) ведёт 06, в scope G1 не входит. **HEAD исследовательского PR** фиксируется отдельно в GitHub PR и при адресном handoff 04. **Версии:** JLS 21, API Java SE 21, стабильные языковые возможности, без preview. Фактический локальный инструмент — OpenJDK 21.0.12.1, Debian Linux 13, `javac --release 21`.

## 1. Почему два curricular ID, педагогическая архитектура

**Вопрос AS:** как проверять *внутреннее предположение программы*, если сам механизм проверки может быть выключен через политику JVM? **Вопрос OT:** как метод выразит *допустимое отсутствие результата* и что сделают цепочки преобразований/альтернатив? Это принципиально разные контракты. `assert` — оператор языка с переключаемой семантикой; `Optional` — объект API Java, присутствующий независимо от флагов JVM. Нельзя выдать отсутствие результата за ошибку или `AssertionError` за обязательную проверку аргумента.

**Предлагаем 2 самостоятельные статьи:** будущие `knowledge/01-java-core/assertions.md` (CORE-ASSERT) и `knowledge/01-java-core/optional.md` (CORE-OPTIONAL) — **не существующие файлы, поэтому не Markdown-ссылки**. Общий G2 runnable `examples/core/assert-optional/` оправдан только если он показывает раздельные JVM-процессы `java`/`java -ea` и наблюдаемую ленивость fallback; отсутствие такого runnable не служит автоматическим блокером, если статьи дают полноценные воспроизводимые короткие примеры. Не требуется ритуальный Gradle/JUnit/Mockito module. Содержание ``6–8` — исследовательские полные исходники/эталоны Markdown, **не публикация Java source в репозитории**.

**Область P0:** `assert`/JVM policy, отключаемые выражения и сообщение, ошибки контракта и обязательные runtime проверки; `Optional<T>` creation/absence, eager/lazy, null/function contracts, value-based, primitives, API return design. **Вне пары:** полный `CON-JMM` и динамическое переключение classloader, реальный JPMS/cross-module assertions, общий курс ORM/JSON/serialization, stream collectors `COL-CONTRACTS`, `CORE-REFLECTION`, performance JIT/JMH/GC, general `CORE-EXCEPTIONS`, GUI IDE/Obsidian и preview.

### 1.1 Реальные опубликованные зависимости и новые пробелы

Фактический G5 main имеет **29 отдельных статей Java Core, раскрывающих 30 из 42 curricular ID**, 12 без самостоятельной Implemented статьи. [COVERAGE](../COVERAGE.md), [CURRICULUM](../CURRICULUM.md) могут отставать до G6 #158 — это не означает отсутствия уже опубликованных Syntax/Operators. Сверено с существующим Git tree BASE/main, а не только историческими статусами.

| Существующий учебный материал | Уже опубликовано | Остаётся отдельному P0 |
|---|---|---|
| [CORE-EXCEPTIONS](../../knowledge/01-java-core/exceptions.md) | `Throwable/Error/RuntimeException`, распространение, checked/unchecked | disabled/enabled assert; `AssertionError` с message; IAE/ISE vs Optional absence |
| [CORE-RESOURCES](../../knowledge/01-java-core/resources.md) | try-with-resources, suppressed exceptions | `assert` не инструмент cleanup или проверки external input |
| [CORE-TYPES](../../knowledge/01-java-core/types.md) | `null`, ref, `boolean/Boolean` и `final` | `assert` boolean Expression1, Optional<T> container не null sentinel |
| [CORE-GENERICS](../../knowledge/01-java-core/generics.md) | T, variance/inference | return types `Optional<T>`, `map/flatMap` vs specialized primitives |
| [CORE-STREAMS](../../knowledge/01-java-core/streams.md) | `findFirst():Optional<T>`, `IntStream.findFirst():OptionalInt`, терминальный результат | полный API absence, eager/lazy, Exception/null contracts |
| [CORE-STREAM-ADV](../../knowledge/01-java-core/stream-advanced.md) | `reduce(BinaryOperator):Optional<T>`, stream/collector | `Optional.stream` внутри `flatMap`, не повторять 36 stream-методов |
| [CORE-INIT](../../knowledge/01-java-core/initialization.md) | class initialization vs loading and errors | специальное правило assert при незавершённой class initialization `P1` |
| [CORE-EQUALITY](../../knowledge/01-java-core/equality.md) | `==` vs `equals` и контракт сравнения | value-based Optional: *equal interchangeable*, не identity singleton |
| [CORE-CONTROL](../../knowledge/01-java-core/control-flow.md) | if/return/throw и side effects | conditional skipping of assert Expression1/Expression2 and JVM enablement |
| [CORE-OPERATORS](../../knowledge/01-java-core/operators.md) | eager parameter expressions, порядок вызовов, side effects | `orElse(expensive())` vs `orElseGet(()->expensive())`, `requireNonNull` message supplier |
| [CORE-FUNCTIONS](../../knowledge/01-java-core/functions.md) | Supplier/Function/Consumer, lambdas | точные Optional predicate/mapper/fallback contracts |

Официальные рамки проекта — [AGENTS](../../AGENTS.md), [ROLE_MODEL](../ROLE_MODEL.md), [WORKFLOW](../WORKFLOW.md), [JAVA_CORE_PLAN](../JAVA_CORE_PLAN.md), [EXERCISE_AND_TESTING_STANDARD](../EXERCISE_AND_TESTING_STANDARD.md). **Не** менять существующие статьи/реестры в данном исследовательском PR. Ни одно упоминание `assert`/`Optional` в старых статьях **не** считается самостоятельным Implemented двух ID.

## 2. Version-pinned нормативные A и вопросы C

**A нормативные источники именно Java SE 21** (A выводит правила, C даёт интервью-вопрос, но не утверждение, как Java работает):

| ID | Первичный JLS/Javadoc/JDK21 | Какие гарантии даёт |
|---|---|---|
| A1 | [JLS 21 `14.10 assert statement](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.10) | grammar, boolean/Boolean, compile-time void restriction для detail, вычисление Expression1/2, AssertionError; особый initialization случай |
| A2 | [Java 21 java launcher](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html), `-ea/-da/-esa/-dsa`, class/package targeting | default assertions disabled; флаги, порядок и scope; система классов отличается от user classes |
| A3 | [AssertionError API 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/AssertionError.html), [Error](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Error.html) | AssertionError extends Error, detailMessage constructors, отличается от RuntimeException |
| A4 | [Objects API 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html), `requireNonNull(T)`/`(T,String)`/`(T,Supplier<String>)`; [JLS `15.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.7) | NPE для null, lazy supplier **при null** vs eager переданный String expression, аргументы методов оцениваются до вызова |
| A5 | [IllegalArgumentException 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/IllegalArgumentException.html), [IllegalStateException 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/IllegalStateException.html), [NullPointerException 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/NullPointerException.html) | нарушение параметра vs недопустимое состояние/момент; unchecked exceptions, **семантический выбор API**, а не автоматическая категоризация каждого failure |
| A6 | [Optional<T> 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html), class header/`empty/of/ofNullable/isPresent/isEmpty/get` | no nullable payload, value-based identity, API Note «primarily return type»; NoSuchElementException для empty get |
| A7 | [Optional<T> 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html), `orElse/orElseGet/or/orElseThrow` | eager method-argument vs lazy suppliers; null supplier/invocation; custom exception |
| A8 | [Optional<T> 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html), `map/flatMap/filter/ifPresent/ifPresentOrElse/stream` | null mapper result empty vs flatMap null NPE; 0/1 streams; optional-bearing mapping |
| A9 | [OptionalInt 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/OptionalInt.html), [OptionalLong 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/OptionalLong.html), [OptionalDouble 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/OptionalDouble.html) | specialized value-based primitive containers, getAsX, primitive stream and no `map/flatMap` like generic Optional |
| A10 | [JLS 21 `12.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.2), [`14.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.10) | **P1 boundary**: assert executed before completion of class initialization is enabled even if normally disabled; не делаем обобщений о dynamic classloaders |

**Проверенные независимые C-источники**, указанное слово в вопросе проверено по реальному содержимому, не по памяти:
- **C1 / `Q-TEAMLEAD`** [teamlead/java-interview-questions README](https://github.com/teamlead/java-interview-questions/blob/main/README.md): строки с вопросами *“Why is the Assert Operator used”* (две записи) и *“What I heard about Optional class”*. Текст источника не является подтверждением частоты вопросов и не техническая норма.
- **C2 / `Q-CHERNYSHOV-RU`** [Shell26/Java-Developer core.md](https://github.com/Shell26/Java-Developer/blob/master/core.md): **«Для чего используется оператор assert?»**. Формулировку о роли assert перепроверять по JLS, не переносить устаревшее «assert всегда проверяется».
- **C3 / `Q-HRUDAY`** [Core Java Interview Question Bank README](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md): реальные вопросы «What is IllegalArgumentException?», «What is IllegalStateException?» (`Exception Handling`), «Explain flatMap() with Optional» (`Functional programming`). В банке **не найдено** адресного блока со всеми Optional methods или флагами `-ea`, поэтому на них не ссылаемся как на будто бы заданные вопросы.
- **C4 / дополнительный тематический C вне текущего реестра** [Baeldung Java 8 Interview Questions](https://www.baeldung.com/JAVA-8-INTERVIEW-QUESTIONS): реальное обсуждение `Optional` как absent value. Дополнительно независимо подтверждённый банк [anton-liauchuk/java-interview README](https://github.com/anton-liauchuk/java-interview/blob/master/README.md) формулирует *“orElse() vs orElseGet() in Optional?”*. Эти C — **не новый нормативный реестр и не обещание статистического покрытия**.

## 3. Матрица CORE-ASSERT — 11 P0

`ASxx` — маркировка stdout `AssertProbe`; `A-Nxx` — законченные компиляционно-отрицательные `.java`; `default` и `-ea` имеют *разные эталоны*.

| ID / P | Что обязан объяснить кандидат, не лозунг | A / C | Positive/counterexample и граница |
|---|---|---|---|
| AS-01 P0 | `assert cond` — language statement, а не вызов JUnit; при disabled не исполняется ни условие, ни тело сообщения | A1/A2/C1,C2 | `AS01,AS02,AS03`, два режима JVM |
| AS-02 P0 | Два синтаксиса `assert cond;` и `assert cond:detail;`; detail only if enabled and cond false | A1,A3/C1 | `AS03,AS04,AS12`; `A-N02` void detail compile error |
| AS-03 P0 | Порядок/область `-ea/-da`; default disabled, отдельный JVM launch на каждый режим | A2/C1 | `AS01,AS15`, `-ea -da:AssertProbe`/`-da -ea:AssertProbe` отдельно |
| AS-04 P0 | `-esa/-dsa` относятся к system classes, не тождественны `-ea` user code | A2/C1 | `java -esa AssertProbe` имеет default stdout; `java -ea -dsa AssertProbe` = enabled stdout; реальные проверки |
| AS-05 P0 | `AssertionError` extends `Error`, не проверяемое исключение метода и не IAE | A1,A3,A5/C1,C3 | `AS03,AS12` с runtime catch; не обобщать catch(Error) как практику приложения |
| AS-06 P0 | Side effects в assert-condition опасны: между выключенным/включённым JVM различается состояние | A1/C1,C2 | `AS05=0` или `AS05=1`; **антипример**, не рекомендуемая реализация |
| AS-07 P0 | Валидация аргумента обязана работать независимо от `-ea` | A1,A4,A5/C1,C3 | `AS09=NPE`, `AS10=IAE` в обоих запусках; не безопасность через assert |
| AS-08 P0 | `Objects.requireNonNull` возвращает ссылку или бросает NPE, String expression eagerly vs Supplier message lazy | A4/C1,C3 | `AS06,AS07,AS08,AS09`; supplier создаётся как аргумент, вызывается только когда null |
| AS-09 P0 | IAE vs ISE — смысл *недопустимого аргумента* против *недопустимого состояния операции* | A5/C3 | `AS10,AS11,AS13,AS14`; это API design, не правило компилятора |
| AS-10 P0 | `assert` требует boolean/Boolean expression; detail не может быть void | A1/C1,C2 | изолированные `A-N01/A-N02` (точные `compiler.err.*`) |
| AS-11 P0 | `AssertionError` (internal invariant) и Optional.empty (легальное отсутствие) не взаимозаменяемы; на public API-boundary выбирают контракт | A1,A5,A6/C1,C2,C4 | `AS03` vs `OT02/OT06` vs `AS09–11`; явное решение таблицы `9` |

## 4. Матрица CORE-OPTIONAL — 18 P0

| ID / P | Проверяемый механизм API/граница | A / C | Trace, negative или runtime |
|---|---|---|---|
| OT-01 P0 | `Optional<T>` представляет наличие/отсутствие **non-null** значения, не nullable wrapper value | A6/C1,C4 | `OT01–OT03` и `OT32` |
| OT-02 P0 | `of(null)` бросает NPE; `ofNullable(null)` и `empty()` пусты | A6/C1,C4 | `OT03,OT04` runtime vs compiler |
| OT-03 P0 | `isPresent/isEmpty` противоположны по признаку наличия; `Optional` может быть empty без исключения | A6/C1,C4 | `OT01,OT02` |
| OT-04 P0 | `get()` и no-arg `orElseThrow()` на empty выбрасывают NSEE | A6,A7/C1,C4 | `OT05–OT07,OT36`, это **runtime**, не javac |
| OT-05 P0 | `orElse(fallback())` вычисляет аргумент eager при present, даже когда fallback не выбран | A7 + JLS `15.7/C1,C4 | `OT09=Java:1`; разбор counters |
| OT-06 P0 | `orElseGet(supplier)` вызывает supplier только при absent, null supplier tolerated при present | A7/C1,C4 | `OT10,OT11,OT22,OT23` |
| OT-07 P0 | `or(Supplier<Optional>)` возвращает **Optional**, не T; absent вызовет supplier, null result → NPE | A7/C1,C4 | `OT18–OT19,OT21` и `O-N07` |
| OT-08 P0 | `map` обычное T→U, возвращённый null превращается в empty | A8/C1,C4 | `OT12,OT13,OT17,OT33`; `O-N05` неправильное ожидаемое T |
| OT-09 P0 | `flatMap` функция T→Optional<U>, избегает `Optional<Optional<U>>`; null return throws NPE | A8/C3,C4 | `OT14,OT20` и `O-N06` |
| OT-10 P0 | `filter` оставляет present value при true, иначе empty, empty не вызывает predicate | A8/C1,C4 | `OT15,OT16`, empty trace `OT17` |
| OT-11 P0 | `ifPresent` и `ifPresentOrElse` как побочные действия, не вычисление результата T | A8/C1,C4 | `OT27=Java-empty` |
| OT-12 P0 | `orElseThrow(exceptionSupplier)` на absent бросает выбранное исключение, на present возвращает значение | A7/C1,C4 | `OT08=ISE:missing` и `OT36=Java` |
| OT-13 P0 | `stream()` превращает один Optional в Stream длины 0 или 1, `flatMap(Optional::stream)` пропускает пустые | A8/C3,C4 | `OT25,OT26`, не полный Stream/Collectors курс |
| OT-14 P0 | `OptionalInt/Long/Double` специализированы, have `getAsX/stream`, не имеют generic `map` API | A9/C1,C3 | `OT28–OT31,OT34,OT35`, `O-N08` |
| OT-15 P0 | `Optional` **value-based**: `equals` допустим, identity и singleton `empty()` не гарантированы, не синхронизироваться на optional | A6,A9/C1,C4 | `OT32=true`, **не** `empty()==empty()`; не JMM contract |
| OT-16 P0 | Optional API Note «primarily method return type», не blanket JLS ban на field/parameter; wrapper **сам не должен быть null** | A6/C1,C4 | `lookup(String):Optional<String>` и `OT03`; ORM/serialization P2 |
| OT-17 P0 | `null` возвращённый mapper → empty vs `null` возвращённый `flatMap/or` → NPE; exceptions различны | A7,A8/C1,C3 | `OT13=true, OT20/OT21=NPE` |
| OT-18 P0 | Absence не равна ошибке входного аргумента и не заменяет бизнес-ошибки; аккуратно выбрать API contract | A4,A5,A6/C1,C3,C4 | `OT19` «другое найденное» против `AS09..11` «недопустимая операция» |

**Итого P0 = 11 AS + 18 OT = 29.** Это матрица вопросов до написания статей; наличие строк здесь не означает, что темы уже Implemented. Ревьюер 04 должен проверять строка → JLS/Javadoc → full-source/evidence → будущий текст.

## 5. P1/P2 — важные границы и честные not run

| ID | P | Содержательная задача / предел |
|---|---|---|
| AS-12 | P1 | **Особый JLS-случай**: assert, исполненный *до завершения инициализации собственного класса*, enabled даже когда default disabled (A1,A10); **не выполнен локальный cyclic-init эксперимент**, только норма |
| AS-13 | P1 | `Boolean` в Expression1 unboxing null → runtime NPE при enabled; disabled — отсутствие вычисления; JLS A1/`5.1.8`, требует отдельную воспроизводимую probe при G2 |
| AS-14 | P1 | Последовательность флагов `-ea/-da` + named/unnamed package `...`; отдельное поведение class loader policy, частично класс-scope проверен |
| AS-15 | P1 | AssertionError(detail) toString/getMessage и исключения в message expression, A1/A3, без blanket «всегда AssertionError если false» при бросающем detail |
| AS-16 | P1 | Проверяемый security-precondition anti-pattern: `assert userHasPermission()` может быть disabled; демонстрировать без эксплуатации/продуктовых данных |
| AS-17 | P1 | Валидация `requireNonNullElse/ElseGet` и null suppliers/defaults, A4, не путать с Optional |
| OT-19 | P1 | Null mapper/predicate/function arguments: `map/flatMap/filter` проверяют mapper, даже если Optional пуст; Javadoc A8, дополнить тестом в G2 |
| OT-20 | P1 | `Optional.or(null)` vs `orElseGet(null)`: supplier-null contract отличается (A7), нужны отдельные пробы; не обобщать все suppliers |
| OT-21 | P1 | `Optional.get` API Note предпочитает `orElseThrow()`; небезусловная стилевая рекомендация, не language rule |
| OT-22 | P1 | `Optional.empty()` identity не гарантирована; `hashCode`/`equals` по содержимому (A6); не выводить performance из наблюдений |
| OT-23 | P1 | Primitive Optional APIs имеют `orElseGet`/`orElseThrow`/`stream`, но **не имеют `map/flatMap/filter` как у Optional<T>`; подтверждать по A9 |
| OT-24 | P1 | `get` after `isPresent` vs chaining, выбор удобного метода зависит от контекста/читабельности, не выпускать «никогда не get» как норму |
| AS-18 | P2 | Runtime classloader assertion policy/early cyclic initialization, full JPMS/agent instrumentation — not run |
| AS-19 | P2 | Test frameworks/JUnit assert* vs language assert, Gradle/JUnit integration — not run |
| AS-20 | P2 | JVM optimisation, JIT/GC and performance cost of asserts — not run, не выводить ускорение |
| OT-25 | P2 | ORM/JPA/JSON/serialization semantics of Optional as field/parameter; не заявлять запрет JLS или корректную интеграцию без версии framework |
| OT-26 | P2 | Structured concurrency/JMM/value-based identity runtime subtleties: не синхронизироваться на Optional, **не выполнять** конкурентный тест |
| OT-27 | P2 | Primitive variants NaN/signed zero equality edge, full comparison APIs; отдельная теория numerical values |
| OT-28 | P2 | Реализационная стратегия Optional allocation/scalar replacement и microbenchmarks: not run |
| OT-29 | P2 | Optional in collectors/findFirst with null elements, parallel streams and encounter-order, полноценный [CORE-STREAMS](../../knowledge/01-java-core/streams.md) и [CORE-STREAM-ADV](../../knowledge/01-java-core/stream-advanced.md), не включать целиком |

**Итого: 29 P0 / 12 P1 / 8 P2.** Пределы `P2` — указатели на отдельные вопросы, а не claims о готовности других curricular ID.

## 6. Два полностью записанных, реально проверенных положительных Java21 source

Универсальная команда `javac --release 21 -Xlint:all AssertProbe.java OptionalProbe.java` в чистом временном каталоге возвратила **exit 0, warnings 0**. Выполнены отдельные процессы `java AssertProbe`, `java -ea AssertProbe`, `java OptionalProbe`; дополнительно `-da`, `-esa`, `-dsa`, class-specific `-ea:AssertProbe`, `-ea -da:AssertProbe`, `-da -ea:AssertProbe`. Все завершены `exit 0`, для каждого эталона выполнен `diff -u` (`exit 0`) на байтах UTF-8 + LF. **15/15/36 stdout lines** трёх содержательно различных режимов — **66 строк**, а не 66 JUnit-тестов. Разные режимы `AssertProbe` — отдельные JVM запуски, а не «переключатель в одном процессе».

### 6.1 AssertProbe.java — полный исходник

~~~java
import java.util.Objects;

public class AssertProbe {
    private static int conditionCalls;
    private static int detailCalls;
    private static int textCalls;
    private static boolean condition(boolean value) {
        conditionCalls++;
        return value;
    }
    private static String detail() {
        detailCalls++;
        return "expected-67";
    }
    private static String text() {
        textCalls++;
        return "required-message";
    }
    private static String legal(String value) {
        return Objects.requireNonNull(value, "value");
    }
    private static int age(int value) {
        if (value < 0) throw new IllegalArgumentException("negative-age");
        return value;
    }
    private static String state(boolean ready) {
        if (!ready) throw new IllegalStateException("not-ready");
        return "ready";
    }
    public static void main(String[] args) {
        boolean assertionEnabled = false;
        assert assertionEnabled = true;
        System.out.println("AS01=" + assertionEnabled);
        assert condition(true) : detail();
        System.out.println("AS02=" + conditionCalls + ":" + detailCalls);
        try {
            assert condition(false) : detail();
            System.out.println("AS03=skipped");
        } catch (AssertionError error) {
            System.out.println("AS03=AssertionError:" + error.getMessage());
        }
        System.out.println("AS04=" + conditionCalls + ":" + detailCalls);
        int sideEffects = 0;
        assert ++sideEffects == 1;
        System.out.println("AS05=" + sideEffects);
        String good = "ok";
        Objects.requireNonNull(good, text());
        System.out.println("AS06=" + textCalls);
        Objects.requireNonNull(good, AssertProbe::text);
        System.out.println("AS07=" + textCalls);
        try {
            Objects.requireNonNull(null, (java.util.function.Supplier<String>) AssertProbe::text);
        } catch (NullPointerException error) {
            System.out.println("AS08=NPE:" + error.getMessage() + ":" + textCalls);
        }
        try { legal(null); } catch (NullPointerException error) { System.out.println("AS09=NPE:" + error.getMessage()); }
        try { age(-1); } catch (IllegalArgumentException error) { System.out.println("AS10=IAE:" + error.getMessage()); }
        try { state(false); } catch (IllegalStateException error) { System.out.println("AS11=ISE:" + error.getMessage()); }
        try {
            assert false;
            System.out.println("AS12=skipped");
        } catch (AssertionError error) {
            System.out.println("AS12=AssertionError:" + (error.getMessage() == null));
        }
        System.out.println("AS13=" + age(67));
        System.out.println("AS14=" + state(true));
        System.out.println("AS15=" + AssertProbe.class.desiredAssertionStatus());
    }
}
~~~

**Эталон A0 (15 строк):** `java AssertProbe`; также `java -da AssertProbe`, `java -esa AssertProbe`, `java -dsa AssertProbe` и `java -ea -da:AssertProbe AssertProbe` в данной локальной проверке:

~~~text
AS01=false
AS02=0:0
AS03=skipped
AS04=0:0
AS05=0
AS06=1
AS07=1
AS08=NPE:required-message:2
AS09=NPE:value
AS10=IAE:negative-age
AS11=ISE:not-ready
AS12=skipped
AS13=67
AS14=ready
AS15=false
~~~

**Эталон A1 (15 строк):** `java -ea AssertProbe`; также `java -ea:AssertProbe AssertProbe`, `java -da -ea:AssertProbe AssertProbe` и `java -ea -dsa AssertProbe` в данной локальной проверке:

~~~text
AS01=true
AS02=1:0
AS03=AssertionError:expected-67
AS04=2:1
AS05=1
AS06=1
AS07=1
AS08=NPE:required-message:2
AS09=NPE:value
AS10=IAE:negative-age
AS11=ISE:not-ready
AS12=AssertionError:true
AS13=67
AS14=ready
AS15=true
~~~

**Привязка смысла к строкам:** `AS02/AS04` демонстрируют **нет вызова обоих выражений** при disabled и вызов `detail` только при false Expression1; `AS05` — сознательно вредный side-effect внутри assertion; `AS06/AS07/AS08` — eager expression `text()` и ленивый вызов через `Supplier<String>`. `AS09/AS10/AS11` работают одинаково при обоих JVM режимах; обязательную валидацию нельзя подменить `assert`. `AS15` — диагностический вывод `Class.desiredAssertionStatus()` *этого* JVM запуска, не гарантия неизменяемости policy до initialization для всех классов.

### 6.2 OptionalProbe.java — полный исходник

~~~java
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
~~~

**Эталон OT (36 строк):** `java OptionalProbe`, не зависит от `-ea` в данном коде.

~~~text
OT01=true:false
OT02=false:true
OT03=true
OT04=NPE
OT05=Java
OT06=NSEE
OT07=NSEE
OT08=ISE:missing
OT09=Java:1
OT10=Java:1
OT11=fallback:2
OT12=4
OT13=true
OT14=Java
OT15=true
OT16=Java
OT17=true
OT18=Java
OT19=other
OT20=NPE:flatMap-null
OT21=NPE:or-null
OT22=NPE:missing-supplier
OT23=Java
OT24=null
OT25=[A, B]
OT26=1:0
OT27=Java-empty
OT28=67
OT29=67
OT30=2.5
OT31=67
OT32=true
OT33=5
OT34=NSEE:int
OT35=true:true
OT36=Java
~~~

**Важная трасса:** `OT09` вызывает `fallback`, хотя значение присутствует, поэтому счетчик 1; `OT10` не вызывает supplier (счётчик остаётся 1), `OT11` вызывает при empty (становится 2). `OT13` отличает null-return из `map` от `OT20` null-return из `flatMap`. `OT23` доказывает `orElseGet(null)` допустим *при present*, `OT22` — невозможность вызвать null supplier *при absent*. `OT24` допускает `null` как запасной результат `orElse` — **не** внутри Optional. Значение счетчика и сообщения специально детерминированные, без clock/threads/network.

### 6.3 Команды, повторная проверка, фактические артефакты

~~~sh
java -version                            # OpenJDK 21.0.12.1, Linux
javac -version                           # javac 21.0.12.1
javac --release 21 -Xlint:all AssertProbe.java OptionalProbe.java
java AssertProbe > AssertProbe.default.stdout
java -ea AssertProbe > AssertProbe.ea.stdout
java -ea -da:AssertProbe AssertProbe > AssertProbe.ea-da.stdout
java -da -ea:AssertProbe AssertProbe > AssertProbe.da-class-ea.stdout
java -ea:AssertProbe AssertProbe > AssertProbe.class-ea.stdout
java -esa AssertProbe > AssertProbe.esa.stdout
java -ea -dsa AssertProbe > AssertProbe.ea-dsa.stdout
java OptionalProbe > OptionalProbe.stdout
diff -u AssertProbe.default.expected.stdout AssertProbe.default.stdout
diff -u AssertProbe.ea.expected.stdout AssertProbe.ea.stdout
diff -u OptionalProbe.expected.stdout OptionalProbe.stdout
# Для scoped JVM флагов также сверить с соответствующим A0/A1 ожидаемым файлом.
~~~

Ожидаемые файлы `*.expected.stdout` выше — **содержимое code blocks**, не самостоятельные файлы данного PR. В локальном временном каталоге они были восстановлены, реальные `diff -u` и сравнения scoped outputs вернули `exit 0`. `sha256sum` (UTF-8+LF): `AssertProbe.default.stdout = a44a4e134830d9d25268e6627be0e537d3714fcd756d5bc041158be07e67301e`; `AssertProbe.ea.stdout = 164cadf935ce8e3ea58b84ee1e6768e0fdc830f0b06fdc77eabd33a15d2ea747`; `OptionalProbe.stdout = 0958bd7fbe9cd36a07d59fa800c3ad0b64c5c7d63a421b05170c76bf2a908476`. Не надо утверждать, что GitHub CI запустила Java из Markdown.

## 7. Изолированные compile-negative — только действительно запрещённое Java21

**11/11 самостоятельных полных `.java` ниже** реально компилировались *по одному* на OpenJDK `javac 21.0.12.1` как `javac --release 21 -XDrawDiagnostics -d negative negative/Filename.java`. Для каждого **exit 1**, тематический `compiler.err.*` выведен ниже; отсутствие сторонних файлов/пакетов **не** является причиной отказа. Это не 11 JUnit тестов и не CI проекта. Runtime NPE/NSEE/IAE/ISE/AssertionError из ``6.1–6.2` **не** входят в эти 11.

| ID и самостоятельный filename | **Полный source** (компилировать отдельно) | **Фактически полученная содержательная категория** |
|---|---|---|
| `A-N01` — `AS_N01_NotBoolean.java` | `class AS_N01_NotBoolean { void check() { assert 67; } }` | `compiler.err.prob.found.req / int→boolean` |
| `A-N02` — `AS_N02_VoidDetail.java` | `class AS_N02_VoidDetail { void check() { assert false : System.out.println("message"); } }` | `compiler.err.void.not.allowed.here` |
| `O-N01` — `OT_N01_PrimitiveTypeArgument.java` | `import java.util.Optional; class OT_N01_PrimitiveTypeArgument { Optional<int> v; }` | `compiler.err.type.found.req / type.req.ref` |
| `O-N02` — `OT_N02_OptionalOfInt.java` | `import java.util.Optional; class OT_N02_OptionalOfInt { int f() { return Optional.of(67).getAsInt(); } }` | `compiler.err.cant.resolve.location.args / no getAsInt on Optional<Integer>` |
| `O-N03` — `OT_N03_WrongType.java` | `import java.util.Optional; class OT_N03_WrongType { Optional<String> v = Optional.ofNullable(67); }` | `compiler.err.prob.found.req / incompatible generic bounds` |
| `O-N04` — `OT_N04_ElseGetNotSupplier.java` | `import java.util.Optional; class OT_N04_ElseGetNotSupplier { String f() { return Optional.of("A").orElseGet("B"); } }` | `compiler.err.cant.apply.symbol / requires Supplier` |
| `O-N05` — `OT_N05_MapChangesType.java` | `import java.util.Optional; class OT_N05_MapChangesType { Optional<String> v = Optional.of("A").map(String::length); }` | `compiler.err.prob.found.req / Optional<Integer> not Optional<String>` |
| `O-N06` — `OT_N06_FlatMapNotOptional.java` | `import java.util.Optional; class OT_N06_FlatMapNotOptional { Optional<Integer> v = Optional.of("A").flatMap(s -> s.length()); }` | `compiler.err.prob.found.req / lambda must return Optional` |
| `O-N07` — `OT_N07_OrNotOptional.java` | `import java.util.Optional; class OT_N07_OrNotOptional { Optional<String> v = Optional.<String>empty().or(() -> "B"); }` | `compiler.err.cant.apply.symbol / Supplier<Optional>` |
| `O-N08` — `OT_N08_PrimitiveVariantMap.java` | `import java.util.OptionalInt; class OT_N08_PrimitiveVariantMap { int f() { return OptionalInt.of(67).map(x -> x+1).getAsInt(); } }` | `compiler.err.cant.resolve.location.args / OptionalInt has no map` |
| `O-N09` — `OT_N09_StreamWrongType.java` | `import java.util.Optional; class OT_N09_StreamWrongType { Optional<String> v = Optional.of("A").stream(); }` | `compiler.err.prob.found.req / Stream not Optional` |

**Почему отрицательных только 11:** у `assert` и `Optional` главные провалы происходят **во время исполнения**, поэтому придумывать дополнительные компиляторные ошибки было бы некорректно. `Optional.of(null)`, `Optional.empty().get()`, `present.flatMap(s -> null)` и `assert false` — **валидный исходный Java**, вызывающий runtime исключение при определённых условиях; `assert` без `-ea` вообще не проверит условие. Осмысленные compile-time failures ограничены типами Expression1/Expression2 и API method signatures/generic bounds. В отличие от старых G1, нет безосновательного норматива «минимум 20 negatives».

## 8. Что должно попасть в будущие G2/G3/G4; статические проверки

**G2 02:** написать два связных самостоятельных объяснения с микротрейсом для каждого P0, без повторения `exceptions/resources/generics/streams`. Для assert обязательно привести **два запуска** и разницу между side-effect и обязательной проверкой; выделить особый JLS `14.10 early-initialization как P1 без заявления локального исполнения. Для Optional сделать таблицу **состояние present/empty × метод × вычисление аргумента/вызов Supplier × результат/exception**; объяснить, почему `map` and `flatMap` имеют разные contracts и отличаются типами. Не объявлять отсутствие человека/записи IAE автоматически: API contract нужно описать, не угадывать.

**G2 03:** один учебно оправданный runnable возможен (два Java source, разные CLI modes, 15/15/36 эталоны, корректный `verify.sh`), но не обязательный Gradle-модуль. Negative cases — если автоматизированы, **каждый отдельно**, exit nonzero + тематический diagnostic key; не считать runtime ошибку компиляторным отказом. После приёмки G1 00 назначит конкретные G2 Issues — роль 01 этого не делает.

**G3 05:** естественный подробный русский текст с примерами «поставили external validation на assert → выключили проверки → ошибочный объект живёт дальше» и «orElse вызывают fallback заранее». Никаких безосновательных рекомендаций «Optional всегда поле/не поле», «Optional Serializable»; нормативно это value-based return-oriented API, framework integration отдельно. Проверить ссылки на существующие материалы и не оставлять «шаблонный генеративный» стиль.

**G1/G4 04:** независимое review по **exact HEAD**: JLS `14.10 + `12.4.2, `java -ea/-da/-esa/-dsa`, Javadoc Optional/Objects, 29 P0, реальные C (вопросы, не статистика), 15/15/36 stdout и 11 isolate compile-negative, чёткое отделение runtime exceptions, ссылки и ограничения. Исправление существенной ошибки → новый HEAD и повторный review. Полный acceptance G1 — только 00, G4 — на интегрированном G2/G3 HEAD.

**Not run:** проектный Gradle/JUnit/Actions CI не компилирует Java-код из нового исследовательского Markdown; иные JDK vendors/версии/OS, GUI Obsidian/IntelliJ/Markdown anchor navigation, реальная работа JVM classloader assertion policy до/после initialization, system-library code с enabled `-esa`, JPMS/multi-module launch, serialization/ORM/JPA/JSON frameworks, production DB, JMM/concurrency/performance/JIT/GC/JMH, альтернативные компиляторы, property-based tests и preview — **не проверялись**. Java21 стандарт/version pin не позволяет автоматически обобщать поведение всех окружений.

**Static file-link audit:** полный GitHub API recursive tree исходного BASE main, **без усечения**; из `docs/plans/` разрешены все реальные относительные Markdown paths плана и отмечено количество проверенных. **Это не HTTP URL checking и не GUI click audit.** Будущих статей `assertions.md`/`optional.md` пока нет, и Markdown-ссылок на них не создано.

## 9. Статус и передача эстафеты

| Объект | Фактический статус |
|---|---|
| 29 P0, 12 P1, 8 P2, versioned A/C registry | `G1 PROPOSED, NOT ACCEPTED` |
| AssertProbe + OptionalProbe, Java21 compile and 15/15/36 stdout, scoped launch variants | `verified locally`, **not CI of Markdown** |
| 11/11 isolated full-source compile-negative topical diagnostics | `verified locally`, **not CI of Markdown** |
| CORE-ASSERT / CORE-OPTIONAL published articles | `NOT Implemented / planned` |
| Java sources, examples, workflows, Gradle and status registries in G1 branch | **not changed** |
| Проектная Actions CI exact HEAD | Проверить и записать реальное состояние отдельно после открытия PR |
| Classloader/preview/ORM/JMM/other JDK/OS/JIT/GC/JMH | `not run/out of scope` |

### Передача эстафеты
Кому: **04 — Independent Reviewer**.
Что сделать: выполнить **независимое G1 review исследовательского PR на точном полном HEAD SHA**, проверить нормативные факты `assert`/`Optional`, 29 P0, 11 negative + разные runtime, 2 Java sources и 3 expected outputs, launcher flags/hidden early-init boundary, real C-source formulations, относительные ссылки и `not run`. Выдать `PASS` или `CHANGES` с точными блокерами/lines/Javadoc/контрпримерами. При изменении HEAD нужен новый review. Только после PASS передать **00** на решение `G1 ACCEPTED/BLOCKED` и merge; не начинать G2.
Передать: оператор пересылает **прямую ссылку на отдельный адресный опубликованный GitHub PR-комментарий** (точный PR и HEAD, ожидаемое действие), а не общий PR URL.
