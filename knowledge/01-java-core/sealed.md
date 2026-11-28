# CORE-SEALED — управляемые иерархии типов Java 21

**Главный вопрос:** какие классы вправе непосредственно наследовать данный класс или реализовать данный интерфейс, и почему ограничение *прямых* подтипов не запрещает всех косвенных наследников? Материал рассчитан на Java SE 21 без preview. Предварительно полезны [наследование](inheritance.md), [интерфейсы](interfaces.md), [классы](classes.md), [record](records.md). Сразу после статьи читайте [pattern matching и switch](patterns.md). Sealed classes и interfaces стали постоянной возможностью языка в Java 17; pattern matching для switch и record patterns — в Java 21. См. [историю изменений языка Java](https://docs.oracle.com/en/java/javase/21/language/java-language-changes-release.html). Все примеры здесь рассчитаны на Java 21 без `--enable-preview`.

**Основание:** [принятый исследовательский план](../../docs/plans/CORE-SEALED-PATTERNS.md), матрица **S-01–S-20: 12 P0 / 5 P1 / 3 P2**. Норма — JLS 21 [§8.1.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.1.2), [§8.1.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.6), [§9.1.1.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.1.1.4) и [§9.1.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.1.4). Пометки о фактическом запуске внизу отделены от ожидаемых результатов отдельных code snippets.

## 1. Final, sealed, non-sealed — три разные цели (S-01, S-05, S-07, S-16)

Незакрытый обычный класс при соблюдении доступности можно расширять новыми подклассами. Это подходит библиотечной точке расширения. Однако бизнес-результат часто должен представлять ограниченный набор вариантов: успешное значение, ошибку или ожидание. Здесь полезен **sealed type**: разработчик указывает допустимые **прямые** дочерние типы, а компилятор отклоняет неразрешённые непосредственные наследования.

`final` для класса запрещает дальнейшее наследование вообще. `sealed` разрешает перечисленные прямые ветви. `non-sealed` может объявить разрешённая ветвь, чтобы заново открыть её наследование; не существует права написать `non-sealed` на любом обычном классе без хотя бы одного непосредственного sealed-супертипа. Для обычного косвенного наследника открытой ветви этот модификатор не нужен и без другого непосредственного sealed-супертипа запрещён. Правила доступности, конструкторов и наследования сохраняются.

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

У `Result` три **прямых** допустимых подтипа — `Ok`, `Err`, `Pending`. А `Later` — уже **косвенный** подтип `Result`. Он легален, хотя не перечислен в `permits интерфейса Result`, потому что наследуется от открытой ветви `Pending`. Но `class Rogue implements Result` без включения в permits не скомпилируется. Это и есть различие между контролем первого шага и контролем всей иерархии.

Иерархия интерфейсов может быть ориентированным ациклическим графом, а не деревом. Стрелки означают непосредственное наследование или реализацию: `Both → A → R` и `Both → B → R`, где R разрешает A и B, а обе ветви non-sealed. Один объект Both одновременно принадлежит A и B. Аналогично класс AB может непосредственно реализовать два sealed-интерфейса X и Y, если оба разрешают AB. Это подтверждено в отдельном Probe для D-A16a.

Если `non-sealed class A implements R`, то `class C extends A` допустим, но добавление `implements R` в объявление C создаёт ещё одну, уже прямую связь с R. Она должна независимо удовлетворять permits интерфейса R; если разрешён только A, javac отклонит C. Поэтому sealed не обещает непересекающихся вариантов и не отменяет обычные условия наследования.

Внутреннее состояние `Pending.count` изменяемо: метод `increment` вправе увеличить его. Sealed **не обещает глубокой неизменяемости**, потокобезопасности, финальности всех членов и стабильности hash. Эти контракты отдельно раскрываются в [CORE-IMMUTABLE](immutability.md).

## 2. Правила прямого наследника и точность permits (S-02, S-04, S-12, S-14)

Когда sealed declaration явно содержит `permits A, B`, каждый указанный тип обязан **непосредственно** наследовать/реализовать sealed родителя. Нельзя внести в список только внука, ошибочно принимая его за непосредственного сына. Нельзя также добавить прямого наследника, не включённого в список. У обычного sealed-класса или sealed-интерфейса должен быть хотя бы один допустимый прямой потомок: пустой неявный список приводит к ошибке компиляции. Неявно sealed enum рассматривается отдельно (§5).

Разрешённый непосредственный subclass или subinterface должен явно либо неявно определить продолжение иерархии: **`final`, `sealed` или `non-sealed`**. Record как класс неявно final, потому подходит sealed interface. Но `class Pending implements Result {}` без какого-либо из трёх вариантов недопустим. Интерфейс, в отличие от класса, не может быть final: direct subinterface выбирает sealed или non-sealed. У enum возможен неявный sealed или final; у обычного класса без применимого неявного модификатора отсутствие требуемого явного модификатора — ошибка. Для прямого subinterface нельзя заменять sealed/non-sealed на final.

~~~java
sealed abstract class Operation permits Read, Write {}
sealed class Read extends Operation permits ReadOne {}
final class ReadOne extends Read {}
non-sealed class Write extends Operation {}
final class WriteLater extends Write {}
~~~

Здесь допустимы оба прямых подтипа Operation, а также их косвенные наследники. Конструкция `sealed abstract class` законна: абстрактность относится к созданию экземпляра, sealed — к перечню прямых потомков. Сам `ReadOne` не обязан находиться в списке Operation.permits: он прямой потомок Read.

**Отрицательные случаи, компилятор:** `class Unlisted extends Operation`; обычный `class Child extends Operation`, не выбравший модификатор; `sealed final class Both`; `class Plain permits Child`; `non-sealed class Loose` без sealed supertype. Их нельзя объединить в один позитивный файл: каждый относится к отдельной diagnostic category. В G1 это N01–N06, N20–N22. Здесь нет runtime exception — программа вообще не проходит javac.

В явном `permits` используются имена типов (TypeName), а не параметризации или «все наследники из пакета». Не требуется писать именно canonical name: доступное простое имя вложенного класса подходит, если разрешается в нужный тип. В `package p; sealed interface R permits D, p.D {}` два имени обозначают один тип, поэтому это дубликат. `permits C<String>` и `permits C<?>` недопустимы грамматически. В неявном permits действует другое правило — у кандидата должно существовать canonical name (§3).

Разрешение в permits необходимо, но недостаточно: имя типа должно разрешаться и быть доступным, непосредственная связь — законной, а конструктор потомка должен законно вызвать конструктор предка. Например, в теле одного внешнего класса допустимы `sealed static class PrivateRoot permits Hidden { private PrivateRoot() {} }` и `private static final class Hidden extends PrivateRoot {}`. Это фактически скомпилировано и создан экземпляр Hidden: доступ к private-конструктору разрешён в пределах объемлющего top-level класса. В отдельном top-level классе такой вызов private-конструктора запрещён; отдельно проверено и отклонение недоступного private nested child в permits другого top-level класса. См. [правила доступа](access.md) и [инициализацию](initialization.md), [JLS §6.6.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6.1).

## 3. Неявный permits: same compilation unit и canonical name (S-03, S-14)

Можно не писать `permits`, если разрешённые **прямые** типы объявлены в той же *compilation unit* (обычно один исходный файл) и имеют canonical names. Тогда компилятор формирует разрешённый список из подходящих объявлений; он **не сканирует classpath, другие файлы пакета или исходники других модулей**. Если подходящих объявлений нет, inferred set пуст и javac отклонит объявление (проверено отдельно для обычного класса и интерфейса).

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

`Word` и `Marker` — прямые реализации с canonical names, объявленные рядом с `Token`. Переезд `Marker` в другой файл без явного разрешения меняет корректность программы. Локальные классы, локальные интерфейсы и анонимные классы не участвуют в таком выводе: у них нет canonical name. Не путайте *nested* class, у которого canonical name обычно есть, с *local* class внутри метода.

По [JLS §14.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.3) локальному объявлению запрещено явно указывать sealed/non-sealed, а локальному классу или интерфейсу — непосредственно наследовать sealed-тип. Обычный анонимный класс также не может иметь непосредственный sealed-супертип. Это не запрещает локальные и анонимные реализации через открытый промежуточный тип: если `non-sealed interface A extends R` разрешён R, то внутри метода допустимы `class Local implements A {}`, `interface LocalInterface extends A {}` и `new A() {}`. В фактическом Probe локальный интерфейс наследовал другую открытую ветвь B; обе открытые ветви проверены. Отдельный случай неявно sealed локального enum с телом константы допустим (§5); локальный record также допустим, но запрет прямой связи с sealed-супертипом остаётся.

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

Проверенная G1 команда: `javac --release 21 -Xlint:all -d out module-info.java p/Root.java q/Good.java` — **exit 0 без warnings** на OpenJDK 21.0.12.1. Эти два небольших Java-блока **намеренно отдельные файлы**, не единая compilation unit. **M01 не запускался через JVM `--module-path`**, здесь не утверждается, что модульное приложение целиком тестировалось. Доступность типов при выполнении и экспорт пакетов — отдельная задача JPMS. В D-A16a обе выбранные компиляции N24/M01 повторены на том же JDK с теми же объявлениями (временные файлы с упрощёнными пробелами/переносами); N24 exit 1 с той же категорией, M01 exit 0, оба потока компилятора пусты для M01. Правило совместного модуля не экспортирует package q и не делает private или package-private тип доступным произвольному клиенту: правила JPMS и обычной доступности проверяются независимо. См. [CORE-MODULES](modules.md).

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

В `permits Success` указывается объявление Success, а не его параметризация. Для исчерпывающего switch важен статический тип селектора, например следующий фрагмент из отдельного фактического Probe (вложенные объявления и метод):

~~~java
sealed interface Generic<T> permits Text,Value {}
record Text(String s) implements Generic<String> {}
record Value<T>(T n) implements Generic<T> {}
static int value(Generic<Integer> g) {
    return switch(g) { case Value<Integer> v -> v.n(); };
}
~~~

Вызов `value(new Value<>(67))` дал 67. Text реализует Generic<String>, поэтому не является вариантом Generic<Integer> в корректно типизированном коде; для этого селектора достаточно Value<Integer>. Это проверка статической применимости и покрытия, а не появление reified generic arguments во время выполнения. Для другого статического типа селектора набор необходимых ветвей может измениться. [JLS §14.11.1.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11.1.1) задаёт критерий; более общие правила применимости generic patterns — в [CORE-PATTERNS](patterns.md) (PM-22). Этот пример и контраст конкретного класса в §6 раскрывают PM-05.

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

По [JLS §9.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.8) sealed-интерфейс не является функциональным. Но разрешённый non-sealed подинтерфейс может быть функциональным, если удовлетворяет обычному контракту SAM (в частности, согласованности абстрактных методов):

~~~java
sealed interface Task permits OpenTask { int run(); }
@FunctionalInterface non-sealed interface OpenTask extends Task {}
~~~

В отдельном Probe `OpenTask lambda = () -> 67` и `OpenTask ref = Probe::code` (static метод code возвращает 67) законны. После расширяющего преобразования `Task task = lambda` вызов task.run() даёт 67; method reference через Task также даёт 67. Анонимная и локальная реализации OpenTask дали 68 и 69. Лямбда и ссылка на метод имеют target type OpenTask, а не Task; последующее присваивание родительскому типу не создаёт нового прямого наследника. Прямая lambda с target Task и аннотация @FunctionalInterface на sealed-родителе проверены как отдельные compile errors. Не всякий non-sealed подинтерфейс автоматически становится SAM.

**Enum:** по [JLS §8.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.9) enum неявно final, если ни одна константа не имеет собственного тела класса; иначе enum неявно sealed, а его разрешённые прямые потомки — анонимные классы констант с такими телами. Явные final, sealed и non-sealed в объявлении enum запрещены (проверено тремя независимыми отрицательными компиляциями).

В Probe `enum Plain implements Kind { A }` и `enum Fancy implements Kind { A { @Override int code(){return 67;} }; int code(){return 0;} }`, где Kind — sealed-интерфейс, разрешающий Plain, Fancy и record Item, дали следующие результаты: `Plain.class.isSealed()` — false, `Fancy.class.isSealed()` — true, `Fancy.A.code()` — 67. Это наблюдение только [Class.isSealed()](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html#isSealed()) и переопределения метода, не полная проверка Reflection. Локальные enum с телом константы и record без прямой sealed-связи также скомпилированы и дали 67:67. Enum и record могут реализовывать подходящий sealed-интерфейс при соблюдении правил прямых подтипов, но не могут явно наследовать произвольный sealed-класс: их базовые классы заданы языком (Enum и Record). Это контраст внутри статьи о sealed; конструкторы, identity и специализированные коллекции — в [CORE-ENUMS](enums.md), а Reflection — в [отдельной теме](reflection.md). Историческая инструкция до G5 об обновлении curriculum относилась к публикации исходной статьи, не к действиям читателя.

## 6. Зачем sealed помогает исчерпывающему switch (S-01–S-05, S-11, S-16)

Компилятор способен оценивать coverage всех разрешённых ветвей sealed hierarchy. Если у `Result` ровно три permitted direct subtypes `Ok`, `Err` и `Pending`, достаточно трёх типовых ветвей, даже если существует `Later extends Pending`. Такой вывод о покрытии разрешённых ветвей применим к sealed-интерфейсу или абстрактному sealed-классу при учёте статического типа селектора (§5); для конкретного sealed-класса одного покрытия потомков недостаточно.

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

**Конкретный sealed-класс.** В следующем фрагменте из фактического Probe Root сам не abstract, его конструктор доступен в данном контексте, поэтому экземпляр Root тоже возможен:

~~~java
sealed static class Root permits Leaf {}
static final class Leaf extends Root {}
static String classify(Root r) {
    return switch(r) {
        case Leaf l -> "leaf";
        case Root x -> "root";
    };
}
~~~

Вызовы classify(new Root()) и classify(new Leaf()) дали root и leaf. Если оставить только case Leaf, отдельный контекстуальный исходник ConcreteSwitch отклоняется с compiler.err.not.exhaustive. Нужна ветка, покрывающая сам Root: здесь case Root после Leaf; вместо неё возможно default. Для абстрактного sealed-класса экземпляр непосредственно самого класса невозможен, и покрытие применимых прямых ветвей может быть достаточно. Для интерфейса Answer выше открытая ветвь PendingAnswer покрывает также косвенные реализации. Для параметризованного селектора (§5) некоторые разрешённые объявления могут оказаться неприменимыми. Это контрасты PM-05, не полный разбор dominance, null и guards.

**G1 положительный исходник `SealedProbe.java`:** 11 подписанных строк `S01=ok:67`, `S02=err:bad`, `S03=pending` (косвенный Later), `S04=token:A` и `S05=marker` (implicit permits), `S06=true` (sealed abstract State), `S07=true` (`Class.isSealed()`), `S08=true` (record), `S09=true` (Later instanceof Pending), `S10=generic` и `S11=1` (после изменения счётчика в Pending). Это **11 наблюдений одного Java-процесса, не 11 тестов**. Принятый исходник и точный stdout приведены целиком в [G1 §7.1](../../docs/plans/CORE-SEALED-PATTERNS.md). Для самостоятельной проверки на Java 21 используйте [общий runnable Sealed + Patterns](../../examples/core/sealed-patterns/README.md): [SealedProbe.java](../../examples/core/sealed-patterns/src/SealedProbe.java) с 11 подписанными строками и [PatternsProbe.java](../../examples/core/sealed-patterns/src/PatternsProbe.java) с 20 строками; отдельный `verify.sh` сверяет два stdout с двумя эталонами через `diff -u`. Это 31 наблюдение **двух** JVM-процессов, не 31 JUnit-тест.

## 7. Чем compile error отличается от runtime (S-02, S-04, S-07, S-10, S-12)

| Случай | Этап | Причина |
|---|---|---|
| Неразрешённый direct наследник sealed parent | **javac error** | Не в permits |
| Direct child без явного или неявного final/sealed/non-sealed | **javac error** | Нет необходимого модификатора; у обычного класса без неявного варианта нужен явный |
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

Ограничение прямых подтипов влияет и на допустимость сужающего преобразования. Пусть `sealed interface R permits C {}`, `final class C implements R {}` и независимый `interface X {}`. Тогда у R и X не может быть общего ненулевого экземпляра: C не реализует X, а других вариантов нет. По [JLS §5.1.6.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.6.1) они статически несовместимы для такого сужения; `r instanceof X` и `(X) r` отклонены двумя отдельными компиляциями, где r имеет тип R.

Если R разрешает открытые A и B, как в §1, класс Shared может реализовать A и Other. При `R open = new Shared()` проверка `open instanceof Other` дала true, сужающий cast успешен; при `R neither = new Both()` (Both не реализует Other) проверка дала false, допустимый по статическим типам cast бросил ClassCastException. Следовательно, compile error из-за доказанной несовместимости типов отличается от проверки конкретного объекта при исполнении. Не следует выводить, что все sealed-соседи непересекающиеся или что JVM обязательно оптимизирует проверку. См. [преобразования типов](conversions.md).

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

**Фактические проверки D-A16a (02).** Временный Probe.java вне репозитория скомпилирован на Debian 13 OpenJDK 21.0.12.1 командой `javac --release 21 -encoding UTF-8 -Xlint:all -Werror -XDrawDiagnostics` и запущен отдельной JVM: exit 0 в обоих случаях, stdout/stderr компилятора и stderr JVM пусты. Получено 11 строк stdout (одна метка ожидаемого ClassCastException с проверкой точного класса, десять других наблюдений/меток), SHA-256 `2e6bab7f947712a90d5ac63c9fe376c5404461fc2ab8a3a0cbfd8d3a94a2062d`. Новые 27 изолированных контекстуальных отрицательных компиляций и межпакетная N24 (итого 28) дали exit 1 и тематические diagnostics, отдельно M01 exit 0 без запуска JVM. Это не повтор всех исторических N01–N23; полные исходники, команды и фактические результаты — [приложение проверки 02](https://github.com/Melchiorius/backend-interview-labs/issues/375#issuecomment-7239672243).

Общий runner проверен без инструментирования на пяти восстановленных входных файлах с исходными Git blob SHA и режимами (не полный клон). В исходном окружении, LC_ALL=C/TZ=Pacific/Honolulu и LC_ALL=C.utf8/TZ=Asia/Tokyo все три запуска завершились с exit 0 и пустым внешним stderr. Runner использует одну компиляцию javac --release 21 -Xlint:all двух исходников и проверяет отсутствие stderr компилятора; далее две JVM, два diff -u с эталонами и вывод обеих фактических трасс плюс итоговый маркер. Сам runner не проверяет отдельно stderr JVM, число строк или SHA-256. Авторская внешняя сверка подтвердила S11/111 байт и P20/279 байт с полными эталонами, одинаковый внешний stdout32/473 байта (31 наблюдение и маркер), SHA-256 `e929cf66fe84455c8bab9469e49a5759d5bb4ff7b941c5611d0c363c39c6efed`. В отдельном Probe измерены en_US/Etc/UTC/UTF-8, en_US/Pacific/Honolulu/UTF-8, en/Asia/Tokyo/UTF-8, не настройки внутри runner JVM; доступны только C, C.utf8, POSIX. Исторические 24 отрицательные компиляции G1, 24 маркера EXPECTED REJECTION полного CI и новые 28 компиляций — разные проверки, их не суммируют. Не все встроенные фрагменты компилировались отдельно.

**NOT RUN:** автоматическая компиляция всех inline Java-блоков, негативные N01–N24 в GitHub CI, JVM module-path, separate binary evolution, GUI Obsidian/IntelliJ, альтернативные JDK, полноценные Reflection/углублённые enum internals в рамках CORE-SEALED, JIT/GC/JMH и иные P2-эксперименты.

## Самопроверка

1. R разрешает A, A объявлен non-sealed. Законны ли `class C extends A` и `class C extends A implements R`, если R — интерфейс, A — класс?
2. Root — конкретный sealed-класс с единственным разрешённым Leaf. Достаточна ли ветка case Leaf для switch-expression по Root?
3. Generic<Integer> — селектор из §5. Нужна ли ветка Text? Почему?
4. У sealed-интерфейса Task один абстрактный метод, а разрешённый OpenTask — non-sealed SAM. Какой тип может быть целевым для lambda?

> [!note]- Ответы
> 1. Первый вариант допустим при обычных условиях наследования; второй вводит запрещённую непосредственную связь с R (§1).
> 2. Нет: экземпляр самого Root возможен; нужен, например, case Root после Leaf или default (§6).
> 3. Нет: Text реализует Generic<String>, несовместимый с данным статическим типом селектора (§5).
> 4. OpenTask, затем значение можно присвоить переменной Task; непосредственно Task не является SAM (§5).
