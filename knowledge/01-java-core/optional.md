# CORE-OPTIONAL — отсутствие результата без `null`-контракта в Java 21

**Область:** Java SE 21 API, без preview. [Принятый G1-план](../../docs/plans/CORE-ASSERT-OPTIONAL.md) и [G2 Issue #162](https://github.com/Melchiorius/backend-interview-labs/issues/162). Покрытие: **OT-01–OT-18 (18 P0), OT-19–OT-24 (6 P1), OT-25–OT-29 (5 P2)**. Это не полный курс [generics](generics.md), [functional interfaces](functions.md), [streams](streams.md), [advanced streams](stream-advanced.md) или [exceptions](exceptions.md); связанные гарантии разобраны здесь только применительно к `Optional`. [CORE-ASSERT](assertions.md) объясняет, почему допустимое отсутствие не эквивалентно нарушению инварианта.

Предположим, сервис ищет пользователя по ключу. Корректный запрос может ничего не найти. Возвращать `Optional<User>` позволяет **на уровне типа результата** показать два законных исхода: значение есть или его нет. Но это не новый способ передавать `null` повсюду. У `Optional<T>` собственные контракты создания, преобразования, альтернатив и исключений; знание одного `isPresent()` не позволяет предсказать, сработает ли дорогой fallback или будет ли пропущена функция.

## 1. Создание и два состояния (OT-01, OT-02, OT-03)

~~~java
import java.util.Optional;

class RepositoryExample {
    static Optional<String> findNickname(String key) {
        return "known".equals(key) ? Optional.of("Java") : Optional.empty();
    }
    static void run() {
        Optional<String> present = findNickname("known");
        Optional<String> absent = findNickname("unknown");
        System.out.println(present.isPresent() + ":" + present.isEmpty()); // true:false
        System.out.println(absent.isPresent() + ":" + absent.isEmpty());   // false:true
    }
}
~~~

В `Optional<String>` **может храниться ненулевая строка либо отсутствие строки**. Сам контейнер в корректном API-контракте **не должен быть `null`**: метод, обещающий `Optional<T>`, но возвращающий `null`, нарушает ожидания вызывающей стороны.

Три фабрики различаются:

| Вызов | Результат |
|---|---|
| `Optional.empty()` | Пустой контейнер |
| `Optional.of("Java")` | Присутствующее ненулевое значение |
| `Optional.of(null)` | **Runtime** `NullPointerException` |
| `Optional.ofNullable(null)` | Пустой контейнер |
| `Optional.ofNullable("Java")` | Присутствующее значение |

В принятом G1 `OT01=true:false`, `OT02=false:true`, `OT03=true` и `OT04=NPE`. **`of(null)` компилируется:** ошибка возникает при исполнении вызова; этот случай не входит в 11 compiler-negative.

## 2. Получение значения и граница исключения (OT-04, OT-12; OT-21 P1)

`get()` возвращает T при наличии, но выбрасывает `NoSuchElementException` при пустоте. Современный Java 21 API Note для `get` предпочитает `orElseThrow()` без аргументов, когда требуется строгое получение; это рекомендация API, **не запрет компилятора**.

~~~java
Optional<String> name = Optional.of("Java");
Optional<String> missing = Optional.empty();

System.out.println(name.orElseThrow()); // Java
// missing.get();             // runtime NoSuchElementException
// missing.orElseThrow();     // тот же тип runtime исключения
// missing.orElseThrow(() -> new IllegalStateException("missing"));
//                               runtime IllegalStateException при empty
~~~

Перегрузка `orElseThrow(Supplier<? extends X>)` позволяет **выбрать исключение по контракту приложения**: supplier вызывается только если значение отсутствует. В G1 `OT05=Java`, `OT06=NSEE`, `OT07=NSEE`, `OT08=ISE:missing`, `OT36=Java`. Комментарии с вызовами выше нужно *раскомментировать по одному*: это корректный компилируемый код, но без `catch` программа завершится с runtime исключением.

**Законное отсутствие** не обязано становиться исключением. В API поиска можно вернуть `empty()`; в API «дай существующего пользователя либо ошибка» — `orElseThrow(...)`. Это выбор контракта, не автоматическое правило, что всякое `empty` — авария.

## 3. Альтернативное значение: eager `orElse` и lazy `orElseGet` (OT-05, OT-06; OT-20 P1)

У метода с параметром `orElse(T other)` **сначала вычисляется аргумент** по обычным правилам Java, независимо от того, будет ли он использован. У метода `orElseGet(Supplier<? extends T>)` сначала передаётся объект supplier, но его `get()` вызывается **только когда Optional пуст**.

~~~java
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

class FallbackDemo {
    static String fallback(AtomicInteger calls) {
        calls.incrementAndGet();
        return "fallback";
    }
    static void run() {
        AtomicInteger count = new AtomicInteger();
        Optional<String> present = Optional.of("Java");
        Optional<String> absent = Optional.empty();
        System.out.println(present.orElse(fallback(count)) + ":" + count.get());            // Java:1
        System.out.println(present.orElseGet(() -> fallback(count)) + ":" + count.get());  // Java:1
        System.out.println(absent.orElseGet(() -> fallback(count)) + ":" + count.get());   // fallback:2
    }
}
~~~

Пошагово: первый вызов передаёт **результат** выполнения `fallback`, поэтому `count` уже равен 1; сам `Optional` содержит `"Java"` и возвращает его, игнорируя готовый запасной результат. Второй вызов получает *функцию*, значение присутствует — функция не запускается. Третий вызов видит пустоту и вызывает функцию — счётчик становится 2. Это принятые G1 `OT09–OT11`.

**Пограничные null-ситуации различаются:** `Optional.<String>of("Java").orElseGet(null)` допустим при present (G1 `OT23=Java`), но `Optional.<String>empty().orElseGet(null)` даёт **runtime NPE** (`OT22=NPE:missing-supplier`). `Optional.<String>empty().orElse(null)` **возвращает `null`** (`OT24=null`) — контейнер остаётся пустым; null здесь лишь внешнее запасное значение типа T, а не содержимое `Optional`. Это не универсальное разрешение на nullable-return из любого метода.

## 4. Альтернативный контейнер: `or(Supplier<Optional>)` (OT-07; OT-20 P1)

`or` отличается по **типу результата** от `orElse` и `orElseGet`: возвращает `Optional<T>`, а не T. Это удобно для последовательного поиска из двух источников без распаковки контейнера:

~~~java
Optional<String> inCache = Optional.empty();
Optional<String> inStorage = inCache.or(() -> Optional.of("storage"));
System.out.println(inStorage.orElse("none")); // storage
~~~

Если исходное значение присутствует, supplier для `or` не вызывается. Если отсутствует, supplier вызывается и обязан вернуть **ненулевой Optional**; `Optional.<String>empty().or(() -> null)` даёт **runtime NPE** (G1 `OT21`). Важная P1-граница: **сам аргумент supplier у `or(null)` проверяется на null даже при present** (см. [Optional API §or](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html#or(java.util.function.Supplier))); не переносите сюда поведение `present.orElseGet(null)`. Автор 02 отдельно проверил этот случай во временной Java21-пробе: `present.or(null)` дал NPE, а `present.orElseGet(null)` не бросил исключение. Это не 36 G1 stdout и не проектный CI.

У G1 `OT18=Java` (present оставлен неизменным), `OT19=other` (empty получил новый Optional). Неверная компиляционная запись `empty.or(() -> "B")` — **O-N07**: supplier должен возвращать контейнер, не строку.

## 5. `map` и `flatMap`: разные типы и разные правила `null` (OT-08, OT-09, OT-17; OT-19 P1)

`map(Function<T,U>)` преобразует присутствующее значение и оборачивает его результат через логику `ofNullable`. Если mapper вернул `null`, получаем **empty**, а не присутствующий `null`:

~~~java
Optional<String> word = Optional.of("Java");
Optional<Integer> length = word.map(String::length); // Optional<Integer> с 4
Optional<String> mappedNull = word.map(s -> null);   // Optional.empty()
~~~

`flatMap(Function<T,Optional<U>>)` ожидает, что mapper *сам* вернёт `Optional<U>`. Это не постобработка вложенного контейнера магией: если функция возвращает `null` вместо `Optional`, будет **runtime NPE**.

~~~java
static Optional<String> lookup(String input) {
    return Optional.ofNullable(input);
}

Optional<String> present = Optional.of("Java");
Optional<String> flat = present.flatMap(s -> Optional.of(s)); // Optional[Java]
// present.flatMap(s -> null); // компилируется, но при выполнении NPE
~~~


Контраст G1: `OT12=4`, `OT13=true` для `map(s -> null).isEmpty()`, `OT14=Java` для `flatMap`, а `OT20=NPE:flatMap-null` **во время исполнения**. `OT17=true` — **настоящий `absent.map(String::length).isEmpty()`**, а не `empty.filter`; это важное исправление ошибочной подписи G1, см. §6.

При compile-time ошибки другие: `Optional<String> x = Optional.of("A").map(String::length);` невозможно присвоить как `Optional<String>`, поскольку результат `Optional<Integer>` (O-N05). `flatMap(s -> s.length())` вместо Optional-результата не проходит согласование типов (O-N06). Это **compile-negative**, не NPE.

**P1 null-аргументы:** в Java 21 `map(null)`, `flatMap(null)` и `filter(null)` запрещены по контракту и дают runtime NPE **даже у пустого Optional**, потому что сами функции валидируются до ветки о наличии значения. Не путайте **не-вызов правильной функции** при empty и **недопустимость null вместо самой функции**. Это утверждение следует из [Optional API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html); в G1 отдельного исполнения null-function аргументов не было. Автор 02 дополнительно проверил `empty.map(null)`, `empty.flatMap(null)`, `empty.filter(null)` на OpenJDK21.0.12.1: все три дали runtime NPE. Временный код скомпилировался с `javac --release 21 -Xlint:all` без предупреждений, но не опубликован как часть CI.

## 6. `filter` и обязательное исправление N2: пустой контейнер не вызывает predicate (OT-10)

`filter(Predicate<T>)` возвращает тот же присутствующий элемент при `true`, пустой контейнер при `false` и пустой контейнер **без вызова корректного predicate** при исходном empty. Показать только present-ветви недостаточно: это и стало замечанием N2 независимого G1 review.

~~~java
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

class FilterDemo {
    static void run() {
        Optional<String> value = Optional.of("Java");
        System.out.println(value.filter(s -> s.length() > 4).isEmpty()); // true
        System.out.println(value.filter(s -> s.length() == 4).orElse("none")); // Java

        AtomicInteger predicateCalls = new AtomicInteger();
        Optional<String> missing = Optional.empty();
        Optional<String> filtered = missing.filter(s -> {
            predicateCalls.incrementAndGet();
            return true;
        });
        System.out.println(filtered.isEmpty() + ":" + predicateCalls.get()); // true:0
    }
}
~~~

**Это исправление G1-N2 в самом G2-тексте:** `OT15=true` и `OT16=Java` подтверждают две present-ветви; приведённый **новый, завершённый `empty.filter` trace** показывает `true:0`. **`OT17` не используется как доказательство `empty.filter`:** это строка `absent.map(...).isEmpty()`, дающая `true`. Принятый G1-план намеренно не изменён; Новый `FilterDemo` **отдельно скомпилирован и запущен автором 02** на OpenJDK21.0.12.1 с `javac --release 21 -Xlint:all` без диагностики; фактические три строки: `true`, `Java`, `true:0`. Это локальное evidence G2, **не** принятые G1 stdout и не committed runnable/CI.

Если передать `null` вместо самого predicate, применяется контракт обязательного ненулевого аргумента, см. §5: «empty не вызывает predicate» **не означает**, что `empty.filter(null)` допустим.

## 7. Побочные действия: `ifPresent` и `ifPresentOrElse` (OT-11)

Иногда нужно **сделать действие**, а не получить новое значение. `ifPresent(Consumer)` вызывает consumer только при present; `ifPresentOrElse(Consumer,Runnable)` вызывает ровно одну соответствующую ветвь. Оба возвращают `void` — ими нельзя присвоить T или Optional:

~~~java
StringBuilder actions = new StringBuilder();
Optional.of("Java").ifPresent(actions::append);
Optional.<String>empty().ifPresentOrElse(actions::append, () -> actions.append("-empty"));
System.out.println(actions); // Java-empty
~~~

Именно это даёт G1 `OT27=Java-empty`. Побочное действие внутри consumer может иметь собственную ошибку, но отсутствие значения **само по себе** не запускает consumer.

## 8. Мост к потокам: `Optional.stream()` (OT-13)

`stream()` возвращает **Stream<T> с нулём или одним элементом**. Это позволяет обрабатывать поток результатов поиска, не писать ручную проверку `isPresent`:

~~~java
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

class StreamBridge {
    static void run() {
        List<String> found = Stream.of(
                Optional.of("A"), Optional.<String>empty(), Optional.of("B"))
                .flatMap(Optional::stream)
                .toList();
        System.out.println(found); // [A, B]
    }
}
~~~

G1 `OT25=[A, B]`, `OT26=1:0` для `present.stream().count()` и `empty.stream().count()`. `stream()` возвращает **Stream**, а не Optional — попытка присвоить `Optional<String> v = Optional.of("A").stream();` проваливает компиляцию (O-N09). Устройство ленивого Stream, encounter order, collector и parallel execution рассматриваются в [CORE-STREAMS](streams.md) и [CORE-STREAM-ADV](stream-advanced.md), не здесь.

## 9. `OptionalInt`, `OptionalLong`, `OptionalDouble` (OT-14; OT-23 P1)

Для примитивов есть специальные **value-based** API, которые хранят значения `int`, `long` и `double`. Они не взаимозаменяемы с `Optional<Integer>`, хотя идея пустого/присутствующего состояния сходна:

| Класс | Создание | Получение | Поток / альтернатива |
|---|---|---|---|
| `OptionalInt` | `of(67)` / `empty()` | `getAsInt()` | `IntStream stream()`, `orElse(int)` |
| `OptionalLong` | `of(67L)` / `empty()` | `getAsLong()` | `LongStream stream()`, `orElse(long)` |
| `OptionalDouble` | `of(2.5)` / `empty()` | `getAsDouble()` | `DoubleStream stream()`, `orElse(double)` |

~~~java
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.OptionalDouble;

class PrimitiveOptional {
    static void run() {
        System.out.println(OptionalInt.of(67).getAsInt());   // 67
        System.out.println(OptionalLong.of(67L).stream().sum()); // 67
        System.out.println(OptionalDouble.empty().isEmpty()); // true
    }
}
~~~

G1 `OT28=67`, `OT29=67`, `OT30=2.5`, `OT31=67`, `OT34=NSEE:int` при пустом `getAsInt()`, `OT35=true:true` для двух пустых типов. Имеются `orElseGet`, `orElseThrow` и `stream`, но **нет** generic-операций `map/flatMap/filter` аналогично `Optional<T>` на API этих примитивных классов: `OptionalInt.of(67).map(x -> x+1)` — ошибка метода (O-N08). Не выдавайте отсутствие этих API-методов за отсутствие возможностей преобразовать примитивное значение в прикладном коде.

Источники: [OptionalInt Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/OptionalInt.html), [OptionalLong](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/OptionalLong.html), [OptionalDouble](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/OptionalDouble.html).

## 10. Равенство, identity и контракт публичного API (OT-15, OT-16, OT-18; OT-22 P1)

`Optional` — **value-based class**. Методы `equals`/`hashCode` опираются на значение и состояние пустоты, а **идентичность ссылки не является контрактом**: даже если в конкретной JVM `Optional.empty() == Optional.empty()` получилось `true`, нельзя полагаться на singleton или синхронизироваться на объекте Optional. `OT32=true` в G1 — результат **`equals`** для двух `Optional.of("same")`, **не** `==`. [Optional API Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html).

В API Note рекомендуется использовать `Optional` **преимущественно как тип возвращаемого результата** для ответа «значение может отсутствовать». Это **не запрет языка Java на поле, параметр или вложенную generic-структуру** с `Optional`. Для persistence/serialization/JSON/ORM выбор зависит от конкретной версии и правил инструмента; этот набор интеграций не исследован.

~~~java
import java.util.Optional;

class UserLookup {
    static Optional<String> lookup(String key) {
        if (key == null) throw new IllegalArgumentException("key");
        return "known".equals(key) ? Optional.of("Java") : Optional.empty();
    }
}
~~~

Здесь `null` в **ключе** — ошибка API-контракта, но ненайденная запись — **допустимая пустота**. Если по другому контракту `null` тоже означает «нет запроса», код должен явно так и определять; нет универсального правила, что для всякой функции `null` — IAE. Не скрывайте ошибочное состояние при помощи `Optional.empty()`; [CORE-ASSERT](assertions.md) отдельно разбирает инварианты и обязательные проверки.

## 11. Матрица выбора: present / empty × вызов × эффект

| Операция | При present | При empty | Важная граница |
|---|---|---|---|
| `orElse(fallback())` | Вызывается `fallback()`, но возвращается T из Optional | Вызывается `fallback()`, возвращается fallback T | Аргумент eager |
| `orElseGet(() -> fallback())` | Supplier **не** вызывается | Supplier вызывается, возвращает T | null supplier при present допустим, при empty — NPE |
| `or(() -> Optional.of("x"))` | Supplier **не** вызывается | Supplier вызывается, возвращается Optional | null *аргумент* supplier недопустим; null result — NPE |
| `map(f)` | f вызывается; null result → empty | f не вызывается | null *аргумент* f → NPE даже empty |
| `flatMap(f)` | f вызывается; null result → NPE | f не вызывается | Ожидает Optional<U> |
| `filter(p)` | p вызывается; false → empty | p не вызывается | Пустой `filter` продемонстрирован в §6 |
| `ifPresentOrElse(a,e)` | Выполняется a | Выполняется e | `void`, не Optional<T> |
| `stream()` | Stream из 1 элемента | Пустой Stream | Конвейер Stream — другая тема |
| `orElseThrow()` | Возвращает T | Бросает NSEE | Runtime, не compile-negative |

Для ряда методов контракт null **самого callback-аргумента** отличается от его вызова. Не обобщайте таблицу на `null` у всех callback: отдельные Javadoc-предусловия имеют приоритет.

## 12. Воспроизводимая трасса принятого G1: OT01–OT36

В [G1 §6.2](../../docs/plans/CORE-ASSERT-OPTIONAL.md) есть полный, проверенный на OpenJDK21 `OptionalProbe.java` с **36 подписанными строками одного JVM-процесса**. Это **отдельная** программа от двух запусков `AssertProbe`, а не 36 JUnit-тестов. Пример счётчика fallback использует `AtomicInteger` для удобной явной трассы, **не** демонстрирует многопоточность.

| Строки | Ожидаемое | Разобранная причинная связь |
|---|---|---|
| OT01/OT02/OT03/OT04 | `true:false` / `false:true` / `true` / `NPE` | Создание и presence, `of(null)` runtime |
| OT05/OT06/OT07/OT08 | `Java` / `NSEE` / `NSEE` / `ISE:missing` | Строгое извлечение и exception supplier |
| OT09/OT10/OT11 | `Java:1` / `Java:1` / `fallback:2` | Eager `orElse` vs lazy `orElseGet` |
| OT12/OT13/OT14 | `4` / `true` / `Java` | `map`, null→empty и `flatMap` |
| OT15/OT16/OT17 | `true` / `Java` / `true` | present.filter false/true, **absent.map**, НЕ empty.filter |
| OT18/OT19 | `Java` / `other` | `or` present/absent |
| OT20/OT21/OT22/OT23/OT24 | `NPE:flatMap-null` / `NPE:or-null` / `NPE:missing-supplier` / `Java` / `null` | Runtime null contracts |
| OT25/OT26/OT27 | `[A, B]` / `1:0` / `Java-empty` | Stream мост и действия |
| OT28/OT29/OT30/OT31 | `67` / `67` / `2.5` / `67` | Специализированные примитивные контейнеры |
| OT32/OT33/OT34/OT35/OT36 | `true` / `5` / `NSEE:int` / `true:true` / `Java` | Value-based equals, map и API-получение |

**Специальное исправление N2 (отдельно от G1 stdout):** новый фрагмент `FilterDemo` из §6 действительно вызывает `missing.filter(validPredicate)`. Он **выполнен автором 02** на OpenJDK21.0.12.1: три stdout строки `true` / `Java` / `true:0`. В принятом плане строка `OT17` относится только к `absent.map`. Изменений в принятом G1-файле **нет**. Эта новая локальная проба не заменяет 36 ранее проверенных строк G1 и не является committed CI.

## 13. Что компилятор отклоняет, а что решается в runtime (OT-08/09/14/17)

В принятом G1 из 11 негативных компиляций **9 относятся к Optional**: O-N01–O-N09. Кратко, не подменяя полные изолированные исходники из [G1 §7](../../docs/plans/CORE-ASSERT-OPTIONAL.md):

| Негативный файл | Категория ошибки компиляции |
|---|---|
| O-N01 | `Optional<int>` невозможен: аргумент generic-типа обязан быть ссылочным |
| O-N02 | У `Optional<Integer>` нет метода `getAsInt()` |
| O-N03 | `Optional<String>` нельзя присвоить результат `ofNullable(67)` |
| O-N04 | `orElseGet("B")`: ожидается Supplier, а не String |
| O-N05 | `map(String::length)` возвращает `Optional<Integer>`, не `Optional<String>` |
| O-N06 | `flatMap(s -> s.length())`: возвращаемый тип не Optional |
| O-N07 | `or(() -> "B")`: supplier должен вернуть Optional |
| O-N08 | У `OptionalInt` нет `map` generic API |
| O-N09 | `stream()` возвращает Stream, не Optional |

Иное дело `Optional.of(null)`, `empty.get()`, `flatMap(s -> null)`, `or(() -> null)`, `empty.orElseGet(null)`: **валидно компилируются**, но при указанном пути выполнения бросают runtime NPE/NSEE. Подменять runtime-проверку отрицательным `javac` нельзя. 04 проверил **11/11 изолированных полных compiler-negative** всего набора G1 с тематическими диагностическими категориями **вне обычного проектного CI**.

## 14. Границы P1/P2 и карта полноты OT-01–OT-29

| ID | P | Раздел, механизм и доказательная граница |
|---|---|---|
| OT-01 | P0 | §1: Optional<T> non-null value/absence, OT01–03 |
| OT-02 | P0 | §1: of(null) NPE runtime vs ofNullable, OT03/04 |
| OT-03 | P0 | §1: isPresent/isEmpty, OT01/02 |
| OT-04 | P0 | §2: get/orElseThrow empty → NSEE, OT05–07 |
| OT-05 | P0 | §3: eager orElse, OT09 |
| OT-06 | P0 | §3: lazy orElseGet, OT10/11/22/23 |
| OT-07 | P0 | §4: or(Supplier<Optional>), OT18–21 |
| OT-08 | P0 | §5: map null-return → empty, OT12/13/17/33 |
| OT-09 | P0 | §5: flatMap Optional result and null-return NPE, OT14/20 |
| OT-10 | P0 | §6: present.filter OT15/16 + **new empty.filter true:0 trace**; OT17 is absent.map |
| OT-11 | P0 | §7: ifPresent/ifPresentOrElse, OT27 |
| OT-12 | P0 | §2: orElseThrow custom exception, OT08 |
| OT-13 | P0 | §8: Optional.stream 0/1, OT25/26 |
| OT-14 | P0 | §9: OptionalInt/Long/Double distinct API, OT28–31/34/35 |
| OT-15 | P0 | §10: value-based equals not identity, OT32 |
| OT-16 | P0 | §10: primarily return type API Note, no JLS ban on fields |
| OT-17 | P0 | §5, §11: null contracts map/flatMap/or, OT13/20/21 |
| OT-18 | P0 | §10: absence vs invalid input/state, Optional and IAE |
| OT-19 | P1 | §5: null mapper/filter argument, runtime NPE; separate probe not run |
| OT-20 | P1 | §3–4: or(null) vs orElseGet(null), different checks |
| OT-21 | P1 | §2: API Note get vs orElseThrow |
| OT-22 | P1 | §10: equals/hashCode, empty singleton not guaranteed |
| OT-23 | P1 | §9: primitive API lacks generic map/flatMap |
| OT-24 | P1 | §2, §11: choose get after isPresent or chain on readability |
| OT-25 | P2 | §10: ORM/JPA/JSON/serialization integrations NOT RUN |
| OT-26 | P2 | §10: JMM/structured concurrency and synchronization NOT RUN |
| OT-27 | P2 | §9: primitive double NaN/signed-zero edge NOT RUN |
| OT-28 | P2 | §10: scalar replacement/allocation/JIT benchmarks NOT RUN |
| OT-29 | P2 | §8: parallel streams/collectors/null-source edge NOT RUN |

**Нормативные Java 21 источники:** [Optional<T> API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html), [OptionalInt](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/OptionalInt.html), [OptionalLong](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/OptionalLong.html), [OptionalDouble](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/OptionalDouble.html), [JLS §15.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.7). Реальные интервью-вопросы C1/C3/C4 см. в [принятом G1 §2](../../docs/plans/CORE-ASSERT-OPTIONAL.md); их роль — подтвердить постановку вопроса, не JLS/API правила или статистику.

**Граница доказанного:** исходники `AssertProbe` и `OptionalProbe` в G1 были выполнены на OpenJDK 21.0.12.1 и дали соответственно 15+15 и 36 stdout строк, отдельно 11 compiler-negative; это **историческое G1/04 evidence**, а не опубликованный автором G2 runnable и не автоматическая компиляция Java-блоков этой статьи. Новый `FilterDemo` §6 специально добавлен для исправления N2 и отдельно скомпилирован/запущен: stdout `true` / `Java` / `true:0`. Дополнительная временная локальная проба подтвердила null-аргументы mapper/predicate, `or(null)` и `orElseGet(null)`. Эти исходники не публикуются в авторском G2 PR и не входят в CI. До интеграции технического PR роли 03 не обещать доступность будущего `examples/core/assert-optional/`. **NOT RUN:** компиляция всех inline фрагментов этой статьи, отдельные JVM classloader/JPMS/JMM modes, system assertions при `-esa`, ORM/JSON/JPA, JIT/GC/JMH и другие версии JDK/OS, GUI/anchors, все P2. `COVERAGE.md` и `CURRICULUM.md` не менять до G5.
