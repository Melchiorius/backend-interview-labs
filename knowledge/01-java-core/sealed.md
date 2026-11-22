# CORE-SEALED — управляемые иерархии типов Java 21

**Главный вопрос:** какие классы вправе непосредственно наследовать данный класс или реализовать данный интерфейс, и почему ограничение *прямых* подтипов не запрещает всех косвенных наследников? Материал рассчитан на Java SE 21 без preview. Предварительно полезны [наследование](inheritance.md), [интерфейсы](interfaces.md), [классы](classes.md), [record](records.md). Сразу после статьи читайте [pattern matching и switch](patterns.md).

**Основание:** [принятый исследовательский план](../../docs/plans/CORE-SEALED-PATTERNS.md), матрица **S-01–S-20: 12 P0 / 5 P1 / 3 P2**. Норма — JLS 21 [§8.1.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.1.2), [§8.1.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.6), [§9.1.1.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.1.1.4) и [§9.1.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.1.4). Пометки о фактическом запуске внизу отделены от ожидаемых результатов отдельных code snippets.

## 1. Final, sealed, non-sealed — три разные цели (S-01, S-05, S-07, S-16)

Незакрытый обычный класс при соблюдении доступности можно расширять новыми подклассами. Это подходит библиотечной точке расширения. Однако бизнес-результат часто должен представлять ограниченный набор вариантов: успешное значение, ошибку или ожидание. Здесь полезен **sealed type**: разработчик указывает допустимые **прямые** дочерние типы, а компилятор отклоняет неразрешённые непосредственные наследования.

`final` для класса запрещает дальнейшее наследование вообще. `sealed` разрешает перечисленные прямые ветви. `non-sealed` может объявить разрешённая ветвь, чтобы заново открыть её наследование; не существует права написать `non-sealed` на любом обычном классе без sealed-предка.

~~~java
sealed interface Result permits Ok, Err, Pending {}
record Ok(int value) implements Result {}
record Err(String message) implements Result {}
non-sealed class Pending implements Result {
    int count;
    void increment() { count++; }
}
final class Later extends Pending {}
~~~

У `Result` три **прямых** допустимых подтипа — `Ok`, `Err`, `Pending`. А `Later` — уже **косвенный** подтип `Result`. Он легален, хотя не перечислен в `permits Result`, потому что наследуется от открытой ветви `Pending`. Но `class Rogue implements Result` без включения в permits не скомпилируется. Это и есть различие между контролем первого шага и контролем всего дерева.

Внутреннее состояние `Pending.count` изменяемо: метод `increment` вправе увеличить его. Sealed **не обещает глубокой неизменяемости**, потокобезопасности, финальности всех членов и стабильности hash. Эти контракты отдельно раскрываются в [CORE-IMMUTABLE](immutability.md).

## 2. Правила прямого наследника и точность permits (S-02, S-04, S-12, S-14)

Когда sealed declaration явно содержит `permits A, B`, каждый указанный тип обязан **непосредственно** наследовать/реализовать sealed родителя. Нельзя внести в список только внука, ошибочно принимая его за непосредственного сына. Нельзя также добавить прямого наследника, не включённого в список.

Разрешённый непосредственный subclass или subinterface должен явно либо неявно определить продолжение иерархии: **`final`, `sealed` или `non-sealed`**. Record как класс неявно final, потому подходит sealed interface. Но `class Pending implements Result {}` без какого-либо из трёх вариантов недопустим. Интерфейс, в отличие от класса, не может быть final: direct subinterface выбирает sealed или non-sealed.

~~~java
sealed abstract class Operation permits Read, Write {}
sealed class Read extends Operation permits ReadOne {}
final class ReadOne extends Read {}
non-sealed class Write extends Operation {}
final class WriteLater extends Write {}
~~~

Здесь допустимы оба прямых подтипа Operation, а также их косвенные наследники. Конструкция `sealed abstract class` законна: абстрактность относится к созданию экземпляра, sealed — к перечню прямых потомков. Сам `ReadOne` не обязан находиться в списке Operation.permits: он прямой потомок Read.

**Отрицательные случаи, компилятор:** `class Unlisted extends Operation`; обычный `class Child extends Operation`, не выбравший модификатор; `sealed final class Both`; `class Plain permits Child`; `non-sealed class Loose` без sealed supertype. Их нельзя объединить в один позитивный файл: каждый относится к отдельной diagnostic category. В G1 это N01–N06, N20–N22. Здесь нет runtime exception — программа вообще не проходит javac.

Особенно важно, что `permits` содержит имена, а не «все наследники из пакета». У типа должны быть корректное каноническое имя, доступность и требуемая непосредственная связь с sealed declaration; отдельное разрешение не отменяет правил модулей и пакетов.

## 3. Неявный permits: same compilation unit и canonical name (S-03, S-14)

Можно не писать `permits`, если разрешённые **прямые** типы объявлены в той же *compilation unit* (обычно один исходный файл) и имеют canonical names. Тогда компилятор формирует разрешённый список из подходящих объявлений; он **не сканирует classpath, другие файлы пакета или исходники других модулей**.

~~~java
class ImplicitDemo {
    sealed interface Token {}
    record Word(String text) implements Token {}
    final static class Marker implements Token {}

    static String describe(Token t) {
        return switch (t) {
            case Word(String text) -> "word:" + text;
            case Marker m -> "marker";
        };
    }
    public static void main(String[] args) {
        System.out.println(describe(new Word("A"))); // word:A
        System.out.println(describe(new Marker()));   // marker
    }
}
~~~

`Word` и `Marker` — прямые реализации с canonical names, объявленные рядом с `Token`. Переезд `Marker` в другой файл без явного разрешения меняет корректность программы. Anonymous classes и local classes не могут «случайно» стать такими permitted implementations: у них нет необходимого canonical name. Не путайте *nested* class, у которого canonical name обычно есть, с *local* class внутри метода.

Эта деталь важна при обычном рефакторинге: перенос одного объявления в другой файл — не только косметическое изменение, если sealed parent полагается на implicit permits.

## 4. Именованный и безымянный модули: разные границы (S-06, S-17)

Для sealed type, относящегося к **unnamed module**, каждый permitted direct subtype должен быть в **том же package**. Поэтому объявления в `p.Root` и `q.Bad` не могут образовать permitted direct relation внутри обычного unnamed module. Исследовательская компиляция N24 в двух файлах `p/Root.java` и `q/Bad.java` дала ожидаемую javac category `class.in.unnamed.module.cant.extend.sealed.in.diff.package`.

Для sealed type из **named module** правило шире по пакетам, но строже по модулю: permitted direct subtype может жить в **другом package того же named module**, однако не в произвольном другом модуле. G1 **M01** специально проверил эту разницу в трёх отдельных файлах:

~~~text
module-info.java
p/Root.java
q/Good.java
~~~

В первом файле: `module sealed.pilot { exports p; }`. Во втором:

~~~java
package p;
public sealed interface Root permits q.Good {}
~~~

В третьем файле:

~~~java
package q;
public final class Good implements p.Root {}
~~~

Проверенная G1 команда: `javac --release 21 -Xlint:all -d out module-info.java p/Root.java q/Good.java` — **exit 0 без warnings** на OpenJDK 21.0.12.1. Эти два небольших Java-блока **намеренно отдельные файлы**, не единая compilation unit. **M01 не запускался через JVM `--module-path`**, здесь не утверждается, что модульное приложение целиком тестировалось. Доступность типов при выполнении и экспорт пакетов — отдельная задача JPMS.

## 5. Record, generic types и SAM-ограничение (S-08, S-09, S-10, S-15)

Record уже неявно final и может быть непосредственной реализацией sealed interface. Это удобно для предметного результата, в котором важны виды значения, а не изменение внутренних объектов.

~~~java
sealed interface Response<T> permits Success {}
record Success<T>(T value) implements Response<T> {}

class GenericSealedDemo {
    public static void main(String[] args) {
        Response<String> value = new Success<>("ok");
        System.out.println(value instanceof Success<?>); // true
    }
}
~~~

Generic arguments остаются обычными Java types: `Response<int>` — compile error из-за primitive type argument; `Response<int[]>`, напротив, корректно, потому что **сам массив — ссылочный тип**. Содержимое `int[]` остаётся изменяемым даже внутри record, что подробно разобрано в [CORE-ARRAYS](arrays.md), [CORE-GENERICS](generics.md), [CORE-RECORDS](records.md).

**Sealed interface не является target functional interface для lambda**, даже если в нём всего один абстрактный метод. Lambda не может обойти перечень permitted direct implementations.

~~~java
sealed interface Task permits TaskImpl {
    void run();
}
final class TaskImpl implements Task {
    public void run() {}
}
// Task task = () -> {}; // compile error, а не новое разрешённое воплощение
~~~

Сама концепция [SAM и lambda](interfaces.md) полезна, но не отменяет специальное ограничение sealed interface. В G1 N07 отдельно проверено отклонение такой lambda по категории compiler diagnostic.

**Enum:** особые правила Java21 могут делать enum неявно final либо sealed, когда отдельные константы имеют class bodies; enum может законно реализовать подходящий sealed interface при соблюдении JLS. Это лишь **контраст внутри статьи о sealed**: конструкторы и методы enum, постоянство identity и специализированные коллекции подробно описаны отдельно в [CORE-ENUMS](enums.md). Реестр curriculum обновляется только после G5 публикации, не в этой авторской ветке.

## 6. Зачем sealed помогает исчерпывающему switch (S-01–S-05, S-11, S-16)

Компилятор способен оценивать coverage всех разрешённых ветвей sealed hierarchy. Если у `Result` ровно три permitted direct subtypes `Ok`, `Err` и `Pending`, достаточно трёх типовых ветвей, даже если существует `Later extends Pending`.

~~~java
sealed interface Answer permits Yes, No, PendingAnswer {}
record Yes(int value) implements Answer {}
record No(String reason) implements Answer {}
non-sealed class PendingAnswer implements Answer {}
final class LaterAnswer extends PendingAnswer {}

class SealedSwitch {
    static String classify(Answer a) {
        return switch (a) {
            case Yes(int value) -> "yes:" + value;
            case No(String reason) -> "no:" + reason;
            case PendingAnswer p -> "pending";
        };
    }
    public static void main(String[] args) {
        System.out.println(classify(new Yes(67)));  // yes:67
        System.out.println(classify(new LaterAnswer())); // pending
    }
}
~~~

**Трассировка:** `new LaterAnswer()` соответствует типовой метке `case PendingAnswer p`, потому что `LaterAnswer` — её подтип. Никакой `case LaterAnswer l` не требуется; если разместить такую метку **после** `case PendingAnswer p`, она станет dominated. При этом компилятор отвергнет switch-expression, если пропущена вся ветка `PendingAnswer` и нет достаточного иного покрытия. Exhaustiveness, dominance и null-selector — правила [CORE-PATTERNS](patterns.md), а не встроенный equals.

**G1 положительный исходник `SealedProbe.java`:** 11 подписанных строк `S01=ok:67`, `S02=err:bad`, `S03=pending` (косвенный Later), `S04=token:A` и `S05=marker` (implicit permits), `S06=true` (sealed abstract State), `S07=true` (`Class.isSealed()`), `S08=true` (record), `S09=true` (Later instanceof Pending), `S10=generic` и `S11=1` (после изменения счётчика в Pending). Это **11 наблюдений одного Java-процесса, не 11 тестов**. Принятый исходник и точный stdout приведены целиком в [G1 §7.1](../../docs/plans/CORE-SEALED-PATTERNS.md). Для самостоятельной проверки на Java 21 используйте [общий runnable Sealed + Patterns](../../examples/core/sealed-patterns/README.md): [SealedProbe.java](../../examples/core/sealed-patterns/src/SealedProbe.java) с 11 подписанными строками и [PatternsProbe.java](../../examples/core/sealed-patterns/src/PatternsProbe.java) с 20 строками; отдельный `verify.sh` сверяет два stdout с двумя эталонами через `diff -u`. Это 31 наблюдение **двух** JVM-процессов, не 31 JUnit-тест.

## 7. Чем compile error отличается от runtime (S-02, S-04, S-07, S-10, S-12)

| Случай | Этап | Причина |
|---|---|---|
| Неразрешённый direct наследник sealed parent | **javac error** | Не в permits |
| Direct child без final/sealed/non-sealed | **javac error** | Не выбрал продолжение закрытия |
| `permits` у обычного класса | **javac error** | Отсутствует sealed |
| `sealed final class` | **javac error** | Несовместимая пара модификаторов |
| `non-sealed class Loose {}` | **javac error** | Нет direct sealed supertype |
| Lambda как реализация sealed interface | **javac error** | Не functional interface |
| `Later extends Pending`, Pending non-sealed | **Законно** | Косвенный потомок |
| `pending.increment()` | **Законно** | Sealed не делает объект immutable |
| Switch-expression без достаточных ветвей | **javac error** | Не exhaustive |
| Switch с record deconstruction и сломанным accessor | **runtime MatchException** | Совсем другой контракт pattern matching |

По G1 **N01–N23** компилировались изолированно с ожидаемыми diagnostic keys, **N24** отдельно охватывал unnamed-module cross-package. Не нужно помещать эти негативные исходники в положительный runnable: иначе его сборка ожидаемо провалится. Ошибки pattern dominance, null selector и `MatchException` раскрыты в [CORE-PATTERNS](patterns.md).

## 8. Границы: открытая ветвь, эволюция, Reflection и производительность (S-18–S-20)

Программная модель sealed полезна, когда набор *прямых* вариантов контролируется автором API. Но добавление нового permitted subtype требует повторного анализа всех клиентов, написавших exhaustive switch. Sealed не делает бинарную совместимость изменений автоматической.

**S-18 P2:** отдельная бинарная эволюция после раздельной компиляции (потенциальные linkage failures и `IncompatibleClassChangeError`) **не проводилась**; см. [JLS 21 §13](https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html). Нельзя выдавать теоретический риск за результат проведённого JVM эксперимента.

**S-19 P2:** G1 проверил `Class.isSealed()`, но исследование полного Reflection API, `getPermittedSubclasses()`, разных модулей и настоящего JVM module-path запуска **не выполнено**. M01 — позитивная **компиляция**, а не JPMS runtime.

**S-20 P2:** JVM может использовать дополнительную информацию о типах для оптимизации, но из этого не следует гарантия производительности. Нет JMH/профилирования JIT/GC и измерений; не утверждайте, что sealed всегда быстрее интерфейса.

## Карта S-01–S-20: точная привязка к тексту

| ID | Приоритет | Раздел и предметная проверка |
|---|---|---|
| S-01 | P0 | §1/6: sealed class vs sealed interface; direct vs indirect |
| S-02 | P0 | §2/7: explicit permits и Unlisted (N01/N06) |
| S-03 | P0 | §3: implicit permits, compilation unit, canonical name |
| S-04 | P0 | §2/5: direct final/sealed/non-sealed (N02/N22) |
| S-05 | P0 | §1/6: Pending non-sealed, Later allowed, S03/S09 |
| S-06 | P0 | §4: unnamed same package vs named same module; N24/M01 |
| S-07 | P0 | §1/2: final vs sealed (N05) |
| S-08 | P0 | §5: record implicit final, G1 S08 |
| S-09 | P0 | §5: generic sealed Response<T>, G1 S10 |
| S-10 | P0 | §5: sealed interface не SAM, N07 |
| S-11 | P0 | §1/6: mutable Pending, G1 S11 |
| S-12 | P0 | §2/7: invalid permits, non-sealed without parent (N03/N04/N20) |
| S-13 | P1 | §2: sealed abstract class и цепочка sealed→sealed→final |
| S-14 | P1 | §§2–3: permits и canonical names, no local/anonymous |
| S-15 | P1 | §5: enum как контраст и [отдельная CORE-ENUMS](enums.md), без заявления о её реализации в этой статье |
| S-16 | P1 | §§1/6/8: domain result vs unrestricted inheritance |
| S-17 | P1 | §4: named module positive compile M01, не module runtime |
| S-18 | P2 | §8: binary evolution / ICCE — NOT RUN |
| S-19 | P2 | §8: Reflection/JPMS module-path — NOT RUN |
| S-20 | P2 | §8: JIT/GC/JMH — NOT RUN |

## Источники, факты проверки и ограничения

**Нормативно:** [JLS 21 classes §8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html), [JLS 21 interfaces §9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html), [JLS 21 switch §14](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html); [Oracle Java 21 sealed guide](https://docs.oracle.com/en/java/javase/21/language/sealed-classes-and-interfaces.html). [JEP 409](https://openjdk.org/jeps/409) — история. Независимые интервью-источники из [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md) помогают выбирать вопросы, **не заменяют JLS**.

**G1 фактически проверял** `javac --release 21 -Xlint:all` и `java` на Debian OpenJDK 21.0.12.1 для двух полных исходников: S01–S11 (11 строк) и P01–P20 (20 строк). Отдельно N01–N24 дали тематические compile errors и M01 положительную компиляцию named module. Данные принадлежат принятому исследованию; **не утверждается**, что каждый отдельный snippet этой статьи независимо запускался автором 02 или включён в CI.

**NOT RUN:** автоматическая компиляция всех inline Java-блоков, негативные N01–N24 в GitHub CI, JVM module-path, separate binary evolution, GUI Obsidian/IntelliJ, альтернативные JDK, полноценные Reflection/углублённые enum internals в рамках CORE-SEALED, JIT/GC/JMH и иные P2-эксперименты.
