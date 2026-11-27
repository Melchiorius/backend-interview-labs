# CORE-REGEX — регулярные выражения Java 21: язык шаблона, Matcher, замены и границы безопасности

**Практическая задача.** В строке `user=67; role=admin` требуется извлечь два значения, проверить формат всего поля или заменить часть текста. Три операции похожи внешне, но имеют разные границы поиска и правила интерпретации. Особенно опасно перепутать **экранирование Java-строки** с **синтаксисом регулярного выражения**, а буквальное значение замены — с шаблоном подстановки групп.

Эта статья объясняет `java.util.regex.Pattern`, `Matcher`, методы `String`, Unicode и ограничения движка **Java SE 21 без preview**. Операторы pattern matching (`instanceof`, `switch`, record patterns) — самостоятельная [CORE-PATTERNS](patterns.md), не регулярные выражения. Основные смежные тексты: [CORE-STRINGS](strings.md) (`split`, `replaceAll`, UTF-16), [CORE-OPERATORS](operators.md) (Java-выражения), [CORE-EXCEPTIONS](exceptions.md) (runtime-исключения), [CORE-ARRAYS](arrays.md) (строки и массивы результатов).

**Нормативная опора:** [JLS 21 §3.10.7 — escape sequences](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10.7), [Pattern API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html), [Matcher API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Matcher.html), [PatternSyntaxException API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/PatternSyntaxException.html), [String API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html). Реальная исследовательская матрица и полный исторически исполненный Java21 source находятся в [принятом G1-плане](../../docs/plans/CORE-REGEX-UTIL.md), §3, §5–§7. **Покрытие:** RG-01–RG-31 = 31 P0; RG-32–RG-37 = 6 P1; RG-38–RG-41 = 4 P2. Метки обозначают исследованные вопросы, **не 41 unit test**.

**Исторический комплект практики:** [RegexProbe.java](../../examples/core/regex-util/src/RegexProbe.java), [эталон вывода — 33 строки](../../examples/core/regex-util/regex.expected.stdout) и [инструкция по запуску](../../examples/core/regex-util/README.md). Рядом находится [UtilProbe.java](../../examples/core/regex-util/src/UtilProbe.java) с 38 строками. В сумме это **71 наблюдение двух отдельных JVM, а не 71 JUnit-тест**. Исходники и эталоны перенесены из принятого G1 §6 без изменения логики, кроме поясняющих комментариев `// Theory:`. Исторический [интегрированный CI #45164089936](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45164089936) относится только к HEAD `804f4412ad5df1f081cb74e90602bef4fb3d3977`, не к последующим изменениям.

**Как читать код.** Блоки операторов — независимые фрагменты тела метода, не один склеиваемый исходник. Поместите импорты `java.util.regex.*` и `java.util.*` в начало класса, а не внутрь метода; имена переменных в соседних блоках могут повторяться. Исторические шесть отрицательных компиляционных сценариев Regex и восемь Util проверялись отдельно на OpenJDK 21.0.12.1, не в штатном CI. Новые конечные проверки D-A23a и их пределы указаны в §10.

## 1. Два парсера подряд: Java-литерал → Pattern (RG-01, RG-02, RG-20)

В исходном `"\\d+"` Java-компилятор сначала обрабатывает два символа `\\` как **один обратный слеш** в значении `String`. Затем `Pattern.compile` интерпретирует полученное `\d+` как последовательность одной или более цифр. Записать `"\d+"` **в Java source нельзя**: `\d` не является допустимой escape-последовательностью Java. Это ошибка компиляции, а не `PatternSyntaxException`. Строка, полученная во время выполнения из файла, конфигурации или HTTP, **не является Java-литералом**: Java-компилятор не обрабатывает её обратные слеши как escape-последовательности исходного кода. В `Pattern.compile` передаётся фактическое содержимое такой строки; автоматически удваивать обратные слеши не нужно.

~~~java
import java.util.regex.Pattern;

String regex = "\\d+";                // значение String: \d+
Pattern digits = Pattern.compile(regex); // компилируется синтаксис regex
System.out.println(digits.matcher("67").matches());   // true
System.out.println(digits.matcher("id=67").matches()); // false
~~~

`Pattern` — повторно используемое скомпилированное представление шаблона. Для каждого входа `pattern.matcher(input)` создаётся **отдельный Matcher**, который хранит изменяемое состояние поиска. `Pattern.compile("[")` — **корректный Java source**, но некорректный regex: при выполнении `compile` будет `PatternSyntaxException` (unchecked). Не стоит объявлять любое падение regex «ошибкой Java-компиляции». Обратный слеш в regex может относиться к классу символов (`\d`), границе (`\b`) или буквальному метасимволу (`\.`); `"\\b"` — regex word boundary, а `"\b"` — уже Java backspace.

Не всякий одиночный обратный слеш означает ошибку Java: некоторые escape-последовательности допустимы, но меняют смысл ещё до вызова Pattern.

| Java source (литерал) | Фактическое значение String | Смысл для Pattern |
|---|---|---|
| `"\\d+"` | `\d+` | одна или более цифр |
| `"\d+"` | значения нет | ошибка javac, regex не выполняется |
| `"\s"` | U+0020 (пробел) | буквальный пробел |
| `"\\s"` | `\s` | класс whitespace (по умолчанию ASCII) |
| `"\b"` | U+0008 (backspace) | буквальный backspace |
| `"\\b"` | `\b` | граница слова, не потребляет символ |
| `"\t"` | U+0009 (tab) | буквальная табуляция |
| `"\\t"` | `\t` | regex-escape табуляции |
| `"\u0061"` | `a` | буквальная a |
| `"\\u0061"` | `\u0061` | regex-escape U+0061 |

[JLS §3.2–3.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.3) описывает раннюю обработку Unicode escapes до выделения токенов; например, Unicode escape перевода строки внутри обычного строкового литерала не заменяет Java escape `\n`. Не путайте это с regex Unicode-escape, который получает уже готовый String. JSON, YAML, properties и другие форматы конфигурации могут иметь **собственные** правила экранирования: сначала выясняйте, какое значение декодировано форматом, затем проверяйте Pattern.

~~~java
System.out.println("\t".matches("\s"));   // false
System.out.println("\t".matches("\\s"));  // true
System.out.println("\b".matches("\b"));   // true: backspace
System.out.println("\t".matches("\\t"));  // true
System.out.println("a".matches("\\u0061")); // true
~~~

## 2. Три режима поиска и изменяемое состояние (RG-03–RG-08, RG-24)

`matches()` требует, чтобы шаблон покрыл **весь текущий region**; `lookingAt()` — совпадение **от начала region**, но может оставить суффикс; `find()` ищет **очередное совпадение внутри region**. `"id=67".matches("\\d+")` возвращает `false`, хотя `Matcher.find()` для `\d+` обнаружит `67`. `String.matches` принимает **String regex**, не `Pattern` и не готовый `Matcher`.

~~~java
import java.util.regex.*;

Matcher m = Pattern.compile("\\d+").matcher("u67 v68");
System.out.println(m.matches());   // false: целиком не цифры
System.out.println(m.lookingAt()); // false: вход начинается с u
while (m.find()) {
    System.out.println(m.group() + "@" + m.start() + "-" + m.end());
}
// 67@1-3
// 68@5-7
m.reset();
System.out.println(m.find()); // true: поиск снова начинается с начала
~~~

`start()` включает первую позицию совпадения, `end()` указывает **на позицию после последнего элемента**; индексы заданы в единицах `char`/UTF-16, не в grapheme clusters. После успешного поиска `group()` возвращает совпавшую часть; перед первой успешной операцией или после неудачного поиска `group()` и `start()` могут бросить `IllegalStateException`. Код, вызывающий `group()` без проверки `find()/matches()`, логически ошибочен даже если компилируется.

Явное состояние Matcher — последнее совпадение и границы/значения его групп; неявное — входная последовательность и позиция добавления (append position, изначально 0). `reset()` удаляет явное состояние, возвращает region ко **всему входу** и append position к 0, сохраняя настройки anchoring/transparency. `reset(newInput)` делает то же с новой последовательностью. `region(start,end)` сначала сбрасывает Matcher, затем задаёт новый полуинтервал. `find(int start)` сбрасывает Matcher и начинает поиск с абсолютного UTF-16 индекса во всём входе, не со смещения относительно прежнего region; недопустимый индекс даёт IndexOutOfBoundsException. Это не просто очередной `find()`. Один Matcher не является безопасным общим состоянием для конкурентных вызовов.

~~~java
Matcher m = Pattern.compile("a").matcher("aXaYa").region(2, 4)
        .useAnchoringBounds(false).useTransparentBounds(true);
if (m.find()) System.out.println(m.start()); // 2
m.reset();
System.out.println(m.regionStart() + ":" + m.regionEnd()); // 0:5
System.out.println(m.hasAnchoringBounds() + ":" + m.hasTransparentBounds()); // false:true
m.region(2, 4);
if (m.find(4)) System.out.println(m.start()); // 4, region снова 0:5
m.reset("ba");
if (m.find()) System.out.println(m.start()); // 1, region 0:2
~~~

Если нужно сохранить ограничения при повторном поиске, заново задайте region после reset; не рассчитывайте на его сохранение при всех операциях. Позиция добавления важна при заменах (§5).

## 3. Группы, нулевая длина, квантификаторы (RG-09–RG-13, RG-30)

Круглые скобки `(...)` захватывают подстроку; номера начинаются с 1 и назначаются по порядку открывающих захватывающих скобок слева направо. `group(0)` — всё совпадение, `(?<id>...)` имеет и имя, и номер, а `(?:...)` нового номера не создаёт. Число захватывающих групп `groupCount()` известно до поиска, в отличие от значений групп. Методы `group/start/end` требуют успешного последнего совпадения (иначе IllegalStateException); при успешном совпадении неизвестный номер даёт IndexOutOfBoundsException, неизвестное имя — IllegalArgumentException. Неучаствующая группа не равна пустой участвовавшей группе.

~~~java
Matcher m = Pattern.compile("(?<name>[a-z]+)=(\\d+)").matcher("user=67");
if (m.matches()) {
    System.out.println(m.group("name")); // user
    System.out.println(m.group(2));      // 67
    System.out.println(m.groupCount());   // 2
}
System.out.println(Pattern.compile("(?:a)+").matcher("aa").groupCount()); // 0
~~~

Для шаблона `(?<optional>a)?(?<empty>b*)(?<value>c)` и входа `"c"`, после успешного matches:

| Группа | group | start | end |
|---|---|---:|---:|
| optional / 1 (не участвовала) | null (ссылка) | -1 | -1 |
| empty / 2 (участвовала, ноль символов) | `""` | 0 | 0 |
| value / 3 (непустая) | `"c"` | 0 | 1 |

~~~java
Matcher m = Pattern.compile("(?<optional>a)?(?<empty>b*)(?<value>c)").matcher("c");
System.out.println(m.groupCount()); // 3 до совпадения
System.out.println(m.hasMatch());   // false
if (m.matches()) {
    MatchResult saved = m.toMatchResult();
    System.out.println(saved.namedGroups().get("empty")); // 2
    m.reset("abc");
    if (m.matches()) {
        System.out.println(m.group(1));     // a
        System.out.println(saved.group(1)); // null
    }
}
~~~

`toMatchResult()` фиксирует состояние совпадения независимо от последующих операций этого Matcher, не обещая идентичность ссылок String. В Java 21 у [MatchResult](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/MatchResult.html) и Matcher доступны `namedGroups()` (немодифицируемое отображение имён на номера в реализации Matcher и его снимке) и `hasMatch()` (признак действительного результата). Именованные группы и эти методы не устраняют необходимость проверить успех поиска. При повторении группы ранее захваченное значение может сохраниться, если очередная итерация внутренней группы не совпала: `(a(b)?)+` на `"aba"` даёт группу 2 `"b"`, не null.

Жадный `.*` пробует большее число повторений и может отступать; ленивый `.*?` начинает с меньшего числа и может расширяться; possessive `.*+` не возвращает уже захваченное этим квантификатором. Это порядок попыток в данном шаблоне, а не обещание глобально самого длинного/короткого совпадения. `a+a` на `"aa"` успешно, `a++a` нет: первая часть забрала обе a. Atomic group `(?>...)` фиксирует выбранный успешный путь внутри группы: `(a|ab)c` на `"abc"` успешно после возврата к альтернативе ab, `(?>a|ab)c` — нет. Порядок альтернатив важен: find для `a|ab` на `"ab"` даёт a, для `ab|a` — ab. Ни один из этих примеров не устанавливает общую сложность regex.

~~~java
System.out.println(Pattern.compile("<.*>").matcher("<a><b>").find()); // true
System.out.println(Pattern.compile("a++a").matcher("aa").matches());    // false
Matcher zero = Pattern.compile("(?=a)").matcher("aa"); // lookahead ничего не потребляет
while (zero.find()) System.out.println(zero.start() + ":" + zero.end());
// 0:0
// 1:1
~~~

`find()` умеет продолжать итерацию даже после совпадения **нулевой длины**; без этого самодельный цикл, вручную повторяющий поиск с прежнего индекса, мог бы зациклиться. Lookahead `(?=...)` проверяет, что впереди допускается шаблон, но **не потребляет** этот текст; обычный `\d+` после lookahead всё ещё нужен, если требуется включить цифры в совпадение.

~~~java
Matcher greedy = Pattern.compile("a.+b").matcher("a1b2b");
Matcher reluctant = Pattern.compile("a.+?b").matcher("a1b2b");
if (greedy.find() && reluctant.find()) {
    System.out.println(greedy.group() + "/" + reluctant.group()); // a1b2b/a1b
}
System.out.println("abc".matches("(a|ab)c"));   // true
System.out.println("abc".matches("(?>a|ab)c")); // false
Matcher zero = Pattern.compile("(?=a)|$").matcher("aa");
while (zero.find()) System.out.println(zero.start() + ":" + zero.end());
// 0:0, 1:1, 2:2 (в том числе конец входа)
~~~

На локальном OpenJDK 21.0.12.1 пустой шаблон на `"😀"` дал позиции `0:0, 1:1, 2:2`. Это наблюдение, а не универсальная спецификация внутреннего алгоритма продвижения. Не предполагается, что продвижение всегда происходит на целую Unicode code point или графему. Индексы абсолютны относительно входной последовательности (§6), а не относительно региона.

## 4. Якоря и режимы Pattern (RG-14–RG-16, RG-26, RG-27)

`^` и `$` обозначают границы, но не заменяют matches. В обычном режиме `$` может совпасть также перед завершающим разделителем строки: find для `^a$` на `"a\n"` находит a, matches — false. `\A` обозначает начало входа, `\Z` — конец или позицию перед финальным разделителем, `\z` — строгий конец. Для Matcher с anchoring bounds эти границы могут трактоваться как границы region (§6). По умолчанию разделители строк — LF, CR, CRLF, U+0085, U+2028, U+2029; `UNIX_LINES` оставляет только LF для поведения `. ^ $`. `MULTILINE` добавляет начала/концы строк для `^/$`, а `DOTALL` независимо позволяет точке покрывать разделители строк.

~~~java
System.out.println(Pattern.compile("^67$", Pattern.MULTILINE)
        .matcher("a\n67\nb").find());                     // true
System.out.println(Pattern.compile("a.b")
        .matcher("a\nb").matches());                     // false
System.out.println(Pattern.compile("a.b", Pattern.DOTALL)
        .matcher("a\nb").matches());                     // true
System.out.println(Pattern.compile("AB", Pattern.CASE_INSENSITIVE)
        .matcher("ab").matches());                       // true
~~~

`Pattern.LITERAL` объявляет **весь переданный текст шаблона** буквальным: `Pattern.compile("a.b", Pattern.LITERAL)` сопоставляет именно `a.b`, а не `axb`. `Pattern.COMMENTS` включает режим игнорирования неэкранированных пробелов и комментариев внутри **синтаксиса регулярного выражения, в том числе внутри классов символов**. Например, `[a b]` без флага сопоставляет пробел, а с `COMMENTS` — нет; для буквального пробела в классе его надо экранировать в regex (например, в Java-строке `"[a\\ b]"`). Этот флаг никак не меняет обработку строковых литералов самим Java-компилятором. При сопоставлении пользовательского **буквального** фрагмента чаще проще `Pattern.quote(userInput)`, а не ручное экранирование каждого метасимвола.

### Флаги регистра, классов и канонической эквивалентности

| Флаг(и) | `é` сопоставляется с `É`? | Что меняется |
|---|---|---|
| CASE_INSENSITIVE | нет | по умолчанию ASCII-регистр |
| UNICODE_CASE без CASE_INSENSITIVE | нет | сам не включает игнорирование регистра |
| CASE_INSENSITIVE + UNICODE_CASE | да | Unicode-aware сравнение регистра |
| UNICODE_CHARACTER_CLASS (UCC) | нет | Unicode-классы; подразумевает UNICODE_CASE, не CASE_INSENSITIVE |
| CASE_INSENSITIVE + UCC | да | Unicode-классы и Unicode-aware игнорирование регистра |

UCC меняет предопределённые классы и POSIX-классы (например, `\d` и `\p{Lower}`), но сам по себе не превращает явно заданный диапазон `[0-9]` в множество всех десятичных Unicode-цифр. Свойство `\p{IsLatin}` задаёт Unicode script, а не ASCII-диапазон. `LITERAL` сохраняет действие CASE_INSENSITIVE и UNICODE_CASE, не интерпретируя метасимволы. `CANON_EQ` сопоставляет канонически эквивалентные представления при сопоставлении; это не операция нормализации или изменение String и не обещание полного case folding, одинакового поведения всех письменностей либо малой стоимости.

~~~java
int ci = Pattern.CASE_INSENSITIVE;
int uc = Pattern.UNICODE_CASE;
System.out.println(Pattern.compile("é", ci).matcher("É").matches()); // false
System.out.println(Pattern.compile("é", ci | uc).matcher("É").matches()); // true
System.out.println(Pattern.compile("[0-9]", Pattern.UNICODE_CHARACTER_CLASS)
        .matcher("٣").matches()); // false
System.out.println(Pattern.compile("é.", Pattern.LITERAL | ci | uc)
        .matcher("É.").matches()); // true
System.out.println(Pattern.compile("é", Pattern.CANON_EQ)
        .matcher("e\u0301").matches()); // true
~~~

## 5. Два независимых языка: шаблон поиска и replacement (RG-17–RG-19, RG-21, RG-22)

У `replaceAll(regex,replacement)` **оба** аргумента интерпретируются по правилам, но **по разным**: первый — regex Pattern, второй — replacement grammar, где `$1` подставляет первую группу, а обратный слеш служит экранированием. Поэтому `Pattern.quote` защищает *шаблон поиска*, а `Matcher.quoteReplacement` — *буквальную строку замены*. Менять эти методы местами нельзя.

~~~java
String v = "user-67";
System.out.println(v.replaceAll("(\\w+)-(\\d+)", "$2:$1")); // 67:user
System.out.println("a.b".replaceAll(Pattern.quote("."), ":")); // a:b
String replacement = "$1\\path";
System.out.println(Pattern.compile("67").matcher("67")
        .replaceAll(Matcher.quoteReplacement(replacement))); // буквальные $1\path
~~~

`replaceFirst` заменяет только первое совпадение, `replaceAll` — все найденные неперекрывающиеся совпадения; `String.replace` работает с буквальными последовательностями. **Ошибочная ссылка в replacement не означает безусловное исключение:** если есть совпадение и обработка доходит до недопустимой ссылки `$9` при отсутствии группы 9, возникает IndexOutOfBoundsException. Если совпадений нет, замена не производится: `"x".replaceAll("(a)", "$9")` возвращает `"x"`. Аналогичный контраст для replaceFirst. Не используйте эти методы как безусловную валидацию произвольного replacement. Java-компилятор не проверяет его грамматику. Подробности простых перегрузок — в [CORE-STRINGS](strings.md).

| Replacement при совпадении | Правило / результат |
|---|---|
| `$0` | всё совпадение |
| `$1`, `${name}` | группа по номеру / имени |
| `$12` при единственной группе | группа 1, затем буквальная 2 (следующие цифры включаются, пока номер допустим) |
| `<$1>` при неучаствовавшей группе 1 | `<>`, а не текст null |
| `$9` при отсутствии группы 9 | IndexOutOfBoundsException |
| `${missing}` при отсутствии такой именованной группы | IllegalArgumentException |
| одиночный `$` или обратный слеш в конце | ошибочная грамматика; в проверенных случаях IllegalArgumentException |

~~~java
try {
    System.out.println("a".replaceAll("(a)", "$9"));
} catch (IndexOutOfBoundsException ex) {
    System.out.println(ex.getClass().getSimpleName()); // IndexOutOfBoundsException
}
System.out.println("x".replaceAll("(a)", "$9")); // x
System.out.println("a".replaceAll("(a)", "$0/$1/$12")); // a/a/a2
System.out.println("ab".replaceAll("(?<x>a)b", "${x}")); // a
System.out.println("b".replaceAll("(a)?b", "<$1>")); // <>
System.out.println(Pattern.compile("(a)").matcher("a")
        .replaceAll(m -> "$1!")); // a!
System.out.println(Pattern.compile("(a)").matcher("a")
        .replaceAll(m -> Matcher.quoteReplacement("$9"))); // $9
~~~

Функциональные перегрузки replaceAll/replaceFirst также интерпретируют возвращаемую функцией строку как replacement; это не автоматически буквальный результат. Не меняйте Matcher из функции, а MatchResult в callback не храните как стабильный снимок за пределами вызова. Если часть replacement — пользовательский литерал, экранируйте **эту часть**, сохранив намеренные ссылки на группы: `"$1" + Matcher.quoteReplacement("$9\\x")`. Не конструируйте regex из недоверенного синтаксиса, если задача требует буквального текста.

**Замены и region.** Оба метода Matcher replaceFirst/replaceAll сначала выполняют reset. Даже если region выбран заранее, поиск начнётся по всему входу. Например:

~~~java
String text = "a67b68";
System.out.println(text.replaceFirst("\\d+", "X")); // aXb68
System.out.println(text.replaceAll("\\d+", "X"));   // aXbX
Matcher m = Pattern.compile("\\d+").matcher(text).region(4, 6);
System.out.println(m.replaceFirst("X")); // aXb68, не a67bX
m.region(4, 6);
System.out.println(m.replaceAll("X"));   // aXbX
~~~

`appendReplacement` при успешном совпадении дописывает в переданный буфер часть исходного входа от append position до start, затем раскрытую замену и переводит append position в end. `appendTail` дописывает остаток от append position до конца **входа**, не просто до regionEnd. Вместе они не являются автоматическим API замены только содержимого region; если это требуется, отдельно задайте область преобразования (например, выделите подстроку, замените в ней, соберите с исходными префиксом и суффиксом, учитывая изменённый контекст границ). Не подменяйте этим поиск, которому необходимо видеть исходный контекст за границами области.

## 6. Region, границы поиска и UTF-16 (RG-23, RG-25, RG-28, RG-29)

`Matcher.region(start,end)` ограничивает область работы полуинтервалом **[start,end)**. По умолчанию границы региона **opaque** для lookaround и некоторых проверок границы; `useTransparentBounds(true)` позволяет lookahead/lookbehind «видеть» символы вне региона, но само совпадение всё ещё ограничено регионом. Якоря `^`/`$` отдельно подчиняются `useAnchoringBounds(boolean)`: прозрачность и привязка — два независимых параметра.

~~~java
Matcher m = Pattern.compile("(?<=a)67").matcher("a67b").region(1, 3);
System.out.println(m.find());                       // false: opaque region
m.reset().region(1, 3).useTransparentBounds(true);
System.out.println(m.find());                       // true: lookbehind видит a

String s = "😀"; // supplementary Unicode code point U+1F600
System.out.println(s.length());                      // 2 UTF-16 code units
System.out.println(s.codePointCount(0, s.length())); // 1 code point
~~~

Для входа `"a67\nb"` с region [1,4) получаем следующую матрицу find (не matches):

| Anchoring | Transparent | `^67$` | `(?<=a)67` |
|---|---|---|---|
| true | false | true | false |
| true | true | true | true |
| false | false | false | false |
| false | true | false | true |

При anchoring=true region ведёт себя для якорей как вход с конечным LF; выключение anchoring не разрешает lookbehind. Transparent bounds независимо меняет видимость префикса для lookbehind. В отдельном примере без region `\Aa\Z` на `"a\n"` находит a, `\Aa\z` — нет. С MULTILINE для `"x\ra\rb"` find `^a$` успешен, с MULTILINE + UNIX_LINES — нет. Исторический RG28 — **другой** пример: `\bword` на `"xxword"` с region [2,6) даёт true при opaque и false при transparent; это не результат показанного выше lookbehind.

Результаты `start()/end()` и `String.length()` выражены в UTF-16 единицах, а не в «пользовательских символах». Regex API поддерживает дополнительные Unicode-конструкции, но не обещает автоматически разбить каждый видимый grapheme cluster на одну позицию. По умолчанию `\d` ориентирован на ASCII-цифры, а `UNICODE_CHARACTER_CLASS` расширяет поведение предопределённых классов:

~~~java
String arabicDigit = "٣"; // U+0663
System.out.println(Pattern.compile("\\d").matcher(arabicDigit).matches()); // false
System.out.println(Pattern.compile("\\d", Pattern.UNICODE_CHARACTER_CLASS)
        .matcher(arabicDigit).matches());                                     // true
System.out.println(java.util.Arrays.toString("a,b,".split(",", 0)));  // [a, b]
System.out.println(java.util.Arrays.toString("a,b,".split(",", -1))); // [a, b, ]
~~~

`String.split(regex,limit)` (и `Pattern.split`) при положительном limit применяет разделитель не более limit−1 раз; остаток попадает в последний элемент, даже если содержит разделители. При 0 удаляются только конечные пустые элементы; отрицательный limit сохраняет их. Положительной ширины совпадение в начале образует ведущую пустую строку, нулевой ширины в начале — нет. Если совпадения нет, результат содержит одну строку, весь исходный вход (даже `""`). Это отличается от `",".split(",",0)`, дающего массив длины 0. Согласуется с [CORE-STRINGS](strings.md). Для буквальной точки-разделителя нужен regex `\.`, в Java source `"\\."`.

| Выражение | Результат (кавычки отделяют пустые строки) |
|---|---|
| `",a,,".split(",",2)` | ["", "a,,"] |
| `",a,,".split(",",0)` | ["", "a"] |
| `",a,,".split(",",-1)` | ["", "a", "", ""] |
| `"abc".split("(?=a)",-1)` | ["abc"] |
| `"".split(",",0)` | [""] |
| `",".split(",",0)` | [] |

В Java 21 `String.splitWithDelimiters` и `Pattern.splitWithDelimiters` возвращают также совпавшие разделители между подстроками, например `"a,b,c".splitWithDelimiters(",",2)` → ["a", ",", "b,c"]. При положительном limit не более limit−1 применений шаблона, до 2×limit−1 элементов; при 0 удаляются конечные пустые строки, включая пустые разделители, при отрицательном сохраняются. Начальное zero-width совпадение не добавляет ни пустую начальную подстроку, ни пустой разделитель. Полная матрица разделителей не проверялась.

**Code point и графема.** Для supplementary `"😀"` длина String равна 2, число code points равно 1, consuming `.` находит диапазон 0:2. Для `"e\u0301"` длина и число code points равны 2, но это одна расширенная графема; `.` даёт два диапазона 0:1 и 1:2, `\X` — один 0:2.

~~~java
String combined = "e\u0301";
Matcher dot = Pattern.compile(".").matcher(combined);
while (dot.find()) System.out.println(dot.start() + ":" + dot.end());
// 0:1, 1:2
Matcher cluster = Pattern.compile("\\X").matcher(combined);
while (cluster.find()) System.out.println(cluster.start() + ":" + cluster.end());
// 0:2
~~~

Не каждый regex сохраняет границы графем: см. также нулевую длину в §3. Подсчёт UTF-16 единиц, code points и extended grapheme clusters отвечает на три разные задачи. Проверены только приведённые конечные случаи, не весь Unicode.

## 7. Ошибка Java, ошибка regex и время выполнения — три разных вопроса (RG-20, RG-21, RG-31)

| Где ошибка | Пример | Когда проявляется |
|---|---|---|
| Java lexical/compiler | Java source с недопустимым `"\d+"` или вызов `Pattern.group(1)` | Во время `javac`: неверный escape / отсутствующий метод |
| Regex grammar | `Pattern.compile("[")` | При исполнении `compile`: `PatternSyntaxException` |
| Matcher state | `Pattern.compile("a").matcher("a").group()` без успешного поиска | При исполнении: `IllegalStateException` |
| Replacement grammar | Подстановка `"$9"` без группы 9 | При обработке реально найденного совпадения: IndexOutOfBoundsException; при отсутствии совпадения этот пример возвращает исходный текст |
| Вход/производительность | Сложный backtracking-шаблон на длинном недоверенном вводе | Возможны большие затраты CPU; не эквивалентно Java syntax error |

`Pattern` реализует развитый regex-язык с backtracking. У некоторых сочетаний альтернатив, вложенных квантификаторов и почти-подходящих входов время проверки может резко увеличиваться (**ReDoS**). Пример класса риска: `(a+)+$` на длинной последовательности `a` с завершающим `!`, но **здесь мы не измеряли** зависимость времени от длины и не обещаем конкретную сложность или лимит в миллисекундах. У `Matcher` нет универсального стандартного timeout-параметра. Ограничение длины входа и шаблона, отказ от regex с произвольным пользовательским синтаксисом, упрощение неоднозначных выражений и тестирование на враждебных данных — инженерные меры, а не гарантия линейной работы любого regex. Поток с `Future.get(timeout)` сам по себе **не доказывает**, что вычисление остановлено; прерывание не является нормативной гарантией прекращения матчинга.

## 8. P1: расширенные контракты и пределы проверок (RG-32–RG-37)

**RG-32.** `usePattern` меняет шаблон, сохраняя позицию во входе и append position, но теряя информацию о группах последнего совпадения; это не reset. В следующем примере второй find продолжает после a и сохраняет append position: итог A-B-c, не повторно скопированный префикс.

~~~java
Matcher m = Pattern.compile("(a)").matcher("a-b-c");
StringBuilder out = new StringBuilder();
if (m.find()) {
    m.appendReplacement(out, "A");
    m.usePattern(Pattern.compile("(b)"));
    if (m.find()) m.appendReplacement(out, "B");
}
m.appendTail(out);
System.out.println(out); // A-B-c
~~~

`hitEnd()` сообщает, достиг ли движок конца при последнем поиске; true означает, что добавление текста потенциально изменит результат. `requireEnd()` при успешном совпадении означает возможность потерять его при добавлении текста; false не обещает неизменность длины или групп. После неуспешного поиска requireEnd не имеет содержательного значения. Проверенные find на входе `"a"`:

| Pattern | find | hitEnd | requireEnd |
|---|---|---|---|
| `a$` | true | true | true |
| `a+` | true | true | false |
| `ab` | false | true | не интерпретируем |

Эти флаги не образуют готовый потоковый парсер: управление буфером, завершением ввода и границами сообщений остаётся задачей вызывающего кода.

**RG-33.** Lookbehind проверяет левый контекст, atomic group фиксирует выбор внутри группы (§3, §6). Проверены отдельные короткие примеры, не все формы lookbehind и не сложность backtracking.

**RG-34.** UNICODE_CASE, CANON_EQ, Unicode properties и графемы разобраны в §4 и §6; каноническое сопоставление не сводится к String.equals и не нормализует результат. Полноценные многоязычные тесты, CLDR и цена CANON_EQ не проверялись.

**RG-35.** `Pattern.asPredicate()` проверяет наличие совпадения (find), `asMatchPredicate()` — весь вход (matches). `splitAsStream` выдаёт последовательность подстрок без завершающих пустых строк. `Matcher.results()` создаёт ленивый последовательный Java Stream результатов: поиск начинается при терминальной операции с текущего состояния, без автоматического reset. После ранее выполненного find следующий поиск продолжится с оставшейся позиции.

~~~java
Pattern digits = Pattern.compile("\\d+");
System.out.println(digits.asPredicate().test("id67")); // true
System.out.println(digits.asMatchPredicate().test("id67")); // false
System.out.println(Pattern.compile(",").splitAsStream("a,b,").toList()); // [a, b]
Matcher m = digits.matcher("a67b68c69");
if (m.find()) System.out.println(m.group()); // 67
System.out.println(m.results().map(MatchResult::group).toList()); // [68, 69]
~~~

Не меняйте Matcher во время обработки потока результатов; обнаружение изменения и ConcurrentModificationException — best effort, не средство синхронизации. После терминальной операции состояние поиска изменилось, для нового независимого поиска выполните reset и при необходимости заново задайте region. Java Stream не превращает Matcher в потокобезопасный объект; конкурентный и полнофайловый режимы не проверялись.

**RG-36.** `Pattern` документирован как immutable и безопасен для нескольких потоков; `Matcher` явно **не thread-safe**. Один Pattern можно разделять, а Matcher создавать на вызов/вход. Concurrency stress не выполнялся.

**RG-37.** Независимые anchoring и transparent bounds: матрица четырёх сочетаний на одном коротком входе (§6); не исчерпывающая проверка всех якорей, границ слов и концов строк.

## 9. Адресная карта G1: 31 P0 + 6 P1 + 4 P2

В таблице знак § указывает на раздел статьи, не отдельный исполненный тест. Исторические stdout `RG01–RG33` относятся к полному коду в [G1 §6.1](../../docs/plans/CORE-REGEX-UTIL.md); например RG08 использует имена word/n, а пример §3 — name и группу 2. RG28 относится к границе слова, не к примеру lookbehind (§6). Новые примеры не меняют смысла исторических ID. Отдельные проверки доработки описаны в §10.

| ID | Где разобран механизм / граница |
|---|---|
| RG-01 | § 1: Java escape и regex grammar |
| RG-02 | § 1–2: Pattern reuse и Matcher state |
| RG-03 | § 2: String.matches полное совпадение |
| RG-04 | § 2: Matcher.matches и region |
| RG-05 | § 2: lookingAt — префикс |
| RG-06 | § 2: find, start/end |
| RG-07 | § 2: повторный find/reset |
| RG-08 | § 2,7: group без successful match |
| RG-09 | § 3: named/capturing groups |
| RG-10 | § 3: noncapturing groupCount |
| RG-11 | § 3: greedy vs reluctant |
| RG-12 | § 3: possessive без отдачи backtracking |
| RG-13 | § 3: zero-width progression |
| RG-14 | § 4: ^/$ и MULTILINE |
| RG-15 | § 4: DOTALL |
| RG-16 | § 4: CASE_INSENSITIVE, Unicode caveat |
| RG-17 | § 4–5: Pattern.quote |
| RG-18 | § 5: replacement $1/$2 |
| RG-19 | § 5: Matcher.quoteReplacement |
| RG-20 | § 1,7: runtime PatternSyntaxException |
| RG-21 | § 5,7: некорректная ссылка на группу |
| RG-22 | § 5: replaceFirst vs replaceAll |
| RG-23 | § 6: \d и UNICODE_CHARACTER_CLASS |
| RG-24 | § 2: Matcher.reset |
| RG-25 | § 6: opaque vs transparent region |
| RG-26 | § 4: Pattern.LITERAL |
| RG-27 | § 4: COMMENTS |
| RG-28 | § 6: String.split и limit |
| RG-29 | § 6: supplementary Unicode / UTF-16 |
| RG-30 | § 3: lookahead |
| RG-31 | § 7: backtracking / ReDoS threat model |
| RG-32 | § 8 P1: usePattern, hitEnd, requireEnd |
| RG-33 | § 8 P1: lookbehind и atomic group |
| RG-34 | § 8 P1: Unicode flags и normalization |
| RG-35 | § 8 P1: Predicate/Stream APIs |
| RG-36 | § 2,8 P1: Pattern vs Matcher concurrency |
| RG-37 | § 6,8 P1: region bounds |
| RG-38 | P2: real ReDoS benchmark, mitigation experiments **NOT RUN** |
| RG-39 | P2: Unicode/CLDR/CORE-I18N **NOT RUN** |
| RG-40 | P2: полный backreferences/streaming/untrusted parsing **NOT RUN** |
| RG-41 | P2: альтернативные движки и lint plugins **NOT RUN** |

### Задачи на прогноз и диагностику

Попробуйте сначала получить ответ без запуска кода.

1. `Pattern.compile("a").matcher("aXaYa").region(2,4)`: какой индекс у первого find, а после reset и find? Что произойдёт, если после region(2,4) вызвать find(4)?
   **Разбор:** 2, затем 0; find(4) сбрасывает region и ищет с абсолютного индекса 4, находит a в 4. Чтобы снова ограничить поиск, надо явно задать region.
2. Шаблон `(a)?(b*)c` на `"c"`: одинаковы ли результаты group(1), group(2)? 
   **Разбор:** первая не участвовала — null, start=end=-1; вторая участвовала без символов — "", start=end=0. Проверка `group(1).isEmpty()` без проверки null ошибочна.
3. Клиент хочет заменить совпавшее `"a"` буквальным `"$9"`. Почему прямой replaceAll с этим replacement падает на `"a"`, но не на `"x"`?
   **Разбор:** грамматика replacement применяется к совпадению, которого во втором входе нет; передайте `Matcher.quoteReplacement("$9")`, тогда буквальное значение не будет ссылкой на группу. Если же нужно подставить группу 1 и буквальный суффикс, экранируйте только суффикс.
4. Что вернёт `",a,,".split(",",2)`, а при 0 и −1?
   **Разбор:** ["", "a,,"] — одно применение разделителя и остаток; ["", "a"] — конечные пустые отброшены; ["", "a", "", ""] — сохранены. Ведущая пустая не удаляется этим правилом. Пустой вход без совпадения даёт [""].

## 10. Источники A / интервью C / что не проверено

**A — нормативные гарантии Java 21:** [JLS string escapes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10.7), [Pattern](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html), [Matcher](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Matcher.html), [PatternSyntaxException](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/PatternSyntaxException.html), [String](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html). Код и наблюдения `RGxx` в [G1-плане](../../docs/plans/CORE-REGEX-UTIL.md) выполнены исторически на Debian OpenJDK **21.0.12.1**, 33 строки stdout, SHA-256 `aee7f4eb5f2e17050773b3ec4a07033bbc866040a6bffcaef7d142ff0b693f12`. Это **не** означает, что GitHub CI компилирует Java code fences.

**C — проверенные вопросы для ретроспективы:** [JavaInUse Regular Expressions Interview Questions](https://www.javainuse.com/misc/regex-interview-questions) спрашивает различие `find/matches`, классы Pattern/Matcher; [Java Zero to Advanced — regex interviews](https://sanjay-mathivanan.github.io/JAVA_Zero-to-Advanced/14_Regular-Expressions/06_Interview_Questions/) явно спрашивает thread-safety и catastrophic backtracking. Это широкие подсказки об ожидаемых уточнениях, **не** нормативная семантика и не статистика частот. Отдельные точные вопросы про `useTransparentBounds`, нулевую длину, named groups и replacement errors в прочитанных подборках не подтверждены (**C GAP**), но раскрыты по API. Сверка с [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md) и [планом G1](../../docs/plans/CORE-REGEX-UTIL.md) сделана до написания.

**Историческая хронология CI комплекта:** [технический #45162628699](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45162628699) — SUCCESS на `802a964becbd0933418ec20d61f35e83f0b77749`; [первый интегрированный #45163840909](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45163840909) — SUCCESS на `eb66aaf3baa21f2ed2eaee046622ab51389b09cc`; [интегрированный #45164089936](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45164089936) — SUCCESS на `804f4412ad5df1f081cb74e90602bef4fb3d3977`. Историческая проверка включает общую сборку и положительный запуск двух Java-программ (одна чистая компиляция Java 21, две JVM, два сравнения эталонов и SHA-256). Эти результаты относятся только к указанным SHA. CI последующей доработки фиксируется отдельно в её PR.

**Исторические отрицательные компиляционные проверки:** G1 §7 содержит 6 независимых RG_N01–RG_N06 (illegal Java escapes, неверная сигнатура String.matches, вызов group на Pattern, неправильная перегрузка group и тип результата) и восемь UT-сценариев. Их отдельно исполняли в G1 через `javac --release 21 -XDrawDiagnostics`; это не runtime errors и не часть позитивного stdout. Они не переименованы в проверки текущей доработки.

**Проверки D-A23a.** В [отчётах задачи #333](https://github.com/Melchiorius/backend-interview-labs/issues/333) зафиксированы новые независимые конечные проверки автора на Debian OpenJDK/javac 21.0.12.1: временный RemediationProbe.java, строгая компиляция `--release 21 -encoding UTF-8 -Xlint:all -Werror` и запуск JVM с явными UTF-8 параметрами, оба exit 0, без stderr. Проверены контрасты N01–N13, в том числе обе ветви RG-21 ($9 при наличии/отсутствии совпадения), malformed/named/numeric и функциональная замена. Отдельный IllegalEscape.java с настоящим недопустимым Java escape отклонён javac с `compiler.err.illegal.esc.char` (exit 1); Java-valid `Pattern.compile("[")` в положительной пробе вызвал ожидаемый PatternSyntaxException. Это не повтор исторических 14 отрицательных исходников и не 24 ожидаемых отклонения других проверок CI.

На тех же локальных Java/javac выполнен неизменённый общий regex-util runner на пяти точных входных blobs (две программы, два эталона и скрипт): одна чистая компиляция, две JVM с явным UTF-8, 33+38 строк и точные SHA-256 исторических эталонов, exit 0, пустой stderr и штатный маркер успеха (сам runner печатает маркер, а не все 71 строку). Новый временный исходник не добавлен в репозиторий и не исполняется штатным CI. Конечные пробы проверяют описанные контрасты, а не автоматически извлечённые и отдельно скомпилированные все inline blocks.

**Не проверено:** исчерпывающие Unicode/CLDR/Grapheme/Normalizer тесты, другие JDK/OS/движки, ReDoS benchmark/timeout/security audit, конкурентный stress, полноценный streaming parser, все P2, GUI Obsidian/IntelliJ и HTTP/rendered-anchor verification. Эти примеры не доказывают гарантированный timeout, линейную сложность или потокобезопасность Matcher.
