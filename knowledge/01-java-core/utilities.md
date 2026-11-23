# CORE-UTIL — Objects, Comparator, Arrays и представления коллекций в Java 21

**Практический вопрос.** Как сравнить два nullable значения, отсортировать записи по двум ключам, найти элемент массива и вернуть «неизменяемый список»? Подводные камни начинаются с различий: **null-safe ≠ deep**, **unmodifiable ≠ snapshot**, **компараторная эквивалентность ≠ equals**, а **binarySearch требует отсортированного входа**. Эти различия непосредственно меняют данные и результат программы.

Материал относится к **Java SE 21 без preview**. Ранее опубликованы [CORE-EQUALITY](equality.md), [CORE-ARRAYS](arrays.md), [CORE-IMMUTABLE](immutability.md), [CORE-FUNCTIONS](functions.md) и [CORE-EXCEPTIONS](exceptions.md); здесь их контракты сопоставляются на связных примерах. Это не замена полных будущих тем `COL-SORTED`, `CORE-I18N` или `CORE-SERIAL`.

**Нормативные источники A:** [Objects API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html), [Comparator API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html), [Arrays API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html), [List API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html), [Collections API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html), [TreeSet API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeSet.html), [JLS 21 `15](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html). Принятый план [CORE-REGEX-UTIL G1](../../docs/plans/CORE-REGEX-UTIL.md), `4–`7, содержит полную историческую `UtilProbe.java` (38 строк stdout) и восемь отдельных компиляционно-отрицательных исходников. **Матрица статьи:** UT-01–UT-29 = 29 P0, UT-30–UT-36 = 7 P1, UT-37–UT-41 = 5 P2. Это вопросы, а не количество JUnit-тестов.

## 1. Objects: null, вложенные массивы и хеши (UT-01–UT-03)

`Objects.equals(a,b)` сначала обрабатывает `null`, а для ненулевого `a` вызывает его `equals(b)`. Если два разных `int[]` содержат те же элементы, обычный `equals` остаётся **identity**, так как массивы наследуют реализацию от `Object`. `Objects.deepEquals` умеет сравнивать массивы по элементам, но не отражает все поля произвольных POJO и не является универсальным алгоритмом обхода графов.

~~~java
import java.util.*;

int[] a = {67};
int[] b = {67};
System.out.println(Objects.equals(null, null)); // true
System.out.println(Objects.equals(a, b));       // false
System.out.println(Arrays.equals(a, b));        // true
System.out.println(Objects.deepEquals(a, b));   // true
Object[] left = {new int[]{67}};
Object[] right = {new int[]{67}};
System.out.println(Arrays.equals(left, right));     // false
System.out.println(Arrays.deepEquals(left, right)); // true
~~~

**Тонкость сигнатур:** `Arrays.deepEquals(Object[],Object[])` нельзя вызвать напрямую для двух `int[]` — это ошибка *javac*, а не `false`. Однако `Objects.deepEquals(int[],int[])` допустим, так как параметры типа `Object`, и в runtime он обнаружит массивы. Для циклических структур не объявляйте `deepEquals` универсальным безопасным обходчиком.

`Objects.hashCode(obj)` возвращает 0 для null и `obj.hashCode()` для ненулевого объекта. `Objects.hash(values...)` вычисляет хеш **последовательности**, поэтому результаты могут различаться даже для одного элемента:

~~~java
System.out.println(Objects.hashCode(67)); // 67
System.out.println(Objects.hash(67));     // 98 = 31 * 1 + 67
System.out.println(Objects.toString(null, "missing")); // missing
~~~

`Objects.toString(obj,defaultValue)` возвращает *переданную* альтернативу при null; она сама может быть null. `String.valueOf((Object)null)`, напротив, даёт текст `"null"`. Ни один хеш не является уникальным persistent ID или криптографической гарантией; правила согласованности с equals — в [CORE-EQUALITY](equality.md).

## 2. requireNonNull и проверка полуинтервалов (UT-04–UT-07)

`requireNonNull(value)` возвращает ту же ссылку, если value ненулевое, или бросает `NullPointerException`. Перегрузка `requireNonNull(value, String)` использует сообщение, а `requireNonNull(value, Supplier<String>)` вызывает `Supplier.get()` **только при null**, позволяя не формировать дорогое диагностическое сообщение на успешном пути.

~~~java
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

var calls = new AtomicInteger();
String ok = Objects.requireNonNull("ready", () -> {
    calls.incrementAndGet();
    return "missing";
});
System.out.println(ok + ":" + calls.get()); // ready:0
try {
    Objects.requireNonNull(null, () -> {
        calls.incrementAndGet();
        return "missing";
    });
} catch (NullPointerException e) {
    System.out.println(e.getMessage() + ":" + calls.get()); // missing:1
}
~~~

Эта ленивость относится к **вызову Supplier**, а не к вычислению произвольных аргументов Java. `requireNonNullElse` и `requireNonNullElseGet` (UT-32) возвращают *запасное значение*, а не сообщение исключения.

Методы контроля границ проверяют **разные формы одного полуинтервала**: `checkIndex(index,length)` требует `0 ≤ index < length`; `checkFromToIndex(from,to,length)` — `0 ≤ from ≤ to ≤ length`; `checkFromIndexSize(from,size,length)` — неотрицательные from и size и размещение участка в пределах length. Для `[1,3)` size=2, а не 3. Неверные значения приводят к **runtime** `IndexOutOfBoundsException`.

~~~java
System.out.println(Objects.checkIndex(2, 3));             // 2
System.out.println(Objects.checkFromToIndex(1, 3, 3));    // 1
System.out.println(Objects.checkFromIndexSize(1, 2, 3));  // 1
try { Objects.checkIndex(3, 3); }
catch (IndexOutOfBoundsException ex) { System.out.println("outside"); }
~~~

Успех не даёт гарантии, что исходный контейнер потом не изменится конкурентно. А текст исключения не является переносимым API-протоколом.

## 3. Comparator: лексикографический порядок, reversed и null (UT-08–UT-12)

`Comparator.compare(a,b)` определяет знак отношения; результату не нужно равняться точно `-1/0/1`. `Comparator.comparing` извлекает первый ключ, `thenComparingInt` добавляет второй **только при эквивалентности первых**:

~~~java
import java.util.*;

record User(String name, int rank) {}
Comparator<User> byNameAndRank = Comparator.comparing(User::name)
        .thenComparingInt(User::rank);
var users = new ArrayList<>(List.of(
        new User("bob", 2), new User("anna", 3), new User("bob", 1)));
users.sort(byNameAndRank);
System.out.println(users);
// [User[name=anna, rank=3], User[name=bob, rank=1], User[name=bob, rank=2]]
~~~

`reversed()` разворачивает **всю готовую композицию**, а не лишь второй критерий. Поэтому `byNameAndRank.reversed()` меняет порядок и по name, и по rank. Если нужен обратный rank только при одинаковых именах, задайте для второго ключа отдельный reversed comparator.

`Comparator.nullsFirst(inner)` / `nullsLast(inner)` определяют порядок **самих null-элементов**. Это не то же самое, что `null` у **извлечённого ключа**: `Comparator.comparing(User::name)` без null-aware компаратора ключа при `name == null` может бросить NPE.

~~~java
Comparator<User> byNullableName = Comparator.comparing(
        User::name, Comparator.nullsLast(Comparator.naturalOrder()));
// Здесь nullsLast применён к строке-ключу, а не к User.
~~~

`nullsFirst(naturalOrder()).reversed()` ставит null **последним** вместе с разворотом порядка ненулевых значений. Тонкий, но нормативный случай `Comparator.nullsFirst(null)`: null меньше остальных, а **все ненулевые элементы эквивалентны** между собой, независимо от equals. Это особенно важно при использовании в сортированных множествах.

## 4. Comparator-эквивалентность и TreeSet (UT-13, UT-14, UT-27)

`HashSet` использует `equals/hashCode`, `TreeSet` определяет принадлежность через `compare(a,b)==0` (либо `compareTo`). Если сравнение по регистронезависимому порядку считает `"A"` и `"a"` одинаковыми, а `String.equals` — нет, размеры множества отличаются.

~~~java
import java.util.*;
System.out.println(new HashSet<>(List.of("A", "a")).size()); // 2
var sorted = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
sorted.add("A");
sorted.add("a");
System.out.println(sorted.size()); // 1
~~~

Это не ошибка реализации дерева: у порядка и равенства **разные контракты**. Но comparator, несовместимый с equals, может нарушить общий ожидаемый Set semantics при сравнении с другими Set. Полный дизайн контрактов и сортированных структур выходит за границы статьи; [CORE-EQUALITY](equality.md) объясняет математический смысл и инженерные последствия.

## 5. Arrays.equals, deepEquals, mismatch и compare (UT-15–UT-18)

`Arrays.equals(int[],int[])` сравнивает элементы по позициям. Для `Object[]` обычный `Arrays.equals` обращается к equals элементов; вложенный `int[]` остаётся identity. `Arrays.deepEquals(Object[],Object[])` рекурсивно использует array-aware сравнение для массивных элементов — это **не клонирование** массива, не рефлексивное сравнение всех полей POJO и не универсальное сравнение произвольных циклов.

`Arrays.mismatch` возвращает **индекс первой разницы**; `-1` при совпадении, а при одинаковом префиксе массивов разной длины — длину короткого массива. `Arrays.compare` реализует **лексикографический порядок**; знак результата, а не его величина, содержит смысл порядка.

~~~java
import java.util.Arrays;
System.out.println(Arrays.mismatch(new int[]{1, 67}, new int[]{1, 68})); // 1
System.out.println(Arrays.mismatch(new int[]{1}, new int[]{1}));         // -1
System.out.println(Arrays.compare(new int[]{1, 67}, new int[]{1, 68}) < 0); // true
~~~

Эти overload зависят от типа массива: нельзя бездумно переносить сравнение `int[]` на `Integer[]` с nullable элементами, где применяются другие правила.

## 6. Arrays.sort и binarySearch: предусловие и формула (UT-19, UT-20, UT-28, UT-29)

`Arrays.sort(int[])` сортирует по примитивному порядку, `Arrays.sort(T[],Comparator)` — по переданному comparator. **Объектная сортировка стабильна согласно Arrays API**: элементы, эквивалентные по comparator, сохраняют взаимный порядок. Из этого **не следует** конкретный алгоритм реализации и не требуется понятие «identity стабильности» для примитивов.

~~~java
import java.util.*;
int[] numbers = {67, 1, 9};
Arrays.sort(numbers); // [1, 9, 67]
System.out.println(Arrays.binarySearch(numbers, 9)); // 1
System.out.println(Arrays.binarySearch(numbers, 8)); // -2
String[] words = {"bbb", "a", "cc"};
Arrays.sort(words, Comparator.comparingInt(String::length));
System.out.println(Arrays.toString(words)); // [a, cc, bbb]
~~~

Если ключ не найден, ответ равен **`-(insertionPoint)-1`** (здесь insertion point 1 даёт `-2`). Для hit возвращается один индекс равного элемента, но при нескольких равных конкретный выбор **не гарантирован**. Binary search требует, чтобы массив был **предварительно отсортирован совместимым порядком**. На неотсортированном входе результат API *undefined*: программа может успешно выполниться, но его нельзя интерпретировать как корректное доказательство наличия/отсутствия элемента. Это runtime предусловие, а не ограничение javac.

## 7. Arrays.asList: фиксированный размер и двусторонний backing (UT-21–UT-23)

`Arrays.asList(T... a)` возвращает **фиксированного размера List backed исходным массивом**. `set` меняет соответствующую ячейку исходного массива, а изменение массива видно в List. `add`/`remove`, изменяющие размер, бросают `UnsupportedOperationException`. Слова **fixed-size** и **unmodifiable** означают разные свойства.

~~~java
import java.util.*;
String[] source = {"A", "B"};
List<String> view = Arrays.asList(source);
view.set(0, "C");
System.out.println(Arrays.toString(source)); // [C, B]
source[1] = "D";
System.out.println(view); // [C, D]
try { view.add("E"); }
catch (UnsupportedOperationException e) { System.out.println("fixed-size"); }
~~~

`Arrays.asList(new int[]{1, 67})` — **`List<int[]>` размера 1**, а не `List<Integer>` размера 2, потому что `int[]` — один объект для generic varargs. Вызов `.add(67)` на таком списке **не компилируется** (ожидается `int[]`); это другой тип ошибки, чем `UnsupportedOperationException` от `add` на допустимом `List<String>` в runtime.

## 8. List.of, unmodifiable live view и shallow copyOf (UT-24–UT-26)

`List.of(...)` создаёт unmodifiable list, отвергает null и операции `set/add`. `Collections.unmodifiableList(backing)` — **живая read-only обёртка** над mutable backing: через обёртку нельзя менять, но внешние изменения backing доступны для чтения. `List.copyOf(backing)` — **structural snapshot**: внешние изменения структуры backing не появляются в возвращённом списке. API допускает повторное использование уже unmodifiable списка, поэтому утверждение «всегда создаётся новая ссылка» ложно.

~~~java
import java.util.*;
var backing = new ArrayList<>(List.of("A"));
List<String> live = Collections.unmodifiableList(backing);
List<String> snapshot = List.copyOf(backing);
backing.set(0, "B");
System.out.println(live);     // [B]
System.out.println(snapshot); // [A]
try { live.set(0, "X"); }
catch (UnsupportedOperationException e) { System.out.println("read-only"); }
~~~

**Shallow:** `List.copyOf` фиксирует ссылки на элементы, а не копирует их состояние. Если элемент mutable, состояние может измениться после получения списка:

~~~java
var mutable = new StringBuilder("A");
List<StringBuilder> snap = List.copyOf(List.of(mutable));
mutable.append("67");
System.out.println(snap.get(0)); // A67
~~~

Unmodifiable не означает deeply immutable. Это принцип владения ссылками, разобранный в [CORE-IMMUTABLE](immutability.md). `List.copyOf` и `List.of` не допускают null элементов.

## 9. P1: расширение API без приписывания непроведённых проверок (UT-30–UT-36)

**UT-30.** `Arrays.parallelSort` и `parallelPrefix` используют параллельные варианты обработки; **не гарантируют ускорение**, а поведение accumulator и эффекты конкуренции требуют отдельного исследования. Микробенчмарк/JMM stress NOT RUN.

**UT-31.** `Arrays.fill` записывает одинаковую ссылку в ячейки `Object[]`, если передать один mutable object; `Arrays.setAll` вызывает generator для индекса, а `parallelSetAll` может вычислять элементы параллельно. Это не обещает deep-copy или потокобезопасность генератора.

**UT-32.** `Objects.requireNonNullElse` выбирает default value, `requireNonNullElseGet` — ленивый supplier запасного значения; default должен быть non-null. Это не перегрузки, формирующие текст `NullPointerException`; отдельные вызовы не исполнялись.

**UT-33.** `Objects.checkIndex/checkFromToIndex/checkFromIndexSize` имеют `long`-варианты. Проверка суммы и границ проводится согласно API без необходимости вручную делать потенциально переполняющееся сложение; точные сообщения исключений или дополнительные крайние длинные диапазоны NOT RUN.

**UT-34.** `Comparator.comparingDouble` использует семантику `Double.compare`: NaN и signed zero требуют явного решения. Locale-sensitive collation для естественного языка — отдельная тема `CORE-I18N`, здесь не проверена.

**UT-35.** Если comparator нарушает транзитивность/другие требования порядка, сортировка может дать некорректное поведение; `Arrays.sort(Object[])` в некоторых случаях *может* бросить `IllegalArgumentException`, но это **не гарантированный детектор** каждой ошибки comparator. Не использовать исключение как валидатор контракта.

**UT-36.** `Arrays.copyOf` и `copyOfRange` копируют **внешний массив**, но сохраняют ссылочные элементы (shallow). Подробные примеры уже опубликованы в [CORE-ARRAYS](arrays.md); дополнительный большой код здесь не нужен.

## 10. Ошибки Java-компиляции ≠ runtime failure

| Ситуация | Этап и механизм |
|---|---|
| `Arrays.deepEquals(int[],int[])` | Не существует такой перегрузки: `javac`-negative |
| `Arrays.binarySearch(int[],String)` | Неверный тип ключа: `javac`-negative |
| `Objects.checkIndex(1,"3")` | Неверный тип length: `javac`-negative |
| `Comparator<String> c = (a,b) -> a.length() < b.length()` | Lambda возвращает boolean вместо int: `javac`-negative |
| `Arrays.asList(new int[]{1,67}).add(67)` | `List<int[]>` не принимает int: `javac`-negative |
| `Comparator.nullsFirst("A")`, `Arrays.sort(List.of(2,1))`, `requireNonNull("X",()->67)` | Неверные сигнатуры/типы: `javac`-negative |
| `Objects.requireNonNull(null)`, неверный `checkIndex`, `List.of(null)` | Корректно компилируются; в runtime исключение |
| `Arrays.asList(new String[]{"A"}).add("B")` | Корректно компилируется; `UnsupportedOperationException` в runtime |
| `Arrays.binarySearch` на неотсортированном массиве | Корректно компилируется; API не определяет результат |

В [G1 `7](../../docs/plans/CORE-REGEX-UTIL.md) находятся **8 изолированных UT_N01–UT_N08** с проверенными содержательными категориями `compiler.err.*`. Исследовательская роль и отдельно роль 03 запускали их на OpenJDK21.0.12.1, **но не стандартный project CI**, и роль 02 здесь не заявляет отдельного прогона. Диагностические ключи — наблюдения конкретного javac, не вечная строка спецификации.

## 11. Адресная матрица 29 P0 / 7 P1 / 5 P2

**UT01–UT38 stdout** — помеченные наблюдения исторически выполненного [G1 `6.2](../../docs/plans/CORE-REGEX-UTIL.md), не 38 JUnit-тестов. `` ниже обозначает **раздел учебной статьи**, а не номер теста.

| ID | Где дан механизм / граница |
|---|---|
| UT-01 | `1: Objects.equals/deepEquals, null и массив |
| UT-02 | `1: hashCode vs hash(varargs) |
| UT-03 | `1: Objects.toString fallback |
| UT-04 | `2: requireNonNull сообщение |
| UT-05 | `2: Supplier<String> ленив |
| UT-06 | `2: checkIndex |
| UT-07 | `2: checkFromToIndex vs checkFromIndexSize |
| UT-08 | `3: comparing/thenComparingInt |
| UT-09 | `3: reversed всей композиции |
| UT-10 | `3: nullsFirst/nullsLast |
| UT-11 | `3: reverse и null-order |
| UT-12 | `3: null key в extractor |
| UT-13 | `4: comparator не согласован с equals |
| UT-14 | `3–4: Comparator.nullsFirst(null) |
| UT-15 | `5: Arrays.equals примитивных |
| UT-16 | `1,5: Arrays.deepEquals вложенных |
| UT-17 | `5: mismatch и индекс |
| UT-18 | `5: compare лексикографический |
| UT-19 | `6: sort примитивов |
| UT-20 | `6: binarySearch, insertion point |
| UT-21 | `7: backed Arrays.asList aliasing |
| UT-22 | `7: fixed-size/UOE |
| UT-23 | `7,10: int[] varargs → List<int[]> |
| UT-24 | `8: List.of null/unmodifiable |
| UT-25 | `8: unmodifiable live vs copyOf snapshot |
| UT-26 | `8: shallow copyOf и mutable item |
| UT-27 | `4: HashSet vs TreeSet |
| UT-28 | `6: sort(T[],Comparator) |
| UT-29 | `6: object stable sort API vs primitive |
| UT-30 | `9 P1: parallelSort/parallelPrefix NOT RUN |
| UT-31 | `9 P1: fill/setAll aliasing |
| UT-32 | `9 P1: requireNonNullElse/ElseGet |
| UT-33 | `9 P1: check* long overloads |
| UT-34 | `9 P1: comparingDouble, NaN/signed zero/locale |
| UT-35 | `9 P1: comparator violations и conditional IAE |
| UT-36 | `9 P1 + [CORE-ARRAYS](arrays.md): shallow copyOf |
| UT-37 | P2: parallel sorting JMM/performance **NOT RUN** |
| UT-38 | P2: JDK сортировочные алгоритмы/vendor optimizations **NOT RUN** |
| UT-39 | P2: полный `COL-SORTED` **NOT RUN** |
| UT-40 | P2: `CORE-I18N`/Collator/locale sorting **NOT RUN** |
| UT-41 | P2: `CORE-SERIAL`/deep copy циклических графов **NOT RUN** |

## 12. Первоисточники, реальные вопросы C и границы доказанного

**A — спецификация и API:** [Objects](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html), [Comparator](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html), [Arrays](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html), [Collections](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html), [List](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html), [TreeSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeSet.html). Нормативные гарантии отделены от данных конкретного Java21 запуска и от бизнес-политик сортировки.

**C — реально существующие интервью-формулировки:** [Q-HRUDAY](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md) содержит вопросы `Arrays.sort/binarySearch/asList` и `Comparator/Comparable`; [Q-CHERNYSHOV](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/master/eng/README.md) — вопросы про `Collections.unmodifiableList` и естественный порядок. Эти банки подтверждают **общие направления вопросов**, а не точные UT-02/UT-07/UT-14 и не частоту на реальных собеседованиях. Для `Objects.deepEquals`, `Comparator.nullsFirst(null)` и `Arrays.mismatch` точные независимые C-вопросы не подтверждены (**C GAP**): в статье они есть ради корректного объяснения API.

**Положительная историческая G1-проверка:** Debian OpenJDK 21.0.12.1, полный `UtilProbe.java` и stdout UT01–UT38 (38 строк), SHA-256 `57cff41cdf848efe2a0092c3fe938efd91e1a3f537adc1213327609253eddbb7`. Совместно с RegexProbe — 33+38=**71 stdout-строка из двух JVM, не JUnit-тесты**. Роль 03 независимо подтвердила полные source SHA, выводы и три fresh-temp повтора [в Issue #196](https://github.com/Melchiorius/backend-interview-labs/issues/196#issuecomment-7098732530), а также 14 отдельных javac-negatives (6 RG+8 UT). **Это локальный G1 evidence, не committed G2 CI**.

**NOT RUN этой теоретической публикацией:** новые project runners/CI до фактической интеграции 03, отдельная компиляция всех inline-фрагментов статьи, 8 UT-negatives в штатном CI, другие JDK/OS/vendors, timing/performance и stress, parallel sorting/JMM, гипотетические comparator failures, DB/SERIAL/I18N и locale-sensitive Collator, альтернативные сортировочные алгоритмы, P2, Obsidian/IntelliJ GUI, HTTP/anchors. Реестры [COVERAGE](../../docs/COVERAGE.md) и [CURRICULUM](../../docs/CURRICULUM.md) меняет G6 после G5, а не автор G2.
