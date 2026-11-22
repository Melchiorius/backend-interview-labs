# CORE-RECORDS — record в Java 21: компоненты, конструкторы, равенство и границы гарантий

**Область:** Java SE 21, без preview. Здесь разбирается **языковой контракт record**, а не «быстрый способ сделать любой объект неизменяемым». Начальные зависимости: [классы и конструкторы](classes.md), [передача ссылок](parameters.md), [равенство и hashCode](equality.md); затем прочитайте самостоятельную статью [CORE-IMMUTABLE — владение изменяемым состоянием](immutability.md). Для массивов и generics предусмотрены [CORE-ARRAYS](arrays.md) и [CORE-GENERICS](generics.md).

**Исследовательская база:** [принятый G1-план CORE-RECORDS + CORE-IMMUTABLE](../../docs/plans/CORE-RECORDS-IMMUTABLE.md), матрица **R-01–R-23: 14 P0 / 6 P1 / 3 P2**. Нормативный источник — [JLS Java 21 §8.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10) и [java.lang.Record](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Record.html). Примеры ниже — отдельные демонстрационные фрагменты; если не указано обратное, их не следует склеивать в один файл и считать результатом одного запуска. Для воспроизведения основных механизмов используйте [общий Java 21 runnable Records + Immutable](../../examples/core/records-immutability/README.md): два класса `RecordsProbe.java` (R01–R16) и `ImmutableProbe.java` (I01–I10), два независимых запуска и два точных эталона stdout. Это **26 подписанных наблюдений двух процессов**, не 26 отдельных unit-тестов.

## 1. Что record обещает и зачем он нужен (R-01, R-02, R-13)

Обычный класс сам решает, какие поля хранить, какие методы предоставлять и что считать равенством. **Record** заявляет, что основное состояние описывается *компонентами* в заголовке, а доступ к этому состоянию прозрачен: например, `record Point(int x, int y) {}` объявляет тип с двумя компонентами типа `int`. Record — особый вид **класса**, не новый примитивный тип, не интерфейс и не кортеж JVM.

Компилятор обеспечивает для каждого компонента неявное `private final` поле, публичный accessor с именем компонента и соответствующим возвращаемым типом, а также стандартные методы `equals`, `hashCode` и `toString`, если их не определили явно. Вызов `point.x()` — обращение к accessor, а не к публичному полю и не к `getX()`. Запись `point.x = 67` снаружи невозможна; менять поле компонента после присваивания также нельзя.

~~~java
record Point(int x, int y) {}

class RecordBasics {
    public static void main(String[] args) {
        Point point = new Point(67, 2);
        System.out.println(point.x());                // 67
        System.out.println(point.y());                // 2
        System.out.println(point instanceof Record);  // true
        System.out.println(point.getClass().isRecord()); // true
    }
}
~~~

Record **неявно `final`**, прямо наследует `java.lang.Record` и не может расширять произвольный класс. При этом он может реализовать интерфейсы; методы интерфейса остаются обычным Java-контрактом. Ни `record extends SomeClass`, ни `abstract record` не являются допустимым способом объявить record — это compile-time ошибки, а не особое поведение JVM.

**Неизменяемость не следует автоматически.** `final` поле компонента сохраняет **ссылку**, но не запрещает изменение объекта, на который она указывает. `record Scores(int[] values) {}` разрешён: `values()` вернёт массив, элементы которого можно изменить. У record нет встроенного требования «все компоненты non-null»: `record Holder<T>(T value) {}` допускает `new Holder<String>(null)`, если наш конструктор не запретил это. Поэтому record и [наблюдаемая неизменяемость](immutability.md) — разные понятия.

## 2. Компоненты и поля: что можно добавить в тело record (R-02, R-09, R-14)

Заголовок record описывает **полный набор его полей экземпляра**, которые несут состояние компонентов. Нельзя дописать ещё одно не-`static` поле, объявить instance initializer или обычным методом заново присвоить компоненту. Однако статические константы и методы допустимы, равно как обычные методы экземпляра, использующие уже объявленные компоненты.

~~~java
record Temperature(int celsius) {
    static final String UNIT = "°C";     // статическое поле допустимо

    int fahrenheit() {
        return celsius * 9 / 5 + 32;
    }
    // int extra = 67;                  // compile error: другое instance field
    // { System.out.println("init"); } // compile error: instance initializer
    // void set(int v) { this.celsius = v; } // compile error: final field
}
~~~

**Имена компонентов тоже ограничены.** Кроме обычных правил идентификаторов, для некоторых имён вроде `toString` есть специальный запрет: `record Broken(int toString) {}` некорректен. Два компонента с одинаковым именем также недопустимы. Явный accessor должен иметь **точное имя и возвращаемый тип** компонента, быть публичным и не пытаться подменить поле setter'ом: для `record Value(int n)` нельзя объявить `public long n()`. Это проверяется компилятором, не откладывается до runtime.

**Чем это полезно:** класс с существенным дополнительным скрытым состоянием нельзя механически «переделать в record» без изменения модели. Выбирайте обычный класс, если у объекта больше собственного состояния, чем допускает контракт компонентов, или если вам необходимы специальные правила инкапсуляции.

## 3. Canonical и compact constructors: создание корректного значения (R-06, R-07, R-16)

У каждого record есть **canonical constructor**: по одному параметру для каждого компонента, с совместимыми именами и типами. Когда мы его не пишем, компилятор предоставляет неявный конструктор. Если нужно валидировать параметры или нормализовать их, конструктор задают явно.

**Compact constructor** — сокращённая запись canonical constructor без списка параметров. Параметры доступны по именам компонентов; в теле **нормализуют параметр**, а после успешного выполнения тела компилятор обеспечивает неявное присваивание полям. Вручную писать `this.name = ...` внутри compact constructor **запрещено**.

~~~java
import java.util.Objects;

record Person(String name, int age) {
    Person {
        name = Objects.requireNonNull(name, "name").strip();
        if (name.isBlank()) {
            throw new IllegalArgumentException("blank name");
        }
        if (age < 0) {
            throw new IllegalArgumentException("age");
        }
    }

    Person(String name) {
        this(name, 0);       // дополнительный конструктор делегирует canonical
    }

    Person withAge(int newAge) {
        return new Person(name, newAge); // новое значение; поля этого не меняются
    }
}

class RecordConstructorDemo {
    public static void main(String[] args) {
        Person p = new Person(" Ada ", 67);
        System.out.println(p.name());          // Ada
        System.out.println(p.withAge(2).age()); // 2
        System.out.println(p.age());           // 67
        try {
            new Person("X", -1);
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage()); // age
        }
    }
}
~~~

**Последовательность:** вызывающий передаёт значение ссылки на строку и целое число; тело compact constructor проверяет и при необходимости заменяет значение *параметра* `name`, затем в финальные поля попадают нормализованные значения. Если проверка выбросила исключение, экземпляр не становится успешно сконструированным значением. Это не обещание, что произвольный конструктор с утечкой `this` безопасен для многопоточности.

При **явном canonical constructor** список параметров пишется полностью, а присваивание полям выполняется в нём обычным образом:

~~~java
record Ratio(int numerator, int denominator) {
    public Ratio(int numerator, int denominator) {
        if (denominator == 0) throw new IllegalArgumentException("zero denominator");
        this.numerator = numerator;
        this.denominator = denominator;
    }
}
~~~

Уровень доступности canonical constructor должен соответствовать требованиям record; он не может быть *менее доступным*, чем сам record. Например, объявить `private` canonical constructor для доступного на уровне пакета record запрещено. У canonical constructor есть и другие ограничения JLS: нельзя просто добавить произвольный checked `throws`, несовпадающее имя параметра или явный `return` из compact constructor. **Дополнительный, не-canonical конструктор** обязан в качестве первого действия делегировать другому конструктору этого record через `this(...)`; напрямую заполнять component fields в таком конструкторе нельзя.

**Частая ошибка на интервью:** «compact constructor автоматически делает record immutable» — нет. Он позволяет в одном месте проверять и нормализовать параметры, но если параметр является изменяемым объектом, нужно отдельно продумать владение и копирование.

## 4. Generated equals, hashCode и toString (R-03, R-04, R-05, R-08, R-12)

Генерируемый `equals` сравнивает **соответствующие значения компонентов**, а не только `this == other`. Два разных record-объекта с равными компонентами могут быть равны. Значение `hashCode` согласовано с этим равенством: у двух равных записей одинаковый hash; **точную числовую формулу и число не следует заучивать**. `toString` содержит имя record и представление компонентов; его подробный формат — **не протокол сериализации** и не основа для парсинга бизнес-сообщений.

~~~java
record Coordinate(int x, String label) {}

class RecordEquality {
    public static void main(String[] args) {
        Coordinate a = new Coordinate(67, "east");
        Coordinate b = new Coordinate(67, new String("east"));

        System.out.println(a == b);                // false: разные экземпляры
        System.out.println(a.equals(b));           // true: равные компоненты
        System.out.println(a.hashCode() == b.hashCode()); // true: обязательный контракт
        System.out.println(a.toString().contains("label=east")); // true в этом примере/JDK
    }
}
~~~

**Граница:** используйте `equals` для содержательного сравнения записей, но не пытайтесь восстановить значения из произвольного вывода `toString`. Если ключом карты является record с изменяемыми компонентами, сначала проверьте стабильность равенства и хэша; сама запись не создаёт магической защиты от внешних мутаций.

### 4.1 Массив как компонент: равное содержимое не означает равенство записей (R-08)

У массива `equals` унаследован от `Object`, поэтому сравнение двух разных массивов обычно основано на идентичности, а не на элементах. Генерируемый record `equals` использует **равенство соответствующих компонентных значений по их обычному контракту**, то есть не вызывает автоматически `Arrays.equals` или `Arrays.deepEquals`.

~~~java
import java.util.Arrays;

record Scores(int[] values) {}

class RecordArrayEquality {
    public static void main(String[] args) {
        int[] first = {1, 2};
        int[] second = {1, 2};

        System.out.println(Arrays.equals(first, second)); // true: элементы совпадают
        System.out.println(new Scores(first).equals(new Scores(second))); // false
        System.out.println(new Scores(first).equals(new Scores(first)));  // true
        new Scores(first).values()[0] = 67;
        System.out.println(first[0]); // 67: accessor вернул тот же массив
    }
}
~~~

Последняя строка показывает **aliasing**, а не баг в Java: caller и record держат ссылки на один массив. Более подробно: [массивы, shallow copies и reference equality](arrays.md), [границы defensive copying](immutability.md).

### 4.2 double, NaN и знаковый ноль (R-12)

Для `double` стандартное сравнение в генерируемом `equals` ведёт себя **не как примитивное `==`**, а согласно семантике wrapper equality/`Double.compare`: соответствующие NaN считаются равными, а `-0.0` и `+0.0` — различными. Это нужно для согласованности сравнения и хэшей.

~~~java
record Measurement(double reading) {}

class RecordDoubles {
    public static void main(String[] args) {
        System.out.println(new Measurement(Double.NaN)
                .equals(new Measurement(Double.NaN))); // true
        System.out.println(new Measurement(-0.0)
                .equals(new Measurement(+0.0)));       // false
        System.out.println(Double.NaN == Double.NaN);  // false: примитивное ==
        System.out.println(-0.0 == +0.0);              // true: примитивное ==
    }
}
~~~

**Что объяснить на собеседовании:** если кандидат выводит поведение record только из правил `==` над primitive double, он ошибётся именно на NaN и signed zero. Не переносите проверенную семантику `equals` на округление, математическое равенство вещественных измерений или предсказание точного `hashCode`.

## 5. Record copy invariant и опасный accessor с clone (R-15)

Это наиболее важная ловушка. [Контракт java.lang.Record](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Record.html) формулирует ожидаемое свойство копирования: если получить компоненты через accessors и собрать новый экземпляр того же record, то результат должен сравниваться равным исходному. Схематически: `R copy = new R(original.c1(), ..., original.cn()); original.equals(copy)` должно быть `true`.

Попытка «сделать record неизменяемым» через **одно лишь копирование массива в accessor** ломает это свойство при генерируемом равенстве:

~~~java
record BadAccessor(int[] values) {
    @Override
    public int[] values() {
        return values.clone(); // АНТИПРИМЕР: отдельно меняет семантику accessor
    }
}

class BrokenRecordCopy {
    public static void main(String[] args) {
        int[] source = {67};
        BadAccessor original = new BadAccessor(source);
        BadAccessor copy = new BadAccessor(original.values());

        System.out.println(original.values() == source); // false: выдан новый массив
        System.out.println(original.equals(copy));       // false: R13 в G1
    }
}
~~~

**Почему возникает `false`:** оригинал хранит ссылку `source`; accessor выдал новый объект массива с теми же элементами; canonical constructor новой записи сохранил **новую ссылку**. Генерируемый `equals` сравнивает эти массивы по ссылочной идентичности, поэтому копия не равна оригиналу. Вдобавок исходный массив в конструктор передан без copy-in, так что внешняя ссылка всё ещё может менять состояние original.

Это **нарушение record copy invariant**, а не доказательство, что `clone()` запрещён или что массивы нельзя копировать. **Не советуйте BadAccessor как правильный immutable record.** Для корректной модели с array-компонентом придётся одновременно согласовать защиту входов и выходов, семантику `equals/hashCode` и реконструкцию компонентов; часто проще использовать обычный [`final class` с private array и двумя копиями](immutability.md). Альтернатива — хранить в качестве компонента устойчивый value-type, например snapshot `List<String>`, осознавая, что это *не deep copy* mutable elements.

## 6. Generic records, методы, интерфейсы и вложенность (R-10, R-11, R-18)

Record может иметь типовые параметры, реализовывать интерфейс, объявлять методы, `static` константы и методы, а также дополнительный конструктор с обязательной делегацией canonical.

~~~java
interface HasValue<T> { T value(); }

record Box<T>(T value) implements HasValue<T> {
    static String kind() { return "box"; }
    boolean hasValue() { return value != null; }
}

class RecordsInContainer {
    static record Nested(int value) {}

    void demo() {
        record Local(int value) {}  // local record не захватывает экземпляр enclosing class
        System.out.println(new Local(67).value()); // 67
        System.out.println(Box.kind());             // box
        Box<String> box = new Box<>("ok");
        System.out.println(box.hasValue());         // true
        System.out.println(new Nested(3).value());  // 3
    }
}
~~~

Вложенный и локальный record не получают скрытую ссылку на экземпляр внешнего класса: они **неявно static по своей природе**. Запись `record Box<T>(T value)` корректна, а `Box<int>` — **compile-time error**: generic argument должен быть ссылочным типом (вместо primitive может понадобиться `Integer`). Подробно — в [generics и invariance](generics.md).

**Методы не делают record автоматически mutable или immutable:** пользовательский метод может посчитать новое значение, вернуть новый record, обратиться к внешнему ресурсу или поменять достижимый через компонент объект. Оценивайте конкретное поведение, а не наличие ключевого слова `record`.

## 7. Аннотации компонентов и varargs (R-19, R-20 · P1)

Аннотация на компоненте record может иметь разные допустимые точки применения: компонент/поле, параметр canonical constructor, accessor или type-use — **в зависимости от `@Target` самой аннотации**. Нельзя обещать, что любая аннотация копируется на все перечисленные места независимо от её целей.

~~~java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.METHOD})
@interface Audited {}

record Packet(@Audited String payload) {}
~~~

Record-компонент может использовать varargs как **последний компонент**, например `record Words(String... tokens) {}`. Такой компонент представлен массивом, поэтому вновь возникают правила array identity и mutable aliasing. С varargs не-reifiable обобщённых элементов возможны предупреждения о heap pollution; правила `@SafeVarargs` и безопасности доступа обсуждаются в [CORE-ARRAYS](arrays.md) и [CORE-ERASURE](erasure.md). Пометка `@SafeVarargs` **не** превращает небезопасный код в безопасный; нельзя автоматически считать всякую record-конструкцию с varargs свободной от проблем.

## 8. Ошибка компиляции, ошибка выполнения или ошибка проектирования? (R-09, R-14, R-16)

| Случай | Тип результата | Почему |
|---|---|---|
| `record X(int n) extends Object {}` | **compile error** | В grammar record нет `extends` |
| `record X(int n) { int extra = 1; }` | **compile error** | Постороннее instance field |
| `record X(int n) { X { this.n = 67; } }` | **compile error** | Нельзя присваивать компонентному final field в compact constructor |
| `record X(int n) { public long n() { return n; } }` | **compile error** | Возвращаемый тип accessor не совпадает с типом компонента |
| `record X(int n) { X() { System.out.println(67); } }` | **compile error** | Non-canonical constructor не делегирует другому constructor |
| `new Person("X", -1)` из §3 | **runtime IllegalArgumentException** | Проверка собственного инварианта |
| `new Scores(new int[]{67}).values()[0] = 1` | Компилируется и меняет массив | Нет автоматической deep immutability |
| `new BadAccessor(a).equals(new BadAccessor(a.clone()))` | Компилируется, даёт `false` | Ошибка проектирования: нарушен copy invariant |
| `Box<int>` | **compile error** | Primitive как generic type argument |

G1 §6 содержит **N01–N16 — шестнадцать изолированных compile-negative исходников**, проверенных исследователями по ожидаемым javac diagnostic keys. N01 намеренно проверяет запрещённую *грамматику* record `extends`; это не случайно недописанная скобка. Runtime-исключения нашего конструктора и изменяемость массива **не** считаются ошибками компиляции и не входят в этот счёт. Новые фрагменты из этой статьи не становятся автоматически проверенными в CI только потому, что G1 проверял другой код.

## 9. Когда использовать record, а когда нужен обычный класс

**Record хорош**, если публичная модель — прозрачный набор компонентных значений, компоненты имеют понятную семантику сравнения, а invariants выразимы в canonical/compact constructor без дополнительного instance state. Например, `record Point(int x, int y)` или validated `record Person(String name, int age)` естественно использовать как данные.

**Обычный `final class` часто лучше**, если требуется скрыть массив и всегда возвращать его копию, вести внутреннее состояние, определить сложный invariant/identity или изменить состав публичной модели без раскрытия полей-компонентов. Record **может** содержать snapshot структурно immutable списка; но тем самым он не делает mutable elements deeply immutable. Подробный пример проектирования — в [CORE-IMMUTABLE](immutability.md).

## Карта покрытия R-01–R-23

| ID | Приоритет | Где объяснено и какой контрпример |
|---|---|---|
| R-01 | P0 | §1: special final class, `Record`, interface vs superclass |
| R-02 | P0 | §§1–2: components, final fields, public accessor |
| R-03 | P0 | §4: generated equals и разные экземпляры |
| R-04 | P0 | §4: hashCode при равенстве; нет обещания числа |
| R-05 | P0 | §4: generated toString; не сериализация |
| R-06 | P0 | §3: canonical constructor, параметры, доступ |
| R-07 | P0 | §3: compact constructor нормализует параметры |
| R-08 | P0 | §4.1: два равносодержательных разных массива, `false` |
| R-09 | P0 | §2/8: посторонние instance fields/initializers запрещены |
| R-10 | P0 | §6: generic record/implements, primitive type argument |
| R-11 | P0 | §§3/6: методы, дополнительный constructor делегирует |
| R-12 | P0 | §4.2: double NaN/±0.0 |
| R-13 | P0 | §1/3: null разрешён без собственной валидации |
| R-14 | P0 | §§2/8: имена компонент, сигнатуры accessors |
| R-15 | P1 | §5: BadAccessor clone accessor нарушает copy invariant |
| R-16 | P1 | §3: explicit vs compact canonical, видимость/throws |
| R-17 | P1 | §4/5: custom equals/hashCode/toString должны сохранять логический контракт |
| R-18 | P1 | §2/6: static, nested/local records |
| R-19 | P1 | §7: `@Target` и propagation аннотаций |
| R-20 | P1 | §7: varargs/non-reifiable, `@SafeVarargs` boundaries |
| R-21 | P2 | **Граница**: сериализация record и canonical deserialization требуют CORE-SERIAL |
| R-22 | P2 | **Граница статьи о record:** [record patterns / Java21 switch](patterns.md) разбираются в отдельной CORE-PATTERNS, а не подменяются этим разделом |
| R-23 | P2 | **Граница**: reflection `getRecordComponents`/class-file representation — CORE-REFLECTION |

## Нормативные источники и пределы проверки

**Первоисточники:** [JLS 21 §8.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10), [§8.10.1 (components)](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10.1), [§8.10.2 (body)](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10.2), [§8.10.3 (members)](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10.3), [§8.10.4 (constructors)](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10.4), [Record Java21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Record.html), [Objects.equals](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html#equals(java.lang.Object,java.lang.Object)). Интервью-вопросы из [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md) указывают, что стоит уметь объяснять, но **не заменяют Java SE контракт**.

**Принятое G1** содержит полный позитивный `RecordsProbe.java` с 16 подписанными наблюдениями R01–R16 (в том числе намеренно плохое R13=false) и 16 отдельно проверенных negative N01–N16 в §6. Исходные результаты G1 получены исследователями на OpenJDK 21.0.12.1 с `--release 21 -Xlint:all`, но **не означают, что все Java-блоки выше прогнаны автоматически**. [Общий runnable с двумя G1-программами](../../examples/core/records-immutability/README.md) интегрирован из [технического PR #133](https://github.com/Melchiorius/backend-interview-labs/pull/133) в авторскую ветку; исходники и эталоны сверены с G1 §5, `verify.sh` выполняет компиляцию `javac --release 21 -Xlint:all` и два строгих `diff -u`. Роль 03 отдельно проверила опубликованные Git blobs и выполнила локальные запуски на Debian OpenJDK 21.0.12.1 без предупреждений. **CI общего итогового HEAD** и редакционные ворота фиксируются отдельно в [PR #132](https://github.com/Melchiorius/backend-interview-labs/pull/132); это не подтверждение автоматической компиляции всех блоков кода в статье.

**NOT RUN в рамках этой статьи без отдельного свидетельства:** автоматическая компиляция всех inline Java-фрагментов; N01–N16 в действующей CI; альтернативные JDK/OS; GUI Obsidian/IntelliJ; reflection/class-file inspection, record serialization/ORM, record patterns как отдельная тема, deep-copy циклических графов, JMM publication/concurrency stress, performance/JMH/GC/JIT. P2 **не** помечаются как реализованные темы.
