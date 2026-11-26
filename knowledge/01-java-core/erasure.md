# CORE-ERASURE — стирание типов, unchecked-код и границы runtime

**Java SE 21.** Основание — [принятый G1-план Generics/Erasure](../../docs/plans/CORE-GENERICS-ERASURE.md), Issue [#82](https://github.com/Melchiorius/backend-interview-labs/issues/82). Начинать следует со статьи [CORE-GENERICS](generics.md): она объясняет, какие связи типов компилятор устанавливает *до запуска*. Здесь разберём, какие из этих сведений остаются при выполнении, где появляются проверки и почему некоторые ошибки проявляются поздно.

## Почему типы параметров не становятся отдельными runtime-классами (GE-10)

~~~java
List<String> words = new ArrayList<>();
List<Integer> numbers = new ArrayList<>();
System.out.println(words.getClass() == numbers.getClass()); // true
~~~

Мы создали два объекта **одной реализации** `ArrayList`. Java проверяет при компиляции разные контракты ссылок `List<String>` и `List<Integer>`, но runtime-класс экземпляра — `ArrayList`, а не новый «ArrayList-of-String». Этот результат нельзя обобщать как «абсолютно все generic-сведения исчезают»: в декларациях методов и суперклассов могут оставаться generic **метаданные**; при доступе к значениям компилятор может вводить приведения типов. Строгое правило — [JLS 21 §4.6 (type erasure)](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.6).

Упрощённая схема стирания:

| Исходная конструкция | Что важно после erasure |
|---|---|
| `List<String>` или `List<Integer>` | `List` (типовая параметризация не создаёт два runtime-класса) |
| `T` без явной границы | стирается к `Object` |
| `T extends Number` | стирается к `Number` |
| `T extends Number & Comparable<T>` | к первой, левой границе — `Number` |
| `<T> T identity(T t)` | соответствующая erased сигнатура использует `Object` |

Это **модель статических типов и сигнатур**, а не обещание точного Java-исходника, который генератор класса обязан физически записать. Нельзя выводить спецификацию `javac` или количество байткод-методов только из таблицы.

Полное правило [JLS §4.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.6) рекурсивно: стираем аргументы параметризованного типа, для вложенного `T.C` получаем вложенный тип `C` стёртого внешнего типа `T` (то есть `|T|.C`), для `T[]` — тип компонента, для переменной типа — **стирание** её крайней левой границы. Остальные типы сохраняются. Например, `T extends List<String>` стирается в `List`, а `T[]` — в `List[]`; `Outer<String>.Inner<Integer>[]` — в массив `Outer.Inner`. Если левая граница — интерфейс `Runnable`, стирание даёт именно его, не Object. Ограничения на несколько границ и `Comparable<? super T>` уже разобраны в [CORE-GENERICS](generics.md).

Не смешивайте три понятия: сигнатура Java-метода (имя, параметры типа, типы формальных параметров с учётом правил адаптации, **не возвращаемый тип**), её стирание (имя и стёртые типы формальных параметров, без параметров типа), и дескриптор метода в файле класса (classfile) (типы параметров **и результата**). Возвращаемый тип тоже подвергается стиранию, но не становится частью Java-сигнатуры. Поэтому изменить только возвращаемый тип недостаточно для перегрузки. [JLS §8.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.2), [JVMS §4.3.3](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html#jvms-4.3.3), [CORE-INHERITANCE](inheritance.md).

Сохраните `ErasedTypes.java`:

~~~java
import java.util.*;
class ErasedTypes {
    static class Outer<T> { class Inner<U> {} }
    static <T extends List<String>> T[] echo(T[] a) { return a; }
    static <T> T identity(T t) { return t; }
    static <T extends Runnable & java.io.Serializable> T first(T t) { return t; }
    static Outer<String>.Inner<Integer>[] nested;
    public static void main(String[] args) throws ReflectiveOperationException {
        var m = ErasedTypes.class.getDeclaredMethod("echo", List[].class);
        var id = ErasedTypes.class.getDeclaredMethod("identity", Object.class);
        var f = ErasedTypes.class.getDeclaredMethod("first", Runnable.class);
        System.out.println(m.getReturnType().getTypeName() + ":" + id.getReturnType().getSimpleName());
        System.out.println(f.getReturnType().getSimpleName() + ":" + ErasedTypes.class.getDeclaredField("nested").getType().getTypeName());
    }
}
~~~

Фактический вывод: `java.util.List[]:Object`, затем `Runnable:ErasedTypes$Outer$Inner[]`. Команда `javap -classpath out -s -p ErasedTypes` на этом классе дала дескрипторы поля `[LErasedTypes$Outer$Inner;`, метода echo `([Ljava/util/List;)[Ljava/util/List;`, identity `(Ljava/lang/Object;)Ljava/lang/Object;` и first `(Ljava/lang/Runnable;)Ljava/lang/Runnable;`. Это наблюдение javac 21, не полный разбор формата classfile.

## Где берётся ClassCastException после правильного вызова generic-метода

Компилятор проверяет совместимость выражений с обобщёнными типами. Если `get()` статически возвращает `String`, проверка совместимости значения с требуемым типом при выполнении зависит от контекста использования результата. Небезопасные операции с сырыми типами (raw types) и непроверяемые преобразования (unchecked conversions) способны нарушить статический контракт.

~~~java
List<String> strings = new ArrayList<>();
List raw = strings;            // raw-ссылка: обход параметра String
raw.add(67);                  // компилируется с unchecked warning

try {
    String text = strings.get(0); // ClassCastException при использовании результата как String
} catch (ClassCastException expected) {
    System.out.println("Значение не String");
}
~~~

В момент `raw.add(67)` объект `Integer` попал в тот же список; сама запись не обязана бросать исключение. Ошибка обнаруживается, когда извлечённое значение должно соответствовать ожидаемому `String`. Это не означает, что *любой* unchecked warning обязательно приводит к исключению. Но предупреждение показывает: компилятор не смог обеспечить всю гарантию типизации. [JLS §4.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.8), [§5.1.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.9).

`List raw` — **raw type**, используемый главным образом для совместимости с устаревшими API. Современный код, если элементный тип действительно неизвестен, обычно должен предпочесть `List<?>`: он разрешает безопасное чтение как `Object`, не давая свободно записывать произвольные элементы. Raw-ссылка разрешает больше небезопасных вызовов, но не делает программу более типобезопасной.

Сырой тип (raw type) и непроверяемое преобразование (unchecked conversion) — разные понятия. По [JLS §4.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.8) при вызове метода/конструктора через сырой тип предупреждение о непроверяемом вызове (unchecked call) возникает, если стирание меняет хотя бы один тип формального параметра. Одно лишь изменение типа результата или типов в `throws` недостаточно. При записи в поле компилятор предупреждает, если стирание меняет тип этого поля; при одном лишь чтении такого предупреждения нет. Собственные нестатические члены сырого класса имеют стёртые типы, унаследованные определяются через соответствующий супертип (например, у необобщённого суперкласса сигнатура `String label()` сохраняется), статические члены сохраняют объявленные типы. Само объявление сырой ссылки может вызвать отдельное предупреждение `rawtypes` у javac; присваивание ссылки сырого типа переменной параметризованного типа — другой случай unchecked conversion (§5.1.9).

Сравним четыре операции: чтение через сырую ссылку, чтение через типизированную ссылку с присваиванием результата переменной `Object`, вызов `get` без использования результата и присваивание переменной `String`. Предупреждения намеренны, поэтому при компиляции этого примера не включайте `-Werror`.

Сохраните `RawReads.java`:

~~~java
import java.util.*;
class RawReads {
    static class Legacy { String label() { return "legacy"; } }
    static class Box<T> extends Legacy { T get() { return null; } static String tag() { return "static"; } }
    public static void main(String[] args) {
        List<String> strings = new ArrayList<>();
        List raw = strings;
        raw.add(67);
        Object a = raw.get(0), b = strings.get(0);
        strings.get(0);
        System.out.println(a + ":" + b + ":ignored ok");
        try { String s = strings.get(0); throw new AssertionError(s); }
        catch (ClassCastException e) { System.out.println("typed=CCE"); }
        List<String> assigned = raw;
        System.out.println(assigned.size());
        List checked = Collections.checkedList(new ArrayList<String>(), String.class);
        try { checked.add(67); throw new AssertionError(); }
        catch (ClassCastException e) { System.out.println("checked=CCE at add"); }
        Box box = new Box<String>();
        System.out.println(Box.tag() + ":" + box.label() + ":" + box.get());
    }
}
~~~

Фактический вывод по строкам: `67:67:ignored ok`, `typed=CCE`, `1`, `checked=CCE at add`, `static:legacy:null`.

Три предупреждения `rawtypes` относятся к объявлениям сырых ссылок, два предупреждения `unchecked` — к вызовам `add`, одно `unchecked` — к присваиванию ссылки `raw` переменной `List<String>`. Для чтения в `Object` и вызова `get` без использования результата здесь не требуется проверка совместимости со `String`. В выводе `javap -classpath out -c -p RawReads` вызовы `get` на смещениях 24, 32 и 41 не сопровождаются приведением к `String` (на 46 — `pop`); после `get` на 69 идёт `checkcast String` на 74. Это наблюдение конкретного компилятора, а не обязательное приведение после каждого `get`.

[Collections.checkedList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#checkedList(java.util.List,java.lang.Class)) возвращает представление с динамической проверкой вставляемых элементов: неверный тип отклоняется сразу. Гарантия отсутствия неверно типизированных элементов требует, чтобы исходный список уже был корректен и дальнейший доступ осуществлялся через представление; изменение исходного списка через другую ссылку (alias) позволяет обойти проверку.

### Непроверяемое приведение не обязательно пропускает проверку класса

По [JLS §5.1.6.2–3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.6.2) допустимое непроверяемое сужающее ссылочное преобразование из `S` в тип `T`, не являющийся типом-пересечением, полностью непроверяемое (completely unchecked), если стирание `S` является подтипом стирания `T` (включая равенство типов); иначе оно частично непроверяемое (partially unchecked). Во втором случае нужна проверка совместимости со стёртым целевым типом при выполнении, в первом — такая проверка при самом приведении не требуется. Для типов-пересечений правило уточняется по составляющим; здесь рассматриваем обычные ссылочные типы.

`Object → List<String>` — частично непроверяемое приведение: проверяется совместимость объекта с интерфейсом `List`, а не тип элементов `String`. `List<?> → List<String>` — полностью непроверяемое: стёртые типы совпадают, но последующее использование элемента может потребовать отдельной проверки. Не следует смешивать эти приведения с присваиванием ссылки сырого типа переменной параметризованного типа по §5.1.9. См. [CORE-CONVERSIONS](conversions.md).

Сохраните `CastCases.java`:

~~~java
import java.util.*;
class CastCases {
    public static void main(String[] args) {
        Object number = 67;
        try { List<String> a = (List<String>) number; throw new AssertionError(a); }
        catch (ClassCastException e) { System.out.println("partial=CCE at cast"); }
        List<?> unknown = List.of(67);
        List<String> claimed = (List<String>) unknown;
        System.out.println("complete=cast ok:" + claimed.size());
        try { String s = claimed.get(0); throw new AssertionError(s); }
        catch (ClassCastException e) { System.out.println("complete=CCE at read"); }
    }
}
~~~

Фактический вывод: `partial=CCE at cast`, `complete=cast ok:1`, `complete=CCE at read`. При компиляции без `-Werror` получены два предупреждения о непроверяемом приведении (unchecked cast). В выводе `javap` для частичного приведения есть `checkcast List` на смещении 7, для полного такой проверки списка нет; после вызова `get` на 59 при типизированном чтении следует `checkcast String` на 64. Таким образом, два предупреждения одной категории не означают одинакового поведения при выполнении.

## Reifiable / non-reifiable: что runtime может проверить (GE-11, GE-12, GE-18)

**Reifiable** — тип, информация о котором полностью доступна при выполнении в смысле [JLS §4.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.7). Исчерпывающий перечень: необобщённый класс или интерфейс; параметризация, у которой все аргументы — неограниченные подстановочные аргументы (wildcards); сырой тип; примитив; массив с типом элемента, являющимся reifiable; вложенный тип, где каждый компонент типа, разделённый точкой, reifiable. Например, `Outer<?>.Inner<?>` удовлетворяет условию, `Outer<?>.Inner<String>` — нет. Тип-пересечение не является reifiable. `List<String>` и `List<? extends Number>` тоже non-reifiable. Это отдельное свойство типа, не универсальное разрешение всех синтаксических конструкций.

Однако неверно говорить «в Java невозможно `instanceof` с любым non-reifiable типом». С Java 16 ключевое правило [JLS §15.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2) связано с **совместимостью при проверяемом приведении (checked cast compatibility)** ([§5.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.5)). Сравним:

~~~java
List<String> typed = new ArrayList<>();
boolean first = typed instanceof List<?>;             // корректно
boolean second = typed instanceof ArrayList<String>; // корректно в Java 21

Object object = new ArrayList<String>();
boolean third = object instanceof List<?>;           // корректно
// boolean fourth = object instanceof List<String>;  // ошибка компиляции
~~~

Во втором случае статический тип выражения ограничивает допустимую параметризацию при проверяемом приведении к `ArrayList<String>`. JVM проверяет совместимость объекта со стёртым целевым типом, но не проверяет аргументы типа элементов списка. В четвёртом случае статический `Object` не даёт такой гарантии: приведение к `List<String>` потребовало бы непроверяемого преобразования (unchecked), поэтому `instanceof` запрещён. Формула «справа всегда должен стоять reifiable тип» является **устаревшим обобщением** для Java 21.

Проверяемая совместимость приведения опирается на статические предположения, которые небезопасный код способен нарушить. `instanceof` возвращает false для null. Следующий пример специально загрязняет список через другую ссылку сырого типа; компилируйте без `-Werror`.

Сохраните `PollutedCheck.java`:

~~~java
import java.util.*;
class PollutedCheck {
    public static void main(String[] args) {
        List<String> typed = new ArrayList<>();
        List raw = typed;
        raw.add(67);
        System.out.println(typed instanceof ArrayList<String>);
        List<String> absent = null;
        System.out.println(absent instanceof ArrayList<String>);
        try { String s = typed.get(0); throw new AssertionError(s); }
        catch (ClassCastException e) { System.out.println("CCE at read"); }
    }
}
~~~

Фактический вывод: `true`, `false`, `CCE at read`. Компилятор выдал предупреждения `rawtypes` и `unchecked` о вызове через сырой тип, но не отверг проверяемое instanceof. JVM проверила принадлежность объекта классу ArrayList, а не элементы: присваивание результата переменной `String` вызвало `ClassCastException`. Не используйте положительный результат instanceof для доказательства отсутствия загрязнения.

Похожее ограничение объясняет, почему запрещены конструкции:

~~~java
class Factory<T> {
    // T item = new T();         // у T нет известного конструктора как у класса
    // Class<T> type = T.class;  // class literal переменной типа запрещён
    // T[] items = new T[2];    // нельзя создать массив с таким компонентным типом
}

// List<String>[] array = new List<String>[2]; // нельзя создать generic array
List<?>[] wildcardArrays = new List<?>[2];    // допустимо
~~~

В случае `new T()` есть также принципиальная проблема выбора конкретного конструктора, а не только стирания типов. При создании массива необходим тип компонента, допускающий предусмотренные языком проверки записи во время выполнения. `List<?>[]` допустим поскольку тип компонента является reifiable, но не превращает произвольный массив в безопасный `List<String>[]`. [JLS §10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html), [§15.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9).

Литерал класса (class literal) подчиняется отдельной грамматике [JLS §15.8.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.8.2): `List.class` и `int.class` допустимы, `List<?>.class` и `List<String>.class` — нет, хотя `List<?>` относится к reifiable-типам. Переменная типа T также не разрешена в T.class. У `Object o` проверка `o instanceof List<? extends Number>` запрещена из-за отсутствия совместимости при проверяемом приведении, а `o instanceof List<?>` допустима. Это не отменяет правила Java 16+ для статически совместимых параметризаций.

Объявление `T[] items` допустимо; запрещено непосредственно создавать `new T[2]`. Передайте уже существующий массив либо фабрику: `Supplier<T>` может создавать значение (в том числе вызовом известного конструктора), `IntFunction<T[]>` — массив запрошенной длины. Фабрика определяет фактический класс результата; обещание размера и допустимых значений зависит от её контракта. Токен класса `Class<T>` также может служить входом для [Array.newInstance](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Array.html#newInstance(java.lang.Class,int)), но результат API имеет тип Object; токен `List.class` не содержит аргумента типа `String` и не делает произвольное приведение к T[] безопасным. Не используйте непроверяемое приведение массива как универсальный рецепт. См. [CORE-ARRAYS](arrays.md) и [CORE-FUNCTIONS](functions.md).

Сохраните `ArrayFactories.java`:

~~~java
import java.util.*;
import java.util.function.*;
class ArrayFactories {
    static class Box<T> { final T[] items; Box(T[] items) { this.items = items; } }
    static <T> T create(Supplier<T> factory) { return factory.get(); }
    static <T> T[] allocate(IntFunction<T[]> factory, int size) { return factory.apply(size); }
    static class Outer<T> { class Inner<U> {} }
    public static void main(String[] args) {
        Box<String> box = new Box<>(new String[]{"a"});
        String[] array = allocate(String[]::new, 2);
        System.out.println(box.items.getClass().getTypeName() + ":" + array.length + ":" + create(StringBuilder::new).getClass().getSimpleName());
        List<?>[] lists = new List<?>[1]; lists[0] = List.of(67);
        System.out.println(lists.getClass().getComponentType().getTypeName() + ":" + lists[0].get(0));
        Object[] alias = lists;
        try { alias[0] = "not a list"; throw new AssertionError(); }
        catch (ArrayStoreException e) { System.out.println("ASE at store"); }
        Object inner = new Outer<String>().new Inner<Integer>();
        System.out.println(inner instanceof Outer<?>.Inner<?>);
        System.out.println(List.class.getName() + ":" + int.class.getName());
    }
}
~~~

Фактический вывод: `java.lang.String[]:2:StringBuilder`, `java.util.List:67`, `ASE at store`, `true`, `java.util.List:int`.

Первая строка: переданный `String[]` сохранил свой класс; фабрики создали массив длины 2 и объект `StringBuilder`. Вторая строка: фактический тип компонента массива `List<?>[]` — `List`, а первый список содержит 67. Третья строка: запись строки через ссылку `Object[]` отклонена проверкой типа компонента (`ArrayStoreException`), а не проверкой аргумента типа элементов списка. Четвёртая — успешная проверка `instanceof` для вложенного типа с двумя неограниченными подстановочными аргументами. Последняя показывает допустимые литералы классов. [JLS §10.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html#jls-10.5), [§15.10.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.10.1). Отдельные отрицательные файлы ниже.

## Heap pollution: когда статическое обещание нарушено (GE-13, GE-14)

**Heap pollution** возникает, когда переменная параметризованного типа ссылается на объект, тип которого несовместим с её статическим обещанием. Она может возникнуть при использовании сырых типов, непроверяемых преобразований и некоторых операций с параметрами переменной арности. Это не просто «ошибка в куче»: термин описывает нарушение типовой согласованности, а не физическую порчу памяти JVM. [JLS §4.12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.2).

Рассмотрим метод с параметром переменной арности (varargs) `List<String>... lists`: его формальный параметр имеет тип массива. При вызове в режиме переменной арности по правилам вызова формируется массив аргументов, а его компонентный тип при необходимости стирается. При вызове в режиме фиксированной арности с уже совместимым массивом передаётся ссылка на этот массив. Это семантика языка, а не обещание физического выделения памяти после оптимизации.

Через другую ссылку на тот же массив (alias) вызываемый метод может записать несовместимый по параметризации список, если фактический тип компонента массива допускает такую запись. При более узком типе компонента `ArrayStoreException` может возникнуть уже на записи. [JLS §8.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.1), [§15.12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.2), [§15.12.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.4.2), [CORE-CONVERSIONS](conversions.md).

~~~java
@SafeVarargs
static String unsafeFirst(List<String>... lists) {
    Object[] alias = lists;
    alias[0] = List.of(67);
    return lists[0].get(0);
}

try {
    unsafeFirst(List.of("ok")); // выбросит ClassCastException
} catch (ClassCastException expected) {
    System.out.println("Обещание List<String> нарушено");
}
~~~

Метод `unsafeFirst` присваивает ссылку `lists` переменной `alias` типа `Object[]`. Затем он записывает через `alias[0]` объект `List.of(67)` в тот же массив: фактический тип компонента позволяет принять `List`. При чтении `lists[0].get(0)` метод пытается использовать `Integer` как `String`, что приводит к `ClassCastException`. В таком методе `@SafeVarargs` **не обеспечивает безопасность**: аннотацией автор утверждает, что тело не выполняет потенциально небезопасных операций с параметром переменной арности. Она не вводит дополнительную проверку при выполнении и не исправляет запись через другую ссылку.

Компилятор может выдавать предупреждения даже при наличии аннотации, если включить соответствующие категории проверок: `javac --release 21 -Xlint:unchecked -Xlint:varargs ...`. Нельзя фиксировать точный текст предупреждения как норму JLS; важны условия небезопасной операции. [JLS §9.6.4.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4.7), [API @SafeVarargs](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/SafeVarargs.html).

### Где допустима аннотация @SafeVarargs (GE-24)

В Java 21 аннотацию можно использовать на статическом методе, методе экземпляра с `final` или `private`, а также на конструкторе, если объявлен параметр переменной арности. Компилятор проверяет допустимость аннотации, но **не доказывает** безопасность тела. Переопределяемый публичный метод экземпляра аннотировать нельзя. Автор метода должен проверить, не записывает ли он несовместимые значения в массив аргументов и не передаёт ли ссылку на этот массив туда, где возможны небезопасные операции. Не следует использовать `@SafeVarargs` только ради устранения предупреждений.

Аннотация разрешена **только для объявления с переменным числом аргументов**. Метод с параметром String[] без многоточия аннотировать нельзя, даже если он статический. По [JLS §9.6.4.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4.7) у SafeVarargs нелокальный эффект: она подавляет предупреждение `unchecked` о параметре переменной арности с non-reifiable типом элемента в объявлении и предупреждение о создании непроверяемого массива в местах вызова. `@SuppressWarnings("unchecked")` на объявлении метода имеет локальный эффект и не даёт такого обещания внешним вызывающим сторонам. Это не подавление всех диагностик и не доказательство безопасности тела.

Сначала пример без аннотации (компилируется без `-Werror`):

Сохраните `VarargsWarnings.java`:

~~~java
import java.util.*;
class VarargsWarnings {
    static int count(List<String>... lists) { return lists.length; }
    public static void main(String[] args) { System.out.println(count(List.of("a"))); }
}
~~~

Вызов `count(List.of("a"))` в режиме переменной арности возвращает длину массива 1; программа печатает `1`. Фактически получены два предупреждения `unchecked`: `unchecked.varargs.non.reifiable.type` у объявления `count` и `unchecked.generic.array.creation` у его вызова.

Теперь пример с аннотацией на намеренно опасном теле (компилируется без `-Werror`):

Сохраните `VarargsUnsafe.java`:

~~~java
import java.util.*;
class VarargsUnsafe {
    @SafeVarargs static String first(List<String>... lists) {
        Object[] alias = lists;
        alias[0] = List.of(67);
        return lists[0].get(0);
    }
    public static void main(String[] args) {
        try { first(List.of("a")); throw new AssertionError(); }
        catch (ClassCastException e) { System.out.println("CCE"); }
    }
}
~~~

Вызов `first(List.of("a"))` формирует массив аргументов; метод меняет его первый элемент через другую ссылку, а затем пытается прочитать значение как `String`. Программа перехватывает исключение и печатает `CCE`. Два предыдущих вида предупреждений в этом примере подавлены аннотацией, но `-Xlint:varargs` выявил `varargs.unsafe.use.varargs.param` при присваивании ссылки `lists` переменной `alias`. Компилятор не обязан диагностировать всякое нарушение; отсутствие предупреждений не доказывает безопасность.

В безопасном примере метод читает значения из массива аргументов переменной арности и передаёт их в коллекцию, не меняя и не публикуя сам массив. Реальный API [Collections.addAll](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#addAll(java.util.Collection,T...)) использует `@SafeVarargs static <T> boolean addAll(Collection<? super T> c, T... elements)`; он добавляет элементы в коллекцию, а не обещает успешную вставку при любом её контракте. Это также пример PECS из [CORE-GENERICS](generics.md).

Сохраните `VarargsSafe.java`:

~~~java
import java.util.*;
class VarargsSafe {
    @SafeVarargs static <T> int add(Collection<? super T> target, T... values) {
        for (T value : values) target.add(value);
        return values.length;
    }
    @SafeVarargs private <T> int privateCount(T... values) { return values.length; }
    @SafeVarargs final <T> int finalCount(T... values) { return values.length; }
    @SafeVarargs <T> VarargsSafe(T... values) {}
    public static void main(String[] args) {
        List<Number> target = new ArrayList<>();
        Integer[] supplied = {67, 68};
        int a = add(target, supplied), b = add(target, 69);
        Collections.addAll(target, 70, 71);
        System.out.println(a + ":" + b + ":" + target + ":" + Arrays.toString(supplied));
        VarargsSafe v = new VarargsSafe(List.of("a"));
        System.out.println(v.privateCount(List.of("a")) + ":" + v.finalCount(List.of("b")));
    }
}
~~~

Фактический вывод: `2:1:[67, 68, 69, 70, 71]:[67, 68]`, затем `1:1`.

`add(target, supplied)` — вызов в режиме фиксированной арности с уже созданным `Integer[]`; метод возвращает 2. `add(target, 69)` — вызов в режиме переменной арности, возвращает 1. `Collections.addAll(target, 70, 71)` дополняет коллекцию. Массив `supplied` остался `[67, 68]`. Во второй строке видны результаты вызовов `privateCount` и `finalCount`, каждый возвращает 1; допустимый обобщённый конструктор также был вызван. Все эти операции скомпилированы в строгом режиме без предупреждений. Запрещённые места аннотации проверяют `BadFixedAnnotation` и `BadOverrideAnnotation` ниже.

## Warning не равен ошибке компиляции (GE-23)

| Сценарий | Ожидаемая категория | Что это значит |
|---|---|---|
| `List raw = typed; raw.add(67);` | предупреждение о непроверяемом вызове (unchecked call) | `javac` допускает вызов, но типовая гарантия ослаблена |
| `List<String> list = raw;` | предупреждение о непроверяемом преобразовании (unchecked conversion) | сырой тип исходной ссылки не гарантирует соответствие `String` |
| `(List<String>) object` при подходящем статическом типе выражения | возможно предупреждение о непроверяемом приведении (unchecked cast) | приведение может быть разрешено, но аргумент типа не проверяется при выполнении |
| `new List<String>[2]` | ошибка компиляции | массив с non-reifiable компонентным типом нельзя создать |
| Два `m(List<String>)` / `m(List<Integer>)` | ошибка компиляции | конфликт после стирания сигнатур |

Для изолированного опыта используйте `javac --release 21 -XDrawDiagnostics -Xlint:all File.java` (в частности, категории rawtypes, unchecked, varargs). Для примеров, которые намеренно вызывают предупреждения, не включайте `-Werror`, иначе предупреждение станет причиной неуспешной сборки. Для корректных и отдельно отрицательных примеров используйте также `-Werror`. Предупреждение само по себе не является ошибкой языка. Новые RawReads, CastCases, VarargsWarnings и VarargsUnsafe воспроизводят механизмы четырёх отдельных проверок W01–W04 принятого плана, а не заменяют всю историческую серию. Точные позиции и ключи диагностик — в протоколе ниже.

## Почему overload только по generic-аргументу невозможен (GE-15)

~~~java
class Overloads {
    // void use(List<String> items) { }
    // void use(List<Integer> items) { }
}
~~~

Если раскомментировать оба метода, `javac` отвергнет объявления: оба имеют одинаковую **сигнатуру после стирания** `use(List)`. Это не просто правило «JVM игнорирует generics», а нормативное ограничение совместимости сигнатур и конфликта имён (name clash). Перегрузка, различающаяся другими параметрами или действительно различными типами после стирания, может оставаться допустимой; выводить запрет на *любые* перегрузки обобщённых методов было бы неверно. [JLS §8.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.2), [§4.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.6).

## Переопределение после стирания: откуда bridge (GE-16)

Правила переопределения методов обобщённого базового класса проверяются на уровне исходного кода Java. После стирания дескрипторы методов родителя и наследника могут различаться. Компилятор обязан сохранить предусмотренное языком полиморфное поведение; для этого он может сформировать **посреднический метод (bridge method)**.

~~~java
class Parent<T> {
    T get() { return null; }
}

class Child extends Parent<String> {
    @Override String get() { return "child"; }
}

Parent<String> parent = new Child();
System.out.println(parent.get()); // child
~~~

В Java-коде `Child.get()` корректно переопределяет метод родителя с учётом аргумента типа `String`. После стирания результат `Parent<T>.get()` имеет тип `Object`, а результат `Child.get()` — `String`. Для вызова через базовый тип компилятор может создать посреднический метод (bridge method), сохраняющий требуемое поведение. Спецификация языка не устанавливает универсального фиксированного числа посреднических или синтетических методов (bridge/synthetic) для всех компиляторов.

Наличие bridge у конкретного `javac` можно наблюдать через reflection:

~~~java
for (var method : Child.class.getDeclaredMethods()) {
    if (method.isBridge()) {
        System.out.println("bridge: " + method.getName());
    }
}
~~~

Этот фрагмент зависит от представления конкретного скомпилированного класса; корректнее говорить «`javac 21` в данной проверке сгенерировал посреднический метод», если проверка действительно выполнена, а не обещать одинаковый результат любой JVM. Обобщённые сигнатуры/вывод типов и правила переопределения: [JLS §8.4.8.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.3), [Method.isBridge API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Method.html#isBridge()).

Есть и другой вид посреднического метода: у `Parent<T>.put(T)` после стирания параметр Object, а у `Child extends Parent<String>` переопределяющий метод принимает String. Вызов через Parent<String> выбирает специализированную реализацию. Через ссылку сырого типа `Parent` компилятор допускает передачу `Integer` с предупреждением. В классе, полученном при проверке с `javac`, исключение возникает внутри посреднического метода **до** входа в put(String).

Сохраните `BridgeCase.java`:

~~~java
class BridgeCase {
    static class Parent<T> { void put(T value) {} }
    static class Child extends Parent<String> {
        int calls;
        @Override void put(String value) { calls++; System.out.println("child=" + value); }
    }
    public static void main(String[] args) {
        Child child = new Child(); Parent<String> typed = child;
        typed.put("ok");
        Parent raw = child;
        try { raw.put(67); throw new AssertionError(); }
        catch (ClassCastException e) { System.out.println("bridge=CCE:" + child.calls); }
        for (var m : Child.class.getDeclaredMethods()) {
            if (m.isBridge()) System.out.println(m.getParameterTypes()[0].getSimpleName() + ":" + m.isSynthetic());
        }
    }
}
~~~

Фактический вывод: `child=ok`, `bridge=CCE:1`, `Object:true`. Тело `put(String)` вызвано только один раз: второй вызов через сырую ссылку завершился исключением до входа в тело. Через рефлексию у посреднического метода обнаружены параметр `Object` и признак `synthetic`. При компиляции без `-Werror` получены два предупреждения: `rawtypes` для `Parent` и `unchecked` о вызове `put(T)`. Фактический фрагмент `javap -classpath out -c -p -v 'BridgeCase$Child'`:

~~~text
  void put(java.lang.Object);
    descriptor: (Ljava/lang/Object;)V
    flags: (0x1040) ACC_BRIDGE, ACC_SYNTHETIC
    Code:
      stack=2, locals=2, args_size=2
         0: aload_0
         1: aload_1
         2: checkcast     #29                 // class java/lang/String
         5: invokevirtual #31                 // Method put:(Ljava/lang/String;)V
         8: return
~~~

Посреднический метод с дескриптором `(Ljava/lang/Object;)V` проверяет совместимость аргумента с `String` (`checkcast` на смещении 2), затем вызывает реализацию с дескриптором `(Ljava/lang/String;)V` (`invokevirtual` на 5). Флаги описаны в [JVMS §4.6](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html#jvms-4.6). Номера констант и смещения — наблюдение этого javac, не универсальное требование к числу или расположению методов. В исходнике Child.put(String) — переопределение, а не перегрузка, возвращаемый тип не входит в Java-сигнатуру. Напротив, если в наследнике P<String> написать f(Object) при объявлении P<T>.f(T), это не переопределяет f(String), а стёртые исходные объявления конфликтуют. Отдельный пример `BadInheritedClash` отклонён с диагностикой `name.clash.same.erasure.no.override`; `BadClash` показывает конфликт двух перегрузок в одном классе, а `BadOverrideReturn` — несовместимый тип результата при переопределении. [JLS §8.4.8.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.3), [CORE-INHERITANCE](inheritance.md).

## Generic-метаданные через Reflection: что можно увидеть (GE-22, GE-29)

Erasure не стирает всякую информацию **об объявлениях**: reflection может возвращать generic return type и parameterized type **декларации метода**. Например, `Method.getGenericReturnType()` может сообщить, что возвращаемый тип метода `List<String>`. Но `new ArrayList<String>().getClass()` сообщает runtime-класс объекта и не восстанавливает «какой `String` аргумент был выбран при создании» исключительно из экземпляра.

~~~java
class Api {
    public List<String> names() { return List.of("a"); }
}

var method = Api.class.getMethod("names");
System.out.println(method.getGenericReturnType()); // java.util.List<java.lang.String> для javac21
~~~

Это не проверка типов произвольных элементов списка. Если рефлексия не находит нужное объявление или метаданные недоступны, ситуация требует отдельного анализа. Для дальнейшего изучения см. [CORE-REFLECTION](reflection.md); полный разбор `ParameterizedType` и атрибутов файла класса, включая `Signature`, остаётся за пределами этой статьи.

На одном объекте Method сравним [getReturnType](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Method.html#getReturnType()) и [getGenericReturnType](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Method.html#getGenericReturnType()). Первое возвращает Class, второе — Type с объявленной информацией. Анонимный подкласс `new ArrayList<String>() {}` объявляет конкретный параметризованный суперкласс, который может быть отражён через [getGenericSuperclass](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html#getGenericSuperclass()). Это метаданные объявления нового подкласса, не механизм узнавания произвольного аргумента типа у обычного объекта. Поиск метода может завершиться исключением `NoSuchMethodException`, которое компилятор требует обработать или объявить в `throws`. В примере `main` объявляет `ReflectiveOperationException`.

Сохраните `MetadataCase.java`:

~~~java
import java.util.*;
class MetadataCase {
    static class Api { public List<String> names() { return List.of("a"); } }
    public static void main(String[] args) throws ReflectiveOperationException {
        var method = Api.class.getMethod("names");
        System.out.println(method.getReturnType().getName() + ":" + method.getGenericReturnType().getTypeName());
        var ordinary = new ArrayList<String>();
        var anonymous = new ArrayList<String>() {};
        System.out.println(ordinary.getClass().getName());
        System.out.println(anonymous.getClass().getGenericSuperclass().getTypeName());
    }
}
~~~

Фактический вывод состоит из трёх строк: `java.util.List:java.util.List<java.lang.String>`, `java.util.ArrayList`, `java.util.ArrayList<java.lang.String>`. Первая строка относится к одному объявлению `names`, третья — к объявленному суперклассу анонимного класса. Ни одна не проверяет все элементы списка и не доказывает отсутствие загрязнения. Полный разбор `Signature` и `ParameterizedType` остаётся за рамками.

## Другие ограничения параметризованных типов (GE-20)

~~~java
// class GenericProblem<T> extends Exception { } // ошибка компиляции
~~~

Обобщённый класс нельзя делать прямым или косвенным подклассом `Throwable` ([JLS §8.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.2)). Это связано с требованиями к исключениям при выполнении и отсутствием необходимой информации об аргументах типа во время выполнения. Но запрет на обобщённый класс исключения не запрещает обобщённый метод с переменной типа в разделе `throws`, если выполнены требования к её границе.

Переменную типа с верхней границей `Throwable` или его подклассом разрешено использовать в `throws` ([JLS §8.4.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.6)). Но [§14.20](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20) запрещает переменную типа в объявлении параметра `catch`. Это отдельное правило, не запрет объявления обобщённого метода. При вызове `fail(new IOException("67"))` компилятор учитывает возможность проверяемого исключения `IOException`; в примере оно обработано соответствующим блоком `catch`. См. [CORE-EXCEPTIONS](exceptions.md).

Сохраните `ThrowsCase.java`:

~~~java
import java.io.IOException;
class ThrowsCase {
    static <T extends Exception> void fail(T e) throws T { throw e; }
    public static void main(String[] args) {
        try { fail(new IOException("67")); }
        catch (IOException e) { System.out.println(e.getClass().getSimpleName() + ":" + e.getMessage()); }
    }
}
~~~

Фактический вывод `IOException:67`, строгая компиляция без предупреждений. `BadCatchT` и `BadGenericThrowable` ниже отвергнуты по разным основаниям.

## Краткий разбор типичных неверных ответов

- «Generics исчезают полностью» — неверно: остаются runtime-классы, проверки/casts и метаданные *объявлений*.
- «Любой unchecked немедленно бросит ClassCastException» — неверно: исключение возможно в конкретной точке использования, а может не возникнуть.
- «`instanceof List<String>` всегда запрещён» — неверно для Java 21: критерий — checked cast compatibility с **левым статическим типом**.
- «`@SafeVarargs` обезопасит метод» — неверно: тело обязано быть действительно безопасным.
- «Раз `List<String>[]` нельзя, то вообще любые массивы списков нельзя» — неверно: `List<?>[]` допустим.
- «После стирания overriding ломается» — неверно: исходный контракт сохраняется, в том числе благодаря bridge там, где он необходим в реализации.
- «Статический тип `List<String>` — отдельный класс в JVM» — неверно: объект может иметь обычный runtime-класс `ArrayList`.

## Отдельные отрицательные компиляции D-A11b

Каждый файл компилируется отдельно; эти примеры не входят в успешный скрипт и не являются полным повтором исторических N01–N22. Сначала создание и литералы класса, затем массивы и instanceof:

`BadNewT.java`:

~~~java
class BadNewT<T> { T x = new T(); }
~~~

`BadTClass.java`:

~~~java
class BadTClass<T> { Class<T> c = T.class; }
~~~

`BadTArray.java`:

~~~java
class BadTArray<T> { T[] a = new T[1]; }
~~~

`BadListArray.java`:

~~~java
import java.util.*; class BadListArray { List<String>[] a = new List<String>[1]; }
~~~

`BadWildcardLiteral.java`:

~~~java
import java.util.*; class BadWildcardLiteral { Class<?> c = List<?>.class; }
~~~

`BadArrayAssignment.java`:

~~~java
import java.util.*; class BadArrayAssignment { List<String>[] a = new List<?>[1]; }
~~~

`BadInstanceof.java`:

~~~java
import java.util.*; class BadInstanceof { boolean f(Object x) { return x instanceof List<String>; } }
~~~

`BadBoundedInstanceof.java`:

~~~java
import java.util.*; class BadBoundedInstanceof { boolean f(Object x) { return x instanceof List<? extends Number>; } }
~~~

Фактические причины: type.found.req/type.parameter/type.req.class; type.var.cant.be.deref; generic.array.creation (два файла); expected token.identifier (две диагностики для BadWildcardLiteral); несовместимое присваивание List<?>[] в List<String>[]; instanceof.reifiable.not.safe для обоих Object-примеров. Последний ключ — формулировка javac, не универсальное требование reifiability справа.

Места аннотации и конфликты методов:

`BadFixedAnnotation.java`:

~~~java
class BadFixedAnnotation { @SafeVarargs static void f(String[] a) {} }
~~~

`BadOverrideAnnotation.java`:

~~~java
class BadOverrideAnnotation { @SafeVarargs void f(java.util.List<String>... a) {} }
~~~

`BadClash.java`:

~~~java
import java.util.*; class BadClash { void f(List<String> x) {} void f(List<Integer> x) {} }
~~~

`BadInheritedClash.java`:

~~~java
class BadInheritedClash { static class P<T> { void f(T x) {} } static class C extends P<String> { void f(Object x) {} } }
~~~

`BadOverrideReturn.java`:

~~~java
class BadOverrideReturn { static class P<T> { T get() { return null; } } static class C extends P<String> { @Override Object get() { return null; } } }
~~~

Фактические ключи: varargs.invalid.trustme.anno с non.varargs.meth / virtual.varargs; name.clash.same.erasure; name.clash.same.erasure.no.override; override.incompatible.ret и method.does.not.override.superclass. Последний файл дал две ошибки. Наконец, исключения:

`BadGenericThrowable.java`:

~~~java
class BadGenericThrowable<T> extends Exception {}
~~~

`BadCatchT.java`:

~~~java
class BadCatchT { static <T extends Exception> void f(T e) { try { throw e; } catch (T caught) {} } }
~~~

Ключи диагностик: `generic.throwable` и `type.found.req/type.parameter/type.req.class`. Дополнительно `BadGenericThrowable` дал предупреждение `missing.SVUID` об отсутствии `serialVersionUID`; причина отказа — именно запрет обобщённого подкласса `Throwable`.

Все 15 независимых компиляций завершились с кодом 1. Получено 17 диагностик ошибок (две у `BadWildcardLiteral`, две у `BadOverrideReturn` и по одной у остальных 13 файлов) и одно дополнительное предупреждение. Число диагностик не равно числу отклонённых файлов.

## Вопросы для самопроверки

| Вопрос | Ответ |
|---|---|
| Во что стирается T[] при T extends List<String>? | List[]; применяется стирание крайней левой границы и компонента массива. |
| Входит ли результат в сигнатуру Java-метода? | Нет; дескриптор JVM содержит и параметры, и результат. |
| Почему чтение в `Object` может пройти, а чтение в `String` — нет? | В проверенном примере использование результата как `String` требует проверки совместимости, а присваивание переменной `Object` — нет. |
| Когда непроверяемое приведение может завершиться исключением? | При частично непроверяемом приведении проверяется совместимость со стёртым целевым типом; полностью непроверяемое приведение такой проверки не требует, но последующее использование значения как конкретного типа может привести к исключению. |
| List<?> reifiable; допустим ли List<?>.class? | Нет: отдельная грамматика class literal. |
| Доказывает ли true у typed instanceof ArrayList<String> тип всех элементов? | Нет, загрязнение может нарушить статическое предположение. |
| Чем T[] отличается от new T[1]? | Объявление допустимо, непосредственное создание запрещено; можно передать массив или фабрику. |
| Что именно подавляет SafeVarargs? | Конкретные unchecked предупреждения объявления и вызова, не доказывает безопасность. |
| Почему raw.put(67) не входит в тело put(String)? | В проверенном javac посредник put(Object) сначала проверяет String. |
| Можно ли throws T и catch(T e)? | Первое разрешено при подходящей границе, второе запрещено. |

## Фактические проверки D-A11b

Роль 02 выполнила 26 отдельных компиляций временных файлов на Debian OpenJDK/javac 21.0.12.1, `LC_ALL=C`. Команда: `javac --release 21 -XDrawDiagnostics -Xlint:all -Werror -d out Имя.java`; для шести файлов с намеренными предупреждениями (`RawReads`, `CastCases`, `PollutedCheck`, `VarargsWarnings`, `VarargsUnsafe`, `BridgeCase`) опущен только `-Werror`. После успешной компиляции выполнялась команда `java -cp out Имя`.

Пять строгих компиляций завершились с кодом 0 без диагностик; шесть компиляций с намеренными предупреждениями — с кодом 0 (15 предупреждений). Остальные 15 завершились с ожидаемым кодом 1 (17 диагностик ошибок и одно дополнительное предупреждение `serial`). У всех компиляций stdout пуст. Все 11 запусков JVM завершились с кодом 0 и пустым stderr; предусмотренные исключения перехвачены намеренно.

Четыре отдельные команды `javap` для `ErasedTypes`, `RawReads`, `CastCases` и `BridgeCase$Child` завершились с кодом 0 без stderr. Наблюдаемые дескрипторы, инструкции и флаги приведены в соответствующих разделах статьи.

Полные тела проверенных исходников приведены в [статье на теоретическом HEAD `9e68d9a0e6dcbb50a397762e2aff6a5a27def72f`](https://github.com/Melchiorius/backend-interview-labs/blob/9e68d9a0e6dcbb50a397762e2aff6a5a27def72f/knowledge/01-java-core/erasure.md). Их Git-хеши, команды, позиции и ключи диагностик, вывод и результаты `javap` зафиксированы в [фактическом протоколе 02 #7172268517](https://github.com/Melchiorius/backend-interview-labs/issues/308#issuecomment-7172268517). [Issue #308](https://github.com/Melchiorius/backend-interview-labs/issues/308) задаёт объём задачи.

Десять исторических контекстных блоков сохранены; не все из них являются самостоятельными файлами. Полная серия G1 не повторялась. Не проверялись все компиляторы, бинарная совместимость, JIT, производительность, полная модель файла класса и графические интерфейсы. Редакционная правка не является повторным запуском этих экспериментов.

**Первоначальная проверка общего примера ролью 02.** Из точного BASE `443713301242a12d6b2464c4884b537e87110abc` восстановлены три исполняемых входа (`src/GenericsErasureDemo.java`, `expected.stdout`, `verify.sh`) в двух временных деревьях BASE и теоретической версии. README был прочитан удалённо по точному BASE, но в эти деревья ещё не копировался. Git-хеши трёх входов совпали; в обоих деревьях `bash -n` и `bash verify.sh` завершились с кодом 0 и пустым stderr, вывод программы совпал с эталоном, а полные выводы скрипта совпали побайтно.

**Дополнительная проверка ролью 02.** После создания PR в оба временных дерева скопирован точный README; проверены Git-хеши и режимы всех четырёх файлов, повторены `bash -n` и полный запуск `verify.sh` с кодами 0 и пустым stderr. Фактический вывод программы совпал с эталоном (18 строк, 573 байта); полные выводы скрипта в BASE и теоретической версии совпали (19 строк, 618 байт, включая маркер скрипта). Это реконструкция входов, а не полный клон репозитория и не 19 тестов. Подробности в [дополнительном отчёте 02 #7172371159](https://github.com/Melchiorius/backend-interview-labs/pull/309#issuecomment-7172371159).

**Последующее исправление N21 ролью 03.** В исходной версии `copyFirst` выполнялся после `Collections.copy` на уже скопированных значениях: удаление единственной строки тела вспомогательного метода не обнаруживалось проверкой. На интегрированном HEAD `3d506c78598b50c1577800fef280b400bdd493d7` вызов `copyFirst` получает отдельный список назначения `[0, 0]`, а новая проверка требует `[67, 0]`. Прежние `Collections.copy`, проверка `[67, 68]` и её вывод остались отдельными. Сохранены десять прежних вызовов `verify` и добавлен один. См. [README общего примера](../../examples/core/generics-erasure/README.md) и [фактический отчёт 03 #7172572119](https://github.com/Melchiorius/backend-interview-labs/pull/309#issuecomment-7172572119).

В отчёте 03 зафиксированы четыре отдельных варианта: исходный и исправленный подлинные исходники и две временные мутантные копии с удалением единственной строки тела `copyFirst`. Все четыре прошли строгую компиляцию с кодом 0. Исходный подлинный вариант, исходный мутант и исправленный подлинный вариант завершились с кодом 0 при прямом запуске JVM и через `verify.sh`; сравнение с эталоном успешно, вывод программы (18 строк, 573 байта) и вывод скрипта (19 строк, 618 байт) побайтно совпали. Исправленный мутант завершился с кодом 1 при обоих запусках, stdout пуст: сработала именно новая проверка с `AssertionError: copyFirst must replace only the first element`. Скрипт завершился до сравнения и печати вывода; последующее внешнее `cmp` с кодом 1 не было причиной отказа. Мутанты не вошли в репозиторий. Это проверка чувствительности к конкретному изменению, а не исчерпывающее мутационное тестирование.

Обычный [интеграционный CI #45686825922](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45686825922) относится к тому же интегрированному HEAD `3d506c78598b50c1577800fef280b400bdd493d7` и не запускал 26 временных проб или четыре варианта N21. Эти исторические результаты 02 и 03 не являются новыми локальными запусками после редактуры. Для итоговой приёмки нужны актуальный CI и независимый G4.

## Соответствие исследовательскому G1 и пределы статьи

| ID | Раскрытая тема |
|---|---|
| GE-02 (совместно с CORE-GENERICS), GE-10 | Стирание типов: крайняя левая граница, сигнатура метода, класс объекта во время выполнения |
| GE-11 | Java 21 `instanceof`: совместимость при проверяемом приведении, отличие `List<?>` от `List<String>` |
| GE-12 | `new T()`, `T.class`, создание массивов обобщённых типов и reifiable-типы |
| GE-13 | Сырые типы, непроверяемые преобразования, проверки при выполнении и точка `ClassCastException` |
| GE-14 | Heap pollution и `@SafeVarargs` |
| GE-15 | Конфликт сигнатур после стирания |
| GE-16 | Переопределение и посреднические методы (bridge) |
| GE-18, GE-20, GE-22, GE-23, GE-24 (P1) | Массивы `List<?>`, запрет обобщённого подкласса `Throwable`, рефлексия, предупреждения компилятора, допустимость `@SafeVarargs` |
| GE-25 (P1, совместно с [CORE-GENERICS](generics.md)) | Там — контекст вывода типов, обобщённые конструкторы и diamond (N06/N09); здесь — вызовы в режиме фиксированной и переменной арности, ограничения varargs и reifiable-типов, предупреждения (N17). Не полный разбор вывода типов |
| GE-27, GE-28, GE-29 (P2) | Байткод, атрибут `Signature`, `ParameterizedType`, DAO и прокси фреймворков — обозначены границы без заявления полного разбора |

**Не входит в заявленный объём:** полный разбор дескрипторов байткода Java и атрибута `Signature` (выше показаны только необходимые дескрипторы и конкретные инструкции), подробный анализ бинарной совместимости, JIT и размещения объектов в памяти HotSpot, измерение производительности, прокси фреймворков, обобщённые DAO/JPA, восстановление фактического аргумента типа произвольного объекта только по его идентичности. Темы GE-27–29 остаются за указанными границами.

**Источники:** [JLS 21 §4.6–4.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html), [§5.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.5), [§8.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4), [§9.6.4.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4.7), [§15.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2), [API @SafeVarargs](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/SafeVarargs.html), [Method.isBridge](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Method.html#isBridge()). В [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md) нормативные источники отделены от интервью-подборок.

Исторические ориентиры вопросов (не нормативные доказательства и не новый G1): [план CORE-GENERICS-ERASURE §3](../../docs/plans/CORE-GENERICS-ERASURE.md) Q-HRUDAY глава 11 Generics, Q-IN28 Q178–184, Q-CHERNYSHOV §20 вопросы 11–25, Q-TEAMLEAD PECS/static generic method; [первичный аудит A11.2 §2](../../docs/audits/java-core-full-2026-11-24/A11.2-CORE-ERASURE.md) DigitalOcean Java Generics Explained L698–706/747–756, FAQ L918–937/966–1014, официальный Dev.java Type Erasure L41–72 как объясняющий источник, а не норма. Независимый дополнительный специализированный интервью-банк в том аудите не подтверждён; не выводим частоту вопросов и не принимаем ошибочные универсальные запреты instanceof или обещания отсутствия CCE.

**Воспроизводимый пример Java 21:** [README, исходник и полный stdout CORE-GENERICS + CORE-ERASURE](../../examples/core/generics-erasure/README.md). Одна программа показывает запись `Integer` через сырую ссылку и `ClassCastException` при чтении как `String`, `ArrayStoreException` при ковариантном массиве, стёртые типы результата через рефлексию, допустимую в Java 21 проверку `instanceof` и переопределение через обобщённый базовый тип. **18 строк — наблюдения одной программы**, а не 18 независимых тестов. Пример не требует точного числа посреднических методов конкретного `javac` и не включает опасный `@SafeVarargs` в успешный сценарий. Отрицательные N01–N22 и четыре программы W01–W04 с предупреждениями остаются отдельными пробами G1; общий скрипт также не запускает 26 новых отдельных файлов D-A11b. Выше описаны первоначальные проверки теоретической части, историческое ограничение проверки `copyFirst` и последующая независимая проверка исправленного N21.

**Фрагменты в статье** показывают разные случаи и не составляют один общий Java-файл. Связанная статья — [CORE-GENERICS](generics.md).
