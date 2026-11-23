# CORE-REGEX + CORE-UTIL — G1 исследовательский план Java SE 21

**G1 PROPOSED / NOT ACCEPTED.** Роль **01 — Research & Coverage**. [Issue #191](https://github.com/Melchiorius/backend-interview-labs/issues/191), [адресное поручение 00 #7097955818](https://github.com/Melchiorius/backend-interview-labs/issues/191#issuecomment-7097955818), [программа #33](https://github.com/Melchiorius/backend-interview-labs/issues/33). **Исходный BASE поручения:** `3340404a24f4c1175414a873e567ea93a6049c7f` после G5 [PR #188](https://github.com/Melchiorius/backend-interview-labs/pull/188). **Текущий BASE/main при создании ветки:** `cdabdd3561db435cedae02cd92c8f4d63e18e825` после параллельного G6 [PR #192](https://github.com/Melchiorius/backend-interview-labs/pull/192), закрывающего status-only [Issue #190](https://github.com/Melchiorius/backend-interview-labs/issues/190). Этот G1 **не меняет** G6 и не приписывает себе работу 06. Exact HEAD/CI — в PR и адресном review-комментарии.

**Среда локальных проверок:** OpenJDK `javac/java 21.0.12.1+1-1-deb13u1`, Debian GNU/Linux 13; нормы и примеры **Java SE 21 без preview**. Полные Java source и stdout появляются **в `6 ниже**; G1 markdown НЕ исполняется проектным CI автоматически.

## 1. Две независимые темы и перекрытия

**CORE-REGEX:** Java source escaping → regex Pattern syntax → stateful Matcher (matches/find/lookingAt, groups, boundaries, flags, replacements, Unicode), runtime failure и performance/security границы. `Pattern matching` в switch/instanceof Java21 — **другая** тема, не regex.

**CORE-UTIL:** Objects (null/deep equality/index), Comparator (key extraction, ordering and null semantics), Arrays (comparison/sort/search/backed views). `COL-SORTED/COL-CONTRACTS` не объявлены Implemented из-за TreeSet/HashSet мини-примеров. В G2 рекомендуются **две самостоятельные статьи** `knowledge/01-java-core/regex.md` и `knowledge/01-java-core/utilities.md` (пока **НЕ опубликованы** — только inline names, **не** Markdown links), при необходимости две автономные Java21 программы из этого плана. G1 — строго один `docs/plans/CORE-REGEX-UTIL.md`, не трогать `knowledge/`, `examples/`, root README, Gradle/CI/workflow, `COVERAGE/CURRICULUM`.

После G5 #188 37 самостоятельных Java Core статей покрывают 38/42 ID; после G6 #192 реестры синхронизированы. Остальные ещё не реализованные ID — `CORE-REGEX`, `CORE-UTIL`, `CORE-I18N`, `CORE-SERIAL`. Два последних **не входят** в эту волну.

| Реальная опубликованная статья | Уже доказанная глубина | Что добавляет REGEX/UTIL |
|---|---|---|
| [CORE-STRINGS](../../knowledge/01-java-core/strings.md) | `String.split(regex,limit)`, `replace vs replaceAll`, UTF-16 | Pattern/Matcher lifecycle, groups, replacement escaping, zero-width |
| [CORE-EQUALITY](../../knowledge/01-java-core/equality.md) | `Objects.equals`, equals/hashCode vs compareTo | Objects.deepEquals/hash/checkIndex, Comparator chaining/null |
| [CORE-ARRAYS](../../knowledge/01-java-core/arrays.md) | shallow copies, asList backed view, basic sort/binarySearch | deepEquals/mismatch/compare, precise search/ordering contracts |
| [CORE-OPERATORS](../../knowledge/01-java-core/operators.md) | Java source/operators, numbers | Java literal escaping до regex syntax; compile vs runtime |
| [CORE-IMMUTABLE](../../knowledge/01-java-core/immutability.md) | List.copyOf snapshot vs unmodifiableList live wrapper | API aliasing applied to utils, not restating entire class |
| [CORE-EXCEPTIONS](../../knowledge/01-java-core/exceptions.md) | checked/unchecked | PatternSyntaxException, NPE, UOE, IOOBE as concrete runtime |
| [CORE-FUNCTIONS](../../knowledge/01-java-core/functions.md) | lambdas/SAM/method refs | Comparator.comparing and thenComparingInt |

Стандарты работы: [AGENTS](../../AGENTS.md), [ROLE_MODEL](../ROLE_MODEL.md), [WORKFLOW](../WORKFLOW.md), [JAVA_CORE_PLAN](../JAVA_CORE_PLAN.md), [SOURCE_REGISTRY](../SOURCE_REGISTRY.md), [EXERCISE_AND_TESTING_STANDARD](../EXERCISE_AND_TESTING_STANDARD.md), [CURRICULUM](../CURRICULUM.md), [COVERAGE](../COVERAGE.md).

## 2. JLS/Javadoc Java21 (A) и реальные interview questions (C)

| A | Проверенная нормативная Java 21 спецификация |
|---|---|
| R1 | [Pattern Javadoc21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html): syntax, groups, flags, quantifiers, Unicode, quoting and LITERAL |
| R2 | [Matcher Javadoc21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Matcher.html): matches/find/lookingAt, mutable state, region/bounds/reset, group, replacement quoting |
| R3 | [PatternSyntaxException21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/PatternSyntaxException.html): invalid regex → unchecked runtime |
| R4 | [JLS21 `3.10.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10.7): Java escape sequence before regex engine |
| R5 | [String Javadoc21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html): matches/replaceAll/replaceFirst/split |
| U1 | [Objects Javadoc21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html): equals/deepEquals/hash/requireNonNull/checkIndex/checkFrom* |
| U2 | [Comparator Javadoc21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html): consistent with equals, comparing/thenComparing/nulls/reversed |
| U3 | [Arrays Javadoc21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html): equals/deepEquals/mismatch/compare/sort/binarySearch/asList |
| U4 | [List Javadoc21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html), [Collections Javadoc21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html), [TreeSet Javadoc21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeSet.html) |

**C-провенанс (не считать вторичные тексты технической нормой):** **C1 Q-HRUDAY** [README](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md) раздел Arrays, реальные вопросы 13–17 (Arrays.sort, binarySearch, sorted precondition) и Arrays.asList вопросы 2–5 (fixed-size, List.of); **C2 Q-HRUDAY** тот же README `9.25 Comparator реальные 1–7/12–13: Comparator/Comparable, comparing, lambda/sort; **C3 Q-CHERNYSHOV** [English README](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/master/eng/README.md) реальные №200/444 про Collections.unmodifiableList и №449/450 про Comparable/Comparator. **C4 (дополнительный банк ВНЕ реестра)** [JavaInUse Regex Interview Questions](https://www.javainuse.com/misc/regex-interview-questions): Pattern/Matcher, find vs matches и ошибки; **C5 (дополнительный ВНЕ реестра)** [Java: Zero to Advanced Regex Interview Questions](https://sanjay-mathivanan.github.io/JAVA_Zero-to-Advanced/14_Regular-Expressions/06_Interview_Questions/): Q1 Pattern vs Matcher thread-safety, Q2 find vs matches и backtracking. Эти формулировки прочитаны; узкие `useTransparentBounds`/`nullsFirst(null)`/`Arrays.mismatch` **не подтверждены прямым C**, поэтому `C GAP` ниже — **не выдуманные вопросы**.

## 3. CORE-REGEX — 31 обязательный P0

`RGxx` — stdout `RegexProbe.java`; `RG_Nxx` — отдельный compiler-negative. В каждом P0 A норма, C — реально проверенный широкий вопрос либо честный GAP.

| ID | Требуемый механизм, не только название | A | C | Проверяемый след/граница |
|---|---|---|---|---|
| `RG-01` | Java-строка `"\\d+"` превращается в regex `\d+`; illegal Java escape ≠ PatternSyntaxException | R1,R4 | C4 | RG01; RG_N01/02 |
| `RG-02` | Pattern.compile даёт повторно применимый Pattern, Matcher привязан к input и состоянию | R1,R2 | C4 | RG02 |
| `RG-03` | String.matches требует полного совпадения, не ищет подстроку | R1,R5 | C4,C5 | RG03=false |
| `RG-04` | Matcher.matches требует полного совпадения с region | R2 | C4 | RG04=true |
| `RG-05` | Matcher.lookingAt сопоставляет префикс region | R2 | C4 | RG05=true |
| `RG-06` | Matcher.find с start/end [start,end) извлекает подстроки и позиции | R2 | C4 | RG06 |
| `RG-07` | Повторные find и Matcher.reset управляют mutable состоянием | R2 | C4 (шир.) | RG06,RG27 |
| `RG-08` | Matcher.group до успешного find/matches → runtime IllegalStateException | R2 | C GAP | RG07 |
| `RG-09` | Именованные и числовые capturing groups, индексы/значения | R1,R2 | C4 (шир.) | RG08 |
| `RG-10` | Noncapturing `(?:...)` не увеличивает groupCount | R1,R2 | C GAP | RG09=0 |
| `RG-11` | Greedy vs reluctant quantifiers дают разные границы | R1 | C5 (шир.) | RG10 |
| `RG-12` | Possessive `++` запрещает backtracking внутри квантификатора | R1 | C5 (шир.) | RG11=false |
| `RG-13` | Zero-width find iteration корректно продвигается | R1,R2 | C GAP | RG12=[0,1] |
| `RG-14` | ^ и $ + MULTILINE для границ строк | R1 | C GAP | RG13/14 |
| `RG-15` | DOTALL меняет поведение точки на переводе строки | R1 | C GAP | RG15/16 |
| `RG-16` | CASE_INSENSITIVE и отдельные нюансы Unicode case | R1 | C GAP | RG17; UNICODE_CASE not run |
| `RG-17` | Pattern.quote для буквального regex source (не replacement) | R1 | C4 (шир.) | RG18/19 |
| `RG-18` | В replacement `$1/$2` ссылаются на captured groups | R2 | C4 (шир.) | RG20=67:user |
| `RG-19` | Matcher.quoteReplacement экранирует `$` и `\` в replacement, не regex | R2 | C GAP | RG21 |
| `RG-20` | Pattern.compile("[") синтаксически Java-valid, runtime PatternSyntaxException | R1,R3 | C4 (шир.) | RG22 |
| `RG-21` | Недопустимая ссылка `$9` на группу → runtime exception | R2 | C GAP | RG23 |
| `RG-22` | replaceFirst vs replaceAll/замена внутри String | R2,R5 | C4 (шир.) | RG24; CORE-STRINGS bridge |
| `RG-23` | \d default vs UNICODE_CHARACTER_CLASS с арабской цифрой | R1 | C GAP | RG25/26 |
| `RG-24` | Matcher.reset возвращает исходную позицию поиска | R2 | C GAP | RG27 |
| `RG-25` | Opaque vs transparent region bounds изменяют boundary matching | R2 | C GAP | RG28 |
| `RG-26` | Pattern.LITERAL рассматривает regex source буквальным образом | R1 | C GAP | RG29 |
| `RG-27` | COMMENTS пропускает whitespace/комментарии в regex по API | R1 | C GAP | RG30 |
| `RG-28` | String.split(regex,limit): trailing empty при 0 vs -1 | R1,R5 | C4 (шир.) | RG31; уже CORE-STRINGS |
| `RG-29` | Supplementary Unicode codepoint может занимать два UTF-16 code units | R1,R2 | C GAP | RG32 |
| `RG-30` | Lookahead проверяет условие без потребления, consuming token отдельно | R1 | C GAP | RG33 |
| `RG-31` | Риск worst-case backtracking/ReDoS при недоверенном regex; нет универсального API timeout | R1,R2 | C5 | threat model; runtime benchmark NOT RUN |

## 4. CORE-UTIL — 29 обязательных P0

`UTxx` — stdout `UtilProbe.java`; `UT_Nxx` — отдельный compiler-negative. Где использованы TreeSet/HashSet — это **узкий мост** к Collections, не объявление COL-SORTED готовым.

| ID | Механизм | A | C | След/контрпример |
|---|---|---|---|---|
| `UT-01` | Objects.equals null-safe; deepEquals массивов не равен identity equality | U1,U3 | C GAP | UT01–UT03 |
| `UT-02` | Objects.hashCode(single) vs Objects.hash(varargs) разные значения | U1 | C GAP | UT04=67:98 |
| `UT-03` | Objects.toString(null,defaultValue) fallback | U1 | C GAP | UT05 |
| `UT-04` | requireNonNull с String сообщением → runtime NPE, не javac-negative | U1 | C GAP | UT06 |
| `UT-05` | requireNonNull Supplier<String> ленив при nonnull, при null вызывается | U1 | C GAP | UT07/08 |
| `UT-06` | Objects.checkIndex валиден внутри [0,length), иначе IOOBE | U1 | C GAP | UT09/10 |
| `UT-07` | checkFromToIndex vs checkFromIndexSize — разные аргументы границ | U1 | C GAP | UT11/12 |
| `UT-08` | comparing+thenComparingInt — лексикографическая композиция ключей | U2 | C2,C3 | UT13 |
| `UT-09` | Comparator.reversed разворачивает всю композицию | U2 | C2 (шир.) | UT14 |
| `UT-10` | Comparator.nullsFirst/nullsLast сортирует null elements | U2 | C2 (шир.) | UT15/16 |
| `UT-11` | Разворот nullsFirst переносит null в конец | U2 | C GAP | UT17 |
| `UT-12` | comparing(keyExtractor) при null key и natural comparator → NPE | U2 | C GAP | UT18 |
| `UT-13` | Comparator inconsistent with equals: TreeSet считает A/a одним | U2,U4 | C2,C3 | UT19 |
| `UT-14` | Comparator.nullsFirst(null) считает все nonnull эквивалентными | U2 | C GAP | UT20 |
| `UT-15` | Arrays.equals primitive arrays сравнивает элементы | U3 | C1 (шир.) | UT21 |
| `UT-16` | Arrays.deepEquals(Object[]) рекурсивно сравнивает вложенные int[] | U3 | C GAP | UT22/23 |
| `UT-17` | Arrays.mismatch индекс первого несовпадения, -1 при равенстве | U3 | C GAP | UT24 |
| `UT-18` | Arrays.compare лексикографический порядок | U3 | C GAP | UT25 |
| `UT-19` | Arrays.sort(int[]) сортировка primitive overload | U3 | C1 | UT26 |
| `UT-20` | binarySearch требует заранее sorted; hit / negative insertion encoding | U3 | C1 | UT27/28 |
| `UT-21` | Arrays.asList(String[]) backed view: aliasing в обе стороны | U3,U4 | C1 | UT29/30 |
| `UT-22` | Arrays.asList фиксированного размера; add → UOE runtime | U3,U4 | C1 | UT31 |
| `UT-23` | Arrays.asList(int[]) → List<int[]> длины 1, не List<Integer> | U3,U4 | C1 | UT32; UT_N05 |
| `UT-24` | List.of запрещает null и set/add | U4 | C1 | UT33/34 |
| `UT-25` | unmodifiableList live wrapper vs List.copyOf structural snapshot | U4 | C3 | UT35 |
| `UT-26` | List.copyOf shallow: mutable element сохраняет identity | U4 | C3 (шир.) | UT36 |
| `UT-27` | HashSet uses equals/hashCode, TreeSet uses Comparator equivalence | U2,U4 | C3 (шир.) | UT37,UT19; COL-SORTED bridge |
| `UT-28` | Arrays.sort(T[],Comparator) выполняет порядок компаратора | U2,U3 | C1,C2 | UT38 |
| `UT-29` | Arrays.sort(object array) stability = Javadoc contractual, не обещать алгоритм primitive | U3 | C1 (шир.) | API guaranteed; equal-key trace NOT RUN |

**Итого 60 P0 (31 RG, 29 UT).** Это требования к будущим статьям, не 60 готовых разделов и не 60 JUnit tests; 04 вправе осмысленно объединять дубли по нормам. Банк C подтверждает *широкие вопросы*, а не все 60 узких формулировок.

## 5. P1/P2 — обоснованная будущая глубина, NOT Implemented

| ID | Приоритет | Что не подтверждено текущим исследованием |
|---|---|---|
| `RG-32` | P1 | usePattern/find(start)/hitEnd/requireEnd streaming |
| `RG-33` | P1 | lookbehind, atomic groups; not run |
| `RG-34` | P1 | UNICODE_CASE/CANON_EQ/Unicode properties, not run |
| `RG-35` | P1 | asPredicate/asMatchPredicate/splitAsStream/Matcher.results, not run |
| `RG-36` | P1 | Pattern thread-safe vs Matcher not thread-safe, normative, concurrency run NOT DONE |
| `RG-37` | P1 | anchoring/transparent bounds edge cases; only one RG28 tested |
| `UT-30` | P1 | Arrays.parallelSort/parallelPrefix not run |
| `UT-31` | P1 | Arrays.fill/setAll/parallelSetAll aliasing, not run |
| `UT-32` | P1 | Objects.requireNonNullElse/ElseGet, not run |
| `UT-33` | P1 | check* long overloads, overflow endpoints and message wording, not run |
| `UT-34` | P1 | comparingDouble NaN/signed zero and locale collation, not run |
| `UT-35` | P1 | Comparator contract violations and optional sort IAE, not guaranteed |
| `UT-36` | P1 | Arrays.copyOf/copyOfRange shallow, already CORE-ARRAYS |
| `RG-38` | P2 | Real ReDoS throughput, timeouts/JMH, not run |
| `RG-39` | P2 | Unicode normalization/CLDR CORE-I18N, not run |
| `RG-40` | P2 | Full backreferences/streaming parsing and untrusted input, not run |
| `RG-41` | P2 | Alternate regex engines/static lint plugins, not run |
| `UT-37` | P2 | Parallel sorting JMM/concurrency/performance, not run |
| `UT-38` | P2 | JDK implementation algorithms/vendor optimizations, not run |
| `UT-39` | P2 | Full Collections contracts COL-SORTED, not addressed |
| `UT-40` | P2 | Collator/locale aware sorting CORE-I18N, not run |
| `UT-41` | P2 | Deep graph copy/serialization CORE-SERIAL, not run |

**Всего 60 P0 / 13 P1 / 9 P2.** CORE-I18N и CORE-SERIAL остаются отдельными следующими G1-кандидатами, не входят в PR.

## 6. Два положительных Java21 probe и реальные эталоны

Локально выполнены `javac --release 21 -Xlint:all RegexProbe.java UtilProbe.java` (**exit 0, stderr пуст, 0 warnings**), затем два `java` (**exit 0**), и повторно `diff -u` каждого stdout со своим реально полученным эталоном (**2/2 exit 0**). **33 RG + 38 UT = 71 stdout строка** — не 71 JUnit tests; JDK 21.0.12.1 Debian 13. Файлы **НЕ добавлены в G1 git tree как Java**, полные source и stdout приводятся в следующих разделах 6.1–6.2. GitHub Actions не компилирует fenced source автоматически.

### 6.1 RegexProbe.java (полный Java21 source)

~~~java
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexProbe {
    public static void main(String[] args) {
        String regex = "\\d+";
        System.out.println("RG01=" + regex);
        System.out.println("RG02=" + Pattern.compile(regex).matcher("ID67").find());
        System.out.println("RG03=" + "ID67".matches(regex));
        System.out.println("RG04=" + Pattern.compile(regex).matcher("67").matches());
        System.out.println("RG05=" + Pattern.compile(regex).matcher("67x").lookingAt());
        Matcher digits = Pattern.compile(regex).matcher("a67b8");
        List<String> locations = new ArrayList<>();
        while (digits.find()) locations.add(digits.group() + "@" + digits.start() + ":" + digits.end());
        System.out.println("RG06=" + locations);
        try { Pattern.compile("(a)").matcher("a").group(); }
        catch (IllegalStateException ex) { System.out.println("RG07=IllegalStateException"); }
        Matcher named = Pattern.compile("(?<word>[a-z]+)-(?<n>\\d+)").matcher("abc-67");
        System.out.println("RG08=" + named.matches() + ":" + named.group("word") + ":" + named.group(2));
        System.out.println("RG09=" + Pattern.compile("(?:ab)+").matcher("abab").groupCount());
        Matcher greedy = Pattern.compile("a.+b").matcher("a1b2b"); greedy.find();
        Matcher reluctant = Pattern.compile("a.+?b").matcher("a1b2b"); reluctant.find();
        System.out.println("RG10=" + greedy.group() + ":" + reluctant.group());
        System.out.println("RG11=" + Pattern.compile("a.++b").matcher("a1b2b").find());
        Matcher zero = Pattern.compile("(?=a)").matcher("aa");
        List<Integer> zeros = new ArrayList<>(); while (zero.find()) zeros.add(zero.start());
        System.out.println("RG12=" + zeros);
        System.out.println("RG13=" + Pattern.compile("^cat$").matcher("dog\ncat").find());
        System.out.println("RG14=" + Pattern.compile("^cat$", Pattern.MULTILINE).matcher("dog\ncat").find());
        System.out.println("RG15=" + Pattern.compile("a.b").matcher("a\nb").matches());
        System.out.println("RG16=" + Pattern.compile("a.b", Pattern.DOTALL).matcher("a\nb").matches());
        System.out.println("RG17=" + Pattern.compile("abc", Pattern.CASE_INSENSITIVE).matcher("ABC").matches());
        System.out.println("RG18=" + Pattern.compile(Pattern.quote("a.b")).matcher("a.b").matches());
        System.out.println("RG19=" + Pattern.compile("a.b").matcher("axb").matches());
        System.out.println("RG20=" + "user-67".replaceAll("([a-z]+)-(\\d+)", "$2:$1"));
        System.out.println("RG21=" + "67".replaceAll("\\d+", Matcher.quoteReplacement("$\\literal")));
        try { Pattern.compile("["); } catch (PatternSyntaxException ex) { System.out.println("RG22=PatternSyntaxException:" + ex.getIndex()); }
        try { "a".replaceAll("(a)", "$9"); }
        catch (RuntimeException ex) { System.out.println("RG23=" + ex.getClass().getSimpleName()); }
        System.out.println("RG24=" + "a67b".replaceFirst("\\d+", "X"));
        System.out.println("RG25=" + Pattern.compile("\\d").matcher("٣").matches());
        System.out.println("RG26=" + Pattern.compile("\\d", Pattern.UNICODE_CHARACTER_CLASS).matcher("٣").matches());
        Matcher reset = Pattern.compile("a").matcher("aba"); reset.find(); int start = reset.start(); reset.reset(); reset.find();
        System.out.println("RG27=" + start + ":" + reset.start());
        Matcher opaque = Pattern.compile("\\bword").matcher("xxword").region(2,6);
        boolean before = opaque.find(); opaque.reset().region(2,6).useTransparentBounds(true); boolean after = opaque.find();
        System.out.println("RG28=" + before + ":" + after);
        System.out.println("RG29=" + Pattern.compile("a.b", Pattern.LITERAL).matcher("a.b").matches());
        System.out.println("RG30=" + Pattern.compile("a #comment\nb", Pattern.COMMENTS).matcher("ab").matches());
        System.out.println("RG31=" + "a,,b,".split(",").length + ":" + "a,,b,".split(",", -1).length);
        Matcher emoji = Pattern.compile(".").matcher("😀"); emoji.find();
        System.out.println("RG32=" + emoji.group().length() + ":" + emoji.end());
        System.out.println("RG33=" + Pattern.compile("(?=\\d{2})\\d").matcher("67").find());
    }
}
~~~

**Фактический stdout RegexProbe (33 строки, UTF-8/LF):**
~~~text
RG01=\d+
RG02=true
RG03=false
RG04=true
RG05=true
RG06=[67@1:3, 8@4:5]
RG07=IllegalStateException
RG08=true:abc:67
RG09=0
RG10=a1b2b:a1b
RG11=false
RG12=[0, 1]
RG13=false
RG14=true
RG15=false
RG16=true
RG17=true
RG18=true
RG19=true
RG20=67:user
RG21=$\literal
RG22=PatternSyntaxException:0
RG23=IndexOutOfBoundsException
RG24=aXb
RG25=false
RG26=true
RG27=0:0
RG28=true:false
RG29=true
RG30=true
RG31=3:4
RG32=2:2
RG33=true
~~~

**SHA-256(RegexProbe.java):** `a07ec4cae572f134626defdc1fe07a8b3aae0bfbb8781a972297237c2c09dabb`. **SHA-256(regex.stdout):** `aee7f4eb5f2e17050773b3ec4a07033bbc866040a6bffcaef7d142ff0b693f12`.

**Пояснение:** `RG01` — строка regex, полученная после Java source parsing. `RG03/04/05` проверяют full-string vs prefix. `RG07` group до match, `RG22` неверный шаблон, `RG23` несуществующая group в replacement — это **runtime**, не compiler errors. `RG28` зависит от region boundaries, `RG25/26` от Unicode flag, `RG32` показывает UTF-16 индекс 2 для supplementary code point, `RG31` — bridge к CORE-STRINGS. Ни одного недоказанного benchmark о catastrophic backtracking.

### 6.2 UtilProbe.java (полный Java21 source)

~~~java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public class UtilProbe {
    record Person(String name, int age) { public String toString() { return name + ":" + age; } }
    public static void main(String[] args) {
        System.out.println("UT01=" + Objects.equals(null, null));
        int[] x = {67, 1}, y = {67, 1};
        System.out.println("UT02=" + Objects.deepEquals(x, y));
        System.out.println("UT03=" + Objects.equals(x, y));
        System.out.println("UT04=" + Objects.hashCode(67) + ":" + Objects.hash(67));
        System.out.println("UT05=" + Objects.toString(null, "fallback"));
        try { Objects.requireNonNull(null, "required"); }
        catch (NullPointerException ex) { System.out.println("UT06=NullPointerException:" + ex.getMessage()); }
        int[] calls={0};
        Objects.requireNonNull("ok", () -> { calls[0]++; return "lazy"; });
        System.out.println("UT07=" + calls[0]);
        try { Objects.requireNonNull(null, () -> { calls[0]++; return "lazy"; }); }
        catch (NullPointerException ex) { System.out.println("UT08=" + calls[0] + ":" + ex.getMessage()); }
        System.out.println("UT09=" + Objects.checkIndex(2, 3));
        try { Objects.checkIndex(3,3); }
        catch (IndexOutOfBoundsException ex) { System.out.println("UT10=IndexOutOfBoundsException"); }
        System.out.println("UT11=" + Objects.checkFromToIndex(1,3,3));
        try { Objects.checkFromIndexSize(1,3,3); }
        catch (IndexOutOfBoundsException ex) { System.out.println("UT12=IndexOutOfBoundsException"); }
        List<Person> people = new ArrayList<>(List.of(new Person("Bob",30),new Person("Ada",67),new Person("Ada",20)));
        Comparator<Person> cmp = Comparator.comparing(Person::name).thenComparingInt(Person::age);
        people.sort(cmp);
        System.out.println("UT13=" + people);
        people.sort(cmp.reversed());
        System.out.println("UT14=" + people);
        List<String> nullable = new ArrayList<>(Arrays.asList("B",null,"A"));
        nullable.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
        System.out.println("UT15=" + nullable);
        nullable.sort(Comparator.nullsLast(Comparator.naturalOrder()));
        System.out.println("UT16=" + nullable);
        nullable.sort(Comparator.nullsFirst(Comparator.<String>naturalOrder()).reversed());
        System.out.println("UT17=" + nullable);
        try { Comparator.comparing(Person::name).compare(new Person(null,1),new Person("Ada",2)); }
        catch (NullPointerException ex) { System.out.println("UT18=NullPointerException"); }
        TreeSet<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.add("A"); caseInsensitive.add("a");
        System.out.println("UT19=" + caseInsensitive.size());
        TreeSet<String> odd = new TreeSet<>(Comparator.nullsFirst((Comparator<String>) null));
        odd.add("A"); odd.add("B"); odd.add(null);
        System.out.println("UT20=" + odd.size());
        System.out.println("UT21=" + Arrays.equals(x,y));
        Object[] nestedA = {new int[]{1,67}}, nestedB = {new int[]{1,67}};
        System.out.println("UT22=" + Arrays.deepEquals(nestedA,nestedB));
        System.out.println("UT23=" + Arrays.equals(nestedA,nestedB));
        System.out.println("UT24=" + Arrays.mismatch(new int[]{67,1},new int[]{67,2}));
        System.out.println("UT25=" + Integer.signum(Arrays.compare(new int[]{67,1},new int[]{67,2})));
        int[] sorted = {99,10,67}; Arrays.sort(sorted);
        System.out.println("UT26=" + Arrays.toString(sorted));
        System.out.println("UT27=" + Arrays.binarySearch(sorted,67));
        System.out.println("UT28=" + Arrays.binarySearch(sorted,68));
        String[] words = {"A", "B"}; List<String> view = Arrays.asList(words);
        words[0]="Z"; System.out.println("UT29=" + view);
        view.set(1,"Y"); System.out.println("UT30=" + Arrays.toString(words));
        try { view.add("C"); }
        catch (UnsupportedOperationException ex) { System.out.println("UT31=UnsupportedOperationException"); }
        System.out.println("UT32=" + Arrays.asList(new int[]{1,67}).size());
        try { List.of("A",null); }
        catch (NullPointerException ex) { System.out.println("UT33=NullPointerException"); }
        try { List.of("A").set(0,"B"); }
        catch (UnsupportedOperationException ex) { System.out.println("UT34=UnsupportedOperationException"); }
        List<String> source = new ArrayList<>(List.of("A"));
        List<String> unmodView = Collections.unmodifiableList(source);
        List<String> snapshot = List.copyOf(source);
        source.set(0,"B");
        System.out.println("UT35=" + unmodView + ":" + snapshot);
        StringBuilder element = new StringBuilder("A");
        List<StringBuilder> copied = List.copyOf(List.of(element));
        element.append("67");
        System.out.println("UT36=" + copied.get(0));
        System.out.println("UT37=" + new HashSet<>(Arrays.asList("A","A")).size());
        String[] ordered = {"bbb","a","cc"};
        Arrays.sort(ordered, Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
        System.out.println("UT38=" + Arrays.toString(ordered));
    }
}
~~~

**Фактический stdout UtilProbe (38 строк, UTF-8/LF):**
~~~text
UT01=true
UT02=true
UT03=false
UT04=67:98
UT05=fallback
UT06=NullPointerException:required
UT07=0
UT08=1:lazy
UT09=2
UT10=IndexOutOfBoundsException
UT11=1
UT12=IndexOutOfBoundsException
UT13=[Ada:20, Ada:67, Bob:30]
UT14=[Bob:30, Ada:67, Ada:20]
UT15=[null, A, B]
UT16=[A, B, null]
UT17=[B, A, null]
UT18=NullPointerException
UT19=1
UT20=2
UT21=true
UT22=true
UT23=false
UT24=1
UT25=-1
UT26=[10, 67, 99]
UT27=1
UT28=-3
UT29=[Z, B]
UT30=[Z, Y]
UT31=UnsupportedOperationException
UT32=1
UT33=NullPointerException
UT34=UnsupportedOperationException
UT35=[B]:[A]
UT36=A67
UT37=1
UT38=[a, cc, bbb]
~~~

**SHA-256(UtilProbe.java):** `526b953a76154c137a8eddf1d2dbd965e68a789320d263bf007e52719687a6ae`. **SHA-256(util.stdout):** `57cff41cdf848efe2a0092c3fe938efd91e1a3f537adc1213327609253eddbb7`.

**Пояснение:** `UT04` различает hashCode и varargs hash; `UT13–18` — ключи, null и reversal; `UT19/20` — comparator equivalence и потенциальная потеря отдельных элементов в TreeSet; `UT22/23` — deep vs shallow arrays. `UT27/28` дают контрактные значения лишь после `UT26 Arrays.sort`, а не для произвольно неотсортированного массива. `UT29/30/35/36` показывают view aliasing и ограничение shallow snapshot; не распространять эту демонстрацию на полное покрытие Collections.

## 7. Семантически проверенные Java21 compiler-negative (14 отдельных source)

Все **14/14** полнофайловых компиляционно-отрицательных тестов действительно выполнены **независимыми** командами `javac --release 21 -XDrawDiagnostics -d negative/build negative/FILE.java`; каждый получил exit=1 и собственный содержательный diagnostic `compiler.err.*`. Файлы не собраны в один общий batch и не зависят друг от друга. Сам исходник `Pattern.compile("[")` и замена `"$9"` компилируются **успешно**, падают только в runtime, поэтому здесь НЕ присутствуют.

| Файл | Полный независимый исходник | Реальная диагностическая категория | Причина |
|---|---|---|---|
