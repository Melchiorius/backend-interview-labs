# A04 — CORE-CLASSES: независимый предметный аудит

Issue #252, роль 04, 2026-11-24. Frozen source BASE f04f1eb5ee3a400a988757414bcae09113bd885b; [incoming checkpoint A03](https://github.com/Melchiorius/backend-interview-labs/issues/252#issuecomment-7131834171), audit parent 3d830f57685a6e68b74c92ccbad77f20064288f6; Draft PR #253. **A04 COMPLETE / CHANGES recommended: 0 новых BLOCKER, 7 P1, 2 P2.** A02-B01 остаётся открытым. Учебные материалы и исторические планы не изменять; решения 00 после A25.

## 1. Scope, provenance, независимость

| Frozen source | Exact blob | Read |
|---|---|---|
| [classes.md](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/knowledge/01-java-core/classes.md) | 6d95c29fe0b176e0b67568d21a353a8dcc2c574e | 1–350 полностью; хвост 279–350 повторно получен после ограничения первого вывода |
| [CORE-CLASSES.md](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/docs/plans/CORE-CLASSES.md) | 04f2a28523a8939407cea64d3e0c77ef9284b8c7 | 1–72 полностью: 17 original + 14 retrospective rows |
| [ClassesAndObjectsDemo.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/ClassesAndObjectsDemo.java) | 2c40dee06634fe46a3e184dbe12b55afb816deb3 | 1–101 полностью; exact local hash |

History path at frozen: 60af1b048c47404a8da7ee94ec92c12b1585690e original, 68a70d22308bbc944d57528a4ec5a8aa599d4644 access, 86e67b756395373b34cbd9c4c8a7c7435c7d090e inheritance, 261f195a199f61e16abed5b0eaf16605840907b3 init, 4680b057aeabcd3e4d36e15b13b4db0d032c944c object/equality, 7f4b83b1dfc6f3871389faa2b70f663b3bbb71b5 interfaces/composition, 3e328df475016ffe6f32f1190ddf2d47e556e14b records/immutable, aa183b3b1bdba3c5eed8163e4b10aee82a043092 reflection/annotations, edf68df067fe4c4583965af002e225d6371796d2 classloader/modules. No indication of substantive 04 authorship in titles, common login/squash not exhaustive provenance. Historical PASS not counted. A00 old-source diff did not list classes.md.

## 2. Continuous claims review (all lines)

A JLS/API, I local compiler/runtime, B official explanatory material, C interview sample. All claims evaluated in context of ordinary Java21 class, no preview. F/P/N evidence §5; neighbor references targeted only.

| Lines | Claim and example | Verdict / evidence |
|---|---|---|
| 1–23 | class describes data/behavior/construction; object instance, new, alias | F01 false:true and A03 pass-by-value. JLS §4.3.1 object includes array, not just class instance; N01. new normally yields reference, not variable or object copy. |
| 24–73 | CreditAccount validates owner and nonnegative initial balance; positive deposit exact add, withdraw bounds; final owner, mutable balance, private, getter | F02 77:0:MAX; valid amount checks and assignment only after successful addExact. Sequential invariant balance>=0 holds; owner nonblank also invariant. No thread safety guarantee; article class non-final vs final technical demo, overridable behavior is separate, N07. Private external boundary requires nest/lexical caveat. |
| 74–82 | invariant, validation, constructor abrupt completion no normal new result, possible allocation/side effects | OK for ordinary sequential operation; P05 deliberately captures failed instance in local test only, not production pattern. A §12.5/15.9.4; no assumption GC or transactional rollback. N07 precision/link. |
| 83–117 | static shared for class, instance field per object, count increment in constructor, 1/0/2 | F03 exactly 1:0:2. createdCount is successful increments, not count of live objects, no guarantee no overflow. Class identity loader boundary L145. |
| 118–146 | comparison table; no this in static, instance access through explicit reference; static syntax via object; no atomic increment | N02 L123 misleading headings (static member access needs no instance, static method needs explicit instance only to access instance member). F04, N01/02, F11/P01. No actual race test; CON deferred. |
| 147–164 | constructor no return type, void-named method, overload not inherit/override | F05, P06, N10. Bare return; legal, return value illegal JLS §8.8.7. P06 demonstrates method called explicitly, not constructor. Add compact contrast N03. |
| 165–185 | implicit default only without declared constructor, no-args explicit, access rule | F06, N03, N08. Ordinary class condition necessary; accessible applicable superclass constructor required, absence of constructor declarations doesn't guarantee valid class. Non-private inner member default has implicit enclosing-instance formal parameter; record canonical separate, P09. N03. |
| 186–212 | this.field vs parameter; this(...) delegation; Java21 first statement, cyclic prohibition; superclass construction and virtual call caveat | F07, N06/N07/N11. A §8.8.7/7.1 correct no-preview Java21. P02 base virtual call sees default child field 0. INIT L227/243 correct neighbor; no full inheritance audit. |
| 213–241 | ordinary new defaults incl inherited fields, initializer textual order, body, normal reference result; simplified hierarchy; static initialization and compile-time constant | F08 67:69; P03 once per class, P04 clone skip. Scope explicitly simplified but L238 broad “при создании каждого объекта” needs ordinary class initialization boundary; N04. Class initialization (not loading), actual class declaring static field, constant read caveat correct; INIT deeper. No OOME intentionally induced. |
| 242–259 | overload int/long vs return-only conflict, compile-time selection vs override | F09, N04. Basic ordinary overload correct; generic signature/erasure deferred A11. Methods can be inherited and overloaded together; avoid claim only declared in one class. A02/A03 overload findings cross-link. |
| 260–288 | private constructor, factory validation, no cache promise, obtaining object without direct new | F10, N05; P08 lexical nest access, P07/N09 abstract constructor, P09 records separate. CL12 explicitly expected abstract class, not present in article; N05. Constructor private is not global singleton guarantee, no reflection bypass experiment. Serialization exists already, stale future text N09. |
| 289–305 | 12 self-check questions | Core answers correct in scope, L298 default condition N03, L302 final fields correct but not final class/method. Negative balance example refers to lack of validation, not all unprotected API necessarily fails. |
| 306–320 | class static via null; expression evaluated and discarded, exception matters; no physical memory placement claim | F11 Marker twice, P01 normal qualifier side effect and abrupt failure. A §15.12.4.1/4 confirms, not runtime dispatch. No JVM layout claim. |
| 322–342 | original demo and command, commented negatives not automated; existing links and future boundaries | §6 original technical result, 10 checks/3 rejection helpers. Explicit evidence limits correct. L287 future serial stale, L81 CORE-INIT now link, classloader now existing article and JVM-LOADING still broader deferred. N09. |
| 344–350 | JLS §8,12.4,12.5,15.9 and B dev.java | A chapters directly retrieved. B overview direct fetch cache miss, recovered via search + More on Classes opened. Do not import B version drift or loose language. See §7. |

## 3. Original G1 matrix 17/17

Plan 1–72, original L21–37: 9 P0 / 6 P1 / 2 P2. Historical gate separately §4. Original IDs not renumbered.

| ID | P | Plan L | Article L | Verdict |
|---|---|---|---|---|
| CL-01 | P0 | 21 | 5–20 | DEPTH N01 object vs array, correct class examples F01 |
| CL-02 | P0 | 22 | 24–73,206,217–238 | OK state/default/shadowing, links TYPES/PARAMS, no physical layout claim |
| CL-03 | P0 | 23 | 83–145 | DEPTH N02 comparison row misleading, F03 correct |
| CL-04 | P0 | 24 | 147–163,67–81 | OK N10 independent compiler error, bare return nuance N03 |
| CL-05 | P0 | 25 | 165–184 | DEPTH N03 super accessibility condition and special class boundary |
| CL-06 | P0 | 26 | 186–208 | OK F07/N06–07/N11, delegation and shadowing |
| CL-07 | P0 | 27 | 213–240 | DEPTH N04 scope and once-per-class, F08 correct, full INIT linked |
| CL-08 | P0 | 28 | 24–81,324 | OK sequential invariant with overflow and rejection, F02 + exact source; N07 wording |
| CL-09 | P0 | 29 | 125–145 | OK F04/N01–02, static context no this |
| CL-10 | P1 | 30 | 242–259 | OK F09/N04, full overload elsewhere |
| CL-11 | P1 | 31 | 142,304–318 | OK F11/P01, no target dereference |
| CL-12 | P1 | 32 | 260–288 | DEPTH N05 factory covered, abstract class missing local contrast |
| CL-13 | P1 | 33 | 69,302,338 | DEPTH N06 final field covered, class/method absent as expected local minimum |
| CL-14 | P1 | 34 | 210–240 | BOUND explicitly INIT/INHERITANCE with warning; P02 verifies reason |
| CL-15 | P1 | 35 | 72,182,284,338 | OK private/access boundary appropriate, N07 precise terminology, detailed A05 next |
| CL-16 | P2 | 36 | 16–20,287,338–342 | DEPTH N08 minimal getClass contrast absent, actual OBJECT target has mechanism |
| CL-17 | P2 | 37 | 145,320,340 | BOUND correct no global singleton/thread safety, actual CLASSLOADER link |

Totals 8 OK / 7 DEPTH / 2 BOUND; P0 5 OK / 4 DEPTH. No ERROR/MISSING entire row; CL12/13/16 partial omissions recorded (not fully covered). No automatic final PASS.

## 4. Separate retrospective table L55–68 — 14/14 entries

These are the same 17 IDs grouped into 14 rows, not 14 new independent questions; count once above. Re-evaluate claims instead of copying old covered labels.

| Plan row | Original grouped ID | Independent check |
|---|---|---|
| 55 | CL-01 | covered class examples, N01 precision taxonomy |
| 56 | CL-02 | OK, state/default, F02/F08 |
| 57 | CL-03,09 | CL03 DEPTH misleading table; CL09 OK static negatives |
| 58 | CL-04,05 | CL04 OK; CL05 DEPTH, N08 negative superclass |
| 59 | CL-06 | OK F07; cyclic/first/reference negatives |
| 60 | CL-07 | DEPTH N04 but acknowledged hierarchy boundary, INIT source actual |
| 61 | CL-08 | OK sequential invariant, runtime invalid/overflow cases, N07 |
| 62 | CL-10 | OK, N04 actual compiler duplicate |
| 63 | CL-11 | OK, static-null example and qualifier P01 |
| 64 | CL-12 | factory covered but abstract class absent, DEPTH |
| 65 | CL-13 | correct old status mentioned/deferred, minimum class/method missing, DEPTH |
| 66 | CL-14 | BOUND consistent with deferred and warning |
| 67 | CL-15 | OK scoped mention and actual ACCESS target, N07 |
| 68 | CL-16,17 | CL16 DEPTH, CL17 BOUND, no false current JVM claim |

Plan L41–44/48/70–72 reviewed: original source includes shared count, alias, checks; commented negatives explicitly not claimed executed by old CI. Current independent negatives do not retroactively change that history. No separate exercise required. Historical “all P0 covered” not identical to new depth assessment.

## 5. Local contextual probes and negatives

Actual Debian OpenJDK/javac 21.0.12.1, /tmp/jc-a04. One temporary Probe.java with classes nested in wrapper, equivalent account code and renamed Delegated class for repeated Point, additional print statements, exception catches, assertions to check expected rejected operations. Token is nested in this probe, private negative separately uses unrelated top-level classes. javac --release 21 -Xlint:all Probe.java exit0 with three intentional [static] warnings; java Probe exit0, 23 stdout lines. Not 23 tests. Code snippets not represented as verbatim standalone compilation units.

| Fence | Context and actual output |
|---|---|
| 11–18 | F01=false:true (two accounts and alias) |
| 28–65 | F02=77:0:9223372036854775807; positive deposit/withdraw, rejected zero, null/blank owner, negative start, overdraw and overflow |
| 87–116 | F03=1:0:2 |
| 127–140 | F04=1, isolated N01/N02 for commented errors |
| 151–161 | F05=67:10 |
| 169–180 | F06=0:true, N03 |
| 190–204 | F07=67:0, N06/N07/N11 |
| 223–236 | F08=67:69 |
| 246–254 | F09=int:long, N04 |
| 264–282 | F10=67, rejects blank; N05 independent unrelated class |
| 308–316 | F11=Marker:Marker, warning [static] expected |
| 328–330 shell | original source-file run §6, not PowerShell GUI |

Independent probes (additional labels; reproducible logic in wrapper, not repo source):
- P01 qualifier() prints "P01 qualifier", returns null; qualifier().name() returns Marker; fail().name() where fail throws IllegalStateException("qualifier") propagates it. Actual lines P01 qualifier / P01=Marker / P01 failure=qualifier.
- P02 Base() calls overridable read(); Child.int n=67 and override read returns n. new Child() prints P02 base sees=0. This is a controlled negative example, not recommended design.
- P03 class Chain: static int count; {count++;} Chain(){this(67);} Chain(int n){}; new Chain() => P03 chain initializer=1.
- P04 Cloned implements Cloneable, static count and {count++;}, copy() returns (Cloned)super.clone(); original=new Cloned(); copy=original.copy(); actual P04 clone=true:1 (distinct identity, initializer not repeated).
- P05 Bad constructor assigns static Bad leaked=this, throws IllegalStateException; caught; P05 escaped=true. Local deterministic anti-example only, no concurrency/publication safety claim.
- P06 class VoidNamed with void VoidNamed(){println(...)}; new VoidNamed().VoidNamed() => P06 method called; ordinary default constructor exists because method not constructor.
- P07 abstract class Abstract with constructor printing P07 abstract constructor; new Concrete extends Abstract executes it.
- P08 Outer with nonstatic Inner{} and private static nested Token with private constructor; Outer.get() creates Token. new Outer().get()!=null and new Outer().new Inner()!=null => P08 nest=true:true.
- P09 record Record(int x){Record(){this(67);}}; new Record(67) => P09 record=Record[x=67] (implicit canonical constructor distinct from default).
- P10 new int[1].getClass().isArray() => P10 array=true.

All 14 isolated negatives actual javac --release 21 -XDrawDiagnostics Nnn.java, each exit1; exact statements/classes and observed category:
| Case | Source | compiler.err |
|---|---|---|
| N01 | class N01 {int value;static void f(){value++;}} | non-static.cant.be.ref variable value |
| N02 | class N02 {int value;static void f(){this.value++;}} | non-static.cant.be.ref variable this |
| N03 | class N03 {N03(int x){}static void f(){new N03();}} | cant.apply.symbol constructor arg.length.mismatch |
| N04 | class N04 {String f(int x){return "";}int f(int x){return x;}} | already.defined f(int) |
| N05 | class N05 {static void f(){new PrivateToken();}} class PrivateToken {private PrivateToken(){}} | report.access private |
| N06 | class N06 {N06(){this(67);} N06(int x){this();}} | recursive.ctor.invocation |
| N07 | class N07 {N07(){System.out.println(67);this(67);}N07(int x){}} | call.must.be.first.stmt.in.ctor |
| N08 | class N08Base {N08Base(int x){}} class N08 extends N08Base {} | cant.apply.symbol constructor arg.length.mismatch |
| N09 | abstract class N09 {static void f(){new N09();}} | abstract.cant.be.instantiated |
| N10 | class N10 {N10(){return 67;}} | prob.found.req unexpected.ret.val |
| N11 | class N11 {N11(){this(x);}int x;N11(int x){}} | cant.ref.before.ctor.called |
| N12 | final class N12Base{} class N12 extends N12Base{} | cant.inherit.from.final |
| N13 | class N13Base{final void f(){}}class N13 extends N13Base{void f(){}} | override.meth final |
| N14 | class N14 {static final int x=67; static void f(){x=67;}} | cant.assign.val.to.var static final |

No physical memory experiment; no forced OOME, hostile deserialization, multithreaded race or reflection override. No checked-exception-only superclass probe; normative condition cited, N08 tests absent applicable no-arg constructor. All Java fences contextual positive portions, not verbatim compiling every inline variation. 

## 6. Exact technical source + published CI

Restored complete ClassesAndObjectsDemo.java byte-for-byte. git hash-object returned 2c40dee06634fe46a3e184dbe12b55afb816deb3. javac --release 21 -Xlint:all ClassesAndObjectsDemo.java exit0 no diagnostics; java ClassesAndObjectsDemo and java ClassesAndObjectsDemo.java each exit0, identical actual stdout:
~~~text
first=77, second=1
created=3
CHECK PASSED
~~~
L5–17 explicit check/expectInvalid helpers, no Java assert flag needed. L19–61 main, 10 explicit check calls L25/26/27/28/31/34/39/47/56/57; 3 expectInvalid calls L36–38. Correct independent fields/alias/constructor delegation/shared count, invalid owner/start/deposit, unchanged overdraw and overflow, successful count. L66–101 final CreditAccount: owner stored but owner() not exercised, no test for blank whitespace/null owner or nonpositive withdraw in original runner, covered by additional F02. Counter int not atomic or unbounded, no concurrent evidence. Code differs from article's non-final class and adds constructor/count/accessor; explicitly report, not silently replace. No stdout fixture; source checks actual. [Published push CI #45390717871](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45390717871) historical exact source, raw job #138433360807 reread first=77/created=3 at 12:09:27.2859669Z/.2860411Z, same source step success as frozen A00. No claim new probes ran in CI. Previous audit-only CI #45396249601 success. No Gradle test count.

## 7. A/I/B/C evidence and availability

Direct A retrieval SE21: [JLS §4.3.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.3.1) object includes array; [§8.8.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.7) bare return vs value return; [§8.8.7.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.7.1) explicit invocation, [§8.8.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.9) default form/access/implicit enclosing instance and superclass condition, [§12.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.5) actual five steps, virtual call while incomplete; [§15.12.4.1/4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.4.1) static qualifier evaluated/discarded, abrupt completion, no target. [Object Java21 API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html) summary getClass/clone and all arrays. Chapters 8/12/15 retrieved; other section locators (e.g. §8.1.1.1–2, §8.4.2–3, §8.10.4, §6.6.1, §12.4.1, §15.9.4) supported by JLS rule, tests and linked neighbor; not claim every section reread in full. I local OpenJDK observations only; no pinned HotSpot source needed for language guarantees. B [dev.java More on Classes](https://dev.java/learn/language/oop/classes-objects/more-on-classes/) directly retrieved (this, static, access); current version discusses Java25, don't transplant that change into Java21, or universal physical storage/GC descriptions. Direct overview cache miss, search index recovered its title/summary, not full HTTP verification. 

C fresh retrieval 2026-11-24 of actual questions at pinned revisions previously resolved A01. Four usable sources:
- [teamlead exact README](https://github.com/teamlead/java-interview-questions/blob/4c0784024412904625e1455189d3c05cba07bebf/README.md) L469/471/480–481/568 constructors and abstract, L736/739/782 initialization, L814 copy constructor, L1306–1308 default/copy/private. No frequency statistics.
- [Hruday exact README](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/ced3cf4f91084e9b45366f7a81a70526e7a4be7d/README.md) L148–162,169–172 constructors/return/override/private/chain/copy, L182/184/196 this/super/default parent, L224–225/244/247/249/298–299 final/static. Supports N03/N05/N06.
- [Chernyshov English exact index](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/597e08112ab6a57d6eac8e3c70c070da606dae73/eng/README.md) L43–44 record constructors, L476 constructor/setter/field injection, L577 factory patterns. Adjacent topics, no direct evidence basic default constructor from this sampled excerpt; record/copy/injection deferred where appropriate.
- [Baeldung class structure questions](https://www.baeldung.com/java-classes-initialization-questions), Jan 8 2024, Q1 final, Q3 static, Q4 abstract even without abstract member, Q5 chaining, Q7 static override, Q10 initializer, Q12 singleton. Actual page read relevant sections; do NOT import Q10 conflation of loading and initialization or claim initializer copied into every constructor as normative semantics; Q3 static vs instance field resolution phrasing also imprecise. [Baeldung CORE](https://www.baeldung.com/java-interview-questions) page retrieved but no separate fresh question extraction, not counted as fifth. Historical plan coderboi candidate not independently retrieved in this cycle; UNVERIFIED availability/content, not mandatory authority and no conclusions derived from it. Four direct C sources finite, no coverage guarantee for whole internet.

## 8. Findings register (open until 00)

| ID / severity | Exact location and issue | Evidence / acceptance criterion / proposed role |
|---|---|---|
| JC-A04-N01 P1 (CL01 P0 depth) | L7 defines object only as class instance | JLS §4.3.1 includes arrays; P10 and A03 arrays. Qualify lesson ordinary class instances, add one precise sentence and actual ARRAYS link; no new taxonomy chapter. 02. |
| JC-A04-N02 P1 (CL03 P0 depth) | L118–123 table headings static/instance member, question about access to its fields, static column “Нужна явная ссылка на объект” | F03/F04/F11/N01/N02; JLS §8.3.1.1/8.4.3.2 and §15.12.4.1. Separate “does access to this member need instance?” (static no) from “can static method access instance member?” (yes via explicit available receiver). Count means constructor updates, not live objects/unbounded atomic count; text L85/142 already correct. 02/05. |
| JC-A04-N03 P1 (CL05 P0 depth) | L167–184,298; no condition on implicit super invocation, no scope exception, L149–163 lacks explicit return nuance | JLS §8.8.9 default no declared ctor, implicit super() must be legal, accessible applicable super constructor, checked exception condition; N08 demonstrates compile error. Non-private inner member implicit enclosing formal parameter; record canonical not ordinary default (P08/P09, special types A15/A17); no-args vs default. N10 return expression forbidden, bare return; allowed; void-named method P06 not ctor. Add short worked counterexample and precise boundaries/link ACCESS/INIT/NESTED/RECORDS. 02. |
| JC-A04-N04 P1 (CL07 P0 depth) | L215–240, esp L238 “при создании каждого объекта” overbroad | §12.5, P03 once per class in this chain, P02 virtual dispatch before child initialization, P04 clone distinct with count=1; actual INIT L227/243/263 covers this; SERIAL L107 different reconstruction, no local deserialization run. Scope ordinary new/constructor procedure, per class once (assuming normal execution), do not say all object acquisition triggers init. Explicit static initialization not loading. Link existing neighbors, no unsafe exercise. 02. |
| JC-A04-N05 P1 (CL12) | L260–288 expected abstract class but not addressed | JLS §8.1.1.1, N09, P07; abstract class may have fields/implemented methods/constructor and no abstract methods, direct new forbidden, subclass calls constructor; anonymous concrete subclass distinct. Factory vs abstract not equivalent. Actual INHERITANCE L378. Add one local contrast + link. 02. |
| JC-A04-N06 P1 (CL13) | L69/302/338 only final field, expected method/class | JLS §8.1.1.2/8.4.3.3, N12–14, actual INHERITANCE L256, IMMUTABLE L11. final field assignment vs final instance method override vs final static hiding vs final class subclassing, final method still callable/inherited, not guarantee immutability. Compact table/link, no duplication. 02. |
| JC-A04-N07 P1 | L72/76–81/284 “private” boundary and invariant scope; article class non-final vs demo final | N05 and P08 nest contrast, ACCESS L250; access from permitted enclosing/sibling nested types can be legal. Specify ordinary independent external clients, not absolute lexical/reflection barrier. Invariant for sequential own validated operations, not guarantees about overridable subclass behavior, unchecked publication or concurrency; P05 intentional exception escape. State owner nonblank also invariant, distinguish successful ctor updates from live count. Keep demo useful, no redesign unless 00 decides. 02/05. |
| JC-A04-N08 P2 (CL16) | L287/338–342 link only, missing local getClass/equality contrast; copy constructor C follow-up | Object API and actual OBJECT L26/45/58–60. Add example declared Object vs getClass, identity == versus semantic equals/hashCode link; Java has no magical copy-constructor syntax, ordinary overload can implement chosen shallow/deep policy; not need full OBJECT/EQUALITY copy discussion here. Optional. 02. |
| JC-A04-N09 P2 | L81,287 stale bare INIT/SERIAL future wording; table self-check and source anchors | SERIAL exists frozen L107/56; CORE-CLASSLOADER actual L11 and L179, broader JVM-LOADING still distinct future, don't claim obsolete all JVM. Link directly to published chapters, add §15.12.4.1, §8.8.9, §4.3.1, distinguish examples/actual runner differences. B overview cache miss not proof broken site. 05. |

Severity decision: no new blocker, since direct ordinary examples work, L123 ambiguity contradicted by correct adjacent prose and L238 has ordinary new scope; still four P0 depth gaps, do not ignore at A25. CL12/13 promised minimum absent locally but actual neighbor coverage confirmed. Prior A02-B01 remains open. No remediation issue opened.

## 9. Navigation, pedagogy, limitations, cursor

32/32 relative links article and 6/6 plan resolve on frozen tree 499 entries; three Theory paths exist (types/params/classes). Common example README A01 evidence not repeated. Targeted actual excerpts ACCESS L129/250/270; INHERITANCE L256/378/389; INIT L227/243/263; OBJECT L26/45/58–60; RECORDS L316; IMMUTABLE L11/339; SERIAL L56/107/211; CLASSLOADER L11/179. Not full neighbor review, no HTTP/anchors/Obsidian UI verification. Natural Russian and concrete account invariant with both validation and overflow, motivating example plus counterexamples, no evidence to justify blanket rewrite or infer AI authorship. Improve depth and one misleading table instead of broad duplication.

NOT RUN: clean clone, local Gradle/JUnit, all inline variations, every JVM/OS, javap, full external crawl/anchors/GUI, physical memory/JIT/GC, races/benchmarks, destructive OOME or deserialization, full coderboi candidate; B overview direct cache miss (alternate B retrieved). Local source-file and compiled exact source both executed, contextual Probe and 14 separate negatives actually executed. No external blocker for A04 procedure. C finite sample. 

**A04 COMPLETE**, cumulative 4/41 articles, 4/42 IDs, 52/925 original rows; 1 BLOCKER / 26 P1 / 8 P2 (A01–A04). Next A05 CORE-ACCESS frozen access.md blob dac21a3d4009e590c755354300f191aa4105fa7e L1–421; CORE-ACCESS.md blob ee5bc3765377541b0759eefe5aaacf7bc47de980 L1–167, 20 rows AC-01..20 L60–79 and access table L43–50 separately; examples/core/access (verify.sh blob fb8f2035e857959a6e98c3b7f0df2caca4c1c69d mode 100644, invoked bash). Inspect entire package, read exact CI/negatives; cross-check A04-N03/N07. No source edits.

### Передача эстафеты
Кому: **04 — Independent Reviewer (продолжение аудита)**.
Что сделать: выполнить A05 CORE-ACCESS на frozen source f04f1eb5ee3a400a988757414bcae09113bd885b, access.md L1–421, plan L1–167, 20 original rows plus access table, весь examples/core/access. Восстановить actual refs, проверить A/I/C, код/runner/negative evidence, сохранить отдельный отчёт и обновить индекс. При необходимости промежуточный cursor; не менять учебные материалы, PR #253 Draft.
Передать: оператор пересылает прямую ссылку на опубликованный checkpoint Issue #252.
