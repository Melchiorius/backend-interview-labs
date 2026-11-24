# CORE-ARRAYS — массивы Java 21: типы, ковариантность, копии и представления

**Область:** Java SE 21, без preview. **Сначала:** [типы и значения](types.md), [преобразования](conversions.md), [передача аргументов](parameters.md). Затем: [generics](generics.md), [стирание типов](erasure.md), [равенство](equality.md), [Stream API](streams.md), [строки и Builder](strings.md). Эта статья объясняет не только синтаксис массива, но и **почему проверка типов иногда происходит при компиляции, а иногда — при записи элемента**, как устроено разделение ссылок и какие контракты есть у копирования.

**Основа:** [принятый исследовательский G1-план](../../docs/plans/CORE-ARRAYS-STRINGS.md), матрица **AR-01–AR-22**, в том числе 16 P0 / 4 P1 / 2 P2. Основные нормы — [JLS 21 §10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html), [JLS §15.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.10), [Arrays Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html). **Строки в кавычках в stdout ниже — ожидаемые результаты показанных входов, а не свидетельство компиляции каждого фрагмента этого Markdown.** Для самостоятельного воспроизведения используйте [общий runnable Arrays + Strings/Builders](../../examples/core/arrays-strings/README.md): `ArraysG1.java` выводит A01–A24, `StringsG1.java` — S01–S28. Это **52 наблюдения двух запусков**, не 52 независимых теста.

## 1. Массив — объект фиксированной длины с конкретным типом компонента (AR-01, AR-02, AR-03)

Массив объединяет элементы **одного компонентного типа**, к которым обращаются по индексам от нуля до `length - 1`. Длину выбирают при создании; менять её нельзя. Массив не является списком с `add`. Каждый массив, включая `int[]`, — объект: его ссылка совместима с `Object`, `Cloneable` и `java.io.Serializable`.

**Типы компонентов нельзя смешивать:**

- `int[]` хранит значения примитивного `int`; `Integer[]` хранит ссылки на `Integer`. Autoboxing не превращает весь `int[]` в `Integer[]`.
- `String[]` — массив ссылок на `String`, то есть одновременно подтип `Object[]`. `int[]` **не** подтип `Object[]` и не подтип `Integer[]`; его элементы не являются объектами.
- У ссылки `Object array` можно вызвать `getClass()`: во время выполнения компонентный тип массива остаётся известным. Это существенно для проверки записи элемента (§2).

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

## 3. Индексы, многомерность и массивы массивов (AR-02, AR-06, AR-16, AR-17)

Запись `int[][]` означает **массив, элемент которого — ссылка на `int[]`**, а не обязательно сплошную прямоугольную область памяти. Внешний массив имеет свою `length`, каждая непустая строка — свою. Строки могут иметь разные длины (*jagged*) либо оставаться `null`.

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

Ошибка длины/индекса/несовместимости типов означает, что нельзя делать вид, будто копирование **транзакционно**. Особенно важен reference store:

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

**Почему так:** Javadoc [System.arraycopy](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/System.html#arraycopy(java.lang.Object,int,java.lang.Object,int,int)) определяет поведение, когда очередной reference element несовместим с фактическим типом компонента назначения: предыдущие элементы, которые можно было скопировать, уже скопированы. В примере это первый элемент `"OK"`, а Integer на следующей позиции вызывает `ArrayStoreException`. Это **гарантия условий такого префикса в API**, а не обещание общей транзакционной атомарности или определённого частичного результата при *любых* других ошибках. Исходный G1 A20 проверяет лишь класс исключения; этот отдельный читательский фрагмент демонстрирует смысл рекомендации независимого ревью, **пока не объявлен выполненным авторским CI**.

Сравнение выбора: `clone` — такая же длина, `copyOf` — новая длина, `arraycopy` — запись в уже существующее назначение, включая возможное перекрытие и частичное изменение. Ссылки и aliasing подробнее в [передаче аргументов](parameters.md).

## 5. equals, deepEquals и печать массивов (AR-10, AR-11, AR-21 · P2 граница)

Массив не переопределяет содержательный `Object.equals`; поэтому `a.equals(b)` для разных объектов массива не равно сравнению их элементов. `Arrays.equals(int[], int[])` сравнивает **элементы одномерного массива**, а у `Object[]` применяет равенство соответствующих элементов согласно своему контракту. Для вложенных массивов нужен `Arrays.deepEquals(Object[], Object[])`, который распознаёт вложенные массивы и сравнивает их содержимое рекурсивно.

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
        System.out.println(Arrays.toString(left));          // ссылки на вложенные массивы
        System.out.println(Arrays.deepToString(left));      // [[1], [2]]
    }
}
~~~

`Arrays.toString` полезен для элементов одного уровня, `deepToString` — для вложенных массивов, `deepHashCode` — для согласованного содержательного хэша при соответствующих условиях. Однако это **не универсальные алгоритмы сравнения произвольных циклических графов объектов**: для `deepEquals` Javadoc не определяет поведение массивов с самоссылками, а для других deep-операций есть собственные ограничения. **AR-21 P2:** здесь не конструируется произвольный самоссылочный граф, не обещаются termination и его корректная обработка. Для доменной модели объектов с циклами нужна отдельная явно определённая семантика.

## 6. Arrays.asList — представление поверх массива, а не «новый ArrayList» (AR-12, AR-13, AR-18)

`Arrays.asList(T... a)` создаёт список **фиксированного размера**, связанный (*backed by*) с **переданным массивом**. Это `List`, но не обязательно `java.util.ArrayList`. Метод `set` изменяет тот же элемент массива; изменение массива видно через список. `add/remove` меняют размер — запрещены.

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

`List.of` и `List.copyOf` дают **unmodifiable**-списки, запрещают null. `Arrays.asList` допускает null, сохраняет фиксированную длину и поддерживает `set`. При этом `List.copyOf` может вернуть переданный список напрямую, если тот уже соответствует его требованиям, поэтому не обещайте отдельный новый объект List при каждом вызове; существенная гарантия — отсутствие дальнейшего отражения изменений исходного *изменяемого backed view* в указанном сценарии. Контракты `Collection`/`List`/`Map`, ограничения операций и отличия неизменяемой копии от связанного с оригиналом представления объяснены в [COL-CONTRACTS](../02-collections/contracts.md).

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

Почему список длины 1? Generic varargs работает с типами ссылок, а `int[]` — **один ссылочный объект массива**. `Arrays.stream(int[])` имеет отдельную перегрузку, возвращающую `IntStream`; не смешивайте её с `Stream<Integer>`. Подробности — в [CORE-STREAMS](streams.md).

## 7. Обобщённые массивы, reifiable-компоненты и реальная опасность @SafeVarargs (AR-14, AR-15, AR-22)

Элемент массива должен иметь **reifiable** тип: тип, проверяемый в нужной степени во время выполнения. `new List<String>[3]` не компилируется — параметр `String` после стирания не доступен JVM для надёжной проверки `ArrayStoreException`. При этом `new List<?>[3]` допустим: unbounded wildcard-тип reifiable.

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

Аннотация `@SafeVarargs` в этом фрагменте **намеренно ложная**. Она может подавить соответствующие предупреждения в местах вызова, **не** устраняет unsafe store и не делает метод доказанно безопасным; JVM не проверяет параметр `<String>` каждого List при записи в массив List. Использовать такой метод в продукции нельзя. В исходном G1 этот случай вынесен отдельным probe, а в [CORE-ERASURE](erasure.md) объясняются unchecked/raw-переходы.

На собеседовании важно назвать **две разные ошибки**: `new List<String>[1]` — *compile-time* generic array creation; сознательно «загрязнённый» varargs — компилирующийся код с возможной ошибкой **позднее, при чтении элемента**. `@SafeVarargs` разрешён лишь в предусмотренных JLS контекстах и подразумевает обязательство разработчика по корректности.

## 8. Сортировка, поиск и границы API (AR-19 · P1)

`Arrays.sort` сортирует элементы согласно контракту нужной перегрузки: у `int[]` числовой порядок, у ссылочных массивов — естественный порядок/Comparator. `Arrays.binarySearch` предполагает **отсортированный** массив с согласованным сравнением; без этого корректность результата не гарантируется. Если ключ не найден, результат кодирует точку вставки: `-(insertionPoint) - 1`.

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
| AR-14 | P0 | §7: `new List<String>[]` compile error, `List<?>[]` |
| AR-15 | P0 | §7: heap pollution и намеренно ложная `@SafeVarargs` |
| AR-16 | P0 | §§2–4,9: compile-time vs runtime ошибки индекса/store/copy |
| AR-17 | P1 | §§1,3: дополнительные default values, пустой массив |
| AR-18 | P1 | §6: `asList/List.of/List.copyOf`, null/backing/mutability |
| AR-19 | P1 | §8: `sort/binarySearch`, предварительная сортировка |
| AR-20 | P1 | §4: API partial-prefix reference `arraycopy` (не полная транзакция) |
| AR-21 | P2 | §5: циклические self-referential массивы **не покрыты алгоритмом** |
| AR-22 | P2 | §7: расширенные raw/generic varargs, lint и benchmarks **не доказаны** |

## Источники и пределы воспроизводимости

**Спецификация:** [JLS §10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html) и [§15.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.10), [JLS §4.7 reifiable](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.7), [JLS §9.6.4.7 SafeVarargs](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4.7), [Arrays Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html), [System.arraycopy](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/System.html#arraycopy(java.lang.Object,int,java.lang.Object,int,int)), [List](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html). [G1-исследование](../../docs/plans/CORE-ARRAYS-STRINGS.md) сопоставляет нормативные источники с интервью-вопросами; [реестр](../../docs/SOURCE_REGISTRY.md) отделяет банки вопросов от Java SE API.

**Воспроизводимость:** G1 содержит полный `ArraysG1.java` с **24 подписанными строками stdout A01–A24**, а также отдельные компиляционно-отрицательные N01–N24 (некоторые относятся к массивам, другие — к строкам/Builder). Не считать A-строки 24 unit-tests и не путать негативные `javac` с намеренно перехваченными runtime exceptions. [Автономный запускаемый пример](../../examples/core/arrays-strings/README.md) включён в ту же ветку: два исходника из принятого G1 §6, два побайтных эталона stdout и один `verify.sh` с двумя вызовами `java` и двумя `diff -u`. Роль 03 проверила опубликованные Git blobs и локально выполнила `javac --release 21` / два запуска / два сравнения на OpenJDK 21.0.12.1 (PASS). GitHub Actions на **конечном интегрированном HEAD** проверяются отдельно в [PR #124](https://github.com/Melchiorius/backend-interview-labs/pull/124); этот локальный результат **не означает компиляции всех snippets статьи**.

**Пока не заявлены выполненными:** автоматическая компиляция *всех* inline Java-блоков из Markdown; N01–N24 в CI; GUI Obsidian/IntelliJ; альтернативные JDK/OS; benchmark/JMH, GC/layout/HotSpot и произвольные циклические графы. Отдельный локальный авторский smoke на OpenJDK 21.0.12.1 подтвердил эквивалентный §4 partial-copy случай: при несовместимом втором элементе назначения получен допустимый префикс `[OK, old1, old2]` и перехвачен `ArrayStoreException`. Это не автоматическая компиляция всех фрагментов статьи и не CI. На том же JDK автор отдельно восстановил N01–N24 из G1 §7: **24/24 независимых `javac --release 21`** завершились ошибкой ожидаемой **диагностической категории** (не просто nonzero exit), без синтаксических/EOF ошибок; временные исходники не закоммичены и не запускаются в позитивном CI. Для строк/UTF-16 и Builder продолжайте в [CORE-STRINGS](strings.md).

**Дополнительный материал по стандартным API:** [CORE-UTIL](utilities.md) сопоставляет `Arrays.equals/deepEquals/mismatch/compare/sort/binarySearch`, fixed-size backed `Arrays.asList`, компараторы и shallow-copy с примерами и предусловиями.
