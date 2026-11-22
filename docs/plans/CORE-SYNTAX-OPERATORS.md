# CORE-SYNTAX + CORE-OPERATORS — G1 research (Java SE 21)

**Статус: G1 PROPOSED / NOT ACCEPTED**. **Ответственный:** 01 — Research & Coverage. **Поручение:** [Issue #151](https://github.com/Melchiorius/backend-interview-labs/issues/151), [G0→G1 comment #7076049056](https://github.com/Melchiorius/backend-interview-labs/issues/151#issuecomment-7076049056), родитель #33.
**BASE/main на старте ветки:** `62d9398040282cec1178c7fccbabef57d895f402`, G5 [PR #148](https://github.com/Melchiorius/backend-interview-labs/pull/148) уже опубликован, G6 документарных реестров — [Issue #150](https://github.com/Melchiorius/backend-interview-labs/issues/150), отдельная параллельная работа 06. **Exact HEAD этой ветки** — в GitHub PR и адресной передаче 04, после публикации документа. Норма: **JLS/JDK Java SE 21, без preview**. Среда *локально выполненных* Java-проб: OpenJDK `javac/java 21.0.12.1`, Linux Debian 13, `--release 21`, без Gradle/CI. Только исследовательский Markdown, не G2.

## 1. Область ответственности, архитектура и что должен суметь объяснить читатель

**Предлагаем две самостоятельные статьи**: `CORE-SYNTAX` (путь *будущего* `knowledge/01-java-core/syntax.md`, ещё не существует), `CORE-OPERATORS` (*будущего* `knowledge/01-java-core/operators.md`, ещё не существует). У них общая грань — контекст имён, типизация выражения и его положение в коде, но **синтаксис и область видимости не тождественны семантике вычисления**. Один возможный runnable-набор `examples/core/syntax-operators/` с двумя независимыми Java21 процессами `SyntaxProbe` и `OperatorsProbe` и небольшим multi-file package/import fixture (`PackageProbe`), только если это действительно помогает обучению; его автор — 03 после решения 00, а не исследователь 01. Нет требования отдельного Gradle/JUnit/Mockito модуля ради численности тестов.

Проверяемый результат обучения:
- По **CORE-SYNTAX**: по полным файлам Java объяснить compilation unit, `package`, обычный/static import, простое/qualified имя, public top-level filename, различие shadowing и незаконной коллизии локальных, `this`-доступ к скрытому полю, область блока, видимость имени после объявления, definite assignment, `var` и effectively final capture; при этом отличать compile error от ошибки запуска `main`.
- По **CORE-OPERATORS**: разобрать parse/grouping по приоритету, **ассоциативность** и **порядок исполнения операндов** отдельно; заранее назвать точное значение и side effects у `++/--`, `+=`, `&&/||` vs boolean `&/|`, `?:`, numeric promotion, int overflow, деление/остаток, shifts, `String +`, `==` и assignment expression; различать допустимый, но опасный код и compile-negative.

**Не объявлять Implemented:** `CORE-SYNTAX`, `CORE-OPERATORS` до G5; `CORE-ASSERT`, `CORE-OPTIONAL`, `CORE-OPERATORS`-соседство `CORE-TYPES` и `CORE-CONVERSIONS` не закрывает эти ID автоматически. JPMS, classloaders, JVM bytecode, reflection, compiler internals, полного operator overloading (которого в Java нет), deep concurrency/JMM, performance/JIT/GC/JMH, текстовые блоки как целый курс, preview unnamed classes/instance main methods — **вне G1**.

### 1.1 Фактически опубликованное дерево и границы повторения

Сверены полный [GitHub tree](https://github.com/Melchiorius/backend-interview-labs/tree/main) BASE (282 записи, `truncated=false`) и существующие учебные статьи. На G5 #148 фактически **27 самостоятельных Java Core статей и 28 из 42 curricular ID**, 14 ещё Planned/не Implemented. Реестры [CURRICULUM](../CURRICULUM.md) и [COVERAGE](../COVERAGE.md) в момент выдачи Issue могли отставать до G6 #150; **не** использовать старое число в реестрах как доказательство, что Enums/Control не существуют.

| Уже опубликованная статья, реально существующая локальная ссылка | Уже раскрытая глубина | Наша недостающая P0 / граница |
|---|---|---|
| [CORE-TYPES](../../knowledge/01-java-core/types.md) | переменная, примитивы, `var`, `final`, `==` | syntax scope/name-resolution, operator-specific evaluation traces |
| [CORE-CONVERSIONS](../../knowledge/01-java-core/conversions.md) | типовые conversion contexts, binary numeric promotion и narrowing | применение этих правил в `+=`, `?:`, shift, overflow и concatenation с побочными эффектами |
| [CORE-PARAMS](../../knowledge/01-java-core/parameters.md) | передача по значению, evaluation argument order | гарантированный order как общий закон JLS `15.7, не второй текст о параметрах |
| [CORE-CLASSES](../../knowledge/01-java-core/classes.md) | class, field, constructor, `this` | structure of compilation unit, package/import/локальный scope |
| [CORE-INIT](../../knowledge/01-java-core/initialization.md) | порядок инициализации класса и полей | структура файлов и expression order **не** class-init algorithm |
| [CORE-CONTROL](../../knowledge/01-java-core/control-flow.md) | if/loop/switch, short-circuit как часть трассы, DA/reachability | полная операционная семантика приоритета, eager boolean ops, assignment and shifts |
| [CORE-ENUMS](../../knowledge/01-java-core/enums.md) | enum и switch-константы | constant expression and name resolution без повторения enum API |
| [CORE-PATTERNS](../../knowledge/01-java-core/patterns.md) | pattern binding scopes, `switch` | lexical scope обычных locals и imports |
| [CORE-FUNCTIONS](../../knowledge/01-java-core/functions.md) | `effectively final` capture, lambda lexical scope | почему compiler `SY14` отказывает, без нового курса lambda |
| [CORE-EQUALITY](../../knowledge/01-java-core/equality.md) | object/reference equality и `hashCode` | операторы `==`/`!=` и NaN/signed-zero как numeric subtleties |
| [CORE-ACCESS](../../knowledge/01-java-core/access.md) | public/private/protected/package-private | доступность *top-level* типов при package/import, multi-file example |
| [CORE-STRINGS](../../knowledge/01-java-core/strings.md) | immutability, concatenation/string API | `+` grouping vs order, conversions in expression |

Структура курса сверена также с [JAVA_CORE_PLAN](../JAVA_CORE_PLAN.md); [AGENTS](../../AGENTS.md), [ROLE_MODEL](../ROLE_MODEL.md), [WORKFLOW](../WORKFLOW.md), [EXERCISE_AND_TESTING_STANDARD](../EXERCISE_AND_TESTING_STANDARD.md). **В будущем** после G5/G6 связать новые статьи ссылками с типами/параметрами/контролем, сейчас не создавать ссылки на несуществующие файлы и не менять эти документы.

## 2. Источники: version-pinned A/JLS/JDK21 и независимые вопросы C

В каждом P0 ниже A — **доказательство**, C — **реальный внешний вопрос**, не гарантия правильного ответа или частоты появления на интервью. По [SOURCE_REGISTRY](../SOURCE_REGISTRY.md) статус `OFF-JLS21`/`OFF-API21` — A; `Q-HRUDAY` и `Q-BAELDUNG-CORE` — C. Ссылки проверены по оглавлениям именно Java 21.

| A | Нормативная точка, Java SE 21 |
|---|---|
| A1 | [JLS `2.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-2.html#jls-2.3), [`3.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.3), [`3.8/`3.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.8): token/grammar, Unicode escapes, identifiers and keywords |
| A2 | [JLS `7.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.3), [`7.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.4), [`7.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.6): ordinary compilation unit, package, public top-level constraints and host file names |
| A3 | [JLS `7.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.5), [`7.5.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.5.1), [`7.5.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.5.2), [`7.5.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.5.3), [`7.5.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.5.4): type/static import, on-demand/single, ambiguity, no implicit import of subpackages |
| A4 | [JLS `6.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.3), [`6.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.4.1), [`6.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.4.2), [`6.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.5), [`6.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6): lexical scope, shadowing vs obscuring vs hiding, name resolution, accessibility |
| A5 | [JLS `14.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.2), [`14.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.4), [`14.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.4.1), [`16](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html), [`4.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4): blocks, locals, `var`, DA and final |
| A6 | [JLS `15.27.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27.2), [`8.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4), [JDK21 java launcher](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html): lambda capture, method signature, ordinary `public static void main(String[] args)` entry point and invocation rules |
| A7 | [JLS `15.7.1–`15.7.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.7), [`15.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.2): Java **guarantees left-to-right operand and argument evaluation**, precedence/grouping and associativity are separate questions |
| A8 | [JLS `15.14.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.14.2), [`15.15.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.15.1), [`15.26.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.26.1), [`15.26.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.26.2): post/pre increment, simple and compound assignment, compound saved LHS value |
| A9 | [JLS `5.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.6), [`4.2.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.2.2), [`15.17](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.17), [`15.18.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.18.2): promotion, int overflow, division/remainder and integer division by zero runtime behavior |
| A10 | [JLS `15.22.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.22.2), [`15.23](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.23), [`15.24](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.24), [`15.25](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.25): boolean eager vs short-circuit, conditional operator typing and conditional evaluation |
| A11 | [JLS `15.19](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.19), [`15.21](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.21), [`15.18.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.18.1): shift masking, numeric/reference equality, String concatenation |
| A12 | [JLS `15.29](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.29), [`14.11](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11): compile-time constant expressions and switch labels (switch entire course is [CORE-CONTROL](../../knowledge/01-java-core/control-flow.md)) |

**C1 / Q-HRUDAY:** [Core Java Interview Question Bank README](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md), **действительно прочитанные** ``2–4: `Java Program Structure` («What is the structure of a Java program?», «Why is the main() method static?», «What happens if main() is declared private?»); `Variables & Data Types` («What is variable shadowing?», «What is variable hiding?», «What happens when integer overflow occurs?»); `Operators` («What is the difference between && and &?», «Explain short-circuit evaluation», «What are shift operators?», «What is the ternary operator?», «What is operator precedence?», «What is operator associativity?»). `Packages` («What happens if two imported classes have the same name?»). Вопросы существуют в банке; **не** приписываем ему P0 answer, guarantee или подтверждённую частотность.

**C2 / Q-BAELDUNG-CORE:** [Java Interview Questions — Q2 What Is the Difference Between Import and Static Imports?](https://www.baeldung.com/java-interview-questions), конкретный вопрос со static member import и обычными imports; `Q3 Which Access Modifiers Are Available` — дополнение о package-private/public top-level. Первоисточник JLS приоритетнее любых обобщений страницы. На этом же сайте не принимать спорные ответы про thread-safety/implementation в качестве норм.

**C3 / Q-TEAMLEAD (дополнительный широкий контроль):** [teamlead/java-interview-questions](https://github.com/teamlead/java-interview-questions). Реестр уже отмечает, что «частота» в подборке — данные её автора, не наша статистика; не приписываем конкретную формулировку без адресной проверки. Для каждого P0 указаны подтверждённые `C1` и, где применимо, `C2`; соответствующий авторский вопрос расширен граничным случаем без утверждения, что банк приводит ровно такой же пример.

## 3. CORE-SYNTAX — P0 matrix (16 вопросов)

`SYxx` — строки stdout `SyntaxProbe`; `PKxx` — `PackageProbe`; `SY-Nxx` — один из отдельных compiler-negative. Приоритет P0/P1/P2 — задача учебного покрытия, **не** рейтинг уровня собеседования. Для каждого P0 закреплены A, C и проверяемая probe.

| ID / P | Вопрос, механизм и контрпример | A / независимый C | Демонстрация, граница |
|---|---|---|---|
| SY-01 P0 | Compilation unit = optional package, imports, top-level declarations, **не** объект и не entry method | A1,A2 / C1 | `SyntaxProbe` плюс `PackageProbe`; `SY-N16` import after type запрещён |
| SY-02 P0 | Named package vs unnamed package, почему имя пакета не classloader API | A2,A4 / C1 | `PK01` vs `SY01`; separate files обязательно |
| SY-03 P0 | Single type import vs static member import; imports позволяют короткое имя, но не создают поле | A3 / C1,C2 | `SY03,PK01–PK03`; `SY-N17` instance field не является импортируемым static member |
| SY-04 P0 | Explicit import, on-demand ambiguity, single-type collisions | A3,A4 / C1,C2 | `SY-N11,SY-N13`; в `SY09` fully qualified name обходится без импорта |
| SY-05 P0 | Simple vs fully qualified name, implicit `java.lang`, доступность не значит «автозагрузка» | A2,A3,A4 / C1,C2 | `SY09,PK03`; не class loading |
| SY-06 P0 | `public` top-level, host file naming и два public top-level в одном .java | A2 / C1,C2 | `SY-N09,SY-N10`; конкретная диагностика `javac`, не абстрактная обязанность любой файловой системы |
| SY-07 P0 | Local blocks and scope: локальная переменная не видна за границами блока | A4,A5 / C1 | `SY13,SY14,SY-N03`; не поля объекта |
| SY-08 P0 | Local variable shadows field, `this.field` разрешает явное обращение к полю | A4,A5 / C1 | `SY01,SY02`; отличить от повторного local declaration `SY-N01` |
| SY-09 P0 | Duplicate local in overlapping scope illegal, не говорить «в Java всегда разрешён nested shadowing» | A4,A5 / C1 | `SY-N01`; допустимое field-shadowing — SY01 |
| SY-10 P0 | Имя local доступно после объявления, не до; попытка вне scope — другая диагностика | A4,A5 / C1 | `SY-N02,SY-N03`; qualified name для локальной нет |
| SY-11 P0 | Definite assignment: существует объявление, но возможное неприсваивание при достижении чтения — compile error | A5 / C1 | `SY06,SY07` vs `SY-N04` |
| SY-12 P0 | `var` — inference при initialized local, не динамический тип; нет `var field`, `var n=null`, multi-declarator | A5 / C1 | `SY08`, `SY-N05..SY-N08`; углубление типов уже [CORE-TYPES](../../knowledge/01-java-core/types.md) |
| SY-13 P0 | Effectively-final local captured by lambda, capture после reassign compile error | A5,A6 / C1 | `SY05` vs `SY-N14`; [CORE-FUNCTIONS](../../knowledge/01-java-core/functions.md) раскрывает lambda |
| SY-14 P0 | Обычный `public static void main(String[] args)` JDK21 launcher contract и args, не preview instance-main | A6 / C1 | `SY10` и три запуска JVM; `java` требует подходящий entry point при нормальном class launch |
| SY-15 P0 | Java lexical tokens/keywords и позиция `import` перед top-level declarations | A1,A2 / C1 | корректные imports в двух исходниках vs намеренно grammar-level `SY-N16`; не полноценный tokenizer |
| SY-16 P0 | Нельзя статически импортировать **instance** field; import ambiguity ≠ scope collision | A3,A4 / C1,C2 | `SY-N12,SY-N17`; `PK01` корректно статически импортирует constant и method |

## 4. CORE-OPERATORS — P0 matrix (20 вопросов)

| ID / P | Что объяснить и почему общий лозунг неверен | A / независимый C | Проба и граница |
|---|---|---|---|
| OP-01 P0 | Precedence задаёт группировку `1+2*3` vs `(1+2)*3` | A7,A9 / C1 | `OP01=7,OP02=9` |
| OP-02 P0 | Associativity/grouping ≠ evaluation order; Java оценивает аргументы/операнды слева направо | A7 / C1 | `OP21=67:LR,OP25=3:3` |
| OP-03 P0 | Postfix возвращает старое, prefix — новое, а `x=x++` возвращает старое значение самому x | A8 / C1 | `OP03–OP05`; `OP-N13` требует variable |
| OP-04 P0 | Compound assignment includes narrowing, `short += 1` vs `short = short + 1` | A8,A9 / C1 | `OP06=-32768` / `OP-N02`, нет скрытого «безопасного int» |
| OP-05 P0 | Java `int` overflow wraps, без автоматически бросаемого exception | A9 / C1 | `OP07=-2147483648`; отдельные checked arithmetic APIs вне этой матрицы |
| OP-06 P0 | Integer division truncates toward zero; remainder sign of dividend; zero causes runtime `ArithmeticException` | A9 / C1 | `OP08,OP26,OP27`, отличие runtime от compile-negative |
| OP-07 P0 | Binary numeric promotion меняет тип вычисления до assignment | A9 / C1 | `OP18/OP19` (контраст conditional numeric), `OP-N02/OP-N01`; подробные conversions см. [CORE-CONVERSIONS](../../knowledge/01-java-core/conversions.md) |
| OP-08 P0 | Shift distance masking (`int` low 5, `long` low 6 bits), arithmetic `>>` vs logical `>>>` | A11 / C1 | `OP09=2,OP10=2147483647,OP11=2` / `OP-N06` |
| OP-09 P0 | `&&` пропускает правый operand когда левый false | A10 / C1 | `OP12=false:0` |
| OP-10 P0 | `&`/`|` with boolean evaluate BOTH operands, в отличие от `&&`/`||` | A10 / C1 | `OP13=false:1,OP15=true:1` |
| OP-11 P0 | `||` short circuit при true, правая сторона не вызывается | A10 / C1 | `OP14=true:0` |
| OP-12 P0 | `?:` выполняет только выбранную branch, тип вычисляется по правилам conditional expression | A10 / C1 | `OP18,OP19,OP24=67:0` / `OP-N05` |
| OP-13 P0 | String concatenation `+` left-associative: `1+2+"x"` ≠ `"x"+1+2` | A11 / C1 | `OP16=3x,OP17=x12`, исходные `String` не меняются |
| OP-14 P0 | `==` vs `equals`: numeric equality и reference identity — разные контракты | A11 / C1 | `OP22=false` для NaN и `OP-N12` bad type; связь [CORE-EQUALITY](../../knowledge/01-java-core/equality.md) |
| OP-15 P0 | NaN comparison and signed zeros floating equality/`Double.compare` не одно отношение | A9,A11 / C1 | `OP22=false, OP23=-1`; не полный floating-point курс |
| OP-16 P0 | Compound assignment remembers original LHS value BEFORE right operand side effects | A7,A8 / C1 | `OP20=4` при `a += (a=3)` |
| OP-17 P0 | Argument evaluation guaranteed left-to-right, не «порядок неопределён, как в C» | A7 / C1 | `OP21=67:LR` |
| OP-18 P0 | Assignment expression value, right association `a=b=d` | A8 / C1 | `OP25=3:3`; `OP-N10` unassigned compound |
| OP-19 P0 | Operator applicability: boolean not numeric, unary `+` numeric, shifts require integral | A8,A9,A10,A11 / C1 | `OP-N04,OP-N06–OP-N08,OP-N14` |
| OP-20 P0 | Compile-time constant case vs runtime expression: `case dynamic` illegal; `int n=1/0` can compile yet fail at runtime | A9,A12 / C1 | `OP-N09` vs `OP27=ArithmeticException:1`, separate probe `CheckDivisionZeroCompile` compiled exit 0; no false compile-negative |

**Итого: 36 P0 (SY 16 + OP 20)**. Чтобы принять G2 как полноценное объяснение, **каждая** P0 должна иметь связный раздел статьи с механизмом, пояснением границ и воспроизводимым example/negative, а не просто метку в таблице.

## 5. P1 и P2 (не завышать обещания)

| ID | Приоритет | Задача / граница |
|---|---|---|
| SY-17 | P1 | Shadowing fields, hiding static member, obscuring package/type names: раздельные определения A4, не сводить к «одноимённая переменная скрывает всё» |
| SY-18 | P1 | Single-type import priority vs on-demand wildcard, static imports source collision; `SY-N11..SY-N13` |
| SY-19 | P1 | Package-private top-level cross-package negative `SY-N15` vs `public` import `PK01`, без JPMS runtime |
| SY-20 | P1 | Source-file naming and multiple package-private top-level declarations; ограничения host/compiler A2 |
| SY-21 | P1 | `var` restricted identifier and Java 21 stable vs preview; запрет var fields/standalone null/type params |
| SY-22 | P1 | Unicode escape translation happens before tokenization, комментарий с escaped newline / опасность спонтанного parser trouble; A1, не давать непроверенный negative |
| SY-23 | P1 | Package/import resolution vs access control; import не делает package-private тип доступным |
| OP-21 | P1 | Unary/binary `+` overload by built-in string concatenation only; Java не позволяет arbitrary operator overloading |
| OP-22 | P1 | `>>>` vs `>>` negative values and exact masked shift distance; `OP09..11` |
| OP-23 | P1 | Expression `i=i++`, compound assignment side-effect traps, уметь проследить saved LHS; `OP05/OP20` |
| OP-24 | P1 | String conversion `null`/overloaded `+` vs `StringBuilder` — [CORE-STRINGS](../../knowledge/01-java-core/strings.md), отдельные corner cases не дублировать |
| OP-25 | P1 | `?:` numeric typing vs target typing poly expressions, API overload выбор — [CORE-CONVERSIONS](../../knowledge/01-java-core/conversions.md) и [CORE-PARAMS](../../knowledge/01-java-core/parameters.md) |
| OP-26 | P1 | Constant expressions can narrow byte/short/char only if representable; `OP-N15` invalid 128, `OP18` valid |
| SY-24 | P2 | Module path/exports and precise compiler file-system/launcher variants, Java21 preview unnamed class instance main **not run** |
| SY-25 | P2 | Classloader initialization, reflection, annotation processors, JShell, IDE/Git/Obsidian navigation **not run** |
| SY-26 | P2 | Pattern variable flow scopes more deeply than already published [CORE-PATTERNS](../../knowledge/01-java-core/patterns.md), no duplicate curriculum promotion |
| OP-27 | P2 | Floating NaN/infinities/underflow subnormal, strictfp/strict floating semantics beyond two selected contrasts |
| OP-28 | P2 | Java Memory Model/happens-before/reordering/volatile vs observable evaluation order; CON-JMM planned, **not run** |
| OP-29 | P2 | JVM bytecode/JIT/GC/JMH vs logical language-level order; **not run** |
| OP-30 | P2 | Operator details for array access side effects, generic boxing/NPE, long expression chains and exhaustive source-tooling tests, left for future |

**Итого:** 36 P0, 13 P1 (7 SY + 6 OP), 7 P2 (3 SY + 4 OP). Никакой P2 из таблицы не объявляется Implemented или Verified «по соседству».

## 6. Проверенные positive-проекты — весь исходный Java21 код внутри G1 Markdown

**Действительно выполнено:** OpenJDK 21.0.12.1 / Linux, чистый временный каталог; два независимых single-file source `SyntaxProbe.java`, `OperatorsProbe.java` и один дополнительный корректный multi-file package fixture (`p/Helper.java` + `demo/PackageProbe.java`). Компиляция `--release 21 -Xlint:all` прошла с **exit 0 и пустым stderr**; каждый `java` запуск дал exit 0, `diff -u` с эталоном exit 0. Два основные JVM процесса содержат **14+27=41** строки stdout; вместе с третьим package-probe **44 строки**. Эти строки — не JUnit tests. Java **исходники не добавлены** в репозиторий G1.

### 6.1 SyntaxProbe.java (полная автономная единица, 14 stdout)

~~~java
import static java.lang.Math.max;
import java.util.List;
import java.util.function.IntSupplier;

public class SyntaxProbe {
    static int value = 67;
    private int balance = 9;

    static String shadow() {
        int value = 7;
        int inside;
        {
            int inner = value + 1;
            inside = inner;
        }
        return value + ":" + SyntaxProbe.value + ":" + inside;
    }

    int fieldShadow(int balance) {
        return balance + this.balance;
    }

    static int capture() {
        int stable = 67;
        IntSupplier supplier = () -> stable + 1;
        return supplier.getAsInt();
    }

    static String blocks(boolean chosen) {
        String answer;
        if (chosen) {
            answer = "yes";
        } else {
            answer = "no";
        }
        return answer;
    }

    public static void main(String[] args) {
        System.out.println("SY01=" + shadow());
        System.out.println("SY02=" + new SyntaxProbe().fieldShadow(5));
        System.out.println("SY03=" + max(6, 7));
        System.out.println("SY04=" + List.of("x", "y").size());
        System.out.println("SY05=" + capture());
        System.out.println("SY06=" + blocks(false));
        System.out.println("SY07=" + blocks(true));
        var words = List.of("Java", "21");
        System.out.println("SY08=" + words.get(0) + words.get(1));
        System.out.println("SY09=" + java.util.Map.of("k", 67).get("k"));
        System.out.println("SY10=" + (args.length == 0));
        System.out.println("SY11=" + SyntaxProbe.class.getSimpleName());
        System.out.println("SY12=" + ("hello" instanceof String));
        int outer = 5;
        { int inner = outer + 2; System.out.println("SY13=" + inner); }
        System.out.println("SY14=" + outer);
    }
}
~~~

**Эталон, действительно полученный (14 строк):**
~~~text
SY01=7:67:8
SY02=14
SY03=7
SY04=2
SY05=68
SY06=no
SY07=yes
SY08=Java21
SY09=67
SY10=true
SY11=SyntaxProbe
SY12=true
SY13=7
SY14=5
~~~

`SY01` законно shadowed поле **классом local**, `SY02` — параметр и `this.balance`. `SY05` — effectively final capture, запрет reassign ниже в `SY-N14`. `SY13` — локальная переменная `inner` существует только в block.

### 6.2 OperatorsProbe.java (полная автономная единица, 27 stdout)

~~~java
public class OperatorsProbe {
    static boolean mark(int[] counter, boolean result) {
        counter[0]++;
        return result;
    }
    static int trace(StringBuilder order, String symbol, int value) {
        order.append(symbol);
        return value;
    }
    static int two(int a, int b) { return a * 10 + b; }

    public static void main(String[] args) {
        System.out.println("OP01=" + (1 + 2 * 3));
        System.out.println("OP02=" + ((1 + 2) * 3));
        int x = 67;
        int old = x++;
        System.out.println("OP03=" + old + ":" + x);
        int now = ++x;
        System.out.println("OP04=" + now + ":" + x);
        x = 4;
        x = x++;
        System.out.println("OP05=" + x);
        short s = Short.MAX_VALUE;
        s += 1;
        System.out.println("OP06=" + s);
        System.out.println("OP07=" + (Integer.MAX_VALUE + 1));
        System.out.println("OP08=" + (-7 / 3) + ":" + (-7 % 3));
        System.out.println("OP09=" + (1 << 33));
        System.out.println("OP10=" + (-1 >>> 1));
        System.out.println("OP11=" + (1L << 65));
        int[] c1 = {0};
        boolean sc = false && mark(c1, true);
        System.out.println("OP12=" + sc + ":" + c1[0]);
        int[] c2 = {0};
        boolean eager = false & mark(c2, true);
        System.out.println("OP13=" + eager + ":" + c2[0]);
        int[] c3 = {0};
        boolean or = true || mark(c3, false);
        System.out.println("OP14=" + or + ":" + c3[0]);
        int[] c4 = {0};
        boolean eagerOr = true | mark(c4, false);
        System.out.println("OP15=" + eagerOr + ":" + c4[0]);
        System.out.println("OP16=" + (1 + 2 + "x"));
        System.out.println("OP17=" + ("x" + 1 + 2));
        byte small = true ? 1 : 2;
        System.out.println("OP18=" + small);
        Number numeric = true ? 1 : 2.5;
        System.out.println("OP19=" + numeric + ":" + numeric.getClass().getSimpleName());
        int assign = 1;
        assign += (assign = 3);
        System.out.println("OP20=" + assign);
        StringBuilder order = new StringBuilder();
        int result = two(trace(order, "L", 6), trace(order, "R", 7));
        System.out.println("OP21=" + result + ":" + order);
        System.out.println("OP22=" + (Double.NaN == Double.NaN));
        System.out.println("OP23=" + Double.compare(-0.0, 0.0));
        int[] c5 = {0};
        int choose = true ? 67 : (c5[0]++);
        System.out.println("OP24=" + choose + ":" + c5[0]);
        int a = 1, b = 2, d = 3;
        a = b = d;
        System.out.println("OP25=" + a + ":" + b);
        System.out.println("OP26=" + (7 / 3) + ":" + (7 % 3));
        int[] side = {0};
        try {
            int zero = Integer.parseInt("0");
            int notValue = side[0]++ / zero;
            System.out.println(notValue);
        } catch (ArithmeticException ex) {
            System.out.println("OP27=ArithmeticException:" + side[0]);
        }
    }
}
~~~

**Эталон, действительно полученный (27 строк):**
~~~text
OP01=7
OP02=9
OP03=67:68
OP04=69:69
OP05=4
OP06=-32768
OP07=-2147483648
OP08=-2:-1
OP09=2
OP10=2147483647
OP11=2
OP12=false:0
OP13=false:1
OP14=true:0
OP15=true:1
OP16=3x
OP17=x12
OP18=1
OP19=1.0:Double
OP20=4
OP21=67:LR
OP22=false
OP23=-1
OP24=67:0
OP25=3:3
OP26=2:1
OP27=ArithmeticException:1
~~~

**Контроль тонкостей:** `OP20` сохраняет старую величину LHS до RHS side-effect; `OP21` не «компилятор случайно выбрал LR», а Java гарантирует left-to-right. `OP19` у выбранной `int` branch при наличии `double` branch numeric expression становится `double`. `OP27` деление на ноль **компилируется** и вызывает runtime exception; dynamic `zero` специально предотвращает ненужное lint-предупреждение об очевидном литеральном `/0`, но не меняет демонстрируемый контракт. `OP23` различает `Double.compare` и `==` для signed zero.

### 6.3 Корректная multi-file package/static-import конфигурация, третий запуск

Дерево из **двух отдельных** .java с соответствующими package statements, не один «условно исполняемый» файл:

~~~text
p/Helper.java
demo/PackageProbe.java
~~~

`p/Helper.java`:
~~~java
package p;
public final class Helper {
    private Helper() {}
    public static final int VALUE = 67;
    public static String greet() { return "hello"; }
}
~~~

`demo/PackageProbe.java`:
~~~java
package demo;
import p.Helper;
import static p.Helper.greet;
import static p.Helper.VALUE;
public class PackageProbe {
    public static void main(String[] args) {
        System.out.println("PK01=" + greet() + ":" + VALUE);
        System.out.println("PK02=" + Helper.VALUE);
        System.out.println("PK03=" + java.util.List.of(6, 7).size());
    }
}
~~~

**Эталон третьего процесса (3 строки):**
~~~text
PK01=hello:67
PK02=67
PK03=2
~~~

**Фактически исполненные команды:**
~~~sh
java -version
javac -version
javac --release 21 -Xlint:all SyntaxProbe.java OperatorsProbe.java
java SyntaxProbe > SyntaxProbe.actual.stdout
java OperatorsProbe > OperatorsProbe.actual.stdout
javac --release 21 -Xlint:all -d out p/Helper.java demo/PackageProbe.java
java -cp out demo.PackageProbe > PackageProbe.actual.stdout
diff -u SyntaxProbe.expected.stdout SyntaxProbe.actual.stdout
diff -u OperatorsProbe.expected.stdout OperatorsProbe.actual.stdout
diff -u PackageProbe.expected.stdout PackageProbe.actual.stdout
~~~

После создания файлов `*.expected.stdout` с содержимым выше в локальном временном каталоге обе компиляции `exit 0, stderr empty`, все 3 процесса `exit 0`, все три `diff -u exit 0`. SHA-256 (байты stdout, UTF-8, строки завершаются LF): Syntax `c5c7fd65b40875dd8306dd787cfec87214b4e1794bd3dc474e5fff41e18ee5d5`; Operators `aef549331bdf46bf59ef13d297db5f84748e9cf312d7ede6496e2fc355ae0d40`; Package `4663c41c673307bf01e60da8555fb821effbdb80066281e0dc072a80e17407ad`. Контрольные суммы — evidence конкретных файлов опыта, не обещание формата вывода в другом locale/JDK.

## 7. Compile-negative: содержательная диагностика, не неполный файл

**Выполнено на OpenJDK javac 21.0.12.1**: все **32** приведённых ниже самостоятельных исходника по одному сохранялись как `negative/<filename>` и каждый отдельно компилировался командой `javac --release 21 -XDrawDiagnostics -d negative/out negative/<filename>`; **32/32 exit != 0** с релевантным `compiler.err.*` ключом, который приведён в правой колонке. `SY-N16` — специально помеченный grammar negative (import после class), позиция ошибки точно на запрещённом `import`, не accidental EOF. `SY-N09/SY-N10` зависят от конкретного файлового правила реализации javac и имен файлов, как допускает JLS `7.6`. **Каждая** строка — полная самостоятельная compilation unit; при проверке не склеивать всё в один исходник. Перечень ниже содержит 16 SY и 16 OP.

| ID — отдельный filename | Полный source (одна или несколько строк) | Проверенная первая тематическая категория `compiler.err.*` |
|---|---|---|
| `SY01_DuplicateLocal.java` | `class SY01_DuplicateLocal { void f() { int v = 1; int v = 2; } }` | `compiler.err.already.defined` |
| `SY02_BeforeDeclaration.java` | `class SY02_BeforeDeclaration { void f() { System.out.println(n); int n = 67; } }` | `compiler.err.cant.resolve.location` |
| `SY03_OutsideBlock.java` | `class SY03_OutsideBlock { void f() { { int n = 67; } System.out.println(n); } }` | `compiler.err.cant.resolve.location` |
| `SY04_Unassigned.java` | `class SY04_Unassigned { int f(boolean condition) { int n; if (condition) n = 67; return n; } }` | `compiler.err.var.might.not.have.been.initialized` |
| `SY05_VarNoInit.java` | `class SY05_VarNoInit { void f() { var n; n = 67; } }` | `compiler.err.cant.infer.local.var.type` |
| `SY06_VarNull.java` | `class SY06_VarNull { void f() { var n = null; } }` | `compiler.err.cant.infer.local.var.type` |
| `SY07_VarField.java` | `class SY07_VarField { var n = 67; }` | `compiler.err.restricted.type.not.allowed.here` |
| `SY08_VarMany.java` | `class SY08_VarMany { void f() { var a = 1, b = 2; } }` | `compiler.err.restricted.type.not.allowed.compound` |
| `SY09_PublicMismatch.java` | `public class ActualDifferentName { public static void main(String[] a) {} }` | `compiler.err.class.public.should.be.in.file` |
| `SY10_TwoPublic.java` | `public class SY10_TwoPublic {} public class MoreThanOne {}` | `compiler.err.class.public.should.be.in.file` |
| `SY11_AmbigDemand.java` | `import java.util.*; import java.awt.*; class SY11_AmbigDemand { List<String> items; }` | `compiler.err.ref.ambiguous` |
| `SY12_AmbigStatic.java` | `import static java.lang.Math.max; import static java.lang.Integer.max; class SY12_AmbigStatic { int n = max(1,2); }` | `compiler.err.ref.ambiguous` |
| `SY13_TwoSingleImports.java` | `import java.util.List; import java.awt.List; class SY13_TwoSingleImports { List names; }` | `compiler.err.ref.ambiguous + already.defined.single.import` |
| `SY14_ReassignedCapture.java` | `import java.util.function.IntSupplier; class SY14_ReassignedCapture { int f() { int n=1; IntSupplier s=() -> n; n=2; return s.getAsInt(); } }` | `compiler.err.cant.ref.non.effectively.final.var` |
| `SY16_ImportAfterClass.java` | `class SY16_ImportAfterClass {} import java.util.List;` | `compiler.err.expected4 (intentional grammar violation at import)` |
| `SY17_ImportInstanceField.java` | `import static java.awt.Point.x; class SY17_ImportInstanceField { int value = x; }` | `compiler.err.cant.resolve.location (no static member x)` |
| `OP01_LongToInt.java` | `class OP01_LongToInt { int n = 67L; }` | `compiler.err.prob.found.req (long → int loss)` |
| `OP02_ShortPlus.java` | `class OP02_ShortPlus { void f() { short s=1; s = s + 1; } }` | `compiler.err.prob.found.req (int → short loss)` |
| `OP03_FinalIncrement.java` | `class OP03_FinalIncrement { void f() { final int n = 67; n++; } }` | `compiler.err.cant.assign.val.to.var` |
| `OP04_StringUnaryPlus.java` | `class OP04_StringUnaryPlus { String s = +"x"; }` | `compiler.err.operator.cant.be.applied` |
| `OP05_TernaryMismatch.java` | `class OP05_TernaryMismatch { int n = true ? "x" : 67; }` | `compiler.err.prob.found.req (incompatible conditional branches)` |
| `OP06_BooleanShift.java` | `class OP06_BooleanShift { int n = true << 1; }` | `compiler.err.operator.cant.be.applied.1` |
| `OP07_BitwiseMixed.java` | `class OP07_BitwiseMixed { boolean n = true & 1; }` | `compiler.err.operator.cant.be.applied.1` |
| `OP08_NumberBoolean.java` | `class OP08_NumberBoolean { int n = 67 + true; }` | `compiler.err.operator.cant.be.applied.1` |
| `OP09_NonconstantCase.java` | `class OP09_NonconstantCase { int f(int n) { int key=67; return switch(n) { case key -> 1; default -> 0; }; } }` | `compiler.err.const.expr.req` |
| `OP10_UnassignedCompound.java` | `class OP10_UnassignedCompound { void f() { int n; n += 1; } }` | `compiler.err.var.might.not.have.been.initialized` |
| `OP11_IntToString.java` | `class OP11_IntToString { String s = 1 + 2; }` | `compiler.err.prob.found.req (int not String)` |
| `OP12_Incomparable.java` | `class OP12_Incomparable { boolean test(String s) { return s == 67; } }` | `compiler.err.operator.cant.be.applied.1` |
| `OP13_NonLvalueIncrement.java` | `class OP13_NonLvalueIncrement { int f(int a, int b) { return ++(a + b); } }` | `compiler.err.unexpected.type (not a variable)` |
| `OP14_BadCondition.java` | `class OP14_BadCondition { void f() { if (67) {} } }` | `compiler.err.prob.found.req (int not boolean)` |
| `OP15_ByteConstOutOfRange.java` | `class OP15_ByteConstOutOfRange { byte b = 127 + 1; }` | `compiler.err.prob.found.req (int → byte loss)` |
| `OP16_LongShiftToInt.java` | `class OP16_LongShiftToInt { int n = 1L << 67; }` | `compiler.err.prob.found.req (long → int loss)` |

### 7.1 Отдельный полный двухфайловый негатив — package-private accessibility (SY-N15)

В отличие от single-source negatives, `SY-N15` — корректная **двухфайловая конфигурация**, где ошибка именно в `public`-доступности типа другого пакета, а не из-за отсутствия зависимости:

~~~text
negative-package/
  p/Hidden.java
  q/UseHidden.java
~~~

`p/Hidden.java`:
~~~java
package p;
class Hidden { public static int value=67; }
~~~

`q/UseHidden.java`:
~~~java
package q;
import p.Hidden;
public class UseHidden { Hidden ref; }
~~~

**Реально выполненная команда:** `javac --release 21 -XDrawDiagnostics -d out p/Hidden.java q/UseHidden.java` (из каталога `negative-package` с заранее созданной `out`), **exit 1**, ключ `compiler.err.not.def.public.cant.access: p.Hidden, p` дважды. Оба пакета/файла существуют; ошибка не про classpath. Корректная противоположность — `public p.Helper` из `6.3`.

**Итого 33/33 ожидаемых compile-negative:** 16 SY single-source + 16 OP single-source + 1 multi-file SY-N15. Проверена категория первой релевантной диагностики, а не только ненулевой status. `-XDrawDiagnostics` и формат ошибок — реализация `javac 21.0.12.1`, JLS не обязывает другие компиляторы выдавать те же слова/позиции.

**Два намеренно НЕ засчитанных негативных утверждения:**
- `int n = 1 / 0;` **успешно компилируется** при `javac --release 21` (самостоятельная полная единица `class CheckDivisionZeroCompile { int n=1/0; }`, exit 0), а при выполнении соответствующего выражения происходит runtime `ArithmeticException`. Этот контраст — часть OP-20, не «недопустимая константа».
- Изначально взятый `import static` реальных **static** членов `Thread.MIN_PRIORITY` и `String.CASE_INSENSITIVE_ORDER` оказался **допустимым Java21** (exit 0). Он заменён `SY-N17` с заведомо **instance field** `java.awt.Point.x`, и получен содержательный `cant.resolve.location`. Рецензент не должен превращать допустимый импорт в ошибку для красивого счётчика.

## 8. Педагогический чек-лист ролей и границы проверки

**Для 02 — Knowledge Author:** два самостоятельных русскоязычных связных объяснения, от concrete compilation unit → scopes/name resolution → static imports и от parse tree → типы → side-effect trace → result. Показывать сначала корректную программу, затем *законченный* контрпример и JLS rule. Для каждой P0 `SY-01..SY-16` / `OP-01..OP-20` дать текстовый механизм. Не переписывать полноценные ``CORE-TYPES/CONVERSIONS/PARAMS/CONTROL`, а дать локальную минимальную связь и настоящую новую глубину. Не обещать, что `import` что-то загружает при runtime или что Java имеет неопределённый порядок вычисления как C/C++. Не писать `short+=` = проверенная операция без переполнения, это implicit narrowing и overflow.

**Для 03 — Examples & Exercises Engineer:** если принято общее runnable — два независимых main `SyntaxProbe` (14 строк) и `OperatorsProbe` (27 строк) плюс multi-file fixture `PackageProbe` (3 строки); expected stdout хранить отдельно у G2, `verify.sh` только при учебной необходимости. Negative tests либо отдельный проверяемый launcher, либо завершённые фрагменты с конкретными кодами в статье, не смешивать с успешным javac. **Не** считать stdout lines единицами JUnit. Для compilation errors не полагаться только на exit!=0 без проверки `compiler.err.*` причины. Не создавать избыточный Gradle project.

**Для 05 — Knowledge Editor:** естественный связный русский текст, не лекция из терминов; графическая схема scope/name (если поможет), читаемые контрпримеры и их комментарии, проверка актуальных относительных локальных ссылок и отсутствия fake links к `syntax.md`/`operators.md` до G2, стилистическая проверка «следов генерации».

**Для 04 — Independent Reviewer:** независимо сверить JLS `7.3–`7.6`, `6.3/`6.4`, ``15.7/15.25/15.26`, левосторонний saved value compound assignment, signed remainder, `String +`, signed-zero, all 36 P0; потребовать explicit PASS/CHANGES на exact PR HEAD. Проверить 32 isolated plus one multi-file negative, positive Stdout evidence 14+27+3 и отметку positive-but-not-negative для `1/0` и `import static`. Изменение SHA → новый review.

**Границы `not run`:** код внутри исследовательского Markdown **не компилируется GitHub Actions CI проекта автоматически** (локальные `java/javac` выполнены *вне* репо, не заявлены project CI); Gradle/JUnit/Mockito integration, GUI Obsidian/IntelliJ/anchor resolution, все URL/HTTP checks для embedded external links, иной вендор/JDK/OS, JPMS/multi-module linking, class loader runtime, compiler alternative, annotation processor, JIT/GC/JMH/performance, concurrency/JMM, Java21 preview unnamed classes/instance main — не выполнены и не засчитаны как Verified. Нормативные страницы JLS pin to Java21, даже если новейшие JDK изменили preview/feature state.

**Static link audit G1:** полное дерево по исходному `main`, 282 записи `truncated=false`, все относительные ссылки текущего файла разрешены в `docs/plans/` и checked Git tree; будущее `knowledge/01-java-core/syntax.md` и `operators.md` названы без Markdown-link. Проверка ограничена существованием файлов, **не** GUI Obsidian и **не** фрагментами `#heading`.

## 9. Gate G1 и передача эстафеты

| Что | Честный статус |
|---|---|
| Исследовательская матрица `36 P0/13 P1/7 P2` | `G1 PROPOSED / NOT ACCEPTED` |
| Two autonomous positives + package fixture, 44 stdout | local OpenJDK21 verified; project CI *not run* |
| 32 isolated negatives + one 2-file package access negative | 33/33 verified local with topical diagnostics, project CI *not run* |
| Разделение новых curricular ID от уже опубликованных | audited, but full educational completeness to be independently reviewed |
| `knowledge/01-java-core/syntax.md` and `operators.md` | planned, currently **absent** |
| Java/Gradle/CI/COVERAGE/CURRICULUM changes | **none** in G1 |
| Preview/JPMS runtime/JMM/other vendors/performance | not run / out of scope |

### Передача эстафеты

Кому: **04 — Independent Reviewer**.
Что сделать: провести независимое нормативное **G1-review** данного исследовательского плана в **PR на exact HEAD**: проверить 36 P0, источники A/C, Java21 pure syntax, separate compilation structure, shadowing vs collision, operator order/compound saved LHS, 44 positive stdout and 33 compiler-negative diagnostics, explicitly safe limits/`not run`. Выдать **PASS или CHANGES** с конкретными blocking locations, подлежащими проверке примерами; при исправлении HEAD — повторное review. После PASS передать **00 — Project Coordinator** для решения `G1 ACCEPTED/BLOCKED` и merge; 01 не начинает G2 и не правит статусные реестры.
Передать: оператор пересылает **прямую ссылку на отдельный опубликованный адресный GitHub PR-комментарий** (Issue/PR number, full HEAD SHA, шаг) роли 04, не общий URL PR.
