# A07 — CORE-INIT: независимый предметный аудит Java 21

Issue #252, 04 Independent Reviewer, 2026-11-24. Frozen source f04f1eb5ee3a400a988757414bcae09113bd885b; [incoming A06 checkpoint](https://github.com/Melchiorius/backend-interview-labs/issues/252#issuecomment-7132333699), audit parent e9bf760977cbb3b7f73574c4f62dcf73c19cc02a; Draft PR #253. **A07 COMPLETE / CHANGES: 1 new BLOCKER, 8 P1, 2 P2.** A02/A05/A06 blockers retained. No source edits, no remediation before 00 decision after A25.

## 1. Scope/provenance

| Frozen file | Blob | Read |
|---|---|---|
| [initialization.md](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/knowledge/01-java-core/initialization.md) | 6a59e971c8501084089e13881d6ca7e19661708f | 885/885 lines, contiguous separate calls 1–248,249–442,443–662,663–885 |
| [CORE-INIT.md](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/docs/plans/CORE-INIT.md) | 80a428a25305aa5f09844c6254a004945b404227 | 276/276, separate calls 1–119,120–200,201–276; 20 original rows 120–139 |
| [README](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/initialization/README.md) | 22c3ec516a4a35af14f4da55997726080a96f791 | 108/108 lines |
| [InitializationDemo.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/initialization/src/InitializationDemo.java) | 89777df4cf02018c0cf2381bbf0ca01f54efeb49 | complete 126 source lines, local exact hash |
| [expected.stdout](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/initialization/expected.stdout) | 53a990195e07dde9eca9a250bef2e1017f71c8da | 29/29, local exact hash |
| [verify.sh](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/initialization/verify.sh) | a6af428dcee3ff7e4c7c794ce080055ab7f76ee9 | complete, mode100644, local exact hash |

History path: 261f195a199f61e16abed5b0eaf16605840907b3 PR #54 original; 7f4b83b1dfc6f3871389faa2b70f663b3bbb71b5 interfaces/composition; edf68df067fe4c4583965af002e225d6371796d2 modules/classloader. No substantive authorship by 04 identified in history titles; shared Git identity/squash limits attribution. Prior PASS not proof of current completeness. Normative source and actual local probes below.

## 2. Continuous review

A = JLS/API, I = observed compiler/runtime. All contiguous article ranges read, including tails and 20-row self-map. Preserve strong central example and distinction between initialization vs loading.

| Article lines | Claims | New assessment |
|---|---|---|
| 1–18 | lifecycle 5 steps, loading/linking/class init, object default fields | correct main model, scope ordinary creation and recursive requests (N01/N05); linking may defer resolution. ConstantValue step later explicit. |
| 19–200 | central example, commands, expected output | actual comparison: article L26–151 equals fetched Java source excluding trailing newline; L170–198 equals fixture excluding trailing newline; exact local runner succeeded §5. |
| 201–248 | 11 step analysis: const, BOXED, parent, interface default/postorder, direct interface, constructor order, virtual call, this chain | correct for example; no general promise all fields still default, class init complete here actually true. Repeated this-chain no duplicate init. |
| 249–266 | loading/verification/preparation/resolution, class/init constructor body, initial class before main | correct ordinary model; L263 scope ordinary creation, not clone/deserialization; N05. |
| 267–300 | active use, nontriggers, reflection, loadClass and forName | JLS triggers correct; clarify qualifier side effects, custom loader code, reflection operation distinction (N02). Standard loader probe confirms. |
| 301–333 | inherited static field initializes declaring type | correct, active probe confirms inherited static method too; no guarantee qualifier expression has no other effect. |
| 334–350 | constant variable table, early step before super, binary embedding | correct type and expression distinction. “может” weak for compiled Java constant field references (JLS13.1), N09; no two-version build performed. |
| 352–399 | recursive default interface postorder, synthetic default methods, direct interface not ancestors | correct JLS12.4.1/2, local interface and interface_direct reproduce diamond and default-parent skip in direct init. Caveat side effects of initializer body explicitly separate from automatic process. N01 recursion. |
| 401–442 | static field/block text order, simple/qualified forward references | actual order and forward probes, simple negative. N07 precise §8.3.3 restrictions and blank final definite assignment. |
| 443–458 | default values including positive float zero, local definite assignment, all inherited/declaration fields | correct normative initial state; avoid treating noninherited private superclass fields as absent (A06-N01); local unassigned reject. |
| 459–497 | allocation/default/constructor chain and this delegation | normal chain correct. L461 general preceding class init requires same-thread exception N01; step3 skips args, N05. Object has no super. Instance initializer executes once only for normally reached constructor stage; N05. |
| 498–539 | implicit super(), no-args, default ctor, Java21 first statement, cycle | **B01 L500–502** zero actual args not necessarily zero formal params; varargs accepted even for implicit default subclass. N05 extra restrictions. Original Parent(int) negative correct. |
| 540–559 | instance field/block text order | actual order; normal successful path, no guarantee when superclass ctor fails. |
| 560–576 | virtual calls during construction, default subclass state, private/final helper caveat | central example correct. Earlier overridable call can write subclass fields; actual early_write=68 then final initializer 67; N03. |
| 577–605 | volatile this escape, future writes not published retroactively, same-thread vs interthread | correct; local escape=0 then after=67 same-thread only. No claim race result. |
| 606–662 | erroneous class, wrapping non-Error, Error unchanged, parent/interface failure | local failure case correct; JLS12.4.2 step11 OOME fallback, N04, failure scope per runtime type. No retry guarantee after failure, no assertion stacktrace exact. |
| 663–707 | retry instance failure vs erroneous class | actual instance_retry; no guarantee every retry succeeds, article says may, correct; partial escape/side effects not rolled back N05. |
| 708–744 | same-thread recursive A/B reads default A, 2/1 | actual recursive; correct, use to qualify earlier general sequencing N01. |
| 745–757 | unique init lock, waiting, same-thread recursion, HB/static safe publication, final field scope | sound basic model; N06 clarify lock not held throughout initializer, no synchronization guarantee for arbitrary escaped references, no promise mutable singleton safe. No concurrency experiment. |
| 758–790 | ClassCircularityError loading/derivation, source cycle, cross-thread deadlock, loader identity | correct article does not claim locks held while executing initializer; historical plan CI18 L137 “держат ... locks” misleading, N06. Source cycle only observed, runtime ClassCircularityError/deadlock/defining loader isolation not run. |
| 791–805 | common false statements and corrections | good, add B01/N01/N03/N06 qualifier. |
| 806–832 | self-map 20 rows | all original IDs checked §3, not independent count. |
| 833–885 | boundaries, sources, exclusions | actual classloader/modules links present, no broad coverage claim; C refreshed independently, additional locators and README detail N10. |

## 3. Original matrix 20/20 (10 P0/6 P1/4 P2)

| ID | P | Plan L | Article L | Verdict |
|---|---|---|---|---|
| CI-01 | P0 | 120 | 5–17,249–265 | OK, phases separated |
| CI-02 | P0 | 121 | 267–300 | DEPTH N02 trigger vs incidental execution/custom loader |
| CI-03 | P0 | 122 | 203–209,334–350 | OK constant rule and early step |
| CI-04 | P0 | 123 | 207–209,301–333 | OK declaring field/method; active probe |
| CI-05 | P0 | 124 | 352–373,401–442,461 | DEPTH N01 normal order vs recursive new |
| CI-06 | P0 | 125 | 213–219,366–399 | OK default interface order/synthetic caveat |
| CI-07 | P0 | 126 | 223,443–458 | OK initial default values and DA |
| CI-08 | P0 | 127 | 459–559 | ERROR B01, super with no actual args can choose varargs; N05 args/abrupt |
| CI-09 | P0 | 128 | 233–235,560–575 | DEPTH N03 default values not guaranteed to persist |
| CI-10 | P0 | 129 | 606–661 | DEPTH N04 OOME wrapper fallback, actual normal failures OK |
| CI-11 | P1 | 130 | 708–744 | OK actual recursive=2/1 |
| CI-12 | P1 | 131 | 745–757 | DEPTH N06 precise lock/HB scope |
| CI-13 | P1 | 132 | 663–707 | OK instance retry |
| CI-14 | P1 | 133 | 279–299 | DEPTH N02 API caveats and worked output |
| CI-15 | P1 | 134 | 577–605 | OK language/JMM distinction |
| CI-16 | P1 | 135 | 419–441,455 | DEPTH N07 precise forward reference and blank final DA; N08 initializer exceptions extra follow-up |
| CI-17 | P2 | 136 | 760–773 | BOUND source negative only, runtime not run |
| CI-18 | P2 | 137 | 775–783 | BOUND theoretical wait cycle, N06 historical plan lock wording |
| CI-19 | P2 | 138 | 785–789 | BOUND defining loader identity, not full loader test |
| CI-20 | P2 | 139 | 348–350 | BOUND binary compatibility, N09 precision |

Totals 8 OK / 7 DEPTH / 1 ERROR / 4 BOUND. P0 5 OK/4 DEPTH/1 ERROR. Article L812–831 self-map checked separately same IDs no double-count. Plan L1–119 source/boundary/exact rules read; L120–139 matrix above; L141–203 seven historical positive and five negative probes described, original temp files not preserved, no new credit; L205–225 two Java blocks covered by central/recursive actual, L229–276 gate/CI scope and risks read. Historical G1 CI not init harness, later integration CI yes, preserve history. Plan L104/199 and matrix CI08 need erratum if 00 authorizes (no plan edits now); plan L137 lock wording N06.

## 4. Normative review and independent C sources

Direct A retrieval JLS SE21 [§8.8.7–8.8.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.7), source chapter L2610–2706, default ctor wording §8.8.9 L2780–2784, distinguish no actual args vs no formal params and applicability via §15.12.2; actual compiler confirms. [§12.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.1) default and synthetic methods; [§12.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.2) steps 2–6 release lock, recursion step3, step10 notify, step11 OOME fallback; [§15.9.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9.4) qualifier, allocation/default, left-to-right args and invocation. [ClassLoader API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ClassLoader.html#loadClass(java.lang.String)) public loadClass and default protected overload, custom override boundary. Other locators §4.12.4–5, §8.3.3, §8.6/7, §12.5, §13.1/13.4.9, §16.8–9, §17.4.5/17.5.1, JVMS5.3/5.5, Class.forName API relied on as established norms and article links, not claim every section freshly read in full. I actual Debian/Temurin output only. B dev.java historical cache miss, not refreshed or used.

C fresh finite question sample, no frequency claims:
- [teamlead pinned](https://github.com/teamlead/java-interview-questions/blob/4c0784024412904625e1455189d3c05cba07bebf/README.md) L610/715 static/nonstatic initialization, L657/730 failure, L736 static blocks, L739 constructor/blocks hierarchy, L777 defaults, L782 initializer. Old plan approximate line locators differ; actual pinned line numbers here.
- [Hruday pinned](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/ced3cf4f91084e9b45366f7a81a70526e7a4be7d/README.md) nonplan C, L171 constructor exception, L293 creation vs init, L312–317 static blocks incl checked exceptions, L1604–1612 loading/initialization, L1933 publication, L2820 reflection trigger. N08 checked initializer rules explicit fresh gap.
- [Baeldung structure and initialization Q10](https://www.baeldung.com/java-classes-initialization-questions) actual page retrieved: phrasing conflates loading with initialization and compiler copies blocks into every constructor; article correctly avoids both. C prompt not authority.
- [Chernyshov RU pinned](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/597e08112ab6a57d6eac8e3c70c070da606dae73/ru/README.md) L90–93 Singleton/thread safety/DCL, only adjacent C; direct full order/failure gap in selected pattern. Four fetched C sources (three direct/relevant, one adjacent), historical Baeldung CORE not refreshed; no claim exhaustive.

## 5. Exact runnable/fixture and CI

Read all four package files, restored 3 executable inputs byte-identically in /tmp/jc-a07/exact/examples/core/initialization. Actual git hash-object matched all three blobs in §1 (3472/766/482 bytes). Local Debian OpenJDK 21.0.12.1, javac 21.0.12.1. bash verify.sh exit0 (script computes root from own path), compiles --release 21, captures actual.stdout, diff -u against expected.stdout, prints 29 lines and CHECK PASSED: stdout matches expected.stdout. No JUnit tests/explicit check calls; exact stdout comparator, not 29 independent tests. Article embedded source and expected output both equal committed content modulo final newline, verified by fetched content comparison. README all commands/read scope, PowerShell not run. No original negative files.

[Exact frozen push CI #45390717871](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45390717871), job 138433360807 raw log 12:09:36.5593267Z–12:09:37.1168418Z, Temurin21.0.13-9, all 29 same lines and marker. Previous A06 audit CI #45399782344 success. Original historical 01 G1 local temp cases not reproduced byte-for-byte; new probes below. Runner only central demo, not failure/recursive/loader/concurrency. Actual fixture correct; README L95 step4 “Child(int), вызванного через super()” reverses delegation: Child(int) was called by this(67), it itself calls super(). N10 editorial fix.

## 6. New independent local probes

Actual /tmp/jc-a07/probes.py created 25 distinct directories with Main.java and separate output. javac --release 21 -XDrawDiagnostics -d <case> <case>/Main.java. 14 positive compile exit0 and java -cp <case> Main exit0 (timeout5), 11 negative compiler exit1, actual outputs/diagnostics below. All separate from exact original runner. Contextual equivalents, not every article fragment copied verbatim. No concurrency, loader isolation, heap exhaustion or fabricated output.

| Case | Source scenario | Actual stdout (line separator / where clear) |
|---|---|---|
| varargs | Parent(int... xs){println(xs.length)}; Child extends Parent{Child(){}}; Implicit extends Parent{}; new each | 0 / 0, B01 |
| reentrant_new | C.static C instance=new C(); static int value=67; C(){println(value)}; then read value | during=0 / after=67, N01 |
| early_write | Parent constructor write();read(); Child overrides write n=68, read prints n; Child int n=67 initializer | 68 / 67, N03 |
| active | C.class,new C[1], qualifier().K constant where qualifier prints and returns null; loader.loadClass C, Class.forName false; D extends C, D.f inherited static; E.v assignment; Class.forName D true | C / 1 / qualifier / 67 / loaded / init C / call C / init E / 67 / init D, N02 |
| forward | static a=Main.b; via=read() before b=67; instance x=this.y before y=68; static block k=67 | 0:0:0:68:67 |
| failure | A initializer IllegalStateException, C extends A first/second C.y then A.x; B initializer AssertionError first/second; I default interface failing initializer and D implements I first/second | ExceptionInInitializerError:IllegalStateException / NoClassDefFoundError:ExceptionInInitializerError / NoClassDefFoundError:ExceptionInInitializerError / AssertionError:none / NoClassDefFoundError:ExceptionInInitializerError / ExceptionInInitializerError:IllegalStateException / NoClassDefFoundError:ExceptionInInitializerError. Cause text observation not guarantee. |
| instance_retry | first instance block throws, second succeeds | first attempt / success:2 |
| recursive | A.x=B.y+1, B.y=A.x+1, first A.x | 2/1 |
| order | static field/block/field; instance field/block/field; C(){this(67);...}; C(int) | static field / static block / static field2 / field / block / field2 / C(int) / C() |
| interface | A declares default; B extends A no own default; C extends A own default; D implements B,C; read D.d then B.b | A / C / D / 1 / B / 1 |
| interface_direct | A and B extends A both own defaults; read B.b directly | B / 1 (A not initialized) |
| arg_order | C class block, new C(arg("new arg")), super(arg("super arg")), parent body, child field/body | C class / new arg / super arg / P body / C field / C body |
| escape | parent invokes override register, volatile escaped=this; prints escaped.value before Child initializer, then read after | escape=0 / after=67; same thread only |
| custom_loader | overridden public loadClass for Target calls Class.forName(name,true,getParent()) | target init / done; not claim default loader behaves this way |

| Negative | Exact core statement | Observed compiler.err category |
|---|---|---|
| no_super | P(int i) only; C(){ } | cant.apply.symbol arg.length.mismatch |
| cycle_this | C(){this(67);} C(int i){this();} | recursive.ctor.invocation |
| not_first | C(){println(67);super();} | call.must.be.first.stmt.in.ctor |
| forward_simple | static int a=b; static int b=67; | illegal.forward.ref |
| cycle_classes | A extends B; B extends A | cyclic.inheritance |
| blank_final | final int x; C(){} | var.might.not.have.been.initialized |
| blank_final_static | static final int x; | var.might.not.have.been.initialized |
| local_unassigned | int x; println(x); | var.might.not.have.been.initialized |
| checked_super | P() throws IOException; C(){} | unreported.exception.need.to.catch.or.throw |
| ambiguous_super | P(String...a), P(Integer...a), C(){} | ref.ambiguous |
| early_this | int x; C(){this(x);} C(int x){} | cant.ref.before.ctor.called |

17 Java fences: L25–152 exact central; 291–297 active loader contextual; 303–323 active inherited member (method probe + exact central field); 376–397 interface_direct contextual; 403–415 order; 421–426 forward_simple; 430–439 forward; 476–494 order; 504–512 no_super; 520–527 not_first; 533–538 cycle_this; 544–556 order; 581–598 escape; 610–642 failure contextual (A first failure through C); 667–690 instance_retry; 712–726 recursive; 768–771 cycle_classes. All read/mapped; not literally all same source. Two shell blocks, four text blocks. Plan two Java fences contextual; historical commands not run verbatim. README shell command runner executed, PowerShell not run. No fresh synthetic default compiler bytecode, reflection actual get/invoke, binary compatibility two-version build, checked initializer direct negative, or failed superclass ctor side-effect probe; relevant normative findings distinguished from executed evidence.

## 7. Findings (open until 00)

| ID / severity | Locus | Evidence / acceptance criterion / proposed role |
|---|---|---|
| JC-A07-B01 BLOCKER (CI08 P0) | L500–502 (and historical plan implicit no-args premise) | “нет доступного конструктора без аргументов → не компилируется” reads as no formal params; actual Parent(int... xs) no zero-formal constructor, both explicit Child(){} and implicit subclass accepted, outputs 0/0. JLS8.8.7–9 and invocation applicability, distinguish zero actual args vs empty formal list, accessible applicable unambiguous constructor and checked throws; no_super/ambiguous_super/checked_super negatives actual. Preserve original Parent(int) failure. Add contrast, cross A04-N03/A05-N01, 02 and optional 03 regression after 00. |
| JC-A07-N01 P1 (CI05 P0 depth) | L7–15,461 ordinary class-init before instance; L352–399 | JLS12.4.2 step3 same-thread recursion may return normally before completion; actual static C.instance=new C then value=67 prints during=0, after=67. Explicit normal external first-use vs recursive exception, link existing L708; direct interface rule only automatic, explicit body may trigger others. 02. |
| JC-A07-N02 P1 (CI02/14) | L279–299,332,346 | Class literal/array/constant expression not intrinsic init trigger; qualifier or arg eval may have side effects, static method declaring class rule, standard loadClass not active trigger. Actual active output, custom_loader override may explicitly initialize. Distinguish default loading contract vs arbitrary custom code; reflection examples Field.get/Method.invoke on static, metadata lookup not same; array Class.forName name boundary, not claim all reflective actions. Add small actual markers, link CLASSLOADER. 02. |
| JC-A07-N03 P1 (CI09 P0 depth) | L564–571 and broader “fields still default” shorthand | default values assigned initially, may have changed before own field initializer: actual Parent write then read override 68, child initializer later sets 67. Correct central 0/null, phrase “may still contain default values” not always. Keep advice about final helper. Cross A04/A06. 02. |
| JC-A07-N04 P1 (CI10 P0 depth) | L651–657 | JLS12.4.2 step11 if allocation of wrapper EIIE fails OOME used; not unconditionally wraps every non-Error. State erroneous applies particular runtime definition; subsequent initialization request fails, not every use (class literal/constant can still work); parent/interface failure propagation actual; NCDFE not unique to erroneous state (also missing class). No forced OOME. 02. |
| JC-A07-N05 P1 (CI08) | L459–472,542,663–699 | JLS15.9.4 allocation/default then args left-to-right then constructor; §8.8.7.1 evaluates this/super args before invoked body, abrupt skips later steps; Object root special, qualified super inner and enum/record special linked. Actual arg_order and early_this. Scope ordinary construction normally completing, no claim clone/deserialization invokes init, no rollback of side effects on failure. Add concise sequence, cross A03-N03/A04-N04. 02. |
| JC-A07-N06 P1 (CI12/18) | L747–756,775–783, plan L137 | JLS12.4.2 releases LC before initializer/dependency execution; waits on in-progress state, not necessarily both threads hold monitors; correct article wait graph, fix wording in future erratum. HB visibility for later successful initialization use, not references escaped before completion nor subsequent mutable updates; no class monitor assumption. No actual concurrency test, retain CON-JMM boundary. 02. |
| JC-A07-N07 P1 (CI16) | L419–441,455, sources §16 | JLS8.3.3 simple name restrictions context and assignment LHS exceptions, qualified/method access can read default, not universal permission incl blank final definite assignment. Actual forward=0:0:0:68:67 and blank_final/static/local negatives; add one short DA contrast and instance field case, not full types repeat. 02. |
| JC-A07-N08 P1 (post-draft C gap) | L401–442,540–559,606–661 | Hruday L316 asks checked exceptions from static initializer. Add compile-time boundary: named class static initializer cannot propagate checked exception; instance initializer checked exception conditions for constructors (§11.2.3), normal completion rule §8.6/8.7; helper throwing unchecked used correctly. Normative gap, no new checked initializer compile probe. Link EXCEPTIONS. 02. |
| JC-A07-N09 P2 (CI20) | L205/350 “может быть встроено” | JLS13.1/13.4.9 constant field reference compiled to value (different from discretionary JIT optimization), old binary keeps old value until recompilation where applicable; distinguish ordinary static final nonconstant reference. Two-version binary experiment not run. 02. |
| JC-A07-N10 P2 | README step4, article L812 and links | README says Child(int) called via super(), actually invoked by this(67) and itself invokes super(); fix direction. Map L812 old heading vs actual “Loading, linking...”; add direct plan link and §15.9.4/§11.2.3 source locators, pin C locators. Index A07 technical path accidentally README name with verify.sh hash from A00 inventory; correct only audit index if authorized/needed, source untouched. 05. |

Severity B01 actual false generalization at P0, other omissions scoped correct central examples, no extra blocker manufactured. A02/A05/A06 remain. No remediation.

## 8. Navigation, limitations, next cursor

Frozen complete tree499; 10/10 article +4/4 plan +6/6 example README =20/20 relative file targets, no anchor/HTTP check. Actual article source and fixture exact match; central teaching trace especially strong. No inference of AI authorship. No source edits. NOT RUN full clone/Gradle/JUnit, all exact snippet variants, GUI/PowerShell/Obsidian, full HTTP/anchors, synthetic default javap, physical memory, OOME, GC/JIT, reflection get/invoke, classloader isolation, runtime ClassCircularityError, concurrent lock/deadlock/race tests, binary two-version compilation, checked initializer compiler probe. Four C sources finite, Chernyshov adjacent, historical Baeldung CORE/dev.java not refreshed. Local exact runner and 25 new cases actual. No external execution blocker.

**A07 COMPLETE**, cumulative 7/41 articles, 7/42 IDs, 117/925 original rows; 4 BLOCKER / 48 P1 / 14 P2. Next A08 CORE-OBJECT + CORE-EQUALITY: object.md blob 5956b2e3c4192ae4a822cc212af8d75bdedba121 L1–212; equality.md blob 9707444fd8203ba347f28b69d602f94bd3742222 L1–484 (1–232,233–484); CORE-OBJECT-EQUALITY.md blob db74fca5f0dae7b150b6d1aa005ddb3890688869 L1–280, 26 original OE-01..26 L50–75; entire examples/core/equality 4 blobs incl README 0efbfcd661ff03883d3b3c4cdc30f9084ab0fb27, src/EqualityDemo.java dce3f059609d2f64002be9fbfdc10e2c694f3b3b, expected.stdout 89bdcf313ea57bce1d552ee2e826b1944aeb2246, verify.sh c01290b8ad00c0516ce82ac7584a46ba9ece666d mode100755. Cross-check A04-N01/N04/N08, A05-N06, A06-IH25, A07-N05. One stage two IDs, 26 rows once. If necessary checkpoint contiguous range rather than false COMPLETE.

### Передача эстафеты
Кому: **04 — Independent Reviewer (продолжение аудита)**.
Что сделать: выполнить A08 CORE-OBJECT/EQUALITY на frozen source f04f1eb5ee3a400a988757414bcae09113bd885b, object.md L1–212, equality.md L1–484, plan 1–280, OE-01..26, entire equality package/fixture, A/I/C checks; сохранить отчёт/индекс и новый cursor, при необходимости промежуточный checkpoint. PR #253 Draft; учебные материалы не менять.
Передать: оператор пересылает прямую ссылку на опубликованный checkpoint Issue #252.
