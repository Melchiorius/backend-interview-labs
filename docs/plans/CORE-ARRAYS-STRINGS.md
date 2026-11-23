# CORE-ARRAYS + CORE-STRINGS + CORE-BUILDERS — исследовательский план G1 (Java SE 21)

**Статус:** **G1 PROPOSED / NOT ACCEPTED**; до написания статей. **Роль:** 01 — Research & Coverage. [Issue #119](https://github.com/Melchiorius/backend-interview-labs/issues/119), [адресное поручение 00](https://github.com/Melchiorius/backend-interview-labs/issues/119#issuecomment-7068612421), родитель #33. **Фактический BASE:** `4137f0e8453165882acfaff262d6df18db9f1b93` (после G6 Stream PR #120); на момент выдачи Issue был `48656a8db6ca87128619cee98a0174060baaec5f`. **Норма:** JLS / Java SE API **21**, без preview. **Авторская локальная среда:** OpenJDK `21.0.12.1` / `javac 21.0.12.1` (Debian), `--release 21`. Точный HEAD и CI появятся в описании PR; этот документ не повышает COVERAGE и не является готовой статьёй.

**Позднейшая пометка о статусе (2026-11-23; не часть исходного G1):** сохранённое выше `G1 PROPOSED / NOT ACCEPTED` описывает этап подготовки этого плана. Позднее роль 00 формально приняла G1: [решение по PR #121](https://github.com/Melchiorius/backend-interview-labs/pull/121#issuecomment-7068864661) на проверенном G1 HEAD `c1d2d20679050263864a2fd2ab4485f62fde2112`. Соответствующие учебные материалы G2 впоследствии опубликованы через [G5 PR #124](https://github.com/Melchiorius/backend-interview-labs/pull/124), merge commit `97566d1cba493c6eadd2b065d94c4b993eca63e8`. Это разные этапы и SHA: исторический BASE, матрицы P0/P1/P2 и результаты исследований ниже не изменены.


## 1. Задача читателя и архитектура будущих материалов

Кандидат должен **предсказать тип, вывод и вид отказа** для коварных массивов (covariance, generic arrays, shallow copy, varargs, views), различать String/char/Unicode code points и выбирать StringBuilder/StringBuffer по контракту — не механически называть API. Граница изучения — Java 21, а не особенности OpenJDK Heap/Compact Strings/JIT/количества аллокаций.

**Рекомендация роли 01: две связанные самостоятельные статьи, а не три:** 
1. **CORE-ARRAYS** — JLS §10, generic array/reifiability/varargs, jagged arrays, копирование, сравнение и view `Arrays.asList`; приоритет AR-01–AR-16.
2. **CORE-STRINGS + CORE-BUILDERS** — неизменяемость `String`, UTF-16/code points, regex split и overloads; внутри отдельные связанные разделы о `StringBuilder`/`StringBuffer`, capacity/length, synchronization; TX-01–TX-14 и BL-01–BL-06.

**Почему:** Builders являются узкой функционально связанной частью текстовой обработки и с самостоятельной 8–12-страничной статьёй рискуют искусственным дублированием String; в двух статьях проще поддерживать механику и примеры. Curriculum ID `CORE-BUILDERS` остается отдельной проверяемой *темой*, не обязательным количеством Markdown-файлов. Роль 00 вправе после G1 принять три статьи, если педагогическая полнота лучше. Обе статьи — **не** полный курс `COL-SEQUENTIAL`, IO/charset или JMM.

**Runnable после G1:** один автономный набор `examples/core/arrays-strings/` с **двумя Java source + двумя эталонными stdout** и одной `verify.sh` потенциально полезен, поскольку показывает сразу две категории runtime-ловушек и отличие compile-negative от runtime. Однако роль 03 должна отдельно подтвердить педагогическую пользу, способность выполнить в чистом клоне и объём поддержки. Полноценный Gradle/JUnit/Mockito exercise не требуется по формальному признаку: [EXERCISE_AND_TESTING_STANDARD](../EXERCISE_AND_TESTING_STANDARD.md). **Не создавать runnable или статьи до решения 00 G1.**

## 2. Аудит фактического main — что уже известно и чего ещё нет

По дереву `main @ 4137f0e8453165882acfaff262d6df18db9f1b93` **фактически существуют 19 самостоятельных файлов** `knowledge/01-java-core/*.md`. Карта [CURRICULUM](../CURRICULUM.md) и [JAVA_CORE_PLAN](../JAVA_CORE_PLAN.md) содержит все три curricular ID, однако **нет** отдельных `arrays.md`, `strings.md`, `builders.md`. G6 Stream #118/#120 синхронизирован; нельзя использовать устаревшее «17» из исторических записей COVERAGE как текущую численность. Наличие связанных кратких разделов означает **частичное объяснение**, не готовую отдельную тему.

**Существующие объяснения — переиспользовать ссылкой, не переписывать полностью:**

- [CORE-TYPES](../../knowledge/01-java-core/types.md) — примитивы/ссылки, `null`; не объявляет массивную ковариантность полностью исследованной.
- [CORE-CONVERSIONS](../../knowledge/01-java-core/conversions.md) — string conversion и constant expressions/pool, краткие `"Ja" + "va"` и `new String`; новая String-статья должна дать более полный Unicode/regex/overload механизм.
- [CORE-GENERICS](../../knowledge/01-java-core/generics.md) — массивная ковариантность против инвариантных `List<T>` (GE-09), `ArrayStoreException`.
- [CORE-ERASURE](../../knowledge/01-java-core/erasure.md) — `new T[]` / `new List<String>[]`, `List<?>[]` и предупреждение о ложном `@SafeVarargs`; новая Arrays-статья объясняет сам массив, а не повторяет стирание типов и мосты.
- [CORE-EQUALITY](../../knowledge/01-java-core/equality.md) — `Arrays.equals/deepEquals`, сравнение идентичности, вложенные массивы; связать с изменяемостью и циклическими массивами, не повторять весь хеш-контракт.
- [CORE-STREAMS](../../knowledge/01-java-core/streams.md), [CORE-STREAM-ADV](../../knowledge/01-java-core/stream-advanced.md) — `Arrays.stream`/`IntStream` и вопросы про terminal, не превращать arrays в повторение Stream API.
- [CORE-PARAMS](../../knowledge/01-java-core/parameters.md) — varargs/вызовы; [CORE-OBJECT](../../knowledge/01-java-core/object.md) — identity/clone Object.

**Соседние *неопубликованные либо не охваченные этим G1* ID без вымышленных Markdown-ссылок:** `COL-SEQUENTIAL`, `COL-CONTRACTS`, `IO-BYTES`, `CORE-IMMUTABLE`, `CORE-I18N`, `CORE-REGEX`, `JVM-MEMORY`, `JVM-JIT`, `CON-RACE`. `Arrays.asList` — мост к Collections, но не замена Collection/Iterator/ArrayList; Unicode code points — не UTF-8 bytes и не grapheme clusters; упоминание pool/compact strings не закрывает JVM.

## 3. Нормативные источники A — точная версия Java 21

Основные реестровые ID: [OFF-JLS21 и OFF-API21](../SOURCE_REGISTRY.md). Интервью-каталоги **не** нормативны.

| Документ | Проверяемая норма / ограничение |
|---|---|
| [JLS 21 §10.1–§10.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html#jls-10.1), [§15.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.10) | Массив — объект с фиксированной длиной, компонентный тип; jagged массивы, индексы, zero/default values, создание, размер. Массив примитивов не является массивом wrapper-объектов. |
| [JLS §10.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html#jls-10.5), [§4.7 reifiable](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.7) | Ковариантная ссылка на reference arrays и проверка store по фактическому component type; generic array creation при non-reifiable компоненте запрещено. Нормативно различать runtime `ArrayStoreException` и compile-time generic error. |
| [JLS §10.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html#jls-10.7), [§10.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html#jls-10.9) | `length` — public final **поле**; массивы Cloneable/Serializable; clone shallow для вложенных массивов. `char[]` — не `String`. |
| [JLS §4.12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.2), [§8.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.1), [§9.6.4.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4.7) | Heap pollution / varargs и `@SafeVarargs`: аннотация снимает определённые предупреждения о потенциально небезопасной операции, но не делает код семантически безопасным. |
| [Arrays Javadoc Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html) | `copyOf`, `equals`/`deepEquals`, `toString`/`deepToString`, `asList` fixed-size backed array, overloads `stream`. У `deepEquals` при циклических самоссылочных массивах поведение **не определено API**: не обещать корректный графовый обход. |
| [System.arraycopy Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/System.html#arraycopy(java.lang.Object,int,java.lang.Object,int,int)) | Копирование с перекрытием как через временную копию; runtime типовые/диапазонные ошибки и условия частичного результата для несовместимых reference elements. |
| [JLS 21 §3.10.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10.5), [§15.18.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.18.1), [§15.29](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.29) | Литералы и constant expressions, интернирование строковых констант, конкатенация и вычисление слева направо. Реализация через конкретный StringBuilder/ConcatFactory **не** гарантируется. |
| [String Javadoc 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html) | immutable, final, `equals/==`, `intern`, `length/charAt` (UTF-16 code units), `codePointAt/codePointCount/codePoints/offsetByCodePoints`, `substring`, `split` как **regex**, `replace` vs `replaceAll`, `strip/trim/isBlank`, `join`, `valueOf` overloads. |
| [Character Javadoc 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Character.html) | UTF-16 surrogate pairs, supplementary code points; `char` — одна 16-bit code unit, не любой Unicode символ. |
| [StringBuilder](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/StringBuilder.html), [StringBuffer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/StringBuffer.html) | Mutable sequences, `append/insert`, `capacity/length/ensureCapacity/setLength`; Builder **без гарантии синхронизации**, Buffer с синхронизованными методами, но не гарантия атомарности произвольной многошаговой логики. |
| [List.of / copyOf](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html) | Unmodifiable, запрет null; `Arrays.asList` допускает set и отражает изменения в исходном массиве. Не смешивать `List.of` и backing-view. |

**Уровень утверждений:** указанные JLS/API гарантии ≠ договорённость о внутреннем поле `byte[]` у `String`, расположении string pool в HotSpot, числе объектов при `+`, скорости Builder против `+` или однозначном исходном байтовом кодировании. Изолированные тесты проверяют одну Java 21 реализацию; нормативность — в JLS/API.

## 4. Независимые интервью-вопросы C и оценка охвата

Конкретные **проверенные первичные списки вопросов**, уже имеющиеся в [SOURCE_REGISTRY](../SOURCE_REGISTRY.md):

- **Q-HRUDAY:** [Chapter 5 String, StringBuilder & StringBuffer](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md) — **128 вопросов по оглавлению**, в том числе why immutable, intern, сравнение, конкатенация, append, Buffer/Builder; **Chapter 8 Arrays — 80 вопросов** (covariance, cloning, jagged, asList). Эти числа — структура каталога, **не** частота реальных интервью.
- **Q-CHERNYSHOV:** [§12 String — 23 вопроса](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/master/ru/README.md), в том числе immutable/pool/intern/Builder vs Buffer/substring/concat; «compact strings Java 9» в чужом списке — лишь вопрос об реализации, не нормативное утверждение.
- **Q-IN28:** [Strings #16–22 и Arrays #109–112](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/master/readme.md), сравнение Builder/Buffer, конкатенация в цикле, default values массивов, вывод и сравнение массивов. Не приписывать банку обсуждение всех Java 21 Unicode corner cases.
- **Q-TEAMLEAD:** [вопросы](https://github.com/teamlead/java-interview-questions) «What is String Pool», «Stringbuilder and Stringbuffer», «difference Stringbuilder and concatenation», «cloning of objects, arrays and two-dimensional arrays», «What are arrays in Java». Встречаются повторные prompts и вопросы о JVM internals, их частота не независимая статистика.
- **Q-BAELDUNG-CORE** — имеется в реестре, но весь каталог ответов по String/Arrays на Java 21 не проверялся; не считать его самостоятельным подтверждением спорных API-положений.

**Неэквивалентность источников:** банковские ответы могут быть историческими/ошибочными; normative claims взяты только из Java SE 21 JLS/Javadoc. Вопросы C показывают основные направления, а оставленные из-за недостаточности этих списков P0-ловушки (UTF-16/кодовые точки, generic array creation, overloads null, shallow/arraycopy) подтверждены отдельно API и кодом.

## 5. Матрица задач G1: 51 вопрос — 36 P0 / 10 P1 / 5 P2

Приоритет — важность для учебного материала, **не** уровень должности. Каждая P0 привязана к A норме и прямому probe или чётко обозначенной границе уже опубликованной статьи. Метки `A01–A24` и `S01–S28` ниже обозначают **строки stdout одного из двух Java-процессов**, не отдельные тесты; N01–N24 — **24 отдельных javac compilation units**.

### 5.1 CORE-ARRAYS — 22 вопроса (16 P0 / 4 P1 / 2 P2)

| ID / приоритет | Механизм и граница | Нормативный A | Proof/probe или ссылка | Будущая статья |
|---|---|---|---|---|
| AR-01 **P0** | Примитивные и ссылочные массивы; int[] не Integer[] и не Object[]. | JLS §10.1–§10.2 | A01/A02, N01/N02/N23 | ARRAYS |
| AR-02 **P0** | Создание, нулевая/по-умолчанию инициализация, размер и финальное поле length. | JLS §10.3,§10.7 | A01/A02, N07/N24 | ARRAYS |
| AR-03 **P0** | Массив — Object, его runtime component type; массивы не generic-коллекции. | JLS §10.1,§10.8 | A02/A04 | ARRAYS |
| AR-04 **P0** | Ковариантность String[]→Object[] и runtime ArrayStoreException, не CCE. | JLS §10.5 | A03/A04, N03 | ARRAYS |
| AR-05 **P0** | Отличие invariance List<T> от array covariance; runtime check как причина reifiability. | JLS §4.7,§10.5 | N03/N04; CORE-GENERICS | ARRAYS |
| AR-06 **P0** | Многомерный массив — массив массивов; jagged rows разной длины/могут быть null. | JLS §10.2–§10.4,§15.10 | A05, N09/N10 | ARRAYS |
| AR-07 **P0** | clone массива и shallow-copy верхнего уровня; внутренние массивы разделяются. | JLS §10.7 | A06/A24 | ARRAYS |
| AR-08 **P0** | Arrays.copyOf: новая внешняя оболочка, shallow reference elements, truncation/padding. | Arrays.copyOf API | A07/A08, N19 | ARRAYS |
| AR-09 **P0** | System.arraycopy: overlapping ranges, ошибки типов и возможные частичные копии. | System.arraycopy API | A09/A20; отметить partial guarantees | ARRAYS |
| AR-10 **P0** | Идентичность массивов; Arrays.equals vs deepEquals и различие вложенных. | Arrays.equals/deepEquals API | A10/A11; CORE-EQUALITY | ARRAYS |
| AR-11 **P0** | Arrays.toString/deepToString для отладки; ограничения self-referential arrays. | Arrays.toString/deepToString API | A01/A12; boundary cyclic | ARRAYS |
| AR-12 **P0** | Arrays.asList: fixed-size List backed array; set допустим, add запрещён. | Arrays.asList API | A13/A14 | ARRAYS |
| AR-13 **P0** | Arrays.asList(int[]) как 1 элемент vs Arrays.stream(int[]) как IntStream; reference stream. | Arrays.asList/stream API | A15/A16, N11 | ARRAYS |
| AR-14 **P0** | Generic array creation для non-reifiable component запрещено; List<?>[] допустим. | JLS §4.7,§10.3,§15.10 | A22, N04/N05; CORE-ERASURE | ARRAYS |
| AR-15 **P0** | Generic varargs, heap pollution, неправильный @SafeVarargs; аннотация не проверяет тело. | JLS §4.12.2,§8.4.1,§9.6.4.7 | A21 (намеренно небезопасный код), CORE-ERASURE | ARRAYS |
| AR-16 **P0** | Компиляция vs runtime: неверный индекс, отрицательная длина, null access, arraycopy store. | JLS §10.3/§10.4/§10.5; System API | A17–A20, N06/N08 | ARRAYS |
| AR-17 **P1** | Дополнительные значения по умолчанию boolean/char/reference и new T[0]. | JLS §4.12.5,§10.3 | A01 и будущий inline пример | ARRAYS |
| AR-18 **P1** | Arrays.asList vs List.of vs List.copyOf: backing, null и mutability/identity. | Arrays/List API | A13/A14/A23; не путать | ARRAYS |
| AR-19 **P1** | Arrays.sort и binarySearch, условия sorted/Comparator, порядок и ограничения. | Arrays API | Будущий короткий inline пример | ARRAYS |
| AR-20 **P1** | System.arraycopy: частичный результат при несовместимом reference element. | System.arraycopy API | Отдельная проверка, A20 только exception class | ARRAYS |
| AR-21 **P2** | Deep equals/hash/toString при циклических self-referential массивах не универсальный графовый алгоритм. | Arrays.deepEquals/deepToString API | Непроверенная граница | ARRAYS |
| AR-22 **P2** | Сложные generic varargs/lint heap pollution и performance массивов vs коллекций. | JLS §4.7,§15.12; JVM boundary | Отдельный этап COL/JVM | ARRAYS |

### 5.2 CORE-STRINGS — 20 вопросов (14 P0 / 4 P1 / 2 P2)

| ID / приоритет | Механизм и граница | Нормативный A | Proof/probe или ссылка | Будущая статья |
|---|---|---|---|---|
| TX-01 **P0** | String immutable/final; concat создаёт результат, не меняет исходную строку. | String API, JLS §4.3.3 | S01 | STRINGS |
| TX-02 **P0** | String.equals сравнивает содержимое, == сравнивает ссылки. | String.equals; JLS §15.21 | S02; CORE-EQUALITY | STRINGS |
| TX-03 **P0** | Литералы и constant expressions интернируются; new String/intern; без обещаний размещения памяти. | JLS §3.10.5,§15.29; String.intern | S03; CORE-CONVERSIONS | STRINGS |
| TX-04 **P0** | Оператор +: строковый контекст, left associativity, вычисление слева направо, значение null. | JLS §15.7,§15.18.1,§5.1.11 | S04/S14; CORE-CONVERSIONS | STRINGS |
| TX-05 **P0** | UTF-16 code units: length/charAt ≠ Unicode code points; суррогатные пары. | String/Character API | S05/S06/S08 | STRINGS |
| TX-06 **P0** | codePointAt/codePointCount/codePoints/offsetByCodePoints и индексы substring в char units. | String API | S05–S08 | STRINGS |
| TX-07 **P0** | substring(start,end): end exclusive, immutable String, ошибки диапазонов. | String.substring API | S07/S26/S28 | STRINGS |
| TX-08 **P0** | trim vs strip и isBlank/isEmpty; Character.isWhitespace vs <=U+0020. | String.strip/trim/isBlank API | S09/S10 | STRINGS |
| TX-09 **P0** | split использует regex, экранирование Java literal + regex, limit=0 отбрасывает хвост. | String.split API | S11/S12 | STRINGS |
| TX-10 **P0** | replace(CharSequence) literal vs replaceAll(regex) regex. | String.replace/replaceAll API | S16 | STRINGS |
| TX-11 **P0** | String.join: null element превращается в текст «null» при non-null delimiter. | String.join API | S13 | STRINGS |
| TX-12 **P0** | String.valueOf((Object)null) vs overload char[] null — различный контракт. | String.valueOf overloads API | S14/S15 | STRINGS |
| TX-13 **P0** | charAt возвращает char; codePointAt возвращает int, обоснование типов/ошибок. | String API | S06/S27, N15/N17 | STRINGS |
| TX-14 **P0** | String vs char[]/StringBuilder различны как типы; String.length() метод. | JLS §10.9, String API | N12/N13/N16/N23 | STRINGS |
| TX-15 **P1** | Визуально одинаковые Unicode последовательности без normalization могут быть не equal. | String.equals/Unicode | S17; CORE-I18N boundary | STRINGS |
| TX-16 **P1** | String switch и null; case чувствителен к значению и equality, не reference identity. | JLS §14.11 | Будущий positive/negative фрагмент; проверить JDK21 | STRINGS |
| TX-17 **P1** | String.compareTo UTF-16 lexicographic, не locale-aware collator. | String.compareTo API | CORE-I18N boundary | STRINGS |
| TX-18 **P1** | String.format/ formatted и Locale; нет безусловного locale-independent результата. | String.format/Formatter API | S25 c Locale.ROOT | STRINGS |
| TX-19 **P2** | Compact strings, pool placement/GC, substring sharing, JIT concat strategy — implementation details. | JLS vs JVM | Не обещать как API | STRINGS |
| TX-20 **P2** | Символы как grapheme clusters/emoji ZWJ, Unicode normalization/charset — не codePointCount. | Character/BreakIterator; IO-BYTES | CORE-I18N/IO boundary | STRINGS |

### 5.3 CORE-BUILDERS — 9 вопросов (6 P0 / 2 P1 / 1 P2)

| ID / приоритет | Механизм и граница | Нормативный A | Proof/probe или ссылка | Будущая статья |
|---|---|---|---|---|
| BL-01 **P0** | StringBuilder — mutable CharSequence; append/insert меняют состояние. | StringBuilder API | S18/S21, N12/N13 | BUILDERS |
| BL-02 **P0** | StringBuilder vs StringBuffer: no synchronization guarantee vs synchronized methods; внешний сценарий не атомарен. | StringBuilder/StringBuffer API | S23; design boundary | BUILDERS |
| BL-03 **P0** | length — использованные UTF-16 units; capacity — резерв; ensureCapacity не меняет length. | StringBuilder API | S18/S19, N20 | BUILDERS |
| BL-04 **P0** | Builder equality — inherited identity, contentEquals как содержательная проверка. | StringBuilder/String API | S20 | BUILDERS |
| BL-05 **P0** | append overloads: String null → «null», char[] null → NPE. | StringBuilder.append API | S21/S22 | BUILDERS |
| BL-06 **P0** | setLength может добавить U+0000; char operations и суррогаты требуют аккуратности. | AbstractStringBuilder/StringBuilder API | S24 | BUILDERS |
| BL-07 **P1** | Смена текста циклом + vs builder: семантика одинаковая, JIT/число аллокаций не гарантированы. | JLS §15.18, StringBuilder API | Рекомендация без benchmark | BUILDERS |
| BL-08 **P1** | StringBuilder.toString/append/insert/delete/reverse, форматирование; API contracts и исключения. | StringBuilder API | S18/S23, N14/N18 | BUILDERS |
| BL-09 **P2** | Concurrency на общих Builder/Buffer, external mutable CharSequence, atomic operations vs transaction. | StringBuffer API, CON-RACE | Не объявлять JMM завершённым | BUILDERS |

## 6. Положительные и контролируемые runtime-пробы (две программы)

Создать в **отдельной временной директории**, не в репозитории, два полных файла `ArraysG1.java` и `StringsG1.java` с точно приведённым содержимым:

```bash
mkdir -p out
javac --release 21 -d out ArraysG1.java StringsG1.java
java -cp out ArraysG1 > arrays.actual
java -cp out StringsG1 > strings.actual
# В arrays.expected и strings.expected скопировать строки из блоков ниже, с конечным newline.
diff -u arrays.expected arrays.actual
diff -u strings.expected strings.actual
```

**Реальный прогон автора 01:** OpenJDK/javac `21.0.12.1`, компиляция **двух** Java-файлов одним вызовом успешна, затем **два Java-процесса**, stdout **24/24 A-метки** и **28/28 S-меток** в точности соответствует эталонам. Runtime исключения **перехвачены** методом `fails` и отображаются как simple class name; классы A03/A14/A17–A21/A23 и S15/S22/S27/S28 **не** являются отрицательными компиляциями. Источники помещены только в этот Markdown, CI репозитория их **не запускает**.

### 6.1 ArraysG1.java

```java
import java.util.*;
import java.util.stream.*;

public class ArraysG1 {
    static void show(String id, Object value) { System.out.println(id + "=" + value); }
    static String fails(Runnable action) {
        try { action.run(); return "NO_ERROR"; }
        catch (RuntimeException ex) { return ex.getClass().getSimpleName(); }
    }
    @SafeVarargs // Deliberately false safety claim: demonstrates a runtime hazard, NOT recommended.
    static void unsafe(List<String>... lists) {
        Object[] alias = lists;
        alias[0] = List.of(67);
        String value = lists[0].get(0);
        show("A99", value); // never reached
    }
    public static void main(String[] args) {
        int[] ints = new int[2];
        String[] refs = new String[2];
        show("A01", Arrays.toString(ints) + "," + Arrays.toString(refs));
        show("A02", ints.length + "," + (ints instanceof Object));
        String[] strings = {"a", "b"};
        Object[] covariant = strings;
        show("A03", fails(() -> covariant[0] = 67));
        show("A04", covariant.getClass().getComponentType() == String.class);
        int[][] jagged = new int[2][];
        jagged[0] = new int[]{1, 2}; jagged[1] = new int[]{3};
        show("A05", jagged.length + "," + jagged[0].length + "," + jagged[1].length);
        int[][] copyOuter = jagged.clone();
        copyOuter[0][0] = 67;
        show("A06", (jagged != copyOuter) + "," + (jagged[0] == copyOuter[0]) + "," + jagged[0][0]);
        int[][] viaCopy = Arrays.copyOf(jagged, jagged.length);
        show("A07", (viaCopy[0] == jagged[0]) + "," + (viaCopy != jagged));
        int[] shortCopy = Arrays.copyOf(new int[]{4}, 3);
        show("A08", Arrays.toString(shortCopy));
        int[] overlapped = {1, 2, 3, 4};
        System.arraycopy(overlapped, 0, overlapped, 1, 3);
        show("A09", Arrays.toString(overlapped));
        show("A10", Arrays.equals(new int[]{1,2}, new int[]{1,2}));
        int[][] deepA={{1},{2}}, deepB={{1},{2}};
        show("A11", Arrays.equals(deepA, deepB) + "," + Arrays.deepEquals(deepA, deepB));
        show("A12", Arrays.deepToString(deepA));
        String[] source = {"x", "y"};
        List<String> fixed = Arrays.asList(source);
        fixed.set(0, "z"); source[1] = "q";
        show("A13", Arrays.toString(source) + "," + fixed);
        show("A14", fails(() -> fixed.add("t")));
        show("A15", Arrays.asList(new int[]{1,2}).size() + "," + Arrays.stream(new int[]{1,2}).sum());
        show("A16", Arrays.stream(new String[]{"p", "q"}).count());
        show("A17", fails(() -> { int n = ints[2]; }));
        show("A18", fails(() -> { int[] wrong = new int[-1]; }));
        show("A19", fails(() -> { int n = ((int[])null).length; }));
        show("A20", fails(() -> System.arraycopy(new Object[]{"ok", 67}, 0, new String[2], 0, 2)));
        show("A21", fails(() -> unsafe(List.of("ok"))));
        List<?>[] wildcard = new List<?>[1]; wildcard[0] = List.of(67);
        show("A22", wildcard[0].get(0));
        show("A23", fails(() -> List.of("x", null)));
        show("A24", new int[]{67}.clone().length);
    }
}
```

**Реальный stdout (24 строки, один запуск):**

```text
A01=[0, 0],[null, null]
A02=2,true
A03=ArrayStoreException
A04=true
A05=2,2,1
A06=true,true,67
A07=true,true
A08=[4, 0, 0]
A09=[1, 1, 2, 3]
A10=true
A11=false,true
A12=[[1], [2]]
A13=[z, q],[z, q]
A14=UnsupportedOperationException
A15=1,3
A16=2
A17=ArrayIndexOutOfBoundsException
A18=NegativeArraySizeException
A19=NullPointerException
A20=ArrayStoreException
A21=ClassCastException
A22=67
A23=NullPointerException
A24=1
```

**Смысл выходов:** A03 — действительный String[] проверяет попытку записи Integer несмотря на статическую ссылку Object[]; A06/A07 — копия верхнего массива не копирует вложенный; A09 — перекрывающаяся `arraycopy` ведёт себя как предварительное сохранение элементов; A11 — shallow `Arrays.equals` против `deepEquals`; A13/A14 — `asList`-backed fixed-size view, в отличие от `List.of` (A23); A15 — примитивный массив трактуется единственным элементом generic varargs, но `Arrays.stream(int[])` — IntStream; A20 — несовместимый reference arraycopy; A21 — **преднамеренно небезопасная** реализация с ложным `@SafeVarargs`: подтверждает CCE в момент извлечения `String`, **не пример хорошей аннотации**. A22 — допустимый reifiable `List<?>[]`.

### 6.2 StringsG1.java

```java
import java.util.*;

public class StringsG1 {
    static void show(String id, Object value) { System.out.println(id + "=" + value); }
    static String fails(Runnable action) {
        try { action.run(); return "NO_ERROR"; }
        catch (RuntimeException ex) { return ex.getClass().getSimpleName(); }
    }
    public static void main(String[] args) {
        String immutable = "a";
        String joined = immutable.concat("b");
        show("S01", immutable + "," + joined);
        show("S02", "Java".equals(new String("Java")) + "," + ("Java" == new String("Java")));
        show("S03", (("Ja" + "va") == "Java") + "," + (new String("Java").intern() == "Java"));
        show("S04", (67 + 1 + "x") + "," + ("x" + 67 + 1));
        String emoji = new String(Character.toChars(0x1F600));
        String mixed = "A" + emoji + "Z";
        show("S05", mixed.length() + "," + mixed.codePointCount(0,mixed.length()));
        show("S06", Character.isHighSurrogate(mixed.charAt(1)) + "," + mixed.codePointAt(1));
        show("S07", mixed.offsetByCodePoints(0,2) + "," + mixed.substring(1,3).equals(emoji));
        show("S08", mixed.codePoints().count());
        String spaced = "\u2003X\u2003";
        show("S09", spaced.trim().length() + "," + spaced.strip().length());
        show("S10", "\u2003".isBlank() + "," + "".isEmpty() + "," + " ".isEmpty());
        show("S11", Arrays.toString("a.b".split("\\.")));
        show("S12", Arrays.toString("a,b,,".split(",")) + "," + Arrays.toString("a,b,,".split(",",-1)));
        show("S13", String.join("|", "a", null));
        show("S14", String.valueOf((Object)null));
        show("S15", fails(() -> String.valueOf((char[])null)));
        show("S16", "a.c".replace(".", "!") + "," + "a.c".replaceAll(".", "!"));
        show("S17", "\u00E9".equals("e\u0301"));
        StringBuilder b = new StringBuilder("ab"); b.append(67).insert(0, "[").append(']');
        show("S18", b + "," + b.length());
        int oldLen = b.length(); b.ensureCapacity(30);
        show("S19", (b.length() == oldLen) + "," + (b.capacity() >= 30));
        show("S20", new StringBuilder("a").equals(new StringBuilder("a")) + "," + "a".contentEquals(new StringBuilder("a")));
        show("S21", new StringBuilder().append((String)null).toString());
        show("S22", fails(() -> new StringBuilder().append((char[])null)));
        StringBuffer buff = new StringBuffer("x"); buff.append("y");
        show("S23", buff.toString());
        StringBuilder padded = new StringBuilder("x"); padded.setLength(3);
        show("S24", padded.length() + "," + ((int)padded.charAt(1)) + "," + ((int)padded.charAt(2)));
        show("S25", String.format(Locale.ROOT, "%d", 67));
        show("S26", "abc".substring(1, 3));
        show("S27", fails(() -> "abc".charAt(3)));
        show("S28", fails(() -> "a".substring(2)));
    }
}
```

**Реальный stdout (28 строк, один запуск):**

```text
S01=a,ab
S02=true,false
S03=true,true
S04=68x,x671
S05=4,3
S06=true,128512
S07=3,true
S08=3
S09=3,1
S10=true,true,false
S11=[a, b]
S12=[a, b],[a, b, , ]
S13=a|null
S14=null
S15=NullPointerException
S16=a!c,!!!
S17=false
S18=[ab67],6
S19=true,true
S20=false,true
S21=null
S22=NullPointerException
S23=xy
S24=3,0,0
S25=67
S26=bc
S27=StringIndexOutOfBoundsException
S28=StringIndexOutOfBoundsException
```

**Смысл выходов:** S03 проверяет **интернированный литерал/константное выражение**, не расположение pool; S05–S08 используют U+1F600 (две `char` code units и одна Unicode code point); S09/S10 различают Unicode whitespace и старый диапазон trim; S11 — корректно экранированная точка regex `"\\."` в Java string literal; S12 демонстрирует нулевой default limit и сохранение пустых хвостов при `-1`; S15, S22 — перегрузки с `char[] null`, а не общий «любой null возвращает строку»; S19 — `ensureCapacity` **не увеличивает length**; S20 — Builder не наследует String value equals; S24 — `setLength` добавляет U+0000.

## 7. Отрицательные **compile-time** проверки javac Java 21

Все N01–N24 — **разные самостоятельные Java-файлы** `negative/<ID>.java`, исходник каждой строки ниже полный. Не объединять snippets в один файл. Пример команды: `javac --release 21 -d out-negative negative/N04_GenericArrayCreation.java`. Остальные выполняются **по одному** с собственным файлом/именем класса. Отсутствие вывода stdout ожидаемо; проверять **категорию ошибки**, а не только nonzero exit. Текст диагностики и число сообщений — особенности javac, правило запрета — JLS/API.

| ID / файл | Полный Java 21 source | Ожидаемая и реально полученная причина отказа |
|---|---|---|
| `N01_PrimitiveBoxedArray` | `class N01_PrimitiveBoxedArray { Integer[] a = new int[2]; }` | int[] ≠ Integer[]; incompatible types |
| `N02_PrimitiveObjectArray` | `class N02_PrimitiveObjectArray { Object[] a = new int[2]; }` | int[] — Object, но не Object[] |
| `N03_DownwardArrayAssignment` | `class N03_DownwardArrayAssignment { String[] a = new Object[2]; }` | Обратная ссылка Object[] → String[] без cast недопустима |
| `N04_GenericArrayCreation` | `import java.util.*; class N04_GenericArrayCreation { List<String>[] a = new List<String>[2]; }` | generic array creation: List<String> не reifiable |
| `N05_TypeVariableArrayCreation` | `class N05_TypeVariableArrayCreation<T> { T[] a = new T[2]; }` | generic array creation: неизвестный runtime компонент T |
| `N06_FractionalArraySize` | `class N06_FractionalArraySize { int[] a = new int[2.5]; }` | incompatible types: double index/размер без cast |
| `N07_ArrayLengthMethod` | `class N07_ArrayLengthMethod { int n = new int[2].length(); }` | length — поле; cannot find symbol method length() |
| `N08_WrongArrayElement` | `class N08_WrongArrayElement { void f(){ int[] a = new int[1]; a[0] = "x"; } }` | String нельзя записать в int[] |
| `N09_TooManyDimensions` | `class N09_TooManyDimensions { int[][] a = new int[2][3][]; }` | int[][][] не преобразуется в int[][] |
| `N10_JaggedWrongSubarray` | `class N10_JaggedWrongSubarray { void f(){ int[][] a = new int[1][]; a[0] = new Integer[]{67}; } }` | Integer[] несовместим с ожидаемым int[] |
| `N11_PrimitiveAsListGeneric` | `import java.util.*; class N11_PrimitiveAsListGeneric { List<String> a = Arrays.asList(new int[]{1,2}); }` | generic inference: элемент List — int[], не String |
| `N12_BuilderIsNotString` | `class N12_BuilderIsNotString { String s = new StringBuilder("x"); }` | StringBuilder не String |
| `N13_StringNotBuilder` | `class N13_StringNotBuilder { StringBuilder b = "x"; }` | Литерал String не StringBuilder |
| `N14_ToStringMethodReference` | `class N14_ToStringMethodReference { String s = new StringBuilder("x").toString; }` | toString — метод, не поле |
| `N15_CodePointNarrowing` | `class N15_CodePointNarrowing { char c = "abc".codePointAt(0); }` | codePointAt возвращает int, narrowing не неявный |
| `N16_StringLengthField` | `class N16_StringLengthField { int n = "x".length; }` | String имеет length(), не поле length |
| `N17_CharAsString` | `class N17_CharAsString { String s = "abc".charAt(0); }` | char не присваивается String |
| `N18_AppendNoArgs` | `class N18_AppendNoArgs { StringBuilder b = new StringBuilder().append(); }` | append требует аргумент |
| `N19_CopyOfLongLength` | `import java.util.*; class N19_CopyOfLongLength { int[] a = Arrays.copyOf(new int[]{1}, 2L); }` | Arrays.copyOf(int[],int), не overload с long |
| `N20_CapacityField` | `class N20_CapacityField { void f(){ StringBuilder b = new StringBuilder(); b.capacity = 20; } }` | capacity() — метод, поле capacity недоступно |
| `N21_JoinNeedsCharSequences` | `class N21_JoinNeedsCharSequences { String s = String.join("-", 67); }` | String.join(CharSequence,CharSequence...) не int |
| `N22_StripNoBooleanArg` | `class N22_StripNoBooleanArg { String s = "x".strip(true); }` | strip() без boolean |
| `N23_PrimitiveReferenceArrayAssignment` | `class N23_PrimitiveReferenceArrayAssignment { char[] a = new String[2]; }` | char[] и String[] не совместимы |
| `N24_FinalArrayLength` | `class N24_FinalArrayLength { void f(){ int[] a = {1,2}; a.length = 3; } }` | array.length — final, нельзя присвоить |

**Фактическая проверка:** OpenJDK/javac 21.0.12.1, `--release 21`. Все **24/24 изолированных компиляции** завершились ошибкой **заявленной диагностической категории**, без accidental EOF/parser failure. Исходная экспериментальная попытка N10 с неправильным синтаксисом `new int[][3]` была **исключена до публикации**: она давала `']' expected` и не проверяла желаемый смысл. Опубликованная N10 — корректно сформированная операция записи несовместимого `Integer[]` в `int[][]`.

**Что компилируется и ошибается только при исполнении:** `new int[-1]` (NegativeArraySizeException), `arr[arr.length]` (ArrayIndexOutOfBoundsException), `Object[] o = new String[2]; o[0] = 67;` (ArrayStoreException), `String.valueOf((char[])null)` (NPE), `new StringBuilder().append((char[])null)` (NPE). Не переносить эти случаи в compile-negative матрицу. Допустимы `List<?>[] a = new List<?>[2]` и `String s = "x" + (Object)null` — они не compile errors.

## 8. Риски, анти-обобщения и требования к будущим авторам

1. **Variance:** `String[] → Object[]` и `String[] → Object` допустимы, `int[] → Object[]` — нет; ошибка записи несовместимого элемента возникает во **время исполнения** (тип исходного объекта массива проверяется JVM). Generics invariance обычно блокирует неподходящий тип **при компиляции**.
2. **Shallow:** `clone()` массива и `Arrays.copyOf` создают новый внешний массив, но сохраняют ссылки на внутренние объекты/подмассивы. Не обещать «полноценный deep clone»; `Arrays.deepEquals` — способ сравнения вложенных элементов, **не способ копирования**.
3. **Arrays.asList:** это **backed fixed-size List**, не независимый ArrayList. `set` работает, `add/remove` размера — UnsupportedOperationException. `Arrays.asList(new int[]{1,2})` содержит **один int[]**, а не два Integer; отдельный `Arrays.stream(int[])`.
4. **Varargs:** наличие `@SafeVarargs` не делает небезопасное тело безопасным. A21 — доказательство **ложного обещания**; нельзя советовать копировать этот код в production.
5. **Unicode:** `String.length` — UTF-16 code units, `codePointCount` — code points; последнее **тоже не равно** количеству видимых графем. Index для `charAt/substring` по code units; невнимательная нарезка способна разрезать surrogate pair.
6. **String intern:** нормативное поведение строковых литералов/константных выражений и `String.intern`; число объектов для конкатенации, размещение pool и внутреннее `byte[]` — реализация, не гарантии языка.
7. **Null overloads:** `valueOf((Object)null)` и `append((String)null)` дают текст `"null"`, но `valueOf((char[])null)` / `append((char[])null)` — NPE. Проверять **статическую перегрузку**, не обобщать на все null.
8. **Builders:** `StringBuilder` mutability и отсутствие synchronization guarantee; `StringBuffer` синхронизирует методы, но сочетание нескольких операций или переданный mutable `CharSequence` требует собственной координации. `capacity` не размер текста и не обещание улучшения performance при любой нагрузке.
9. **Comparisons:** `String.equals` сравнивает кодовые единицы строки, не нормализует Unicode автоматически. Locale-sensitive сортировка, normalization, charset и безопасность regex — самостоятельные темы.
10. **Tests are not spec:** A/S stdout подтверждён одним OpenJDK 21 runtime; без обязательств о конкретном HotSpot/JIT/GC, поточных расписаниях, времени работы или байтовых представлениях. Compile-negative проверяются по diagnostics, а не только exit code; IDE/Obsidian GUI пока не проверялся.

## 9. Реальные проверки, непроверенные границы и G1 gate

**Авторская проверка роли 01:** официальные ссылки JLS 21 §10/§15 и Java SE 21 Javadocs; реальные вопросы C сверены по четырём оригинальным GitHub спискам; фактическое дерево содержит **19** самостоятельных Java Core статей, три новых ID остаются без собственных статей. Positive: **2 компиляционных единицы / 1 javac / 2 java invocations**, **24+28=52 строки**. Negative: **24 самостоятельных javac --release 21** по заявленным категориям, **0 accidental parser/EOF errors**. Локальные исходники лежат во временном каталоге автора; **в этот PR входит только исследовательский Markdown**, Java/Gradle/CI/учебные статьи не коммитить. Перед публикацией GitHub CI проверяется отдельно по фактическому HEAD и **не** доказывает Markdown snippets.

**Not run/не подтверждено:** локальный Gradle/JUnit/Mockito репозитория, альтернативные JDK/JVM, GUI Obsidian и IntelliJ рендер/клики, actual `System.arraycopy` partially-copied-prefix после ошибки, performance/allocations (JMH), HotSpot string layout, GC/pool placement, multi-thread Builder/Buffer race scenarios, реальный IO/charset, grapheme segmentation, regex catastrophic behavior. Если Java CI существующего проекта зелёный, он проверяет опубликованные примеры и тесты, а не этот Markdown автоматически.

**Обязательные вопросы независимого G1 review 04:**
- [ ] Пересчитать 51 ID (AR-01–AR-22, TX-01–TX-20, BL-01–BL-09) и 36 P0/10 P1/5 P2. Для каждого P0 проверить нормативный Java21 источник, педагогическое обещание и прямую проверку либо точно отграниченный существующий материал.
- [ ] Проверить P0 массивов: `Object[]` vs `int[]`, covariance/ArrayStoreException, generic array/reifiable, jagged/clone/copy, backed `Arrays.asList`, varargs `@SafeVarargs` с ложным утверждением, отличие массивов из §10 от Collections.
- [ ] Проверить P0 строк: UTF-16/code point/charAt/substring, `split("\\.")` и limit, Unicode strip vs trim, интернирование/`==`, null overloads, отсутствие ложных JVM/JIT гарантий.
- [ ] Независимо восстановить и скомпилировать **полные два Java sources** §6, сравнить 24+28 строки; отдельно N01–N24, **в особенности N04/N05/N10/N11/N15/N19/N24** — по смысловым javac diagnostics, а не просто nonzero. Не смешивать runtime А/S failures и compile-negative.
- [ ] Сверить локальные Markdown links с фактическим BASE, оценить архитектуру «массивы отдельно; строки и Builder вместе» и объём опционального runnable; одобрять учебные статьи **нельзя** на основании одного G1 плана.
- [ ] Дать независимый G1 PASS/CHANGES **по точному HEAD**, устранить блокеры при необходимости. После PASS уведомить 00 отдельным `Кому: 00` для **его** решения G1 ACCEPTED/BLOCKED. Merge только 00, G2 только по его назначению, COVERAGE/CURRICULUM не повышать на G1.

### Передача эстафеты
Кому: 04 — Independent Reviewer
Что сделать: независимо проверить G1-план `docs/plans/CORE-ARRAYS-STRINGS.md` в исследовательском PR на точном HEAD, Java 21 JLS/API, матрицу 51 ID, две программы (52 stdout) и 24 isolated negative compilations. Дать PASS/CHANGES; при PASS передать 00 для самостоятельного G1 решения.
Передать: оператор пересылает прямую ссылку на отдельный адресный опубликованный GitHub-комментарий после открытия PR.