# CORE-ENUMS — перечисления как типы, экземпляры и состояния (Java 21)

**Вопрос для собеседования:** чем enum в Java отличается от набора `public static final int`, как устроены его экземпляры и поведение, и какие гарантии действительно дают `EnumSet`, `EnumMap` и `switch`? Здесь используется **Java SE 21 без preview**. Предварительно полезны [классы и конструкторы](classes.md), [наследование](inheritance.md), [интерфейсы](interfaces.md), [сравнение объектов](equality.md), [неизменяемость](immutability.md). Управляющие конструкции и `yield` разбираются отдельно в [CORE-CONTROL](control-flow.md); типовые patterns — в [CORE-PATTERNS](patterns.md).

**Основание:** [принятый исследовательский G1 CORE-ENUMS + CORE-CONTROL](../../docs/plans/CORE-ENUMS-CONTROL.md), Issue [#146](https://github.com/Melchiorius/backend-interview-labs/issues/146), **E-01–E-25 = 17 P0 / 5 P1 / 3 P2**. Нормы: [JLS Java21 §8.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.9), [§8.9.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.9.1), [§8.9.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.9.2), [java.lang.Enum API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Enum.html), [EnumSet API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/EnumSet.html), [EnumMap API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/EnumMap.html). Учебный текст отличается от полной автономной G1-программы: отдельные snippets здесь не считаются автоматически выполненными только на основании G1.

## 1. Почему enum — настоящий тип, а не числовые константы (E-01, E-02, E-07)

Когда состояние заказа кодируется значениями `int`, метод, принимающий `int state`, может получить любое целое число — в том числе бессмысленное. С enum набор допустимых *именованных экземпляров* задаётся в объявлении типа, а компилятор различает, например, `Status` и `Priority`. Каждый объявленный элемент — объект собственного enum-типа и неявное `public static final` поле. Enum-класс непосредственно наследует специализированный `java.lang.Enum<E>`; он не является record, не наследуется произвольно от другого класса и не разрешает создавать новые значения через `new`, Reflection-обход или произвольное наследование.

~~~java
enum Status { NEW, PAID, SENT, DONE }
enum Priority { HIGH, LOW }

class EnumIdentityDemo {
    static void inspect() {
        Status status = Status.NEW;
        System.out.println(status == Status.valueOf("NEW")); // true
        System.out.println(status.equals(Status.NEW));       // true
        // status = Priority.HIGH; // ошибка типов при компиляции
        // Status another = new Status(); // создать новую enum-константу нельзя
    }
}
~~~

`==` сравнивает здесь идентичность заранее определённой enum-константы. В `java.lang.Enum` методы `equals` и `hashCode` финальны: невозможно переопределить их для сравнения по изменяемым полям. В отличие от объекта с произвольным изменяемым состоянием, **каждая объявленная enum-константа — единственный экземпляр своего значения в пределах соответствующей загрузки типа**. Это не делает её изменяемые поля автоматически неизменяемыми или потокобезопасными (§7).

**Порядок объявления имеет значение** для ряда API. Метод, синтезируемый компилятором для каждого enum-типа, `values()` возвращает массив констант в порядке объявления. `valueOf(String)` распознаёт *точное имя*, а не отображаемую строку. Эти два метода относятся к конкретному объявленному enum-типу, в отличие от generic `Enum.valueOf(Class<T>, String)`.

## 2. Конструкторы и инициализация: от аргумента константы к инварианту (E-05, E-06, E-22)

У констант могут быть параметры, которые при инициализации передаются в конструктор. У enum-конструктора **нет общедоступной формы для клиентского `new`**: разрешён `private` либо отсутствие модификатора доступа, причём последний случай по семантике также private. `public` и `protected` запрещены **на этапе компиляции** (исследовательские N01/N02), а не скрываются только от IDE.

~~~java
enum Status {
    NEW(67), PAID(80), SENT(90), DONE(100);

    private final int businessCode;
    Status(int code) { this.businessCode = code; }
    int code() { return businessCode; }

    boolean mayAdvanceTo(Status target) {
        return switch (this) {
            case NEW -> target == PAID;
            case PAID -> target == SENT;
            case SENT -> target == DONE;
            case DONE -> false;
        };
    }
}

class OrderLifecycle {
    public static void main(String[] args) {
        System.out.println(Status.NEW.code());                 // 67
        System.out.println(Status.NEW.mayAdvanceTo(Status.PAID)); // true
        System.out.println(Status.NEW.mayAdvanceTo(Status.DONE)); // false
    }
}
~~~

**Трассировка:** при создании объекта константы `NEW` её аргумент `67` сохраняется в `businessCode`. Это **явный код бизнеса**, который не зависит от позиции константы в исходном файле. Правило перехода — обычный доменный метод над состояниями, не автоматическая способность всех enum. Он возвращает false при запрещённом перескоке NEW→DONE. Наличие такого метода **не делает** сам заказ с полем Status неизменяемым; присваивание нового состояния в обычном объекте заказа остаётся задачей его владельца.

**Тонкая ошибка порядка инициализации (E-22):** enum-конструктор инициализирует каждую константу во время class initialization. Компилятор запрещает прямо обращаться из enum-конструктора к не являющемуся constant variable статическому полю того же enum (N09), поскольку оно ещё может быть не инициализировано. Это отдельное правило JLS §8.9.2, а не общий запрет статических методов в enum или «любое поле static всегда null». Обычное `static final int`, инициализированное константным выражением, имеет иную категорию.

## 3. name, ordinal, toString и опасность «сохранить ordinal в БД» (E-02–E-04, E-13, E-14)

Три внешне похожих API означают разные вещи. `name()` возвращает **точное имя** в исходном объявлении и не может быть переопределён. `ordinal()` возвращает индекс этой константы в порядке объявлений **с нуля**; он также не переопределяется. `toString()` изначально возвращает имя, но может быть переопределён для отображения человеку. Эти строки могут намеренно отличаться.

~~~java
enum DisplayState {
    NEW(67), PAID(80);
    private final int code;
    DisplayState(int code) { this.code = code; }
    int code() { return code; }

    @Override public String toString() {
        return "status-" + name().toLowerCase();
    }
}

class EnumNameDemo {
    public static void main(String[] args) {
        System.out.println(DisplayState.NEW.name());       // NEW
        System.out.println(DisplayState.NEW.ordinal());    // 0
        System.out.println(DisplayState.NEW.toString());   // status-new
        System.out.println(DisplayState.NEW.code());       // 67
        System.out.println(DisplayState.valueOf("NEW"));   // status-new
    }
}
~~~

Последняя строка особенно показательна: `valueOf("NEW")` нашёл enum-константу по имени, а печать **найденного объекта** вызвала её переопределённый `toString()`. Поэтому `DisplayState.valueOf("status-new")` не сработает: Java ищет имя `NEW`, а не UI-представление. Неизвестное имя вызывает **IllegalArgumentException**, `valueOf(null)` — **NullPointerException**, уже *при выполнении*. Они не являются compiler-negative.

**Почему ordinal не business ID:** если вставить новую константу перед PAID, `ordinal()` PAID изменится. Нельзя считать ordinal стабильным идентификатором хранения/интеграционного API. Для бизнес-кода используйте явное поле, как `code=67`, либо отдельное документированное сопоставление. `name()` точен для *текущего* объявления, но переименование значения в новом релизе тоже требует миграции совместимости клиентов — не объявляйте даже name вечным внешним протоколом без отдельного контракта.

Нельзя написать собственный `values()` или `valueOf(String)` с конфликтующей сигнатурой в enum (N06/N07), а переопределение финальных `name()`/`ordinal()` компилятор отвергнет (N04/N05). Отдельный статический helper с другим именем допустим.

## 4. Тела отдельных констант: один enum, разные реализации (E-08, E-09, E-10, E-16, E-21)

Иногда константы должны выполнять одну операцию по-разному. Длинная цепочка `if (op==PLUS)` не обязательна: enum может объявить абстрактный метод, а каждая константа реализует его в **собственном теле класса конкретной enum-константы**. Далее будем называть его «телом константы». Enum также может реализовать обычный интерфейс, но не может явно написать `extends ArbitraryBase` или объявить параметры типа как `enum E<T>`.

~~~java
interface Arithmetic {
    int apply(int a, int b);
}

enum Operation implements Arithmetic {
    PLUS {
        @Override public int apply(int a, int b) { return a + b; }
    },
    TIMES {
        @Override public int apply(int a, int b) { return a * b; }
    };

    // Дополнительно можно объявить abstract метод здесь и потребовать
    // реализацию у всех констант; достаточно метода из интерфейса.
}

class EnumOperationDemo {
    public static void main(String[] args) {
        System.out.println(Operation.PLUS.apply(6, 7));  // 13
        System.out.println(Operation.TIMES.apply(6, 7)); // 42, арифметический результат
        System.out.println(Operation.PLUS.getDeclaringClass() == Operation.class); // true
        System.out.println(Operation.PLUS.getClass() == Operation.class);          // false
    }
}
~~~

Первые две операции имеют разное поведение, хотя обе константы имеют объявленный enum-type Operation. В примере вторая арифметическая операция действительно равна 42: это **произведение 6×7**, а не условное число-заглушка. Если одна константа не реализует абстрактную обязанность, как в G1 N08, исходник отклоняется до выполнения.

**Неявные `final` и `sealed` (E-10):** enum без тел отдельных констант **неявно `final`**, поскольку для его значений не создаются дополнительные подклассы. Enum хотя бы с одним телом константы **неявно `sealed`**: такие тела задают особые классы, допустимые как его непосредственные подклассы по JLS. При этом явное объявление `final enum E` запрещено (N13). Это не разрешает внешнему классу написать `extends Operation`.

**getClass vs getDeclaringClass (E-16):** `getClass()` у константы с собственным телом описывает её actual runtime class, отличающийся от самого Operation. `getDeclaringClass()` возвращает именно объявленный enum type Operation. Для обычной константы без собственного тела эти классы совпадают. G1 E21=true для Status.NEW, E22=false для Operation.PLUS при сравнении actual и declaring classes.

## 5. EnumSet — специализированное множество именованных вариантов (E-11, E-18)

`EnumSet<Status>` хранит значения **одного конкретного enum-типа**, имеет API множества (добавление, проверка, удаление), а при обходе использует **порядок объявления констант**, не hash order и не отсортированный `toString`.

~~~java
import java.util.EnumSet;

enum Step { NEW, PAID, SENT, DONE }

class SetDemo {
    public static void main(String[] args) {
        EnumSet<Step> s = EnumSet.of(Step.DONE, Step.NEW, Step.SENT);
        System.out.println(s);                  // [NEW, SENT, DONE]
        System.out.println(s.contains(null));   // false
        System.out.println(s.remove(null));     // false
        try {
            s.add(null);
        } catch (NullPointerException ex) {
            System.out.println("NPE");          // NPE
        }
    }
}
~~~

**Разбор:** объявление задаёт NEW, PAID, SENT, DONE, поэтому даже при добавлении DONE первой обход начинается с NEW. `contains(null)` и `remove(null)` не выбрасывают исключение — они возвращают false; но `add(null)` запрещён и бросает NPE. Попытка `EnumSet<Step>.add(OtherEnum.VALUE)` с несовместимым типом — статический **compiler-negative N29**, а не штатный false. Type erasure и raw types в этой статье отдельно не изучаются; при нарушении типовых контрактов небезопасными raw-приведениями возможны другие ошибки, не доказанные здесь.

**Итератор EnumSet — weakly consistent (E-18, P1).** По API он не выбрасывает `ConcurrentModificationException` и может, но не обязан, отражать изменения во время обхода. Это **не обещание снимка**, не гарантия детерминированного порядка наблюдения конкурентных вставок и не означает автоматически thread-safe коллекцию для всех операций. Гонки и память Java Memory Model — отдельная тема; эксперимент со стресс-нагрузкой не выполнялся.

## 6. EnumMap: ключи по объявлению, null value и неоднозначность get (E-12, E-19)

`EnumMap<Step, String>` использует **enum-ключи** и при итерировании обходит их в порядке объявления, независимо от того, в какой последовательности были вызваны `put`. **Null key не допускается** для записи, но **null value допускается**. Поэтому `map.get(k)==null` само по себе не позволяет выяснить, был ли ключ отсутствующим или ему явно сопоставили null. Для этого нужен `containsKey(k)`.

~~~java
import java.util.EnumMap;

enum Step { NEW, PAID, SENT, DONE }

class MapDemo {
    public static void main(String[] args) {
        EnumMap<Step, String> m = new EnumMap<>(Step.class);
        m.put(Step.SENT, "sent");
        m.put(Step.NEW, "new");
        m.put(Step.DONE, null);
        System.out.println(m);                       // {NEW=new, SENT=sent, DONE=null}
        System.out.println(m.containsKey(Step.DONE));  // true
        System.out.println(m.get(Step.DONE) == null);  // true
        System.out.println(m.containsKey(null));       // false
        try {
            m.put(null, "bad");
        } catch (NullPointerException ex) {
            System.out.println("NPE");                // NPE
        }
    }
}
~~~

**Контраст с EnumSet:** там null вообще не может быть *элементом*; здесь null может быть *значением*, но не ключом. У EnumMap weakly consistent iterators и специализированная реализация, но нельзя из этого делать гарантии времени O(1) на всех JVM, точной структуры памяти, атомарности всей карты или необходимости synchronizing на mutable enum. Полные контракты Map и сравнение коллекций относятся к отдельным curricular ID.

## 7. Состояния, единственность констант и изменяемость — где ловушка (E-06, E-15)

Для управления заказом enum удобен как **тип состояния**: сервис хранит текущее значение `Status` и проверяет разрешённость перехода. При данной загрузке enum-типа **каждая его константа — единственный общий экземпляр соответствующего значения**, но весь enum-тип не является одним объектом. Если добавить изменяемый счётчик запросов в `Status.NEW`, он будет общим для всех обращений к этой константе. Счётчик *не* становится потокобезопасным только из-за того, что он находится внутри enum.

~~~java
enum MutableState {
    NEW;
    private int visits;

    void recordVisit() { visits++; }
    int visits() { return visits; }
}

class MutableEnumDemo {
    public static void main(String[] args) {
        MutableState.NEW.recordVisit();
        System.out.println(MutableState.NEW.visits()); // 1 в отдельном процессе
        System.out.println(MutableState.NEW == MutableState.valueOf("NEW")); // true
    }
}
~~~

`visits++` — составное чтение, изменение и запись. Без необходимой синхронизации несколько потоков могут терять обновления; это **не** результат эксперимента с гонкой, а следствие отсутствия гарантии атомарности для обычного поля. Единственность экземпляра каждой enum-константы предотвращает создание дополнительных значений, но **не** обеспечивает атомарности обращений к её изменяемым полям. Подробнее — [CORE-IMMUTABLE](immutability.md); проверка JMM и многопоточных гонок вне охвата этой темы.

**Проектное решение:** используйте enum для *перечня состояний и поведения над ними*, а динамическое состояние конкретного заказа храните в отдельном объекте или хранилище заказа. В G1 вызов `Status.NEW.visit()` демонстрирует общий изменяемый счётчик (`E20=1`) в одном процессе, а не рекомендуемую реализацию счётчика для разных заказов внутри общей константы.

## 8. Enum и switch: исчерпываемость не равна «default обязателен» (E-17, C-22)

Для switch-expression над enum все допустимые константы могут быть перечислены как ветви, и тогда `default` **не требуется**. В **Java21** даже квалифицированная constant label `case Status.NEW` **законна**: это подтвердил положительный G1 E26. Старое правило из шпаргалки «в case enum разрешено только NEW без квалификации» нельзя использовать как универсальное Java21 compiler error.

~~~java
enum Status { NEW, PAID, DONE }

class EnumSwitchDemo {
    static String format(Status status) {
        return switch (status) {
            case Status.NEW -> "new";
            case Status.PAID -> "paid";
            case Status.DONE -> "done";
        };
    }
    public static void main(String[] args) {
        System.out.println(format(Status.PAID)); // paid
    }
}
~~~

Компилятор проверяет полноту возможных значений enum при текущей компиляции; неполный switch-expression над иным selector без соответствующего coverage — compiler-negative (N16). Но legacy switch-statement не обязан быть исчерпывающим (§ CORE-CONTROL). **Null selector** при отсутствии `case null` приводит к NPE: обычный `default` не служит универсальным обработчиком null. Эволюция enum после отдельной компиляции старого клиента и добавления значения **не тестировалась** (E-23/P2); не выводите наблюдённый `MatchException` из примеров CORE-PATTERNS автоматически для этой ситуации.

## 9. Границы специальных механизмов (E-20, E-23–E-25)

**E-20 P1 — enum и сериализация.** Java Serialization сохраняет идентичность конкретной enum-константы по её имени и объявленному типу, а не создаёт обычный независимый экземпляр по набору полей. Однако здесь не рассматриваются детально `readResolve`, графы сериализации, отражение и эволюция формата. Особое правило следует из [JLS §8.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.9) и [Enum API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Enum.html), **не** из E01/E25 или локального эксперимента с `ObjectInputStream`.

**E-23 P2 — отдельная бинарная эволюция:** добавление/переименование enum-константы после раздельной компиляции клиента и библиотеки, поведение старых exhaustive switch, возможные runtime exceptions. **NOT RUN**, даже если JLS [§13](https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html) описывает варианты совместимости.

**E-24 P2 — Reflection, cloning и serialization internals:** enum имеет особые ограничения, но обойти/измерить Reflection/JVM-границы отдельными experiments здесь не пытались. Не объявлять этот абзац реализацией самостоятельного курса CORE-SERIAL.

**E-25 P2 — внутренности EnumSet/EnumMap и производительность:** API определяет *наблюдаемый* порядок, допустимые значения и характеристики итераторов; **не обещаем** фиксированного layout памяти, количества аллокаций, выигрыша в JIT, thread-safety или конкретных результатов benchmark. Другие JDK и GC/JMH не проверены.

## 10. Сценарий G1: что означают E01–E30

В принятом [плане G1 §7.1](../../docs/plans/CORE-ENUMS-CONTROL.md) приведена полная Java 21-программа `EnumProbe.java` с 30 строками вывода. Она уже опубликована как [исходный файл](../../examples/core/enums-control/src/EnumProbe.java) вместе с [эталонным выводом](../../examples/core/enums-control/enum.expected.stdout) и [инструкцией запуска общего примера](../../examples/core/enums-control/README.md). Это отдельная исполняемая программа, а не автоматическая компиляция всех Java-фрагментов этой статьи.

| Строки G1 | Наблюдение | Что проверяем |
|---|---|---|
| E01–E06 | список в порядке объявления; `name=NEW`, `ordinal=0`, `toString=status-new`; поиск PAID; business-code 67 | §1–3 |
| E07–E10 | true/false для state transitions; PLUS 6+7=13, символ TIMES `*` | §2/4 |
| E11–E14 | set/map в порядке объявления; DONE присвоено null при `containsKey=true` | §5–6 |
| E15–E19 | `false, NPE, NPE, IAE, NPE` | null-контракты и `valueOf` |
| E20–E25 | visits=1, getClass/declaring true/false, sealed false/true, equals true | §4/7 |
| E26–E30 | `paid, true, false, false, false` | qualified case, `values`, null keys, name≠toString |

**Подписи и количество:** E01–E30 — **30 stdout-строк одного** процесса; связанный `ControlProbe.java` — 21 строка **другого** процесса. Это **51 наблюдение двух запусков, не 51 unit-тест**. Не путать обнаруженную локально возможность компиляции `case Status.NEW` с намеренно исключённым ошибочным кандидатом **N27**, отсутствующим среди 31 compiler-negative.

## Карта покрытия E-01–E-25

| ID | Приоритет | Конкретное раскрытие, пример или честная граница |
|---|---|---|
| E-01 | P0 | §1 enum как класс, Enum base, static final instances; N03/N32 |
| E-02 | P0 | §1/3 values/valueOf и порядок; E01/E05/E27 |
| E-03 | P0 | §3 name vs overridden toString; E02/E04/E30 |
| E-04 | P0 | §3 ordinal vs explicit businessCode 67; E03/E06 |
| E-05 | P0 | §2 constructor access/arguments/initialization; N01–N03 |
| E-06 | P0 | §2/7 доменная state machine NEW→PAID→SENT→DONE, E07/E08 |
| E-07 | P0 | §1/7 identity, final equals/hashCode, E05/E25 |
| E-08 | P0 | §4 constant-specific bodies/interface operations; E09/E10/N08 |
| E-09 | P0 | §4 implements interface, cannot extends arbitrary/generic; N11/N12/N32 |
| E-10 | P0 | §4 implicit final/sealed, no explicit final enum; E23/E24/N13 |
| E-11 | P0 | §5 EnumSet order/type/null semantics; E11/E15/E16/E29/N29 |
| E-12 | P0 | §6 EnumMap key order/null-key/null-value; E12–E14/E17/E28 |
| E-13 | P0 | §3 valueOf exact name, IAE/NPE runtime; E05/E18/E19 |
| E-14 | P0 | §3 generated values/valueOf and final methods; N04–N07 |
| E-15 | P0 | §7 общий экземпляр конкретной константы и изменяемый счётчик не дают гарантий потокобезопасности; E20 |
| E-16 | P0 | §4 getClass vs getDeclaringClass; E21/E22 |
| E-17 | P0 | §8 exhaustive enum switch/case Status.NEW; E26/C07/C08 |
| E-18 | P1 | §5 weakly consistent EnumSet iterator, no snapshot/thread safety |
| E-19 | P1 | §6 EnumMap iteration/null-value/containsKey distinction |
| E-20 | P1 | §9 special enum serialization contract, no full serialization experiment |
| E-21 | P1 | §4 abstract per-constant implementation vs N08 |
| E-22 | P1 | §2 early static non-constant field reference N09 |
| E-23 | P2 | §9 separate compilation/binary evolution — NOT RUN |
| E-24 | P2 | §9 reflection/serialization/cloning internals — NOT RUN |
| E-25 | P2 | §9 memory/layout/JMM/JIT/benchmark — NOT RUN |

## Источники, доказательства и не проведённые проверки

**Норма A:** [JLS §8.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.9), [Enum API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Enum.html), [EnumSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/EnumSet.html), [EnumMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/EnumMap.html), [JLS switch §14.11](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11). Интервью-банки C1–C4 из [G1 §3](../../docs/plans/CORE-ENUMS-CONTROL.md) дают реальную форму вопросов, но **не доказывают** ни язык Java21, ни частоту вопросов. Статус заголовка \`G1 PROPOSED\` в историческом файле устарел: решение [00 G1 ACCEPTED](https://github.com/Melchiorius/backend-interview-labs/pull/145#issuecomment-7072342456) принято **после** [независимого G1 PASS 04](https://github.com/Melchiorius/backend-interview-labs/pull/145#pullrequestreview-6009419489); сам G1 план роль 02 не редактирует.

**Исследование G1 и независимая проверка 04:** на OpenJDK 21.0.12.1 два полных Java-исходника с `javac --release 21 -Xlint:all` были собраны и запущены; вывод **30+21 строк** сверялся с эталонами. Отдельно проверены **31/31 отрицательных примеров компиляции** N01–N26/N28–N32 по диагностическим категориям; N11 — преднамеренная ошибка грамматики, а N27 исключён, так как `case Status.NEW` допустим в Java 21. **В интегрированном G2** [GitHub Actions #44978108366](https://github.com/Melchiorius/backend-interview-labs/actions/runs/44978108366) успешно проверил оба опубликованных исходника и их эталонный вывод на HEAD `c6c4bfe822130db220aeffd390798a52e2cc9992` через [workflow](../../.github/workflows/java-build.yml). Последующие изменения требуют отдельного CI; ни этот запуск, ни G1 не означают, что все встроенные Java-фрагменты статьи компилировались или что 31 отрицательный пример запускается в CI.

**NOT RUN без дополнительного свидетельства:** компиляция всех самостоятельных Java-фрагментов статьи в CI, 31 отрицательный пример в обычном CI, многопоточные проверки JMM, раздельная компиляция и бинарная эволюция, внутренние механизмы сериализации и Reflection, выполнение с JPMS, JMH/JIT/GC, другие JDK/ОС, Obsidian/IntelliJ GUI и Markdown-якоря. До G5 реестры `COVERAGE/CURRICULUM` не обновляются.
