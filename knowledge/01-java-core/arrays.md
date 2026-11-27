# CORE-ARRAYS — массивы Java 21: типы, ковариантность, копии и представления

**Область:** Java SE 21, без preview. **Сначала:** [типы и значения](types.md), [преобразования](conversions.md), [передача аргументов](parameters.md). Затем: [generics](generics.md), [стирание типов](erasure.md), [равенство](equality.md), [Stream API](streams.md), [строки и Builder](strings.md). Эта статья объясняет не только синтаксис массива, но и **почему проверка типов иногда происходит при компиляции, а иногда — при записи элемента**, как устроено разделение ссылок и какие контракты есть у копирования.

**Основа:** [принятый исследовательский G1-план](../../docs/plans/CORE-ARRAYS-STRINGS.md), матрица **AR-01–AR-22**, в том числе 16 P0 / 4 P1 / 2 P2. Основные нормы — [JLS 21 §10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html), [JLS §15.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.10), [Arrays Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html). **Ожидаемые результаты и комментарии к выводу в учебных фрагментах описывают показанные входы, а не свидетельствуют о компиляции каждого фрагмента этой статьи.** Для самостоятельного воспроизведения используйте [общий runnable Arrays + Strings/Builders](../../examples/core/arrays-strings/README.md): `ArraysG1.java` выводит A01–A24, `StringsG1.java` — S01–S28. Это **52 наблюдения двух запусков**, не 52 независимых теста.

## 1. Массив — объект фиксированной длины с конкретным типом компонента (AR-01, AR-02, AR-03)

Массив объединяет элементы **одного компонентного типа**, к которым обращаются по индексам от нуля до `length - 1`. Длину выбирают при создании; менять её нельзя. Массив не является списком с `add`. Каждый массив, включая `int[]`, — объект: его ссылка совместима с `Object`, `Cloneable` и `java.io.Serializable`.

**Компонентный тип един для данного массива, но фактические классы объектов могут различаться:** например, `Number[]` может содержать `Integer`, `Double` и `null`.

- `int[]` хранит значения примитивного `int`; `Integer[]` хранит ссылки на `Integer`. Autoboxing не превращает весь `int[]` в `Integer[]`.
- `String[]` — массив ссылок на `String`, то есть одновременно подтип `Object[]`. `int[]` **не** подтип `Object[]` и не подтип `Integer[]`; его элементы не являются объектами.
- У ссылки `Object array` можно вызвать `getClass()`: во время выполнения компонентный тип массива остаётся известным. Это существенно для проверки записи элемента (§2).

Различайте непосредственный тип компонента и конечный тип элемента: у `int[][]` это соответственно `int[]` и `int`. Хотя `int[]` не является `Object[]`, сам `int[]` — ссылочный тип, совместимый с `Object`, `Cloneable` и `java.io.Serializable`. Поэтому `int[][]` совместим с `Object[]`, `Cloneable[]` и `java.io.Serializable[]`, но не с `Object[][]` ([JLS §10.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html#jls-10.1), [§4.10.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.10.3)).

~~~java
import java.util.Arrays;

class ArrayBasics {
    public static void main(String[] args) {
        int[] numbers = new int[3];
        String[] texts = new String[2];
        Object asObject = numbers;      // любой массив — Object
        System.out.println(numbers.length);      // 3
        System.out.println(Arrays.toString(numbers)); // [0, 0, 0]
        System.out.println(Arrays.toString(texts));   // [null, null]
        System.out.println(asObject instanceof Object[]); // false
        System.out.println(texts instanceof Object[]);    // true
        System.out.println(numbers.getClass().getComponentType()); // int
    }
}
~~~

У экземпляра массива `length` — **поле** типа `int`, а не метод `length()` и не `size()`. Поле `public final`, но **элементы изменяемы**: `numbers[0] = 67` допустимо. Ссылка `final int[] a` запрещает переназначить `a`, но не запрещает `a[0] = 67`. Обращение `numbers[3]` — ошибка *во время выполнения*, а `numbers.length()` — ошибка *компиляции*.

При создании `new int[3]` все элементы получают `0`; у `boolean[]` — `false`, у `char[]` — `'\0'` (нулевая кодовая единица), у массивов ссылок — `null`. Это **инициализация компонента массива**, не вызов произвольных конструкторов элементов. У `new String[3]` не создаются три объекта String. Возможен и массив нулевой длины `new int[0]`.

**Как применять:** если нужен набор фиксированного размера без дополнительной обёртки — массив естественен. Когда нужна изменяемая длина/операции списка, используйте коллекцию, но не приписывайте ей правила массивов. Отдельный синтаксис/boxing — в [CORE-TYPES](types.md) и [CORE-CONVERSIONS](conversions.md).

Сравнение массива примитивов, массива ссылок и коллекций с упаковкой примитивов, включая локальность доступа и ограничения оценок памяти, приведено в [COL-TRADEOFFS](../02-collections/tradeoffs.md).

## 2. Ковариантность ссылочных массивов и runtime ArrayStoreException (AR-04, AR-05)

В Java допустимо присваивание `String[]` переменной типа `Object[]`. Это **ковариантность массивов ссылочных типов**. Однако реальный массив остаётся `String[]`; JVM не позволяет записать в него `Integer` только потому, что видимая ссылка имеет тип `Object[]`.

~~~java
class ArrayStore {
    public static void main(String[] args) {
        String[] real = {"abc"};
        Object[] alias = real;        // компилируется
        alias[0] = "xyz";             // можно: реальный компонент String
        try {
            alias[0] = 67;            // компилируется по типу Object[]
        } catch (ArrayStoreException e) {
            System.out.println("ArrayStoreException");
        }
        System.out.println(real[0]); // xyz, исходный элемент не заменён Integer
    }
}
~~~

**Механизм:** компилятор проверяет присваивание `Object` в компонент `Object[]` по *статическому* типу выражения. JVM при store проверяет *фактический* компонентный класс массива; несовместимый объект вызывает `ArrayStoreException`, а не `ClassCastException`. Исключение возникает при попытке записи, а не при самом присваивании `String[] → Object[]`. Если код выполняет явное неверное приведение массива, тогда может быть другой механизм — `ClassCastException`.

У generics другая стратегия: `List<String>` **не** подтип `List<Object>`. Компилятор отклоняет:

~~~java
import java.util.List;
class Invariance {
    // List<Object> a = List.<String>of("x");  // compile error
    // Object[] b = new int[1];                 // compile error
    // Integer[] c = new int[1];                // compile error
}
~~~

Именно здесь на собеседовании полезно разделить два разных вопроса: «Разрешено ли присвоить ссылку?» и «Разрешено ли потом записать конкретный элемент?» Массивы и обобщённые коллекции отвечают на них по-разному; детали wildcard/PECS — в [CORE-GENERICS](generics.md).

Для многомерного массива проверяется совместимость **объекта строки**, а не каждого её элемента. Отдельный фрагмент внутри метода:

~~~java
int[][] matrix = new int[2][1];
Object[] rows = matrix;
Cloneable[] clones = matrix;
java.io.Serializable[] serializable = matrix;
rows[0] = new int[]{67}; // допустимо
rows[1] = null;          // допустимо для ссылочного компонента
// rows[0] = new long[1]; // компилируется, при исполнении ArrayStoreException
// Object[][] wrong = matrix; // ошибка компиляции
~~~

## 3. Индексы, многомерность и массивы массивов (AR-02, AR-06, AR-16, AR-17)

Запись `int[][]` означает **массив, элемент которого — ссылка на `int[]`**, а не обязательно сплошную прямоугольную область памяти. Внешний массив имеет свою `length`, каждая строка, отличная от `null`, — свою (в том числе длину 0). Строки могут иметь разные длины (*jagged*) либо оставаться `null`.

~~~java
import java.util.Arrays;

class Jagged {
    public static void main(String[] args) {
        int[][] rows = new int[3][]; // пока [null, null, null]
        rows[0] = new int[]{1, 2};
        rows[1] = new int[0];
        // rows[2] остаётся null
        System.out.println(rows.length);       // 3
        System.out.println(rows[0].length);    // 2
        System.out.println(rows[1].length);    // 0
        System.out.println(Arrays.deepToString(rows)); // [[1, 2], [], null]
    }
}
~~~

Не пишите `rows[2].length`, пока элемент может быть `null`: это `NullPointerException`. `rows[3]` даст `ArrayIndexOutOfBoundsException`; `new int[-1]` — `NegativeArraySizeException`; чтение `nullArray[0]` также приводит к NPE. **Ни один из этих случаев не является compile-negative** — код компилируется, исключение определяется значениями при выполнении.

**Важно различать синтаксис:** `new int[1][1]` создаёт двумерный массив размером 1×1; это **не** обращение по индексам. Для демонстрации ошибки границ сначала создайте массив, затем обращайтесь к элементу с неверным индексом. В следующем примере все три фрагмента **компилируются**, а исключения возникают при выполнении:

~~~java
class ArrayRuntimeFailures {
    public static void main(String[] args) {
        try {
            int[] items = new int[1];
            System.out.println(items[1]);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("wrong index");
        }

        try {
            int[] invalid = new int[-1];
            System.out.println(invalid.length);
        } catch (NegativeArraySizeException e) {
            System.out.println("negative length");
        }

        try {
            int[] absent = null;
            System.out.println(absent.length);
        } catch (NullPointerException e) {
            System.out.println("null array");
        }
    }
}
~~~

Ожидаемый вывод: `wrong index`, `negative length`, `null array` (по одной строке). **Именно этот самостоятельный фрагмент** проверен автором на OpenJDK/javac 21.0.12.1: `javac --release 21` и запуск `java ArrayRuntimeFailures` завершились успешно. Это не проверка всех остальных inline-фрагментов статьи и не часть CI.

**Правило:** размерность выражения массива, количество индексных операций и длина *каждой строки* — разные вещи. Лишние квадратные скобки после `new int[n]` не являются способом «проверить индекс».

### Тип индекса, размерности и порядок вычислений

Для индекса и каждого выражения длины применяется унарное числовое продвижение (*unary numeric promotion*); результат должен иметь тип `int`. Допустимы `byte`, `short`, `char`, `int` и соответствующие распаковываемые обёртки; `long` не подходит даже для константы `0L`. Распаковка ссылки `null` выбрасывает NPE. Инициализатор `{67}` допустим в объявлении `int[] a = {67};` или в выражении `new int[]{67}`; отдельно `a = {67};` недопустимо. Размеры задают перед пустыми скобками: `new int[2][]` допустимо, `new int[][1]` нет. `new int[2][0]` создаёт две отдельные строки длины 0, не две ссылки на один пустой массив.

При создании массива с выражениями длин они вычисляются слева направо **до** проверки отрицательных значений. Если вычисление внезапно завершается исключением, следующие выражения не вычисляются, проверка отрицательных размеров может не состояться. При чтении элемента сначала вычисляется ссылка на массив, затем индекс; только потом проверяются `null` и границы. При простом присваивании элементу сначала вычисляются ссылка и индекс, затем правая часть, затем выполняются проверки `null`/границ и (для ссылок) совместимости записи. При составном присваивании (`+=` и т.п.) проверки `null`/границ и чтение старого элемента происходят **до** вычисления правой части. Исключение при вычислении ссылки/индекса может прервать все последующие шаги.

Следующий самостоятельный класс показывает порядок на конечных входах, без больших выделений памяти:

~~~java
class EvaluationOrder {
    static int calls;
    static int tick(int n) { calls++; return n; }
    public static void main(String[] args) {
        try { int[][] a = new int[tick(-1)][tick(1)]; }
        catch (NegativeArraySizeException e) { System.out.println(calls); } // 2
        int[] absent = null;
        calls = 0;
        try { absent[tick(0)] = tick(67); }
        catch (NullPointerException e) { System.out.println(calls); } // 2
        calls = 0;
        try { absent[tick(0)] += tick(67); }
        catch (NullPointerException e) { System.out.println(calls); } // 1
    }
}
~~~

Нормы: [JLS §15.10.1–4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.10), [§15.26.1–2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.26), [CORE-CONVERSIONS](conversions.md).

## 4. clone, copyOf и arraycopy: три контракта, shallow и aliasing (AR-07, AR-08, AR-09, AR-20)

**Shallow copy** означает новый **внешний** объект массива, элементы которого копируются как значения. Для массива ссылок это **те же ссылки**, а не клоны объектов. Для `int[][]` внутренние `int[]` продолжают разделяться.

~~~java
import java.util.Arrays;

class Shallow {
    public static void main(String[] args) {
        int[][] original = {{1, 2}, {3}};
        int[][] clone = original.clone();
        int[][] extended = Arrays.copyOf(original, 3);
        System.out.println(clone == original);       // false
        System.out.println(clone[0] == original[0]);  // true
        original[0][0] = 67;
        System.out.println(clone[0][0]);             // 67
        System.out.println(extended[0][0]);          // 67
        System.out.println(extended[2]);             // null, новая внешняя позиция
    }
}
~~~

`clone()` сохраняет длину и компонентный тип, `Arrays.copyOf(array, newLength)` может **обрезать** элементы или **дополнить** (null для ссылок, нулевые значения для примитивов). Для двухмерной матрицы ни то ни другое не клонирует строки автоматически. Чтобы изолировать внутренние массивы, их нужно копировать отдельно, определив политику для null.

У массивов `clone()` — особый публичный метод, возвращающий статический тип массива и не объявляющий проверяемых исключений, в отличие от `protected Object.clone()` с `CloneNotSupportedException` ([JLS §10.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html#jls-10.7), [CORE-OBJECT](object.md)). Например, объявленный внутри класса метод `static <T> T[] same(T[] a) { return a.clone(); }` компилируется без cast; если передан `String[]`, фактический класс результата — `String[]`. У примитивного массива значения независимы, у ссылочного скопированы значения ссылок; переприсваивание позиции копии не меняет позицию оригинала.

Двухаргументный `Arrays.copyOf(T[], int)` сохраняет фактический класс массива, даже если ссылка расширена до `Object[]`. Перегрузка с третьим аргументом позволяет задать класс нового массива явно, но при несовместимом элементе возникает `ArrayStoreException`. `copyOfRange(a, from, to)` копирует полуинтервал `[from,to)`; начало должно быть от 0 до длины включительно, конец не меньше начала, выход конца за длину дополняется нулями/`null`. При отрицательной новой длине `copyOf` выбрасывает `NegativeArraySizeException`; при `null` исходном массиве — NPE (при остальных корректных аргументах). Для `copyOfRange` обратный диапазон вызывает `IllegalArgumentException`, недопустимое начало — `ArrayIndexOutOfBoundsException`.

Отдельный фрагмент внутри метода:

~~~java
Object[] widened = new String[]{"a"};
Object[] copied = java.util.Arrays.copyOf(widened, 2);
System.out.println(copied.getClass() == String[].class); // true
// copied[1] = 67; // ArrayStoreException при исполнении
Object[] flexible = java.util.Arrays.copyOf(widened, 2, Object[].class);
flexible[1] = 67; // допустимо
// java.util.Arrays.copyOf(new Object[]{67}, 1, String[].class); // ASE
System.out.println(java.util.Arrays.toString(
        java.util.Arrays.copyOfRange(new int[]{1,2}, 1, 4))); // [2, 0, 0]
~~~

**System.arraycopy** копирует диапазон **в существующий** массив. Для перекрывающихся областей одного массива результат определён так, как если бы значения соответствующего диапазона сначала были сохранены во временной области, а затем записаны в назначение:

~~~java
import java.util.Arrays;

class OverlapCopy {
    public static void main(String[] args) {
        int[] a = {1, 2, 3, 4};
        System.arraycopy(a, 0, a, 1, 3);
        System.out.println(Arrays.toString(a)); // [1, 1, 2, 3]
    }
}
~~~

Не все ошибки `System.arraycopy` дают частичное изменение. API задаёт последовательность проверок до копирования: (1) если любая ссылка `null` — `NullPointerException`; (2) если один аргумент не массив, один массив примитивный, другой ссылочный, либо оба примитивные с разными типами компонентов — `ArrayStoreException`; (3) если позиции/длина отрицательны или диапазон выходит за границы — `IndexOutOfBoundsException`. При этих отказах массив назначения не изменяется. Примитивы не расширяются и не упаковываются автоматически. Даже нулевая длина не отменяет эти проверки: `int[] → long[]` даёт ASE при длине 0, тогда как `Integer[] → String[]` с корректными границами и длиной 0 допустимо, поскольку нет элемента для проверки. Частичное изменение возможно при несовместимости конкретного ссылочного элемента после предварительных проверок:

~~~java
import java.util.Arrays;

class PartialCopy {
    public static void main(String[] args) {
        Object[] source = {"OK", 67, "unused"};
        String[] destination = {"old0", "old1", "old2"};
        try {
            System.arraycopy(source, 0, destination, 0, 3);
        } catch (ArrayStoreException e) {
            System.out.println(Arrays.toString(destination));
            // [OK, old1, old2]: допустимый префикс уже скопирован
        }
    }
}
~~~

**Почему так:** Javadoc [System.arraycopy](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/System.html#arraycopy(java.lang.Object,int,java.lang.Object,int,int)) определяет поведение, когда очередной reference element несовместим с фактическим типом компонента назначения: предыдущие элементы, которые можно было скопировать, уже скопированы. В примере это первый элемент `"OK"`, а Integer на следующей позиции вызывает `ArrayStoreException`. Это **гарантия условий такого префикса в API**, а не обещание общей транзакционной атомарности или определённого частичного результата при *любых* других ошибках. A20 защищённого `ArraysG1` проверяет только класс исключения. `PartialCopy` — самостоятельный учебный фрагмент с ожидаемым выводом; эквивалентный случай проверен отдельной временной пробой D-A14a, а сам фрагмент в CI не исполняется.

В отдельной временной пробе роли 02 при исходной авторской доработке D-A14a проверены приоритет `null`/типов перед границами, неизменность назначения после предварительного отказа, совместимый префикс `[OK, old1, old2]` и оба случая нулевой длины. Это новый на тот момент локальный опыт, отдельный от исторического smoke и A20, не утверждение о транзакциях, конкуренции или внутреннем алгоритме.

Сравнение выбора: `clone` — такая же длина, `copyOf` — новая длина, `arraycopy` — запись в уже существующее назначение, включая возможное перекрытие и частичное изменение. Ссылки и aliasing подробнее в [передаче аргументов](parameters.md).

## 5. equals, deepEquals и печать массивов (AR-10, AR-11, AR-21 · P2 граница)

Массив использует унаследованное `Object.equals`: два разных массива не равны через него по содержимому. `Arrays.equals` выбирается по типам массивов: у примитивных — соответствующая перегрузка, у `Object[]` — равенство соответствующих элементов с обработкой `null`. Для `float[]`/`double[]` равенство элементов соответствует `Float.equals`/`Double.equals`: NaN равны, а `+0.0` и `-0.0` различны, в отличие от примитивного `==`. Для вложенных массивов `Arrays.deepEquals(Object[], Object[])` рекурсивно сравнивает вложенные массивы, включая однотипные примитивные массивы.

~~~java
import java.util.Arrays;

class ArrayEquality {
    public static void main(String[] args) {
        int[] a = {1, 2}, b = {1, 2};
        System.out.println(a == b);                 // false
        System.out.println(a.equals(b));             // false
        System.out.println(Arrays.equals(a, b));     // true

        int[][] left = {{1}, {2}};
        int[][] right = {{1}, {2}};
        System.out.println(Arrays.equals(left, right));     // false
        System.out.println(Arrays.deepEquals(left, right)); // true
        System.out.println(Arrays.toString(left));          // строки вложенных массивов в стиле Object.toString
        System.out.println(Arrays.deepToString(left));      // [[1], [2]]
    }
}
~~~

`Arrays.toString` полезен для элементов одного уровня; в этом примере строки вложенных массивов имеют вид имени класса с `@` и шестнадцатеричным хеш-кодом, а не адресов или содержимого. Не фиксируйте конкретный суффикс. `deepToString` отображает содержимое вложенных массивов.

Для содержательного равенства выбирайте соответствующую пару `Arrays.equals`/`Arrays.hashCode` или `Arrays.deepEquals`/`Arrays.deepHashCode` на ациклических вложенных массивах. В перегрузках для массивов целиком (не диапазонов) две ссылки со значением `null` равны, ровно одна — нет; хеш-код массива `null` равен 0. Например, `Arrays.equals(new double[]{Double.NaN}, new double[]{Double.NaN})` — `true`, а сравнение массивов `new float[]{0f}` и `new float[]{-0f}` — `false`. Подробнее — [CORE-EQUALITY](equality.md) и [CORE-NUMBERS](numbers.md).

**Циклы: контракты различаются.** `Arrays.deepToString` обнаруживает прямую/косвенную самоссылку массива и отображает её как `[...]`. Для `Object[] cycle = new Object[1]; cycle[0] = cycle;` результат `Arrays.deepToString(cycle)` — `[[...]]`. Напротив, `deepEquals` и `deepHashCode` для массивов, прямо или косвенно содержащих себя, имеют неопределённое API поведение; такие вызовы здесь не выполняются. **AR-21 P2:** это не универсальная обработка циклических графов произвольных объектов; например, собственный рекурсивный `toString` обычного объекта не покрывается этой гарантией.

## 6. Arrays.asList — представление поверх массива, а не «новый ArrayList» (AR-12, AR-13, AR-18)

`Arrays.asList(T... a)` создаёт список **фиксированного размера**, связанный (*backed by*) с **переданным массивом**. Это `List`, реализующий `RandomAccess` и `Serializable`, но не обязательно `java.util.ArrayList`. Поддерживаемый `set` изменяет тот же элемент массива; изменение массива видно через список. Операции, которые действительно меняли бы размер, выбрасывают `UnsupportedOperationException`; допустим успешный no-op, например `remove` отсутствующего элемента может вернуть `false`.

~~~java
import java.util.Arrays;
import java.util.List;

class BackedList {
    public static void main(String[] args) {
        String[] words = {"old", "b"};
        List<String> view = Arrays.asList(words);
        view.set(0, "new");                // допустимо
        System.out.println(words[0]);      // new
        words[1] = "changed";
        System.out.println(view.get(1));   // changed
        try {
            view.add("c");
        } catch (UnsupportedOperationException e) {
            System.out.println("fixed-size");
        }

        List<String> unmodifiable = List.of("a", "b");
        // unmodifiable.set(0, "c") -> UnsupportedOperationException
        List<String> snapshot = List.copyOf(view); // снимок этих значений, не backed view
        words[0] = "later";
        System.out.println(snapshot.get(0)); // new
    }
}
~~~

Ограничение `set` следует из реального компонентного типа массива, а не только из generic-типа представления. Следующий отдельный фрагмент размещается внутри метода:

~~~java
Object[] alias = new String[]{"old"};
java.util.List<Object> view = java.util.Arrays.asList(alias);
view.set(0, "new"); // допустимо
// view.set(0, 67); // ArrayStoreException при исполнении
System.out.println(view.remove("absent")); // false на проверенном OpenJDK 21
~~~

Фиксированный размер не означает запрета всех необязательных операций независимо от входа; API допускает отказ и для операции, которая не меняет коллекцию. См. [COL-SEQUENTIAL](../02-collections/sequential.md).

`List.of` и `List.copyOf` дают **unmodifiable**-списки, запрещают null. `Arrays.asList` допускает элементы `null`, но не `null` вместо самого массива. `List.copyOf` создаёт структурный снимок: последующие изменения исходного списка, в том числе изменяемого backed view, в результате не отражаются. Снимок поверхностный: он сохраняет ссылки на элементы, а не копирует объекты, поэтому последующая мутация общего объекта (например, `StringBuilder.append`) видна и через unmodifiable-список. API допускает повторное использование подходящего неизменяемого списка, но нельзя рассчитывать на это для произвольного unmodifiable-представления изменяемого источника. Не гарантируется новый объект List при каждом вызове, равно как и глубокое копирование. Контракты `Collection`/`List`/`Map`, ограничения операций и отличия неизменяемой копии от связанного с оригиналом представления объяснены в [COL-CONTRACTS](../02-collections/contracts.md).

**Ловушка primitive varargs:**

~~~java
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

class PrimitiveArrayViews {
    public static void main(String[] args) {
        int[] xs = {1, 2, 3};
        List<int[]> oneElement = Arrays.asList(xs);
        System.out.println(oneElement.size()); // 1
        System.out.println(oneElement.get(0) == xs); // true
        System.out.println(Arrays.stream(xs).sum()); // 6
        IntStream stream = Arrays.stream(xs);       // примитивные int, не Stream<int[]>
        System.out.println(stream.count());         // 3
    }
}
~~~

Почему список длины 1? Generic varargs работает с типами ссылок. `int[]` — один ссылочный объект; для вызова `Arrays.asList(xs)` компилятор создаёт внешний varargs-массив `int[][]` длины 1. Его непосредственный компонентный тип — `int[]`, а значение единственного элемента — ссылка на `xs`. `oneElement.set(0, new int[]{67})` заменяет позицию внешнего массива, не изменяя содержимое первоначального `xs`. Напротив, до замены `oneElement.get(0)[0]` обращается к исходному массиву. `Arrays.stream(int[])` возвращает `IntStream`; если нужен `List<Integer>`, можно использовать `Arrays.stream(xs).boxed().toList()` (unmodifiable-список).

Отдельный контраст: `Arrays.asList((String[]) null)` выбрасывает NPE, поскольку передан `null` вместо varargs-массива. `Arrays.asList((String) null)` создаёт массив с одним элементом `null`, список размера 1. Подробности — в [CORE-STREAMS](streams.md).

## 7. Обобщённые массивы, reifiable-компоненты и реальная опасность @SafeVarargs (AR-14, AR-15, AR-22)

**Важно различать объявление массива и его непосредственное создание:** для выражения `new` или инициализатора массива тип элемента должен быть reifiable. Это ограничение не запрещает объявлять, использовать, получать или клонировать уже существующие массивы с non-reifiable статическим компонентным типом. Например, `List<String>[] a;` допустимо, но `List<String>[] a = {};` запрещено ([JLS §10.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html#jls-10.6)). `List<String>[]` и `T[]` допустимы в объявлениях, параметрах и результатах методов. `new List<String>[3]`, `new T[1]` и `new List<? extends Number>[1]` запрещены, поскольку компонент non-reifiable; `new List<?>[3]` допустим, поскольку параметризация только неограниченными wildcard reifiable. См. [JLS §4.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.7), [§10.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html#jls-10.3), [§15.10.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.10.1).

~~~java
import java.util.List;

class GenericArrays {
    static List<?>[] allowed() {
        List<?>[] items = new List<?>[2];
        items[0] = List.of("text");
        items[1] = List.of(67);
        return items;
    }
    // List<String>[] bad = new List<String>[2]; // compile error: generic array creation
}
~~~

Следующий самостоятельный класс компилируется без unchecked cast:

~~~java
class ArrayTypes {
    java.util.List<String>[] declared; // законное объявление
    static java.util.List<String>[] receive(java.util.List<String>[] a) {
        return a.clone(); // уже полученный массив
    }
    static <T> T[] cloneOf(T[] a) { return a.clone(); }
    // static <T> T[] bad() { return new T[1]; } // generic array creation
    // java.util.List<? extends Number>[] bad = new java.util.List<? extends Number>[1];
}
~~~

В исходной доработке D-A14a роль 02 строго скомпилировала отдельный класс `Probe`: объявление `List<String>[]` и тело метода приёма/клонирования без unchecked cast законны; при запуске для `List<String>[]` была проверена только ветка с `null`, а не клонирование массива со значением, отличным от `null`. Три запрещённые формы `new` и запрещённый инициализатор получили `compiler.err.generic.array.creation`. Отдельный `ListReceive` получил через varargs непустой `List<String>[]`: запуск подтвердил новый внешний массив, общую ссылку на элемент и сохранение фактического класса `List[]`; компиляция с `-Xlint:all` без `-Werror` зафиксировала предупреждение `[varargs]` на `receive(a)`, а не ошибку generic array creation. Это отдельные пробы, не дословные запуски `ArrayTypes` и `GenericArrays`. JVM проверяет фактический класс компонента (для созданного `List<?>[]` — `List`), но не параметр типа каждого списка. Через `Object[]` попытка записать `Integer` как объект приводит к ASE; запись `List<Integer>` в массив с runtime-компонентом `List` сама по себе проходит эту проверку. Это не делает безопасным загрязнение статически типизированного массива `List<String>[]`. Согласование с [CORE-GENERICS](generics.md) и [CORE-ERASURE](erasure.md).

**Нельзя «обойти» правило небезопасным cast, назвав код типобезопасным**. Raw-массивы и varargs могут привести к *heap pollution*: ссылка статически обещает `List<String>`, а реально хранит `List<Integer>`.

~~~java
import java.util.List;

class UnsafeVarargs {
    @SafeVarargs
    static void unsafe(List<String>... input) {
        Object[] alias = input;             // массив списков, доступ через Object[]
        alias[0] = List.of(67);             // heap pollution; runtime store здесь возможен
        String word = input[0].get(0);      // ClassCastException из-за вставленного Integer
        System.out.println(word);
    }

    public static void main(String[] args) {
        try {
            unsafe(List.of("safe"));
        } catch (ClassCastException e) {
            System.out.println("heap pollution");
        }
    }
}
~~~

При вызове метода переменной арности, когда требуется синтетический varargs-массив, он создаётся с фактическим типом, соответствующим стиранию применимого инстанцированного типа массива ([JLS 21 §15.12.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.4.2)): здесь это `List[]`, хотя статический тип параметра `List<String>[]`. Уже совместимый массив можно передать без создания нового varargs-массива. Это правило не разрешает писать `new List<String>[1]` и не является обходом через unchecked cast. Аннотация `@SafeVarargs` в этом фрагменте **намеренно ложная**: она может подавить соответствующие предупреждения в местах вызова, но **не** устраняет unsafe store, не делает метод доказанно безопасным и не меняет runtime-проверку компонента: JVM не проверяет параметр `<String>` каждого List при записи в массив List. Использовать такой метод в продукции нельзя. В исходном G1 этот случай вынесен отдельным probe, а в [CORE-ERASURE](erasure.md) объясняются unchecked/raw-переходы.

На OpenJDK/javac 21.0.12.1 отдельная эквивалентная проба с `@SafeVarargs`, `Object[] alias = input` и последующей небезопасной записью скомпилировалась при `--release 21 -Xlint:all` с предупреждением `compiler.warn.varargs.unsafe.use.varargs.param` (категория `[varargs]`), затем завершилась после перехвата `ClassCastException`. Это не ошибка языка при компиляции; аннотация не подавляет все предупреждения о небезопасном использовании массива в теле. При `-Werror` предупреждение может сделать компиляцию неуспешной, но не меняет правила создания массивов.

На собеседовании важно назвать **две разные ошибки**: `new List<String>[1]` — *compile-time* generic array creation; сознательно «загрязнённый» varargs — компилирующийся код с возможной ошибкой **позднее, при чтении элемента**. `@SafeVarargs` разрешён лишь в предусмотренных JLS контекстах и подразумевает обязательство разработчика по корректности.

## 8. Сортировка, поиск и границы API (AR-19 · P1)

`Arrays.sort` сортирует элементы согласно контракту нужной перегрузки: у `int[]` числовой порядок, у ссылочных массивов — естественный порядок/Comparator. Сортировка объектов **стабильна**: элементы, для которых сравнение даёт 0, сохраняют взаимный порядок, даже если `equals` возвращает `false`. `Arrays.binarySearch` предполагает массив, отсортированный по тому же порядку; без этого результат не определён. При нескольких эквивалентных ключах не гарантируется первый или последний индекс. При отсутствии ключа результат кодирует точку вставки: `-(insertionPoint) - 1`.

~~~java
import java.util.Arrays;

class SortedLookup {
    public static void main(String[] args) {
        int[] data = {9, 1, 5};
        Arrays.sort(data);
        System.out.println(Arrays.toString(data)); // [1, 5, 9]
        System.out.println(Arrays.binarySearch(data, 5)); // 1
        System.out.println(Arrays.binarySearch(data, 7)); // -3 (точка вставки 2)
    }
}
~~~

Отдельный фрагмент внутри метода:

~~~java
String[] words = {"bb", "aa", "c"};
var byLength = java.util.Comparator.comparingInt(String::length);
java.util.Arrays.sort(words, byLength); // [c, bb, aa]
int hit = java.util.Arrays.binarySearch(words, "zz", byLength);
System.out.println(hit == 1 || hit == 2); // true; не поиск по equals
int[] data = {1,3,5,9};
System.out.println(java.util.Arrays.binarySearch(data, 1, 4, 7)); // -4
double[] d = {Double.NaN, 0.0, -0.0};
java.util.Arrays.sort(d);
System.out.println(java.util.Arrays.toString(d)); // [-0.0, 0.0, NaN]
~~~

Диапазон `[fromIndex,toIndex)` исключает верхнюю границу; точка вставки — абсолютный индекс в исходном массиве, не смещение от `fromIndex` (здесь 3). Для `float`/`double` сортировка и поиск используют полный порядок, в котором отрицательный ноль предшествует положительному, NaN больше остальных значений, а все NaN эквивалентны. Конкретный алгоритм сортировки не гарантирован. См. [CORE-EQUALITY](equality.md), [CORE-UTIL](utilities.md).

Это не доказательство, что `binarySearch` «быстрее» для любого маленького массива: он работает с предварительно отсортированными данными, а сортировка тоже требует времени. Не обещайте стабильную сортировку примитивов по некому исходному порядку равных объектов и не смешивайте требования `Comparator` с равенством ссылок.

## 9. Короткие интервью-ловушки: что компилируется, а что падает

| Код/ситуация | Стадия | Причина |
|---|---|---|
| `Object x = new int[1];` | Компилируется | Массив — объект |
| `Object[] x = new int[1];` | **Не компилируется** | `int[]` не `Object[]` |
| `Object[] x = new String[1]; x[0] = 67;` | **Runtime** `ArrayStoreException` | Ковариантность ссылки, проверка фактического компонента |
| `List<String> s = List.of("x"); List<Object> x = s;` | **Не компилируется** | `List<String>` не подтип `List<Object>`; прямое `List<Object> x = List.of("x")` может компилироваться благодаря выводу типов |
| `int[] a = new int[-1];` | **Runtime** `NegativeArraySizeException` | Размер вычислен отрицательным |
| `new int[2].length()` | **Не компилируется** | `length` — поле |
| `new List<String>[1]` | **Не компилируется** | Non-reifiable компонент |
| `Arrays.asList(new int[]{1,2})` | Компилируется | Один элемент `int[]` |
| `Arrays.asList(new String[]{"x"}).add("y")` | **Runtime** `UnsupportedOperationException` | Fixed-size view |
| `System.arraycopy(Object[]→String[])` с несовместимым элементом | **Runtime** `ArrayStoreException` | Может быть скопирован совместимый префикс |

**Разбор ошибки собеседования:** «В массиве Java нет generics, значит типы не проверяются» — неверно. Типовой контроль есть, но у ковариантных ссылочных массивов часть проверки отложена до записи; generic-коллекции используют другие ограничения уже на стадии компиляции.

## Карта покрытия AR-01–AR-22 и честные границы

| ID | Приоритет | Механизм в статье и пример |
|---|---|---|
| AR-01 | P0 | §1: `int[]` vs `Integer[]` vs `Object[]`; §9 compile-negative |
| AR-02 | P0 | §1: `new`, default values, `length` поле; §3 индексы |
| AR-03 | P0 | §1: массив как `Object`, runtime component type |
| AR-04 | P0 | §2: `String[]→Object[]`, runtime `ArrayStoreException` |
| AR-05 | P0 | §2: invariance `List<T>`, reifiability и граница generics |
| AR-06 | P0 | §3: jagged, null, разные `length` строк |
| AR-07 | P0 | §4: `clone` shallow и aliasing вложенных строк |
| AR-08 | P0 | §4: `Arrays.copyOf` truncation/padding и shallow |
| AR-09 | P0 | §4: overlap/exception контракт `arraycopy` |
| AR-10 | P0 | §5: identity/`equals`/`deepEquals` |
| AR-11 | P0 | §5: `toString/deepToString`, цикл как граница |
| AR-12 | P0 | §6: `Arrays.asList` backed/fixed-size/set |
| AR-13 | P0 | §6: `asList(int[])` 1 элемент vs `IntStream` |
| AR-14 | P0 | §7: законные объявления/приём/clone `List<String>[]`/`T[]`, три запрещённых формы создания, `List<?>[]` |
| AR-15 | P0 | §7: heap pollution и намеренно ложная `@SafeVarargs` |
| AR-16 | P0 | §§2–4,9: compile-time vs runtime ошибки индекса/store/copy |
| AR-17 | P1 | §§1,3: дополнительные default values, пустой массив |
| AR-18 | P1 | §6: `asList/List.of/List.copyOf`, null/backing/mutability |
| AR-19 | P1 | §8: `sort/binarySearch`, предварительная сортировка |
| AR-20 | P1 | §4: API partial-prefix reference `arraycopy` (не полная транзакция) |
| AR-21 | P2 | §5: `deepToString` обрабатывает самоссылки массивов; `deepEquals/deepHashCode` для самоссылок не определены; произвольные графы вне scope |
| AR-22 | P2 | §7: локальный lint и CCE для одного unsafe varargs; расширенные raw/generic varargs и benchmarks вне scope |

## Источники и пределы воспроизводимости

**Спецификация:** [JLS §10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html) и [§15.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.10), [JLS §4.7 reifiable](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.7), [JLS §9.6.4.7 SafeVarargs](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4.7), [Arrays Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html), [System.arraycopy](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/System.html#arraycopy(java.lang.Object,int,java.lang.Object,int,int)), [List](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html). [G1-исследование](../../docs/plans/CORE-ARRAYS-STRINGS.md) сопоставляет нормативные источники с интервью-вопросами; [реестр](../../docs/SOURCE_REGISTRY.md) отделяет банки вопросов от Java SE API.

**Воспроизводимость:** G1 содержит полный `ArraysG1.java` с **24 подписанными строками stdout A01–A24**, а также отдельные компиляционно-отрицательные N01–N24 (некоторые относятся к массивам, другие — к строкам/Builder). Не считать A-строки 24 unit-tests и не путать негативные `javac` с намеренно перехваченными runtime exceptions. Исторически [автономный запускаемый пример](../../examples/core/arrays-strings/README.md) опубликован в общем пакете: два исходника из принятого G1 §6, два побайтных эталона stdout и один `verify.sh` с двумя вызовами `java` и двумя `diff -u`. Тогда роль 03 проверила опубликованные Git blobs и локально выполнила `javac --release 21` / два запуска / два сравнения на OpenJDK 21.0.12.1 (PASS); история интеграционного CI отражена в [PR #124](https://github.com/Melchiorius/backend-interview-labs/pull/124). Это исторические свидетельства, не результаты текущего HEAD. Для D-A14a роль 02 отдельно воспроизвела неизменённый общий runner при исходной авторской доработке; текущий CI проверяется отдельно на точном HEAD. Ни одна из этих проверок не означает компиляции всех фрагментов статьи.

**Пока не заявлены выполненными:** автоматическая компиляция *всех* inline Java-блоков из Markdown; запуск исторических N01–N24 G1 в CI; GUI Obsidian/IntelliJ; альтернативные JDK/OS; benchmark/JMH, GC/layout/HotSpot и произвольные циклические графы. Исторический отдельный локальный авторский smoke на OpenJDK 21.0.12.1 подтвердил эквивалентный §4 partial-copy случай: при несовместимом втором элементе назначения получен допустимый префикс `[OK, old1, old2]` и перехвачен `ArrayStoreException`. Это не автоматическая компиляция всех фрагментов статьи и не CI. Тогда же автор отдельно восстановил N01–N24 из G1 §7: **24/24 независимых `javac --release 21`** завершились ошибкой ожидаемой **диагностической категории** (не просто nonzero exit), без синтаксических/EOF ошибок; временные исходники не закоммичены и не запускаются в позитивном CI. Не смешивайте их с 24 маркерами `EXPECTED REJECTION` существующих проверок наследования/доступа в CI или с девятью новыми временными отрицательными единицами D-A14a, среди которых `BadDims` и `BadInit` намеренно проверяют ошибки грамматики. Для строк/UTF-16 и Builder продолжайте в [CORE-STRINGS](strings.md).

**D-A14a — временные пробы исходной авторской доработки роли 02:** на OpenJDK/javac 21.0.12.1 выполнено 13 компиляций: 1 строгая успешная (`Probe`), 2 успешные с разрешёнными предупреждениями (`UnsafeVarargs`, `ListReceive`), 9 ожидаемых отклонений по правилам языка и 1 отклонение предупреждения в режиме `-Werror` (не ошибка языка). Три запуска JVM завершились с кодом 0: `Probe` выдал восемь строк OK для конечных проверок типов массивов, порядка вычислений, копирования, равенства/хеширования, печати, представлений списков, generic array creation и сортировки/поиска; `UnsafeVarargs` перехватил `ClassCastException`; `ListReceive` подтвердил новый внешний массив, общий элемент и класс `List[]`. Из циклических операций проверялась только печать через `deepToString`. Отдельный неизменённый runner дал 52 строки двух эталонов и итоговый маркер. Исходники, фактические результаты, SHA и границы проверок зафиксированы в [авторском отчёте роли 02 #7206528031](https://github.com/Melchiorius/backend-interview-labs/issues/327#issuecomment-7206528031). Это свидетельства исходной доработки, не повторные локальные прогоны при редактуре, не новые постоянные тесты, не проверка всех фрагментов или входов и не повторное исследование G1.

**Переход к C2:** [последовательные коллекции](../02-collections/sequential.md) объясняют различие длины массива, размера и вместимости `ArrayList`, а также связь `Arrays.asList` с исходным массивом. [Итераторы и цикл for-each](../02-collections/iteration.md) показывают, почему JLS обходит массив и `Iterable` разными способами.

**Дополнительный материал по стандартным API:** [CORE-UTIL](utilities.md) сопоставляет `Arrays.equals/deepEquals/mismatch/compare/sort/binarySearch`, fixed-size backed `Arrays.asList`, компараторы и shallow-copy с примерами и предусловиями.
