# CORE-NUMBERS + CORE-DATETIME — G1 исследовательский план Java SE 21

**Статус: G1 PROPOSED / NOT ACCEPTED.** Роль: 01 — Research & Coverage. Основание: [Issue #183](https://github.com/Melchiorius/backend-interview-labs/issues/183) / [адресное G0-поручение 00 #7086311311](https://github.com/Melchiorius/backend-interview-labs/issues/183#issuecomment-7086311311). **Исходный BASE/main:** `edf68df067fe4c4583965af002e225d6371796d2` (G5 [PR #179](https://github.com/Melchiorius/backend-interview-labs/pull/179) по MODULES+CLASSLOADER). Одновременно 06 выполняет статусный G6 [#182](https://github.com/Melchiorius/backend-interview-labs/issues/182) / PR #184, поэтому содержательные файлы могут опережать `COVERAGE/CURRICULUM`; перед review ветка синхронизируется с **актуальным** main, exact SHA будет отражён в PR и адресном 04-комментарии. **Версия:** JLS/Javadoc/JDK **Java SE 21 без preview**, OpenJDK `21.0.12.1+1-1-deb13u1`, Debian GNU/Linux 13, `javac --release 21`. Числовые/временные выводы — отдельные локальные эксперименты, не GitHub CI snippets.

## 1. Проблема, две самостоятельные области и границы

**CORE-NUMBERS:** почему обычные `int/double` не обладают свойствами математических целых и точных десятичных дробей, как `BigInteger/BigDecimal` задают другую модель, когда операция переполняется, бросает исключение или меняет представление? Отдельно вопрос «какую точность и округление требует бизнес-договор», не универсальный закон «деньги всегда HALF_EVEN».

**CORE-DATETIME:** почему `Instant` и `LocalDateTime` имеют разные семантики, почему региональная `ZoneId` — не только численное смещение `ZoneOffset`, что делают правила зоны в gap/overlap DST, почему день не всегда 24 elapsed hours и как сделать время в тесте воспроизводимым? «UTC и wall clock» не заменяются физическими монотонными таймерами.

**Рекомендуем две самостоятельные связные G2 статьи:** будущие `knowledge/01-java-core/numbers.md` (CORE-NUMBERS) и `knowledge/01-java-core/datetime.md` (CORE-DATETIME). Эти файлы **ещё не существуют**: имена приведены только как inline code, **без fake Markdown links**. Для реального воспроизводимого G2 подходят 2 автономные Java-программы с отдельными фикстурами (числа, дата); проектный Gradle/JUnit module **не обязателен**, если образовательный эффект полностью достигнут короткими кодовыми примерами. Роль 01 не создаёт статьи, Java-файлы, CI, не назначает G2.

**Вне scope:** CORE-I18N (локали/валютный формат), DB DECIMAL/DB JDBC TIMESTAMP/ORM, финансовые/налоговые политики, DIST clock skew/NTP, JVM/JMH/GC, BigDecimal performance; CORE-REGEX/UTIL/SERIAL остаются самостоятельными ID. Временные результаты зависят от versioned tzdb. Примеры не обещают одинаковые даты зон после изменения правил всех стран.

### 1.1 Фактические опубликованные статьи: already covered vs missing

После G5 PR #179 в `main` **35 отдельных Java Core статей, материал для 36 из 42 curricular ID, 6 not Implemented**; status-only G6 #182 параллельно синхронизирует реестры. Пересечения ниже **не** означают готовность двух новых ID.

| Существующий материал | Уже разобрано | Новая обязательная глубина |
|---|---|---|
| [types](../../knowledge/01-java-core/types.md) | primitive types, краткий overflow/NaN, диапазоны | проверяемое `Math.addExact`, BigDecimal и decimal/IEEE контракт |
| [conversions](../../knowledge/01-java-core/conversions.md) | casting, numeric promotions, narrowing/overflow | применение с decimal/exact semantics и исключениями |
| [operators](../../knowledge/01-java-core/operators.md) | overflow и integer division, NaN/-0.0 мост | BigDecimal constructor/scale/rounding и бизнес-граница |
| [equality](../../knowledge/01-java-core/equality.md) | BigDecimal equals/compareTo и HashSet/TreeSet | раздельная шкала scale/precision, действия, округление и divide |
| [strings](../../knowledge/01-java-core/strings.md) | String/Unicode/formatting, строковые операции | String-as-decimal input, java.time parsing, fixed formatter |
| [arrays](../../knowledge/01-java-core/arrays.md) | storage, array bounds, копии | числовые операции на элементах — это не новая полная работа с массивами |
| [exceptions](../../knowledge/01-java-core/exceptions.md) | checked/unchecked/Throwable | конкретные runtime ArithmeticException/DateTimeException/DateTimeParseException |
| [records](../../knowledge/01-java-core/records.md) | Double.NaN/-0.0 record equality | числовые primitive/Double.compare vs BigDecimal equality |
| [immutability](../../knowledge/01-java-core/immutability.md) | value objects, defensive copies | новые immutable java.time values, ошибочные ожидания мутации `plusDays` |
| [generics](../../knowledge/01-java-core/generics.md) | generic APIs/inference | BigDecimal typed comparison не generics standalone |
| [erasure](../../knowledge/01-java-core/erasure.md) | generic metadata/casts | не смешивать numerical precision с type erasure |
| [streams](../../knowledge/01-java-core/streams.md) | stream pipelines, grouping | `BigDecimal` в stream reduction отдельный P2, не закрывается |

Прочитаны нормативные [AGENTS](../../AGENTS.md), [ROLE_MODEL](../ROLE_MODEL.md), [WORKFLOW](../WORKFLOW.md), [JAVA_CORE_PLAN](../JAVA_CORE_PLAN.md), [CURRICULUM](../CURRICULUM.md), [COVERAGE](../COVERAGE.md), [SOURCE_REGISTRY](../SOURCE_REGISTRY.md), [EXERCISE_AND_TESTING_STANDARD](../EXERCISE_AND_TESTING_STANDARD.md). Не меняем их в G1.

## 2. Нормативные A и независимо проверенные интервью C

Источники **A** фиксированы на Java 21: JLS для языка, Javadoc для библиотечного API. **C** только показывает существование формулировки интервью-вопроса; не доказывает норму и не является статистикой частоты.

| ID | Нормативная ссылка A (Java SE 21) и применяемая гарантия |
|---|---|
| A1 | [JLS `5.6 numeric promotion](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.6), [`15.17](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.17), [`15.18](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.18): promotions, division and additive overflow |
| A2 | [JLS `4.2.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.2.3), [`15.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.4), [`15.21.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.21.1), [Double API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Double.html): IEEE754, NaN, infinity, ±0.0, strict floating |
| A3 | [Math API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Math.html), [StrictMath API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/StrictMath.html): addExact/multiplyExact and Math.fma scope |
| A4 | [BigDecimal API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html): `BigDecimal(String)` vs `(double)` vs `valueOf(double)`; scale/precision/unscaledValue, equals/compareTo, divide and stripTrailingZeros |
| A5 | [MathContext API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/MathContext.html), [RoundingMode API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/RoundingMode.html): significant precision vs fixed scale, tie modes, UNNECESSARY |
| A6 | [BigInteger API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigInteger.html): arbitrary-precision integer arithmetic |
| B1 | [Instant API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Instant.html), [ZoneOffset API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/ZoneOffset.html): timeline, epoch, UTC offset |
| B2 | [Clock API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Clock.html): fixed clock for tests, no ambient time |
| B3 | [LocalDate21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalDate.html), [LocalTime21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalTime.html), [LocalDateTime21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalDateTime.html), [OffsetDateTime21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/OffsetDateTime.html) |
| B4 | [ZonedDateTime21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/ZonedDateTime.html), [ZoneId21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/ZoneId.html): DST gap/overlap and zone conversions, immutable |
| B5 | [ZoneRules21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/zone/ZoneRules.html), [ZoneOffsetTransition21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/zone/ZoneOffsetTransition.html): getValidOffsets 0/1/2, tzdb dependence |
| B6 | [Duration21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Duration.html), [Period21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Period.html), [ChronoUnit21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/temporal/ChronoUnit.html): seconds vs calendar amount |
| B7 | [DateTimeFormatter21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeFormatter.html), [DateTimeParseException21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeParseException.html): ISO formats, explicit locale/zone, parsing failures |
| B8 | [DateTimeException21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/DateTimeException.html), [UnsupportedTemporalTypeException21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/temporal/UnsupportedTemporalTypeException.html): unsupported temporal field vs invalid parse |

**Точно прочитанные источники вопросов C:**
- **C1 / Q-HRUDAY**, [README](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md), primitives question №18 “What happens when integer overflow occurs?” (строка 98). Это **широкий** вопрос: `Math.addExact` отдельным пунктом там не подтверждён.
- **C2 / Q-HRUDAY**, тот же [README](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md), floating question №16 “Why should floating-point values not be compared using ==?” (строка 119). Он не доказывает самостоятельный вопрос о NaN/-0.0.
- **C3 / дополнительный независимый банк вне SOURCE_REGISTRY** [sudhir-tech/java-backend-interview-prep, questions 149/150](https://github.com/sudhir-tech/java-backend-interview-prep/blob/main/01-core-java/core-java-interview-questions.md): реальные “What is BigDecimal used for?” и “Why use BigDecimal(String)?”; прочитаны исходные заголовки. Это **дополнение**, а не молчаливое расширение официального реестра.
- **C4 / Q-BAELDUNG-CORE соседний подтверждённый интервью-опрос** [Java 8 Interview Questions, section 10 Q1](https://www.baeldung.com/java-8-interview-questions): “Tell Us About the New Date and Time API in Java 8”. Q-BAELDUNG-CORE в реестре ссылается на **другую** страницу `java-interview-questions`, на которой такого вопроса **нет**. Это явно дополнительная тематическая страница того же издателя, не выдаём её за найденный раздел в реестровой ссылке.
- **C5 / Q-CHERNYSHOV** [английский README №208](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/master/eng/README.md): *“Why are LocalDate and LocalDateTime immutable?”* (строка 266). Вопрос подтверждён; содержание ответов не служит Java21 нормой.

**C GAP:** в прочитанных C-банках **нет доказанной самостоятельной формулировки** про `BigDecimal.divide(1/3)`, `HALF_EVEN` vs `HALF_UP`, `getValidOffsets`/DST gap & overlap, `withZoneSameInstant`/`withZoneSameLocal`, `Clock.fixed`, точное Javadoc `BigDecimal.valueOf`. Они обязательны в P0 как необходимые *follow-up механизмы* для широких C, но не как якобы дословные внешние вопросы. Для `CORE-DATETIME` это особенно заметный исследовательский пробел в текущем реестре.

## 3. CORE-NUMBERS — NM P0 матрица

**Полный исходник** `NumbersProbe.java` и output `NM01–NM33` — ниже `5.1`. «A» доказывает контракт, «C» показывает исходное широкое/точное interview wording, «trace» различает runtime/compile/design.

| ID | P | Адресная механика и отличие от уже опубликованной статьи | A | C | Доказательство/контрпример |
|---|---|---|---|---|---|
| `NM-01` | P0 | int/long overflow wraps; не исключение; не переносить на BigInteger | A1 | C1 | NM01, NM30; ранее types/operators |
| `NM-02` | P0 | Math.addExact проверяет overflow и бросает ArithmeticException | A3 | C1 (широкий) | NM02 и NM01 различаются |
| `NM-03` | P0 | Math.multiplyExact для long проверяет overflow | A3 | C1 (широкий) | NM03 runtime ArithmeticException |
| `NM-04` | P0 | int / int обрезает к нулю, floating division имеет другую семантику | A1 | C1 (широкий) | NM04=2:2.5 |
| `NM-05` | P0 | integer /0 Runtime ArithmeticException; floating /0 infinity/NaN, если нет 0/0 | A1,A2 | C GAP | NM05 vs NM09/NM12 |
| `NM-06` | P0 | binary numeric promotion: byte + int -> int; boxing наблюдаемого результата | A1 | C1 (широкий) | NM06=Integer, static conversion is CORE-CONVERSIONS |
| `NM-07` | P0 | IEEE binary floating point, 0.1+0.2 ≠ точные 0.3 | A1,A2 | C3 | NM07=0.30000000000000004 |
| `NM-08` | P0 | NaN comparisons / Double.isNaN; NaN != NaN | A1,A2 | C2 | NM08=true:false |
| `NM-09` | P0 | Infinity vs finite numbers; isInfinite, float division by zero | A1,A2 | C GAP | NM09/NM12 |
| `NM-10` | P0 | -0.0 == 0.0, но Double.compare и raw bits различают знак | A2 | C2 (широкий) | NM10/NM11; no HashMap generalization |
| `NM-11` | P0 | BigDecimal(String) точно представляет строковую десятичную дробь | A4 | C3 | NM13=0.1 |
| `NM-12` | P0 | new BigDecimal(double) точно переводит двоичную аппроксимацию, не строковую 0.1 | A4 | C3 | NM14 полная длинная десятичная запись |
| `NM-13` | P0 | BigDecimal.valueOf(double) использует canonical Double.toString (в данном случае 0.1) | A4 | C3 | NM15/NM16; не promise every double exact decimal source |
| `NM-14` | P0 | scale vs precision vs unscaled integer, value=unscaled*10^-scale | A4 | C GAP | NM17/NM18/NM21 |
| `NM-15` | P0 | equals зависит от scale, compareTo численное равенство | A4 | C3 (общий BigDecimal) | NM19 false:0, already equality bridge |
| `NM-16` | P0 | HashSet vs TreeSet may differ for BigDecimal (order inconsistent with equals) | A4 | C GAP | NM20=2:1, keep collection module separate |
| `NM-17` | P0 | stripTrailingZeros/normalization can change representation/scale | A4 | C GAP | NM22=1; not a money normalization policy |
| `NM-18` | P0 | BigDecimal.divide exact nonterminating decimal -> ArithmeticException | A4 | C3 (общий BigDecimal) | NM23 vs NM32 terminating 1/8 |
| `NM-19` | P0 | divide(scale,RoundingMode) vs divide(MathContext) precision semantics | A4,A5 | C GAP | NM24=0.33 vs NM25=0.3333 |
| `NM-20` | P0 | HALF_EVEN vs HALF_UP tie handling; UNNECESSARY signals rounding needed | A5 | C GAP | NM26–NM29 |
| `NM-21` | P0 | BigInteger arbitrary precision numeric vs long overflow | A6 | C1 (широкий) | NM30 = 9223372036854775808 |
| `NM-22` | P0 | Decimal units, scale and policy are a *domain* decision, not automatic currency/tax law | A4,A5 | C3 (общий BigDecimal) | NM31=6700; DB DECIMAL/Money module out of scope |
## 4. CORE-DATETIME — DT P0 матрица

**Полный исходник** `DateTimeProbe.java` и output `DT01–DT38` — ниже `5.2`. Для `Europe/Berlin` использованы дата 2024 и реально установленная **tzdb 2026b** в OpenJDK21 Debian13. Нормативно зона может меняться по будущим правовым правилам; hardcoded expected output — тест именно этой среды/tzdb, а не обещание всех окружений.

| ID | P | Адресная механика | A | C | Положительный/отрицательный trace |
|---|---|---|---|---|---|
| `DT-01` | P0 | Instant = точка UTC timeline; epochSeconds ≠ local representation | B1 | C4,C3 | DT01/DT36 |
| `DT-02` | P0 | Clock.fixed replaces mutable ambient now in reproducible tests | B2 | C4 (широкий) | DT01–DT04, DT34 |
| `DT-03` | P0 | LocalDate/LocalTime/LocalDateTime no zone/offset; not globally unique instant | B3 | C3,C4 | DT02–DT04,DT37 |
| `DT-04` | P0 | ZoneId regional rules vs ZoneOffset fixed displacement | B4 | C4 (широкий) | DT05/DT06/DT35 |
| `DT-05` | P0 | ZonedDateTime associates local representation with rules-based ZoneId | B4 | C4 | DT05 and DT07 |
| `DT-06` | P0 | OffsetDateTime binds offset, not future regional rule transitions | B3 | C4 | DT08 vs DT05 |
| `DT-07` | P0 | DST spring gap: local wall time may have zero valid offsets | B5 | C GAP | DT09/DT10 |
| `DT-08` | P0 | ZonedDateTime resolving gap moves invalid local time forward | B4,B5 | C GAP | DT11=03:30+02:00 |
| `DT-09` | P0 | ZonedDateTime.ofStrict rejects invalid offset/time for zone | B4 | C GAP | DT12 runtime DateTimeException, NOT javac |
| `DT-10` | P0 | DST autumn overlap has two valid offsets (Berlin 2024, tzdb 2026b) | B5 | C GAP | DT13=[+02:00,+01:00] |
| `DT-11` | P0 | Earlier/later overlap offsets select two different instants | B4,B5 | C GAP | DT14/DT15 and DT16=60 minutes |
| `DT-12` | P0 | withZoneSameInstant preserves instant, changes wall clock | B4 | C4 (широкий) | DT17/DT19=true |
| `DT-13` | P0 | withZoneSameLocal preserves wall time, may change instant | B4 | C4 (широкий) | DT18/DT20=false |
| `DT-14` | P0 | Period is calendar amount, Duration is seconds+nanos | B6 | C GAP | DT24/DT25; period ≠ exactly 24 hours |
| `DT-15` | P0 | plusDays vs plusHours differ across DST transition | B4,B6 | C GAP | DT21–DT24/DT27: 23 vs 24h |
| `DT-16` | P0 | ChronoUnit.DAYS on dates counts calendar days, not elapsed hours | B6 | C GAP | DT26=1 vs DT24=23 |
| `DT-17` | P0 | DateTimeFormatter pattern, withZone and fixed Locale; no implicit default zone | B7 | C4 (широкий) | DT28 explicit ISO-like custom format |
| `DT-18` | P0 | ISO parsing from text, invalid date raises DateTimeParseException at runtime | B7 | C4 (широкий) | DT29/DT30 |
| `DT-19` | P0 | Missing temporal fields yields DateTimeException/UnsupportedTemporalTypeException | B8 | C GAP | DT31/DT32 runtime, not compile |
| `DT-20` | P0 | java.time immutable, plusDays returns new value; fixed Clock isn't a mutable time source | B1,B3,B6 | C5 | DT33 and API evidence; old LocalDate unchanged |
## 5. Две действительно скомпилированные и исполненные Java 21 программы

**Реальное окружение** — `openjdk version "21.0.12.1" 2026-08-18`, vendor `Debian`, JDK build `21.0.12.1+1-1-deb13u1`, Debian GNU/Linux 13, kernel Linux `6.18.57`. `ZoneRulesProvider.getVersions("Europe/Berlin").lastKey() == "2026b"`. Никакого `ZoneId.systemDefault()`, wall clock now, random values, locale-dependent formatting в этих source нет.

**Реально выполнено в изолированном каталоге:**
~~~sh
javac --release 21 -Xlint:all NumbersProbe.java DateTimeProbe.java
java NumbersProbe > numbers.stdout
java DateTimeProbe > datetime.stdout
# После сохранения текстовых expected fixtures из блоков ниже, с LF:
diff -u numbers.expected.stdout numbers.stdout
diff -u datetime.expected.stdout datetime.stdout
sha256sum numbers.stdout datetime.stdout
~~~
`javac` **exit 0, stderr 0 bytes**, оба `java` **exit 0**; повторные отдельные запуски с `diff -u` по первоначальному stdout: **2/2 exit 0**. **33 + 38 = 71** строки stdout (это **не 71 JUnit tests**), два SHA256 указаны после выводов. Compiler-negative (`6`) выполнялись отдельно и не входят в эти 71 строку.

### 5.1 NumbersProbe.java — полный проверенный source

~~~java
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.TreeSet;

public class NumbersProbe {
    public static void main(String[] args) {
        int max = Integer.MAX_VALUE;
        System.out.println("NM01=" + (max + 1));
        try { Math.addExact(max, 1); } catch (ArithmeticException e) { System.out.println("NM02=ArithmeticException:addExact"); }
        try { Math.multiplyExact(Long.MAX_VALUE, 2L); } catch (ArithmeticException e) { System.out.println("NM03=ArithmeticException:multiplyExact"); }
        System.out.println("NM04=" + (5 / 2) + ":" + (5.0 / 2));
        try { int zero = 0; System.out.println(5 / zero); }
        catch (ArithmeticException e) { System.out.println("NM05=ArithmeticException:divisionByZero"); }
        byte small = 67;
        System.out.println("NM06=" + ((Object) (small + 1)).getClass().getSimpleName());
        System.out.println("NM07=" + (0.1 + 0.2));
        System.out.println("NM08=" + Double.isNaN(Double.NaN) + ":" + (Double.NaN == Double.NaN));
        System.out.println("NM09=" + (Double.POSITIVE_INFINITY > 67.0) + ":" + (1.0 / 0.0));
        System.out.println("NM10=" + (0.0 == -0.0) + ":" + Double.compare(-0.0, 0.0));
        System.out.println("NM11=" + (Double.doubleToRawLongBits(-0.0) == Double.doubleToRawLongBits(0.0)));
        System.out.println("NM12=" + Double.isInfinite(67.0 / 0.0));
        BigDecimal text = new BigDecimal("0.1");
        BigDecimal binary = new BigDecimal(0.1);
        BigDecimal viaValueOf = BigDecimal.valueOf(0.1);
        System.out.println("NM13=" + text);
        System.out.println("NM14=" + binary);
        System.out.println("NM15=" + viaValueOf);
        System.out.println("NM16=" + text.equals(viaValueOf) + ":" + text.equals(binary));
        BigDecimal a = new BigDecimal("2.0");
        BigDecimal b = new BigDecimal("2.00");
        System.out.println("NM17=" + a.scale() + ":" + b.scale());
        System.out.println("NM18=" + a.precision() + ":" + b.precision());
        System.out.println("NM19=" + a.equals(b) + ":" + a.compareTo(b));
        System.out.println("NM20=" + new HashSet<>(java.util.List.of(a,b)).size() + ":" + new TreeSet<>(java.util.List.of(a,b)).size());
        System.out.println("NM21=" + a.unscaledValue());
        System.out.println("NM22=" + new BigDecimal("1.00").stripTrailingZeros().toPlainString());
        try { BigDecimal.ONE.divide(new BigDecimal("3")); }
        catch (ArithmeticException e) { System.out.println("NM23=ArithmeticException:nonterminating"); }
        System.out.println("NM24=" + BigDecimal.ONE.divide(new BigDecimal("3"), 2, RoundingMode.HALF_EVEN));
        System.out.println("NM25=" + BigDecimal.ONE.divide(new BigDecimal("3"), new MathContext(4, RoundingMode.HALF_UP)));
        System.out.println("NM26=" + new BigDecimal("2.5").setScale(0, RoundingMode.HALF_EVEN));
        System.out.println("NM27=" + new BigDecimal("2.5").setScale(0, RoundingMode.HALF_UP));
        System.out.println("NM28=" + new BigDecimal("3.5").setScale(0, RoundingMode.HALF_EVEN));
        try { new BigDecimal("2.55").setScale(1, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { System.out.println("NM29=ArithmeticException:roundingRequired"); }
        System.out.println("NM30=" + BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
        System.out.println("NM31=" + new BigDecimal("67.00").movePointRight(2).toPlainString());
        System.out.println("NM32=" + new BigDecimal("1.0").divide(new BigDecimal("8")));
        System.out.println("NM33=" + (Math.fma(2.0,3.0,1.0)));
    }
}
~~~

**Реальный stdout, 33 строки (UTF-8 + LF):**
~~~text
NM01=-2147483648
NM02=ArithmeticException:addExact
NM03=ArithmeticException:multiplyExact
NM04=2:2.5
NM05=ArithmeticException:divisionByZero
NM06=Integer
NM07=0.30000000000000004
NM08=true:false
NM09=true:Infinity
NM10=true:-1
NM11=false
NM12=true
NM13=0.1
NM14=0.1000000000000000055511151231257827021181583404541015625
NM15=0.1
NM16=true:false
NM17=1:2
NM18=2:3
NM19=false:0
NM20=2:1
NM21=20
NM22=1
NM23=ArithmeticException:nonterminating
NM24=0.33
NM25=0.3333
NM26=2
NM27=3
NM28=4
NM29=ArithmeticException:roundingRequired
NM30=9223372036854775808
NM31=6700
NM32=0.125
NM33=7.0
~~~

`SHA-256(numbers.stdout) = 9c9e8000292ee4face04197388feadb50ba8cdc1e7a9099c3b89ecbdd4b6ea83`.

**Трактовка:** `NM07` фиксирует двоичную аппроксимацию, а `NM13–NM15` показывают **три разные операции конструирования**. `NM17/18` — разные *scale* и *precision*. `NM20` не утверждает, что `HashSet` или `TreeSet` «ошибочны»: это разные контракты эквивалентности. `NM26–NM28` показывают точку tie и правила HALF_EVEN/HALF_UP только на данных значениях. `NM23/NM29` — runtime ArithmeticException; `BigInteger` в `NM30` не «исправляет» int переполнение задним числом. `NM33` лишь минимальное значение `Math.fma`, не тестирует качество округления — это P1.

### 5.2 DateTimeProbe.java — полный проверенный source

~~~java
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.zone.ZoneRules;
import java.util.Locale;

public class DateTimeProbe {
    public static void main(String[] args) {
        ZoneId berlin = ZoneId.of("Europe/Berlin");
        ZoneId tokyo = ZoneId.of("Asia/Tokyo");
        Instant instant = Instant.parse("2024-03-30T12:34:56Z");
        Clock fixed = Clock.fixed(instant, ZoneOffset.UTC);
        System.out.println("DT01=" + Instant.now(fixed));
        System.out.println("DT02=" + LocalDate.now(fixed));
        System.out.println("DT03=" + LocalTime.now(fixed));
        System.out.println("DT04=" + LocalDateTime.now(fixed));
        ZonedDateTime berlinTime = instant.atZone(berlin);
        System.out.println("DT05=" + berlinTime);
        System.out.println("DT06=" + instant.atZone(tokyo));
        System.out.println("DT07=" + berlinTime.toInstant().equals(instant));
        System.out.println("DT08=" + OffsetDateTime.ofInstant(instant, berlin));
        LocalDateTime wall = LocalDateTime.of(2024,3,31,2,30);
        ZoneRules rules = berlin.getRules();
        System.out.println("DT09=" + wall);
        System.out.println("DT10=" + rules.getValidOffsets(wall).size());
        System.out.println("DT11=" + wall.atZone(berlin));
        try { ZonedDateTime.ofStrict(wall, ZoneOffset.ofHours(1), berlin); }
        catch (DateTimeException e) { System.out.println("DT12=DateTimeException:gap"); }
        LocalDateTime repeated = LocalDateTime.of(2024,10,27,2,30);
        System.out.println("DT13=" + rules.getValidOffsets(repeated));
        ZonedDateTime first = repeated.atZone(berlin);
        System.out.println("DT14=" + first);
        ZonedDateTime second = first.withLaterOffsetAtOverlap();
        System.out.println("DT15=" + second);
        System.out.println("DT16=" + Duration.between(first.toInstant(),second.toInstant()).toMinutes());
        System.out.println("DT17=" + berlinTime.withZoneSameInstant(tokyo));
        System.out.println("DT18=" + berlinTime.withZoneSameLocal(tokyo));
        System.out.println("DT19=" + berlinTime.withZoneSameInstant(tokyo).toInstant().equals(instant));
        System.out.println("DT20=" + berlinTime.withZoneSameLocal(tokyo).toInstant().equals(instant));
        ZonedDateTime before = LocalDateTime.of(2024,3,30,12,0).atZone(berlin);
        ZonedDateTime plusDay = before.plusDays(1);
        ZonedDateTime plusHours = before.plusHours(24);
        System.out.println("DT21=" + before);
        System.out.println("DT22=" + plusDay);
        System.out.println("DT23=" + plusHours);
        System.out.println("DT24=" + Duration.between(before.toInstant(),plusDay.toInstant()).toHours());
        System.out.println("DT25=" + Period.between(before.toLocalDate(),plusDay.toLocalDate()));
        System.out.println("DT26=" + ChronoUnit.DAYS.between(before.toLocalDate(),plusDay.toLocalDate()));
        System.out.println("DT27=" + Duration.between(before.toInstant(),plusHours.toInstant()).toHours());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm XXX", Locale.ROOT).withZone(berlin);
        System.out.println("DT28=" + formatter.format(instant));
        System.out.println("DT29=" + Instant.parse("2024-03-30T12:34:56Z"));
        try { LocalDate.parse("2024-02-30"); }
        catch (DateTimeParseException e) { System.out.println("DT30=DateTimeParseException"); }
        try { Instant.from(LocalDate.of(2024,3,30)); }
        catch (DateTimeException e) { System.out.println("DT31=DateTimeException:missingInstant"); }
        try { LocalDate.of(2024,3,30).getLong(java.time.temporal.ChronoField.HOUR_OF_DAY); }
        catch (UnsupportedTemporalTypeException e) { System.out.println("DT32=UnsupportedTemporalTypeException"); }
        System.out.println("DT33=" + LocalDate.of(2024,3,30).plusDays(1));
        System.out.println("DT34=" + (fixed.getZone().equals(ZoneOffset.UTC)));
        System.out.println("DT35=" + berlin.getId());
        System.out.println("DT36=" + instant.getEpochSecond());
        System.out.println("DT37=" + LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
        System.out.println("DT38=" + berlinTime.toLocalDateTime());
    }
}
~~~

**Реальный stdout, 38 строк (UTF-8 + LF, tzdb 2026b):**
~~~text
DT01=2024-03-30T12:34:56Z
DT02=2024-03-30
DT03=12:34:56
DT04=2024-03-30T12:34:56
DT05=2024-03-30T13:34:56+01:00[Europe/Berlin]
DT06=2024-03-30T21:34:56+09:00[Asia/Tokyo]
DT07=true
DT08=2024-03-30T13:34:56+01:00
DT09=2024-03-31T02:30
DT10=0
DT11=2024-03-31T03:30+02:00[Europe/Berlin]
DT12=DateTimeException:gap
DT13=[+02:00, +01:00]
DT14=2024-10-27T02:30+02:00[Europe/Berlin]
DT15=2024-10-27T02:30+01:00[Europe/Berlin]
DT16=60
DT17=2024-03-30T21:34:56+09:00[Asia/Tokyo]
DT18=2024-03-30T13:34:56+09:00[Asia/Tokyo]
DT19=true
DT20=false
DT21=2024-03-30T12:00+01:00[Europe/Berlin]
DT22=2024-03-31T12:00+02:00[Europe/Berlin]
DT23=2024-03-31T13:00+02:00[Europe/Berlin]
DT24=23
DT25=P1D
DT26=1
DT27=24
DT28=2024-03-30 13:34 +01:00
DT29=2024-03-30T12:34:56Z
DT30=DateTimeParseException
DT31=DateTimeException:missingInstant
DT32=UnsupportedTemporalTypeException
DT33=2024-03-31
DT34=true
DT35=Europe/Berlin
DT36=1711802096
DT37=2024-03-30T12:34:56
DT38=2024-03-30T13:34:56
~~~

`SHA-256(datetime.stdout) = 71a4bb9934201ec65ba8e7d89658dd19a9b815d40a6c2218b6dd125d61b4d908`.

**Ключевая причинная цепочка:** `DT10=0` → gap в Europe/Berlin 2024-03-31 02:30; `DT11` стандартная корректировка к 03:30, `DT12` строгий вызов отклоняет невалидный offset. В overlap `DT13` показывает **2** offset, `DT14/DT15` — одно wall time, **два Instant, отличающихся на 60 минут**. `DT22` (calendar plusDays) сохраняет 12:00 и даёт **23 фактических часа**, `DT23` (plusHours 24) даёт 13:00. `DT17/DT19=true` сохраняет Instant при переносе зоны; `DT18/DT20=false` сохраняет wall-clock time, но меняет Instant. `DT30–DT32` — именно runtime exceptions. `DT33` возвращает новую LocalDate, не мутируя исходную.

## 6. Изолированные компиляционно-отрицательные случаи: ровно 8, без искусственных failures

В отличие от ошибок исполнения `BigDecimal.divide` и `ZonedDateTime.ofStrict`, **все перечисленные ниже 8 полных .java файла действительно были скомпилированы отдельно** с `javac --release 21 -XDrawDiagnostics -d negative <filename>`: у **каждого exit 1**, проверен **конкретный диагностический compiler.err.* key** и содержательная причина. Не допускается «прошёл тест» только по nonzero exit без проверки ошибки.

| Самостоятельный файл | Полный Java source (скопировать в файл с этим именем) | Реальная категория `compiler.err.*` | Причина |
|---|---|---|---|
| `N_N01_DoubleAsBigDecimal.java` | `import java.math.BigDecimal; class N_N01_DoubleAsBigDecimal { BigDecimal x = 0.1; }` | `compiler.err.prob.found.req` | double cannot be assigned to BigDecimal, a reference type |
| `N_N02_ExactDouble.java` | `class N_N02_ExactDouble { double x = Math.addExact(1.0, 2.0); }` | `compiler.err.cant.apply.symbols` | Math.addExact has int/long overloads, not double |
| `N_N03_OverflowConstant.java` | `class N_N03_OverflowConstant { int x = 2147483648; }` | `compiler.err.int.number.too.large` | decimal int literal outside int range (not int runtime overflow) |
| `N_N04_Narrowing.java` | `class N_N04_Narrowing { short x = 32768; }` | `compiler.err.prob.found.req` | constant narrowing would lose range |
| `D_N01_LocalDateTimeToInstant.java` | `import java.time.*; class D_N01_LocalDateTimeToInstant { Instant f() { return LocalDateTime.of(2024,3,30,12,0).toInstant(); } }` | `compiler.err.cant.apply.symbol` | LocalDateTime.toInstant requires ZoneOffset |
| `D_N02_PeriodInstant.java` | `import java.time.*; class D_N02_PeriodInstant { Period f() { return Period.between(Instant.EPOCH, Instant.EPOCH.plusSeconds(1)); } }` | `compiler.err.cant.apply.symbol` | Period.between requires LocalDate arguments, not Instant |
| `D_N03_WrongZone.java` | `import java.time.*; class D_N03_WrongZone { ZonedDateTime f() { return Instant.EPOCH.atZone(ZoneOffset.UTC.getId()); } }` | `compiler.err.cant.apply.symbol` | Instant.atZone requires ZoneId, not String |
| `D_N04_WrongFormatter.java` | `import java.time.*; import java.time.format.*; class D_N04_WrongFormatter { String f() { return LocalDate.of(2024,3,30).format("yyyy-MM-dd"); } }` | `compiler.err.cant.apply.symbol` | LocalDate.format accepts DateTimeFormatter, not String |

Команда исполнения каждого случая в чистом каталоге:
~~~sh
mkdir -p negative/out
for f in negative/*.java; do
  javac --release 21 -XDrawDiagnostics -d negative/out "$f" > actual.out 2> actual.err
  status=$?
  # Проверить status == 1 и ожидаемый compiler.err.KEY из таблицы ДЛЯ ЭТОГО ФАЙЛА.
  # Не считать общую компиляцию восьми файлов восемью изолированными проверками.
done
~~~

**Точная граница ошибок:** `int n = Integer.MAX_VALUE + 1` **компилируется** и даёт отрицательный результат при overflow; `int x = 2147483648` **не компилируется** из-за слишком большого *int literal*. `0.1+0.2`, `new BigDecimal(0.1)`, `BigDecimal.ONE.divide(BigDecimal.valueOf(3))`, `LocalDate.parse("2024-02-30")` и `ZonedDateTime.ofStrict(gap,...)` **корректны синтаксически**; их неожиданный результат или исключение относятся к **runtime**, а не к проверке javac. Для `D_N01` недостаточен `LocalDateTime` без offset: метод `toInstant(ZoneOffset)` существует, но нулевая арность — compile error.

## 7. P1/P2: неизученные границы нельзя скрывать за P0
| ID | Семейство | Приоритет | Подробность и фактический предел |
|---|---|---|---|
| `NM-23` | NM | P1 | strict floating-point since Java17; strictfp obsolete but keyword retained; norm JLS `15.4, compiler warning not probed |
| `NM-24` | NM | P1 | Math.fma vs separately rounded operations and strict Math/StrictMath; NM33 basic only, accuracy/perf not measured |
| `NM-25` | NM | P1 | BigDecimal.toString vs toPlainString, negative scale after stripTrailingZeros, canonicalization policy not generalized |
| `NM-26` | NM | P1 | MathContext precision vs setScale decimals; rounding propagation through multi-step calculations |
| `NM-27` | NM | P1 | Double.compare/Double.equals/NaN payload, signed zero and records, don't infer collection contract |
| `NM-28` | NM | P1 | Overflows of integer expressions constant-folded vs runtime, narrowing conversions and diagnostics |
| `DT-21` | DT | P1 | Invalid leap dates, ResolverStyle.SMART vs STRICT and uuuu vs yyyy formatting details |
| `DT-22` | DT | P1 | LocalDate.atStartOfDay(zone) and gap at midnight; historical tzdb transitions differ from Berlin test |
| `DT-23` | DT | P1 | OffsetDateTime vs ZonedDateTime with old/new tzdb after re-resolving; future changes not measured |
| `DT-24` | DT | P1 | Clock.system, tick/offset and dependency injection; no real timer scheduler/network-clock proof |
| `DT-25` | DT | P1 | System.nanoTime relative intervals not a wall clock; not run; separate time measurement scope |
| `NM-29` | NM | P2 | Custom monetary unit/FX rounding taxes/business regulation/ISO currencies, database DECIMAL interop — not run |
| `NM-30` | NM | P2 | IEEE 754 subnormals/NaN payloads, transcendental error bounds, real hardware/JIT/StrictMath benchmarking — not run |
| `NM-31` | NM | P2 | Vector API/BigInteger performance/cryptographic constant time — not run |
| `NM-32` | NM | P2 | Locale-sensitive numeric parsing/currency NumberFormat belongs CORE-I18N — not run |
| `DT-26` | DT | P2 | Other tzdb/JDK/OS and future zone rule updates, historical calendar quirks — not run |
| `DT-27` | DT | P2 | Distributed clocks/NTP/clock skew/leap seconds/monotonic deadlines — not run |
| `DT-28` | DT | P2 | JDBC/TIMESTAMP/ORM persistence types and DB session timezone — not run |
| `DT-29` | DT | P2 | Locale/chronology/calendar systems, DateTimeFormatterBuilder, advanced formatting — CORE-I18N / not run |

**Итого 42 P0 (22 NM + 20 DT), 11 P1, 8 P2.** P0 — требования для будущей самостоятельной статьи, **не** заявление реализованности. Некоторые P0 являются нормативным углублением широкого C; отдельного исходного C для них нет, что отражено в матрице (GAP).

## 8. Инструкции для последующих ролей, аудиты и not run

**02 — Knowledge Author:** создать две автономные связные учебные статьи (NUMBERS и DATETIME), не словарь. Для чисел вести причинную цепочку representation → promotion/overflow → floating failure → BigDecimal representations → scale/equals/compareTo → precision/rounding → design choice. Для времени: момент → локальные значения → zone rules → gap/overlap → conversion → Period/Duration/Clock → exceptions. Для каждой P0 нужны нормативный A/версионная оговорка, объяснение, проверяемая трасса и контрпример; уже закрытые в TYPES/OPERATORS/EQUALITY мосты коротко пересказывать с переходами, не удваивать весь старый материал. Не выводить выбор денежного округления из одной Java API функции.

**03 — Examples & Exercises:** при решении 00 о G2 — полнофайловые два runner с отдельными stdout/expected/verify, JDK21 `--release 21 -Xlint:all`; TZDB-dependent fixture маркировать `Europe/Berlin 2024, tzdb 2026b` и предупреждать, что обновление tzdb может изменить вывод; предусмотреть проверку версии или подходящее явное требование к среде. В compiler-negative каждый source отдельно и сверять не только exit!=0, но expected key/semantic reason. Нельзя подменить Java code fences из Markdown полноценной проектной CI: runner публикуется G2 только при назначении.

**05 — Knowledge Editor:** естественный русский, последовательная цепочка «почему», дифференциация двоичной аппроксимации и десятичного представления, ±0.0 vs NaN; DST gap/overlap и два разных «дня». Проверить реальные Obsidian-переходы только когда появятся статьи, не создавать fake links. Удалить шаблонные фразы, не упрощая нормативную точность.

**04 — Independent Reviewer:** проверить точный исследовательский HEAD, 42 P0, все A/C/GAP и 71 stdout, 8 compile-negative; отдельно проверить длинную десятичную запись `new BigDecimal(0.1)`, `BigDecimal.valueOf`, `MathContext` vs setScale, time gap/overlap и versioned tzdb 2026b, отсутствие времени машины/системной локали. PASS/CHANGES с точными блокерами/строками/нормативной базой, CHANGES → новый HEAD и повторное независимое review. Только 00 принимает G1 ACCEPTED/BLOCKED, merge и даёт G2 Issues 02/03.

**Честно NOT RUN:** project Gradle/JUnit/Actions **не компилирует Java code fences нового G1 Markdown**; другие JDK vendors/OS/tzdb версии, все будущие DST переходы, произвольные tzdb исторические исправления, денежные/налоговые и банковские политики, JDBC/DB DECIMAL/TIMESTAMP persistence, real locale/CLDR tests, distributed clocks/NTP, leap seconds, monotonic `System.nanoTime`, JIT/JMH/GC, BigDecimal microbenchmarks, GUI Obsidian/IDE, HTTP status и anchor click checks, все P2 и отдельные curricular CORE-I18N/DB/REGEX/UTIL/SERIAL. Локальные компиляции и stderr/outputs **реально выполнены**, в отличие от неисполненных пунктов.

**Git file-target static audit:** использовать полный untruncated GitHub recursive tree актуального BASE/main; все уникальные `../`/`../../` ссылки из этого файла `docs/plans/CORE-NUMBERS-DATETIME.md` должны разрешаться в существующие пути. Это **не** GUI/anchor/HTTP-проверка. Число путей и missing=0 указать в PR после реальной сверки. Будущие `numbers.md`/`datetime.md` не должны быть fake Markdown targets.

## 9. Статусы и передача эстафеты

| Объект | G1 статус |
|---|---|
| Java 21 A/JLS/API+C реальная provenance/GAP, NM/DT матрицы 42/11/8 | `G1 PROPOSED / NOT ACCEPTED` |
| Two real JDK21 programs and expected 33/38 output + sha256 + 2 diff | `VERIFIED locally, not CI of Markdown` |
| 8/8 isolated compiler-negative topical diagnostics | `VERIFIED locally, not CI of Markdown` |
| CORE-NUMBERS и CORE-DATETIME опубликованные самостоятельные статьи | `NOT Implemented` |
| knowledge/examples/Gradle/CI/COVERAGE/CURRICULUM в G1 PR | `NO CHANGES` |
| Exact G1 HEAD Actions | Проверить реально после открытия PR; SUCCESS не подразумевается автоматически |
| other JDK/OS/tzdb, domain money/JDBC/monotonic clocks, GUI/P2 | `NOT RUN / OUT OF SCOPE` |

### Передача эстафеты
Кому: **04 — Independent Reviewer**.
Что сделать: независимое G1-review **PR на exact HEAD SHA**: Java21 normative A, реальные C/GAP, 42 P0/11 P1/8 P2, полнофайловые два положительных Java21 source и 33/38 stdout, 8 тематических javac negatives, DST tzdb 2026b, links и not run. Выдать PASS/CHANGES с конкретными формальными/строковыми блокерами. Любая правка HEAD требует повторного review. После PASS передать 00 для формального G1 ACCEPTED/BLOCKED/merge; до этого не начинать G2.
Передать: оператор пересылает **прямую ссылку на адресный опубликованный комментарий в PR**, не общий URL PR/Issue.
