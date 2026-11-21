# CORE-RECORDS + CORE-IMMUTABLE — предварительный G1-план (Java SE 21)

**Статус: G1 PROPOSED / NOT ACCEPTED.** Роль 01 — Research & Coverage; [Issue #127](https://github.com/Melchiorius/backend-interview-labs/issues/127), программа [#33](https://github.com/Melchiorius/backend-interview-labs/issues/33).
**BASE/main на момент исследования:** `7218877f94cddfcfd30bf9c149192fbce3b4cb23` (после G6 PR #128); это не SHA будущего G2 и не замена актуального HEAD исследовательского PR.
**Версия нормы:** JLS Java SE **21**, API JDK **21**, без preview. **Проверки ниже:** OpenJDK `javac 21.0.12.1` и `java 21.0.12.1`, Linux, `--release 21`. G1 не публикует учебные статьи и не изменяет `COVERAGE`/`CURRICULUM`.

## 1. Вопрос читателя, область и решение о композиции

**Что должен уметь кандидат:** объяснить, что именно гарантирует объявление `record` и чего оно не гарантирует; предсказать результат сравнения двух записей, если компонент — массив; построить валидируемую запись с compact constructor; отделить неизменяемость ссылочного поля от неизменяемости достижимого состояния; закрыть утечки через входные и выходные ссылки; различить защитную копию и unmodifiable view; выбрать `record` либо обычный `final class` под конкретный инвариант.

**Рекомендация G1: две самостоятельные содержательные статьи, один совместный runnable при G2, если авторы сочтут полезным.**
- `CORE-RECORDS`: семантика объявления, автоматические члены, constructors, equality, ограничения, примеры и вопрос о transparently exposed components; предполагаемый путь `knowledge/01-java-core/records.md` **ещё не существует, ссылкой на готовый материал не является**.
- `CORE-IMMUTABLE`: invariants, aliasing, snapshot/view, copy-in/copy-out, shallow/deep boundary, `final`, проектирование обычного класса; предполагаемый путь `knowledge/01-java-core/immutability.md` **ещё не существует**.
- Один возможный runnable `examples/core/records-immutability/` с двумя независимо запускаемыми программами (`RecordsProbe`, `ImmutableProbe`) позволит сопоставить поведение; создавать его должна 03 после G1, не роль 01. Никаких ритуальных упражнений/тестов не требуется.

Почему не одна статья: ограничения record — спецификация языка и контракты генерируемых методов, а неизменяемость — свойство внешне наблюдаемого поведения произвольного объекта, требующее анализа графа ссылок и инвариантов. `record` не является универсальным immutable-классом. Подмена понятий приводит к неверным ответам и плохому API. Почему не три статьи: defensive copies и unmodifiable views — разные стороны одной задачи управления aliasing, отдельный файл без самостоятельной предметной цели дал бы дублирование.

**Не входит:** `CORE-ENUMS`, `CORE-SEALED`, `CORE-PATTERNS`, полная теория `COL-CONTRACTS`, `CON-JMM`, `CORE-REFLECTION`, `CORE-SERIAL`, JPMS, JPA/ORM, особенности конкретной JVM/bytecode, JIT, производительность, GC и безопасность потоковой публикации. `record patterns` Java 21 — только мост к `CORE-PATTERNS`, не содержимое нынешнего P0.

## 2. Действительное покрытие на BASE/main и рабочие связи

По актуальным [CURRICULUM](../CURRICULUM.md), [COVERAGE](../COVERAGE.md) и [JAVA_CORE_PLAN](../JAVA_CORE_PLAN.md) опубликованы **21 самостоятельная Java Core статья / 22 из 42 curricular ID**. `CORE-RECORDS` и `CORE-IMMUTABLE` остаются **не опубликованными самостоятельными темами**; встречающаяся в готовых статьях вводная информация не заменяет G1/G2.

| Опубликованный материал, реальный локальный путь | Что уже объяснено | Что именно остаётся этому G1 |
|---|---|---|
| [CORE-TYPES](../../knowledge/01-java-core/types.md) | ссылочные типы, `null`, `final` у переменной | `final` у поля компонента record vs состояние объекта |
| [CORE-CLASSES](../../knowledge/01-java-core/classes.md) | объекты, поле, конструктор, инвариант | специальный canonical/compact record constructor, copy-in/out |
| [CORE-PARAMS](../../knowledge/01-java-core/parameters.md) | передача *значения ссылки*, aliasing, String | что делать с ссылкой после получения в constructor/accessor |
| [CORE-OBJECT](../../knowledge/01-java-core/object.md) | Object, identity, базовое равенство | refined contract `java.lang.Record` |
| [CORE-EQUALITY](../../knowledge/01-java-core/equality.md) | value equality, hashCode, mutable-key risks | генерируемые методы record и массивы как компоненты |
| [CORE-GENERICS](../../knowledge/01-java-core/generics.md) | параметризация, инвариантность | generic record и статические ограничения |
| [CORE-ARRAYS](../../knowledge/01-java-core/arrays.md) | ковариантность, изменяемость, shallow copy, Arrays | дефект record с массивом и defensive-copy design |
| [CORE-STRINGS/BUILDERS](../../knowledge/01-java-core/strings.md) | immutable String vs mutable Builder | общая модель неизменяемого объекта со сложным графом ссылок |
| [CORE-INHERITANCE](../../knowledge/01-java-core/inheritance.md) | final и inheritance | record implicitly final, superclass Record, interfaces |

Ссылки в таблице ведут **только к существующим файлам**; будущие ID оставлены обычным текстом. Обратные ссылки на новые статьи добавить после G5/G6, не в исследовательском PR.

## 3. Источники, версия, уровень доверия

Начальная карта — [SOURCE_REGISTRY](../SOURCE_REGISTRY.md); ниже конкретизированы источники, применимые именно к этой паре. **A** — нормативные контракты, **C** — внешний независимый банк вопросов, полезный для поиска пропусков, но не для доказательства ответа.

**A — спецификация/API:**
- **A1:** [JLS 21 `8.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10), [`8.10.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10.1) components, [`8.10.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10.2) record body, [`8.10.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10.3) fields/accessors/generated methods, [`8.10.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10.4) constructors. Обязательный первоисточник.
- **A2:** [`8.10.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10.4.1) normal canonical constructor и [`8.10.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10.4.2) compact; [`8.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1) ограничения класса. Не переносить синтаксис из preview JDK.
- **A3:** [java.lang.Record — Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Record.html): контракт equality/copy invariant, `hashCode` и `toString`. Алгоритмы и порядок сравнения не фиксировать сверх нормы.
- **A4:** [List Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html), метод `copyOf` и раздел «Unmodifiable Lists»: no `null`; возвращённый список не отражает последующие структурные изменения исходной коллекции; **элементы могут оставаться mutable**, identity/copy allocation не обещаны.
- **A5:** [Collections.unmodifiableList Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#unmodifiableList(java.util.List)): *view*, читающий актуальный backing list; попытка изменить через view вызывает `UnsupportedOperationException`.
- **A6:** [Arrays.copyOf Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html#copyOf(int%5B%5D,int)), [Object Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html), [Collection — unmodifiable vs immutable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collection.html). `copyOf` для массива создаёт новый массив, но вложенные mutable элементы не копирует рекурсивно.
- **A7 (только граница):** [JLS 21 `4.12.4 final variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4), [`17.5 final field semantics](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.5). Полные правила publication/happens-before относятся к `CON-JMM`; не заменять их лозунгом «все immutable thread-safe».

**C — конкретные *вопросы*, независимо от A:**
- **C1 / Q-CHERNYSHOV:** [What is Record and Since Which Version?](https://viacheslavchernyshov.github.io/java-interview-questions-and-answers/questions/20-records-and-generics/01-what-is-record-in-java-and-since-which-version-are-they-available/), [Differences from Regular Class](https://viacheslavchernyshov.github.io/java-interview-questions-and-answers/questions/20-records-and-generics/02-what-are-the-main-differences-between-record-and-regular-class/), [Generated Methods](https://viacheslavchernyshov.github.io/java-interview-questions-and-answers/questions/20-records-and-generics/05-what-methods-are-automatically-generated-for-a-record/), [Constructors](https://viacheslavchernyshov.github.io/java-interview-questions-and-answers/questions/20-records-and-generics/06-can-you-override-constructor-in-record/), [Immutable Class](https://viacheslavchernyshov.github.io/java-interview-questions-and-answers/questions/13-immutability/03-how-to-create-an-immutable-class-in-java/). Использовать **формулировки проверяемых вопросов**, технические обобщения/bytecode утверждения перепроверять в A.
- **C2 / Q-BAELDUNG-CLASS:** [Java Structure and Initialization Interview Questions — Q8: What Is an Immutable Class, and How Can You Create One?](https://www.baeldung.com/java-classes-initialization-questions). Дополнительно `java-record-keyword` — поясняющий материал B, **не** второй нормативный источник: [Record Keyword](https://www.baeldung.com/java-record-keyword).
- **C3 / Q-TEAMLEAD:** [teamlead/java-interview-questions](https://github.com/teamlead/java-interview-questions), прямые строки вопросов “What is ensured by immutable”, “Why is immutable so important”, “What implies immutable”; слабая/машинная терминология исходного списка — не переносить её в статью без редакции.
  
Наличие трёх независимых наборов вопросов **не** является статистическим свидетельством частоты, полноты или правильности чужих ответов. Записи C помогают сформулировать вопросы и проверить упущения, но каждое утверждение ниже имеет основание A.

## 4. Матрица P0: вопрос → точный механизм → проверка → граница

Здесь P0/P1/P2 — обязательность в конкретной теме, **не** ярлык Middle/Senior. `Rxx/Ixx` — ID вопроса плана, `R01…` и `I01…` в колонке «Проба» — маркеры stdout; `Nxx` — изолированная compile-negative проба. В каждой строке P0 есть нормативный A и самостоятельная проверка либо контрпример.

| ID | P | Вопрос / ожидаемое объяснение | A / интервью C | Проба / контрпример; граница |
|---|---|---|---|---|
| R-01 | P0 | Что есть record: специальный `final` класс, прямой суперкласс `Record`, реализуемые интерфейсы | A1,A3 / C1 | `RecordsProbe` / N10,N11; не вся иерархия |
| R-02 | P0 | Компонент, `private final` field, публичный accessor без префикса `get` | A1,A3 / C1 | R01, N04,N13; не reflection API |
| R-03 | P0 | `equals` генерируется по компонентам, а не identity всей записи | A1,A3 / C1 | R02,R07,R09; см. [equality](../../knowledge/01-java-core/equality.md) |
| R-04 | P0 | `hashCode` согласован с `equals`; точное число/алгоритм не обещаны | A1,A3 / C1 | R03 (`равные → одинаковый hash`); не полагаться на hash как ID |
| R-05 | P0 | `toString` отражает record name, компоненты и значения; формат деталей реализации не превращать в «формат для парсинга» | A1,A3 / C1 | R04 (`contains` на этом JDK); не API сериализации |
| R-06 | P0 | Canonical constructor: какие параметры, имена, доступность; отличие от default constructor обычного класса | A1,A2 / C1 | R01,R06; N07,N08 |
| R-07 | P0 | Compact constructor: скрытые параметры, нормализация до присваивания полям, валидация | A2 / C1 | R01,R06; N05,N06; не писать `this.name = ...` |
| R-08 | P0 | Почему две записи с равными по содержимому `int[]` могут быть не равны | A1,A3,A6 / C1 | R07=false, R08=true, R09=true; не выдавать `Arrays.equals` за record equality |
| R-09 | P0 | Почему нельзя добавить ещё instance field или instance initializer | A1 / C1 | N02,N03; статические поля допустимы (R15) |
| R-10 | P0 | Generic record и `implements` интерфейса допустимы, primitive type argument — нет | A1, A7 / C1 | R12, N15; generic inference подробно в [generics](../../knowledge/01-java-core/generics.md) |
| R-11 | P0 | Можно ли добавить обычный метод/overload constructor и каким образом должен делегировать дополнительный constructor | A1,A2 / C1 | R05,R16,N12,N16 |
| R-12 | P0 | Значения `double` в generated equals: NaN и signed zero; сравнение как wrapper `compare` | A1,A3 / C1 | R10=true, R11=false; не заменять на примитивное `==` |
| R-13 | P0 | Может ли компонент быть `null`; когда валидатор сам запрещает null | A1,A2,A4 / C1 | R14=true для GenericBox, constructor `Person` отсекает null; no “record автоматически non-null” |
| R-14 | P0 | Ограничения имён компонентов, дубликаты и правильный тип accessor | A1 / C1 | N09,N13,N14; не путать с setter |
| I-01 | P0 | Что значит наблюдаемая неизменяемость объекта, где проходит внешняя граница | A4,A6,A7 / C2,C3 | I01/I02: состояние source меняет Unsafe, но не Safe; mutable graph отдельно |
| I-02 | P0 | `final` поле фиксирует значение ссылки, а не запрещает менять объект по ссылке | A7,A6 / C2,C3 | I01, [types](../../knowledge/01-java-core/types.md); `CON-JMM` только граница |
| I-03 | P0 | Утечка alias в конструкторе (input) mutable List/array | A4,A6 / C2 | I01,I06 (контраст); [parameters](../../knowledge/01-java-core/parameters.md) |
| I-04 | P0 | Утечка alias через accessor/getter (output) | A6,A7 / C2 | I06 (изменение выданного массива не изменяет Numbers); нельзя возвращать внутренний массив |
| I-05 | P0 | `List.copyOf` snapshot структуры и порядка vs later mutations source | A4 / C2 | I02=[A], I01=[A,B]; mutable elements не копируются глубоко |
| I-06 | P0 | `Collections.unmodifiableList` — live view, не snapshot | A5 / C2,C3 | I03=[A,B], вход source изменён; backing alias остаётся |
| I-07 | P0 | Почему unmodifiable list отвергает mutator, но mutable элементы внутри всё ещё меняются | A4,A6 / C2 | I04=UOE, I07=67; `UnsupportedOperationException` — runtime |
| I-08 | P0 | Почему `List.copyOf` нельзя применять к коллекции с null и почему null у record допустим | A4,A1 / C2 | I05=NPE, R14=true; не путать источники исключений |
| I-09 | P0 | Защитное копирование массива на входе и выходе; `clone` и `Arrays.copyOf` — не рекурсивная deep copy | A6 / C2 | I06=67, I09=[1,0,0] при удлинении; массивы подробно в [arrays](../../knowledge/01-java-core/arrays.md) |
| I-10 | P0 | Валидация инварианта в constructor до публикации ссылки/состояния | A1,A6,A7 / C2 | R06=age, I10=empty; не считать конструкцию с нарушенным invariant допустимой |
| I-11 | P0 | Почему `record` ≠ глубокая immutable; что меняет `List.copyOf` у List<String> | A1,A3,A4 / C1,C2 | I01/I02; для элементов mutable см. I07 |
| I-12 | P0 | Когда выбрать `record`, а когда обычный `final class` | A1,A3,A6 / C1,C2 | `Safe` vs `Numbers`: arrays требуют контролируемых output copies; record copy invariant см. P1 R-15 |

**P0 acceptance для будущих статей:** каждый вопрос имеет собственное ясное объяснение/контрпример, а не только ссылку на таблицу; при отсутствующей зависимости — минимальный текст прямо на месте и явный пробел в COVERAGE после публикации.

### Дополнительные вопросы P1 и границы P2

| ID | P | Вопрос | Основание / требование |
|---|---|---|---|
| R-15 | P1 | Почему переопределённый accessor с копией массива ломает `Record` copy invariant при стандартном `equals` | A3, R13=false; **не рекомендовать** `BadAccessor` как корректный immutable record |
| R-16 | P1 | Explicit canonical constructor vs compact, публичность и checked exceptions | A2, N07,N08; синхронизировать сигнатуру |
| R-17 | P1 | Пользовательские `equals/hashCode/toString`: сохранять совместимость контрактов, отказываться от «сравним лишь часть компонент» без анализа | A1,A3; не вводить равенство по mutable hash key без доказательств |
| R-18 | P1 | Static fields/methods, вложенные и local records: `static`-характер и отсутствие enclosing instance | A1; проверить отдельным compact snippet при G2 |
| R-19 | P1 | Аннотации record components и propagation по применимым `@Target` | A1 `8.10.1`; не считать одинаковыми все места появления |
| R-20 | P1 | Varargs record component и non-reifiable type, warning, `@SafeVarargs` условия | A1 `8.10.1`; не делать центральным P0 |
| I-13 | P1 | Deep copy для nested mutable elements: `List.copyOf` недостаточен | A4,A6; I07 и схема графа ссылок |
| I-14 | P1 | Пределы `Collections.unmodifiableList` и identity optimization `List.copyOf` | A4,A5; I08 наблюдение только данного случая, **не обещать новый экземпляр** |
| I-15 | P1 | Equality/hash stability и immutable value key | A3,A6; связь с [equality](../../knowledge/01-java-core/equality.md) |
| I-16 | P1 | Object graph и циклические ссылки, limits of arbitrary deep copying | A6; не обещать универсальный рекурсивный clone |
| R-21 | P2 | Record serialization, deserialization canonical constructor | A1,A3; будущая `CORE-SERIAL`, **не принято как проверенное** |
| R-22 | P2 | Record patterns Java 21 / `instanceof` и switch | JLS 21 patterns; будущая `CORE-PATTERNS` |
| R-23 | P2 | Runtime reflection `getRecordComponents` и class-file representation | `CORE-REFLECTION` / JVM; без bytecode догадок |
| I-17 | P2 | Final-field semantics, safe publication, escaped this / happens-before | A7; будущая `CON-JMM` |
| I-18 | P2 | Record DTO, ORM/JPA constraints, JavaBeans, framework integration | только после отдельной проверки библиотек |
| I-19 | P2 | Performance, allocation, memory, GC, JIT, version-specific implementation | измерения и отдельный JVM-план, никаких обещаний в G1 |

**Отдельный нормативный узел R-15:** `java.lang.Record` требует: при реконструкции `R copy = new R(r.c1(), …, r.cn())` должно быть `r.equals(copy)`. Если accessor компонента массива возвращает `clone`, а `equals` остаётся генерируемым (у массивов reference equality), reconstructed record нарушит инвариант. Нельзя «исправлять» immutable record-массив только изменением accessor; придётся проектировать согласованные `equals/hashCode` и copy invariant либо выбрать обычный `final class`, либо изменить модель компонент на подходящие устойчивые значения. Вывод получен из A3 и R13, а не из догадки о байткоде.

## 5. Проверенные полные positive-программы (НЕ файлы репозитория)

Это рабочие **тексты исследования в Markdown**, не реализация G2. Каждую программу сохранять в одноимённый `.java` и запускать независимо. Оба исходника компилируются одновременно с `javac --release 21 -Xlint:all RecordsProbe.java ImmutableProbe.java`; реально выполнены `java RecordsProbe` и `java ImmutableProbe` на OpenJDK 21.0.12.1.

### 5.1 RecordsProbe.java

~~~java
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RecordsProbe {
    record Person(String name, int age) {
        static final String KIND = "person";
        Person(String name) { this(name, 0); }
        Person {
            name = Objects.requireNonNull(name).strip();
            if (age < 0) throw new IllegalArgumentException("age");
        }
        Person withName(String value) { return new Person(value, age); }
    }
    record ArrayBox(int[] values) {}
    record DoubleBox(double value) {}
    record GenericBox<T>(T value) implements java.io.Serializable {}
    record BadAccessor(int[] values) {
        @Override public int[] values() { return values.clone(); }
    }
    public static void main(String[] args) {
        Person p = new Person(" Ada ", 67);
        Person q = new Person("Ada", 67);
        System.out.println("R01=" + p.name() + ":" + p.age());
        System.out.println("R02=" + p.equals(q));
        System.out.println("R03=" + (p.hashCode() == q.hashCode()));
        System.out.println("R04=" + p.toString().contains("name=Ada"));
        System.out.println("R05=" + p.withName("Bob").name());
        try { new Person("X", -1); } catch (IllegalArgumentException ex) { System.out.println("R06=" + ex.getMessage()); }
        int[] first = {1, 2};
        int[] second = {1, 2};
        System.out.println("R07=" + new ArrayBox(first).equals(new ArrayBox(second)));
        System.out.println("R08=" + Arrays.equals(first, second));
        System.out.println("R09=" + new ArrayBox(first).equals(new ArrayBox(first)));
        System.out.println("R10=" + new DoubleBox(Double.NaN).equals(new DoubleBox(Double.NaN)));
        System.out.println("R11=" + new DoubleBox(-0.0).equals(new DoubleBox(+0.0)));
        GenericBox<String> generic = new GenericBox<>("ok");
        System.out.println("R12=" + generic.value());
        BadAccessor b = new BadAccessor(first);
        BadAccessor copy = new BadAccessor(b.values());
        System.out.println("R13=" + b.equals(copy)); // нарушен Record.copy invariant
        System.out.println("R14=" + (new GenericBox<String>(null).value() == null));
        System.out.println("R15=" + Person.KIND);
        System.out.println("R16=" + new Person(" Ed ").age());
    }
}
~~~

**Реальный stdout OpenJDK 21.0.12.1:**

~~~text
R01=Ada:67
R02=true
R03=true
R04=true
R05=Bob
R06=age
R07=false
R08=true
R09=true
R10=true
R11=false
R12=ok
R13=false
R14=true
R15=person
R16=0
~~~

**Разбор граничных строк:** `R07` — одинаковое содержимое **разных** `int[]` не делает записи равными; `R09` — одна и та же ссылка на массив даёт равенство записей в данном примере; `R10/R11` — правило wrapper compare, особенно NaN и ±0.0; `R13` — намеренно **плохой accessor** и нарушение copy invariant; `R04` — лишь проверенное свойство формата этого выполнения, не утверждение обо всех JDK; точный `hashCode` не печатаем как гарантированное число.

### 5.2 ImmutableProbe.java

~~~java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ImmutableProbe {
    record Unsafe(List<String> names) {}
    record Safe(List<String> names) {
        Safe { names = List.copyOf(names); }
    }
    static final class Numbers {
        private final int[] data;
        Numbers(int[] data) {
            if (data.length == 0) throw new IllegalArgumentException("empty");
            this.data = data.clone();
        }
        int[] data() { return data.clone(); }
        int first() { return data[0]; }
    }
    static final class MutableElement { int n; MutableElement(int n) { this.n = n; } }
    public static void main(String[] args) {
        List<String> source = new ArrayList<>(List.of("A"));
        Unsafe unsafe = new Unsafe(source);
        Safe safe = new Safe(source);
        List<String> view = Collections.unmodifiableList(source);
        source.add("B");
        System.out.println("I01=" + unsafe.names());
        System.out.println("I02=" + safe.names());
        System.out.println("I03=" + view);
        try { safe.names().add("C"); } catch (UnsupportedOperationException e) { System.out.println("I04=UOE"); }
        try { List.copyOf(Arrays.asList("A", null)); } catch (NullPointerException e) { System.out.println("I05=NPE"); }
        int[] input = {67};
        Numbers numbers = new Numbers(input);
        input[0] = 1;
        numbers.data()[0] = 2;
        System.out.println("I06=" + numbers.first());
        MutableElement element = new MutableElement(1);
        List<MutableElement> shallow = List.copyOf(List.of(element));
        element.n = 67;
        System.out.println("I07=" + shallow.get(0).n);
        System.out.println("I08=" + (source == safe.names())); // наблюдение, не универсальная гарантия identity
        System.out.println("I09=" + Arrays.toString(Arrays.copyOf(new int[]{1}, 3)));
        try { new Numbers(new int[0]); } catch (IllegalArgumentException e) { System.out.println("I10=" + e.getMessage()); }
    }
}
~~~

**Реальный stdout OpenJDK 21.0.12.1:**

~~~text
I01=[A, B]
I02=[A]
I03=[A, B]
I04=UOE
I05=NPE
I06=67
I07=67
I08=false
I09=[1, 0, 0]
I10=empty
~~~

**Разбор:** в `I01` опасен alias; в `I02` структурный snapshot; `I03` читает изменённую исходную коллекцию; `I04` и `I05` — разные **runtime** исключения, не ошибки компиляции; `I06` — copy-in + copy-out; `I07` — мутируемый элемент меняется и в unmodifiable list. `I08` **не доказывает**, что каждый вызов `List.copyOf` обязан создавать новый объект. Контракт `List.copyOf` не требует новой identity, а допускает переиспользование подходящего unmodifiable объекта.

## 6. Negative compile-пробы: отдельные завершённые единицы

Для каждой строки создать **отдельный** файл с указанным именем и указанным полным source (не объединять строки в одну компиляционную единицу). Команда: `javac --release 21 -XDrawDiagnostics Nxx_Name.java`. Выполнено на OpenJDK 21.0.12.1: **все 16 команд завершились с ненулевым кодом**, а первая ключевая диагностика соответствует указанной категории. Поскольку каждый source самодостаточен, ошибки не объясняются отсутствием сторонней зависимости или произвольной опечаткой. `-XDrawDiagnostics` выдаёт ключи `compiler.err.*`; формат текста и позиции могут отличаться между сборками JDK.

| N | Полный source самостоятельного файла | Почему ошибка, проверенный diagnostic key |
|---|---|---|
| N01 — `N01_Extends.java` | `record N01_Extends(int n) extends Object {}` | В grammar record declaration **нет `extends`**; ожидаемая синтаксическая диагностика `compiler.err.expected: '{'` именно в месте запрещённого `extends`. Это намеренный синтаксический контрпример, не случайный parser failure |
| N02 — `N02_InstanceField.java` | `record N02_InstanceField(int n) { int extra = 3; }` | `compiler.err.record.cannot.declare.instance.fields`; instance field вне компонентов запрещён |
| N03 — `N03_Initializer.java` | `record N03_Initializer(int n) { { System.out.println("init"); } }` | `compiler.err.instance.initializer.not.allowed.in.records` |
| N04 — `N04_FinalWrite.java` | `record N04_FinalWrite(int n) { void set() { this.n = 67; } }` | `compiler.err.cant.assign.val.to.var: final` |
| N05 — `N05_CompactField.java` | `record N05_CompactField(int n) { N05_CompactField { this.n = 67; } }` | `compiler.err.cant.assign.val.to.var: final`; нормализовать надо **параметр** `n`, не field |
| N06 — `N06_CompactReturn.java` | `record N06_CompactReturn(int n) { N06_CompactReturn { return; } }` | `compiler.err.invalid.canonical.constructor.in.record` / `canonical.cant.have.return.statement` |
| N07 — `N07_CanonicalName.java` | `record N07_CanonicalName(int n) { N07_CanonicalName(int other) { this.n = other; } }` | `compiler.err.invalid.canonical.constructor.in.record` / `canonical.with.name.mismatch` |
| N08 — `N08_PrivateCanonical.java` | `record N08_PrivateCanonical(int n) { private N08_PrivateCanonical(int n) { this.n = n; } }` | `compiler.err.invalid.canonical.constructor.in.record` / `canonical.must.not.have.stronger.access` (package-level record) |
| N09 — `N09_DuplicateComponent.java` | `record N09_DuplicateComponent(int n, int n) {}` | `compiler.err.already.defined: kindname.record.component` |
| N10 — `N10_AbstractRecord.java` | `abstract record N10_AbstractRecord(int n) {}` | `compiler.err.mod.not.allowed.here: abstract` |
| N11 — `N11_ExtendRecord.java` | `record R11(int n) {} class N11_ExtendRecord extends R11 { N11_ExtendRecord() { super(1); } }` | `compiler.err.cant.inherit.from.final: R11` |
| N12 — `N12_OverloadNoDelegate.java` | `record N12_OverloadNoDelegate(int n) { N12_OverloadNoDelegate() { System.out.println("hello"); } }` | `compiler.err.first.statement.must.be.call.to.another.constructor` |
| N13 — `N13_AccessorType.java` | `record N13_AccessorType(int n) { public long n() { return n; } }` | `compiler.err.invalid.accessor.method.in.record` / `accessor.return.type.doesnt.match` |
| N14 — `N14_ReservedComponent.java` | `record N14_ReservedComponent(int toString) {}` | `compiler.err.illegal.record.component.name` |
| N15 — `N15_GenericPrimitive.java` | `record R15<T>(T value) {} class N15_GenericPrimitive { R15<int> invalid; }` | `compiler.err.type.found.req` / `type.req.ref` — generic argument должен быть ссылочным типом |
| N16 — `N16_NonCanonicalNoFields.java` | `record N16_NonCanonicalNoFields(int n) { N16_NonCanonicalNoFields(int n, boolean flag) { this.n = n; } }` | `compiler.err.first.statement.must.be.call.to.another.constructor`; non-canonical overload обязан делегировать canonical |

**Дисциплина отрицательных проб:** N01 — единственный заранее задуманный *grammar* refusal, и его позиция проверена; N02–N16 — структурно завершённые компиляционные единицы, ожидаемые семантические/контрактные ошибки. `R06`/`I04`/`I05`/`I10` проверяют **runtime**, их нельзя включать в счёт «negative javac». Все `Nxx` проверяются **отдельно от проекта**; Java snippets внутри Markdown **не компилируются автоматически** действующей CI.

### Воспроизводимость без загрязнения репозитория

~~~sh
java -version
javac -version
javac --release 21 -Xlint:all RecordsProbe.java ImmutableProbe.java
java RecordsProbe
java ImmutableProbe
# Для каждого отдельного Nxx: ожидается ненулевой exit status и релевантный key
javac --release 21 -XDrawDiagnostics N02_InstanceField.java
~~~

Фактически выполнено: компиляция двух positive с `--release 21 -Xlint:all` (exit 0, предупреждений не было), оба запуска и 16/16 изолированных negative с `--release 21 -XDrawDiagnostics` (exit non-zero с релевантной диагностикой). **Not run:** Gradle/JUnit CI проекта для этих Markdown snippets, реальный Obsidian/IntelliJ preview, другие JDK/вендоры/OS, JMH/GC/JIT, reflection, serialization, concurrency/JMM и property-based тесты. Ни одного CI SUCCESS проекту не приписываем.

## 7. Границы ошибок и рекомендации для G2/G3/G4

**Три наиболее опасные неправильные формулировки:**
1. «Record неизменяемый» — некорректно без уточнения: его component **fields** final, но компоненты могут указывать на mutable array/list; к тому же пользовательские методы могут менять достижимые объекты.
2. «`List.copyOf` создаёт глубокую копию / всегда новый объект» — неверно: snapshot *структурных ссылок*, mutable elements сохраняются; API допускает переиспользование подходящего unmodifiable списка. `Collections.unmodifiableList`, напротив, возвращает live view на исходную коллекцию.
3. «Для record с массивом достаточно вернуть `clone` из accessor» — нарушает `Record` copy invariant при сгенерированном equals; см. R13. Если требуется массив как внутреннее состояние с контролируемым copy-out, обычный final class обычно проще и безопаснее.

**Контроль текста 02:** сначала определить наблюдаемую неизменяемость и прозрачность record; на каждом примере назвать владельца изменяемого массива/списка и все ссылки; показать порядок событий «создать исходный → передать ссылку → изменить исходный → прочитать»; явно различать compile error/runtime exception/договорённость класса. Не полагаться на метафору «record = data class с deep immutability». Указывать Java 21 и тип гарантии.

**Контроль примеров 03:** оставить два независимых запуска, ожидаемые stdout 16+10 строк; `R13` показать как **антипример**, а не рекомендуемый код. Не добавлять обширную инфраструктуру ради 26 строк. Согласовать с 02 потребность в полноценном runnable и отдельной `verify`; при решении не делать standalone examples сохранить короткие компилируемые блоки в статьях. Не выдавать N01–N16 за CI, если negative не были интегрированы в систему проверок.

**Контроль G3/G4:** понятный русский, ссылки на существующие файлы и будущие ID без фиктивных ссылок; независимая сверка A1/A3 о copy invariant, A4/A5 о snapshot/view, actual compile diagnostics на точном PR HEAD. Положительные stdout и Nxx не являются доказательством корректной потоковой публикации. Изменение существенной части плана после review требует нового review HEAD.

## 8. Статусы, ссылки и следующий gate

| Объект | Статус в G1 |
|---|---|
| `CORE-RECORDS` / `CORE-IMMUTABLE` | `planned` в каталоге; **не** самостоятельные Implemented |
| Исследовательская матрица и фрагменты этого файла | `G1 PROPOSED`; независимое G1-review ещё не выполнено |
| Два positive examples | `verified locally` на указанном JDK и командах, **не** проектной CI |
| 16 isolated compile-negative | `verified locally` на указанном JDK и командах, **не** проектной CI |
| Нормативные и C источники | адресно проверены в Java SE 21; C не гарантирует полноту |
| Новые статьи, их CI и навигация | `not implemented / not run`, только после принятия G1 |
| Отдельные JMM/Reflection/Serialization/Collections | `out of scope`; не повышены до Verified |

Существующие относительные ссылки в ``2–4` адресуют пути из текущего `main` (сверены fetch соответствующих файлов, `CURRICULUM` и `COVERAGE`); новых ссылок на ещё отсутствующие `records.md`/`immutability.md` нет. В рамках этого исследовательского PR **не** редактировать существующие статьи, `COVERAGE`/`CURRICULUM`, Gradle/CI и Java исходники.

### Передача эстафеты

Кому: **04 — Independent Reviewer**.
Что сделать: независимое **G1-review** настоящего плана на точном **HEAD исследовательского PR**; проверить JLS/API Java 21, каждую P0-строку, copy invariant R-15, distinction snapshot/view, 16 negative diagnostics и границы фактического тестирования. Вердикт `PASS` либо конкретные `CHANGES`; после PASS вернуть **00** для `G1 ACCEPTED/BLOCKED` и решения о merge, **не** начинать G2 до решения 00.
Передать: оператор пересылает прямую ссылку на **отдельный опубликованный адресный комментарий** с PR number и полным HEAD SHA, не ссылку на текущий документ вместо комментария.
