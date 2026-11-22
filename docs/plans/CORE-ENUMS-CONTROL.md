# CORE-ENUMS + CORE-CONTROL — исследовательский план G1 (Java SE 21)

**Статус:** `G1 PROPOSED / NOT ACCEPTED`. **Автор:** 01 — Research & Coverage. [Issue #143](https://github.com/Melchiorius/backend-interview-labs/issues/143), [поручение 00](https://github.com/Melchiorius/backend-interview-labs/issues/143#issuecomment-7072135005), программа #33. **BASE/main на момент ветки:** `346f2bd3818c1b66dd7c8822cba6424dfc911dfc`, после документарного G6 Issue #142 и G5 PR #140; точный HEAD будет зафиксирован в PR и адресном комментарии 04. **Нормативная версия:** JLS/API **Java SE 21**, без preview. **Фактическая пробная среда:** OpenJDK `javac 21.0.12.1` / `java 21.0.12.1`, Linux, `--release 21`.

## 1. Проблема и педагогическое решение

**CORE-ENUMS** отвечает на вопрос «что такое закрытый тип с именованными экземплярами, полями/поведением, идентичностью и специализированными коллекциями?». **CORE-CONTROL** — «как именно Java выбирает ветви, повторяет действия, прекращает/пропускает итерацию и вычисляет результат switch?». Связь — **enum state machine + enum switch**, но нельзя делать одно неразделимое изложение: читателю нужны и условия/циклы без enum, и API enum вне switch.

**Рекомендация G1:** **две самостоятельные статьи** с отдельными предметами и одним **общим** runnable-пакетом в G2, если это повышает проверяемость:
- будущий `knowledge/01-java-core/enums.md`: `CORE-ENUMS` (ещё отсутствует; не оформлять как активную ссылку), constructors/constant-specific behavior, API, identity, EnumSet/EnumMap и переходы состояний;
- будущий `knowledge/01-java-core/control-flow.md`: `CORE-CONTROL` (ещё отсутствует), if/loop/labels, legacy/arrow switch statement, switch expression, yield, DA/reachability, evaluation order;
- возможный `examples/core/enums-control/`: `EnumProbe` и `ControlProbe` из `7, два независимых `java`/stdout, при желании изолированные compile-negative. Техническая реализация только роль 03 после G1 ACCEPTED 00; теория — 02, проверка/редактура — 05/04.

**Почему не просто одна статья про switch:** контракт `Enum` (включая стабильное `name` и нестабильность бизнес-смысла `ordinal`) не про управляющие конструкции; `while`/`for`/labels не про enum. **Почему не три статьи:** enum switch должен иллюстрировать обе темы, а не дублироваться в отдельной третьей «связке».

**Не входят в G1/G2 пары:** полный `COL-CONTRACTS` (EnumSet/EnumMap только API-граница), `CORE-OPERATORS` (подробная таблица операторов и precedence), `CORE-SYNTAX`, `CORE-ASSERT`, `CORE-OPTIONAL`, JMM/потокобезопасность изменяемых enum-констант, JPA/JSON, реализация JVM/битовых векторов/массивов как универсальная гарантия, performance/GC/JIT, serialization graph, JPMS. Binary evolution enum при отдельной перекомпиляции — P2/not run. Не объявлять другие curricular ID закрытыми по соседству.

## 2. Реальный main: доказанное предварительное покрытие и пробелы

На актуальном main, **не по устаревшему G6 счётчику**, реально опубликованы **25 самостоятельных статей по 26/42 curricular ID**; [COVERAGE](../COVERAGE.md) и [CURRICULUM](../CURRICULUM.md) уже синхронизированы после #142. `CORE-ENUMS` и `CORE-CONTROL` остаются двумя самостоятельными **не Implemented** ID. Сверено через **полный GitHub tree (271 запись, truncated=false)** и чтение указанных файлов; все реальные относительные ссылки в данной таблице на этом BASE существуют.

| Реальный опубликованный материал | Уже объяснено | Не хватает P0 этой пары |
|---|---|---|
| [CORE-PATTERNS](../../knowledge/01-java-core/patterns.md) | pattern switch, `when`/`case null`, dominance/exhaustiveness для sealed, `MatchException` | **базовый** if/while/for/labels, classic colon fall-through, enum switch и `yield` как часть обычного control |
| [CORE-SEALED](../../knowledge/01-java-core/sealed.md) | permits, closed hierarchy, особые оговорки enum как implicitly final/sealed | enum constants/`values`/`name`/`ordinal`, пер-константное поведение, mutable state |
| [CORE-CLASSES](../../knowledge/01-java-core/classes.md) | поля, методы, конструктор, объектный инвариант | специальные правила enum constructors/constant-specific bodies |
| [CORE-INHERITANCE](../../knowledge/01-java-core/inheritance.md) | extends, final, полиморфизм | enum невозможно new/extends напрямую, implicit Enum superclass |
| [CORE-INTERFACES](../../knowledge/01-java-core/interfaces.md) | implements, default/abstract | enum implements interface и реализация по отдельным константам |
| [CORE-TYPES](../../knowledge/01-java-core/types.md) | primitive/ref/null/variable/final | switch selector, enum identity vs named value |
| [CORE-EQUALITY](../../knowledge/01-java-core/equality.md) | `==` vs `equals` и Object контракт | final identity-based `Enum.equals/hashCode`, mutation effects |
| [CORE-STREAMS](../../knowledge/01-java-core/streams.md) | stream pipeline и терминальные операции | императивные циклы и область break/continue, **не** «Stream — просто for» |
| [CORE-EXCEPTIONS](../../knowledge/01-java-core/exceptions.md) | abrupt completion, throws, finally | flow-through `break/continue/yield` и runtime исключения enum API |
| [CORE-RECORDS](../../knowledge/01-java-core/records.md) | final/special record constructors | не объявлять enum видом record |
| [CORE-IMMUTABLE](../../knowledge/01-java-core/immutability.md) | shallow/deep immutable, `final` ссылка | enum singleton с mutable полем **не** immutable по определению |

Исходные приоритеты сверены с [JAVA_CORE_PLAN](../JAVA_CORE_PLAN.md), требования G1 с [AGENTS](../../AGENTS.md), [WORKFLOW](../WORKFLOW.md), [ROLE_MODEL](../ROLE_MODEL.md) и [EXERCISE_AND_TESTING_STANDARD](../EXERCISE_AND_TESTING_STANDARD.md). Упоминание enum/switch в уже существующей статье ≠ полноценная публикация этих curricular ID.

## 3. Нормативные источники A и интервью-вопросы C

Ниже только **version-pinned** Java21 первоисточники. Банк интервью **не доказывает** правила языка и не устанавливает частоту вопросов. Идентификаторы A/C используются для каждого P0 в `4–5. При конфликте приоритет у A, затем реальная проба помогает обнаружить ошибку в интерпретации.

| ID | Нормативный источник Java 21 / назначение |
|---|---|
| A1 | [JLS 21 `8.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.9), [`8.9.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.9.1), [`8.9.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.9.2), [`8.9.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.9.3): enum declarations/constants/members, constructor constraints, implicit final/sealed, generated `values/valueOf` |
| A2 | [java.lang.Enum, Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Enum.html): `name/ordinal/compareTo/valueOf/equals/hashCode/toString/getDeclaringClass`, identity и version-specific API |
| A3 | [EnumSet, Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/EnumSet.html): один enum-type, declaration order, no null elements, weakly consistent iterator; не обещать thread-safety |
| A4 | [EnumMap, Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/EnumMap.html): enum key order, no null keys/**null values allowed**, weakly consistent iterators |
| A5 | [JLS 21 `14.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.9), [`14.12](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.12), [`14.13](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.13), [`14.14.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.14.1), [`14.14.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.14.2): if/else/while/do/basic-for/enhanced-for |
| A6 | [JLS 21 `14.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.7), [`14.15](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.15), [`14.16](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.16), [`14.17](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.17): labels/break/continue/return |
| A7 | [JLS 21 `14.11](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11), [`14.11.1.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11.1.1), [`14.11.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11.2), [`14.11.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11.3): statement/legacy/enhanced, selector/null/arrow/fall-through, labels/exhaustiveness |
| A8 | [JLS 21 `15.28](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.28), [`15.28.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.28.1), [`14.21](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.21): switch expression, result, `yield`; не путать с `break value` из ранних preview |
| A9 | [JLS 21 `14.22](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.22), [`16](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html): reachability/definite assignment |
| A10 | [JLS 21 `15.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.7), [`15.23](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.23), [`15.24](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.24): evaluation order, short-circuit conditional AND/OR |
| A11 | [JLS 21 `13](https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html), [JLS `14.11](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11): **только граница** separate compilation/version evolution, `MatchException` в возможных ситуациях, но реальный experiment **not run** |
| A12 | [Java 21 language guide: switch expressions/statements](https://docs.oracle.com/en/java/javase/21/language/switch-expressions-and-statements.html): официальный поясняющий источник **B**, не замена JLS A7/A8 |

**Интервью-источники, независимо от A:**
- **C1 / Q-HRUDAY (уже в [SOURCE_REGISTRY](../SOURCE_REGISTRY.md)):** [Core Java Interview Question Bank](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md), действительно проверенные `7.1` («Can an enum have constructors?», «Can enum constants have different behavior?», «Can enums be used in switch statements?»), `7.2` («Difference between name() and toString()», «How do enums work with EnumSet and EnumMap?»), `7.3` («How are enums used in state machines?»), `20.3` («Difference between traditional switch statements and switch expressions», «What is the yield keyword?», «Do switch expressions eliminate fall-through?»). Это **постановки**, не образцовые ответы.
- **C2 / Q-BAELDUNG-CORE и дополнительный адресный материал:** [Java Flow Control Interview Questions (+ Answers)](https://www.baeldung.com/java-flow-control-interview-questions) — реальный вопрос `break vs continue` и labelled/unlabelled control. Банк C из существующего реестра применён адресно; дополнительная страница не автоматически становится новым обязательным реестром.
- **C3 / независимый тематический контроль:** [CodeKru — Conditions and Loops in Java Interview Questions](https://tools.codekru.com/interview-prep/language/lang-control-flow), конкретные вопросы switch fall-through, break vs continue, число итераций и Java21 arrow/yield; используется как контроль возможных пробелов, не как доказательство JLS.
- **C4 / Q-TEAMLEAD** [Java Interview Questions](https://github.com/teamlead/java-interview-questions): широкий систематизатор вопросов по Java Core из реестра; не заявляем, что там есть каждый частный Java21 enum/yield вопрос, и не используем количество вопросов как статистику частоты.

**Важное проверенное исправление старого стереотипа:** `case Status.NEW` (квалифицированное имя enum-константы) **компилируется в Java 21**; `EnumProbe.E26` и реальный standalone `N27`-candidate дали exit 0. Нельзя объявлять это compile-negative на основании устаревшего интервью-ответа. JLS 21 `14.11.1 допускает case-константу, называющую enum constant и совместимую с selector type. Нельзя переносить другие правила поздних выпусков, например switch по `long`, в Java21 без preview.

## 4. Матрица CORE-ENUMS: P0

Схема строки: вопрос/контракт → нормативный источник **A** → независимый банк **C** → конкретный наблюдаемый `E##` (stdout EnumProbe) или `N##` (отдельный javac-negative) и ограничение. Все P0 должны стать собственными объяснениями в G2, а не ссылками на номера.

| ID / P | Что читатель обязан объяснить | A/C | Проверяемый пример, граница |
|---|---|---|---|
| E-01 P0 | Enum — отдельный restricted class, наследующий `Enum<E>`; константы — экземпляры и implicit static final fields | A1,A2/C1 | `E01,E23,N03,N32`; не record/sealed обычного класса |
| E-02 P0 | Набор и порядок констант, `values()` vs `valueOf(String)` | A1,A2/C1 | `E01,E05,E27,N06,N07` |
| E-03 P0 | `name()` гарантирует точный identifier; `toString` можно переопределять | A2/C1 | `E02,E04,E30`; не использовать `toString` как стабильный код |
| E-04 P0 | `ordinal()` — позиция в объявлении с нуля, **не** постоянный business-ID | A2/C1 | `E03`, явный `Status.code()=67` `E06`; порядок может измениться при релизе |
| E-05 P0 | Enum constructor, аргументы при constant declaration, не-public/protected и запрет `new` извне | A1/C1 | `E06,N01,N02,N03`; не путать with protected `Enum` base ctor |
| E-06 P0 | Enum поля/методы, state transition и invariant без setter константы | A1/A2/C1 | `E07,E08`; практическая модель, не автоматический enum feature |
| E-07 P0 | Постоянство identity: `==` vs final `equals`/`hashCode` | A1,A2/C1 | `E05,E25,N04,N05`; не value equality arbitrary mutable fields |
| E-08 P0 | Constant-specific class bodies и реализация abstract/interface operations | A1/C1 | `E09,E10,E22,N08`; per-constant anonymous subclass, не новый вид enum-значений |
| E-09 P0 | Enum может `implements interface`, но не `extends` arbitrary class / generic enum declaration | A1/C1 | `E09,N11,N12,N32`; `N11` deliberate grammar refusal |
| E-10 P0 | Enum неявно final, если нет constant-specific body, иначе implicitly sealed; нельзя писать эти modifiers явно | A1/C1 | `E23=false,E24=true,N13`; [sealed](../../knowledge/01-java-core/sealed.md) |
| E-11 P0 | `EnumSet<E>`: type-safe single enum, declaration-order traversal, без `null` элементов | A3/C1 | `E11,E15,E16,E29,N29`; iterator weakly consistent, не full Collections |
| E-12 P0 | `EnumMap<K,V>`: enum key, declaration-order, no null key / null values allowed | A4/C1 | `E12,E13,E14,E17,E28`; не map thread-safety |
| E-13 P0 | `valueOf` exact name; unknown name → IAE; `null` → NPE | A1,A2/C1 | `E05,E18,E19`; **runtime**, не compile-negative |
| E-14 P0 | Нельзя создавать свои `values()`/`valueOf(String)` или override final `Enum.name/ordinal` | A1,A2/C1 | `N04,N05,N06,N07` |
| E-15 P0 | Enum singleton по ссылке **не означает immutable state** | A1,A2/C1 | `E20=1`; будущий `CON-JMM`, не обещать потокобезопасность |
| E-16 P0 | `getClass` vs `getDeclaringClass` при constant-specific subclass | A1,A2/C1 | `E21=true,E22=false`; не JLS reflection-курс |
| E-17 P0 | Связь enum со строго проверяемым enum switch expression, all constants covered | A1,A7,A8/C1 | `E26, C07,C08`; не утверждать, что один `default` требуется всегда |

## 5. Матрица CORE-CONTROL: P0

| ID / P | Что читатель обязан объяснить | A/C | Проверяемый пример, граница |
|---|---|---|---|
| C-01 P0 | `if/else`; dangling else привязывается к ближайшему незавершённому if | A5/C3 | `C01,C02` |
| C-02 P0 | `while` проверяет condition перед телом, `do/while` после | A5/C3 | `C10,C11` |
| C-03 P0 | Basic `for`: init/condition/update, итерации/side effects | A5/C3 | `C09,C17,C20`; не общее описание всех iterator |
| C-04 P0 | Enhanced `for`: переменная очередного элемента, копия значения, изменение local не переписывает array | A5/A10/C3 | `C12,C18`; mutation через объект-ссылку другой случай |
| C-05 P0 | `break` прерывает innermost loop или switch statement, но с label — соответствующий внешний statement | A6/C2 | `C09,N18,N21` |
| C-06 P0 | `continue` переходит к следующей итерации **loop**, `continue label` требует labelled **loop** | A6/C2 | `C17,C20,N19,N20` |
| C-07 P0 | Действие одного selector expression, сохранение порядка side effects | A7,A10/C3 | `C19=first:1`, не «selector вычисляют для каждого case» |
| C-08 P0 | Legacy `switch` colon падает сквозь соседний case до `break`/return | A7/C1,C3 | `C03=onetwo,C05=two`, `@SuppressWarnings("fallthrough")` только для проверки lint |
| C-09 P0 | Arrow `switch statement` не fall-through, но `switch statement` не является switch expression | A7,A8/C1 | `C04=one` vs `C03`; `N25` mixed syntax |
| C-10 P0 | Legacy `switch statement` может не иметь applicable labels и завершиться без действия, в отличие от expression | A7,A8/C1 | `C06=unchanged,N16` |
| C-11 P0 | `switch expression` даёт значение и должен быть exhaustive; enum covers all constants без обязательного explicit default | A7,A8/C1 | `C07,C08,N16`; evolution отдельно not run |
| C-12 P0 | `yield` внутри switch expression block/colon form, отличие от `break` statement | A8/C1 | `C07,C16,N17,N30` |
| C-13 P0 | Дубли `case/default` и не-constant case values недопустимы | A7/C1,C3 | `N14,N15,N28` |
| C-14 P0 | `case null` Java21 handling для reference selector vs legacy enum switch с null → NPE | A7,A8/C1 | `C14,C15`; [patterns](../../knowledge/01-java-core/patterns.md) содержит углублённый pattern-switch |
| C-15 P0 | Enhanced `switch statement` (например pattern label) должен быть exhaustive | A7/C1 | `N24`; не всякий classic enum switch statement exhaustive |
| C-16 P0 | Conditional `&&/||` short-circuit, слева-направо, прогноз побочных эффектов | A10/C3 | `C13=11,C21=0`; общий `CORE-OPERATORS` остаётся planned |
| C-17 P0 | Unreachable после return и definite-assignment local — отдельные статические ограничения | A9/C3 | `N22,N23`; не назвать runtime failures |
| C-18 P0 | Структура loop labels, scope label, `break absent`, нельзя `continue` в обычный labelled block | A6/C2,C3 | `C09,C20,N20,N21` |
| C-19 P0 | Запрет использовать `break`/`continue` для выхода **через** switch expression; `yield` возвращает значение | A6,A8/C1 | `N30,N31`, не путать с `switch statement` |

**Итого P0: 17 E + 19 C = 36**. Каждая строка снабжена A, конкретным C и проверяемым примером/negative. Для G4 независимый reviewer проверяет, что G2 статьи **действительно раскрывают** каждый P0 с трассой и контрпримером, а не что в плане есть таблица.

## 6. P1 и P2: удержать границы

| ID | Приоритет | Наполнение и условие |
|---|---|---|
| E-18 | P1 | `EnumSet` weakly consistent iterator: в API обещано no CME от итератора, но не обещана snapshot-детерминированность изменений; A3; не симулировать гонку без отдельного теста |
| E-19 | P1 | `EnumMap` key order и null values / distinction `get==null` vs `containsKey`, E13/E14; A4 |
| E-20 | P1 | Специальная сериализация enum сохраняет singleton identity; A1,A2, **не** общий serialization graph/`readResolve` |
| E-21 | P1 | Enum может иметь abstract method, каждый constant-specific body обязан реализовать; N08; A1 |
| E-22 | P1 | Enum constructor доступ к static non-constant запрещён при initialization, N09; A1 |
| C-20 | P1 | Список switch selector типов Java21 не включает `long/boolean/float/double`; A7 |
| C-21 | P1 | `switch` по `String` и null handling vs pattern switch; A7, `C14` |
| C-22 | P1 | Форма `case Status.NEW` допустима **в Java21**, actual E26; уточнить отличие от старых интервью-шпаргалок и qualifier |
| C-23 | P1 | `switch` rule с void statement expression допустим в statement, но не как int expression result; N26; A7,A8 |
| C-24 | P1 | `switch` не смешивает `case X:`/`case X ->` в одном switch block; N25; A7 |
| C-25 | P1 | `return` и `throw` завершают поток управления; связь с [exceptions](../../knowledge/01-java-core/exceptions.md), A6; не повторять весь курс исключений |
| E-23 | P2 | Бинарная эволюция: добавление enum constant после отдельной компиляции старого exhaustive switch, `MatchException` и runtime outcome — **not run**; A11 |
| E-24 | P2 | Отличия enum serialization/cloning/reflection от обычных классов, детали механизмов — отдельная `CORE-SERIAL/REFLECTION` |
| E-25 | P2 | Внутренности EnumSet/EnumMap, precise memory/time guarantees, thread safety — отдельные collections/JMM/benchmark |
| C-26 | P2 | Сложный DA/reachability в try/finally и nested loops, проверить за пределами `N22/N23` |
| C-27 | P2 | Эволюция switch после separate compilation, runtime `MatchException`/ICCE по условиям JLS — **not run**, не экспериментально подтверждено |
| C-28 | P2 | Pattern switch/guards/record deconstruction — **уже опубликована отдельная** [CORE-PATTERNS](../../knowledge/01-java-core/patterns.md), не закрывать заново |
| C-29 | P2 | JPMS, preview later Java, Java compiler internals/JVM bytecode, JMH/JIT/GC — not run |

**Итого: 36 P0 / 11 P1 / 7 P2**. Приоритеты локальны двум темам, не являются оценкой владения Java или тайтла кандидата.

## 7. Две проверенные полные Java21 positive-программы

Команды действительно исполнены из чистого временного каталога **вне репозитория**. Явно проверено отсутствие stderr warnings на `javac --release 21 -Xlint:all`; оба stdout сохранены и сверены повторным запуском через `diff -u` (exit 0). В G1 исходники **только в Markdown**, не коммитить `examples`/Java files до G2. **30 + 21 = 51 отдельная строка вывода двух процессов, не JUnit tests.**

### 7.1 EnumProbe.java — 30 строк stdout
~~~java
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;

public class EnumProbe {
    enum Status {
        NEW(67), PAID(80), SENT(90), DONE(100);
        private final int code;
        private int visits;
        Status(int code) { this.code = code; }
        int code() { return code; }
        void visit() { visits++; }
        int visits() { return visits; }
        boolean canMoveTo(Status next) {
            return switch (this) {
                case NEW -> next == PAID;
                case PAID -> next == SENT;
                case SENT -> next == DONE;
                case DONE -> false;
            };
        }
        @Override public String toString() { return "status-" + name().toLowerCase(); }
    }
    interface Action { int apply(int a, int b); }
    enum Operation implements Action {
        PLUS { public int apply(int a, int b) { return a + b; } },
        TIMES { public int apply(int a, int b) { return a * b; } };
    }
    enum Simple { FIRST, SECOND }
    static String operation(Operation op) { return switch(op) { case PLUS -> "+"; case TIMES -> "*"; }; }
    static String qualified(Status status) {
        return switch (status) {
            case Status.NEW -> "new";
            case Status.PAID -> "paid";
            case Status.SENT -> "sent";
            case Status.DONE -> "done";
        };
    }
    public static void main(String[] args) {
        System.out.println("E01=" + Arrays.toString(Status.values()));
        System.out.println("E02=" + Status.NEW.name());
        System.out.println("E03=" + Status.NEW.ordinal());
        System.out.println("E04=" + Status.NEW.toString());
        System.out.println("E05=" + (Status.valueOf("PAID") == Status.PAID));
        System.out.println("E06=" + Status.NEW.code());
        System.out.println("E07=" + Status.NEW.canMoveTo(Status.PAID));
        System.out.println("E08=" + Status.NEW.canMoveTo(Status.DONE));
        System.out.println("E09=" + Operation.PLUS.apply(6, 7));
        System.out.println("E10=" + operation(Operation.TIMES));
        EnumSet<Status> set = EnumSet.of(Status.DONE, Status.NEW, Status.SENT);
        System.out.println("E11=" + set);
        EnumMap<Status, String> map = new EnumMap<>(Status.class);
        map.put(Status.SENT, "sent");
        map.put(Status.NEW, "new");
        map.put(Status.DONE, null);
        System.out.println("E12=" + map);
        System.out.println("E13=" + map.containsKey(Status.DONE));
        System.out.println("E14=" + (map.get(Status.DONE) == null));
        System.out.println("E15=" + set.contains(null));
        try { set.add(null); } catch (NullPointerException ex) { System.out.println("E16=NPE"); }
        try { map.put(null, "bad"); } catch (NullPointerException ex) { System.out.println("E17=NPE"); }
        try { Status.valueOf("MISSING"); } catch (IllegalArgumentException ex) { System.out.println("E18=IAE"); }
        try { Status.valueOf(null); } catch (NullPointerException ex) { System.out.println("E19=NPE"); }
        Status.NEW.visit();
        System.out.println("E20=" + Status.NEW.visits());
        System.out.println("E21=" + (Status.NEW.getClass() == Status.NEW.getDeclaringClass()));
        System.out.println("E22=" + (Operation.PLUS.getClass() == Operation.PLUS.getDeclaringClass()));
        System.out.println("E23=" + Simple.class.isSealed());
        System.out.println("E24=" + Operation.class.isSealed());
        System.out.println("E25=" + (Status.NEW.equals(Status.valueOf("NEW"))));
        System.out.println("E26=" + qualified(Status.PAID));
        Status[] snapshot = Status.values();
        snapshot[0] = Status.DONE;
        System.out.println("E27=" + (Status.values()[0] == Status.NEW));
        System.out.println("E28=" + map.containsKey(null));
        System.out.println("E29=" + set.remove(null));
        System.out.println("E30=" + Status.NEW.name().equals(Status.NEW.toString()));
    }
}
~~~

**Эталон, реально наблюдавшийся на OpenJDK 21.0.12.1:**
~~~text
E01=[status-new, status-paid, status-sent, status-done]
E02=NEW
E03=0
E04=status-new
E05=true
E06=67
E07=true
E08=false
E09=13
E10=*
E11=[status-new, status-sent, status-done]
E12={status-new=new, status-sent=sent, status-done=null}
E13=true
E14=true
E15=false
E16=NPE
E17=NPE
E18=IAE
E19=NPE
E20=1
E21=true
E22=false
E23=false
E24=true
E25=true
E26=paid
E27=true
E28=false
E29=false
E30=false
~~~

**Ограничения:** `E01/E11/E12` используют переопределённый `toString`, но порядок set/map задаёт API, а не лексикографическое представление. `E23/E24` используют `Class.isSealed` как локальное наблюдение API/JLS, не «доказательство особенностей байткода». `E27` демонстрирует безопасное повторное получение `values()` после изменения полученного массива на *этой JVM*, но не обещает внутреннего способа хранения.

### 7.2 ControlProbe.java — 21 строка stdout
~~~java
import java.util.Arrays;
import java.util.List;

public class ControlProbe {
    enum Stage { NEW, RUNNING, DONE }
    @SuppressWarnings("fallthrough") // демонстрируем намеренный fall-through
    static String legacy(int n) {
        StringBuilder out = new StringBuilder();
        switch (n) {
            case 1: out.append("one");
            case 2: out.append("two"); break;
            default: out.append("other");
        }
        return out.toString();
    }
    static String arrow(int n) {
        StringBuilder out = new StringBuilder();
        switch (n) {
            case 1 -> out.append("one");
            case 2 -> out.append("two");
            default -> out.append("other");
        }
        return out.toString();
    }
    static String evaluate(Stage s) {
        return switch (s) {
            case NEW -> "new";
            case RUNNING -> {
                int local = 67;
                yield "run:" + local;
            }
            case DONE -> "done";
        };
    }
    static int traced() {
        int count = 0;
        outer: for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (j == 1) continue;
                if (i == 2) break outer;
                count++;
            }
        }
        return count;
    }
    static String statementWithoutDefault(int x) {
        String s = "unchanged";
        switch (x) { case 1: s = "one"; break; }
        return s;
    }
    static String danglingElse(boolean a, boolean b) {
        String value = "none";
        if (a)
            if (b) value = "inner-true";
            else value = "inner-false";
        return value;
    }
    static int sideEffects() {
        int x = 0;
        int y = 0;
        if (++x > 0 && ++y > 0) {}
        if (x > 2 && ++y > 0) {}
        return x * 10 + y;
    }
    static String nullSwitch(String input) {
        return switch (input) {
            case null -> "null-explicit";
            case "A" -> "A";
            default -> "other";
        };
    }
    public static void main(String[] args) {
        System.out.println("C01=" + danglingElse(false, true));
        System.out.println("C02=" + danglingElse(true, false));
        System.out.println("C03=" + legacy(1));
        System.out.println("C04=" + arrow(1));
        System.out.println("C05=" + legacy(2));
        System.out.println("C06=" + statementWithoutDefault(67));
        System.out.println("C07=" + evaluate(Stage.RUNNING));
        System.out.println("C08=" + evaluate(Stage.DONE));
        System.out.println("C09=" + traced());
        int w = 0;
        while (w < 3) w++;
        System.out.println("C10=" + w);
        int d = 0;
        do { d++; } while (d < 0);
        System.out.println("C11=" + d);
        int sum = 0;
        for (int v : List.of(1, 2, 3)) sum += v;
        System.out.println("C12=" + sum);
        System.out.println("C13=" + sideEffects());
        System.out.println("C14=" + nullSwitch(null));
        try {
            Stage s = null;
            String value = switch (s) {
                case NEW -> "new";
                case RUNNING -> "run";
                case DONE -> "done";
            };
            System.out.println(value);
        } catch (NullPointerException ex) { System.out.println("C15=NPE"); }
        int colonValue = switch (1) {
            case 1:
                yield 67;
            default:
                yield 0;
        };
        System.out.println("C16=" + colonValue);
        int hit = 0;
        for (int i = 0; i < 3; i++) { if (i == 1) continue; hit++; }
        System.out.println("C17=" + hit);
        int[] xs = {1, 2, 3};
        for (int item : xs) item += 67;
        System.out.println("C18=" + Arrays.toString(xs));
        int[] selector = {0};
        String once = switch (++selector[0]) { case 1 -> "first"; default -> "other"; };
        System.out.println("C19=" + once + ":" + selector[0]);
        int total = 0;
        next: for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (j == 1) continue next;
                total++;
            }
        }
        System.out.println("C20=" + total);
        int[] shortCircuit = {0};
        if (true || ++shortCircuit[0] > 0) { }
        System.out.println("C21=" + shortCircuit[0]);
    }
}
~~~

**Эталон, реально наблюдавшийся на OpenJDK 21.0.12.1:**
~~~text
C01=none
C02=inner-false
C03=onetwo
C04=one
C05=two
C06=unchanged
C07=run:67
C08=done
C09=6
C10=3
C11=1
C12=6
C13=11
C14=null-explicit
C15=NPE
C16=67
C17=2
C18=[1, 2, 3]
C19=first:1
C20=3
C21=0
~~~

**Разбор:** `C03` — намеренный fall-through (в Java21 `-Xlint:all` предупредил бы, поэтому аннотация только у этого метода). `C06` — legacy switch statement допустимо без default. `C07/C16` показывают два способа yield в switch expression (arrow block/colon). `C15` null в enum selector без `case null` даёт runtime NPE, `C14` null явно обрабатывается в Java21. `C20` продолжается **помеченный внешний цикл**, не ближайший внутренний. `C18` изменение локальной переменной item не меняет массив. `C19` показывает однократное вычисление selector, `C21` short-circuit без ++.

### Действительно исполненный протокол

~~~sh
java -version
javac -version
javac --release 21 -Xlint:all EnumProbe.java ControlProbe.java
java EnumProbe > EnumProbe.actual.stdout
java ControlProbe > ControlProbe.actual.stdout
diff -u EnumProbe.expected.stdout EnumProbe.actual.stdout
diff -u ControlProbe.expected.stdout ControlProbe.actual.stdout
~~~

**Результат:** компиляция exit 0, **stderr пустой** после целевой аннотации fall-through, оба `java` exit 0, оба `diff -u` exit 0. Входные sources/эталоны в этом Markdown можно восстановить и сравнить; никаких `.java` или `*.expected.stdout` в PR G1 вне Markdown нет.

## 8. Изолированные compile-negative Java 21

Сохранять **каждый** полный source ниже в отдельный файл `Nxx_Name.java` и компилировать *индивидуально*, не загружая другие отрицательные классы. Для каждого действительно выполнено `javac --release 21 -XDrawDiagnostics -d out probes/Nxx_Name.java` (директория out заранее создана), зафиксирован **ненулевой exit** и тематический `compiler.err.*` ключ. Точный диагностический текст/offset — **не нормативная гарантия** JLS. `N11` — специально задуманный grammar-level запрет `enum extends`, у него diagnostics `expected` по запрещённому `extends`, а не случайная незавершённая единица. Все остальные негативы — законченные файлы. Runtime `E16–E19,C15` не входят в negative javac.

| № / filename | Полный самостоятельный source (одна строка Java) | Проверенная категория первой релевантной диагностики |
|---|---|---|
| 1 / `N01_PublicConstructor.java` | `enum N01_PublicConstructor { A; public N01_PublicConstructor() {} }` | `compiler.err.mod.not.allowed.here: public` |
| 2 / `N02_ProtectedConstructor.java` | `enum N02_ProtectedConstructor { A; protected N02_ProtectedConstructor() {} }` | `compiler.err.mod.not.allowed.here: protected` |
| 3 / `N03_NewEnum.java` | `enum E03 { A; } class N03_NewEnum { E03 x = new E03(); }` | `compiler.err.enum.cant.be.instantiated` |
| 4 / `N04_OverrideOrdinal.java` | `enum N04_OverrideOrdinal { A; public int ordinal() { return 67; } }` | `compiler.err.override.meth / final` |
| 5 / `N05_OverrideName.java` | `enum N05_OverrideName { A; public String name() { return "x"; } }` | `compiler.err.override.meth / final` |
| 6 / `N06_ConflictValues.java` | `enum N06_ConflictValues { A; public static N06_ConflictValues[] values() { return null; } }` | `compiler.err.already.defined: values()` |
| 7 / `N07_ConflictValueOf.java` | `enum N07_ConflictValueOf { A; public static N07_ConflictValueOf valueOf(String s) { return A; } }` | `compiler.err.already.defined: valueOf` |
| 8 / `N08_AbstractBody.java` | `enum N08_AbstractBody { A, B { int f(){ return 67;} }; abstract int f(); }` | `compiler.err.abstract.cant.be.instantiated (A has no implementation)` |
| 9 / `N09_StaticField.java` | `enum N09_StaticField { A; static int shared=67; N09_StaticField() { System.out.println(shared); } }` | `compiler.err.illegal.enum.static.ref` |
| 10 / `N10_DuplicateEnum.java` | `enum N10_DuplicateEnum { A, B, A }` | `compiler.err.already.defined: enum constant A` |
| 11 / `N11_EnumExtends.java` | `enum N11_EnumExtends extends Object { A }` | `compiler.err.expected at invalid extends; intentional grammar diagnostic` |
| 12 / `N12_EnumGeneric.java` | `enum N12_EnumGeneric<T> { A }` | `compiler.err.enum.cant.be.generic` |
| 13 / `N13_FinalEnum.java` | `final enum N13_FinalEnum { A }` | `compiler.err.mod.not.allowed.here: final` |
| 14 / `N14_SwitchDuplicate.java` | `class N14_SwitchDuplicate { int f(int x) { return switch(x) { case 1 -> 2; case 1 -> 3; default -> 4; }; } }` | `compiler.err.duplicate.case.label` |
| 15 / `N15_NonconstantCase.java` | `class N15_NonconstantCase { int f(int x) { int dynamic=67; return switch(x) { case dynamic -> 1; default -> 0; }; } }` | `compiler.err.const.expr.req` |
| 16 / `N16_ExpressionNoDefault.java` | `class N16_ExpressionNoDefault { int f(int x) { return switch(x) { case 1 -> 67; }; } }` | `compiler.err.not.exhaustive` |
| 17 / `N17_YieldOutside.java` | `class N17_YieldOutside { int f() { yield 67; } }` | `compiler.err.no.switch.expression` |
| 18 / `N18_BreakOutside.java` | `class N18_BreakOutside { void f() { break; } }` | `compiler.err.break.outside.switch.loop` |
| 19 / `N19_ContinueOutside.java` | `class N19_ContinueOutside { void f() { continue; } }` | `compiler.err.cont.outside.loop` |
| 20 / `N20_ContinueLabelBlock.java` | `class N20_ContinueLabelBlock { void f() { block: { continue block; } } }` | `compiler.err.not.loop.label` |
| 21 / `N21_UndefinedBreakLabel.java` | `class N21_UndefinedBreakLabel { void f() { for(int i=0;i<3;i++) { break absent; } } }` | `compiler.err.undef.label` |
| 22 / `N22_UnreachableAfterReturn.java` | `class N22_UnreachableAfterReturn { int f() { return 67; System.out.println(1); } }` | `compiler.err.unreachable.stmt` |
| 23 / `N23_UnassignedVariable.java` | `class N23_UnassignedVariable { int f(boolean b) { int v; if (b) v=67; return v; } }` | `compiler.err.var.might.not.have.been.initialized` |
| 24 / `N24_StatementRuleNoExhaustive.java` | `class N24_StatementRuleNoExhaustive { void f(Object o) { switch (o) { case String s -> System.out.println(s); } } }` | `compiler.err.not.exhaustive.statement` |
| 25 / `N25_MixedSwitchKinds.java` | `class N25_MixedSwitchKinds { int f(int n) { return switch(n) { case 1 -> 67; default: yield 0; }; } }` | `compiler.err.switch.mixing.case.types` |
| 26 / `N26_VoidArrowExpression.java` | `class N26_VoidArrowExpression { int f(int n) { return switch(n) { case 1 -> System.out.println(n); default -> 0; }; } }` | `compiler.err.prob.found.req (void is not int result)` |
| 27 / `N28_DuplicateDefault.java` | `class N28_DuplicateDefault { int f(int n) { return switch(n) { case 1 -> 2; default -> 3; default -> 4; }; } }` | `compiler.err.duplicate.default.label` |
| 28 / `N29_EnumSetWrongType.java` | `import java.util.EnumSet; enum E29 { A } enum F29 { B } class N29_EnumSetWrongType { void f() { EnumSet<E29> set=EnumSet.of(E29.A); set.add(F29.B); } }` | `compiler.err.cant.apply.symbol: add incompatible enum F29` |
| 29 / `N30_BreakInSwitchExpression.java` | `class N30_BreakInSwitchExpression { int f(int n) { return switch(n) {case 1 -> { break; } default -> 0;}; } }` | `compiler.err.break.outside.switch.expression` |
| 30 / `N31_ContinueOutsideLoopInSwitch.java` | `class N31_ContinueOutsideLoopInSwitch { int f(int n) { return switch(n) {case 1 -> { continue; } default -> 0;}; } }` | `compiler.err.continue.outside.switch.expression` |
| 31 / `N32_EnumSubclass.java` | `enum E32 { A } class N32_EnumSubclass extends E32 {}` | `compiler.err.cant.inherit.from.final; enum.types.not.extensible` |

**Итог:** **31/31** исследовательских compile-negative — отдельные реальные вызовы javac; все имеют ожидаемую содержательную диагностику. Исходный кандидат `N27_QualifiedEnumCase.java` **не** входит в этот счёт: он был реально скомпилирован с `--release 21` и вернул **exit 0**. Это *положительное* уточнение семантики Java21, перенесённое в `EnumProbe.E26`. Не следует искусственно записывать допустимый Java21 код в список compile-negative.

Возможное отдельное runnable execution этих negative checks и exact stderr artifacts — решение 03/04 после G1, не часть действующей CI. Ни один исполняемый `.java` не добавлен в этот G1 PR.

## 9. Контроль нормативных ловушек и делегирование G2

1. **Конструктор enum:** явный `public`/`protected` запрещены; без модификатора enum constructor **private по норме**. Не писать, что конструктор обязан быть *синтаксически* объявлен с словом `private`. Константа получает свои аргументы конструктора при initialization. Ключевая ошибка `N09` — запрещённая ссылка на **non-constant static enum field** из конструктора до завершения initialization.
2. **Enum и sealed:** без constant-specific bodies enum implicitly `final`; с такими bodies enum implicitly `sealed`. **Нельзя** самому ставить `final` у объявления enum; не путать с `final` inherited Enum method.
3. **name/toString/ordinal:** `name` фиксирован, `toString` override, `ordinal` позиция в объявлении. Бизнес-код должен быть отдельным полем/сопоставлением; при перестановке констант ordinal меняется. Не считать enum constant по определению deeply immutable.
4. **EnumSet/EnumMap:** порядок по declaration, а не по hash/`toString`. `EnumSet.add(null)` NPE, `contains(null)` и `remove(null)` допустимы. `EnumMap` запрещает null **key**, допускает null **value**; `get(k)==null` не отличает absent и mapped-null, используйте `containsKey`. Итераторы weakly consistent, не обещать determinism при конкурентной модификации.
5. **Legacy vs enhanced switch:** традиционный `switch(int)` без pattern/null case не обязан быть exhaustive **как statement**, но любой `switch expression` exhaustive; enhanced statement (например pattern label) также exhaustive (`N24`). Arrow statement **не** expression, `yield` относится к switch expression, colon labels могут fall through.
6. **Null в Java21:** `case null` разрешён для reference selector, но отсутствие такого label для `switch` на null обычно NPE (в частности enum и соответствующий пример `C15`). Не утверждать, что обычный `default` всегда перехватывает null. Для глубокого switch/pattern matching уже есть [CORE-PATTERNS](../../knowledge/01-java-core/patterns.md).
7. **Компиляция vs выполнение:** `N01–N32` отказ javac (кроме исключённого N27); `E16/E17/E19/C15` — runtime NPE, `E18` — runtime IAE. Binary evolution при добавлении enum constant после отдельной перекомпиляции: не запускали, не записывать `MatchException` как «факт локальной пробы».
8. **Верификация:** все `E##/C##` — наблюдения конкретного OpenJDK21, normative утверждения — по A, источник C — лишь формулировка вопроса. `javac -XDrawDiagnostics` keys могут отличаться между компиляторами/патч-релизами. Нельзя выдавать «51 строку stdout» за «51 unit test» или измерение производительности.

**02 Knowledge Author:** две самостоятельные русские статьи, механизм → full trace → антипример → spec rule, с корректными мостами на `patterns.md`/`sealed.md`, без дублирования глубоких JLS pattern switch разборов. **03 Examples & Exercises:** решить с 02, нужен ли один runnable; если да, две независимо запускаемые программы/эталона 30/21 строк, negative-компиляции строго отдельно и тематическими причинами; не создавать Gradle/CI ritual. **05 Editor:** естественный русский текст без следов генеративных штампов; точные relative links и логика от состояний enum к выбору ветви. **04 Reviewer:** зафиксировать exact HEAD каждого gate, P0→готовый текст/пример; independently check N09 (early static), `case Qualified` Java21, `N24` enhanced, difference compile/runtime.

**Строгие not run:** Java snippets G1 Markdown **не исполняются проектной CI автоматически**; Gradle/JUnit проекта на этих кодах — not run; другие JDK/JVM/OS, GUI Obsidian/IntelliJ/anchors, binary evolution и отдельные classloaders, live JPMS, JMH/JIT/GC/performance, preview features и concurrency state races — not run. **Полный GitHub tree** по исходному BASE проверен отдельно (271 запись, `truncated=false`), а все существующие относительные ссылки данного файла сверены по дереву; будущие `enums.md`/`control-flow.md` не объявлены файлами.

## 10. Gate/status и передача эстафеты

| Компонент | Честный статус |
|---|---|
| G1 план, матрица `36 P0/11 P1/7 P2` | `PROPOSED / NOT ACCEPTED` |
| 2 позитивные программы, 51 stdout строка, локальная `--release 21 -Xlint:all` | `Verified locally OpenJDK 21.0.12.1`, проектный CI **not run** |
| 31 отдельный javac negative, `-XDrawDiagnostics` + категория | `Verified locally`, проектный CI **not run** |
| Самостоятельные CORE-ENUMS / CORE-CONTROL статьи | `Planned / NOT Implemented` |
| Выпуск G2, Gradle, examples, COVERAGE/CURRICULUM | **не изменялись в G1** |
| JVM/binary/JPMS/CI-G2/другие JDK | `not run / out of scope` |

### Передача эстафеты

Кому: **04 — Independent Reviewer**.
Что сделать: независимое G1-review **точного HEAD исследовательского PR**: нормативные A по JLS/API Java21, вопросы C, все 36 P0, enum final/sealed/constant behavior и `EnumSet/EnumMap`, legacy/enhanced switch, `yield`/fall-through/null, 31 изолированный diagnostic и специально **успешный** `N27`-candidate; проверить локальные 30+21 stdout, верность границ/not run. Вердикт `PASS` или адресный `CHANGES` с конкретным местом/обоснованием; после существенной правки review по **новому** SHA. После G1 PASS передать 00 для **ACCEPTED/BLOCKED и merge**, а не самому начинать G2.
Передать: оператор пересылает **прямую ссылку на отдельный опубликованный адресный PR-комментарий** с точным HEAD; не URL общего Issue и не ссылку на ещё не опубликованное сообщение.
