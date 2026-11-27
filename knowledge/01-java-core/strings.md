# CORE-STRINGS и CORE-BUILDERS — String, Unicode и изменяемые последовательности Java 21

**Область:** Java SE 21, без preview. **Маршрут:** [типы и преобразования](types.md) → [строковые выражения](conversions.md) → эта статья → [массивы](arrays.md) и [равенство](equality.md). Тема состоит из двух самостоятельных curricular ID: **CORE-STRINGS** (TX-01–TX-20) и **CORE-BUILDERS** (BL-01–BL-09), но они разобраны **в одном читательском материале**: чтобы понимать Builder, сначала нужно понимать, что именно сохраняет и измеряет String.

**Сквозной runnable:** [Arrays + Strings/Builders — две Java 21 программы и эталоны](../../examples/core/arrays-strings/README.md) позволяет проверить A01–A24 и S01–S28 по отдельности (52 строки двух запусков, **не 52 теста**). **G1:** [принятый исследовательский план](../../docs/plans/CORE-ARRAYS-STRINGS.md), **TX-01–TX-20 (14 P0/4 P1/2 P2), BL-01–BL-09 (6 P0/2 P1/1 P2)**. Нормативная основа: [JLS Java21 §3.10.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10.5), [§15.18.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.18.1), [String API 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html), [Character](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Character.html), [StringBuilder](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/StringBuilder.html), [StringBuffer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/StringBuffer.html).

**Как запускать примеры.** Класс с методом `main` можно запускать отдельно. Блоки операторов без объявления класса — самостоятельные фрагменты тела метода (например, `main`): не объединяйте их в один метод, поскольку имена переменных могут повторяться. Переменные из предыдущего примера используются только там, где это явно указано.

## 1. String — неизменяемое значение, а не изменяемый массив символов (TX-01, TX-02)

`String` — неизменяемый (*immutable*) и `final` класс. Если есть строка `s`, операция `s.concat("!")` возвращает строковое значение; **не** меняет символы внутри исходного `s`. Не существует аналога `s[0] = 'A'`: `String` не является массивом `char[]`, а его `length()` — **метод**, не поле `length`.

~~~java
class StringIdentity {
    public static void main(String[] args) {
        String original = "Java";
        String result = original.concat(" 21");
        System.out.println(original);          // Java
        System.out.println(result);            // Java 21

        String sameText = new String("Java");
        System.out.println(original == sameText);      // false: другой объект
        System.out.println(original.equals(sameText)); // true: те же UTF-16 units
    }
}
~~~

`==` для ссылок сравнивает **идентичность объектов**, `equals` у String — **содержимое** (последовательность UTF-16 кодовых единиц). Для бизнес-логики «равны ли введённые логины?» нужен согласованный контракт содержимого, не случайное совпадение ссылок. Но и `equals` не выполняет Unicode-нормализацию, locale-collation или case-folding автоматически — границы см. §9.

**Почему immutable важно:** строку допустимо передавать между частями приложения, не опасаясь, что другая функция изменит её через публичный API. Это **не** означает, что любой объект, содержащий ссылку на String, автоматически потокобезопасен или что JVM обязана создать определённое количество байтовых массивов для операций с ней.

Сам по себе `final` запрещает наследование, но не изменение состояния экземпляра. Контракт String обеспечивает изоляцию от изменяемых данных: после конструктора `String(char[])` изменение исходного массива не меняет строку, а `toCharArray()` возвращает новый массив, не открывающий внутреннее состояние. Неизменяемость не требует нового объекта при каждой операции: `s.concat("") == s` гарантировано API, а идентичность результата `s.substring(0, s.length())` не обещана.

~~~java
char[] source = {'a', 'b'};
String s = new String(source);
source[0] = 'x';
char[] copy = s.toCharArray();
copy[0] = 'y';
System.out.println(s);                // ab
System.out.println(s.concat("") == s); // true
System.out.println((s + "") == s);     // false: неконстантное выражение
~~~

## 2. Литералы, константные выражения и intern — различаем контракт от размещения JVM (TX-03)

Строковые литералы и значения строковых **compile-time constant expressions** интернируются. Одинаковые константные строки используют канонический объект String. Для результата обычной динамической конкатенации нельзя подменять это правилом «все одинаковые строки автоматически хранятся в одном объекте».

~~~java
class InternExample {
    public static void main(String[] args) {
        String literal = "java";
        String folded = "ja" + "va";  // compile-time constant expression
        String fresh = new String("java");

        System.out.println(literal == folded);       // true
        System.out.println(literal == fresh);        // false
        System.out.println(literal == fresh.intern()); // true
        System.out.println(literal.equals(fresh));   // true
    }
}
~~~

Константная переменная по [JLS §4.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4) — это `final` переменная примитивного типа или String, инициализированная константным выражением. Одного `final` недостаточно. По JLS §15.18.1 результат неконстантной строковой конкатенации — новый объект в наблюдаемой семантике языка, а не обещание физического выделения памяти.

~~~java
final String constant = "ja";
final String runtime = new String("ja");
String variable = "ja";
System.out.println(constant + "va" == "java"); // true
System.out.println(runtime + "va" == "java");  // false
System.out.println(variable + "va" == "java"); // false
~~~

`intern()` возвращает каноническую ссылку согласно контракту API. Если пул уже содержит строку с равным содержимым, `intern()` возвращает соответствующую каноническую ссылку; иначе добавляет сам объект и возвращает его. Метод не переприсваивает переменную вызывающего кода и не сливает уже существующие объекты. Например, после `fresh.intern()` без присваивания `fresh` из предыдущего примера всё ещё указывает на отдельный объект.

**Не выводите отсюда** точное место расположения string pool в heap, формат представления символов, правила сборки мусора, число промежуточных объектов, HotSpot Compact Strings или алгоритм JIT — всё это другие вопросы, которые нельзя доказывать идентичностью двух литералов. Норма: [JLS §3.10.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10.5), [§15.29](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.29), [String.intern](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#intern()).

## 3. Конкатенация, порядок вычисления и представление null (TX-04)

Оператор `+` в строковом контексте преобразует операнды в текст. Выражение группируется слева (`(a + b) + c`), а операнды вычисляются **слева направо**. Группировка и порядок вычисления — разные правила, а математической ассоциативности для строкового `+` нет. Это влияет на результат:

~~~java
class StringPlus {
    public static void main(String[] args) {
        System.out.println(1 + 2 + "x");   // 3x: сначала int + int
        System.out.println("x" + 1 + 2);   // x12: строка, затем строка
        String missing = null;
        System.out.println("user=" + missing); // user=null
    }
}
~~~

Строковый текст `"null"` здесь — результат string conversion, **не** свидетельство наличия объекта String у ссылки `missing`. При сложной конкатенации не полагайтесь на конкретный сгенерированный байткод, обязательную реализацию через Builder или JIT-оптимизацию. Если в цикле вы собираете длинную изменяемую строку, чаще понятнее явный Builder (§10 и §15), но **без** обобщённого обещания ускорения.

По [JLS §5.1.11](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.11) строковое преобразование в операторе `+` даёт текст `"null"` как для ссылки `null`, так и для случая, когда `toString()` объекта возвращает ссылку `null`. Библиотечный `String.valueOf(Object)` при аргументе-ссылке, отличном от `null`, возвращает непосредственно результат `toString()` этого объекта: если метод вернул ссылку `null`, результатом `valueOf` тоже будет ссылка `null`, а не текст `"null"`. Массив `char[]` в операторе `+` преобразуется как объект через унаследованный `Object.toString()`: это не адрес в памяти и суффикс такого представления не фиксирован. Перегрузка `valueOf(char[])`, напротив, читает символы массива; выбор перегрузки зависит от статического типа аргумента.

~~~java
Object strange = new Object() {
    @Override public String toString() { return null; }
};
char[] digits = {'6', '7'};
System.out.println(("" + strange).equals("null")); // true
System.out.println(String.valueOf(strange) == null); // true
System.out.println(("" + digits).equals(digits.toString())); // true
System.out.println(String.valueOf(digits)); // 67
System.out.println(String.valueOf((Object) digits).equals(digits.toString())); // true
~~~

## 4. UTF-16 code units, code points и индексы String (TX-05, TX-06, TX-07, TX-13)

Критическая ловушка: **длина Java String — количество 16-битных кодовых единиц UTF-16**, а не количество видимых человеку символов. Символ вне Basic Multilingual Plane (например, U+1F600 😀) представлен **двумя `char`** — high и low surrogate. Каждая кодовая точка Unicode (*code point*) обозначается числом типа `int`, и потому не равна одному `char` для всех Unicode-символов.

~~~java
class UnicodeUnits {
    public static void main(String[] args) {
        String text = "A\uD83D\uDE00B";     // A😀B
        System.out.println(text.length());                // 4 UTF-16 units
        System.out.println(text.codePointCount(0, text.length())); // 3 code points
        System.out.println(Character.isHighSurrogate(text.charAt(1))); // true
        System.out.println(text.codePointAt(1) == 0x1F600); // true
        System.out.println(text.offsetByCodePoints(0, 2));  // 3 (индекс B)
        System.out.println(text.substring(1, 3));          // 😀
        System.out.println(text.codePoints().count());      // 3
    }
}
~~~

Строго различайте **пространства индексов**:

- `charAt(int i)` получает **одну UTF-16 code unit**, возвращает `char` (высокий суррогат в примере — не вся 😀).
- `codePointAt(int i)` принимает **UTF-16 индекс** и возвращает `int` code point, если high surrogate по этому индексу непосредственно сопровождается low surrogate. Иначе возвращает числовое значение одной кодовой единицы (в том числе непарного суррогата, который не является Unicode scalar value). `codePointBefore(i)` смотрит назад: объединяет пару, если непосредственно перед i low surrogate и перед ним high surrogate.
- `codePointCount(beginIndex,endIndex)` считает Unicode code points в **диапазоне UTF-16 индексов**; `offsetByCodePoints` преобразует смещение в code points в новый UTF-16 индекс.
- `substring(begin,end)` использует **UTF-16 индексы**, причём `end` **исключается**. Если разрезать строку посередине суррогатной пары, можно получить строку с одиноким суррогатом; `substring` не выполняет семантическую сегментацию Unicode.
- `chars()` выдаёт числовые значения отдельных UTF-16 единиц, `codePoints()` объединяет корректные пары и выдаёт значения непарных единиц отдельно. Оба возвращают `IntStream`, а не `Stream<Character>`; переход к примитивным потокам разобран в [CORE-STREAMS](streams.md).

~~~java
class SubstringRange {
    public static void main(String[] args) {
        String s = "Java";
        System.out.println(s.substring(1, 3)); // av
        try {
            s.substring(0, 5);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("out of range");
        }
    }
}
~~~

**Даже code point не обязательно равен «видимому символу»**: буква с комбинируемым знаком, флаг или emoji с Zero Width Joiner могут содержать несколько code points. Это отдельная тема Unicode grapheme clusters, не решаемая вызовом `codePointCount`. Обработка графем/нормализация помечены как P2 (TX-20); здесь не проводится полноценный i18n-тест.

Диапазон может начинаться или заканчиваться внутри пары. Следующий фрагмент добавьте в конец метода `UnicodeUnits.main`, перед его закрывающей фигурной скобкой: используется переменная `text = "A\uD83D\uDE00B"` из этого метода, а не из примера `SubstringRange`.

~~~java
System.out.println(text.codePointAt(2) == 0xDE00); // true: low surrogate
System.out.println(text.codePointBefore(3) == 0x1F600); // true
System.out.println(text.codePointCount(1, 2)); // 1
System.out.println(text.codePointCount(2, 3)); // 1
System.out.println(text.offsetByCodePoints(4, -2)); // 1
System.out.println(text.offsetByCodePoints(2, 1)); // 3
System.out.println(text.chars().count()); // 4
System.out.println(new String(new int[]{65, 0x1F600, 66}, 0, 3).equals(text)); // true
~~~

Конструктор принимает массив code points, начальное смещение и количество элементов массива. При недопустимом code point он выбрасывает IllegalArgumentException. Для ошибки диапазона substring API гарантирует IndexOutOfBoundsException, а не конкретный подкласс, наблюдаемый на одном JDK.

## 5. trim, strip, isBlank, isEmpty — проверяем разные условия (TX-08)

`trim()` удаляет начальные и конечные символы с кодовыми значениями **не больше U+0020** согласно контракту метода. `strip()` применяет Unicode-ориентированную классификацию пробельных code points через `Character.isWhitespace`. `isBlank()` проверяет, пуста ли строка или состоит только из таких пробельных code points; `isEmpty()` проверяет лишь нулевую **длину**.

~~~java
class Whitespace {
    public static void main(String[] args) {
        String s = "\u2003x\u2003";      // EM SPACE вокруг x
        System.out.println(s.trim().equals(s));   // true
        System.out.println(s.strip());            // x
        System.out.println("\u2003".isBlank());    // true
        System.out.println("\u2003".isEmpty());    // false
    }
}
~~~

Нельзя писать «`strip` удаляет вообще все Unicode whitespace во всех возможных стандартах»: формальный предикат именно `Character.isWhitespace` на версии Java 21. Например, NBSP (U+00A0) входит в `Character.isSpaceChar`, но не в `Character.isWhitespace`; поэтому `"\u00A0".isBlank()` ложно. Пустая строка даёт true. NUL (U+0000) не является whitespace, но его значение не больше U+0020: `"\0x\0".trim()` даёт "x", а `"\0x\0".strip().length()` равно 3. [String.strip/isBlank](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html), [Character.isWhitespace](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Character.html#isWhitespace(int)).

## 6. split — это regex; limit=0 теряет конечные пустые поля (TX-09)

`String.split(regex)` принимает **регулярное выражение**, а не буквальный разделитель. Точка без экранирования в regex по умолчанию совпадает с любым символом, кроме разделителей строк (line terminators, например `\n`). Флаг `Pattern.DOTALL` или встроенный `(?s)` включает и их. Это не то же самое, что буквальная точка. Для буквальной точки сам regex должен содержать **один** обратный слеш перед точкой: `\.`. В исходном коде Java этот слеш нужно экранировать **ещё раз** в строковом литерале: `"\\."`. Сначала Java преобразует литерал в строку `\.`, затем regex-движок воспринимает `\.` как буквальную точку.

~~~java
import java.util.Arrays;

class RegexSplit {
    public static void main(String[] args) {
        System.out.println(Arrays.toString("a.b".split("\\."))); // [a, b]
        System.out.println(Arrays.toString("a,b,".split(",")));   // [a, b]
        System.out.println(Arrays.toString("a,b,".split(",", -1))); // [a, b, ]
        System.out.println("a.b".split("\\.").length);           // 2
    }
}
~~~

Правила limit действуют так: при положительном значении regex применяется не более limit−1 раз, остаток становится последним элементом; при нуле число разрезов не ограничено, но хвостовые пустые строки удаляются; при любом отрицательном значении хвост сохраняется. Совпадение ненулевой длины в самом начале даёт первый пустой элемент, совпадение нулевой длины в начале — нет. Если совпадений нет, возвращается массив из одного исходного значения, даже если оно пустое.

~~~java
String[] defaultDot = "a\nb".split(".");
System.out.println(defaultDot.length); // 2: "", "\n"
System.out.println(defaultDot[0].isEmpty() && defaultDot[1].equals("\n")); // true
System.out.println("a\nb".split("(?s).").length); // 0
System.out.println(java.util.Arrays.toString(",a,".split(",", -2))); // [, a, ]
System.out.println(java.util.Arrays.toString(",a,".split(",", 0))); // [, a]
System.out.println(java.util.Arrays.toString("a,b,c".split(",", 2))); // [a, b,c]
System.out.println("a,b".split(",", 1)[0]); // a,b
System.out.println("ab".split("(?=a)")[0]); // ab
System.out.println("".split(",").length); // 1: единственный элемент ""
System.out.println(",".split(",").length); // 0: нет элементов
~~~

Последние два массива при Arrays.toString выглядят одинаково (`[]`), поэтому здесь проверяется именно длина. Для literal-подстроки, которую пользователь ввёл сам, можно использовать `Pattern.quote(delimiter)`, иначе спецсимволы изменят смысл регулярного выражения. Нормы: [String.split](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#split(java.lang.String,int)), [Pattern](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html).

## 7. replace, replaceAll и join — разные API (TX-10, TX-11)

`replace(CharSequence,CharSequence)` заменяет **буквальные последовательности**. `replaceAll(regex,replacement)` использует **регулярное выражение** и синтаксис строки замены (включая специальный символ `$` и обратную косую черту). Для буквальной строки замены используйте `Matcher.quoteReplacement`.

~~~java
class Replacement {
    public static void main(String[] args) {
        String s = "a.b";
        System.out.println(s.replace(".", "|"));         // a|b
        System.out.println(s.replaceAll("\\.", "|"));     // a|b
        System.out.println(s.replaceAll(".", "|"));       // |||
        System.out.println(String.join(",", "one", null)); // one,null
    }
}
~~~

Буквальная замена идёт слева направо без перекрытия совпадений: `"aaa".replace("aa", "b")` возвращает "ba". В regex-замене `"x".replaceAll("(x)", "$1$1")` возвращает "xx" (группа 1 подставлена дважды). Pattern.quote защищает шаблон, Matcher.quoteReplacement защищает замену — это разные грамматики:

~~~java
System.out.println(".".replaceAll(java.util.regex.Pattern.quote("."),
        java.util.regex.Matcher.quoteReplacement("$1"))); // $1
~~~

Неверный шаблон "[" вызывает PatternSyntaxException. При наличии совпадения некорректная замена "$" вызывает IllegalArgumentException; ссылка "$2" на отсутствующую числовую группу — IndexOutOfBoundsException, ссылка "${missing}" на отсутствующую именованную — IllegalArgumentException. См. [Matcher](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Matcher.html) и [CORE-REGEX](regex.md).

У join две перегрузки: `join(CharSequence delimiter, CharSequence... elements)` и `join(CharSequence delimiter, Iterable<? extends CharSequence> elements)`. Для обеих null-разделитель и null-контейнер вызывают NPE, а отдельный null-элемент превращается в текст "null". Вызов `String.join(",", null)` не компилируется: типы массива и Iterable несравнимы по специфичности. Приведение выбирает перегрузку и одновременно уточняет смысл аргумента:

~~~java
System.out.println(String.join(",", "a", (CharSequence) null)); // a,null
System.out.println(String.join(",", java.util.Arrays.asList("a", null))); // a,null
// String.join(",", (CharSequence[]) null); // NPE: контейнер
// String.join(",", (Iterable<CharSequence>) null); // NPE: контейнер
~~~

Контракт второй перегрузки: [String.join(Iterable)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#join(java.lang.CharSequence,java.lang.Iterable)). [String.join](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#join(java.lang.CharSequence,java.lang.CharSequence...)).

## 8. Перегрузки String.valueOf(null): неочевидный выбор и NPE (TX-12)

Это не про «String.valueOf умеет превращать любой null в "null"». Java сначала выбирает **перегрузку по статическому типу аргумента**. Среди применимых к literal `null` перегрузок `valueOf(char[])` более специфична, чем `valueOf(Object)`; поэтому некастованный вызов приводит к **NullPointerException** при обработке null-массива. Явно приведённый к Object null ведёт к тексту `"null"`.

~~~java
class NullOverload {
    public static void main(String[] args) {
        System.out.println(String.valueOf((Object) null)); // null
        try {
            System.out.println(String.valueOf(null));       // char[] overload
        } catch (NullPointerException e) {
            System.out.println("NPE from char[] overload");
        }
        System.out.println(String.valueOf((String) null));  // null: Object overload
    }
}
~~~

**Нюанс:** у String нет специальной перегрузки `valueOf(String)`; для `(String)null` здесь применима `valueOf(Object)`. При передачах nullable-данных делайте целевой тип явно, вместо предположения «null всегда одинаков». Это **runtime failure после успешного overload resolution**, не compile-negative. Нормы: [String.valueOf(Object)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#valueOf(java.lang.Object)), [valueOf(char[])](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#valueOf(char%5B%5D)), [JLS §15.12](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12).

## 9. String switch, сравнение, форматирование и Unicode normalization (TX-15–TX-18 · P1)

**Unicode normalization:** два текста могут выглядеть одинаково, но состоять из **разных последовательностей code points**. `String.equals` сравнит их буквально, без Unicode canonical equivalence:

~~~java
import java.text.Normalizer;

class Normalization {
    public static void main(String[] args) {
        String composed = "\u00E9";  // é
        String decomposed = "e\u0301";
        System.out.println(composed.equals(decomposed)); // false
        System.out.println(Normalizer.normalize(
            decomposed, Normalizer.Form.NFC).equals(composed)); // true
    }
}
~~~

Это иллюстрация **NFC-нормализации конкретной пары**, не полный дизайн Unicode locale-aware поиска/сортировки или графемных границ. Если бизнес-логике важен язык пользователя, нужны отдельные требования и [Collator](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/Collator.html), а не `String.compareTo` по умолчанию.

**String switch:** сравнение case-строк проводится по значению, не идентичности String. В Java 21 можно явно обработать `case null`; если `case null` отсутствует, даже наличие `default` не предотвращает NullPointerException для null-селектора. Строковые константные метки чувствительны к регистру и должны быть константными выражениями (например, `case new String("java")` не компилируется). См. [JLS §14.11](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11). В коде ниже нет preview:

~~~java
class StringSwitch {
    static String describe(String name) {
        return switch (name) {
            case null -> "missing";
            case "java" -> "language";
            default -> "other";
        };
    }
    public static void main(String[] args) {
        System.out.println(describe(new String("java"))); // language
        System.out.println(describe(null));               // missing
    }
}
~~~

**String.compareTo** задаёт лексикографическое сравнение UTF-16 единиц, а не порядок букв по алфавиту или числовой порядок Unicode code points. Например, U+1F600 начинается с D83D, который меньше E000, поэтому сравнение строк даёт отрицательный результат, хотя 0x1F600 > 0xE000. compareTo возвращает 0 ровно для равных по equals строк, но ненулевой результат не обязан быть ±1 (сравнение "a" и "z" даёт −25). `equalsIgnoreCase` сравнивает строки по правилу API без учёта регистра, не разворачивая одну кодовую точку в многосимвольную строку. Это не преобразование обеих строк через `toUpperCase(Locale.ROOT)` и не сравнение с учётом локали: `"ß".equalsIgnoreCase("SS")` ложно, хотя `"ß".toUpperCase(Locale.ROOT)` даёт `"SS"`. Для языкового порядка применяют `Collator` с выбранной локалью.

**String.format/formatted** используют локаль категории FORMAT по умолчанию, которая может отличаться от DISPLAY. `formatted` эквивалентен `String.format(this, args)` и не принимает отдельный аргумент Locale. Для явного выбора используйте `String.format(Locale.ROOT, ...)` или нужную локаль. ROOT обеспечивает выбор нейтральной локали для конкретного форматирования, а не универсальный формат сериализации.

~~~java
java.util.Locale old = java.util.Locale.getDefault(java.util.Locale.Category.FORMAT);
java.util.Locale oldDisplay = java.util.Locale.getDefault(java.util.Locale.Category.DISPLAY);
try {
    java.util.Locale.setDefault(java.util.Locale.Category.DISPLAY, java.util.Locale.US);
    java.util.Locale.setDefault(java.util.Locale.Category.FORMAT, java.util.Locale.GERMANY);
    System.out.println(String.format("%.1f", 1.5)); // 1,5
    System.out.println("%.1f".formatted(1.5)); // 1,5
    System.out.println(String.format(java.util.Locale.ROOT, "%.1f", 1.5)); // 1.5
} finally {
    java.util.Locale.setDefault(java.util.Locale.Category.FORMAT, old);
    java.util.Locale.setDefault(java.util.Locale.Category.DISPLAY, oldDisplay);
}
~~~

Этот пример изменяет глобальные настройки процесса: выполняйте его изолированно, не в параллельном тесте приложения. [Locale](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Locale.html).

~~~java
import java.util.Locale;

class Formatting {
    public static void main(String[] args) {
        System.out.println(String.format(Locale.ROOT, "%.1f", 1.5)); // 1.5
        System.out.println("a".compareTo("b") < 0);                  // true
    }
}
~~~

Отдельные P2 темы: сложная Unicode grapheme segmentation, нестандартные форматы/кодировки и JVM internals/Compact Strings/GC. Не превращайте знание одного API-вызова в заявленное покрытие этих тем.

---

## Часть II. CORE-BUILDERS — изменяемые строки и границы синхронизации (BL-01–BL-09)

### 10. Зачем StringBuilder: append и insert меняют один объект (BL-01)

В отличие от String, `StringBuilder` — **изменяемая последовательность UTF-16 кодовых единиц**, реализующая CharSequence. `append`, `insert`, `delete` и `setCharAt` меняют текущий Builder, а `toString()` предоставляет String-снимок значения на момент вызова. Нельзя писать `StringBuilder s = "abc"`: строковый литерал имеет тип String, не StringBuilder.

~~~java
class MutableText {
    public static void main(String[] args) {
        StringBuilder b = new StringBuilder("ab");
        String before = b.toString();
        b.append("cd").insert(1, "-");     // меняем b
        System.out.println(before);        // ab — прежний String не изменён
        System.out.println(b);             // a-bcd
        b.delete(1, 2);
        System.out.println(b);             // abcd
    }
}
~~~

Возвращаемый `toString()` нельзя рассматривать как «доступ к внутреннему изменяемому buffer»; он соответствует состоянию на момент вызова. То, сколько массивов или объектов выделит конкретная JVM при таком преобразовании, не следует из этого учебного примера.

Методы append, insert, delete и replace возвращают ссылку на тот же изменяемый объект (fluent API). Методы substring и subSequence, как toString, возвращают неизменяемый String-снимок выбранного содержимого. В отличие от снимков, `chars()` и `codePoints()` создают потоки данных (Java Stream, не потоки выполнения), которые связываются с содержимым изменяемой последовательности при начале терминальной операции (late binding). Согласно API результат не определён, если последовательность меняют во время чтения её элементов потоком данных. В следующем примере изменение происходит последовательно, до начала терминальной операции `count()`, а не одновременно с чтением.

~~~java
StringBuilder b = new StringBuilder("ab");
String saved = b.substring(0, 1);
CharSequence seq = b.subSequence(0, 1);
var units = b.chars();
System.out.println(b.append('c') == b); // true
System.out.println(saved.equals("a") && seq.toString().equals("a")); // true
System.out.println(units.count()); // 3: обход начался после append
~~~

### 11. StringBuilder vs StringBuffer: synchronized методы ≠ транзакция (BL-02, BL-09)

Оба класса предоставляют изменяемую последовательность и близкие методы, но контракты доступа различаются:

| Класс | Синхронизация | Что важно при проектировании |
|---|---|---|
| `StringBuilder` | **Нет** общей гарантии синхронизации; не предназначен для конкурентной мутации без внешней защиты | Обычно локальное накопление в рамках одного вычисления |
| `StringBuffer` | Методы синхронизируются там, где необходимо, согласно API | **Несколько последовательных методов не образуют автоматически атомарную операцию**, требуется общий lock для составного инварианта |

Типичный некорректный check-then-act:

~~~java
class BufferCompoundAction {
    static void appendOnce(StringBuffer buffer, String suffix) {
        // При нескольких вызывающих потоках между проверкой и append
        // другой поток вправе изменить последовательность:
        if (buffer.indexOf(suffix) < 0) {
            buffer.append(suffix);
        }
    }

    static void appendOnceProtected(StringBuffer buffer, String suffix) {
        synchronized (buffer) {             // один lock для двух шагов
            if (buffer.indexOf(suffix) < 0) {
                buffer.append(suffix);
            }
        }
    }
}
~~~

Наличие синхронизации у отдельных методов Buffer **не означает**, что пара `indexOf → append` атомарна. Исправленный вариант корректен **только если все участники, влияющие на этот инвариант, соблюдают согласованную дисциплину блокировки**. Здесь не выполняется конкурентный stress-test, не обещается обязательный race symptom и не делается вывод, что Buffer быстрее/медленнее Builder во всех нагрузках. `StringBuffer` гарантирует, что отдельные операции над **одним экземпляром** выполняются в некотором последовательном порядке, согласованном с порядком вызовов каждого потока выполнения. При `append` или `insert` монитор получателя не обеспечивает стабильность другого изменяемого объекта, из которого читаются символы. Вызывающий код обязан обеспечить стабильность источника в течение всего чтения: например, получить неизменяемый снимок под согласованной защитой источника и передать его либо ограничить доступ к источнику одним потоком выполнения. Это не гарантирует транзакцию или отсутствие deadlock при захвате нескольких мониторов. Наглядная последовательная модель (не запуск двух потоков):

~~~java
StringBuffer b = new StringBuffer();
boolean first = b.indexOf("x") < 0;
boolean second = b.indexOf("x") < 0;
if (first) b.append("x");
if (second) b.append("x");
System.out.println(b); // xx: две проверки до двух добавлений
~~~

Дальнейшее изучение — будущая тема CON-RACE (составная операция check-then-act и общий монитор). Здесь дана минимальная модель; тема CON-RACE ещё не раскрывается, проверка JMM не заявлена.

### 12. length, capacity, ensureCapacity и setLength (BL-03, BL-06)

У Builder/Buffer два разных числовых свойства. **`length()`** — количество фактически хранимых UTF-16 code units; **`capacity()`** — общее число UTF-16 кодовых единиц, которое можно вместить до расширения хранилища, включая уже занятые. Разность `capacity() - length()` показывает, сколько дополнительных единиц помещается в текущую ёмкость. Вызов `ensureCapacity(n)` может увеличить ёмкость, **не добавляя видимые символы**. Вызов `setLength(n)` меняет логическую длину; при увеличении новая область заполнена кодовой единицей U+0000, а не пробелами.

~~~java
class CapacityAndLength {
    public static void main(String[] args) {
        StringBuilder b = new StringBuilder("ab");
        b.ensureCapacity(67);
        System.out.println(b.length());           // 2
        System.out.println(b.capacity() >= 67);   // true
        b.setLength(4);
        System.out.println(b.length());           // 4
        System.out.println(b.charAt(2) == '\0');   // true
        b.setLength(1);
        System.out.println(b.toString());         // a
    }
}
~~~

Не выводите из `capacity` точное число байтов, размер объекта, способ хранения String или гарантии JVM allocator: это API-показатель количества UTF-16 кодовых единиц. И `setLength` может обрезать суррогатную пару, поэтому при обработке Unicode важно помнить §4.

Начальная capacity у обоих классов — 16 без аргументов, длина входной строки + 16 для конструктора String, либо явно переданное неотрицательное значение. Отрицательная ёмкость конструктора вызывает NegativeArraySizeException. Если minimumCapacity больше текущей ёмкости, ensureCapacity увеличивает её до max(minimumCapacity, 2 × oldCapacity + 2) в обычном случае представимого размера и успешного выделения памяти. Неположительный аргумент ничего не меняет; trimToSize может уменьшить хранилище, точный итог не гарантирован. Например, capacity 3 после ensureCapacity(4) становится 8, а length остаётся 0. После уменьшения length и последующего увеличения удалённые символы не восстанавливаются: `new StringBuilder("abc")` после setLength(1) и setLength(3) содержит 'a', U+0000, U+0000. Отрицательный setLength вызывает IndexOutOfBoundsException.

### 13. equals, contentEquals и выбор способа сравнения (BL-04)

`StringBuilder` не задаёт содержательное `equals` наподобие String: два Builder с одинаковыми текстами — разные объекты и при `equals` сравниваются по идентичности. Правильный вопрос — **что именно должно быть равно**: два объекта-накопителя или их текстовые значения?

~~~java
class BuilderEquality {
    public static void main(String[] args) {
        StringBuilder a = new StringBuilder("same");
        StringBuilder b = new StringBuilder("same");
        System.out.println(a.equals(b));                // false
        System.out.println(a.toString().equals(b.toString())); // true
        System.out.println("same".contentEquals(a));    // true
    }
}
~~~

`String.contentEquals(CharSequence)` сравнивает содержимое, а не ссылки.

Builder и Buffer реализуют Comparable с естественным порядком, несовместимым с equals: два различных накопителя с одинаковым текстом имеют compareTo == 0, но equals == false. HashSet использует equals/hashCode, TreeSet — естественный порядок (при отсутствии явного компаратора).

~~~java
var a = new StringBuilder("a");
var b = new StringBuilder("a");
System.out.println(a.compareTo(b)); // 0
System.out.println(new java.util.HashSet<>(java.util.List.of(a, b)).size()); // 2
System.out.println(new java.util.TreeSet<>(java.util.List.of(a, b)).size()); // 1
~~~

Не меняйте состояние, влияющее на порядок элемента уже в TreeSet: структура не перестроится автоматически. Для содержательного ключа обычно лучше неизменяемый String-снимок. По API String.contentEquals(StringBuffer) синхронизируется на переданном Buffer; перегрузка CharSequence также делает это, когда фактический объект — StringBuffer.

Если объект в другом потоке одновременно меняется, нельзя выводить из одного вызова `contentEquals` требования к атомарной бизнес-проверке; доступ к mutable объектам должен проектироваться отдельно.

### 14. Перегрузки append и null: String vs char[] (BL-05)

У `append`, как и у `String.valueOf`, **перегрузки важнее общего слова «null»**. Для `append((String) null)` по контракту добавляется текст `"null"`. Для `append((char[]) null)` операция ожидает реальный массив символов и приводит к NPE.

~~~java
class AppendNull {
    public static void main(String[] args) {
        StringBuilder b = new StringBuilder();
        b.append((String) null);
        System.out.println(b); // null — четыре буквы
        try {
            b.append((char[]) null);
        } catch (NullPointerException e) {
            System.out.println("char[] null rejected");
        }
    }
}
~~~

Здесь обе строки **успешно компилируются**; отличие проявляется при выполнении. Вызовы append(null) и insert(0, null) не компилируются: среди применимых методов есть несравнимые типы String и char[] (также применимы другие ссылочные перегрузки). Напротив, new StringBuilder(null) компилируется: String более специфичен, чем CharSequence, и выбранный конструктор выбрасывает NPE. Явные приведения к Object, String, CharSequence или StringBuffer в append дают текст "null". В append(CharSequence, start, end) null моделируется четырьмя буквами "null", поэтому диапазон [1,3) даёт "ul"; append(char[], offset, len) ожидает массив и использует смещение и количество, не конечный индекс.

~~~java
System.out.println(new StringBuilder().append((Object) null)); // null
System.out.println(new StringBuilder().append((CharSequence) null, 1, 3)); // ul
System.out.println(new StringBuilder().append(new char[]{'a','b','c'}, 1, 2)); // bc
~~~

Связанный случай `String.valueOf(null)` разобран в §8.

### 15. delete, reverse, вставка и конкатенация в цикле (BL-07, BL-08)

У delete(start,end) и replace(start,end,str) диапазон полуоткрытый, но end больше длины допустим и ограничивается текущей длиной. start должен быть неотрицательным, не больше length и не больше end; при равных допустимых границах delete ничего не делает, replace вставляет текст. substring требует end не больше length; insert допускает offset == length. Нельзя распространять одно правило проверки индекса на все методы.

~~~java
System.out.println(new StringBuilder("abc").delete(1, 67)); // a
System.out.println(new StringBuilder("abc").replace(1, 67, "x")); // ax
System.out.println(new StringBuilder("abc").insert(3, "x")); // abcx
~~~

reverse сохраняет порядок high/low внутри существующей суррогатной пары и может образовать новую пару из прежних непарных low/high при развороте. deleteCharAt удаляет ровно одну UTF-16 единицу, а не целую code point. appendCodePoint добавляет одну или две единицы по Character.charCount, недопустимый code point вызывает IllegalArgumentException. reverse **не** является Unicode grapheme-aware реверсом пользовательского текста.

~~~java
System.out.println(new StringBuilder("\uDE00\uD83D").reverse().toString()
        .equals("\uD83D\uDE00")); // true: образовалась пара
System.out.println(new StringBuilder("\uD83D\uDE00").deleteCharAt(0).charAt(0) == 0xDE00); // true
System.out.println(new StringBuilder().appendCodePoint(0x1F600).length()); // 2
~~~

~~~java
class BuilderOperations {
    public static void main(String[] args) {
        StringBuilder b = new StringBuilder("abc");
        b.insert(1, "-");          // a-bc
        b.delete(1, 2);            // abc
        b.reverse();               // cba
        System.out.println(b);     // cba

        StringBuilder collected = new StringBuilder();
        for (int n : new int[]{1, 2, 3}) {
            collected.append(n);
        }
        System.out.println(collected.toString()); // 123
    }
}
~~~

Техническое правило: в явном цикле с многократным наращиванием текста Builder часто выражает намерение лучше повторных `s = s + x`. Но **конкретное число аллокаций/CPU/оптимизация JIT и обязательное ускорение** не гарантируются JLS и не измерены здесь. Форматирование с `String.format` тоже выбирается по необходимости читаемости и локали, а не по неподтверждённому microbenchmark.

### 16. Сопоставление строковых ошибок: compile-time и runtime

| Выражение | Результат | Причина |
|---|---|---|
| `String s = new StringBuilder("x");` | **Compile error** | Несовместимые типы |
| `StringBuilder b = "x";` | **Compile error** | String не StringBuilder |
| `"abc".length` | **Compile error** | У String `length()` — метод |
| `new char[]{'x'}.length()` | **Compile error** | У массива `length` — поле |
| `"abc".charAt(3)` | **Runtime** `IndexOutOfBoundsException` (подкласс возможен) | Неверный UTF-16 индекс |
| `String.valueOf(null)` | **Runtime** `NullPointerException` | Выбран overload `char[]` |
| `String.valueOf((Object)null)` | Строка `"null"` | Выбран overload `Object` |
| `new StringBuilder().append((String)null)` | Содержит `"null"` | Контракт String overload |
| `new StringBuilder().append((char[])null)` | **Runtime** `NullPointerException` | Нужен реальный char[] |
| `new StringBuilder().charAt(0)` | **Runtime** `IndexOutOfBoundsException` (подкласс возможен) | Builder пуст |

Все «runtime» строки компилируются и выбрасывают исключение **только при достижении указанной операции**. В G1 §7 отдельно вынесены N01–N24 как **самостоятельные исходники, которые должны не скомпилироваться по ожидаемой причине**, не по случайной отсутствующей скобке.

## Карта CORE-STRINGS — TX-01–TX-20

| ID | Уровень | Конкретное объяснение |
|---|---|---|
| TX-01 | P0 | §1: immutable/final; `concat` не меняет источник |
| TX-02 | P0 | §1: `equals` vs `==` |
| TX-03 | P0 | §2: литерал, compile-time constant, `new String`, `intern` |
| TX-04 | P0 | §3: строки + числа, left associativity, null conversion |
| TX-05 | P0 | §4: UTF-16 units, surrogate pairs |
| TX-06 | P0 | §4: `codePointAt/codePointCount/codePoints/offsetByCodePoints` |
| TX-07 | P0 | §4: `substring` end-exclusive и range exception |
| TX-08 | P0 | §5: `trim/strip/isBlank/isEmpty` |
| TX-09 | P0 | §6: regex `split`, экранирование, limit |
| TX-10 | P0 | §7: literal `replace` vs regex `replaceAll` |
| TX-11 | P0 | §7: `join` и null element |
| TX-12 | P0 | §8: `valueOf(null)` vs `(Object)null`, overload |
| TX-13 | P0 | §4: `charAt` как char и `codePointAt` как int |
| TX-14 | P0 | §§1,16: String не `char[]`/Builder, `length()` метод |
| TX-15 | P1 | §9: visually equal Unicode без normalization |
| TX-16 | P1 | §9: `switch(String)` по значению и case null Java21 |
| TX-17 | P1 | §9: `compareTo` не locale collation |
| TX-18 | P1 | §9: `String.format` и явная `Locale.ROOT` |
| TX-19 | P2 | §2: Compact Strings, pool layout, JIT concat **не исследованы** |
| TX-20 | P2 | §4/9: grapheme clusters/ZWJ, charset и normalization **вне полного охвата** |

## Карта CORE-BUILDERS — BL-01–BL-09

| ID | Уровень | Конкретное объяснение |
|---|---|---|
| BL-01 | P0 | §10: mutable Builder, append/insert |
| BL-02 | P0 | §11: Builder без synchronization; Buffer synchronized методы, не составной инвариант |
| BL-03 | P0 | §12: length/capacity/ensureCapacity |
| BL-04 | P0 | §13: `equals` identity vs `String.contentEquals` |
| BL-05 | P0 | §14: append String null vs char[] null |
| BL-06 | P0 | §12: setLength и U+0000, суррогаты |
| BL-07 | P1 | §15: цикл + vs Builder, JIT perf не обещается |
| BL-08 | P1 | §§10,15: toString/insert/delete/reverse, индексы |
| BL-09 | P2 | §11: конкурентная мутация, внешняя CharSequence, стресс JMM **не выполнен** |

## Источники, воспроизводимость и честные пределы

**Нормативные документы:** [JLS §3.10.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10.5), [§15.18.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.18.1), [§15.12 overload](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12), [String Java21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html), [Character](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Character.html), [StringBuilder](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/StringBuilder.html), [StringBuffer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/StringBuffer.html), [Normalizer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/Normalizer.html). Уровень интервью-вопросов и независимые источники — в [G1-плане](../../docs/plans/CORE-ARRAYS-STRINGS.md) и [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md).

Исторические сведения исходной публикации: в принятом G1 есть **полный `StringsG1.java` на 28 строк stdout S01–S28**, отдельно полный `ArraysG1.java` на 24 строки, вместе **52 наблюдения двух Java-процессов**, а не 52 независимых unit tests. Для воспроизводимости доступен [общий runnable с двумя отдельными Java-программами](../../examples/core/arrays-strings/README.md), включённый из [PR #125](https://github.com/Melchiorius/backend-interview-labs/pull/125) в авторскую ветку. Роль 03 на OpenJDK 21.0.12.1 подтвердила побайтное совпадение Git blobs, `javac --release 21`, два `java` и два `diff -u` (24/24 и 28/28 строк). GitHub Actions исторической интеграции исходной публикации зафиксирован в [PR #124](https://github.com/Melchiorius/backend-interview-labs/pull/124); это **не** запуск всех inline Markdown-примеров. Негативные N01–N24 — отдельные `javac --release 21` compilation units, а не тесты ожидаемых runtime ошибок. Автор 02 независимо повторил на OpenJDK/javac **21.0.12.1** все **24/24** по ожидаемым диагностическим категориям без случайных parser/EOF ошибок; временные исходники не закоммичены и не исполняются в CI. Отдельный локальный положительный smoke подтвердил `String.valueOf(null)` → NPE, `String.valueOf((Object)null)` → текст `null`, `switch` с `case null`, Unicode code point, `join`, `split` и Builder capacity/length. Это **не** полная компиляция всех snippets этой статьи и не полный 28-строчный G1 stdout.

Исторические S27/S28 фиксируют наблюдаемый конкретный подкласс StringIndexOutOfBoundsException на том JDK; переносимый контракт charAt/substring — IndexOutOfBoundsException. S23 показывает последовательный append, не потокобезопасность; S17 — неравенство двух строк, не выполнение NFC; A20 — исключение при arraycopy, не содержимое уже скопированного префикса. Эталонные наблюдения сохраняются. G1 и прежние отчёты относятся к исходной публикации, а не к статусу текущей доработки.

**Границы исторических проверок G1 (не выполнено тогда):** автоматическое извлечение и компиляция всех Java snippets из Markdown; negative N01–N24 в CI; GUI Obsidian/IntelliJ; JDK/JVM/OS помимо заявленного; реальные кодировки/файловый IO/locale integration; grapheme segmentation/ZWJ и сложная Unicode-нормализация; Concurrent Builder/Buffer stress; JMH, HotSpot internals, GC и JIT. Никаких выводов о реальной производительности Builder и thread-safety произвольного составного алгоритма здесь не делается.

**Проверки доработки D-A14b.** В [отчёте автора 02](https://github.com/Melchiorius/backend-interview-labs/issues/330#issuecomment-7207400320) зафиксированы две успешные компиляции на OpenJDK/javac 21.0.12.1 с `--release 21 -encoding UTF-8 -Xlint:all -Werror` и два успешных запуска JVM (код завершения 0). Временный `Probe.java` проверил конечный набор условий: точку в regex по умолчанию, DOTALL и буквальную точку, границы `split` (включая пустой массив и массив с пустой строкой), операции со строками, Builder/Buffer и форматирование с разными категориями локали. Эти проверки не являются исчерпывающим доказательством для всех входов. Во временный `Fences.java` вручную перенесены 14 добавленных блоков и исправленный `SubstringRange` (15 из 32 Java-блоков статьи): без комментариев, с отдельными методами-обёртками и явной инициализацией контекста там, где она нужна. Все 53 строки вывода совпали с ожиданиями. Это не автоматическое извлечение всех блоков Markdown; 17 неизменённых блоков отдельно не запускались.

Отдельно автор проверил четыре отрицательных исходника на уровне языка: `Join`, `Append`, `Insert` отклонены из-за неоднозначности перегрузок, `Label` — из-за требования строковой константы в `case`. У `Join` также было предупреждение; эти проверки выполнялись отдельно, без требования `-Werror`. Их не следует смешивать с 24 историческими отрицательными исходниками G1 или с 24 отклонениями существующих проверок доступа, наследования и вложенных классов в CI.

Автор 02 также трижды выполнил неизменённый runner с явным UTF-8: конфигурация default без переопределения языка и страны, en_US и tr_TR. Во всех случаях оба сравнения эталонов успешны, вывод — 53 строки (24 + 28 + завершающий маркер); в stderr были только уведомления JVM о параметрах окружения. Независимо от этого роль 03 при исходной интеграции D-A14b воспроизвела runner с явными UTF-8 параметрами и измеренными локалями en_US, en_US и tr_TR соответственно: 53 строки / 867 байт совпали с эталоном в каждом запуске, stderr пуст. В отдельном `BoundaryProbe` роль 03 выполнила строгую компиляцию и три запуска JVM с кодами завершения 0; проверила документированный супертип исключения диапазона, пустой `concat`, конкретный случай NFC, содержимое префикса после `arraycopy`, последовательную модель составной операции и форматирование с ROOT. Эти результаты не следуют автоматически из S17, A20 или S23. Условия, команды и вывод приведены в [отчёте роли 03](https://github.com/Melchiorius/backend-interview-labs/pull/331#issuecomment-7207496299) и в указанном выше README общего runnable.

Все эти локальные измерения относятся к исходной авторской доработке и интеграции D-A14b, а не к повторному запуску после редакционных изменений прозы. CI проверяет репозиторий на конкретном HEAD отдельно, но не исполняет временные `Probe`, `Fences`, `BoundaryProbe` и четыре новых отрицательных исходника. Полная проверка всех Markdown-примеров, GUI, других JDK и конкурентных расписаний не заявлена.

**Что читать дальше:** [массивы](arrays.md), [конверсии](conversions.md), [равенство](equality.md), [обобщённые типы](generics.md), [лямбды](functions.md) и [потоки](streams.md). Для **текстового представления чисел** и `new BigDecimal("0.1")` vs `new BigDecimal(0.1)` — [CORE-NUMBERS](numbers.md); для **ISO parsing, DateTimeFormatter и явной ZoneId** — [CORE-DATETIME](datetime.md).

**Дополнительный материал по regex:** [CORE-REGEX](regex.md) подробно объясняет отличие Java-экранирования от грамматики `Pattern`, состояния `Matcher`, `find`/`matches`, групп, замен и Unicode; это продолжение изложенных здесь `split` и `replaceAll`.

**Переход к пользовательским языкам и кодировкам:** [CORE-I18N](internationalization.md) разбирает `Locale`, турецкое преобразование регистра, `ResourceBundle` и независимость `Charset` от локали. UTF-16, `String` и кодовые точки изучаются здесь.
