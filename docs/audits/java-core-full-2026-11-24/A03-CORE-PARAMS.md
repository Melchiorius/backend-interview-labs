# A03 — CORE-PARAMS: независимый предметный аудит

Issue #252, роль 04, 2026-11-24. Frozen source f04f1eb5ee3a400a988757414bcae09113bd885b; [входящий checkpoint A02](https://github.com/Melchiorius/backend-interview-labs/issues/252#issuecomment-7131718339); audit parent 3630835fee7dd7a1b289c03d1230b9af3d47e13e; Draft PR #253. **A03 COMPLETE / CHANGES recommended: 0 BLOCKER, 5 P1, 2 P2.** Предыдущий B01 из A02 остаётся открытым. Решение об исправлениях после A25 — 00. Ни одного изменения учебного материала.

## 1. Scope и provenance

| Frozen source | Exact blob | Read |
|---|---|---|
| [parameters.md](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/knowledge/01-java-core/parameters.md) | 2f196462d910f9f0f31be1ecadecc7035220eb1a | 1–272 полностью |
| [CORE-PARAMS.md](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/docs/plans/CORE-PARAMS.md) | 6b5be5c3c3313c2b0781b706fea3fd3de3856de0 | 1–24 полностью |
| [ParametersDemo.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/ParametersDemo.java) | aa16e7151e27e5aea2f6ec1f8ad68bf489e6b95d | 1–82 полностью |
| [ValuesAndReferencesDemo.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/ValuesAndReferencesDemo.java) | 66502911896d6801fccbdda7ab0b5f49e93316e7 | 1–41 A01; reuse, not rerun |

History by path at frozen ref: c8c8142f19f0a3ee64f0543c0fc86b451689963e initial, 077336dc754c411d10749e77749fae841faa24e8 revision, 60af1b048c47404a8da7ee94ec92c12b1585690e classes, 86e67b756395373b34cbd9c4c8a7c7435c7d090e inheritance, dac4ff90888301a9821b340475a84f17ab5ba540 nested/functions, bc22138a9590a24f87191b8c637492c3f763e75f generics/erasure, b096790bdde1af99b91ee7bbac84721f69993d3f syntax/operators. No substantial 04 authorship indication in retrieved commit titles; common login/squash prevents exhaustive attribution. Independent review here, not previous PASS. A00 old-source delta did not include parameters.md.

## 2. Continuous claim review (272/272 lines)

A = JLS/API, I = observed local Java21 runtime/compiler. F/P/N below refer to actual probes. Neighbor excerpts checked only, not full audits.

| Lines | Claims / inline examples | Evaluation |
|---|---|---|
| 1–17 | Motivating config mutation/replacement, argument expression vs fresh formal parameter, invocation conversions, pass-by-value, contextual snippets | JLS §4.12, §8.4.1, §5.3, §15.12.4.5. Correct: value of reference, not copied object or caller variable. Actual argument need not be variable (literal/computed expression). P01 alias same object; F01/F02/F03. |
| 18–33 | int change local only; same for other primitives | F01=67; all primitive values copied, not all types separately executed. No claim reference to physical memory. OK. |
| 34–62 | StringBuilder mutation visible through alias, reassignment affects only local parameter | F02=AB/F03=A; shared object, no clone, not pass-by-reference. “два имени одного объекта” shorthand for separate variables referring to same object, not variable aliasing; P01. Sequential observation, concurrency boundary L236. |
| 63–80 | swap local copies; explicit return or shared container | F04=A:B; P02 swaps array slots but not original variables; N06 enrichment. Correct. |
| 81–103 | primitive array is reference type, change element vs replace array | F05=67:67; P07 covariance runtime check; L102 future CORE-ARRAYS stale N07. Null/bounds errors not covered locally but array topic exists. |
| 104–133 | immutable String += local reference; Integer++ unbox/add/rebox, final vs immutable | F06=A/F07=67, P03 null Integer++ NPE; JLS §15.14.2, §15.26.2, §5.1.7–8. Correct effects; L130 awkward “исходный Integer внутри объекта” (Integer itself object), N07. No assumption new physical allocation on every boxing. final variable not object immutability, F13. |
| 134–154 | null copy safe, replacement safe, append on null NPE, null overload ambiguity | F08, N01/N08. §5.3, §15.12.2.5, §15.12.4.1–4. Direct examples correct. L151 general shorthand needs prerequisite argument evaluation normally completes; P09/P10/P11, N01/N03; null unboxing named only self-check L247, P03/P16. L247 ambiguity compile error not cause of NPE. |
| 155–173 | int... parameter int[], expanded call vs existing array; generics and SafeVarargs separate | F09, P04–07/P14, N03–06; §8.4.1/15.12.4.2. Correct examples; missing zero arguments, null array vs single null element, last/only varargs, same signature int[] and int..., primitive int[] passed to Object... as one element, fixed arity. Existing arrays L254–274 and erasure L82–107 address deeper cases; N02 ties to A02-N05, avoid duplication. |
| 174–190 | argument evaluation left to right, i++ values, abrupt completion skips later args and body | F10 and counter=2, P08/P16. §15.7.4/15.12.4.2. Correct; receiver before args, captured receiver even if variable reassigned by arg, null receiver check after argument evaluation, P09–12, N03. L189 future OPERATORS stale. |
| 191–205 | overload based on static type, selected implementation via override | F11 Object:String, P13 derived:Object/derived:String; §15.12.1–2/4.4. L202 “а не исключительно фактического класса” should exclude runtime argument class altogether from ordinary compile-time overload selection (poly/inference distinct). Dynamic dispatch uses runtime receiver not runtime arg type; inheritance L112–114,359ff supports. N04. |
| 206–220 | explicit return, assign in caller, contract for mutation/shared object | F12=68; P02 container example; existing immutability article defensive copy/ownership, N06/N07. Clear and sufficient. |
| 221–231 | final parameter cannot reassign, object mutation allowed; effectively final capture | F13=AB, N02/N07, P15. JLS §8.4.1/4.12.4/15.27.2. functions L117/145/158 actual corroboration, no implied deep immutability. |
| 232–237 | frame separation not physical stack/heap guarantee, shared object not thread-safe | Appropriate bounds. JLS §15.12.4.5 abstract frame, not stack-vs-heap guarantee; no JMM proof/benchmark. Future JVM/CON remains deferred, not require implementation inspection. |
| 238–251 | nine self-check questions | All point to corresponding mechanisms. L247 separate compile ambiguity from NPE, N01. Follow-up answer not always immediately given, but acceptable self-check. |
| 252–265 | practice claims, two standalone sources, operators/syntax links | Exact source run §5; main source covers asserted cases, historical plan L22 overclaims m(null) reproduction, N05. No Gradle module needed. |
| 266–272 | five primary sources | All four chapter pages retrieved (4,8,15,5); exact §§8.4.1,15.7.4,15.12.4.1/2/4/5 read. Extra precise anchors for N01–03 proposed. No all-HTTP/anchor crawl. |

## 3. G1 matrix, 12/12

No original row IDs; audit-local JC-A03-Rnn = original source line. 6 P0 / 5 P1 / 1 P2. Read whole plan including scope L5, counterexamples L22 and historical verification L24; do not rewrite historical plan.

| Audit ID | P | Question | Article lines | Verdict |
|---|---|---|---|---|
| JC-A03-R09 | P0 | argument/formal, value copy | 5–17 | OK, fresh variables/values |
| JC-A03-R10 | P0 | primitive unchanged caller | 18–33 | OK F01 |
| JC-A03-R11 | P0 | shared mutation vs reassign, swap | 34–80 | OK F02–04/P01 |
| JC-A03-R12 | P0 | arrays, element vs ref | 81–103 | OK F05 |
| JC-A03-R13 | P0 | String +=, Integer++ | 104–133 | OK F06/F07, P03 extension |
| JC-A03-R14 | P0 | null, dereference, ambiguity | 134–154,247 | DEPTH N01, N03; no wrong direct example |
| JC-A03-R15 | P1 | varargs expansion vs existing array | 155–173 | DEPTH N02; basic examples correct |
| JC-A03-R16 | P1 | argument evaluation order | 174–190 | DEPTH N03; order/abrupt correct; receiver interaction omitted |
| JC-A03-R17 | P1 | compile-time overload vs runtime dispatch | 191–205 | OK N04 wording, inheritance link verified |
| JC-A03-R18 | P1 | return replaces value | 206–220 | OK F12/P02 |
| JC-A03-R19 | P1 | final/effectively final | 221–231 | OK F13,N02/N07,P15; functions actual section |
| JC-A03-R20 | P2 | JVM/JMM/publication | 232–237 | BOUND justified |

8 OK, 3 DEPTH, 1 BOUND. P0 5 OK / 1 DEPTH. No ERROR/MISSING row. N05 concerns evidence description, not incorrect pass-by-value principle. A02-N05 remains open; do not create a duplicate requirement to fully teach overload twice.

## 4. Local contextual reproduction and independent boundaries

OpenJDK/javac 21.0.12.1 Debian, /tmp/jc-a03. 13 Java fences + 1 PowerShell. One temporary Probe.java with renamed helpers where same name repeats, class-level static methods + main statements; added println/labels and exception catches to observe outcomes. javac --release 21 -Xlint:all Probe.java exit 0, java Probe exit 0. 30 output lines total (14 F lines + 16 P lines), not 30 tests or 30 independently compiled programs. Exact source-file run separately §5.

| Fence | Observed F result |
|---|---|
| 20–28 | F01=67 |
| 36–44 | F02=AB |
| 50–59 | F03=A |
| 65–77 | F04=A:B |
| 85–100 | F05=67:67 |
| 108–116 | F06=A |
| 120–128 | F07=67 |
| 138–149 | F08=replace-ok:NPE (intentional append failure caught) |
| 159–168 | F09=67; expanded call also completed |
| 178–185 | F10=0, 1; F10-counter=2 |
| 193–200 | F11=Object:String |
| 210–217 | F12=68 |
| 223–228 | F13=AB; negative assignment N02 |
| 258–260 shell | exact java ParametersDemo.java §5 (temp cwd, not PowerShell GUI) |

Independent P cases, exact logic and actual outcomes:
- P01: mutateBoth(x,y) appends B to x then C to y; pass same builder A twice => ABC.
- P02: slots={first,second}; swapElements swaps slots[0]/[1] through temp; print slots[0],slots[1],first,second => B:A:A:B.
- P03: increase(null) uses Integer++ => NPE:unbox.
- P04: pack(int... numbers){return numbers;}; pack(arr)==arr, pack(67)!=pack(67), pack().length, pack((int[])null)==null => true:true:0:true. Observable identity, no physical allocation count claim.
- P05: changeFirst((int[])null) reads length => NPE:null-varargs-array.
- P06: objects(Object... values) reports null-array or length:first type; calls (Object[])null, (Object)null, no args, new int[]{67} => null-array:1:null-element:0:empty:1:int[].
- P07: Object[] covariant=new String[]{"A"}; covariant[0]=67 => ArrayStoreException, array stores runtime type.
- P08: mark(trace,fail(trace),next(trace)); fail appends first then throws IllegalStateException; next would append second and mark body would append body => first:IllegalStateException, neither latter executed.
- P09: target(trace) appends receiver and returns null; target(trace).append(arg(trace)); arg appends :arg then returns "X" => receiver:arg:NPE.
- P10: target(trace).append(badArg(trace)); badArg appends :bad then throws IllegalStateException => receiver:bad:IllegalStateException (argument failure precedes null receiver check).
- P11: failTarget(trace) appends receiver and throws IllegalStateException; failTarget(trace).append(arg(trace)) => receiver:IllegalStateException, arg not reached.
- P12: StringBuilder a=A,b=B,receiver=a; receiver.append(receiver=b); output a,b,receiver==b => AB:B:true. Receiver value captured before argument reassigns caller variable.
- P13: Base.describe(Object) returns base:Object; Derived overrides Object→derived:Object and overloads String→derived:String. Base base=new Derived(); base.describe("A"); ((Derived)base).describe("A") => derived:Object:derived:String.
- P14: choose(Object)→Object, choose(Object...)→array or null-array; choose(new Object[0]), choose((Object[])null), choose((Object)null) => array:null-array:Object; fixed arity applicability, not blanket “varargs last”.
- P15: capture(StringBuilder target){return ()->target.append("B");}; capture(builder A).run() => AB, effectively-final reference permits mutation.
- P16: Integer missing=null; mark(trace,missing,next(trace)) with int parameter, unboxing NPE, trace length 0 => 0:NPE; no later argument/body.

Nine separate negative javac --release 21 -XDrawDiagnostics calls, all exit 1. Each Nnn has independent class wrapper; actual categories:
| Case | Declarations/statement | compiler.err |
|---|---|---|
| N01 | m(String),m(Integer), m(null) | ref.ambiguous |
| N02 | test(final StringBuilder x){x=new StringBuilder();} | final.parameter.may.not.be.assigned |
| N03 | m(int... x,int y) | varargs.must.be.last |
| N04 | m(int... x,String... y) | varargs.must.be.last |
| N05 | m(int... x) and m(int[] x) | array.and.varargs |
| N06 | m(int... x), m(1L) | cant.apply.symbol (varargs.argument.mismatch, possible.loss.of.precision) |
| N07 | test(StringBuilder x){Runnable r=()->x.append("B"); x=new StringBuilder();} | cant.ref.non.effectively.final.var |
| N08 | m(int x),m(null) | cant.apply.symbol (inconvertible.types null,int) |
| N09 | m(int x),m(67,68) | cant.apply.symbol (arg.length.mismatch) |

No generic varargs warning test in A03: existing erasure article describes separate lint probes, A11 will audit. No race/GC/JIT/physical layout observation. All fence positives reproduced contextually, not exact standalone compilation of snippets. Relevant inline cases addressed in §2 and P/N, not every syntactic variation run.

## 5. Original technical source and CI

Restored complete ParametersDemo.java in /tmp/jc-a03, git hash-object returned aa16e7151e27e5aea2f6ec1f8ad68bf489e6b95d exact match. javac --release 21 -Xlint:all ParametersDemo.java success no diagnostics; java ParametersDemo.java exit 0. Actual stdout:
~~~text
number=67, text=AB, array[0]=67
varargs original array[0]=67, overload=Object
CHECK PASSED
~~~
L1–32 theory paths/helpers; L34–76 check calls at 37/41/43/47/49/53/56/61/67/71/72/76 (12 explicit check calls). acceptNull(null) L57 and mutateVarargs(1,2) L62 only completion; no independent assertion of expanded array identity. L78–82 print. No original stdout fixture. The original runner does not contain an ambiguous overload pair String/Integer, does not execute null dereference, abrupt earlier argument or final-parameter negative. Its choose(Object/String) pair is *not ambiguous* for null. Historical plan L22 “Всё это воспроизводится” exceeds actual runner coverage for ambiguity; N05. A02 ConversionsDemo has commented ambiguity and disambiguation, not compiler-negative in CI. A03 N01 now independently verifies, not retroactively CI evidence.

[Exact published push CI #45390717871](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45390717871) frozen SHA success, build job 138433360807 raw logs reread; timestamps 12:09:26.8815549Z / .8842464Z / .8851843Z show same 3 lines; Temurin21 published vs local Debian21. A02 audit-only CI #45395404272 now success. Shared Values A01 exact run demonstrates primitive copy and mutate/reassign, not varargs, overload or ordering. Not rerun in A03; no duplicate test count. No local Gradle/JUnit, clean clone or exact CI environment reproduction.

## 6. A/I/B/C evidence

A direct retrieval: [JLS §8.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.1) freshly initialized formal parameters, one last varargs, array type, final assignment prohibition; [§15.7.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.7.4) left-to-right/abrupt; [§15.12.4.1–5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.4) target first, evaluate args, select actual method/null target check, fresh frame/params; §15.12.4.2 array creation conditional on applicability, erased non-reifiable array, matching array passed directly. Chapter 4 and 5 retrieved; §4.12.4/§5.3/§5.1.8/§15.12.2/§15.14.2/§15.26.2/§15.27.2 used as normative locators plus local evidence; not claim each subsection exhaustively reread. I observed javac/runtime, not pinned HotSpot implementation or allocation guarantees. No B performance methodology claim.

C fresh retrieval 2026-11-24, pinned commits previously resolved A01, not refreshed default refs this turn:
- [Baeldung CORE](https://www.baeldung.com/java-interview-questions), updated May 11 2024, beginner Q1 pass-by-value, Q12 NPE, advanced Q1 immutable String/Q2 static vs dynamic binding; Q1 final answer agrees, initial definition conflating object-copy/reference argument is imprecise and not imported. Other answer sections not audited.
- [teamlead](https://github.com/teamlead/java-interview-questions/blob/4c0784024412904625e1455189d3c05cba07bebf/README.md) L462/472/535 immutability, L471 constructor overloading, L1187 method overloading. No direct pass-by-value or varargs question identified in current filtered excerpt; do not claim otherwise.
- [Hruday](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/ced3cf4f91084e9b45366f7a81a70526e7a4be7d/README.md) L241–242/253 overload vs override, L333/335 final reference and final method parameters, L454–455 String, L613 wrappers immutable. Supports N04/N06; not evidence of observed frequency.
- [Chernyshov](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/597e08112ab6a57d6eac8e3c70c070da606dae73/eng/README.md) L246–249 immutable object/String, L252 final, L274 immutable collections. Adjacent ownership/alias, not evidence of varargs ordering question.
N01–03 are normative follow-ups, not invented C frequency. Finite sample, no absolute completeness. No more exhaustive search of all banks.

## 7. Findings (open until 00)

| ID / severity | Exact location and issue | Acceptance criterion / evidence / role |
|---|---|---|
| JC-A03-N01 P1 (R14 P0 depth) | L136–153,247; null copy correct but no worked unboxing case in null section and self-check conflates compile ambiguity with NPE; L151 shorthand receiver null | JLS §5.1.8/5.3/15.12.4; F08/P03/P16/N01/N08. Distinguish reference null passing, runtime unbox NPE (argument conversion can fail before body), instance method NPE provided earlier evaluation succeeds, m(null) ambiguity compile-time; link N03 and conversions, avoid duplicate. 02/05. |
| JC-A03-N02 P1 (R15) | L155–172 missing common varargs boundaries | §8.4.1/15.12.4.2, P04–07/P14,N03–06. One last varargs, int... and int[] same signature, no args empty, explicit array incl null, Object... single null vs null array, primitive array single object; reference array covariance link ARRAYS, generic warnings ERASURE. Link A02-N05 for fixed-arity/phase choice, not full duplicate. 02. |
| JC-A03-N03 P1 (R16) | L174–189 correct order but no abrupt worked case/receiver capture timing | §15.12.4.1/2/4; P08–12/P16. Show receiver evaluated before args, earlier failure skips later args, null receiver NPE only after normal argument evaluation; earlier side effects not rolled back; argument exception can supersede NPE. Include one worked example and operators link. No universal physical evaluation/CPU order claims. 02/03. |
| JC-A03-N04 P1 | L202 “не исключительно фактического класса” ambiguous about ordinary compile-time overload | JLS §15.12.1–2/4.4, F11/P13; clarify runtime argument class not used for overload choice, compile-time target/arg expressions and applicability/inference; virtual/interface dispatch uses runtime receiver for selected signature, static/private/super not normal overriding; existing inheritance L112–114,359ff. 02/05. |
| JC-A03-N05 P1 (evidence mismatch) | plan L22 claims all counterexamples reproduced by ParametersDemo; ambiguity absent | Exact source L18–19/70–72 has related Object/String overloads, no m(String)/m(Integer) and no ambiguous call; N01 actual new isolated negative confirms intended mechanism, not CI proof. Correct evidence attribution in future erratum if 00 authorizes; optionally 03 add isolated compile-negative under standard, not silently change historical plan or call comment executed. Also completion-only checks not assertion of all states. |
| JC-A03-N06 P2 | L79/219, shared-container/return/alias ownership can be more practical | P01/P02: same object twice mutates both aliases; swap slots differs from swapping caller locals. Add worked example for same-object args and explicit mutation vs defensive copy, link IMMUTABLE L331 and ARRAYS/COMPOSITION rather than expand thread safety. 02 optional. |
| JC-A03-N07 P2 | L102/L189/L219 refer to already published ARRAY/OPERATORS/IMMUTABLE as future; L130 “Integer внутри объекта” | Frozen actual file targets and neighbor claims exist. Change to direct links, distinguish Integer object itself, reference variable and reboxing. Improve self-check with worked answer; no wholesale rewrite. 05. |

## 8. Navigation, pedagogy, limitations, cursor

17/17 article + 3/3 plan relative file targets resolve against frozen tree 499 entries; all three Theory comment targets exist; common examples index 53/53 A01 unchanged. Targeted neighbor evidence: operators L13/276; erasure L82–107 (heap pollution and annotation constraints); arrays L45/254–274; immutability L280/331; functions L117/145/158; inheritance L112–114/157/359. No full neighbor audit; A25 backlinks/contradictions. Article pedagogically clear, uses motivating question, concrete alias/reassignment contrast and independent snippet disclaimer. No reason for broad rewrite or inferred AI authorship. Improve depth, not memorization. 

NOT RUN clean clone, local Gradle/JUnit, GUI/PowerShell, full HTTP/anchors, javap, generic varargs lint in A03, GC/JIT/benchmark/concurrent visibility, all alternative platforms and inline variants. Actual original source and one contextual Probe plus nine independent negatives. No mandatory normative issue unverified, no new blocker. 

**A03 COMPLETE**, cumulative 3/41 articles, 3/42 IDs, 35/925 original rows; 1 BLOCKER / 19 P1 / 6 P2 (A01+A02+A03). Next A04 CORE-CLASSES: frozen classes.md blob 6d95c29fe0b176e0b67568d21a353a8dcc2c574e L1–350; plan CORE-CLASSES.md blob 04f2a28523a8939407cea64d3e0c77ef9284b8c7 L1–72, 17 rows CL-01..17 L21–37, retrospective 14 rows L55–68 separately (not duplicate count), ClassesAndObjectsDemo.java blob 2c40dee06634fe46a3e184dbe12b55afb816deb3. No source changes/remediation, Draft PR #253, no Concurrency.

### Передача эстафеты
Кому: **04 — Independent Reviewer (продолжение аудита)**.
Что сделать: выполнить A04 CORE-CLASSES на frozen source f04f1eb5ee3a400a988757414bcae09113bd885b, classes.md L1–350, plan 1–72, CL-01..17 and separate retrospective checks, ClassesAndObjectsDemo.java. Сохранить отчёт/индекс, exact evidence, findings и следующий cursor. Не исправлять исходники.
Передать: оператор пересылает прямую ссылку на опубликованный checkpoint Issue #252.
