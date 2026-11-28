# CORE-RECORDS — record в Java 21: компоненты, конструкторы, равенство и границы гарантий

**Область:** Java SE 21, без preview. Здесь разбирается **языковой контракт record**, а не «быстрый способ сделать любой объект неизменяемым». Начальные зависимости: [классы и конструкторы](classes.md), [передача ссылок](parameters.md), [равенство и hashCode](equality.md); затем прочитайте самостоятельную статью [CORE-IMMUTABLE — владение изменяемым состоянием](immutability.md). Для массивов и generics предусмотрены [CORE-ARRAYS](arrays.md) и [CORE-GENERICS](generics.md).

**Исследовательская база:** [принятый G1-план CORE-RECORDS + CORE-IMMUTABLE](../../docs/plans/CORE-RECORDS-IMMUTABLE.md), матрица **R-01–R-23: 14 P0 / 6 P1 / 3 P2**. Нормативный источник — [JLS Java 21 §8.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10) и [java.lang.Record](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Record.html). Примеры ниже — отдельные демонстрационные фрагменты; если не указано обратное, их не следует склеивать в один файл и считать результатом одного запуска. Для воспроизведения основных механизмов используйте [общий Java 21 runnable Records + Immutable](../../examples/core/records-immutability/README.md): два класса `RecordsProbe.java` (R01–R16) и `ImmutableProbe.java` (I01–I10), два независимых запуска и два точных эталона stdout. Это **26 подписанных наблюдений двух процессов**, не 26 отдельных unit-тестов.

Record стал постоянной возможностью языка в Java 16; здесь целевая версия остаётся Java 21. Из корня репозитория: `bash examples/core/records-immutability/verify.sh`. Новые отдельные проверки описаны в [полных временных исходниках Probe.java, отрицательных файлах, командах и результатах](https://github.com/Melchiorius/backend-interview-labs/issues/363#issuecomment-7227809467); они не добавлены к общему runner. Номера R-xx карты вопросов и Rxx строк stdout различаются.

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

Допустимы также изменяемые static-поля и static initializers. В фактическом Probe record State(int x) объявляет `static int count; static {count=67;} static void inc(){count++;}`; после inc() счётчик 68, отдельный метод x(int d) возвращает x+d. Это состояние класса, не дополнительное поле экземпляра. Методы abstract и native в теле record запрещены (§8.10.2); отдельные Abstract, Native и ExtraField отклонены. Record не обещает чистых функций, неизменяемого статического состояния или потокобезопасности.

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

**Имена компонентов тоже ограничены.** Кроме обычных правил идентификаторов, запрещены ровно восемь специальных имён: `clone`, `finalize`, `getClass`, `hashCode`, `notify`, `notifyAll`, `toString`, `wait`. Два компонента с одинаковым именем также недопустимы. Например, `record Broken(int toString) {}` некорректен; все восемь запрещённых имён отдельно отклонены компилятором. Имя `equals` в этом списке отсутствует: `record Names(int equals) {}` допустим, а `new Names(67).equals()` возвращает 67.

**Accessor (метод доступа к компоненту)** должен иметь точное имя и объявленный возвращаемый тип компонента. Это публичный (`public`) метод экземпляра без параметров, собственных параметров типа (type parameters) и объявления `throws` (throws clause), даже для unchecked-исключений.

Ковариантный возвращаемый тип здесь не подходит: если компонент объявлен как `Number x`, нельзя написать `Integer x()` вместо `Number x()`. Отдельные примеры `AccessorThrows`, `AccessorGeneric`, `AccessorStatic`, `AccessorPrivate` и `Covariant` отклонены компилятором. Одноимённая перегрузка с параметром допустима: метод `State.x(int)` не является accessor. Все эти ограничения проверяются при компиляции, а не при выполнении программы.

**Чем это полезно:** класс с существенным дополнительным скрытым состоянием нельзя механически «переделать в record» без изменения модели. Выбирайте обычный класс, если у объекта больше собственного состояния, чем допускает контракт компонентов, или если вам необходимы специальные правила инкапсуляции.

## 3. Canonical и compact constructors: создание корректного значения (R-06, R-07, R-16)

У каждого record есть **canonical constructor**: по одному параметру для каждого компонента, с теми же именами и объявленными типами в порядке заголовка. Когда мы его не пишем, компилятор предоставляет неявный конструктор. Если нужно валидировать параметры или нормализовать их, конструктор задают явно.

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

Параметр compact constructor затеняет поле: name — параметр, this.name — поле. До неявных присваиваний прямое чтение this.name запрещено definite assignment (DirectRead отклонён), но вызов instance-метода может наблюдать JVM default value. В фактическом `record Compact(String name){Compact {name=name.strip();System.out.println("compact="+name+","+name());}}` вход " x " дал compact=x,null, после создания c.name().equals("x") подтверждено. Не используйте accessor в теле compact как источник уже нормализованного значения и не выпускайте this для такой проверки. После нормального завершения тела поля получают значения параметров в порядке заголовка. См. [инициализация](initialization.md) и [конструкторы](classes.md).

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

Канонический конструктор не может быть менее доступным, чем сам record. Например, `private` canonical constructor для record с пакетным доступом запрещён. В полной форме канонического конструктора (normal canonical constructor) имена, объявленные типы и порядок параметров должны точно соответствовать компонентам; переменная арность (varargs) соответствующего параметра также должна совпадать.

У канонического конструктора не может быть собственных параметров типа, объявления `throws` (даже unchecked) или явного вызова другого конструктора через `this(...)` либо `super(...)`. Отдельные примеры `BadArity`, `WrongName`, `CanonicalGeneric`, `CanonicalSuper` и `UncheckedThrows` отклонены.

Запрет объявления `throws` не означает запрета выбрасывать непроверяемое исключение из тела. В Probe конструктор `Normal(int x)` при `x < 0` фактически выбросил `IllegalArgumentException` без `throws`; при положительном входе присвоил поле и завершился явным `return;`. Полная форма допускает `return;` после определённого присваивания (definite assignment) всех полей компонентов. В compact constructor оператор `return` запрещён; пример `CompactReturn` отклонён.

Обычный неявный конструктор по умолчанию (default constructor) у record не объявляется: для `NoDefault(int x)` вызов `new NoDefault()` отклонён, тогда как у record без компонентов `Empty()` существует канонический конструктор без параметров. **Дополнительный, неканонический конструктор** обязан первым действием делегировать другому конструктору того же record через `this(...)`; напрямую заполнять поля компонентов в таком конструкторе нельзя.

**Частая ошибка на интервью:** «compact constructor автоматически делает record immutable» — нет. Он позволяет в одном месте проверять и нормализовать параметры, но если параметр является изменяемым объектом, нужно отдельно продумать владение и копирование.

## 4. Generated equals, hashCode и toString (R-03, R-04, R-05, R-08, R-12)

Генерируемый `equals` сравнивает **соответствующие значения компонентов**, а не только `this == other`. Два разных record-объекта с равными компонентами могут быть равны. Значение `hashCode` согласовано с этим равенством: у двух равных записей одинаковый hash; **точную числовую формулу и число не следует заучивать**. `toString` содержит имя record и представление компонентов; его подробный формат — **не протокол сериализации** и не основа для парсинга бизнес-сообщений.

Именно неявные `equals`, `hashCode` и `toString` используют значения **полей** компонентов, а не вызовы методов доступа. Неявный `equals` требует совпадения класса record во время выполнения (runtime class). Ссылочные компоненты сравниваются по семантике `Objects.equals` (две ссылки `null` равны), примитивные — по семантике `compare` соответствующего класса-обёртки.

В Probe record `Mask(int x)` переопределяет метод `public int x(){return 0;}`. При сохранённом значении `x=67` метод доступа возвращает 0, но `new Mask(67)` не равен `new Mask(0)` и равен другому `new Mask(67)`. На проверенном JDK результат `toString` содержит `x=67`, а не значение, возвращённое accessor.

Записи `A(67)` и `B(67)` разных классов не равны. Напротив, `Box<String>(null)` и `Box<Integer>(null)` равны: разные аргументы generic-типа в исходном коде не создают разные runtime-классы, а оба компонента равны `null`. Сравнение самой записи с `null` даёт `false`. Два новых `Empty()` равны, поскольку нет различающихся компонентов, но это разные экземпляры, не singleton. Эти правила не переносятся автоматически на явно написанные методы. См. [CORE-EQUALITY](equality.md) и [generics](generics.md).

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

Record API дополнительно требует инварианта реконструкции (§5) и согласованности равенства, хэша и текста: равные записи должны давать равный текст, с оговоркой для соответствующих равных компонентных значений, у которых собственные строковые представления различаются. Если custom equals игнорирует компонент, следует продумать также hashCode и toString, а не считать любые пользовательские методы заведомо запрещёнными. Порядок сравнения компонентов, конкретный алгоритм hashCode и числовой хэш не специфицированы; хэш может изменяться между запусками даже при одинаковых хэшах компонентов. Текст для человека, не парсинг полей. [Record.equals/hashCode/toString](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Record.html).

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

То же относится к float. В Probe FP(float f,double d) сравнение Float.NaN/Double.NaN с другими NaN payload (Float.intBitsToFloat(0x7fc00067), Double.longBitsToDouble(0x7ff8000000000067L)) дало равенство и одинаковый hashCode; отличие знака нуля каждого типа отдельно сделало записи неравными. Для всех восьми примитивных типов контракт равенства — результат compare соответствующей wrapper class равен нулю, а не обязательные реальные boxing и вызовы compare. Точный алгоритм хэширования не следует выводить из wrapper hashCode. См. [числовые типы](numbers.md) и [преобразования](conversions.md).

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

**Copy-in без copy-out тоже недостаточен:** `CopyIn(int[] values)` с compact constructor `values=values.clone();` и стандартными accessor/equals при `bad.equals(new CopyIn(bad.values()))` даёт `false`, потому что новый конструктор снова копирует массив. Accessor к тому же раскрывает внутренний массив. Для ограниченного случая одного `int[]` можно согласовать контракт так: в Probe роли 02 это вложенный record Good, а точное самостоятельное объявление ниже роль 03 отдельно скомпилировала как `Good.java` вместе с проверочным `Check.java` и запустила Check (см. независимую проверку в конце статьи).

~~~java
import java.util.Arrays;
import java.util.Objects;

record Good(int[] values) {
    Good { values = Objects.requireNonNull(values).clone(); }
    public int[] values() { return values.clone(); }
    public boolean equals(Object o) {
        return o instanceof Good g && Arrays.equals(values, g.values);
    }
    public int hashCode() { return Arrays.hashCode(values); }
    public String toString() {
        return "Good[values=" + Arrays.toString(values) + "]";
    }
}
~~~

Политика: `null` запрещён, пустой массив разрешён, содержимое `int[]` определяет значение. Копия на входе (copy-in) защищает запись от изменения исходного массива вызывающим кодом; копия на выходе (copy-out) — от изменения массива, полученного через accessor. Сравнение содержимого и согласованные `equals`, `hashCode`, `toString` обеспечивают равенство, одинаковый хэш и текст при реконструкции через `new Good(g.values())`.

В Probe роли 02 на входе `a={67}` создан Good g; отдельно `a[0]=1` и `g.values()[0]=2` не изменили `g.values()[0]` (67 после каждой операции). Проверка `Good copy = new Good(g.values());` подтвердила равенство, одинаковые хэш и текст; `new Good(null)` вызвал ожидаемый `NullPointerException`. Роль 03 независимо подтвердила эти свойства точного самостоятельного Good и дополнительно проверила пустой массив, неравное содержимое, симметричность равенства и оба антипримера; исходники и команды приведены в конце статьи.

Здесь `equals`, `hashCode` и `toString` работают с внутренними массивами, не с лишними копиями. Это не универсальная стратегия deep copy для массивов ссылок, графов, циклов или конкурентных изменений входа. Если прозрачное представление компонентов не подходит, обычный final class или подходящий snapshot проще.

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

Member record неявно static, явный static допустим; local record также неявно static, но явный static в локальной декларации запрещён (LocalStatic). Ни тот ни другой не захватывает экземпляр внешнего класса; local record не может захватить даже effectively final локальную переменную, а member record не может использовать type parameter внешнего класса. LocalCapture и OuterType отдельно отклонены non-static.cant.be.ref. Собственные параметры типа разрешены: в Probe `record Local<T>(T value)` дал 67. Интерфейс Default имеет default x()=-1 и twice()=2*x(); WithDefault(int x) implements Default получает сгенерированный accessor x(), переопределяющий совместимый default, а twice() у WithDefault(67) даёт 134. См. [вложенные классы](nested-classes.md), [интерфейсы](interfaces.md).

**Методы не делают record автоматически mutable или immutable:** пользовательский метод может посчитать новое значение, вернуть новый record, обратиться к внешнему ресурсу или поменять достижимый через компонент объект. Оценивайте конкретное поведение, а не наличие ключевого слова `record`.

## 7. Аннотации компонентов и varargs (R-19, R-20 · P1)

Аннотация на компоненте record может иметь разные допустимые точки применения: компонент/поле, параметр canonical constructor, accessor или type-use — **в зависимости от `@Target` самой аннотации**. Нельзя обещать, что любая аннотация копируется на все перечисленные места независимо от её целей.

Декларационные цели `@Target` независимы:

- `RECORD_COMPONENT` — сам компонент;
- `FIELD` — неявное поле компонента;
- `METHOD` — неявный accessor;
- `PARAMETER` — производный параметр неявного или compact canonical constructor.

Явно объявленный accessor и параметры полной формы канонического конструктора получают свои собственные аннотации, а не автоматически аннотации компонента. `TYPE_USE` относится к употреблениям типа — это отдельный контекст. Для обнаружения декларационных аннотаций во время выполнения через reflection необходима политика хранения `RetentionPolicy.RUNTIME`.

В Probe аннотация `Mark` имеет `RUNTIME` и все четыре указанные декларационные цели. Проверялись в указанном порядке: компонент (`getRecordComponents()[0]`), поле (`getDeclaredField("x")`), метод доступа (`getDeclaredMethod("x")`), параметр канонического конструктора (`getDeclaredConstructor(int.class).getParameters()[0]`). Фактические результаты:

- `Implicit` — `true,true,true,true`;
- `Explicit` (полная форма конструктора и явный accessor без Mark) — `true,true,false,false`;
- `AnnotCompact` — `true,true,true,true`.

Это узкая проверка декларационных аннотаций во время выполнения, не проверка всех вариантов `TYPE_USE`, политик retention или атрибутов class-файла.

~~~java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.METHOD})
@interface Audited {}

record Packet(@Audited String payload) {}
~~~

Record-компонент может использовать varargs как **последний компонент**, например `record Words(String... tokens) {}`. Такой компонент представлен массивом, поэтому вновь возникают правила array identity и mutable aliasing. С varargs не-reifiable обобщённых элементов возможны предупреждения о heap pollution; правила `@SafeVarargs` и безопасности доступа обсуждаются в [CORE-ARRAYS](arrays.md) и [CORE-ERASURE](erasure.md). Пометка `@SafeVarargs` **не** превращает небезопасный код в безопасный; нельзя автоматически считать всякую record-конструкцию с varargs свободной от проблем.

Допустим не более одного компонента переменной арности (varargs), причём только последнего: `NotLast` отклонён. В полной форме канонического конструктора соответствующий параметр должен быть объявлен тоже как varargs, а не просто как массив; `BadArity` отклонён. В Probe record `Words(String... words)` с таким явно объявленным конструктором сохраняет переданный `String[]`: `w.words()==words`, изменение `words[0]` видно через accessor, `new Words().words().length` равно 0. Автоматического копирования переданного массива нет.

Для non-reifiable типа элемента (тип, информация о котором не полностью доступна во время выполнения; см. предыдущие ссылки на массивы и стирание типов) возникает unchecked warning, если не применено предусмотренное подавление. `@SafeVarargs` уместна на подходящем каноническом конструкторе лишь при обоснованной гарантии безопасности его работы с varargs. Пометка самого компонента не является обходом этого ограничения: `SafeComponent(@SafeVarargs String... x)` отклонён, поскольку аннотация распространяется на метод доступа `x()` фиксированной арности. Все возможные предупреждения для generic varargs не проверялись.

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
| `original.equals(new BadAccessor(original.values()))` из §5 | Компилируется, даёт `false` | Ошибка проектирования: нарушен copy invariant |
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
| R-12 | P0 | §4.2: float/double NaN payload и ±0.0 |
| R-13 | P0 | §1/3: null разрешён без собственной валидации |
| R-14 | P0 | §§2/8: имена компонент, сигнатуры accessors |
| R-15 | P1 | §5: BadAccessor и CopyIn нарушают copy invariant; Good согласует копирование/равенство |
| R-16 | P1 | §3: explicit vs compact canonical, видимость/throws |
| R-17 | P1 | §4/5: custom equals/hashCode/toString должны сохранять логический контракт |
| R-18 | P1 | §2/6: static, nested/local records |
| R-19 | P1 | §7: `@Target`, retention, implicit/explicit/compact propagation |
| R-20 | P1 | §7: varargs/non-reifiable, `@SafeVarargs` boundaries |
| R-21 | P2 | **Граница**: сериализация record и canonical deserialization требуют CORE-SERIAL |
| R-22 | P2 | **Граница статьи о record:** [record patterns / Java21 switch](patterns.md) разбираются в отдельной CORE-PATTERNS, а не подменяются этим разделом |
| R-23 | P2 | **Граница**: reflection `getRecordComponents`/class-file representation — CORE-REFLECTION |

## Нормативные источники и пределы проверки

**Первоисточники:** [JLS 21 §8.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10), [§8.10.1 (components)](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10.1), [§8.10.2 (body)](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10.2), [§8.10.3 (members)](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10.3), [§8.10.4 (constructors)](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10.4), [Record Java21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Record.html), [Objects.equals](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html#equals(java.lang.Object,java.lang.Object)). Интервью-вопросы из [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md) указывают, что стоит уметь объяснять, но **не заменяют Java SE контракт**.

**Принятое G1** содержит полный позитивный `RecordsProbe.java` с 16 подписанными наблюдениями R01–R16 (в том числе намеренно плохое R13=false) и 16 отдельно проверенных negative N01–N16 в §6. Исходные результаты G1 получены исследователями на OpenJDK 21.0.12.1 с `--release 21 -Xlint:all`, но **не означают, что все Java-блоки выше прогнаны автоматически**. [Общий runnable с двумя G1-программами](../../examples/core/records-immutability/README.md) интегрирован из [технического PR #133](https://github.com/Melchiorius/backend-interview-labs/pull/133) в авторскую ветку; исходники и эталоны сверены с G1 §5, `verify.sh` выполняет компиляцию `javac --release 21 -Xlint:all` и два строгих `diff -u`. Роль 03 отдельно проверила опубликованные Git blobs и выполнила локальные запуски на Debian OpenJDK 21.0.12.1 без предупреждений. Историческая проверка общего HEAD и редакционные ворота зафиксированы в [PR #132](https://github.com/Melchiorius/backend-interview-labs/pull/132); это не подтверждение автоматической компиляции всех блоков кода в статье.

**NOT RUN в рамках этой статьи без отдельного свидетельства:** автоматическая компиляция всех inline Java-фрагментов; N01–N16 в действующей CI; альтернативные JDK/OS; GUI Obsidian/IntelliJ; исчерпывающая reflection/class-file inspection (узкая runtime-проверка аннотаций ниже выполнена), record serialization/ORM, record patterns как отдельная тема, deep-copy циклических графов, JMM publication/concurrency stress, performance/JMH/GC/JIT. P2 **не** помечаются как реализованные темы.

**Особые правила восстановления record:** [CORE-SERIAL](serialization.md) объясняет, почему при десериализации `Serializable record` вызывается канонический конструктор, чем отличается обычный `Serializable`-класс и что означает специальное правило `serialVersionUID`.

**Новые фактические проверки 02.** [Полные исходники, команды, фактические выводы и диагностические категории](https://github.com/Melchiorius/backend-interview-labs/issues/363#issuecomment-7227809467) на Debian OpenJDK/javac 21.0.12.1: Probe.java с --release 21 -encoding UTF-8 -XDrawDiagnostics -Xlint:all -Werror скомпилирован exit 0 без stderr; JVM exit 0, stderr пуст, stdout 20 строк/328 байт (метки проверок и непосредственные наблюдения compact/annotation). 29 отдельных отрицательных компиляций с --release 21 -encoding UTF-8 -XDrawDiagnostics, все exit 1/stdout пуст, включая восемь запрещённых имён; LocalStatic дал две синтаксические диагностики, Abstract — does.not.override.abstract. Это не повтор исторических 16 G1 или 24 ожидаемых отклонений всего CI. В проверке 02 новые фрагменты использовались в составе Probe; независимая проверка 03 точного самостоятельного Good приведена ниже. Не все Markdown-блоки запускались отдельно.

Полный общий runner выполнен дважды на пяти восстановленных входах с проверенными Git blob SHA/mode (не полный клон), default environment и LC_ALL=C.utf8, без инструментирования. Оба exit 0, внешний stderr и build/javac.stderr пусты, инвентарь входов до/после совпал, оба фактических stdout каждого запуска совпали с fixture (четыре cmp exit 0). Одна компиляция двух Java-исходников с --release 21 -Xlint:all, отказ при диагностике javac на stderr, два JVM и два diff -u; stdout обоих процессов сохраняются в build и печатаются скриптом, stderr JVM наследуется, отдельно не захвачен. Фактические R16 строк/144 байта, I10/94; внешний stdout 27 строк/314 байт, SHA-256 38e0ddf952503d6e8adecdf4844b53e70d99428207d0fc61fb63b27ba24bd6ea, после 26 строк маркер `CHECK PASSED: both stdout files match expected; javac emitted zero warnings`. 26 наблюдений, не 26 unit-тестов; R13=false намеренный антипример. SHA-256 обоих эталонов и вывод в приложении. Скрипт не использует Werror или явное encoding; OS locales C/C.utf8/POSIX, locale/charset JVM не измерялись. Никаких изменений общего примера, README, immutability.md или исследовательской базы.

**Независимые фактические проверки 03.** [Полные Good.java, Check.java, команды, результаты и диагностики](https://github.com/Melchiorius/backend-interview-labs/pull/364#issuecomment-7227962097). Точные 14 строк объявления Good из §5 скомпилированы вместе с отдельным проверочным классом на Debian OpenJDK/javac 21.0.12.1: `javac --release 21 -encoding UTF-8 -XDrawDiagnostics -Xlint:all -Werror -d classes Good.java Check.java`, затем `java -cp classes Check`. Компиляция и запуск завершились с кодом 0; stdout/stderr компилятора и stderr JVM пусты. Фактический stdout — 17 строк/458 байт. В нём строки `copy-in=67`, `copy-out=67`, `reconstruct=true,true,Good[values=[67]]`, `empty/null=allowed/NullPointerException` и `bad=copy-in:false,leak:1,accessor:false`. Симметричность равенства и различие содержимого проверены условиями внутри Check, а не отдельными строками вывода. Исполнялся Check, а не Good как класс с main.

В той же независимой пробе проверены различные сырые биты NaN для float и double и неравенство знаковых нулей отдельно для каждого типа. Для аннотаций в порядке компонент/поле/accessor/параметр получены `Implicit=true,true,true,true`, `Explicit=true,true,false,false`, `CompactMark=true,true,true,true` (в Probe 02 последний класс назывался AnnotCompact). Для varargs наблюдалось `varargs=y,0`: изменение переданного массива видно, пустой вызов даёт длину 0. Отдельный `record Var<T>(T... values)` с `@SafeVarargs` на каноническом конструкторе и клонированием массива успешно прошёл строгую компиляцию; после изменения исходного массива наблюдалось `safe-constructor=a`. Стандартный accessor этого Var по-прежнему раскрывает массив: это пример копирования на входе, а не универсальной безопасности или неизменяемости.

В 03 выполнены 17 изолированных отрицательных компиляций (exit 1, stdout пуст), включая два из восьми запрещённых имён, не полный повтор всех 29 случаев 02. LocalStatic дал две синтаксические диагностики, AbstractMethod — обычную ошибку отсутствия реализации абстрактного метода. Исторические 16 отрицательных G1, 29 авторских 02, 17 независимых 03 и 24 ожидаемых отклонения всего CI — разные наборы.

Роль 03 также дважды запустила неизменённый общий runner на пяти восстановленных входах с проверенными Git blob SHA/mode (не полный клон), в default environment и C.utf8, без инструментирования. Оба exit 0, внешний stderr и build/javac.stderr пусты; все восемь cmp (два эталона, полный внешний stdout и инвентарь входов для каждого запуска) дали exit 0. Фактические R16/144, I10/94 и внешний вывод27/314 совпали с приведёнными выше результатами 02, в том числе по SHA-256. stderr JVM наследовался, а не проверялся отдельными захватами; charset/locale JVM не измерялись. Это ранее проведённые независимые проверки, а не новые эксперименты при редакторской правке. Временные пробы не входят в CI общего runner.
