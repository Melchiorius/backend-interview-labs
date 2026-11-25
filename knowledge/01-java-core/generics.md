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

`T` — **переменная типа**, а не имя класса, создаваемого для каждого значения `T`. `Box<T>` — объявление обобщённого класса; `Box<Integer>` — параметризованный тип; `Integer` — фактический аргумент типа. Компилятор сопоставляет объявленное ограничение с употреблением. Это не означает, что при выполнении Java создаёт отдельный класс `Box<Integer>`: границы гарантий во время выполнения описаны в [стирании типов](erasure.md). Нормы: [JLS 21 §8.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.2), [§4.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.5).

Параметризованные типы `Box<Integer>` и `Box<String>` используют **одно объявление класса** с разными статическими контрактами; сами по себе разные аргументы типа не образуют разные классы во время выполнения. Это помогает библиотекам возвращать строго типизированные значения и проверять вызовы на этапе компиляции. При использовании сырых типов (raw types) и непроверяемых преобразований (unchecked conversions) часть гарантий можно обойти — это не опровержение цели generics.

## Параметр класса, параметр метода и границы (GE-01, GE-02)

В обобщённом **методе** параметр объявляется *до* типа результата; он не обязан совпадать с параметром окружающего класса.

~~~java
static <T> T first(T a, T b) {
    return a;
}

String s = first("a", "b");
Integer n = first(67, 68);
~~~

Если алгоритму нужны операции, которых нет у `Object`, задаём верхнюю границу (*bound*). По [JLS §4.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.4) границей может быть одна переменная типа (`<U, T extends U>`), либо класс или интерфейс, за которыми через `&` могут следовать **только интерфейсы**. Поэтому класс `Number` нельзя поставить после интерфейса, а к границе, заданной переменной типа `U`, нельзя дописать `& Runnable`. Стирания составляющих границы должны попарно различаться; кроме того, `T` не может одновременно быть подтипом двух разных параметризаций одного обобщённого интерфейса.

~~~java
static <T extends Number & Comparable<T>> int compare(T a, T b) {
    return a.compareTo(b);
}

int order = compare(67, 68); // отрицательное значение
// compare("67", "68");      // ошибка: String не Number
~~~

Компилятор знает, что `T` одновременно является подтипом `Number` и `Comparable<T>`, поэтому допускает `compareTo`. Стирание переменной типа — стирание её **крайней левой границы**, в том числе если первой стоит не класс, а интерфейс: `T extends Runnable & Serializable` стирается до `Runnable`. Без явно указанной границы используется `Object`. [JLS §4.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.4), [§4.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.6) и [erasure](erasure.md).

Практический контраст: `Child extends Base`, а `Base implements Comparable<Base>`. `Child` умеет сравнивать себя с `Base`, следовательно и с другим `Child`. Граница `T extends Comparable<T>` при явно выбранном `T = Child` не подходит: `Comparable<Base>` не является `Comparable<Child>`. Граница `T extends Comparable<? super T>` разрешает такое сравнение. В [Collections.max](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#max(java.util.Collection)) без компаратора фактическая сигнатура — `static <T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll)`. Указанная `Object` — левая граница именно этой сигнатуры; это не обещание полного разбора рекурсивных границ (F-bounds). Сохраните `Bounds.java`:

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

Фактический вывод: `68:68:-1`, затем `67`. Собственный `max` и библиотечный `Collections.max` возвращают `Child` с большим значением; `compare(67,68)` сохраняет прежний пример с двумя границами. `single` показывает допустимую одиночную границу, заданную переменной типа. Объявление метода `left` с интерфейсом на первом месте в границе компилируется; её стирание выше объясняется нормативно, новый анализ байткода не выполнялся. Отрицательные `BadBoundOrder`, `BadBoundVariable`, `BadBoundRepeat`, `BadComparable` приведены ниже.

Граница `<T extends Number>` ограничивает одну переменную типа `T`. Все употребления `T` в сигнатуре связаны одним допустимым аргументом типа. Это **не** требование, чтобы все фактически переданные объекты имели один класс во время выполнения. Например, `first(67, 3.14)` может удовлетворить ограничениям при присваивании результата `Number` (разбор ниже). У `List<? extends Number>` конкретный аргумент типа неизвестен компилятору; два независимых захвата таких типов не обязаны совпадать.

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

Но `Integer` по-прежнему можно использовать *там, где нужно отдельное значение Number*: `Number value = Integer.valueOf(67);`. Инвариантность — свойство отношения статических типов `List<Integer>` и `List<Number>`; она не зависит от того, изменяем ли конкретный объект списка. Гипотетическая запись `Double` объясняет, зачем нужно ограничение, а не задаёт условие его действия. Нормы: [JLS §4.10.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.10.2), [List API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html).

<a name="три-разных-контракта-listobject-list-list-extends-object-ge-04"></a>

## List<Object> и неизвестный аргумент типа: эквивалентность List<?> и List<? extends Object> (GE-04)

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

Фактический вывод: `a:67`, `true`, `ArrayStoreException`. Ссылки типов `List<?>` и `List<? extends Object>` можно присваивать друг другу; у `B<?>` известна верхняя граница `Number`. Вторая строка показывает лишь то, что два созданных в примере экземпляра `ArrayList` имеют один класс во время выполнения. Сравнение `getClass()` не позволяет узнать их аргументы типа. Третья строка показывает проверку фактического компонентного типа массива `Integer[]` при записи. Отдельный пример `BadObjects` компилятор отклонил. Даже если операция допустима по типам, возможность изменить список зависит от реализации.

## Producer: ? extends Number (GE-05, GE-19)

~~~java
List<? extends Number> producer =
        new ArrayList<Integer>(List.of(67));

Number value = producer.get(0); // чтение безопасно
// producer.add(68);            // ошибка компиляции
producer.add(null);             // компилятор допускает
~~~

Почему нельзя добавить произвольное значение `Integer`, отличное от `null`? Неизвестным аргументом типа может быть `Integer`, `Double` или другой подтип `Number`: компилятор не может доказать, что такое значение подходит каждому из них. При чтении статическая верхняя граница — `Number`, но само значение может быть `null`. Если вспомогательный обобщённый метод удерживает один захваченный тип `T`, он может прочитать и записать значение этого же `T` (пример ниже); произвольной записи это не разрешает.

`null` совместим с любым ссылочным типом и потому особый случай. Но успешная **компиляция** вызова `add(null)` не обещает, что конкретная коллекция разрешает изменение или `null`: неизменяемый список выбросит `UnsupportedOperationException`, а некоторые коллекции запрещают `null`. Wildcard **не делает** исходную коллекцию неизменяемой; он лишь ограничивает операции через эту ссылку. Здесь речь о **совместимости типов при компиляции**, а не о возможности изменять конкретную реализацию. Отдельно см. [контракты коллекций Java 21](../02-collections/contracts.md): необязательные операции, `null`, различия `List`/`Set`/`Map` и отдельные отрицательные компиляционные примеры.

Можно также удалять элементы через `remove(int)`, очищать список через `clear()` и переставлять существующие элементы через вспомогательный обобщённый метод, удерживающий захваченный тип. При поддержке соответствующих операций изменяется сам список; изменения видны и через другую ссылку на него (alias). Для `remove`, `clear` и `set` существенен контракт реализации и, где применимо, допустимый индекс. Полный `Variance.java` ниже показывает и нижнюю границу, и эти операции с изменяемым `ArrayList`.

## Consumer: ? super Integer (GE-06, GE-19)

~~~java
List<? super Integer> consumer = new ArrayList<Number>();
consumer.add(67);               // корректно
Object item = consumer.get(0);  // известен только Object
// Integer x = consumer.get(0); // ошибка компиляции
~~~

Ссылка типа `List<? super Integer>` может указывать на список с аргументом типа `Integer` или любым его надтипом, в том числе интерфейсом `Serializable`, который реализует `Integer`. Нижняя граница разрешает запись значений `Integer` и его подтипов (у `Integer` нет подклассов, он `final`); `null` тоже допустим по типам. Возможность выполнить запись зависит от списка.

При чтении статический тип результата `get` — захваченный тип с верхней границей `Object`, поэтому без дополнительных сведений результат можно использовать как `Object`. Значение может быть `null`. Гарантировать `Integer` нельзя: `List<Number>` мог уже содержать `Double`, а `List<Object>` — произвольные объекты. Верхняя граница `Object` обусловлена объявлением `List<E>`, а не общим правилом для всех обобщённых типов. Например, в `B<T extends Number>` чтение через `B<? super Integer>` даёт значение, совместимое с `Number`.

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

Первая строка `[67]:67:3.14`: `consumer.add(67)` изменяет объект списка `List<Serializable>`, доступный через `serial` и `consumer`; чтение через `consumer` даёт 67 как `Object`. Через `B<? super Integer>` читается значение 3.14 как `Number`.

Вторая строка `[68, 67, null]:null`: вызов `producer.add(null)` добавляет элемент в `ArrayList`, доступный через `alias` и `producer`. Чтение последнего элемента возвращает `null`, а вспомогательный метод `swap` переставляет первые два элемента того же списка. Изменение видно через `alias`.

Третья строка `68:0`: `producer.remove(0)` возвращает 68, затем `producer.clear()` очищает тот же объект, и `alias.size()` возвращает 0.

Четвёртая строка `UnsupportedOperationException`: попытка добавить `null` в неизменяемый список `List.of` приводит к перехваченному исключению. Это наблюдения выбранных реализаций, а не общая гарантия изменяемости. `BadExtendsAdd` и `BadSuperGet` ниже не компилируются.

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

`src.get(i)` отдаёт значение, совместимое с `T`, а `dest.set(i, value)` принимает его. По контракту [Java 21 Collections.copy](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#copy(java.util.List,java.util.List)) **размер (`size`), а не ёмкость (`capacity`)** списка назначения должен быть не меньше размера источника. Метод заменяет элементы, не добавляя новых; элементы назначения с индексами за пределами размера источника остаются прежними.

Список фиксированного размера от `Arrays.asList` подходит, поскольку поддерживает замену элементов. У `new ArrayList<>(10)` без добавлений размер 0, и копирование непустого источника приводит к `IndexOutOfBoundsException`. Для непустого копирования в неизменяемый список `List.of` наблюдается `UnsupportedOperationException`: для замены элементов нужны поддерживаемые операции.

Общий [контракт класса Collections](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html) отдельно оговаривает, что алгоритм, изменяющий переданную коллекцию (destructive algorithm в документации), при вызове без эффекта может выбросить `UnsupportedOperationException` при неподдерживаемой операции, но не обязан этого делать. Поэтому нельзя обещать ни обязательное исключение, ни обязательный успех для любого пустого копирования.

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

Фактический вывод на Debian OpenJDK 21.0.12.1: `[67, 68, 9]`, `IndexOutOfBoundsException`, `UnsupportedOperationException`, `empty=ok` по строкам. Элемент 9 за пределами размера источника сохранился; одной ёмкости недостаточно; непустая замена в `List.of` отклонена. Последний результат относится именно к проверенной версии библиотеки, а не к универсальному контракту копирования пустого источника.

Возможность принять один тип и возвратить другой зависит от **направления передачи значений**. PECS не требует использовать подстановочные аргументы (wildcard) на каждом параметре публичного метода: когда значение и возвращаемый результат должны иметь один и тот же `T`, именованный параметр типа может выразить связь точнее.

## Захват неизвестного типа: wildcard capture (GE-08)

На первый взгляд кажется, что переставить элементы `List<?>` нельзя: неизвестен точный аргумент типа для записи. Компилятор действительно отвергает прямое `list.set(0, list.get(0))`: даже повторное использование имени ссылки в разных выражениях не обязано дать один и тот же захваченный тип. В диагностике `javac` при этом может фигурировать `Object` вместо второго захвата:

~~~java
// void replace(List<?> list) {
//     list.set(0, list.get(0)); // ошибка: Object нельзя записать как захваченный тип
// }
~~~

Чтобы компилятор мог связать чтение и запись с **одним и тем же неизвестным** типом, передадим список во вспомогательный обобщённый метод:

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

При анализе вызова вспомогательного обобщённого метода компилятор выполняет захват (capture conversion): по [JLS §5.1.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.10) подстановочный аргумент заменяется свежей переменной типа с соответствующими верхней и нижней границами, с учётом границ объявления. Внутри `duplicateFirstHelper` один параметр `T` связывает тип результата чтения и тип аргумента записи.

Захват не применяется рекурсивно к вложенным аргументам типа. Он происходит при компиляции, не требует действий во время выполнения и не обнаруживает фактический аргумент `String`. При выполнении самого метода возможны исключения: например, если в списке меньше двух элементов или он не поддерживает `set`.

Два независимых захвата `List<? extends Number>` могут обозначать разные типы элементов. Одинаковая верхняя граница не доказывает, что оба захваченных типа равны одному `T`.

В `BadTwoCapture` ниже компилятор не может согласовать два независимых захвата с одним параметром `T` метода `f(List<T>,List<T>)`: диагностика `javac` содержит `incompatible.eq.bounds` для разных CAP. В `BadCapture` причина отказа другая: при прямом вызове `set(0, get(0))` нет доказанной совместимости результата чтения с типом аргумента записи; в диагностике наблюдается `Object` против CAP. `Variance.swap` выше показывает успешное изменение одного списка через один захват, не разрешая записывать произвольное значение чужого типа.

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

`int` как аргумент типа недопустим; нужны ссылочные типы. Автоупаковка `int → Integer` при помещении числа в `List<Integer>` не отменяет этого ограничения. Правила вывода аргументов типа (inference) и выбора перегрузки нельзя сводить к одному примеру с diamond; конкретные правила — [JLS §15.12](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12), [§5.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.2).

Wildcard — аргумент параметризованного **ссылочного типа**, не имя объявляемой переменной типа (`<T>`). `List<?> x = new ArrayList<String>()` допустимо (см. `Wildcards`), но нельзя использовать wildcard как непосредственный аргумент создаваемого класса в `new ArrayList<?>();`, как непосредственный аргумент прямого супертипа в `extends ArrayList<?>`, или как явно заданный аргумент типа метода в `ClassName.<?>f(67)`. Это отдельные ограничения соответствующих синтаксических позиций, не запрет любого вложенного wildcard в типе. [JLS §4.5.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.5.1), [§8.1.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.4), [§15.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9), [§15.12](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12). Три отдельные отрицательные программы ниже.

> [!note]- Обобщённые конструкторы, явные аргументы метода и сложный вывод типов
> Конструктор может иметь **собственный** параметр типа, не совпадающий с параметром класса. У обобщённого метода иногда полезно явно задать `ClassName.<T>method(...)`, если контекст вывода не выражает нужной связи. Вывод типов с типами-пересечениями (intersection types), перегрузками и лямбдами требует отдельного разбора: из правила diamond нельзя вывести все эти случаи. См. [JLS §8.8.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.4) и [CORE-FUNCTIONS](functions.md).

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

Фактический вывод `67`, затем `a:68`. В `new <Integer> Holder<String>(67, "a")` аргумент перед именем класса относится к собственному параметру конструктора `U`, а `String` — к параметру класса `T`. Собственный `T` статического `identity` скрывает параметр класса. По [JLS §15.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9) нельзя одновременно явно задать аргументы типа конструктора и использовать diamond для класса (`new <Integer> Holder<>(67, "a")`); отдельный `BadDiamondExplicit` это проверяет. Границы статических контекстов и локальных типов см. [CORE-NESTED](nested-classes.md), вывод типов и лямбды — [CORE-FUNCTIONS](functions.md) (см. связанные вопросы A10-N03/N05). Вопросы обобщённых методов с переменным числом аргументов (generic varargs) и стирания остаются в [CORE-ERASURE](erasure.md).

## Что generics НЕ гарантируют

- `? extends Number` не доказывает неизменяемость или потокобезопасность коллекции; `? super Integer` не гарантирует тип `Integer` при чтении.
- Успешный `javac` означает соблюдение проверяемого статического контракта **для данного исходника**; код с сырыми типами (raw types) и непроверяемыми преобразованиями (unchecked) может нарушить обещания типов и обнаружиться позже.
- Нет отдельного класса во время выполнения `List<Integer>`, автоматически проверяющего при извлечении все аргументы типа: [что стирается, какие проверки сохраняются и почему возникает ClassCastException](erasure.md).
- PECS не выражает владение объектом, конкурентные гарантии или правила проверки бизнес-условий; это памятка по выбору ограничений типов при передаче значений.

## Вопросы для самопроверки

| Вопрос | Ответ |
|---|---|
| Чем `List<Object>` отличается от двух эквивалентных записей `List<?>` и `List<? extends Object>`? | У `List<Object>` аргумент типа известен: `Object`; можно записывать совместимые ссылки, если операция поддерживается. У двух обычных неаннотированных wildcard аргумент неизвестен, поэтому нельзя записать произвольное значение, отличное от `null`. `null` допустим по типам; вспомогательный метод может записать значение того же захваченного типа. |
| Почему `Child` с `Comparable<Base>` подходит для `Comparable<? super T>`? | При `T = Child` сравнение с надтипом `Base` разрешено; инвариантный `Comparable<Child>` не следует из `Comparable<Base>`. |
| Можно ли менять список через `List<? extends Number>`? | `remove`, `clear` и перестановка через один захват возможны при поддержке соответствующих операций. Запись произвольного значения, отличного от `null`, не разрешена по типам. |
| Почему ссылке `List<? super Integer>` можно присвоить `List<Serializable>`? | `Serializable` — надтип `Integer`; для `List` верхняя граница типа чтения — `Object`. Результат может быть `null`. |
| Почему `f(List<T>, List<T>)` не принимает два независимых `List<? extends Number>` в `BadTwoCapture`? | Независимые захваты не доказывают равенство аргументов типа. Это не запрет всех вызовов с двумя wildcard. |
| Что даст `Collections.copy(new ArrayList<Number>(10), List.of(67))`? | Размер 0, ёмкость не помогает; `IndexOutOfBoundsException`. |
| Почему `var a = new ArrayList<>()` принимает 67 и строку? | Здесь выводится `ArrayList<Object>` без целевого типа элемента. |
| Что обозначают два аргумента типа `new <Integer> Holder<String>(...)`? | Первый относится к конструктору, второй — к классу; явные аргументы конструктора с diamond для класса запрещены. |

## Отдельные отрицательные примеры D-A11a

Каждый файл компилируется отдельно. Эти 18 исходников не являются исходными 22 отрицательными примерами G1 и не входят в общий скрипт проверки. Инвариантность и захват:

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

`BadInvariant` и `BadObjects` дают несовместимые типы (`prob.found.req/inconvertible.types`). У `BadCapture` — `cant.apply.symbol` с несовместимым аргументом `Object`/CAP, а у `BadTwoCapture` — `cant.apply.symbol` с `incompatible.eq.bounds` для двух независимых захватов.

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

Аргументы типа и позиции wildcard:

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

`BadPrimitive` — `type.found.req/type.req.ref`; `BadDiamondExplicit` — `cant.apply.diamond.1/diamond.and.explicit.params`; `BadWildcardNew` и `BadWildcardSuper` — `type.found.req/type.req.exact`; `BadWildcardMethod` — `illegal.start.of.type`, `illegal.start.of.expr`, `expected: :` (три ошибки синтаксического анализа). Все 18 отдельных компиляций отклонены с кодом 1 и пустым stdout. Всего получена 21 диагностика ошибки: две у `BadBoundRepeat`, три у `BadWildcardMethod` и по одной у остальных 16 файлов. Это не 21 отклонённый файл или тест. Формулировки и число диагностик — наблюдение `javac`, а не правило JLS.

## Фактические проверки D-A11a

Роль 02 выполнила отдельные проверки временных файлов на Debian OpenJDK/javac 21.0.12.1, `LC_ALL=C`. Каждый файл компилировался в собственном каталоге командой `javac --release 21 -XDrawDiagnostics -Xlint:all -Werror -d out Имя.java`; успешно скомпилированные программы запускались командой `java -cp out Имя`.

Из 24 отдельных компиляций шесть завершились с кодом 0, 18 — с ожидаемым кодом 1. Все шесть запусков JVM завершились с кодом 0. У успешных компиляций stdout и stderr пусты, у успешных запусков stderr пуст; у отклонённых компиляций stdout пуст. Исключения в положительных программах перехвачены намеренно.

Полные исходные тексты этих проб приведены в [статье на первоначальном проверенном HEAD `9469fa35f80db201dd239a43c443bf967411be88`](https://github.com/Melchiorius/backend-interview-labs/blob/9469fa35f80db201dd239a43c443bf967411be88/knowledge/01-java-core/generics.md). Их Git-хеши, команды, коды завершения, фактический вывод и диагностические ключи приведены в [адресном отчёте роли 02 #7170070309](https://github.com/Melchiorius/backend-interview-labs/issues/305#issuecomment-7170070309). [Issue #305](https://github.com/Melchiorius/backend-interview-labs/issues/305) — постановка задачи.

Это 24 отдельных локальных компиляции, не 24 теста CI и не повтор всей исторической серии G1. Прежние 15 блоков — контекстные фрагменты и сигнатуры, не все из них самостоятельные запускаемые файлы. Новые варианты байткода, JIT, производительность и полная теория вывода типов не проверялись. Редакционные правки не являются повторным локальным запуском.

Роль 02 восстановила четыре файла общего примера в двух временных деревьях: из BASE `2aa0c75d6d43b523d50bf69432e83fbdfd7850f1` и с теми же входами авторского HEAD `9469fa35f80db201dd239a43c443bf967411be88`. Это не полный клон репозитория; все четыре Git-хеша и режима совпали.

В каждом дереве проверка синтаксиса `bash -n examples/core/generics-erasure/verify.sh` и запуск неизменённого скрипта `bash examples/core/generics-erasure/verify.sh` завершились с кодом 0 и пустым stderr. Скрипт компилирует программу с `javac --release 21 -Xlint:all -Werror`, запускает её и сравнивает stdout с эталоном. Полный вывод обоих запусков совпал побайтно: 18 строк программы из эталона и строка скрипта `CHECK PASSED: stdout matches expected.stdout`. Это 19 строк вывода, а не 19 тестов.

Известное ограничение N21 относится к D-A11b: `copyFirst` вызывается после `Collections.copy` на тех же значениях, поэтому общий скрипт не доказывает, что обнаружит отдельный мутант `copyFirst`, который ничего не делает (no-op). Мутант не запускался и не исправлялся в D-A11a. Исходники, эталон, скрипт и README общего примера не изменены. Первоначальный [CI #45669323048](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45669323048) относится к HEAD `9469fa35f80db201dd239a43c443bf967411be88`, а не к новой редакции и не к 24 временным пробам. Независимый G4 обязателен.

## Карта покрытия G1 и границы

| ID | Где читатель получает ответ |
|---|---|
| GE-01, GE-02 | Обобщённые классы/методы и границы; аспекты выполнения GE-02 — [erasure](erasure.md) |
| GE-03, GE-04 | Инвариантность, эквивалентность `List<?>`/`List<? extends Object>` и отличие от `List<Object>` |
| GE-05, GE-06, GE-07 | Чтение/запись через extends/super, `Variance` и `Collections.copy`/`CopyCases` |
| GE-08, GE-09 | Один захват и две независимые переменные, контраст массивов |
| GE-17 | Вывод общего допустимого аргумента, целевой тип, diamond, wildcard-позиции |
| GE-19, GE-21, GE-25 (P1) | `null`, изменение коллекций, нижняя граница, статический контекст, конструктор; часть GE-25 о переменном числе аргументов — [erasure](erasure.md) |
| GE-26, GE-30 (P2) | Рекурсивные границы (F-bounds), типы-пересечения (intersection types) и углублённый вывод типов — обозначены как дальнейшее углубление, не как полностью реализованная теория |

> [!note]- P2: рекурсивные границы
> В `class Node<T extends Comparable<T>>` параметр `T` появляется внутри собственного ограничения. Это обычная техника для статической связи операций сравнения; строгая теория рекурсивно ограниченного полиморфизма (F-bounded polymorphism) и взаимодействие типов-пересечений с выводом типов требуют отдельного материала. Нельзя по этому примеру делать общие выводы о всех рекурсивных границ — см. [JLS §4.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.4).

**Источники:** [JLS 21 §4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html), [§5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html), [§8.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.2), [§15.12](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12), [Collections.copy API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#copy(java.util.List,java.util.List)). [Реестр источников](../../docs/SOURCE_REGISTRY.md) разделяет нормативные и интервью-материалы; подборки вопросов не служат доказательством языковых гарантий.

Исторические интервью-ориентиры (не нормативные доказательства и не новый G1): [G1 CORE-GENERICS-ERASURE §3](../../docs/plans/CORE-GENERICS-ERASURE.md) — Q-HRUDAY глава 11 Generics, Q-IN28 Q178–184, Q-CHERNYSHOV §20 вопросы 11–25, Q-TEAMLEAD PECS и статические обобщённые методы; [первичный аудит A11.1 §2](../../docs/audits/java-core-full-2026-11-24/A11.1-CORE-GENERICS.md) — Baeldung Java Generics Interview Questions Q10–14, L307–384 исторического чтения. Подборки помогают формулировать вопросы, но не подтверждают частоту или корректность ответов. В частности, нижняя граница охватывает интерфейсы-надтипы, а не только суперклассы.

**Один воспроизводимый пример Java 21:** [CORE-GENERICS + CORE-ERASURE — README, исходник и полный stdout](../../examples/core/generics-erasure/README.md). Программа связывает PECS и `Collections.copy`, верхнюю и нижнюю границы wildcard, захват через вспомогательный метод, ограничения параметров и вывод типов со стиранием и контролируемой ошибкой во время выполнения. Её **18 строк одного запуска** — самостоятельный учебный сценарий, а не повтор P01–P20 принятого G1-плана, не замена 22 отрицательных примеров N01–N22 и четырёх программ с проверкой предупреждений компилятора W01–W04. Он также не является запуском 24 отдельных Java-файлов D-A11a или всех 15 исторических контекстных блоков статьи. Результат и границы проверки приведены в README и в разделе фактических проверок выше; неизменённый общий скрипт не закрывает требования D-A11b.

**Переход дальше:** [CORE-ERASURE](erasure.md) → [наследование](inheritance.md) → [функциональные интерфейсы и lambda](functions.md).
