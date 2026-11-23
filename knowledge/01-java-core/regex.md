# CORE-REGEX — регулярные выражения Java 21: язык шаблона, Matcher, замены и границы безопасности

**Практическая задача.** В строке `user=67; role=admin` требуется извлечь два значения, проверить формат всего поля или заменить часть текста. Три операции похожи внешне, но имеют разные границы поиска и правила интерпретации. Особенно опасно перепутать **экранирование Java-строки** с **синтаксисом регулярного выражения**, а буквальное значение замены — с шаблоном подстановки групп.

Эта статья объясняет `java.util.regex.Pattern`, `Matcher`, методы `String`, Unicode и ограничения движка **Java SE 21 без preview**. Операторы pattern matching (`instanceof`, `switch`, record patterns) — самостоятельная [CORE-PATTERNS](patterns.md), не регулярные выражения. Основные смежные тексты: [CORE-STRINGS](strings.md) (`split`, `replaceAll`, UTF-16), [CORE-OPERATORS](operators.md) (Java-выражения), [CORE-EXCEPTIONS](exceptions.md) (runtime-исключения), [CORE-ARRAYS](arrays.md) (строки и массивы результатов).

**Нормативная опора:** [JLS 21 §3.10.7 — escape sequences](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10.7), [Pattern API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html), [Matcher API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Matcher.html), [PatternSyntaxException API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/PatternSyntaxException.html), [String API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html). Реальная исследовательская матрица и полный исторически исполненный Java21 source находятся в [принятом G1-плане](../../docs/plans/CORE-REGEX-UTIL.md), §3, §5–§7. **Покрытие:** RG-01–RG-31 = 31 P0; RG-32–RG-37 = 6 P1; RG-38–RG-41 = 4 P2. Метки обозначают исследованные вопросы, **не 41 unit test**.

**Самостоятельная практика G2:** [RegexProbe.java](../../examples/core/regex-util/src/RegexProbe.java), [33-строчный stdout-эталон](../../examples/core/regex-util/regex.expected.stdout) и [инструкция с `verify.sh`](../../examples/core/regex-util/README.md). В одном комплекте находится [UtilProbe.java](../../examples/core/regex-util/src/UtilProbe.java) с 38-строчным эталоном; **33+38=71 stdout-строка двух JVM, не 71 JUnit-тест**. Полные исполнимые G1 §6.1/§6.2 исходники и эталоны перенесены без изменения логики (по одной `// Theory:` строке перед каждым source). [Technical GitHub Actions #45162628699](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45162628699) — `completed/SUCCESS` строго на technical HEAD `802a964becbd0933418ec20d61f35e83f0b77749`; он подтверждает одну clean `javac --release 21 -encoding UTF-8 -Xlint:all`, две отдельные JVM, два полных `diff -u`, количества строк и G1 SHA-256. **Для следующего интегрированного авторского HEAD требуется отдельный CI; старый успех не переносится между SHA.** 14 compiler-negative G1 §7 (6 RG, 8 UT) проверены ролью 03 отдельно на OpenJDK 21.0.12.1, **не входят в штатный positive CI**. Никаких regex timeout, performance или гарантий линейного исполнения из этой проверки не следует.

## 1. Два парсера подряд: Java-литерал → Pattern (RG-01, RG-02, RG-20)

В исходном `"\\d+"` Java-компилятор сначала обрабатывает два символа `\\` как **один обратный слеш** в значении `String`. Затем `Pattern.compile` интерпретирует полученное `\d+` как последовательность одной или более цифр. Записать `"\d+"` **в Java source нельзя**: `\d` не является допустимой escape-последовательностью Java. Это ошибка компиляции, а не `PatternSyntaxException`. Внешняя строка, прочитанная из конфигурации или HTTP, уже прошла этот этап, и её не следует автоматически «удваивать» ещё раз.

~~~java
import java.util.regex.Pattern;

String regex = "\\d+";                // значение String: \d+
Pattern digits = Pattern.compile(regex); // компилируется синтаксис regex
System.out.println(digits.matcher("67").matches());   // true
System.out.println(digits.matcher("id=67").matches()); // false
~~~

`Pattern` — повторно используемое скомпилированное представление шаблона. Для каждого входа `pattern.matcher(input)` создаётся **отдельный Matcher**, который хранит изменяемое состояние поиска. `Pattern.compile("[")` — **корректный Java source**, но некорректный regex: при выполнении `compile` будет `PatternSyntaxException` (unchecked). Не стоит объявлять любое падение regex «ошибкой Java-компиляции». Обратный слеш в regex может относиться к классу символов (`\d`), границе (`\b`) или буквальному метасимволу (`\.`); `"\\b"` — regex word boundary, а `"\b"` — уже Java backspace.

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

`reset()` сбрасывает явное состояние поиска, а `find(int start)` дополнительно начинает поиск с указанной позиции и сбрасывает Matcher согласно API; это не «всегда следующий find». Один `Matcher` не является безопасным общим состоянием для конкурентных вызовов. Повторное использование `Pattern` не даёт права обмениваться одним `Matcher` между потоками.

## 3. Группы, нулевая длина, квантификаторы (RG-09–RG-13, RG-30)

Круглые скобки `(...)` **захватывают** подстроку; `group(0)` — всё совпадение, `group(1)` — первая захватывающая группа; `(?<id>...)` позволяет обращаться по имени, а `(?:...)` группирует без нового номера. Если необязательная группа не участвовала в успешном совпадении, её значение может быть `null`. Название группы не превращает `Matcher` в immutable record: значения читаются из состояния **последнего** поиска.

~~~java
Matcher m = Pattern.compile("(?<name>[a-z]+)=(\\d+)").matcher("user=67");
if (m.matches()) {
    System.out.println(m.group("name")); // user
    System.out.println(m.group(2));      // 67
    System.out.println(m.groupCount());   // 2
}
System.out.println(Pattern.compile("(?:a)+").matcher("aa").groupCount()); // 0
~~~

Жадный `.*` старается захватить как можно больше, отступая при необходимости; ленивый `.*?` пробует сначала короткое совпадение; possessive `.*+` **не отдаёт** захваченное backtracking-движку. Например, `Pattern.compile("a+a").matcher("aa").matches()` — `true`, а `Pattern.compile("a++a").matcher("aa").matches()` — `false`: possessive-квантификатор забрал обе `a` и не возвращает их второй части.

~~~java
System.out.println(Pattern.compile("<.*>").matcher("<a><b>").find()); // true
System.out.println(Pattern.compile("a++a").matcher("aa").matches());    // false
Matcher zero = Pattern.compile("(?=a)").matcher("aa"); // lookahead ничего не потребляет
while (zero.find()) System.out.println(zero.start() + ":" + zero.end());
// 0:0
// 1:1
~~~

`find()` умеет продолжать итерацию даже после совпадения **нулевой длины**; без этого самодельный цикл, вручную повторяющий поиск с прежнего индекса, мог бы зациклиться. Lookahead `(?=...)` проверяет, что впереди допускается шаблон, но **не потребляет** этот текст; обычный `\d+` после lookahead всё ещё нужен, если требуется включить цифры в совпадение.

## 4. Якоря и режимы Pattern (RG-14–RG-16, RG-26, RG-27)

`^` и `$` привязывают шаблон к началу/концу входа либо строк в `MULTILINE`. Без него `^67$` не станет успешным только потому, что `67` находится на отдельной строке среди других строк. `DOTALL` позволяет метасимволу `.` покрывать также разрывы строк; `MULTILINE` не включает DOTALL автоматически. `CASE_INSENSITIVE` меняет сравнение регистра, а правила полного Unicode-регистрового сопоставления дополнительно зависят от `UNICODE_CASE` (не выводите из одного ASCII-примера поведение всех письменностей).

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

`Pattern.LITERAL` объявляет **весь переданный текст шаблона** буквальным: `Pattern.compile("a.b", Pattern.LITERAL)` сопоставляет именно `a.b`, а не `axb`. `Pattern.COMMENTS` разрешает пробелы/комментарии **внутри regex grammar** по правилам Pattern; это не флаг Java-лексера и не обещание игнорировать пробелы внутри каждого класса символов. При сопоставлении пользовательского **буквального** фрагмента чаще проще `Pattern.quote(userInput)`, а не ручное экранирование каждого метасимвола.

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

`replaceFirst` заменяет только **первое** совпадение, `replaceAll` — все; `String.replace` не использует regex и замещает буквальные последовательности. Если в replacement написать `$9` при отсутствии соответствующей группы, во время выполнения возникнет исключение (например, `IndexOutOfBoundsException`); Java-компилятор не способен проверить значение replacement внутри `String`. При недоверенных пользовательских данных не конструируйте регулярное выражение или replacement конкатенацией без явного решения, должен ли ввод быть кодом шаблона или буквальным текстом. Подробности простых перегрузок — в [CORE-STRINGS](strings.md).

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

Результаты `start()/end()` и `String.length()` выражены в UTF-16 единицах, а не в «пользовательских символах». Regex API поддерживает дополнительные Unicode-конструкции, но не обещает автоматически разбить каждый видимый grapheme cluster на одну позицию. По умолчанию `\d` ориентирован на ASCII-цифры, а `UNICODE_CHARACTER_CLASS` расширяет поведение предопределённых классов:

~~~java
String arabicDigit = "٣"; // U+0663
System.out.println(Pattern.compile("\\d").matcher(arabicDigit).matches()); // false
System.out.println(Pattern.compile("\\d", Pattern.UNICODE_CHARACTER_CLASS)
        .matcher(arabicDigit).matches());                                     // true
System.out.println(java.util.Arrays.toString("a,b,".split(",", 0)));  // [a, b]
System.out.println(java.util.Arrays.toString("a,b,".split(",", -1))); // [a, b, ]
~~~

`String.split(regex,0)` отбрасывает **конечные** пустые элементы, `split(regex,-1)` сохраняет их; положительный `limit` ограничивает число элементов и оставляет остаток последним. Подробный контракт — в [CORE-STRINGS](strings.md). Для экранирования точки-разделителя нужен regex `\.`, который в Java source записывается как `"\\."`.

## 7. Ошибка Java, ошибка regex и время выполнения — три разных вопроса (RG-20, RG-21, RG-31)

| Где ошибка | Пример | Когда проявляется |
|---|---|---|
| Java lexical/compiler | Java source с недопустимым `"\d+"` или вызов `Pattern.group(1)` | Во время `javac`: неверный escape / отсутствующий метод |
| Regex grammar | `Pattern.compile("[")` | При исполнении `compile`: `PatternSyntaxException` |
| Matcher state | `Pattern.compile("a").matcher("a").group()` без успешного поиска | При исполнении: `IllegalStateException` |
| Replacement grammar | Подстановка `"$9"` без группы 9 | При исполнении `replaceAll`: ошибочная ссылка на группу |
| Вход/производительность | Сложный backtracking-шаблон на длинном недоверенном вводе | Возможны большие затраты CPU; не эквивалентно Java syntax error |

`Pattern` реализует развитый regex-язык с backtracking. У некоторых сочетаний альтернатив, вложенных квантификаторов и почти-подходящих входов время проверки может резко увеличиваться (**ReDoS**). Пример класса риска: `(a+)+$` на длинной последовательности `a` с завершающим `!`, но **здесь мы не измеряли** зависимость времени от длины и не обещаем конкретную сложность или лимит в миллисекундах. У `Matcher` нет универсального стандартного timeout-параметра. Ограничение длины входа и шаблона, отказ от regex с произвольным пользовательским синтаксисом, упрощение неоднозначных выражений и тестирование на враждебных данных — инженерные меры, а не гарантия линейной работы любого regex. Поток с `Future.get(timeout)` сам по себе **не доказывает**, что вычисление остановлено; прерывание не является нормативной гарантией прекращения матчинга.

## 8. P1: где углубляться, не выдавая это за лабораторную проверку (RG-32–RG-37)

**RG-32.** `Matcher.usePattern` меняет Pattern без создания нового Matcher; `find(start)` сбрасывает состояние, `hitEnd()`/`requireEnd()` помогают судить, могло ли дополнение входа изменить результат. Это не полноценный streaming parser «из коробки», наблюдаемая трасса не запускалась.

**RG-33.** Lookbehind `(?<=...)` проверяет текст слева, atomic group `(?>...)` ограничивает backtracking; отличие от possessive-квантификатора определяется грамматикой и областью атомарности. Полный набор граничных экспериментов не исполнялся.

**RG-34.** `UNICODE_CASE`, `CANON_EQ`, Unicode properties (`\p{...}`) и Unicode normalization — отдельные аспекты. Каноническая эквивалентность не сводится к сравнению `String.equals`; многоязычные тесты и цена CANON_EQ **NOT RUN**.

**RG-35.** `Pattern.asPredicate()` соответствует поиску через `find`, `asMatchPredicate()` — полному совпадению; `splitAsStream` и `Matcher.results()` дают потоковые формы API. Поток результатов не делает изменяемый Matcher потокобезопасным; полнофайловые прогоны не сделаны.

**RG-36.** `Pattern` документирован как immutable и безопасен для нескольких потоков; `Matcher` явно **не thread-safe**. Один Pattern можно разделять, а Matcher создавать на вызов/вход. Concurrency stress не выполнялся.

**RG-37.** `useAnchoringBounds` и `useTransparentBounds` должны рассматриваться отдельно: невидимые символы за region влияют на lookaround только в transparent-режиме; `^`/`$` зависят от anchoring bounds. Наблюдён один узкий пример, не все сочетания.

## 9. Адресная карта G1: 31 P0 + 6 P1 + 4 P2

В таблице знак § указывает на раздел статьи, **не отдельный исполненный тест**. Исторические stdout `RG01–RG33` относятся к полному коду в [G1 §6.1](../../docs/plans/CORE-REGEX-UTIL.md); дополнительно опубликован самостоятельный runnable G2 с точной проверкой двух JVM и stdout-эталонов; для каждого изменения авторского SHA отдельно требуется собственный CI.

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

## 10. Источники A / интервью C / что не проверено

**A — нормативные гарантии Java 21:** [JLS string escapes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10.7), [Pattern](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html), [Matcher](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Matcher.html), [PatternSyntaxException](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/PatternSyntaxException.html), [String](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html). Код и наблюдения `RGxx` в [G1-плане](../../docs/plans/CORE-REGEX-UTIL.md) выполнены исторически на Debian OpenJDK **21.0.12.1**, 33 строки stdout, SHA-256 `aee7f4eb5f2e17050773b3ec4a07033bbc866040a6bffcaef7d142ff0b693f12`. Это **не** означает, что GitHub CI компилирует Java code fences.

**C — проверенные вопросы для ретроспективы:** [JavaInUse Regular Expressions Interview Questions](https://www.javainuse.com/misc/regex-interview-questions) спрашивает различие `find/matches`, классы Pattern/Matcher; [Java Zero to Advanced — regex interviews](https://sanjay-mathivanan.github.io/JAVA_Zero-to-Advanced/14_Regular-Expressions/06_Interview_Questions/) явно спрашивает thread-safety и catastrophic backtracking. Это широкие подсказки об ожидаемых уточнениях, **не** нормативная семантика и не статистика частот. Отдельные точные вопросы про `useTransparentBounds`, нулевую длину, named groups и replacement errors в прочитанных подборках не подтверждены (**C GAP**), но раскрыты по API. Сверка с [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md) и [планом G1](../../docs/plans/CORE-REGEX-UTIL.md) сделана до написания.

**Компиляционные negatives:** принятый G1 §7 содержит **6 независимых RG_N01–RG_N06** (illegal Java escapes, неверная сигнатура String.matches, вызов group на Pattern, неправильная перегрузка group и тип результата). Они действительно исполнялись отдельно исследовательской ролью в G1 через `javac --release 21 -XDrawDiagnostics`; это **не** шесть runtime errors и **не** часть позитивного stdout. Здесь не утверждается повторное выполнение отрицательных проверок ролью 02.

**NOT RUN этой статьёй:** автоматическая компиляция всех inline Java blocks, локальный JDK21 запуск ролью 02, другие Java vendors/OS, полноценные Unicode/CLDR/Grapheme/Normalizer тесты, Regex ReDoS benchmark/timeout/security audit, конкаррентный stress, все P2, GUI Obsidian/IntelliJ, HTTP/anchor verification. `CORE-I18N` и `CORE-SERIAL`, а также расширенный `COL-SORTED`, **не объявляются реализованными**. Статусы [COVERAGE](../../docs/COVERAGE.md) и [CURRICULUM](../../docs/CURRICULUM.md) обновляются только после приёмки и G5/G6.
