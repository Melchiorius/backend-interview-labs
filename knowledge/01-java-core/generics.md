# CORE-GENERICS — параметры типов, инвариантность, wildcards и вывод типов

**Java SE 21.** Исследовательская основа — [принятый план CORE-GENERICS/ERASURE](../../docs/plans/CORE-GENERICS-ERASURE.md), Issue [#82](https://github.com/Melchiorius/backend-interview-labs/issues/82). Эта статья объясняет, какие операции **разрешены компилятором** для параметризованных типов. Продолжение о том, что остаётся при выполнении программы, — [CORE-ERASURE](erasure.md). Предпосылки: [типы и ссылки](types.md), [классы и методы](classes.md), [преобразования](conversions.md), [наследование](inheritance.md).

## Зачем generics, если Java уже проверяет типы

Пусть сервис получает объект по ключу из контейнера. Без параметра типа клиенту пришлось бы самостоятельно приводить результат к нужному классу — и ошибка могла бы обнаружиться только при исполнении. С параметром `T` компилятор проверяет согласованность значения, хранящегося в контейнере, с типом результата.

~~~java
final class Box<T> {
    private final T value;
    Box(T value) { this.value = value; }
    T get() { return value; }
}

Box<Integer> box = new Box<>(67);
Integer result = box.get();          // корректно
// String wrong = box.get();         // ошибка компиляции: Integer не String
~~~

`T` — **переменная типа**, а не имя класса, создаваемого для каждого значения `T`. `Box<T>` — объявление обобщённого класса; `Box<Integer>` — параметризованный тип; `Integer` — фактический аргумент типа. Компилятор сопоставляет объявленное ограничение с употреблением. Это не означает, что при выполнении Java создаёт отдельный класс `Box<Integer>`: границы runtime описаны в [стирании типов](erasure.md). Нормы: [JLS 21 §8.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.2), [§4.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.5).

Параметризованные типы `Box<Integer>` и `Box<String>` используют **одно объявление класса** с разными статическими контрактами; сами по себе разные аргументы типа не образуют разные классы во время выполнения. Это помогает библиотекам возвращать строго типизированные значения и проверять вызовы на этапе компиляции. При использовании raw types и непроверяемых преобразований (unchecked conversions) часть гарантий можно обойти — это не опровержение цели generics.

## Параметр класса, параметр метода и границы (GE-01, GE-02)

В обобщённом **методе** параметр объявляется *до* типа результата; он не обязан совпадать с параметром окружающего класса.

~~~java
static <T> T first(T a, T b) {
    return a;
}

String s = first("a", "b");
Integer n = first(67, 68);
~~~

Если алгоритму нужны операции, которых нет у `Object`, задаём верхнюю границу (*bound*). По [JLS §4.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.4) границей может быть одна переменная типа (`<U, T extends U>`), либо класс или интерфейс, за которыми через `&` могут следовать **только интерфейсы**. Поэтому класс `Number` нельзя поставить после интерфейса, а к отдельной границе-переменной `U` нельзя дописать `& Runnable`. Стирания составляющих границы должны попарно различаться; кроме того, `T` не может одновременно быть подтипом двух разных параметризаций одного обобщённого интерфейса.

~~~java
static <T extends Number & Comparable<T>> int compare(T a, T b) {
    return a.compareTo(b);
}

int order = compare(67, 68); // отрицательное значение
// compare("67", "68");      // ошибка: String не Number
~~~

Компилятор знает, что `T` одновременно является подтипом `Number` и `Comparable<T>`, поэтому допускает `compareTo`. Стирание переменной типа — стирание её **крайней левой границы**, в том числе если первой стоит не класс, а интерфейс: `T extends Runnable & Serializable` стирается до `Runnable`. Без явно указанной границы используется `Object`. [JLS §4.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.4), [§4.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.6) и [erasure](erasure.md).

Практический контраст: `Child extends Base`, а `Base implements Comparable<Base>`. `Child` умеет сравнивать себя с `Base`, следовательно и с другим `Child`. Граница `T extends Comparable<T>` при явно выбранном `T = Child` не подходит: `Comparable<Base>` не является `Comparable<Child>`. Граница `T extends Comparable<? super T>` разрешает такое сравнение. В [Collections.max](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#max(java.util.Collection)) без компаратора фактическая сигнатура — `static <T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll)`. Указанная `Object` — левая граница именно этой сигнатуры; это не обещание полного разбора F-bounds. Сохраните `Bounds.java`:

~~~java
import java.util.*;
class Bounds {
    static class Base implements Comparable<Base> {
        final int value;
        Base(int value) { this.value = value; }
        public int compareTo(Base other) { return Integer.compare(value, other.value); }
    }
    static class Child extends Base { Child(int value) { super(value); } }
    static <T extends Comparable<? super T>> T max(T a, T b) {
        return a.compareTo(b) >= 0 ? a : b;
    }
    static <T extends Runnable & java.io.Serializable> T left(T t) { return t; }
    static <U, T extends U> U single(T t) { return t; }
    static <T extends Number & Comparable<T>> int compare(T a, T b) { return a.compareTo(b); }
    public static void main(String[] args) {
        Child a = new Child(67), b = new Child(68);
        Child c = max(a, b), d = Collections.max(List.of(a, b));
        System.out.println(c.value + ":" + d.value + ":" + compare(67, 68));
        System.out.println(Bounds.<Number, Integer>single(67));
    }
}
~~~

Фактический вывод: `68:68:-1`, затем `67`. Собственный `max` и библиотечный `Collections.max` возвращают `Child` с большим значением; `compare(67,68)` сохраняет прежний пример с двумя границами. `single` показывает допустимую одиночную границу-переменную. Декларация `left` с первым интерфейсом компилируется; её стирание выше объясняется нормативно, новый анализ байткода не выполнялся. Отрицательные `BadBoundOrder`, `BadBoundVariable`, `BadBoundRepeat`, `BadComparable` приведены ниже.

Граница `<T extends Number>` относится к одной переменной типа `T`: все позиции `T` в сигнатуре связывает общий допустимый аргумент типа. Это **не** требование, чтобы все фактически переданные объекты имели один класс во время выполнения. Например, `first(67, 3.14)` может удовлетворить ограничениям при присваивании результата `Number` (разбор ниже). У `List<? extends Number>` конкретный аргумент типа неизвестен компилятору; две независимые такие ссылки не дают автоматически одного и того же захваченного типа.

**Статический контекст.** Параметр типа класса `T` нельзя использовать из статического контекста этого класса, в частности как тип статического поля или в сигнатуре и теле статического метода. Его нельзя заимствовать и в статическом вложенном классе. Статический метод может объявить собственный параметр типа (`U` либо даже собственный `T`, скрывающий одноимённый параметр класса). Это другое объявление, а не выбор параметризации экземпляра. [JLS §8.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.2), [§6.5.5.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.5.5.1), [§8.4.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.4); см. [CORE-NESTED](nested-classes.md) и [CORE-FUNCTIONS](functions.md).

~~~java
class Holder<T> {
    T instanceValue;        // корректно
    // static T shared;      // ошибка компиляции
    static <U> U identity(U value) { return value; } // свой U у метода
}
~~~

## Почему List<Integer> не является List<Number> (GE-03)

Хотя `Integer` — подтип `Number`, отсюда **не следует** отношение подтипов между `List<Integer>` и `List<Number>`. Параметризованные типы с разными определёнными аргументами в данном случае **инвариантны**. Рассмотрим, какую проблему предотвратил компилятор:

~~~java
List<Integer> integers = new ArrayList<>();
integers.add(67);
// List<Number> numbers = integers; // ошибка компиляции
~~~

Если бы присваивание разрешили, код мог бы затем вызвать `numbers.add(3.14)`. Это поместило бы `Double` в ту же коллекцию, которую другая ссылка считает списком целых. Ограничение защищает соглашение о типе элемента на этапе компиляции.

Но `Integer` по-прежнему можно использовать *там, где нужно отдельное значение Number*: `Number value = Integer.valueOf(67);`. Разница — в наличии **изменяемого контейнера** и разных возможных операций чтения/записи. Нормы: [JLS §4.10.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.10.2), [List API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html).

## Три разных контракта: List<Object>, List<?>, List<? extends Object> (GE-04)

`List<Object>` обещает возможность работать с элементами как с `Object` и, при поддержке изменяющих операций конкретной коллекцией, записывать объекты подходящих типов. Это **не** супертип для любого `List<String>`.

~~~java
List<String> strings = new ArrayList<>(List.of("a"));
List<?> unknown = strings;                // корректно
List<? extends Object> anyObjects = strings; // корректно
// List<Object> objects = strings;        // ошибка компиляции

Object read = unknown.get(0);
// unknown.add("b");                      // ошибка: неизвестен аргумент типа
~~~

`List<?>` означает список с некоторым неизвестным аргументом типа, а не `List<Object>`. Согласно [JLS §4.5.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.5.1), обычные неаннотированные `?` и `? extends Object` **эквивалентны**, а не просто похожи. Явная запись границы не превращает неизвестный аргумент в `Object`. Захват при разных употреблениях выражений может породить разные свежие переменные типа; учитываются также границы самого объявления (`B<T extends Number>` даёт чтение `Number` через `B<?>`). Это не утверждение об отсутствии значения у аннотаций типа. [JLS §5.1.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.10). Сохраните `Wildcards.java`:

~~~java
import java.util.*;
class Wildcards {
    static class B<T extends Number> { T value; B(T value) { this.value = value; } }
    public static void main(String[] args) {
        List<?> a = new ArrayList<String>(List.of("a"));
        List<? extends Object> b = a;
        List<?> c = b;
        B<?> bounded = new B<>(67);
        Number n = bounded.value;
        System.out.println(c.get(0) + ":" + n);
        System.out.println(new ArrayList<String>().getClass() == new ArrayList<Integer>().getClass());
        Number[] array = new Integer[1];
        try { array[0] = 3.14; throw new AssertionError(); }
        catch (ArrayStoreException e) { System.out.println("ArrayStoreException"); }
    }
}
~~~

Фактический вывод: `a:67`, `true`, `ArrayStoreException`. Обе записи wildcard взаимно присваиваются; у `B<?>` известна верхняя граница `Number`. Два объекта `ArrayList` с разными параметризациями имеют один класс во время выполнения, тогда как массив `Integer[]` проверяет фактический компонентный тип при записи. Контрпример `BadObjects` отдельно отклонён компилятором. Важна совместимость типов; даже допустимые операции изменения зависят от реализации списка.

## Producer: ? extends Number (GE-05, GE-19)

~~~java
List<? extends Number> producer =
        new ArrayList<Integer>(List.of(67));

Number value = producer.get(0); // чтение безопасно
// producer.add(68);            // ошибка компиляции
producer.add(null);             // компилятор допускает
~~~

Почему нельзя добавить произвольный ненулевой `Integer`? Неизвестный аргумент может оказаться `Integer`, `Double` или другим подтипом `Number`, и запись независимого конкретного значения не безопасна для каждого случая. При чтении статическая верхняя граница — `Number`, но само значение может быть `null`. Если вспомогательный обобщённый метод удерживает один захваченный тип `T`, он может прочитать и записать значение этого же `T` (пример ниже); произвольной записи это не разрешает.

`null` совместим с любым ссылочным типом и потому особый случай. Но успешная **компиляция** вызова `add(null)` не обещает, что конкретная коллекция разрешает изменение или `null`: неизменяемый список выбросит `UnsupportedOperationException`, а некоторые коллекции запрещают `null`. Wildcard **не делает** исходную коллекцию неизменяемой; он лишь ограничивает операции через эту ссылку. Здесь речь о **совместимости типов при компиляции**, а не о возможности изменять конкретную реализацию. Отдельно см. [контракты коллекций Java 21](../02-collections/contracts.md): необязательные операции, `null`, различия `List`/`Set`/`Map` и отдельные отрицательные компиляционные примеры.

Можно также удалять элементы через `remove(int)`, очищать список через `clear()` и переставлять существующие элементы через вспомогательный метод захвата; при поддержке соответствующих операций меняется тот же объект, видимый по другой ссылке (alias). Для `remove`, `clear` и `set` существенен контракт реализации и, где применимо, допустимый индекс. Полный `Variance.java` ниже одновременно показывает нижнюю границу и эти операции с изменяемым `ArrayList`.

## Consumer: ? super Integer (GE-06, GE-19)

~~~java
List<? super Integer> consumer = new ArrayList<Number>();
consumer.add(67);               // корректно
Object item = consumer.get(0);  // известен только Object
// Integer x = consumer.get(0); // ошибка компиляции
~~~

Аргументом `List<? super Integer>` может быть `Integer` или любой его надтип, включая совместимый интерфейс, например `Serializable`, реализуемый `Integer`. Нижняя граница допускает запись значения `Integer` и его подтипов (у `Integer` подтипов-классов нет, он `final`); `null` тоже допустим статически. Прочитать гарантированно `Integer` нельзя: `List<Number>` мог уже содержать `Double`, `List<Object>` — произвольные объекты. Без дополнительных сведений чтение имеет верхнюю границу `Object`, потому что именно так ограничен параметр `E` в объявлении `List<E>`. Это не универсальное правило для всех обобщённых объявлений: у `B<T extends Number>` результат чтения через `B<? super Integer>` совместим с `Number`.

Сохраните `Variance.java`:

~~~java
import java.util.*;
import java.io.Serializable;
class Variance {
    static class B<T extends Number> { T value; B(T value) { this.value = value; } }
    static void swap(List<?> list) { swapCaptured(list); }
    static <T> void swapCaptured(List<T> list) {
        T x = list.get(0); list.set(0, list.get(1)); list.set(1, x);
    }
    public static void main(String[] args) {
        List<Serializable> serial = new ArrayList<>();
        List<? super Integer> consumer = serial;
        consumer.add(67); Object value = consumer.get(0);
        B<? super Integer> bounded = new B<Number>(3.14); Number n = bounded.value;
        System.out.println(serial + ":" + value + ":" + n);
        List<Integer> alias = new ArrayList<>(List.of(67, 68));
        List<? extends Number> producer = alias;
        producer.add(null); Number last = producer.get(2);
        swap(producer); System.out.println(alias + ":" + last);
        Number removed = producer.remove(0); producer.clear();
        System.out.println(removed + ":" + alias.size());
        try { List<? extends Number> fixed = List.of(67); fixed.add(null); throw new AssertionError(); }
        catch (UnsupportedOperationException e) { System.out.println("UnsupportedOperationException"); }
    }
}
~~~

Фактический вывод:

~~~text
[67]:67:3.14
[68, 67, null]:null
68:0
UnsupportedOperationException
~~~

В первой строке `List<Serializable>` принимает 67, а у `B<Number>` чтение через нижнюю границу даёт `Number` со значением 3.14. Во второй строке `producer.add(null)` добавил элемент, чтение вернуло `null`, перестановка через один захват изменила `alias`. В третьей строке `remove(0)` вернул 68, `clear()` очистил тот же объект. Четвёртая строка — перехваченное исключение у неизменяемого `List.of`. Это наблюдения выбранных реализаций, а не общая гарантия изменяемости. `BadExtendsAdd` и `BadSuperGet` ниже не компилируются.

Слово `super` здесь относится к ограничению аргумента типа, а не к вызову метода суперкласса. Сравнение с наследованием классов — в [CORE-INHERITANCE](inheritance.md).

## PECS в реальном API: Collections.copy (GE-07)

PECS (*Producer Extends, Consumer Super*) — удобная памятка, **не термин JLS**. Она помогает выбирать границы аргументов, когда один источник **производит** элементы, а другой их **принимает**.

~~~java
List<Integer> source = List.of(67, 68);
List<Number> destination =
        new ArrayList<>(List.of(0, 0));

Collections.copy(destination, source);
System.out.println(destination); // [67, 68]
~~~

Контракт метода библиотеки:

~~~java
public static <T> void copy(
        List<? super T> dest,
        List<? extends T> src)
~~~

`src.get(i)` отдаёт значение, совместимое с `T`; `dest.set(i, value)` принимает его. Поэтому **размер (`size`), а не ёмкость (`capacity`)** назначения должен быть не меньше размера источника. `Collections.copy` не добавляет элементов; если назначение длиннее, его хвост не меняется. Фиксированный по размеру `Arrays.asList` подходит, поскольку поддерживает замену элементов. `new ArrayList<>(10)` без добавлений имеет размер 0, и копирование непустого источника в него завершается `IndexOutOfBoundsException`. Для непустого копирования в неизменяемый `List.of` наблюдается `UnsupportedOperationException`; метод требует поддержки замены через соответствующие операции. [Java 21 Collections.copy](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#copy(java.util.List,java.util.List)). Общий [контракт класса Collections](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html) отдельно оговаривает, что разрушающий алгоритм при вызове без эффекта может выбросить, но не обязан выбрасывать `UnsupportedOperationException` при неподдерживаемой операции. Следовательно нельзя обещать ни обязательное исключение, ни обязательный успех для любого пустого копирования.

Сохраните `CopyCases.java`:

~~~java
import java.util.*;
class CopyCases {
    public static void main(String[] args) {
        List<Number> dest = Arrays.<Number>asList(0, 0, 9);
        Collections.copy(dest, List.of(67, 68)); System.out.println(dest);
        try { Collections.copy(new ArrayList<Number>(10), List.of(67)); throw new AssertionError(); }
        catch (IndexOutOfBoundsException e) { System.out.println("IndexOutOfBoundsException"); }
        try { Collections.copy(List.<Number>of(0), List.of(67)); throw new AssertionError(); }
        catch (UnsupportedOperationException e) { System.out.println("UnsupportedOperationException"); }
        try { Collections.copy(List.of(), List.of()); System.out.println("empty=ok"); }
        catch (UnsupportedOperationException e) { System.out.println("empty=UnsupportedOperationException"); }
    }
}
~~~

Фактический вывод на Debian OpenJDK 21.0.12.1: `[67, 68, 9]`, `IndexOutOfBoundsException`, `UnsupportedOperationException`, `empty=ok` по строкам. Хвост 9 сохранился, одной ёмкости недостаточно, непустая замена в `List.of` отклонена. Последний результат — наблюдение именно этой JVM и библиотеки, не универсальный контракт пустого источника.

Возможность принять один тип и возвратить другой зависит от **направления движения значений**. PECS не требует расставлять wildcard на каждом параметре публичного метода: когда значение и возвращаемый результат должны иметь один и тот же `T`, именованный параметр типа может выразить связь точнее.

## Захват неизвестного типа: wildcard capture (GE-08)

На первый взгляд кажется, что переставить элементы `List<?>` нельзя: неизвестен точный аргумент типа для записи. Компилятор действительно отвергает прямое `list.set(0, list.get(0))`: даже повторное использование имени ссылки в разных выражениях не обязано дать один и тот же захваченный тип. В диагностике `javac` при этом может фигурировать `Object` вместо второго захвата:

~~~java
// void replace(List<?> list) {
//     list.set(0, list.get(0)); // ошибка: Object нельзя записать как захваченный тип
// }
~~~

Чтобы удержать **один и тот же неизвестный** тип для чтения и записи, передадим список в generic helper:

~~~java
static void duplicateFirst(List<?> values) {
    duplicateFirstHelper(values);
}

private static <T> void duplicateFirstHelper(List<T> values) {
    values.set(1, values.get(0));
}

List<String> words = new ArrayList<>(List.of("a", "b"));
duplicateFirst(words);
System.out.println(words); // [a, a]
~~~

При передаче списка во вспомогательный обобщённый метод компилятор выполняет захват (capture conversion): по [JLS §5.1.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.10) подстановочный аргумент заменяется свежей переменной типа с соответствующими верхней и нижней границами, с учётом границ объявления. Внутри `duplicateFirstHelper` один параметр `T` связывает чтение и запись. Захват не применяется рекурсивно к вложенным типовым аргументам и не требует действий во время выполнения, не обнаруживает фактический аргумент `String`. Вызов метода сам может завершиться исключением (например, если элементов меньше двух или `set` не поддерживается).

Два независимых `List<? extends Number>` могут содержать разные типы элементов. Нельзя заставить компилятор считать их одним `T` только потому, что верхняя граница у них одна. В `BadTwoCapture` ниже `f(List<T>,List<T>)` вызывается с двумя независимыми захватами; `javac` сообщает `incompatible.eq.bounds` для разных CAP. В `BadCapture` встречается `Object` против CAP, но причина та же по существу: отсутствует доказанная связь, необходимая для записи. `Variance.swap` выше показывает успешное изменение одного списка без произвольной записи чужого типа.

## Почему массивы ведут себя иначе (GE-09)

Массивы Java **ковариантны**: `Integer[]` допустимо присвоить переменной `Number[]`. В отличие от параметризованных списков, компонентный тип массива проверяется также при записи во время выполнения.

~~~java
Integer[] integers = {67};
Number[] asNumbers = integers; // разрешено
try {
    asNumbers[0] = 3.14;
} catch (ArrayStoreException expected) {
    System.out.println("Нельзя поместить Double в Integer[]");
}
~~~

Проверяется **реальный** тип массива `Integer[]`, а не только статический `Number[]`. Generics с инвариантностью обычно отсекают похожую ошибку **раньше**, при компиляции. Не путайте `ArrayStoreException` с `ClassCastException`: это разные проверки. [JLS §10.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html#jls-10.5). Подробное устройство массивов — [CORE-ARRAYS](arrays.md).

## Вывод аргументов типа, diamond и контекст (GE-17, GE-25)

~~~java
List<Integer> numbers = new ArrayList<>();
numbers.add(67);

List<Integer> empty = Collections.emptyList();
Integer result = first(67, 68); // T выводится по аргументам
~~~

`<>` (diamond) позволяет компилятору вывести аргументы типа, а `Collections.emptyList()` — обобщённый метод, результат которого может зависеть от целевого типа. Это не значит, что Java «всегда сама угадывает» желаемый тип: выбор перегрузки, ограничения границ и контекст присваивания могут сделать программу неоднозначной или недопустимой. [JLS §18.5.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-18.html#jls-18.5.2), [§15.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9).

Вызов `Number n = first(67, 3.14)` требует допустимого общего аргумента типа для упакованных `Integer` и `Double`, а также совместимости результата с `Number`. Из этого не следует, что выведенный `T` точно равен `Number` или что два объекта имеют один класс. В `Inference.<Number>first(67, 3.14)` аргумент типа явно задан. У `var list = new ArrayList<>()` нет целевого `List<Integer>`; здесь выводится `ArrayList<Object>`, поэтому допустимы и 67, и `"a"`. Напротив, `List<Integer> target = new ArrayList<>()` задаёт целевой тип. Сохраните `Inference.java`:

~~~java
import java.util.*;
class Inference {
    static <T> T first(T a, T b) { return a; }
    public static void main(String[] args) {
        Number n = first(67, 3.14);
        Number explicit = Inference.<Number>first(67, 3.14);
        var list = new ArrayList<>(); list.add(67); list.add("a");
        ArrayList<Object> proof = list;
        List<Integer> target = new ArrayList<>(); target.add(67);
        System.out.println(n + ":" + explicit + ":" + proof + ":" + target);
    }
}
~~~

Фактический вывод `67:67:[67, a]:[67]`. Присваивание `ArrayList<Object> proof = list` проверено компилятором: это не вывод о параметризации по содержимому во время выполнения.

~~~java
// List<int> primitives = new ArrayList<int>(); // ошибка: type argument — ссылочный тип
List<Integer> boxed = new ArrayList<>();        // Integer — ссылочная обёртка
~~~

`int` как аргумент типа недопустим; нужны ссылочные типы. Автоупаковка `int → Integer` при помещении числа в `List<Integer>` не отменяет этого ограничения. Граничные случаи inference и перегрузки не следует выводить из одного diamond-примера; конкретные правила — [JLS §15.12](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12), [§5.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.2).

Wildcard — аргумент параметризованного **ссылочного типа**, не имя объявляемой переменной типа (`<T>`). `List<?> x = new ArrayList<String>()` допустимо (см. `Wildcards`), но нельзя использовать wildcard как непосредственный аргумент создаваемого класса в `new ArrayList<?>();`, как непосредственный аргумент прямого супертипа в `extends ArrayList<?>`, или как явно заданный аргумент типа метода в `ClassName.<?>f(67)`. Это отдельные ограничения соответствующих синтаксических позиций, не запрет любого вложенного wildcard в типе. [JLS §4.5.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.5.1), [§8.1.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.4), [§15.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9), [§15.12](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12). Три отдельные отрицательные программы ниже.

> [!note]- Generic constructors, явные аргументы метода и сложный вывод
> Конструктор может иметь **собственный** параметр типа, не совпадающий с параметром класса. У generic method иногда полезно явно задать `ClassName.<T>method(...)`, если контекст вывода не выражает нужной связи. Вывод типов с intersection types, overloaded methods и lambdas становится отдельным существенным механизмом, а не правилом «diamond всегда работает». См. [JLS §8.8.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.4) и [CORE-FUNCTIONS](functions.md).

Сохраните `ConstructorTypes.java`:

~~~java
class ConstructorTypes {
    static class Holder<T> {
        final T value;
        <U> Holder(U tag, T value) { this.value = value; System.out.println(tag); }
        static <T> T identity(T x) { return x; }
    }
    public static void main(String[] args) {
        Holder<String> h = new <Integer> Holder<String>(67, "a");
        System.out.println(h.value + ":" + Holder.<Integer>identity(68));
    }
}
~~~

Фактический вывод `67`, затем `a:68`. В `new <Integer> Holder<String>(67, "a")` аргумент перед именем класса относится к собственному параметру конструктора `U`, а `String` — к параметру класса `T`. Собственный `T` статического `identity` скрывает параметр класса. По [JLS §15.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9) нельзя одновременно явно задать аргументы типа конструктора и использовать diamond для класса (`new <Integer> Holder<>(67, "a")`); отдельный `BadDiamondExplicit` это проверяет. Границы статических контекстов и локальных типов см. [CORE-NESTED](nested-classes.md), вывод типов и лямбды — [CORE-FUNCTIONS](functions.md) (см. связанные вопросы A10-N03/N05). Вопросы generic varargs и стирания остаются в [CORE-ERASURE](erasure.md).

## Что generics НЕ гарантируют

- `? extends Number` не доказывает неизменяемость или потокобезопасность коллекции; `? super Integer` не гарантирует тип `Integer` при чтении.
- Успешный `javac` означает соблюдение проверяемого статического контракта **для данного исходника**; raw/unchecked-код может нарушить обещания типов и обнаружиться позже.
- Нет отдельного runtime-класса `List<Integer>`, автоматически проверяющего при извлечении все аргументы типа: [что стирается, какие проверки сохраняются и почему возникает ClassCastException](erasure.md).
- PECS не выражает владение объектом, конкурентные гарантии или правила бизнес-валидации; это правило выбора generic-ограничений для движения значений.

## Вопросы для самопроверки

| Вопрос | Ответ |
|---|---|
| Чем `List<?>` отличается от `List<? extends Object>` и `List<Object>`? | Первые два обычных неаннотированных wildcard эквивалентны; третий имеет конкретный аргумент `Object`, неизвестный тип не даёт произвольную запись. |
| Почему `Child` с `Comparable<Base>` подходит для `Comparable<? super T>`? | При `T = Child` сравнение с надтипом `Base` разрешено; инвариантный `Comparable<Child>` не следует из `Comparable<Base>`. |
| Можно ли менять список `? extends Number`? | `remove`, `clear`, перестановка через один захват возможны при поддержке операций; произвольная ненулевая запись запрещена. |
| Почему `List<? super Integer>` принимает `List<Serializable>`? | `Serializable` — надтип `Integer`; чтение в общем случае только `Object` для List. |
| Почему два wildcard нельзя передать в `f(List<T>, List<T>)`? | Независимые свежие захваты не доказывают равенство аргументов типа. |
| Что даст `Collections.copy(new ArrayList<Number>(10), List.of(67))`? | Размер 0, ёмкость не помогает; `IndexOutOfBoundsException`. |
| Почему `var a = new ArrayList<>()` принимает 67 и строку? | Здесь выводится `ArrayList<Object>` без целевого типа элемента. |
| Что обозначают два типовых аргумента `new <Integer> Holder<String>(...)`? | Первый относится к конструктору, второй — к классу; явные аргументы конструктора с diamond класса запрещены. |

## Отдельные отрицательные примеры D-A11a

Каждый файл компилируется отдельно. Эти 18 исходников не являются исходными 22 негативами G1 и не входят в общий runner. Инвариантность и захват:

`BadInvariant.java`:

~~~java
import java.util.*; class BadInvariant { List<Number> n = new ArrayList<Integer>(); }
~~~

`BadObjects.java`:

~~~java
import java.util.*; class BadObjects { List<Object> n = new ArrayList<String>(); }
~~~

`BadCapture.java`:

~~~java
import java.util.*; class BadCapture { void f(List<?> x) { x.set(0, x.get(0)); } }
~~~

`BadTwoCapture.java`:

~~~java
import java.util.*; class BadTwoCapture { static <T> void f(List<T> a, List<T> b) {} void g(List<? extends Number> a, List<? extends Number> b) { f(a,b); } }
~~~

Первые два дают несовместимые типы (`prob.found.req/inconvertible.types`); следующие — `cant.apply.symbol` с несовместимым аргументом `Object`/CAP и `incompatible.eq.bounds` соответственно.

Границы и сравнение:

`BadBoundOrder.java`:

~~~java
class BadBoundOrder<T extends Comparable<T> & Number> {}
~~~

`BadBoundVariable.java`:

~~~java
class BadBoundVariable<U, T extends U & Runnable> {}
~~~

`BadBoundRepeat.java`:

~~~java
class BadBoundRepeat<T extends Comparable<String> & Comparable<Integer>> {}
~~~

`BadComparable.java`:

~~~java
class BadComparable { static class Base implements Comparable<Base> { public int compareTo(Base b) { return 0; } } static class Child extends Base {} static <T extends Comparable<T>> T max(T a,T b) { return a; } Child c = BadComparable.<Child>max(new Child(),new Child()); }
~~~

Диагностики: `intf.expected.here`, `type.var.may.not.be.followed.by.other.bounds`, `repeated.interface` вместе с `cant.inherit.diff.arg` (две ошибки), `explicit.param.do.not.conform.to.bounds` внутри `cant.apply.symbol.noargs`.

Статический контекст и операции через wildcard:

`BadStaticT.java`:

~~~java
class BadStaticT<T> { static T value; }
~~~

`BadStaticMethod.java`:

~~~java
class BadStaticMethod<T> { static T f() { return null; } }
~~~

`BadStaticNested.java`:

~~~java
class BadStaticNested<T> { static class Nested { T value; } }
~~~

`BadExtendsAdd.java`:

~~~java
import java.util.*; class BadExtendsAdd { void f(List<? extends Number> x) { x.add(67); } }
~~~

`BadSuperGet.java`:

~~~java
import java.util.*; class BadSuperGet { Integer f(List<? super Integer> x) { return x.get(0); } }
~~~

Первые три — `non-static.cant.be.ref` для переменной типа `T`; `BadExtendsAdd` — `cant.apply.symbols` с несовместимым `int`/CAP, `BadSuperGet` — `prob.found.req/inconvertible.types` CAP/Integer.

Типовые аргументы и позиции wildcard:

`BadPrimitive.java`:

~~~java
import java.util.*; class BadPrimitive { List<int> x; }
~~~

`BadDiamondExplicit.java`:

~~~java
class BadDiamondExplicit<T> { <U> BadDiamondExplicit(U tag) {} Object x = new <Integer> BadDiamondExplicit<>(67); }
~~~

`BadWildcardNew.java`:

~~~java
import java.util.*; class BadWildcardNew { Object x = new ArrayList<?>(); }
~~~

`BadWildcardSuper.java`:

~~~java
import java.util.*; class BadWildcardSuper extends ArrayList<?> {}
~~~

`BadWildcardMethod.java`:

~~~java
class BadWildcardMethod { static <T> T f(T x) { return x; } Object x = BadWildcardMethod.<?>f(67); }
~~~

`BadPrimitive` — `type.found.req/type.req.ref`; `BadDiamondExplicit` — `cant.apply.diamond.1/diamond.and.explicit.params`; `BadWildcardNew` и `BadWildcardSuper` — `type.found.req/type.req.exact`; `BadWildcardMethod` — `illegal.start.of.type`, `illegal.start.of.expr`, `expected: :` (три ошибки синтаксического анализа). Все 18 компиляций завершились с кодом 1 и пустым stdout; у остальных 16 по одной диагностике. Формулировки и число диагностик — наблюдение `javac`, а не правило JLS.

## Фактические проверки D-A11a

Роль 02 выполнила 24 отдельные компиляции временных файлов (6 с кодом 0, 18 с ожидаемым кодом 1) и шесть успешных запусков JVM. Среда Debian OpenJDK/javac 21.0.12.1, `LC_ALL=C`. Каждый файл компилировался в отдельном каталоге командой `javac --release 21 -XDrawDiagnostics -Xlint:all -Werror -d out Имя.java`; положительные запускались `java -cp out Имя`. При успешной компиляции stdout/stderr пусты, при успешных запусках stderr пуст; исключения в программах перехвачены намеренно. Исходные тексты новых проб приведены в этой статье; их Git-хеши, команды, коды завершения, вывод и диагностики — в [адресном отчёте роли 02 #7170070309](https://github.com/Melchiorius/backend-interview-labs/issues/305#issuecomment-7170070309). [Issue #305](https://github.com/Melchiorius/backend-interview-labs/issues/305) — постановка задачи. Прежние 15 блоков — контекстные фрагменты и сигнатуры, не все отдельные запускаемые файлы. Не повторялась полная историческая серия G1, не проверялись новые варианты байткода, JIT, производительность или полная теория вывода типов.

Роль 02 восстановила четыре файла общего примера в двух временных деревьях из BASE `2aa0c75d6d43b523d50bf69432e83fbdfd7850f1` и идентичного по этим входам авторского варианта. Это не полный клон; все четыре Git-хеша и режима сверены. В каждом дереве `bash -n examples/core/generics-erasure/verify.sh` и `bash examples/core/generics-erasure/verify.sh` завершились с кодом 0 и пустым stderr; скрипт компилирует `javac --release 21 -Xlint:all -Werror`, запускает программу и сравнивает stdout с эталоном. Полный вывод обоих запусков совпал побайтно: 18 строк эталона и строка скрипта `CHECK PASSED: stdout matches expected.stdout`. Это не 19 тестов. Известное ограничение N21 из D-A11b: `copyFirst` вызывается после `Collections.copy` на тех же значениях, поэтому общий runner не доказывает обнаружение независимого no-op мутанта `copyFirst`. Мутант не запускался и не исправлялся в D-A11a. Исходники и README общего примера не изменены; новый CI — отдельно от временных проб, независимый G4 обязателен.

## Карта покрытия G1 и границы

| ID | Где читатель получает ответ |
|---|---|
| GE-01, GE-02 | Обобщённые классы/методы и границы; runtime-часть GE-02 — [erasure](erasure.md) |
| GE-03, GE-04 | Инвариантность, эквивалентность `List<?>`/`List<? extends Object>` и отличие от `List<Object>` |
| GE-05, GE-06, GE-07 | Чтение/запись через extends/super, `Variance` и `Collections.copy`/`CopyCases` |
| GE-08, GE-09 | Один захват и две независимые переменные, контраст массивов |
| GE-17 | Вывод общего допустимого аргумента, целевой тип, diamond, wildcard-позиции |
| GE-19, GE-21, GE-25 (P1) | `null`, мутации, нижняя граница, статический контекст, конструктор; varargs-часть GE-25 — [erasure](erasure.md) |
| GE-26, GE-30 (P2) | Recursive/F-bounds, intersection types и deep inference — обозначены как дальнейшее углубление, не как полностью реализованная теория |

> [!note]- P2: рекурсивные границы
> В `class Node<T extends Comparable<T>>` параметр `T` появляется внутри собственного ограничения. Это обычная техника для статической связи операций сравнения; строгая теория F-bounded polymorphism и взаимодействие intersection types с inference требуют отдельного материала. Нельзя по этому примеру делать общие выводы о всех recursive bounds — см. [JLS §4.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.4).

**Источники:** [JLS 21 §4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html), [§5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html), [§8.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.2), [§15.12](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12), [Collections.copy API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#copy(java.util.List,java.util.List)). [Реестр источников](../../docs/SOURCE_REGISTRY.md) разделяет нормативные и интервью-материалы; подборки вопросов не служат доказательством языковых гарантий.

Исторические интервью-ориентиры (не нормативные доказательства и не новый G1): [G1 CORE-GENERICS-ERASURE §3](../../docs/plans/CORE-GENERICS-ERASURE.md) — Q-HRUDAY глава 11 Generics, Q-IN28 Q178–184, Q-CHERNYSHOV §20 вопросы 11–25, Q-TEAMLEAD PECS/static generic method; [первичный аудит A11.1 §2](../../docs/audits/java-core-full-2026-11-24/A11.1-CORE-GENERICS.md) — Baeldung Java Generics Interview Questions Q10–14, L307–384 исторического чтения. Подборки помогают формулировать вопросы, но не подтверждают частоту или корректность ответов. В частности, нижняя граница охватывает надтипы-интерфейсы, а не только суперклассы.

**Один воспроизводимый пример Java 21:** [CORE-GENERICS + CORE-ERASURE — README, исходник и полный stdout](../../examples/core/generics-erasure/README.md). Программа связывает PECS/`Collections.copy`, `? extends` и `add(null)`, `? super`, capture-helper, bounds и inference со стиранием типов и контролируемой runtime-ошибкой. Её **18 строк одного запуска** — дидактически переработанный сценарий, а не побуквенное повторение P01–P20 принятого G1-плана и не замена negative N01–N22 / lint W01–W04. Результат запуска и границы проверок приведены в README. Проверка неизменённого общего runner для D-A11a описана выше; она не закрывает требования D-A11b.

**Переход дальше:** [CORE-ERASURE](erasure.md) → [наследование](inheritance.md) → [функциональные интерфейсы и lambda](functions.md).
