# CORE-STRINGS и CORE-BUILDERS — String, Unicode и изменяемые последовательности Java 21

**Область:** Java SE 21, без preview. **Маршрут:** [типы и преобразования](types.md) → [строковые выражения](conversions.md) → эта статья → [массивы](arrays.md) и [равенство](equality.md). Тема состоит из двух самостоятельных curricular ID: **CORE-STRINGS** (TX-01–TX-20) и **CORE-BUILDERS** (BL-01–BL-09), но они разобраны **в одном читательском материале**: чтобы понимать Builder, сначала нужно понимать, что именно сохраняет и измеряет String.

**Сквозной runnable:** [Arrays + Strings/Builders — две Java 21 программы и эталоны](../../examples/core/arrays-strings/README.md) позволяет проверить A01–A24 и S01–S28 по отдельности (52 строки двух запусков, **не 52 теста**). **G1:** [принятый исследовательский план](../../docs/plans/CORE-ARRAYS-STRINGS.md), **TX-01–TX-20 (14 P0/4 P1/2 P2), BL-01–BL-09 (6 P0/2 P1/1 P2)**. Нормативная основа: [JLS Java21 §3.10.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10.5), [§15.18.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.18.1), [String API 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html), [Character](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Character.html), [StringBuilder](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/StringBuilder.html), [StringBuffer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/StringBuffer.html).

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

`==` для ссылок сравнивает **идентичность объектов**, `equals` у String — **содержимое** (последовательность UTF-16 кодовых единиц). Для бизнес-логики «равны ли введённые логины?» нужен согласованный контракт содержимого, не случайное совпадение ссылок. Но и `equals` не выполняет Unicode-нормализацию, locale-collation или case-folding автоматически — границы см. §10.

**Почему immutable важно:** строку допустимо передавать между частями приложения, не опасаясь, что другая функция изменит её через публичный API. Это **не** означает, что любой объект, содержащий ссылку на String, автоматически потокобезопасен или что JVM обязана создать определённое количество байтовых массивов для операций с ней.

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

`intern()` возвращает каноническое строковое значение согласно контракту API. **Не выводите отсюда** точное место расположения string pool в heap, формат представления символов, правила сборки мусора, число промежуточных объектов, HotSpot Compact Strings или алгоритм JIT — всё это другие вопросы, которые нельзя доказывать идентичностью двух литералов. Норма: [JLS §3.10.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10.5), [§15.29](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.29), [String.intern](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#intern()).

## 3. Конкатенация, порядок вычисления и представление null (TX-04)

Оператор `+` в строковом контексте преобразует операнды в текст, а подвыражения вычисляются **слева направо** с обычной ассоциативностью. Это влияет на результат:

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

Строковый текст `"null"` здесь — результат string conversion, **не** свидетельство наличия объекта String у ссылки `missing`. При сложной конкатенации не полагайтесь на конкретный сгенерированный байткод, обязательную реализацию через Builder или JIT-оптимизацию. Если в цикле вы собираете длинную изменяемую строку, чаще понятнее явный Builder (§11), но **без** обобщённого обещания ускорения.

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
- `codePointAt(int i)` принимает **UTF-16 индекс** и возвращает `int` code point, если там корректная пара; изолированный суррогат обрабатывается по правилам String API.
- `codePointCount(beginIndex,endIndex)` считает Unicode code points в **диапазоне UTF-16 индексов**; `offsetByCodePoints` преобразует смещение в code points в новый UTF-16 индекс.
- `substring(begin,end)` использует **UTF-16 индексы**, причём `end` **исключается**. Если разрезать строку посередине суррогатной пары, можно получить строку с одиноким суррогатом; `substring` не выполняет семантическую сегментацию Unicode.
- `codePoints()` возвращает `IntStream`, а не `Stream<Character>`; переход к примитивным потокам разобран в [CORE-STREAMS](streams.md).

~~~java
class SubstringRange {
    public static void main(String[] args) {
        String s = "Java";
        System.out.println(s.substring(1, 3)); // av
        try {
            s.substring(0, 5);
        } catch (StringIndexOutOfBoundsException e) {
            System.out.println("out of range");
        }
    }
}
~~~

**Даже code point не обязательно равен «видимому символу»**: буква с комбинируемым знаком, флаг или emoji с Zero Width Joiner могут содержать несколько code points. Это отдельная тема Unicode grapheme clusters, не решаемая вызовом `codePointCount`. Обработка графем/нормализация помечены как P2 (TX-20); здесь не проводится полноценный i18n-тест.

## 5. trim, strip, isBlank, isEmpty — проверяем разные условия (TX-08)

`trim()` удаляет начальные и конечные символы с кодовыми значениями **не больше U+0020** согласно контракту метода. `strip()` применяет Unicode-ориентированную классификацию пробельных code points через `Character.isWhitespace`. `isBlank()` проверяет, состоит ли строка из таких пробельных code points; `isEmpty()` проверяет лишь нулевую **длину**.

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

Нельзя писать «`strip` удаляет вообще все Unicode whitespace во всех возможных стандартах»: формальный предикат именно `Character.isWhitespace` на версии Java 21. Для nonbreaking space и иных code points полезно проверять конкретную классификацию, а не интуитивную ширину/видимость символа. [String.strip/isBlank](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html), [Character.isWhitespace](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Character.html#isWhitespace(int)).

## 6. split — это regex; limit=0 теряет конечные пустые поля (TX-09)

`String.split(regex)` принимает **регулярное выражение**, а не буквальный разделитель. Точка без экранирования в regex означает «любой символ». Для буквальной точки сам regex должен содержать **один** обратный слеш перед точкой: `\.`. В исходном коде Java этот слеш нужно экранировать **ещё раз** в строковом литерале: `"\\."`. Сначала Java преобразует литерал в строку `\.`, затем regex-движок воспринимает `\.` как буквальную точку.

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

У `split(regex, 0)` конечные пустые строки отбрасываются; `split(regex, -1)` их сохраняет. При положительном limit итог может содержать максимум указанное количество элементов, и остаток строки становится последним элементом. Для literal-подстроки, которую пользователь ввёл сам, можно использовать `Pattern.quote(delimiter)`, иначе спецсимволы изменят смысл регулярного выражения. Нормы: [String.split](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#split(java.lang.String,int)), [Pattern](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html).

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

Последняя строка намеренна: для non-null delimiter и null-элемента `String.join` отображает такой элемент как текст `"null"`. При **null delimiter** выбрасывается NPE; нельзя выводить из этого, будто все перегрузки `join` одинаково относятся к null-аргументам и коллекциям. [String.join](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#join(java.lang.CharSequence,java.lang.CharSequence...)).

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

**String switch:** сравнение case-строк проводится по значению, не идентичности String. В Java 21 можно явно обработать `case null`; без явной обработки не следует считать null безопасным значением для любого string switch. В коде ниже нет preview:

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

**String.compareTo** задаёт лексикографическое сравнение UTF-16 единиц, а не «порядок букв по русскому/английскому алфавиту». **String.format/formatted** зависят от указанной или выбранной locale, если форматируете locale-sensitive значения. Чтобы явно определить десятичный разделитель, передавайте `Locale.ROOT` (или нужную локаль):

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

### 11. StringBuilder vs StringBuffer: synchronized методы ≠ транзакция (BL-02, BL-09)

Оба класса предоставляют изменяемую последовательность и близкие методы, но контракты доступа различаются:

| Класс | Синхронизация | Что важно при проектировании |
|---|---|---|
| `StringBuilder` | **Нет** общей гарантии синхронизации; не предназначен для конкурентной мутации без внешней защиты | Обычно локальное накопление в рамках одного вычисления |
| `StringBuffer` | Отдельные методы синхронизированы согласно API | **Несколько последовательных методов не образуют автоматически атомарную операцию**, требуется общий lock для составного инварианта |

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

Наличие синхронизации у отдельных методов Buffer **не означает**, что пара `indexOf → append` атомарна. Исправленный вариант корректен **только если все участники, влияющие на этот инвариант, соблюдают согласованную дисциплину блокировки**. Здесь не выполняется конкурентный stress-test, не обещается обязательный race symptom и не делается вывод, что Buffer быстрее/медленнее Builder во всех нагрузках. Рекомендации об общей блокировке — в [многопоточности](../../knowledge/03-concurrency/semaphore.md) как отправной теме, но это не замена полного курса JMM.

### 12. length, capacity, ensureCapacity и setLength (BL-03, BL-06)

У Builder/Buffer два разных числовых свойства. **`length()`** — количество фактически хранимых UTF-16 code units; **`capacity()`** — текущая ёмкость до необходимости расширить хранилище. Вызов `ensureCapacity(n)` может увеличить ёмкость, **не добавляя видимые символы**. Вызов `setLength(n)` меняет логическую длину; при увеличении новая область заполнена кодовой единицей U+0000, а не пробелами.

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

`String.contentEquals(CharSequence)` сравнивает содержимое, а не ссылки. Если объект в другом потоке одновременно меняется, нельзя выводить из одного вызова `contentEquals` требования к атомарной бизнес-проверке; доступ к mutable объектам должен проектироваться отдельно.

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

Здесь обе строки **успешно компилируются**; отличие проявляется при выполнении. Для некастованного literal `null` правила выбора перегрузки нужно анализировать по их параметрам, а не угадывать по итоговой печати. Связанный случай `String.valueOf(null)` разобран в §8.

### 15. delete, reverse, вставка и конкатенация в цикле (BL-07, BL-08)

`insert` и `delete` используют индексы UTF-16 units; некорректные индексы приводят к исключениям, а не к молчаливой правке. `reverse` — операция перестановки последовательности с правилами, описанными в API, в том числе специальной обработкой пар суррогатов; она **не** является Unicode grapheme-aware реверсом пользовательского текста.

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
| `"abc".charAt(3)` | **Runtime** `StringIndexOutOfBoundsException` | Неверный UTF-16 индекс |
| `String.valueOf(null)` | **Runtime** `NullPointerException` | Выбран overload `char[]` |
| `String.valueOf((Object)null)` | Строка `"null"` | Выбран overload `Object` |
| `new StringBuilder().append((String)null)` | Содержит `"null"` | Контракт String overload |
| `new StringBuilder().append((char[])null)` | **Runtime** `NullPointerException` | Нужен реальный char[] |
| `new StringBuilder().charAt(0)` | **Runtime** `StringIndexOutOfBoundsException` | Builder пуст |

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

В принятом G1 есть **полный `StringsG1.java` на 28 строк stdout S01–S28**, отдельно полный `ArraysG1.java` на 24 строки, вместе **52 наблюдения двух Java-процессов**, а не 52 независимых unit tests. Для воспроизводимости доступен [общий runnable с двумя отдельными Java-программами](../../examples/core/arrays-strings/README.md), включённый из [PR #125](https://github.com/Melchiorius/backend-interview-labs/pull/125) в авторскую ветку. Роль 03 на OpenJDK 21.0.12.1 подтвердила побайтное совпадение Git blobs, `javac --release 21`, два `java` и два `diff -u` (24/24 и 28/28 строк). GitHub Actions итогового HEAD подтверждается отдельно в [PR #124](https://github.com/Melchiorius/backend-interview-labs/pull/124); это **не** запуск всех inline Markdown-примеров. Негативные N01–N24 — отдельные `javac --release 21` compilation units, а не тесты ожидаемых runtime ошибок. Автор 02 независимо повторил на OpenJDK/javac **21.0.12.1** все **24/24** по ожидаемым диагностическим категориям без случайных parser/EOF ошибок; временные исходники не закоммичены и не исполняются в CI. Отдельный локальный положительный smoke подтвердил `String.valueOf(null)` → NPE, `String.valueOf((Object)null)` → текст `null`, `switch` с `case null`, Unicode code point, `join`, `split` и Builder capacity/length. Это **не** полная компиляция всех snippets этой статьи и не полный 28-строчный G1 stdout.

**NOT RUN (до отдельного evidence):** автоматическое извлечение и компиляция всех Java snippets из Markdown; negative N01–N24 в CI; GUI Obsidian/IntelliJ; JDK/JVM/OS помимо заявленного; реальные кодировки/файловый IO/locale integration; grapheme segmentation/ZWJ и сложная Unicode-нормализация; Concurrent Builder/Buffer stress; JMH, HotSpot internals, GC и JIT. Никаких выводов о реальной производительности Builder и thread-safety произвольного составного алгоритма здесь не делается.

**Что читать дальше:** [массивы](arrays.md), [конверсии](conversions.md), [равенство](equality.md), [обобщённые типы](generics.md), [лямбды](functions.md) и [потоки](streams.md). Для **текстового представления чисел** и `new BigDecimal("0.1")` vs `new BigDecimal(0.1)` — [CORE-NUMBERS](numbers.md); для **ISO parsing, DateTimeFormatter и явной ZoneId** — [CORE-DATETIME](datetime.md).
