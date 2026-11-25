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

Полное правило [JLS §4.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.6) рекурсивно: стираем аргументы параметризованного типа, для вложенного `T.C` стираем внешний тип, для `T[]` — тип компонента, для переменной типа — **стирание** её крайней левой границы. Остальные типы сохраняются. Например, `T extends List<String>` стирается в `List`, а `T[]` — в `List[]`; `Outer<String>.Inner<Integer>[]` — в массив `Outer.Inner`. Если левая граница — интерфейс `Runnable`, стирание даёт именно его, не Object. Ограничения на несколько границ и `Comparable<? super T>` уже разобраны в [CORE-GENERICS](generics.md).

Не смешивайте три понятия: сигнатура Java-метода (имя, параметры типа, типы формальных параметров с учётом правил адаптации, **не возвращаемый тип**), её стирание (имя и стёртые типы формальных параметров, без параметров типа), и дескриптор метода в classfile (типы параметров **и результата**). Возвращаемый тип тоже подвергается стиранию, но не становится частью Java-сигнатуры. Поэтому изменить только возвращаемый тип недостаточно для перегрузки. [JLS §8.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.2), [JVMS §4.3.3](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html#jvms-4.3.3), [CORE-INHERITANCE](inheritance.md).

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

У обычного клиента компилятор проверяет совместимость generic-выражений; если он знает, что вызов `get()` должен иметь тип `String`, для реального использования результата могут потребоваться проверки или приведения. Некорректный raw/unchecked-код способен нарушить обещание параметризованного типа.

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

Сырой тип (raw type) и непроверяемое преобразование (unchecked conversion) — разные понятия. По [JLS §4.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.8) при вызове метода/конструктора через сырой тип предупреждение unchecked о таком вызове возникает, если стирание меняет хотя бы один тип формального параметра. Одно лишь изменение типа результата или throws недостаточно. Аналогично запись поля предупреждает при изменении его типа стиранием, чтение — нет. Собственные нестатические члены сырого класса имеют стёртые типы, унаследованные определяются через соответствующий супертип (например, у необобщённого суперкласса сигнатура `String label()` сохраняется), статические члены сохраняют объявленные типы. Само объявление сырой ссылки может вызвать отдельное предупреждение `rawtypes` у javac; присваивание сырой ссылки параметризованной — другой случай unchecked conversion (§5.1.9).

Сравним чтение через сырую ссылку, через типизированную ссылку с результатом Object, игнорирование результата и присваивание String. В следующем примере предупреждения намеренны — компилируйте без `-Werror`.

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

Фактический вывод по строкам: `67:67:ignored ok`, `typed=CCE`, `1`, `checked=CCE at add`, `static:legacy:null`. Три предупреждения rawtypes относятся к объявлениям сырых ссылок, два unchecked — к вызовам add, одно unchecked — к присваиванию raw в List<String>. Чтение Object и игнорируемый get не требуют здесь проверки String. В `javap -classpath out -c -p RawReads` вызовы get на смещениях 24, 32 и 41 не сопровождаются приведением к String (на 46 — pop); после get на 69 идёт checkcast String на 74. Это результат конкретного компилятора.

[Collections.checkedList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#checkedList(java.util.List,java.lang.Class)) возвращает представление с динамической проверкой вставляемых элементов: неверный тип отклоняется сразу. Гарантия отсутствия неверно типизированных элементов требует, чтобы исходный список уже был корректен и дальнейший доступ осуществлялся через представление; наличие другого alias на исходный список может обойти проверку.

### Непроверяемое приведение не обязательно пропускает проверку класса

По [JLS §5.1.6.2–3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.6.2) для допустимого unchecked сужающего ссылочного преобразования из S в не-пересечённый тип T оно полностью непроверяемое (completely unchecked), если стирание S является подтипом стирания T (включая равенство типов); иначе частично непроверяемое (partially unchecked). Во втором случае требуется проверка при выполнении, в первом — не требуется именно такая проверка при приведении. Для пересечений правило уточняется по составляющим; здесь рассматриваем обычные ссылочные типы. `Object → List<String>` частично непроверяемое: проверяется List, а не String внутри. `List<?> → List<String>` полностью непроверяемое: стёртые типы совпадают, но последующее использование элемента по-прежнему может потребовать проверки. Это не то же самое, что присваивание сырой ссылки параметризованной по §5.1.9. См. [CORE-CONVERSIONS](conversions.md).

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

Фактический вывод: `partial=CCE at cast`, `complete=cast ok:1`, `complete=CCE at read`. Компиляция без Werror дала два unchecked cast предупреждения. В javap вызов для частичного случая содержит checkcast List на 7, для полного нет такой проверки списка; при типизированном чтении на 59 следует checkcast String на 64. Таким образом, два предупреждения одной категории не означают одинакового поведения при выполнении.

## Reifiable / non-reifiable: что runtime может проверить (GE-11, GE-12, GE-18)

**Reifiable** — тип, информация о котором полностью доступна при выполнении в смысле [JLS §4.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.7). Исчерпывающий перечень: необобщённый класс или интерфейс; параметризация, у которой все аргументы — неограниченные wildcard; сырой тип; примитив; массив с reifiable типом элемента; вложенный тип, где каждый компонент типа, разделённый точкой, reifiable. Например, `Outer<?>.Inner<?>` удовлетворяет условию, `Outer<?>.Inner<String>` — нет. Тип-пересечение не является reifiable. `List<String>` и `List<? extends Number>` тоже non-reifiable. Это отдельное свойство типа, не универсальное разрешение всех синтаксических конструкций.

Однако неверно говорить «в Java невозможно `instanceof` с любым non-reifiable типом». С Java 16 ключевое правило [JLS §15.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2) связано с **checked cast compatibility** ([§5.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.5)). Сравним:

~~~java
List<String> typed = new ArrayList<>();
boolean first = typed instanceof List<?>;             // корректно
boolean second = typed instanceof ArrayList<String>; // корректно в Java 21

Object object = new ArrayList<String>();
boolean third = object instanceof List<?>;           // корректно
// boolean fourth = object instanceof List<String>;  // ошибка компиляции
~~~

Во втором случае статический тип выражения уже ограничивает допустимую параметризацию при проверяемом приведении к `ArrayList<String>`. JVM при этом **не проверяет отдельные runtime type arguments** внутри списка: для проверки принадлежности достаточно класса объекта. В четвёртом случае статический `Object` не даёт такой гарантии; приведение к `List<String>` было бы unchecked, поэтому `instanceof` запрещён. Формула «справа всегда должен стоять reifiable тип» является **устаревшим обобщением** для Java 21.

Проверяемая совместимость приведения опирается на статические предположения, которые небезопасный код способен нарушить. `instanceof` возвращает false для null. Следующий пример специально загрязняет список через сырой alias; компилируйте без Werror.

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

Фактически `true`, `false`, `CCE at read`. Компилятор дал rawtypes и unchecked call предупреждения, но не отверг проверяемое instanceof. JVM проверила принадлежность объекта классу ArrayList, а не элементы: присваивание результата String вызвало CCE. Не используйте положительный результат instanceof для доказательства отсутствия загрязнения.

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

В случае `new T()` есть также принципиальная проблема знания конкретного конструктора, а не только erasure. При массивах компонентный тип должен быть совместим с runtime проверками хранения. `List<?>[]` допустим по правилам reifiability, но не превращает произвольный массив в безопасный `List<String>[]`. [JLS §10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html), [§15.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9).

Литерал класса (class literal) подчиняется отдельной грамматике [JLS §15.8.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.8.2): `List.class` и `int.class` допустимы, `List<?>.class` и `List<String>.class` — нет, хотя List<?> reifiable. Переменная типа T также не разрешена в T.class. У `Object o` проверка `o instanceof List<? extends Number>` запрещена по checked cast compatibility, а `o instanceof List<?>` допустима. Это не отменяет правила Java 16+ для статически совместимых параметризаций.

Объявление `T[] items` допустимо; запрещено непосредственно создавать `new T[2]`. Передайте уже существующий массив либо фабрику: Supplier<T> создаёт значение (в том числе вызовом известного конструктора), IntFunction<T[]> — массив с запрошенной длиной. Фабрика определяет фактический класс результата; обещание размера и допустимых значений зависит от её контракта. Токен класса `Class<T>` также может служить входом для [Array.newInstance](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Array.html#newInstance(java.lang.Class,int)), но результат API имеет тип Object; токен List.class не содержит аргумента String и не делает произвольное приведение к T[] безопасным. Не используйте непроверяемое приведение массива как универсальный рецепт. См. [CORE-ARRAYS](arrays.md) и [CORE-FUNCTIONS](functions.md).

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

Фактический вывод: `java.lang.String[]:2:StringBuilder`, `java.util.List:67`, `ASE at store`, `true`, `java.util.List:int`. В первой строке переданный String[] сохранил класс, фабрика создала массив длины 2 и объект StringBuilder. Во второй фактический компонент массива List<?>[] — List; запись через Object[] строки отклонена проверкой компонента (ArrayStoreException), а не проверкой аргумента типа элементов списка. Вложенный тип с двумя неограниченными wildcard прошёл instanceof. Последняя строка показывает допустимые литералы классов. [JLS §10.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html#jls-10.5), [§15.10.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.10.1). Отдельные отрицательные файлы ниже.

## Heap pollution: когда статическое обещание нарушено (GE-13, GE-14)

**Heap pollution** возникает, когда переменная параметризованного типа ссылается на объект, тип которого несовместим с её статическим обещанием. Она может возникнуть через raw/unchecked conversion и некоторые varargs-механизмы. Это не просто «ошибка в куче»: термин описывает нарушение типовой согласованности, а не физическую порчу памяти JVM. [JLS §4.12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.2).

Рассмотрим обобщённый метод с переменным числом аргументов (generic varargs). Формальный параметр `List<String>... lists` имеет тип массива. При вызове с отдельными аргументами компилятор может создать массив со стёртым компонентным типом; при вызове с уже подходящим массивом в режиме фиксированной арности передаётся эта ссылка, новый массив не обязателен. Через другую ссылку (alias) массива можно записать несовместимый список, если фактический компонент массива допускает такую запись. Если компонентный класс более узок, возможен ArrayStoreException уже при записи. [JLS §8.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.1), [§15.12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.2), [§15.12.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.4.2), [CORE-CONVERSIONS](conversions.md).

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

`alias[0] = List.of(67)` помещает в массив значение, которое runtime-массив может принять как `List`. Но метод затем пытается извлечь `String` из объекта `List<Integer>`, потому что исходная параметризация обещала другой тип. В таком методе `@SafeVarargs` **не обеспечивает безопасность**: это обещание автора о корректности тела относительно потенциально небезопасных операций с varargs, а не runtime-защита и не исправление действий с alias.

Собранная программа может выдавать предупреждения даже при аннотации, если запустить дополнительные категории lint: `javac --release 21 -Xlint:unchecked -Xlint:varargs ...`. Нельзя фиксировать точный текст предупреждения как норму JLS; важны условия небезопасной операции. [JLS §9.6.4.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4.7), [API @SafeVarargs](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/SafeVarargs.html).

### Где допустима аннотация @SafeVarargs (GE-24)

В Java 21 она применима к static-методам, final instance-методам, конструкторам и private instance-методам с varargs. Компилятор проверяет допустимое место аннотации, **не доказывает** произвольную безопасность тела. Аннотировать переопределяемый публичный instance-метод нельзя. Не использовать `@SafeVarargs` просто ради подавления warning: сначала проверить, существует ли реальное загрязнение массива или небезопасная публикация его alias.

Аннотация разрешена **только для объявления с переменным числом аргументов**. Метод с параметром String[] без многоточия аннотировать нельзя, даже если он static. По [JLS §9.6.4.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4.7) у SafeVarargs нелокальный эффект: она подавляет unchecked предупреждение о non-reifiable типе varargs в объявлении и предупреждение о создании непроверяемого массива в местах вызова. `@SuppressWarnings("unchecked")` на объявлении метода имеет локальный эффект и не даёт такого обещания внешним вызывающим сторонам. Это не подавление всех диагностик и не доказательство безопасности тела.

Сначала пример без аннотации (без Werror):

Сохраните `VarargsWarnings.java`:

~~~java
import java.util.*;
class VarargsWarnings {
    static int count(List<String>... lists) { return lists.length; }
    public static void main(String[] args) { System.out.println(count(List.of("a"))); }
}
~~~

Вывод 1; фактически два unchecked предупреждения — `unchecked.varargs.non.reifiable.type` у объявления и `unchecked.generic.array.creation` у вызова.

Теперь аннотация на намеренно опасном теле (без Werror):

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

Вывод CCE. Два предыдущих вида предупреждений подавлены аннотацией в этом примере, но `-Xlint:varargs` выдал `varargs.unsafe.use.varargs.param` при присваивании alias. Компилятор не обязан диагностировать всякое нарушение; отсутствие предупреждений не делает код безопасным.

В безопасном примере метод читает значения из varargs-массива и передаёт их в коллекцию, не меняя и не публикуя сам массив. Реальный API [Collections.addAll](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#addAll(java.util.Collection,T...)) использует `@SafeVarargs static <T> boolean addAll(Collection<? super T> c, T... elements)`; он добавляет элементы в коллекцию, а не обещает успешную вставку при любом её контракте. Это также пример PECS из [CORE-GENERICS](generics.md).

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

Вывод `2:1:[67, 68, 69, 70, 71]:[67, 68]`, затем `1:1`. Переданный Integer[] в первом вызове используется как массив в режиме фиксированной арности, второй вызов с 69 использует переменную арность. Содержимое supplied сохранилось; коллекция пополнилась. Также скомпилированы и вызваны допустимые private/final методы и конструктор. Строгая компиляция без предупреждений. Запрещённые места аннотации — BadFixedAnnotation и BadOverrideAnnotation ниже.

## Warning не равен ошибке компиляции (GE-23)

| Сценарий | Ожидаемая категория | Что это значит |
|---|---|---|
| `List raw = typed; raw.add(67);` | unchecked call warning для небезопасного вызова | `javac` допускает вызов, но типовая гарантия ослаблена |
| `List<String> list = raw;` | unchecked conversion warning | исходный raw не доказывает соответствие `String` |
| `(List<String>) object` при подходящем статическом типе выражения | возможно unchecked cast warning | приведение может быть разрешено, но аргумент типа не подтверждён runtime |
| `new List<String>[2]` | ошибка компиляции | массив с non-reifiable компонентным типом нельзя создать |
| Два `m(List<String>)` / `m(List<Integer>)` | ошибка компиляции | конфликт после стирания сигнатур |

Для изолированного опыта используйте `javac --release 21 -XDrawDiagnostics -Xlint:all File.java` (в частности, категории rawtypes, unchecked, varargs). Для намеренно предупреждающих программ не включайте Werror, иначе предупреждение станет причиной неуспешной сборки. Для корректных и отдельно отрицательных примеров используйте также -Werror. Предупреждение само по себе не является ошибкой языка. Новые RawReads, CastCases, VarargsWarnings и VarargsUnsafe воспроизводят механизмы четырёх отдельных проверок W01–W04 принятого плана, а не заменяют всю историческую серию. Точные позиции и ключи диагностик — в протоколе ниже.

## Почему overload только по generic-аргументу невозможен (GE-15)

~~~java
class Overloads {
    // void use(List<String> items) { }
    // void use(List<Integer> items) { }
}
~~~

Если раскомментировать оба метода, `javac` отвергнет объявления: оба имеют одинаковую **erasure-сигнатуру** `use(List)`. Это не просто правило «JVM игнорирует generics», а нормативное ограничение совместимости сигнатур и name clash. Перегрузка, различающаяся другими параметрами или действительно различными erased типами, может оставаться допустимой; выводить запрет на *любые* generic-overloads было бы неверно. [JLS §8.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.2), [§4.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.6).

## Переопределение после стирания: откуда bridge (GE-16)

При работе с generic-базовым классом контракт переопределения проверяется на уровне Java-исходника, но после стирания типы параметров и возвращаемых значений могут отличаться. Компилятор вправе создавать **bridge method** для сохранения полиморфизма.

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

В Java-коде `Child.get()` корректно переопределяет generic-контракт родителя. В объектном представлении стирание `Parent<T>.get()` даёт возвращаемый `Object`, тогда как `Child.get()` возвращает `String`. Для совместимости вызова через базовый тип компилятор может создать дополнительную посредническую реализацию. Стандарт языка требует соблюдения правил переопределения, но **не устанавливает универсального фиксированного числа synthetic/bridge методов** в classfile всех компиляторов.

Наличие bridge у конкретного `javac` можно наблюдать через reflection:

~~~java
for (var method : Child.class.getDeclaredMethods()) {
    if (method.isBridge()) {
        System.out.println("bridge: " + method.getName());
    }
}
~~~

Этот фрагмент зависит от представления конкретного скомпилированного класса; корректнее говорить «`javac 21` в данной проверке сгенерировал bridge», если проверка действительно выполнена, а не обещать одинаковый результат любой JVM. Обобщённые сигнатуры/вывод типов и правила overriding: [JLS §8.4.8.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.3), [Method.isBridge API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Method.html#isBridge()).

Есть и другой вид посреднического метода: у `Parent<T>.put(T)` после стирания параметр Object, а у `Child extends Parent<String>` переопределяющий метод принимает String. Вызов через Parent<String> выбирает специализированную реализацию. Сырой Parent позволяет передать Integer с предупреждением; на данном javac исключение возникает внутри посреднического метода **до** входа в put(String).

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

Фактический вывод: `child=ok`, `bridge=CCE:1`, `Object:true`. Число вызовов тела осталось 1, отражённый посреднический метод имеет параметр Object и признак synthetic. При компиляции без Werror получены два предупреждения: rawtypes для Parent и unchecked call для put(T). Фактический фрагмент `javap -classpath out -c -p -v 'BridgeCase$Child'`:

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

Посредник имеет дескриптор с Object, проверяет String, затем вызывает реализацию с дескриптором (Ljava/lang/String;)V. Флаги описаны в [JVMS §4.6](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html#jvms-4.6). Номера констант и смещения — наблюдение этого javac, не универсальное требование к числу или расположению методов. В исходнике Child.put(String) — переопределение, а не перегрузка, возвращаемый тип не входит в Java-сигнатуру. Напротив, если в наследнике P<String> написать f(Object) при объявлении P<T>.f(T), это не переопределяет f(String), а стёртые исходные объявления конфликтуют. Отдельный BadInheritedClash отклонён с name.clash.same.erasure.no.override; BadClash — конфликт двух перегрузок в одном классе; BadOverrideReturn — несовместимый результат переопределения. [JLS §8.4.8.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.3), [CORE-INHERITANCE](inheritance.md).

## Generic-метаданные через Reflection: что можно увидеть (GE-22, GE-29)

Erasure не стирает всякую информацию **об объявлениях**: reflection может возвращать generic return type и parameterized type **декларации метода**. Например, `Method.getGenericReturnType()` может сообщить, что возвращаемый тип метода `List<String>`. Но `new ArrayList<String>().getClass()` сообщает runtime-класс объекта и не восстанавливает «какой `String` аргумент был выбран при создании» исключительно из экземпляра.

~~~java
class Api {
    public List<String> names() { return List.of("a"); }
}

var method = Api.class.getMethod("names");
System.out.println(method.getGenericReturnType()); // java.util.List<java.lang.String> для javac21
~~~

Это не типовая проверка произвольных объектов в списке. Когда рефлексия вызвана к API, которого нет у конкретного Java-класса, или когда метаданные недоступны, соответствующие выводы требуют отдельного анализа. Для дальнейшего углубления — будущая CORE-REFLECTION; описание `ParameterizedType` и байткодных атрибутов не является задачей этой статьи.

На одном объекте Method сравним [getReturnType](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Method.html#getReturnType()) и [getGenericReturnType](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Method.html#getGenericReturnType()). Первое возвращает Class, второе — Type с объявленной информацией. Анонимный подкласс `new ArrayList<String>() {}` объявляет конкретный параметризованный суперкласс, который может быть отражён через [getGenericSuperclass](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html#getGenericSuperclass()). Это метаданные объявления нового подкласса, не механизм узнавания произвольного аргумента типа у обычного объекта. Поиск метода может выбросить проверяемое NoSuchMethodException; в примере main объявляет ReflectiveOperationException.

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

Фактически три строки: `java.util.List:java.util.List<java.lang.String>`, `java.util.ArrayList`, `java.util.ArrayList<java.lang.String>`. Первая строка относится к одному объявлению names, третья — к объявленному суперклассу анонимного класса. Ни одна не проверяет все элементы списка и не доказывает отсутствие загрязнения. Полный разбор Signature и ParameterizedType остаётся за рамками.

## Другие ограничения параметризованных типов (GE-20)

~~~java
// class GenericProblem<T> extends Exception { } // ошибка компиляции
~~~

Обобщённый класс нельзя делать прямым или косвенным подклассом `Throwable` ([JLS §8.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.2)). Это связано с требованиями к исключениям при выполнении и отсутствием достаточной runtime-параметризации. Но обычный класс исключения и generic-метод, объявляющий тип исключения в throws-контракте с допустимыми ограничениями, — **не одно и то же**; не следует из запрета `GenericProblem<T>` выводить запрет на любые generic-связанные throws-объявления.

Переменную типа, ограниченную Throwable или его подклассом, разрешено использовать в throws ([JLS §8.4.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.6)). Но [§14.20](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20) запрещает переменную типа в объявлении параметра catch. Это отдельное правило, не запрет объявления обобщённого метода. Вызов с IOException проверяется компилятором как вызов, способный выбросить IOException; можно обработать именно его. См. [CORE-EXCEPTIONS](exceptions.md).

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

Фактический вывод `IOException:67`, строгая компиляция без предупреждений. BadCatchT и BadGenericThrowable ниже отвергнуты по разным основаниям.

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

Ключи generic.throwable и type.found.req/type.parameter/type.req.class. Дополнительно BadGenericThrowable дал предупреждение missing.SVUID; причина отказа — именно запрет обобщённого подкласса Throwable. Все 15 независимых файлов отклонены с кодом 1: 17 диагностик ошибок (две у BadWildcardLiteral, две у BadOverrideReturn и 13 по одной) и одно дополнительное предупреждение. Число диагностик не равно числу файлов.

## Вопросы для самопроверки

| Вопрос | Ответ |
|---|---|
| Во что стирается T[] при T extends List<String>? | List[]; применяется стирание крайней левой границы и компонента массива. |
| Входит ли результат в сигнатуру Java-метода? | Нет; дескриптор JVM содержит и параметры, и результат. |
| Почему Object read может пройти, а String read — нет? | Использование результата как String требует проверки типа; в Object-контексте она здесь не нужна. |
| Где может упасть unchecked cast? | Частично непроверяемый cast проверяет стёртый класс, полностью непроверяемый может пройти и упасть при типизированном чтении. |
| List<?> reifiable; допустим ли List<?>.class? | Нет: отдельная грамматика class literal. |
| Доказывает ли true у typed instanceof ArrayList<String> тип всех элементов? | Нет, загрязнение может нарушить статическое предположение. |
| Чем T[] отличается от new T[1]? | Объявление допустимо, непосредственное создание запрещено; можно передать массив или фабрику. |
| Что именно подавляет SafeVarargs? | Конкретные unchecked предупреждения объявления и вызова, не доказывает безопасность. |
| Почему raw.put(67) не входит в тело put(String)? | В проверенном javac посредник put(Object) сначала проверяет String. |
| Можно ли throws T и catch(T e)? | Первое разрешено при подходящей границе, второе запрещено. |

## Фактические проверки D-A11b

Роль 02 выполнила 26 отдельных компиляций временных файлов на Debian OpenJDK/javac 21.0.12.1, LC_ALL=C. Команды: `javac --release 21 -XDrawDiagnostics -Xlint:all -Werror -d out Имя.java`; для шести намеренно предупреждающих файлов RawReads, CastCases, PollutedCheck, VarargsWarnings, VarargsUnsafe, BridgeCase Werror опущен. При успешной компиляции — `java -cp out Имя`. Пять строгих компиляций exit0 без диагностик, шесть warning-only exit0 (15 предупреждений), 15 отдельных отказов exit1 (17 ошибок и одно дополнительное serial warning). stdout компилятора пуст; все 11 запусков JVM exit0 и stderr пуст, исключения перехвачены намеренно. Четыре отдельные команды javap для ErasedTypes, RawReads, CastCases и BridgeCase$Child exit0 без stderr; конкретные дескрипторы и инструкции приведены выше.

Тела новых исходников находятся в этой статье. Их Git-хеши, команды, позиции/ключи диагностик, вывод и результаты javap — в [адресном фактическом протоколе 02 #7172268517](https://github.com/Melchiorius/backend-interview-labs/issues/308#issuecomment-7172268517). [Issue #308](https://github.com/Melchiorius/backend-interview-labs/issues/308) задаёт объём задачи. Десять исходных контекстных блоков сохранены, не все являются самостоятельными файлами. Не повторялась полная серия G1, не проверялись все компиляторы, бинарная совместимость, JIT, производительность, полная модель classfile или GUI.

Для теоретической части роли 02 три исполняемых входа общего примера (src/GenericsErasureDemo.java, expected.stdout, verify.sh) реконструированы из BASE `443713301242a12d6b2464c4884b537e87110abc` в двух временных деревьях; README прочитан по точному BASE, но в локальные деревья не копировался. Это не полный клон. Git-хеши исполняемых входов совпали с закреплёнными; в обоих деревьях bash -n и bash verify.sh завершились с кодом 0, stderr пуст; эталон совпал с фактическим stdout, полные 19 строк (18 строк программы и маркер скрипта) совпали побайтно. Это не 19 тестов и не проверка всех временных файлов. N21 относится к отдельному исправлению общего примера: в исходной ревизии copyFirst после Collections.copy на тех же данных не выявляет независимый no-op мутант; роль 02 не меняла runner и не запускала мутант. Его проверка — отдельная ответственность 03 после разрешения координатора; итоговая интеграционная приёмка и G4 обязательны.

## Соответствие исследовательскому G1 и пределы статьи

| ID | Раскрытая тема |
|---|---|
| GE-02 (совместно с CORE-GENERICS), GE-10 | Erasure: первая граница, generic signature, runtime object |
| GE-11 | Java 21 `instanceof`: checked cast compatibility, отличие `List<?>` от `List<String>` |
| GE-12 | `new T()`, `T.class`, generic arrays и reifiability |
| GE-13 | Raw/unchecked, runtime cast и точка `ClassCastException` |
| GE-14 | Heap pollution и `@SafeVarargs` |
| GE-15 | Same-erasure name clash |
| GE-16 | Overriding и bridge |
| GE-18, GE-20, GE-22, GE-23, GE-24 (P1) | Массивы `List<?>`, запрет generic Throwable, reflection, lint, допустимость `@SafeVarargs` |
| GE-27, GE-28, GE-29 (P2) | Bytecode/Signature и `ParameterizedType`, framework DAO/proxy — очерчены границы без заявления полного разбора |

**Не входит в заявленный объём:** полный разбор Java bytecode descriptors/Signature attributes (выше только необходимые дескрипторы и конкретные инструкции), бинарная совместимость подробно, JIT/HotSpot memory layout и benchmark, фреймворковые прокси, generic DAO/JPA, достоверная информация о фактическом параметре типа произвольного объекта по его identity. Это темы GE-27–29, не закрытые данным разделом.

**Источники:** [JLS 21 §4.6–4.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html), [§5.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.5), [§8.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4), [§9.6.4.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4.7), [§15.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2), [API @SafeVarargs](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/SafeVarargs.html), [Method.isBridge](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Method.html#isBridge()). В [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md) нормативные источники отделены от интервью-подборок.

Исторические ориентиры вопросов (не нормативные доказательства и не новый G1): [план CORE-GENERICS-ERASURE §3](../../docs/plans/CORE-GENERICS-ERASURE.md) Q-HRUDAY глава 11 Generics, Q-IN28 Q178–184, Q-CHERNYSHOV §20 вопросы 11–25, Q-TEAMLEAD PECS/static generic method; [первичный аудит A11.2 §2](../../docs/audits/java-core-full-2026-11-24/A11.2-CORE-ERASURE.md) DigitalOcean Java Generics Explained L698–706/747–756, FAQ L918–937/966–1014, официальный Dev.java Type Erasure L41–72 как объясняющий источник, а не норма. Независимый дополнительный специализированный интервью-банк в том аудите не подтверждён; не выводим частоту вопросов и не принимаем ошибочные универсальные запреты instanceof или обещания отсутствия CCE.

**Воспроизводимый пример Java 21:** [README, исходник и полный stdout CORE-GENERICS + CORE-ERASURE](../../examples/core/generics-erasure/README.md). Один runnable иллюстрирует raw alias → запись `Integer` → `ClassCastException` при чтении `String`, `ArrayStoreException` при ковариантном массиве, erased return types через Reflection, Java 21 checked-cast-compatible `instanceof` и вызов override через generic parent. **18 строк — наблюдения одной программы**, а не 18 независимых тестов. Он намеренно не требует точного числа bridge-методов конкретного `javac` и не демонстрирует опасный `@SafeVarargs` в success-сценарии. Отдельные отрицательные N01–N22 и lint W01–W04 остаются независимыми пробами G1, а не частью runnable. Этот общий пример не запускает 26 новых отдельных файлов D-A11b; результаты теоретических проб и ограничение проверки исходного copyFirst указаны выше.

**Фрагменты в статье** показывают разные случаи и не составляют один общий Java-файл. Связанная статья — [CORE-GENERICS](generics.md).
