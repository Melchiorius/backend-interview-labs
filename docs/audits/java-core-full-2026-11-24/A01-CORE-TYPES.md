# A01 — CORE-TYPES: полный независимый предметный аудит

**Issue #252, роль 04, 2026-11-24.** Frozen source BASE f04f1eb5ee3a400a988757414bcae09113bd885b; входящий [checkpoint A00](https://github.com/Melchiorius/backend-interview-labs/issues/252#issuecomment-7131388242), входящий audit HEAD fa9dc1a7283ffa240129cf61a4459f1d5d93751d; draft PR #253. **A01 COMPLETE** как процедура проверки, **CHANGES recommended** (0 BLOCKER, 7 P1, 2 P2; два P0 depth gaps). Это не G4 новой статьи и не итоговый вердикт Java Core. Решение об исправлениях принадлежит 00 после A25.

## 1. Scope, provenance, независимость

| Источник | Exact blob | Полное чтение |
|---|---|---|
| [types.md](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/knowledge/01-java-core/types.md) | 37f3f3cf3fd8d1cb03732b28e14176e0539f0d00 | 1–251, без пропусков |
| [CORE-TYPES.md](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/docs/plans/CORE-TYPES.md) | 6ee7a6ac45a98e8960c2e0abae937c35b7381a79 | 1–26, 12/12 строк матрицы |
| [TypesAndVariablesDemo.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/TypesAndVariablesDemo.java) | 8b608d2725bae8aedf255eeaf4d6beb99ec3d11f | 1–56 |
| [ValuesAndReferencesDemo.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/ValuesAndReferencesDemo.java) | 66502911896d6801fccbdda7ab0b5f49e93316e7 | 1–41, общий пример A02/A03 |

History path через GitHub commits: первоначальный c8c8142f19f0a3ee64f0543c0fc86b451689963e; перестройка статьи/примера/плана 6207d9b738419860a123ad8cd800a99bad06815d; cache/pool ce472839c62f12f843470d8e8b2525d7e5c61bf6; последующие связи/редактура 60af1b048c47404a8da7ee94ec92c12b1585690e, 4680b057aeabcd3e4d36e15b13b4db0d032c944c, 7f4b83b1dfc6f3871389faa2b70f663b3bbb71b5, dac4ff90888301a9821b340475a84f17ab5ba540, 69d61c427e7efcab89a55652830050ef16041c8e, b096790bdde1af99b91ee7bbac84721f69993d3f, 3340404a24f4c1175414a873e567ea93a6049c7f. Не обнаружено указания на существенное авторство 04; общий Git login и squash history не дают исчерпывающей атрибуции. 04 не менял статью; временные probes не учебный материал. Старый #210 PASS не засчитан. A00 old-source delta не содержит types.md.

## 2. Сплошной учёт утверждений

Непрерывные диапазоны 1–251. Существенные inline expressions сгруппированы по смыслу; все code fences отдельно в §4. OK — адресный механизм; DEPTH — нужен более точный ответ; BOUND — осмысленная граница; LINK — проверен переход, не вся соседняя статья. Нормативные источники §6, findings §7.

| Строки | Утверждения / inline examples | Оценка и evidence |
|---|---|---|
| 1–12 | Java21, reference passed to method, static typing, variable type/value, две главные категории | JLS §4/4.1/4.12/15.12.4.5: тип/значение, копируется значение аргумента. Локальная без DA существует, чтение запрещено; special null type не назван, N05. |
| 13–27 | count=67, StringBuilder first/second, append B, reassign C, ссылка не объект; практика | P01 AB/AB, exact runner. JLS §4.3.1/15.26.1. Ссылку не отождествлять с физическим адресом; OK. |
| 28–44 | byte/short/int/long signed bounds, char unsigned UTF-16, float/double 32/64, boolean true/false; no unsigned int/long; layout≠width | JLS §4.2.1–5. Целые диапазоны верны; decimal digits 6–9/15–17 без определения точности N01, boolean conversion N03. Size boolean storage не задан; физическая память не выводится из width. |
| 45–58 | MAX_VALUE+1, widening before +, wrap no exception, decimal approximation, NaN/∞/-0; Math.addExact/BigDecimal | JLS §4.2.2–4/5.6/15.18.2/15.21.1; P02/P13. numbers L11,58,70–87 подтверждает overflow/comparison, но не разницу 24/53 и roundtrip. N01. Не переносить на деление на ноль, unboxing и exact methods. |
| 59–68 | Character.toChars(0x1F600), String.length=2/codePointCount=1; grapheme may contain multiple code points | API Character/String, P03, strings L109. char может быть surrogate, не Unicode scalar; N08. IO-BYTES deferred, I18N уже связан через strings L449. |
| 69–87 | int/long literal, suffix L/F, 0b0100_0011, 0x43, 0103, 3_000_000_000L, decimal F/double, char/String/boolean; underscores | JLS §3.10.1–5; P04. Все значения совпадают. 0_103 допустим, 0x_43 и 67_ нет (P14,N26,N27); nondecimal full-width sign/long-min N07. |
| 88–103 | 2147483648 illegal even with long target; double→float implicit invalid; 09; min int direct unary minus | JLS §3.10.1–2/5.2/15.15.4; N01–03,N18,P05. 09 actual javac expected semicolon, не выдумывать буквальную диагностику invalid octal; -(2147483648) invalid. |
| 104–117 | class/interface/array/generic ref types, CharSequence declared vs runtime StringBuilder, override dispatch; int[] object alias | JLS §4.3–5/4.12.6/10/15.12.4.4; P06. Не выводить runtime overload/field lookup; correct links. Type variables omitted, N05. |
| 118–128 | null, missing==null, instance method NPE, int impossible=null, static via null | JLS §4.1/4.3.1/5.2/15.12.4.1/4.4/15.21.3; P07/P17,N04. При abrupt argument evaluation возможно другое исключение раньше NPE, конкретный пример корректен. classes L304–306 confirms static case. L127 awkward grammar N09. |
| 129–144 | primitive ==, distinct new String, String.equals, boxing cache/pool/intern | JLS §15.21/5.1.7–8/3.10.5; P08/P16/P17,N17. Both nulls true, incompatible ref types compile error; N04. conversions L151–169 forbids guarantee for 128, but L226 misleading premise. |
| 145–156 | instance/static/array default, local DA, parameters, catch/resources/pattern categories | JLS §4.12.3/5/16/12.3.2/12.4.2. Valid. Constant variables special init step in initialization L348, N06. |
| 157–172 | Counter total/value/parameter shadowing/blank local next; arrays zero/null | P09 wrapper: this.value=value, next assignment not read. N05 unassigned local. Fields/arrays default not local; N03 depth. |
| 173–182 | scope of name vs lifetime of object, block inside negative | JLS §6.3/4.12.3; P10/N06/P15. Name inaccessible but saved reference remains; no GC timing guarantee. N03 branch DA and shadowing. |
| 183–197 | var local static inference, count/builder, string assignment/null/no initializer invalid, fields/formals forbidden, lambda parameters | JLS §14.4/14.4.1/14.14.2/14.20.3/15.27.1; P11/P16,N07–09,N13–15,N19–22,N25. syntax L124 includes compound declaration; functions L62–69 target type, but promised lambda parameter declaration restrictions not in retrieved var passages. N02 depth. |
| 198–214 | final limit, mutable builder, blank final computed, class/method final vs variable | JLS §4.12.4/16/8.1.1.2/8.4.3.3; P12,N10–12,N24. Same value reassignment forbidden; L213 wording N06; constant/effectively final distinct. |
| 215–229 | 8 self-check questions, scope | Q1–7 refer to mechanisms above; Q8 premise 128 N04. Table points reader back instead of providing answers; acceptable but N09 worked follow-ups. |
| 230–239 | source-file command and claimed checks | exact files CI/local §5; 9 check calls + composite assertion, not JUnit. |
| 240–243 | SYNTAX/OPERATORS links | syntax L95/L110–124 confirms DA/var; operators target exists, full review A18. |
| 244–251 | 6 JLS links | Chapter pages directly opened; add applicable §5/6/15/API locators N09, not full external crawl. |

## 3. Исходная матрица G1 — 12/12

План 1–26: scope L3–5, 12 rows L9–20, negatives L22, historical gate L24, future research L26. 9 P0 / 2 P1 / 1 P2. Все P0 имеют адресный материал, два нуждаются в углублении, отдельные уточнения отмечены. Historical statuses не переписывать.

| ID | P | Plan L | Article L | Verdict | Обоснование |
|---|---|---:|---|---|---|
| TYPES-01 | P0 | 9 | 5–26,129–141 | OK | primitive/reference copy, alias mutation/reassign/identity, P01+runner |
| TYPES-02 | P0 | 10 | 28–43,59–67 | OK | 8 types, signed/unsigned, UTF16, code point; N03/N08 extensions |
| TYPES-03 | P0 | 11 | 37–38,45–58 | DEPTH | overflow/widening verified, floating precision incomplete N01 |
| TYPES-04 | P0 | 12 | 69–102 | OK | promised literals/negative compilation verified, N07 extension |
| TYPES-05 | P0 | 13 | 145–172 | OK | 5 categories and additional categories; runner; N06 caveat |
| TYPES-06 | P0 | 14 | 149–172 | OK | defaults vs DA, P09,N05; N03 branching extension |
| TYPES-07 | P0 | 15 | 173–181 | OK | scope vs lifetime P10/N06/P15, JVM boundary |
| TYPES-08 | P0 | 16 | 183–197 | DEPTH | static inference, field/formal/null/init; N02 practical restrictions |
| TYPES-09 | P0 | 17 | 198–214 | OK | final mutable object, N06 wording/constant variable |
| TYPES-10 | P1 | 18 | 104–128 | OK | null, array, declared/runtime; N05 taxonomy |
| TYPES-11 | P1 | 19 | 129–143,226 | DEPTH | equality and conversions links; N04 comparability/cache premise |
| TYPES-12 | P2 | 20 | 43,173,228 | BOUND | layout/GC/JIT explicit deferred |

Итого 8 OK, 3 DEPTH, 1 BOUND. P0 7 OK + 2 DEPTH. Все исходные listed negatives L22 проверены сейчас. No ERROR/MISSING row; не означает отсутствие findings. A02–A24 not assessed.

## 4. Все code fences, inline cases и local probes

12 Java fences и 1 PowerShell fence, 13 всего. Никакой Java block не является полным compilation unit; отдельные wrappers main, L157–171 Counter + statements. Pnn wrapper class Pnn { public static void main(String[] args) throws Exception { ... } }, каждый отдельно, добавленные println обозначены; P09 Counter static nested class wrapper, исходные поля/update сохранены; P07 NPE пойман для фиксации класса. Временные P13–17 — независимые диагностические probes. Все 17 compile/run реально выполнены на OpenJDK/javac 21.0.12.1, javac --release 21 -Xlint:all Pnn.java, java Pnn, оба exit 0; P17 warning-only [static]. Ни один не committed или CI test. В таблице semicolon разделяет stdout lines, не literal output.

| Fence | Context/purpose | Actual local evidence |
|---|---|---|
| L13–22 | main alias | P01 AB; AB |
| L47–53 | main overflow | P02 -2147483648; 2147483648 |
| L61–65 | main UTF16 | P03 2; 1 |
| L73–84 | literal declarations + added println | P04 67:67:67:67:3000000000:1.5:1.5:A:A:true |
| L90–98 | positive declarations + added min L100/println; negatives separately | P05 2147483648:1.5:-2147483648; N01–03,N18 |
| L108–112 | declared/runtime/array/null + println | P06 StringBuilder:67:null |
| L120–125 | null, intentional runtime exception | P07 true; NullPointerException caught; N04 |
| L131–139 | identity | P08 true; false; true |
| L157–171 | nested Counter wrapper + creation/update/println | P09 0:67:0:0:null; N05 |
| L175–181 | block scope | P10 67; N06 |
| L187–194 | var + println | P11 67:AB; N07–09 |
| L200–211 | final + println | P12 67:AB:67; N10–12,N24 |
| L234–236 | PowerShell source-file launch | exact source bytes run in temp directory §5; not clean clone/PowerShell GUI |

Inline examples L36/41/43/55/57/67/71/86/100/106/114/116/127/141/143/149–156/173/185/196/213/219–226/238/242 addressed in §2/probes; not every possible variation executed. No Mermaid/pseudocode. Additional diagnostic bodies:

~~~java
// P13
float f = 16777216f; double d = 9007199254740992d;
System.out.println((f+1==f)+":"+(d+1==d)+":"+(0.1+0.2==0.3));
System.out.println(Math.nextUp(f)-f);
System.out.println(Double.NaN==Double.NaN);
System.out.println(-0.0==0.0);
// P14
int x = 0xffff_ffff; long y = 0xffff_ffff;
long z = 0xffff_ffffL; int oct = 0_103;
System.out.println(x+":"+y+":"+z+":"+oct);
// P15
Object obj; { var b = new StringBuilder("A"); obj=b; }
System.out.println(obj);
boolean cond=true; int n; if(cond) n=67; else n=68;
System.out.println(n);
// P16
var arr=new int[]{67}; for(var n:arr) System.out.println(n);
try(var r=new java.io.StringReader("A")) { System.out.println((char)r.read()); }
java.util.function.IntUnaryOperator f=(var x)->x+1;
System.out.println(f.applyAsInt(67));
Integer a=127,b=127; System.out.println(a==b); System.out.println(a==127);
// P17 (intentional static-via-null anti-example)
String a=null; String b=null; System.out.println(a==b);
System.out.println(String.valueOf(67)); System.out.println(a.valueOf(67));
~~~

P13 actual true:true:false; 2.0; false; true. P14 -1:-1:4294967295:67. P15 A; 67. P16 67; A; 68; true; true. P17 true; 67; 67, warning [static] static method should be qualified by type name, String, instead of by an expression. P13 shows spacing at integer boundaries, not universal decimal accuracy; P14 sign extension vs long literal.

### 27 isolated negative compilations

Each javac --release 21 -XDrawDiagnostics Nnn.java exit 1, actual diagnostics below. Wrapper class Nnn { void test() { statements } }; N21/N22 class-level separately. No treating warning as failure.

| Case | Statement | Actual compiler.err category/details |
|---|---|---|
| N01 | long wrong = 2147483648; | int.number.too.large |
| N02 | float wrongFraction = 1.5; | prob.found.req (possible.loss.of.precision double,float) |
| N03 | int wrongOctal = 09; | expected ';' |
| N04 | int impossible = null; | prob.found.req (inconvertible.types null,int) |
| N05 | int local; System.out.println(local); | var.might.not.have.been.initialized |
| N06 | {int inside=67;} System.out.println(inside); | cant.resolve.location |
| N07 | var count=67; count="text"; | prob.found.req (inconvertible.types String,int) |
| N08 | var unknown=null; | cant.infer.local.var.type (local.cant.infer.null) |
| N09 | var absent; | cant.infer.local.var.type (local.missing.init) |
| N10 | final int limit=67; limit=68; | cant.assign.val.to.var |
| N11 | final StringBuilder message=new StringBuilder("A"); message=new StringBuilder("C"); | cant.assign.val.to.var |
| N12 | final int computed; computed=67; computed=68; | var.might.already.be.assigned |
| N13 | var a=1,b=2; | restricted.type.not.allowed.compound |
| N14 | var a={67}; | cant.infer.local.var.type (local.array.missing.target) |
| N15 | var f=x->x; | cant.infer.local.var.type (local.lambda.missing.target) |
| N16 | int n; boolean c=true; if(c)n=67; System.out.println(n); | var.might.not.have.been.initialized |
| N17 | String a=null; Integer b=null; System.out.println(a==b); | incomparable.types |
| N18 | int n=-(2147483648); | int.number.too.large |
| N19 | var a=(a=67); | cant.infer.local.var.type (local.self.ref) |
| N20 | var a[]=new int[1]; | restricted.type.not.allowed.array |
| N21 | class N21 { var field=67; } | restricted.type.not.allowed.here |
| N22 | class N22 { void f(var p){} } | restricted.type.not.allowed.here |
| N23 | boolean b = (boolean) 1; | prob.found.req (inconvertible.types int,boolean) |
| N24 | final int n=67; n=67; | cant.assign.val.to.var |
| N25 | var f=String::length; | cant.infer.local.var.type (local.mref.missing.target) |
| N26 | int n=0x_43; | illegal.underscore |
| N27 | int n=67_; | illegal.underscore |

No lambda mixed var/explicit negative executed; normative JLS evidence. No GC/layout/benchmark. All 12 positive Java fragments checked with context, not claimed verbatim compilable standalone.

## 5. Exact standalone sources and CI

Manually restored two complete GitHub sources in /tmp/jc-a01. git hash-object TypesAndVariablesDemo.java ValuesAndReferencesDemo.java exactly matches frozen blobs 8b608d2725bae8aedf255eeaf4d6beb99ec3d11f / 66502911896d6801fccbdda7ab0b5f49e93316e7. java TypesAndVariablesDemo.java and java ValuesAndReferencesDemo.java each exit 0; javac --release 21 -Xlint:all *.java (at this time only these two files) exit 0, no diagnostics. Local Java 21.0.12.1 Debian. Actual stdout:

~~~text
primitive: 67, reference: AB
overflow: -2147483648, widened: 2147483648
UTF-16 length: 2, code points: 1
CHECK PASSED
~~~
~~~text
Inside changeNumber: 100
Outside changeNumber: 67
After append: AB
Inside replace: C
After replace: AB
After reassigning second: AB
CHECK PASSED
~~~

Types L1–12 paths/check helper AssertionError independent of -ea; L13–23 copy/alias/reassignment; L25–30 default fields and first array elements; L32–34 inferred builder behavior not reflection of static type; L36–39 overflow/widening; L41–44 UTF16; L46–48 final mutation; L50–56 stdout. **9 check calls** (primitive, alias, reassign, fields, arrays, var, overflow, UTF16, final). Values L1–4 paths, L6–18 methods, L20–34 trace, L36–38 one composite assertion number and first content; not every intermediate state asserted. No fixture/diff for early standalone examples. 11 stdout lines including two CHECK PASSED, not JUnit. Check calls in Types, at L16,21,23,28,30,34,38,43,48. Both programs use explicit throwing checks.

[Published run #45390717871](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45390717871) exact frozen head push success; [job 138433360807](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45390717871/job/138433360807). Raw log step 12:09:25.2533496Z, workflow L30–36, Temurin 21.0.13-9. Values stdout 12:09:25.6470411–6477290Z, Types 12:09:26.0643640–0696935Z match above. No CI of new temporary probes. A02/A03 may reuse Values evidence with topic relevance, not count duplicate execution.

## 6. A/I/B/C and new interview sample

A: directly opened JLS SE21 [§3.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10), [§4.1–4.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.1), [§4.12](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12), [§5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html), [§6.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.3), [§14.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.4), [§15.21](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.21), [§16](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html), [Float](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Float.html), [Double](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Double.html), [String](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html). Other references (e.g. §8/12, Character.toChars) are normative pointers/neighbor+probe evidence, not claim all pages/anchors re-opened. Specified wrap, literals, var, DA don't require HotSpot source. I: local JVM/runtime observations, not pinned private layout algorithm. No B benchmark/methodology claim requiring review here. JVM layout remains deferred.

C fresh retrieval 2026-11-24, default branch SHA checked before pinned README; real questions paraphrased, C not normative/frequency.

| Source | Actual question locators | Assessment |
|---|---|---|
| [teamlead exact README](https://github.com/teamlead/java-interview-questions/blob/4c0784024412904625e1455189d3c05cba07bebf/README.md) L51/78/522/936/993/1021/1297 | primitive/ref, list/ranges, array object, false stack/heap dichotomy | Main covered; layout caveat important, JVM legitimately deferred |
| [Hruday exact README](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/ced3cf4f91084e9b45366f7a81a70526e7a4be7d/README.md) L79–100,118–121,319–335 | local/instance/static, shadow/hide, literals/casts, boolean/int, overflow/Unicode, comparison, blank final, mutable final ref | N03 boolean/DA/shadow, N01 precision, N06 final. A02 casts/A06 hiding, don't duplicate |
| [Baeldung Java interview questions](https://www.baeldung.com/java-interview-questions) Q1/Q4/Q6/Q8, updated 2024-05-11 | pass-by-value, modifiers, heap/stack, void/Void | Value/final model correct; don't copy universal stack/heap size/speed and generational claims; optional void/Void N08 |
| [Chernyshov English exact index](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/597e08112ab6a57d6eac8e3c70c070da606dae73/eng/README.md) L55,252–253,540–542 | primitive generic type args, final vs immutable, memory | builder counterexample covered; generics A11, JVM deferred |

No direct C evidence for every var declaration restriction, roundtrip digit distinction or same-value final assignment; normative A/probes justify follow-up, not invented frequency. C selection finite, not exhaustive.

## 7. Findings register

All affect CORE-TYPES; other ID links for subsequent audit. No BLOCKER found. P1/P2 recommendations and two depth gaps, not automatic remediation or new curriculum.

| ID / severity | Exact location / issue | Evidence, impact, acceptance criterion and role |
|---|---|---|
| JC-A01-N01 P1, TYPES-03 P0 depth | L37–38,57; 6–9/15–17 significant decimal digits without criterion | JLS §4.2.3 binary32 24-bit / binary64 53-bit significand, roughly 7/16 digits. Distinguish decimal→binary→decimal guarantee (6/15 digits under appropriate exponent/rounding conditions) from sufficient binary→decimal→binary roundtrip digits (9/17); neither universal accuracy, subnormal/overflow caveats. P13 shows 2^24+1 and 2^53+1 rounded to preceding even value, NaN/signed-zero. numbers L70–87 covers comparisons but not this distinction in checked passages. 02 add clear definition + example, link NUMBERS, no fixed epsilon advice. |
| JC-A01-N02 P1, TYPES-08 P0 depth | L185–196 incomplete practical var constraints and inaccurate promise of lambda parameter restrictions in functions | JLS §14.4/14.4.1/14.14.2/14.20.3/15.27.1; N13–15,19–22,25,P16. No multi-declarator, bare array initializer, extra brackets, self-ref, standalone lambda/mref target. Contrast var xs=new int[]{67}, enhanced-for/TWR; all lambda parameters use consistent form, no mix var/explicit/implicit. syntax L124 has compound case; functions L62–69 target typing but checked var passages not parameter syntax. Add brief examples/precise links, 02/05; A10/A18. |
| JC-A01-N03 P1 | L28–41,145–173, boolean/int and DA/shadowing follow-up | C Hruday L87–98, JLS §4.2.5/5/6.3–4/16. N23 forbidden cast, use n!=0. N16 non-final boolean c=true doesn't satisfy DA by itself; P15 both branches assign. Name scope vs DA vs field shadowing at L163; hiding separate A06. Add short contrasts or links SYNTAX/CONVERSIONS/INHERITANCE; 02. |
| JC-A01-N04 P1 TYPES-11 | L141–143,226 question presupposes different result for 128; reference comparison limits | JLS §15.21.1–3/5.1.7–8; N17 incompatible String/Integer even if both null; P17 same type two nulls true, P16 127 constant identity true. Mixed wrapper/int unboxes. conversions L151–169 already correct: no guaranteed result at 128. Rephrase question “что гарантируется для 127 и что нельзя обещать для 128?”, add comparability example/NaN and signed zero link; 02; A02/A08/A18. |
| JC-A01-N05 P1 | L11/106/118 compressed type taxonomy | JLS §4.1/4.3/4.4: primitive/reference kinds plus special null type with no name; null literal not object/class; ref types include type variables, parameterized class/interface not a peer kind. One precise sentence/link to generics; no physical reference address claim. 02; A11. |
| JC-A01-N06 P1 | L150,198–214 final “другое значение” suggests same value reassignment allowed; constant distinction | N24 even same value fails; §4.12.4/16 blank final definite unassignment. Constant variable = final primitive/String initialized with constant expression, not every final; initialization L205,346–350 already covers init/constant step. Say cannot reassign even same value; link INIT/CONVERSIONS, effectively-final lambda as distinct concept. 02; A07/A10. |
| JC-A01-N07 P1 | L69–102 missing nondecimal full-width sign and long-min analogue | JLS §3.10.1, P14, N18/N26/N27. 0xffff_ffff int -1, long assignment sign-extends; suffix L gives +4294967295, long min direct unary minus analogous. 0_103 valid; 0x_43 and trailing underscore invalid. Add short worked contrast, 02; A02/A18. |
| JC-A01-N08 P2 | L36,59–67,104–106 optional void/Void and surrogate boundary | JLS §4.2.1/8.4.5, String/Character API. char may contain unpaired surrogate, codePointCount counts unpaired surrogate as one, not Unicode validation or grapheme count; strings deeper. void not ninth primitive, Void placeholder not numeric wrapper. Link existing STRINGS/CLASSES/GENERICS if appropriate, no new Unicode/JVM course. 02/01 optional. |
| JC-A01-N09 P2 | L127 grammar; L215–228 self-check table; L244–251 source locators; Values composite final check | 05 correct awkward L127; 02 concise worked follow-ups DA/precision/cache and exact §5/6/15 links. Existing self-check table usable, not require LMS. Optional 03 intermediate assertions if valuable; not necessary to turn all snippets into JUnit. No author AI inference. |

N01/N02 are depth gaps in two P0 rows; N04 is P1 depth. No separate unresolved mandatory claim requiring another reviewer before finishing A01; 00 assesses severity cumulatively. Don't pre-certify adjacent topics.

## 8. Navigation, pedagogy, limitations, cursor

26 relative file-target occurrences in article, 3 plan, 53 common example README = **82/82** resolve against frozen tree 499 entries/truncated=false. 4 Theory comment paths (1+3) exist. Repeated links counted. README L7–12/L55–64 links/commands match sources. Neighbor passages directly read: conversions L137–169/229–233, syntax L95/110–124, functions L62–69/388, numbers L11/58/70–87, strings L30–49/109/449, initialization L205/346–350, classes L304–306. Targeted only, not full audit of neighbors. A25 backlinks/contradictions.

Pedagogy: motivated opening, concrete values/alias example, table and negatives, reasonable scope; no compulsory diagram/exercise needed. Depth limited on numeric precision and var; add mechanisms and follow-ups, not term list. L127 awkward, L226 question misleading, otherwise natural Russian; no evidence warrants broad rewrite or attributing text to AI. Reader can learn largely independently; practice relevant. 

**NOT RUN:** clean clone, Gradle/JUnit locally, actual Obsidian/IntelliJ/PowerShell GUI, full HTTP/anchors, other vendors/OS beyond local Debian and published Temurin, physical JVM memory/GC/JIT, benchmarks/stress, full Unicode algorithms, all possible inline variations. 12 Java fences with contextual checks, 17 positive wrapper probes, 27 separate negative javac, two exact standalone sources actually run. Local snippets not CI; no code changed. Source A pointers not exhaustive external crawl; C not exhaustive.

**Checkpoint A01 COMPLETE**, 251/251 article lines, 26/26 plan lines, 12/12 rows; cumulative 1/41 articles, 1/42 curricular ID, 12/925 initial requirements reviewed; 0 BLOCKER, 7 P1, 2 P2. Overall audit IN PROGRESS. Next **A02 CORE-CONVERSIONS**, frozen conversions.md blob a004e1be87a926194033665d2c74d5d859eb7690 L1–270; plan CORE-CONVERSIONS.md 1–23, rows 9–19 (11, incl mixed P0/P1); ConversionsDemo.java and common Values evidence. No PASS Java Core.

### Передача эстафеты
Кому: **04 — Independent Reviewer (продолжение аудита)**.
Что сделать: начать A02 с exact frozen cursor, прочитать index и A01, сохранить findings, проверить весь A02, матрицу/примеры, A/I/C, локальные probes, навигацию и педагогический результат; записать отчёт, обновить README и опубликовать следующий checkpoint. PR #253 остаётся Draft, учебные материалы не менять.
Передать: оператор пересылает прямую ссылку на опубликованный комментарий #252.
