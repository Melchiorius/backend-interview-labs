# CORE-IMMUTABLE — неизменяемость объектов Java 21: владение ссылками, защитные копии и инварианты

**Область:** Java SE 21, без preview. Эта статья — о **наблюдаемом поведении объекта и его достижимого состояния**, а не об одной языковой конструкции. Зависимости: [типы и final](types.md), [ссылки и передача аргументов](parameters.md), [обычные классы и конструкторы](classes.md), [массивы и shallow copy](arrays.md), [равенство](equality.md). Для специального контракта record читайте отдельно [CORE-RECORDS](records.md). Сравнить оба подхода на фактически запускаемом Java 21-коде помогает [общий runnable Records + Immutable](../../examples/core/records-immutability/README.md): `RecordsProbe.java` R01–R16 и `ImmutableProbe.java` I01–I10, два запуска и два независимых эталона (26 наблюдений, **не 26 тестов**).

**Исследовательский G1:** [принятая матрица CORE-RECORDS + CORE-IMMUTABLE](../../docs/plans/CORE-RECORDS-IMMUTABLE.md), **I-01–I-19 = 12 P0 / 4 P1 / 3 P2**. Нормативные источники — [JLS 21 §4.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4), [JLS §8.3.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.3.1.2), [List.copyOf](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html#copyOf(java.util.Collection)), [Collections.unmodifiableList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#unmodifiableList(java.util.List)), [Arrays.copyOf](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html). Здесь цель — проектирование корректной внешней модели, а **не** автоматическое доказательство safe publication в JMM, глубокого клонирования любых графов или производительности.

**Воспроизведение:** из корня репозитория с JDK 21 и Bash: `bash examples/core/records-immutability/verify.sh`. Новые фрагменты проверены в отдельной временной программе: [Probe.java, команды, вывод и хеши](https://github.com/Melchiorius/backend-interview-labs/issues/366#issuecomment-7231597243). Они не входят в общий runner; I-xx карты требований и Ixx строк stdout — разные идентификаторы.

## 1. Что значит «неизменяемый объект» и где его граница (I-01, I-02)

**Наблюдаемая неизменяемость** означает: после успешного конструирования, используя доступные клиенту операции и ссылки, нельзя изменить состояние объекта, которое он обещает считать своим значением. Важно не только, присваиваются ли его поля заново, но и могут ли **другие ссылки** изменить *достижимые через эти поля объекты*.

Рассмотрим объект как граф ссылок, не как строку ключевых слов. Пусть у пользователя есть `List<String> source`, а созданный объект хранит ссылку на тот же список. Даже если поле списка объявлено `final`, пользователь всё ещё может выполнить `source.add("B")`. Внешнее состояние изменится, а метод объекта, читающий список, увидит обновление. Поле-то осталось с прежней ссылкой.

~~~java
import java.util.ArrayList;
import java.util.List;

final class LooksImmutable {
    private final List<String> names;

    LooksImmutable(List<String> names) {
        this.names = names;               // передана и сохранена та же ссылка
    }
    List<String> names() { return names; } // ссылка выходит наружу
}

class MutableThroughAlias {
    public static void main(String[] args) {
        List<String> source = new ArrayList<>(List.of("A"));
        LooksImmutable value = new LooksImmutable(source);
        source.add("B");
        System.out.println(value.names()); // [A, B], хотя поле final
        value.names().add("C");
        System.out.println(source);        // [A, B, C]
    }
}
~~~

**Кто что меняет:** `source` у вызывающего и `names` в объекте — **две ссылки на один изменяемый ArrayList**; после `add` изменяется объект списка, а не присваивание финальному полю. Выданный accessor создаёт ещё одну копию той же ссылки. `final` не означает *deep immutability* и не предотвращает изменение элемента списка или массива.

**Граница:** сам объект может быть *immutable относительно конкретного API*, даже если его реализация содержит внутренний изменяемый кэш при сохранении наблюдаемой семантики. Это требует собственного контракта и доказательства, а не лозунга «ни одного mutable поля». В этой статье разбираются простые значения, где неизменяемость устанавливается конструктором и защитой границ владения.

Неизменяемость оценивают по всему наблюдаемому API: унаследованным и переопределённым методам, выдаваемым ссылкам и обратным вызовам (callbacks). Отсутствие setters и private final поля не исключают изменения наблюдаемого значения. В Probe объявлены вложенные классы:

~~~java
static class Base { private final int n=67; int value(){return n;} }
static final class MutableSub extends Base { int m=67; @Override int value(){return m;} }
~~~

Клиент с `Base b = sub` видит через `b.value()` сначала 67, а после `sub.m=1` — 1. Поле Base.n не менялось. Если контракт требует неизменяемости всех допустимых экземпляров, надо исключить небезопасное переопределение или доказать сохранение инварианта разрешёнными подтипами. `final class` — простой вариант, но не единственный: возможны контролируемое конструирование и ограниченная иерархия с доказанным поведением. Даже final класс должен контролировать выдаваемые ссылки и callbacks. См. [наследование](inheritance.md).

## 2. Constructor invariant и правило владения на входе (I-03, I-10)

**Инвариант** — условие, которому должен удовлетворять каждый *успешно сконструированный* объект и которое сохраняют его публичные операции. Например: массив содержит хотя бы одну оценку, список пользователей не содержит `null`, число кредитов не отрицательно. Значение нужно **проверить до публикации** корректного объекта.

Но одной проверки мало: если вы сохранили внешнюю изменяемую ссылку, вызывающий может изменить ранее проверенное значение **после** конструктора. В частности, `Objects.requireNonNull(list)` удостоверяет лишь, что сама ссылка не null; оно не делает список неизменяемым и не копирует элементы.

Для входного массива нужно определить: кто владеет исходной ссылкой? В типичном API вызывающий **сохраняет право изменять свой массив**, а значит конструктор не должен его просто запоминать.

~~~java
import java.util.Objects;

final class Scores {
    private final int[] values;

    Scores(int[] input) {
        Objects.requireNonNull(input, "input");
        if (input.length == 0) {
            throw new IllegalArgumentException("empty");
        }
        this.values = input.clone(); // COPY-IN: владеем собственным внешним массивом
    }

    int first() {
        return values[0];
    }

    int[] values() {
        return values.clone(); // COPY-OUT: не отдаём внутреннее хранилище
    }
}

class ScoresExample {
    public static void main(String[] args) {
        int[] input = {67};
        Scores scores = new Scores(input);
        input[0] = 1;                  // изменяем только внешний массив
        scores.values()[0] = 2;        // изменяем только копию, полученную из accessor
        System.out.println(scores.first()); // 67

        try {
            new Scores(new int[0]);
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage()); // empty
        }
    }
}
~~~

**Шаги по ссылкам:** конструктор проверяет длину исходного `input`, создаёт `clone` и сохраняет его в приватное финальное поле. После создания у клиента остаётся `input` — другая область хранения. При чтении `scores.values()` возвращается **ещё один новый массив**, не ссылка на внутренний `values`. Поэтому обе внешние модификации не меняют результат `first()`.

**Если опустить copy-in**, изменение `input` поменяет состояние объекта после проверки. **Если опустить copy-out**, клиент сможет получить внутренний массив из accessor и изменить его в обход методов. Для полного контроля mutable array нужны **обе стороны**. Но это только модель обычного `final class`; для record с array-компонентом дополнительно действует [record copy invariant](records.md), поэтому тот же шаблон нельзя переносить механически.

**Граница модели:** этот конструктор предполагает, что во время проверки и копирования массив не меняют конкурентно другие потоки. Если другие потоки уже используют одну ссылку и мутируют её без синхронизации, одной последовательности `check → clone` недостаточно для доказательства отсутствия гонки. Это отдельный сценарий JMM/публикации (§9).

Проверка длины Scores корректна: длина массива не изменяется. Если проверяется **содержимое** изменяемого входа (например, неотрицательность чисел), проверка чужого объекта до копирования может проверить не те данные, которые будут сохранены. Получите принадлежащий объекту снимок, затем проверяйте его и только после успеха сохраняйте результат.

В Probe намеренно нестабильный `ChangingList extends AbstractList<Integer>` имеет size()=1, но get(0) при первом чтении возвращает 67, при следующих — -1. Это последовательная модель изменения между чтениями, не обычный стабильный список или гонка потоков. Два способа подготовки значения:

~~~java
static void validate(List<Integer> values){for(int n:values)if(n<0)throw new IllegalArgumentException("negative");}
static List<Integer> bad(List<Integer> input){validate(input);return List.copyOf(input);}
static List<Integer> good(List<Integer> input){List<Integer> owned=List.copyOf(input);validate(owned);return owned;}
~~~

С отдельным новым ChangingList для каждого вызова: bad сохраняет [-1], good сохраняет [67]; `good(List.of(-1))` обязательно бросает IllegalArgumentException (отсутствие исключения считалось провалом). В конструкторе поле присваивается результату good после проверки. Для сложного содержимого снимок должен включать нужные изменяемые компоненты, а не только ссылки. Нужны стабильность источника на время чтения и доверенное преобразование; этот порядок не гарантирует атомарный снимок конкурирующего графа или универсальную защиту от враждебного кода.

## 3. Defensive copy-in и copy-out: у массивов копия внешней оболочки shallow (I-04, I-09)

`clone()` для массива создаёт новый массив того же runtime-класса и длины. Двухаргументный `Arrays.copyOf(original, newLength)` сохраняет runtime-класс исходного массива, но задаёт новую длину, обрезая или дополняя значения; перегрузка для ссылочных массивов с `newType` задаёт класс результата явно. См. [JLS §10.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html#jls-10.7) и [Arrays.copyOf](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html). Однако для массива **ссылок** элементы копируются как **те же ссылки на вложенные объекты**, а не рекурсивно.

~~~java
import java.util.Arrays;

class ShallowCopyBoundary {
    public static void main(String[] args) {
        int[][] matrix = {{1, 2}};
        int[][] outer = matrix.clone();
        System.out.println(outer != matrix);         // true: внешние массивы разные
        System.out.println(outer[0] == matrix[0]);   // true: внутренняя строка общая

        matrix[0][0] = 67;
        System.out.println(outer[0][0]);            // 67: alias сохраняется

        int[] extended = Arrays.copyOf(new int[]{1}, 3);
        System.out.println(Arrays.toString(extended)); // [1, 0, 0]
    }
}
~~~

Для `int[]` одна копия массива изолирует его элементы, потому что элементы — **примитивные значения**. Для `int[][]`, `StringBuilder[]`, `List<MutableElement>` или `List<List<String>>` нужно **отдельно определить, какие вложенные объекты должны быть независимыми**, и копировать их согласно этому требованию. Многомерный массив — [массив массивов](arrays.md), а не непрерывная матрица по контракту языка.

Если поле объекта содержит `int[][]`, недостаточно вернуть `field.clone()` и объявить весь граф глубоко неизменяемым: вызывающий сможет изменить `returned[0][0]`, потому что внутренний массив разделяется с исходным. Аналогично, `Collections.unmodifiableList` может защищать операции списка, но не делает вложенный `StringBuilder` неизменяемым.

## 4. Список-обёртка и структурный snapshot — принципиально разные контракты (I-05, I-06, I-14)

Здесь часто путают три вещи:

1. **Изменяемый исходный список.** Его хозяин может выполнять `add/remove/set`.
2. **`Collections.unmodifiableList(source)`** — *unmodifiable view*, через который нельзя применять операции изменения списка, включая замену элемента, но view **поддерживается исходным списком**: изменения source остаются видимы.
3. **`List.copyOf(source)`** — *unmodifiable snapshot* элементов source **на момент копирования**, при условии отсутствия null; последующие изменения последовательности ссылок в source (в том числе замена элемента) не отражаются в результате. Это **не рекурсивная копия элементов**.

Те же различия между связанным с исходником представлением и независимой немодифицируемой структурой сопоставлены в [контрактах коллекций Java 21](../02-collections/contracts.md), а пример фиксированного списка `Arrays.asList` и отдельной изменяемой копии разобран в [COL-SEQUENTIAL](../02-collections/sequential.md); в [примере Collections C1](../../examples/collections/contracts-hash/README.md) это наблюдается на изменяемом `StringBuilder` внутри `List.copyOf`.

~~~java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ViewsVersusSnapshots {
    public static void main(String[] args) {
        List<String> source = new ArrayList<>(List.of("A"));
        List<String> view = Collections.unmodifiableList(source);
        List<String> snapshot = List.copyOf(source);

        source.add("B");
        System.out.println(source);    // [A, B]
        System.out.println(view);      // [A, B]: live backed view
        System.out.println(snapshot);  // [A]: структурный snapshot

        try {
            view.add("C");
        } catch (UnsupportedOperationException e) {
            System.out.println("view rejects add");
        }
        try {
            snapshot.add("C");
        } catch (UnsupportedOperationException e) {
            System.out.println("snapshot rejects add");
        }
    }
}
~~~

**Почему две разные причины:** `view.add` запрещён как модификатор через wrapper, но `source.add` всё ещё меняет *базовый список* и виден через view. У snapshot собственная семантика неизменяемой последовательности элементов: добавление, удаление или замена элемента source не меняют последовательность элементов snapshot.

**Замена — не то же самое, что изменение размера.** Для ArrayList вызов `set(0, "67")` заменяет элемент без изменения размера, но это мутация списка. В Probe исходный список [A, B], view = unmodifiableList(source), snap = List.copyOf(view). После source.set(0,"67") view стал [67, B], snap остался [A, B], view.reversed() дал [B, 67]. После добавления C размеры стали 3 и 2 соответственно.

Через немодифицируемую обёртку нельзя изменить последовательность ни напрямую, ни через полученные из неё представления и итераторы. В Probe для view, snap и обёртки собственного списка (ниже) отдельно проверены set, add, listIterator().set после next(), subList(0,1).set и reversed().set. Все 15 вызовов с допустимыми индексами и ненулевыми значениями бросили UnsupportedOperationException; отсутствие исключения считалось ошибкой. Это выбранные операции, не исследование всех методов, обхода или конкурентной модификации. См. [List Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html) и [Collection](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collection.html).

Обёртка может быть способом построить неизменяемое значение при **исключительном владении** базовым списком: `Collections.unmodifiableList(new ArrayList<>(source))`. Если новая копия нигде не раскрывается и не изменяется, а элементы сами устойчивые значения (здесь String), внешний source не управляет её состоянием. В Probe owned сохранил [A, B] после source.set и source.add, как и snap. Это не свойство произвольной unmodifiable view и не глубокая копия mutable elements.

**Важно не обещать новую identity:** API `List.copyOf` допускает переиспользование уже подходящего немодифицируемого списка. В историческом общем примере G1, `ImmutableProbe.java`, строка `I08` печатает результат сравнения `source == safe.names()`: фактически получено `I08=false`. Это не гарантия новой идентичности при каждом вызове copyOf. Главное — контракт снимка последовательности ссылок, а не обязательное выделение нового объекта или глубокая неизменяемость графа.

В новой отдельной пробе роли 02 `snap = List.copyOf(view)` не отражает последующие изменения последовательности исходного списка через его внешнюю ссылку, а сравнение `List.copyOf(snap) == snap` дало `reuse=true`. Это другое наблюдение, не гарантия переиспользования любого немодифицируемого списка. Списки, полученные через `List.of` и `List.copyOf`, являются value-based: используйте логическое равенство, не полагайтесь на конкретную идентичность и не используйте их как мониторы синхронизации. Это контракт API.

**Null:** `List.copyOf` запрещает null-элементы и null-вход; при таком входе возникает **runtime `NullPointerException`**. Для `Collections.unmodifiableList` null-вход тоже бросает NPE, но null-элемент допустим, если допустим в базовом списке. В Probe отдельно потребованы NPE от List.copyOf(null), Collections.unmodifiableList(null), List.copyOf(Arrays.asList("A",null)) и List.of("A",null). Nullable view и owned wrapper над новой ArrayList сохранили null на позиции 1; это допустимый снимок при такой политике домена. Различайте null-компонент record, который может быть законен, и контракт метода copyOf, который запрещает null в коллекции (§6).

~~~java
import java.util.Arrays;
import java.util.List;

class CopyOfNull {
    public static void main(String[] args) {
        try {
            List.copyOf(Arrays.asList("A", null));
        } catch (NullPointerException e) {
            System.out.println("NPE from copyOf");
        }
    }
}
~~~

Этот пример **компилируется**; NPE происходит в методе при выполнении, а не на этапе `javac`.

## 5. Mutable элементы и вложенные ссылки: unmodifiable не значит deeply immutable (I-07, I-13)

Структурно unmodifiable список может содержать изменяемые объекты. Если другой участник держит ссылку на вложенный элемент, он способен изменить его, даже когда `snapshot.add` запрещён. **List.copyOf копирует ссылки на элементы, а не сами элементы**.

~~~java
import java.util.List;

final class MutableCounter {
    int value;
    MutableCounter(int value) { this.value = value; }
}

class ShallowListElements {
    public static void main(String[] args) {
        MutableCounter element = new MutableCounter(1);
        List<MutableCounter> snapshot = List.copyOf(List.of(element));
        element.value = 67;
        System.out.println(snapshot.get(0).value); // 67
        // snapshot.add(element); // компилируется, но runtime UOE при выполнении
    }
}
~~~

**Как сделать содержимое действительно устойчивым:** определите, что является *значением* вложенного элемента. Если это собственный immutable value object, часто достаточно snapshot структурных ссылок (например, `List<String>`, поскольку String immutable). Если элемент — mutable DTO, нужно скопировать или преобразовать каждый элемент в устойчивое представление, **затем** построить unmodifiable коллекцию. Возврат `List.copyOf(mutables)` сам по себе не выполняет deep copy.

Второй пример — **самостоятельная единица компиляции**: для него повторно приведён минимальный `MutableCounter`. Копируйте и запускайте каждый из двух Java-блоков отдельно, а не объединяйте их в один файл (иначе два объявления `MutableCounter` конфликтуют).

~~~java
import java.util.List;

final class MutableCounter {
    int value;
    MutableCounter(int value) { this.value = value; }
}

record CounterValue(int count) {}

class CopyMutableElements {
    static List<CounterValue> freeze(List<MutableCounter> source) {
        return source.stream()
                .map(e -> new CounterValue(e.value))
                .toList();       // структурно unmodifiable List в Java 21
    }

    public static void main(String[] args) {
        MutableCounter element = new MutableCounter(1);
        List<CounterValue> frozen = freeze(List.of(element));
        element.value = 67;
        System.out.println(frozen.get(0).count()); // 1
    }
}
~~~

Здесь текстовое требование — **сохранить значение числа на момент последовательного преобразования**, а не «сделать копию произвольного объектного графа». `Stream.toList()` возвращает unmodifiable список по контракту Java 21, а `CounterValue(int)` хранит примитивное значение, поэтому внешняя мутация `MutableCounter` больше не влияет на него. Важно: если исходные mutable элементы параллельно изменяют в момент чтения, нужен согласованный протокол синхронизации; один `map` не даёт консистентный snapshot конкурирующего графа.

Есть и **выходной alias**: если accessor возвращает unmodifiable список, `get(0)` всё равно может вернуть изменяемый объект. Внешняя копия вложенного списка не защищает внутренние списки. В Probe Counter с n=67 дважды включён в изменяемый row, а row — в input. `shallow = List.copyOf(input)` защищает внешнюю последовательность, но `shallow.get(0).get(0).n=1` изменяет исходный Counter (вторая ссылка тоже видит 1), а row.add(new Counter(2)) меняет размер видимой внутренней строки на 3.

Для модели «список строк из счётчиков» определим значение как последовательность чисел, а не граф идентичностей. Фрагмент выполненной Probe (Counter — изменяемый класс с полем int n):

~~~java
record Value(int n) {}
static List<List<Value>> freeze(List<List<Counter>> input){
    return List.copyOf(input.stream().map(row->List.copyOf(row.stream().map(c->new Value(c.n)).toList())).toList());
}
~~~

Каждый mutable элемент заменяется новым Value, каждая внутренняя и внешняя последовательность становится немодифицируемой. Повторная ссылка на Counter превращается в два равных значения, сохранение identity/топологии не требуется. Null-вход, null-строка и null-элемент не допускаются (в Probe отдельно проверены последние два). После обеих мутаций ранее построенный frozen сохранил два Value(67); add к внутреннему и внешнему списку отвергнут UOE. Нужна стабильность входа; это ограниченное преобразование значений, не алгоритм копирования произвольного графа.

## 6. Пример безопасного record с `List<String>` и пределы record (I-11, I-12)

В record component fields неявно `final`, но поле типа `List<String>` может хранить изменяемый список. Если весь публичный контракт — **значения строк** и порядок, compact constructor может запомнить `List.copyOf`. Это решает утечку структуры для данного типа элементов, не превращая record в универсальный immutable container.

~~~java
import java.util.ArrayList;
import java.util.List;

record UnsafeNames(List<String> names) {}

record Names(List<String> names) {
    Names {
        names = List.copyOf(names); // copy-in/snapshot, Strings сами immutable
    }
}

class SafeRecordList {
    public static void main(String[] args) {
        List<String> source = new ArrayList<>(List.of("A"));
        UnsafeNames unsafe = new UnsafeNames(source);
        Names safe = new Names(source);

        source.add("B");
        System.out.println(unsafe.names()); // [A, B]
        System.out.println(safe.names());   // [A]

        try {
            safe.names().add("C");
        } catch (UnsupportedOperationException e) {
            System.out.println("no add");
        }
    }
}
~~~

**Обратите внимание:** accessor `names()` возвращает уже unmodifiable snapshot, хранящийся в компоненте, и при реконструкции `new Names(safe.names())` сохраняется стандартный record copy invariant (поскольку сравнение List сравнивает соответствующие элементы). Здесь мы **не** переопределяли accessor так, чтобы он каждый раз создавал новый array с reference equality.

Для record с `int[]` другой случай. Если `values()` возвращает `clone`, но стандартный `equals` сравнивает ссылки массивов, реконструированный объект оказывается **не равен** исходному. [Подробный антипример BadAccessor](records.md) показывает R13=false — это **нарушение record copy invariant**. Даже copy-in массива **не** освобождает от согласования `equals/hashCode` и accessors. Если нужен скрытый внутренний массив с copy-in/out, обычный `final class Scores` из §2 зачастую проще.

**Решение о выборе:** record удобен, когда API сознательно прозрачен и состояние компонент адекватно представляет значение. Если требуется скрыть внешнее состояние, добавить собственные поля, гарантировать защитные копии/специальное равенство или управлять сложным жизненным циклом, обычный класс может дать более ясный контракт.

В [CORE-RECORDS §5](records.md) уже проверен `Good(int[] values)`: копии с обеих сторон, сравнение содержимого и согласованные hashCode/toString обеспечивают реконструкцию через accessor. Такой record подходит для прозрачного значения одного int[] при выбранной политике null. Если нужно скрыть представление, проще Scores из §2; для последовательности неизменяемых значений — Names со snapshot. Выбор зависит от контракта API и достижимых изменяемых объектов, а не от record или final в объявлении. Доказательства Good относятся к принятой доработке records.md, здесь не воспроизводились.

## 7. Идентичность, равенство и стабильность ключей Map (I-08, I-15)

**Идентичность ссылки (`==`)**, **значение (`equals`)** и **способность изменять объект** — три разных свойства. Unmodifiable коллекция может быть тем же объектом после copyOf, если уже соответствует требованиям; это не делает логическое значение «той же ссылкой обязано быть всегда». Кроме того, два разных immutable value objects могут быть равны по содержимому и иметь одинаковые хэши.

В качестве ключа `HashMap` часто выбирают значение с **устойчивым `equals/hashCode`**. Если ключ содержит изменяемый список или массив и равенство либо хэш зависят от его содержимого, после мутации поиск ключа может перестать работать ожидаемым образом.

У массива другая семантика: он наследует `Object.equals/hashCode`, то есть сравнивается по идентичности, а его хэш не зависит от изменения элементов. Неявное равенство record использует равенство ссылочных компонентов, а не содержательное `Arrays.equals`. При этом числовой хэш record и алгоритм его вычисления не фиксированы. Стабильность хэша компонента не означает неизменяемость данных; если пользовательское равенство и хэш зависят от элементов массива, внешний alias снова создаёт риск изменяемого ключа.

Для сортированных коллекций аналогичное требование относится к стабильности полей, участвующих в сравнении: см. [изменяемые ключи TreeMap и TreeSet](../02-collections/sorted.md).

~~~java
import java.util.HashMap;
import java.util.Map;

final class MutableKey {
    int n;
    MutableKey(int n) { this.n = n; }
    @Override public boolean equals(Object other) {
        return other instanceof MutableKey k && k.n == n;
    }
    @Override public int hashCode() { return Integer.hashCode(n); }
}

class KeyMutation {
    public static void main(String[] args) {
        MutableKey key = new MutableKey(67);
        Map<MutableKey, String> map = new HashMap<>();
        map.put(key, "value");
        key.n = 1; // теперь hashCode другой
        System.out.println(map.get(key)); // null для этой последовательности
    }
}
~~~

**Гарантия vs наблюдение:** [Map Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html) оставляет поведение неопределённым контрактом, если объект-ключ изменён способом, влияющим на equals, пока он находится в map. Не следует ожидать надёжного поиска при изменении состояния, участвующего в equals/hashCode. Показанный `null` — результат конкретного сценария, не гарантированный исход. Подробная теория контрактов — в [CORE-EQUALITY](equality.md).

**ListKey — равенство по содержимому списка.** В Probe ключом HashMap был record `ListKey(List<Integer> values)`. После замены 67 на 1 через исходный ArrayList хэш записи изменился (наблюдалось 98 → 32), равенство записи с прежним содержимым стало `false`, а `map.get(key)` вывел `null`. Это результат конкретного запуска, не обещание такого результата после любой мутации ключа.

**ArrayKey — равенство по идентичности массива.** Для `ArrayKey(int[] values)` после изменения элемента 67 → 1 хэш массива и хэш записи сохранились в этой проверке. Запись с той же ссылкой на массив была равна ключу, а запись с новым массивом `new int[]{1}` — нет. В этом запуске `am.get(ak)` вернул `"v"`, а `ak.values()[0]` — 1.

Устойчивый ключ по ссылочной семантике не превращает значение в immutable. Если переопределить равенство и хэш по содержимому массива, как в Good из records.md, но оставить внешний alias, возникает риск изменяемого ключа. См. [Object.equals/hashCode](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html) и [Record API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Record.html).

## 8. Произвольный граф и циклические ссылки — почему deep copy не одна строчка (I-16)

Объекты образуют граф: один элемент может ссылаться на другого; два поля могут ссылаться на **один и тот же** вложенный объект; возможна циклическая связь. Если при копировании нужно сохранить такие свойства, обычное рекурсивное `clone` без таблицы уже скопированных объектов может нарушить aliasing либо не завершиться на цикле. Нужно заранее определить:

- Какие вершины графа являются значениями, а какие — идентичностями?
- Должны ли две исходные ссылки на один объект остаться двумя ссылками на одну **копию**?
- Что делать с циклом, внешними ресурсами, null и взаимными ссылками?
- Должна ли копия отражать состояние на один момент времени при конкурентных мутациях?

Решение этих задач зависит от доменной модели; нельзя обещать «универсальная глубокая неизменяемость после `List.copyOf`» или «`Arrays.deepEquals` сравнит любые циклические графы». В этой статье мы умеем **выделять границы владения и копировать выбранные изменяемые компоненты**, но не разрабатываем универсальный графовый copy algorithm. В [CORE-ARRAYS](arrays.md) отдельно рассмотрены shallow и ограниченность deep-операций массивов.

Для ограниченной модели Node с числовым значением и двумя изменяемыми ссылками left/right, включая null, можно сохранить топологию. Используйте [IdentityHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/IdentityHashMap.html): ключом таблицы соответствия служит **идентичность исходного узла**, а не его equals. Иначе два разных равных узла могли бы слиться в одну копию.

Вложенные тип и метод — выполненный фрагмент Probe (нужен import java.util.IdentityHashMap):

~~~java
static final class Node {
    final int n; Node left,right; Node(int n){this.n=n;}
    public boolean equals(Object o){return o instanceof Node x && n==x.n;}
    public int hashCode(){return Integer.hashCode(n);}
}
static Node copy(Node n, IdentityHashMap<Node,Node> memo){
    if(n==null)return null;
    Node existing=memo.get(n);if(existing!=null)return existing;
    Node result=new Node(n.n);memo.put(n,result);
    result.left=copy(n.left,memo);result.right=copy(n.right,memo);return result;
}
~~~

Для каждого впервые встреченного **исходного** узла создаём копию, регистрируем пару «исходный узел → копия» по идентичности исходного узла в memo **до рекурсии**, затем копируем рёбра. Повторная встреча исходной ссылки возвращает уже созданную копию, в том числе при цикле.

В Probe три различных исходных узла `root`, `same`, `other` с `n=67` равны по `equals`. Исходная топология: `root.left=root`, `root.right=same`, `same.left=other`, `same.right=other`. После копирования проверены:

- новая идентичность каждого из трёх узлов относительно соответствующего исходного, сохранённая самоссылка `out.left==out` и общая копия other через два ребра;
- отсутствие слияния разных равных узлов: `out != out.right` и `out.right != out.right.left`;
- null-ребро и `copy(null,memo)==null`, три записи в memo и возвращение той же копии при повторном `copy(root,memo)`.

Для независимого снимка нужна новая таблица. Полученный граф всё ещё изменяем: поля `left/right` доступны для записи. Алгоритм относится к этому стабильному конечному графу; он не гарантирует достаточную глубину стека для произвольного размера, обработку ресурсов, безопасную публикацию или атомарность.

Для примера структурного снимка ссылок и границ безопасной публикации через конкурентную коллекцию см. [COL-CONCURRENT](../02-collections/concurrent.md); стоимость копирования и удержания ссылок разобрана в [COL-TRADEOFFS](../02-collections/tradeoffs.md).

## 9. Потоковая публикация — самостоятельная гарантия, не вывод из final (I-17 · P2)

Неизменяемость внешнего API и **безопасная публикация между потоками** связаны, но не эквивалентны. Специальная семантика `final` полей в Java Memory Model важна для видимости состояния после корректного конструирования при определённых условиях. Однако нельзя доказать корректность произвольного достижимого mutable graph и запрет всех data races только указанием `private final` в примере.

Особенно опасны `this`, опубликованный во время выполнения конструктора, конкурентная мутация входного массива в момент defensive copy, либо callbacks, которые видят частично сконструированное состояние. В этих случаях надо исследовать корректную публикацию, happens-before и протокол доступа к общим mutable объектам.

**I-17 — осознанная P2-граница:** здесь нет независимого happens-before proof, JMM litmus/stress и анализа небезопасного escape. Из одного запуска на OpenJDK нельзя делать выводы о потокобезопасности всех применений. Подробная будущая тема — `CON-JMM`, а не ещё один абзац «final значит thread-safe».

## 10. Что компилируется, а что является runtime/protocol failure

| Ситуация | Стадия | Объяснение |
|---|---|---|
| `final int[] data = {67}; data[0] = 1;` | Компилируется | Final запрещает переназначить **ссылку**, не изменить элемент |
| `List.copyOf(Arrays.asList("A", null))` | **Runtime NPE** | Метод запрещает null-элементы |
| `List.copyOf(source).add("B")` | **Runtime UOE** | Unmodifiable результат |
| `Collections.unmodifiableList(source); source.add("B")` | Компилируется, view изменится | Wrapper не копирует source |
| `values.clone()` у `int[][]` | Компилируется, shallow | Внешний массив новый, ссылки на внутренние массивы общие |
| `BadAccessor.values() { return values.clone(); }` | Компилируется, но нарушает record copy invariant | Копия массива по умолчанию сравнивается по ссылке |
| `new Scores(new int[0])` из §2 | **Runtime IllegalArgumentException** | Собственная проверка инварианта |
| `record Frozen(int[] a) {}` | Компилируется, объект не deeply immutable | Конечная ссылка на mutable array |

Компиляционно-отрицательные примеры N01–N16 из G1 относятся прежде всего к **языковым ограничениям record**. Проверка NPE/UOE/валидации в запущенной программе **не** доказывает, что код не компилируется, и не заменяет `javac -XDrawDiagnostics` с проверкой ожидаемой категории.

## Карта I-01–I-19: механизм, контрпример и честная граница

| ID | Приоритет | Раздел и пример / предел гарантии |
|---|---|---|
| I-01 | P0 | §1: наблюдаемая неизменяемость; alias и переопределяемый метод |
| I-02 | P0 | §1: `final` поле хранит неизменную ссылку, не mutable состояние |
| I-03 | P0 | §1/2: alias на входе (Unsafe/List) и защитное copy-in |
| I-04 | P0 | §2/3: alias на выходе и копия в accessor |
| I-05 | P0 | §4/6: `List.copyOf` snapshot ссылок и порядка, set и copyOf(view) |
| I-06 | P0 | §4: `Collections.unmodifiableList` live view и обёртка собственного списка |
| I-07 | P0 | §4/5: UOE у set/iterator/derived view vs mutable nested element |
| I-08 | P0 | §4/6/7: null/NPE; record null отдельно; value-based identity |
| I-09 | P0 | §2/3: обе копии массива, clone/copyOf shallow |
| I-10 | P0 | §2/6: constructor invariant: проверка принадлежащего объекту снимка |
| I-11 | P0 | §6: record не deeply immutable, snapshot `List<String>` |
| I-12 | P0 | §2/6: выбор final class/record и согласованный Good из §5 records.md |
| I-13 | P1 | §5: mutable elements, CounterValue и двухуровневый freeze |
| I-14 | P1 | §4: view vs copyOf, owned wrapper, value-based identity |
| I-15 | P1 | §7: Map: mutable list key vs array identity; не гарантия null |
| I-16 | P1 | §8: identity memo, цикл и общие ссылки; не универсальный deep copy |
| I-17 | P2 | §9: JMM/final safe publication — **не доказано** |
| I-18 | P2 | **Граница:** DTO, JavaBeans/ORM/JPA и framework-specific contracts — отдельно |
| I-19 | P2 | **Граница:** allocation/JIT/GC/JMH performance — не измерено |

**Связанная новая статья:** [CORE-DATETIME](datetime.md) показывает неизменяемость `Instant`, `LocalDate` и `ZonedDateTime`: `plusDays` возвращает новое значение, а не меняет исходный объект. Это применение принципа value objects, но не новый эксперимент по потокобезопасности или JMM.

## Нормы, пример G1 и пределы проверки

**Первоисточники:** [List.copyOf](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html#copyOf(java.util.Collection)), [Collections.unmodifiableList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#unmodifiableList(java.util.List)), [List.of](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html#of()), [Arrays.clone/copyOf](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html), [JLS §4.12.4 final](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4), [JLS §17.5 final fields](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.5), [java.lang.Record](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Record.html). В [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md) неофициальные интервью-источники используются для формулирования вопросов, **не** как доказательство Java API.

**Исследовательский G1** включает полностью проверенный на OpenJDK 21.0.12.1 `ImmutableProbe.java` с **I01–I10, 10 строк stdout**: `I01=[A, B]`, `I02=[A]`, `I03=[A, B]`, `I04=UOE`, `I05=NPE`, `I06=67`, `I07=67`, `I08=false` (наблюдение, **не** требование новой identity), `I09=[1, 0, 0]`, `I10=empty`. Вместе с `RecordsProbe.java` это **26 наблюдений двух отдельных запусков**, а не 26 unit-tests. [Оба исходника и эталона](../../examples/core/records-immutability/README.md) уже включены в авторскую ветку из [технического PR #133](https://github.com/Melchiorius/backend-interview-labs/pull/133). Роль 03 отдельно сверила опубликованные SHA и воспроизвела оба G1 stdout (16/16 и 10/10) на Debian OpenJDK 21.0.12.1 с нулём предупреждений. Исторический CI объединённого HEAD и приёмка зафиксированы в [PR #132](https://github.com/Melchiorius/backend-interview-labs/pull/132); результат локального запуска не означает компиляции всех inline Java-фрагментов.

**NOT RUN без отдельного evidence:** автоматическое извлечение/компиляция *всех* фрагментов Java из статьи; отрицательные N01–N16 в CI; визуальный Obsidian/IntelliJ; другие JDK/JVM/OS, реальные concurrent mutation/JMM litmus, property-based проверки всех возможных graph aliases, reflection/serialization/ORM, Unicode/IO, benchmark/JMH/GC/JIT. Положительный запуск одного примера не заменяет нормативного доказательства глубокой неизменяемости или безопасной публикации.

**Продолжение про стандартные представления коллекций:** [CORE-UTIL](utilities.md) сопоставляет backed `Arrays.asList`, `Collections.unmodifiableList` как живую обёртку, `List.of` и shallow structural snapshot `List.copyOf`.

**Инварианты при восстановлении объектов:** [CORE-SERIAL](serialization.md) показывает проверку состояния в `readObject`, особые правила канонического конструктора `record` и почему `transient`/shallow-copy сами по себе не гарантируют безопасность или неизменяемость.

**Новые фактические проверки 02.** [Полный Probe.java, команды, stdout и хеши](https://github.com/Melchiorius/backend-interview-labs/issues/366#issuecomment-7231597243) на Debian OpenJDK/javac 21.0.12.1: javac --release 21 -encoding UTF-8 -XDrawDiagnostics -Xlint:all -Werror exit 0, stdout/stderr пусты; JVM exit 0, stderr пуст, 18 строк/274 байта stdout (16 меток assertions и два непосредственных наблюдения reuse и listKey). Проверены 24 ожидаемых runtime-исключения с точным классом и ошибкой при отсутствии исключения: 1 validation, 15 mutators, 4 null API, 2 frozen writes, 2 nested null. Отдельных новых compile-negative не потребовалось. Это не исторические отрицательные record-пробы или 24 ожидаемых отклонения всего CI. Независимый [аудит A15.2 §5](../../docs/audits/java-core-full-2026-11-24/A15.2-CORE-IMMUTABLE.md) ранее дал 20 наблюдений ImmutableAudit; его результаты не считаются новыми проверками 02. Фрагменты выше выполнены в контексте Probe, не по отдельности.

До исправления README по N22 роль 02 выполнила полный общий runner дважды на пяти восстановленных входах с проверенными Git blob SHA/mode (не полный клон), default environment и LC_ALL=C.utf8, без инструментирования. Оба exit 0, внешний stderr и build/javac.stderr пусты; инвентарь до/после совпал, четыре сравнения фактических stdout с fixture дали cmp exit 0. Одна компиляция двух Java-файлов с --release 21 -Xlint:all, проверка отсутствия диагностики javac, два JVM с наследуемым stderr, два diff -u, сохранение build и печать обоих actual.stdout и маркера. Нет Werror/явного encoding/отдельной проверки stderr каждого JVM. Фактические R16 строк/144 байта и I10/94; внешний stdout 27 строк/314 байт SHA-256 38e0ddf952503d6e8adecdf4844b53e70d99428207d0fc61fb63b27ba24bd6ea, последний маркер `CHECK PASSED: both stdout files match expected; javac emitted zero warnings`. 26 наблюдений, не 26 unit-тестов; R13=false намеренный антипример. Хеши и вывод в приложении. OS locales C/C.utf8/POSIX; JVM charset/locale не измерялись. На этапе 02 исходники, эталоны, README и records.md были сохранены без изменений.

**Независимая практическая проверка 03.** [Полный Boundary.java, команды, вывод и отчёт N22](https://github.com/Melchiorius/backend-interview-labs/pull/367#issuecomment-7231732061): адаптированная временная программа скомпилирована на Debian OpenJDK/javac 21.0.12.1 с --release 21 -encoding UTF-8 -XDrawDiagnostics -Xlint:all -Werror (exit 0, stdout/stderr пусты); JVM exit 0, stderr пуст, stdout 37 строк/1355 байт. Отдельно подтверждены 26 ожидаемых runtime-исключений с проверкой точного класса и обязательности исключения. Это не 24 runtime-исключения роли 02 и не 24 ожидаемых отклонения компиляции всего CI. Роль 03 независимо запустила тот же неизменённый исполняемый runner в default environment и C.utf8, оба exit 0 и совпадение фактических выводов с эталонами. README изменён отдельно по N22 в двух строках, исходники и эталоны сохранены. Это ранее проведённые независимые проверки, не новые запуски при редакторской правке и не отдельная компиляция всех листингов статьи.

Актуальный статус темы — [COVERAGE](../../docs/COVERAGE.md), состав тем — [CURRICULUM](../../docs/CURRICULUM.md).
