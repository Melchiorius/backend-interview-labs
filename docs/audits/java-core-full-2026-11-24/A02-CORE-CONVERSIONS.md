# A02 — CORE-CONVERSIONS: независимый предметный аудит

Issue #252, роль 04, 2026-11-24. Frozen source f04f1eb5ee3a400a988757414bcae09113bd885b; incoming [A01 checkpoint](https://github.com/Melchiorius/backend-interview-labs/issues/252#issuecomment-7131579764), audit parent a022e011bec3fbe61c727e0c770f2bb4e3cc79a3; Draft PR #253. **A02 COMPLETE / CHANGES: 1 BLOCKER, 7 P1, 2 P2.** Статус процедуры не означает PASS материала. Не исправлять учебный текст до решения 00. A01 findings сохраняются.

## 1. Scope и independence

| Frozen file | Blob | Чтение |
|---|---|---|
| [conversions.md](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/knowledge/01-java-core/conversions.md) | a004e1be87a926194033665d2c74d5d859eb7690 | 1–270 полностью |
| [CORE-CONVERSIONS.md](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/docs/plans/CORE-CONVERSIONS.md) | 59aaaee690a12b090769374320dfaf9692a4e445 | 1–23 полностью |
| [ConversionsDemo.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/ConversionsDemo.java) | dae91516b7c666c783d42b89d451346215f10745 | 1–69 полностью |
| [ValuesAndReferencesDemo.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/ValuesAndReferencesDemo.java) | 66502911896d6801fccbdda7ab0b5f49e93316e7 | 1–41 в A01; relevance check здесь, не новый запуск |

История path из commits API на frozen: c8c8142f19f0a3ee64f0543c0fc86b451689963e (initial); ce472839c62f12f843470d8e8b2525d7e5c61bf6 (cache/pool); a1ff65bf1da5b7184da1a26bf7e97c522d3a7114 (rebuild); 86e67b756395373b34cbd9c4c8a7c7435c7d090e, bc22138a9590a24f87191b8c637492c3f763e75f, 97566d1cba493c6eadd2b065d94c4b993eca63e8, b096790bdde1af99b91ee7bbac84721f69993d3f, 3340404a24f4c1175414a873e567ea93a6049c7f (later topic integration). No indication of substantial 04 authorship in commit titles; common login and squash history limit attribution. No earlier PASS counted. A00 old-source→frozen scope does not list conversions.md. A01 cross-check N01/N03/N04/N06/N07 relevant: correct 128 caveat retained, do not duplicate precision/literal findings as new blockers.

## 2. Continuous claims review

All 270 source lines covered. A=JLS/API, I=local or pinned implementation, C=question sample; findings §7. F01–17 refer to local contextual reproduction §4. Links to adjacent articles are targeted, not full review.

| Article L | Claims and inline cases | Judgment |
|---|---|---|
| 1–20 | 67 int; byte assignment vs invocation; conversion contexts; cast can lose value/fail | F01/N01; A §5.2/5.3. Correct opening; N02 extends constant narrowing to boxed target. Definition L19 should distinguish compile-time rejection from runtime conversion failure, N08. |
| 21–32 | table five groups; assignment/invocation/cast/numeric/string | JLS Ch5 counts six contexts, strict/loose separate; table combines them and mentions phases, not missing mechanism. N01 clarify grouping and identity conversion, poly target typing. L28 declarations without init schematic, actual read before DA invalid; not a complete runnable. |
| 33–55 | widening normally implicit; int→long exact, long→double rounded; low 8 bits 300→44; 2^53+1→2^53 | F02/F03; §5.1.2–3. Correct; N03 add complete loss-prone widening set and numeric promotion. No universal precision guarantee (A01-N01). |
| 56–65 | double→int 3.9→3, NaN→0, +∞→MAX; prose generalizes saturation to any integral type | F04 correct; **B01 L64 incorrect for byte/short/char**; JLS §5.1.3 two steps, local B01 counterexample. Negative values truncate toward zero. |
| 66–82 | representable constant byte/short/char/int expression, final primitive constant, ordinary variable int; no narrowing in invocation | F05, N01–03,N08–09,N13. Correct; boxed Byte/Short/Character special assignment, constant variable vs final Integer missing N02. |
| 83–99 | byte+byte int, compound implicit cast and wrap | F06, N04/05/16, P19. Correct for arithmetic; exact rule evaluates LHS once, N03. |
| 100–116 | overflow prior to assignment; 3/2 int; 3/2.0 double; target does not always determine operand type | F07/F08. Correct, distinguish numeric arithmetic from numeric choice/poly, P23/P24; N03. |
| 117–128 | boxing int→Integer, unboxing, null NPE, possible allocation/reuse | F09. A §5.1.7–8; wrappers not interchangeable by identity. No guarantee of allocation count, N04 wrapper map and actual compiler lowering optional. |
| 129–136 | int→Integer→Number allowed; 67L→Long; int→long→Long invalid | F10, N06/N10/N14; P18 narrowing+boxing and unbox+widen; N02. |
| 137–154 | constant-boxed 127 identity guaranteed, 128/1000 unspecified, Integer.valueOf API | F11 defaults observed true:false:false:true and cache max 1000 true:true:true:true. JLS §5.1.7 and API verified; no promise false for 128, A01-N04 neighbor correction; N04 value-based class and complete guarantee limits. |
| 155–170 | Integer/int == numeric, null unboxing NPE vs reference null comparison | F12 correct; P24 conditional unboxing surprise, N03/N04. No general equivalence of equals across wrapper types. |
| 171–194 | strict/loose/variable arity applicability, widening long before boxing, unrelated null overload ambiguity, generic limits | F13/F14,N07/N15; A §15.12.2.1–5. Correct examples; lack actual phase 3/fixed-arity varargs and specificity inside first applicable phase, P20, N05. parameters L164–172 covers array passing not all overload phases. |
| 195–208 | downcast not new object, checked incompatible runtime CCE, instanceof | F15, P25, N11, P18b. A §5.1.5/6,5.5. Upcast only hinted, distinction compile-impossible vs checked/unchecked/null cast missing N06; inheritance L47/51 and patterns L9 give relevant mechanism. L205 future CORE-PATTERNS stale N08. |
| 209–218 | string context vs cast, null conversion | F16, P22. A §5.1.11/15.18.1; reference conversion uses toString (null -> text), not cast. N07 left-to-right and null-toString boundary. |
| 219–234 | literal/folded interning, fresh new String, intern canonical, equals; runtime concat contrast | F17/P22; A §3.10.5/15.18.1/15.29 & String.intern. L233 weak 'not necessarily constant' doesn't give rule for actually nonconstant concat (fresh object semantics), N07. strings L30–49 covers main boundary; link valid. No claim actual allocation count/physical pool layout. |
| 235–247 | 8 interview questions; all addressed locally, Q7 NaN/downcast examples correct but B01 generalization remains | Self-check usable, depth follow-ups N09. Distinguish compiler error from runtime exception. |
| 248–261 | practical source-file mode, SYNTAX/OPERATORS, arrays/strings valueOf(null), CORE-NUMBERS | exact source run §5; all file links exist. numbers already published, stale 'further topic' text L260; patterns stale L205; N08. |
| 262–270 | primary source locators | JLS 5/15/3 and Integer/String pages directly retrieved; add section-level locators for B01/N02/N05, no complete HTTP/anchor crawl. |

## 3. Original G1 matrix — 11/11

Plan contains no original IDs. Audit-local JC-A02-Rnn means original line number, not invented historical IDs. Mixed P0/P1 at L11 kept intact. 6 P0 / 3 P1 / 1 P2 / 1 mixed. Full L1–23 read including promise, negatives and scope. 

| Audit ID | Original P | Question | Article L | Verdict |
|---|---|---|---|---|
| JC-A02-R09 | P0 | assignment/invocation/cast/numeric/string | 17–32,66–81 | OK — five groups combine six formal contexts, N01 |
| JC-A02-R10 | P0 | byte=67 vs takeByte(67), final int vs int | 5–17,66–82 | OK F01/F05,N01–03; N02 extension |
| JC-A02-R11 | P0/P1 | widening/narrowing, 300, long/double, NaN/infinity | 33–65 | ERROR B01; examples correct, overgeneralized rule false |
| JC-A02-R12 | P0 | byte addition, +=, overflow before widening, integer division | 83–116 | DEPTH N03; all original examples correct |
| JC-A02-R13 | P0 | boxing/unboxing/valueOf/cache | 117–154 | DEPTH N02/N04; integer guarantee itself correct |
| JC-A02-R14 | P0 | Integer==Integer vs Integer==int and null | 137–169 | OK F11/F12; N04 extra cases |
| JC-A02-R15 | P0 | overload long/Integer/null/varargs | 171–194 | DEPTH N05; varargs only named, fixed-arity exception missing |
| JC-A02-R16 | P1 | upcast/downcast/CCE | 195–208, linked inheritance | DEPTH N06; correct simple cast, null/compile checks missing |
| JC-A02-R17 | P1 | Long x=67 fails | 129–135 | OK N06 |
| JC-A02-R18 | P1 | String pool/intern/folding vs runtime | 209–234 | DEPTH N07; runtime worked contrast absent locally, neighbor covers |
| JC-A02-R19 | P2 | generic overload/inference/bridge | 192–194 | BOUND explicit generic/erasure and JVM tails |

Totals 4 OK / 5 DEPTH / 1 ERROR / 1 BOUND. No completely missing row. P0 depth R12/13/15. Plan L21 negative 'byte small = small + 1' as written is self-reference if declaration; intended case existing byte plus assignment, isolated N05 and N16 verify actual narrowing failure. Historical plan not edited, N09 clarify in future erratum if coordinator approves.

## 4. Local evidence: every fence, additional boundaries

Local Debian OpenJDK and javac 21.0.12.1, /tmp/jc-a02. Probe.java single compilation unit with separate lexical scopes F01–17, static helper methods at class level for mixed method/statements blocks L9–15/173–188. Added labels/println for declaration-only snippets, runtime NPE/CCE caught to report class; not verbatim standalone compilation or JUnit. javac --release 21 -Xlint:all Probe.java then java Probe, final exit 0. First exploratory execution stopped on CCE at Object(Integer) cast to long, deliberately handled as P18b on final run (not a source-file failure or silently corrected author code). Re-run same probe with -XX:AutoBoxCacheMax=1000 exit 0, selected F11/P21 actual lines below. 17 Java fences + 1 shell.

| Fence | Actual F label/output |
|---|---|
| 9–15 | F01=67; takeByte cast call succeeded |
| 37–44 | F02=67:67.0:44 |
| 48–52 | F03=9007199254740992 |
| 58–62 | F04=3:0:2147483647 |
| 70–79 | F05=67:67:67 |
| 85–94 | F06=67:67 |
| 100–105 | F07=-2147483648:2147483648 |
| 109–113 | F08=1.0:1.5 |
| 119–125 | F09=67:NPE |
| 131–135 | F10=Integer:67 |
| 139–149 | F11=true:false:false:true (default observation, not A for latter identities) |
| 157–165 | F12=true:true:NPE |
| 173–178 | F13=long |
| 182–188 | F14=String (helper returns label to demonstrate selected signature) |
| 197–203 | F15=true:CCE (cast retains identity, wrong cast caught) |
| 211–214 | F16=count=67:value=null |
| 220–231 | F17=true:true:false:true:true |
| 254–256 shell | original byte-identical ConversionsDemo source-file run §5 |

Additional probes (actual expressions inside main, use independent variables; helpers specified):
- B01: cast 300.0 to byte, +Infinity to byte/short, -/+Infinity to char then int, -Infinity to long, -3.9 to int. Actual B01=44:-1:-1:0:65535:-9223372036854775808:-3.
- P18: Byte b=67; Short s=67; Character c=67; long wide=Integer.valueOf(67); Number n=67; Object o=67; (long)(Integer)o. Actual P18=67:67:67:67:Integer:67. P18b: long bad=(long)o; caught CCE (Object containing Integer cast to Long before unbox); actual P18b=CCE:Object(Integer) to long.
- P19: int i=0; int[] arr={67,68}; arr[i++]+=1; actual P19=1:68:68.
- P20 helpers phase(Integer)→Integer, phase(int...)→varargs; fixed(Object)→Object, fixed(Object...)→array or null array; ref(Object)→Object, ref(int)→int. Calls phase(67), phase(67,68), fixed(new Object[0]), fixed((Object[])null), ref(Integer.valueOf(67)). Actual P20=Integer:varargs:array:null array:Object.
- P21: int runtime=127; Integer x=runtime,y=runtime; Integer a128=128,b128=128; compare x==y,a128==b128,Integer.valueOf(127)==Integer.valueOf(127). Actual P21=true:false:true. With cache flag F11=true:true:true:true, P21=true:true:true. Runtime observation on this VM, not guaranteed identity of nonconstant boxing by JLS.
- P22: String part="Ja"; final String constant="Ja"; String r=part+"va"; compare r=="Java", constant+"va"=="Java", r.intern()=="Java"; print 'A'+67+"!", "!"+'A'+67. Actual P22=false:true:true:132!:!A67.
- P23: byte small=67; boolean flag=true; overload type(byte)→byte/type(int)→int; type(flag?small:68), type(small+1), 1<<32L. Actual P23=byte:int:1.
- P24: Integer missing=null; int result=flag?missing:67; caught NPE. P25: Object nullRef=null; String cast=(String)nullRef; cast==null true.

All contextual positive observations completed in one process (27 labeled stdout lines, not 27 independently compiled programs); first run had expected exploratory CCE before subsequent labels. All 17 original Java blocks' positive parts reproduced in context, meaningful inline cases reviewed and sampled, not every possible expression run. No bytecode inspection of javac lowering or JIT allocation test.

### 16 isolated negative compilations

Each separate class Nnn with static test method; helper declarations where required. javac --release 21 -XDrawDiagnostics Nnn.java, all exit 1. Actual categories:

| Case | Statement (helpers) | Diagnostic |
|---|---|---|
| N01 | takeByte(67), takeByte(byte) | cant.apply.symbol / possible.loss.of.precision int,byte |
| N02 | int ordinary=67; byte d=ordinary; | prob.found.req / possible.loss.of.precision |
| N03 | byte e=128; | prob.found.req / possible.loss.of.precision |
| N04 | byte first=40,second=27; byte sum=first+second; | prob.found.req / possible.loss.of.precision |
| N05 | byte small=60; small=small+7; | prob.found.req / possible.loss.of.precision |
| N06 | Long wrong=67; | prob.found.req / inconvertible.types int,Long |
| N07 | accept(null), accept(String)/accept(Integer) | ref.ambiguous |
| N08 | int x=67; Byte b=x; | prob.found.req / inconvertible.types int,Byte |
| N09 | takeByte(67), takeByte(Byte) | cant.apply.symbol / inconvertible.types int,Byte |
| N10 | Integer a=67; Long b=a; | prob.found.req / inconvertible.types Integer,Long |
| N11 | String s=(String)Integer.valueOf(67); | prob.found.req / inconvertible.types Integer,String |
| N12 | boolean b=(boolean)67; | prob.found.req / inconvertible.types int,boolean |
| N13 | final Integer n=67; byte b=n; | prob.found.req / inconvertible.types Integer,byte |
| N14 | int x=67; Long a=(Long)x; | prob.found.req / inconvertible.types int,Long |
| N15 | m(67,67), m(long,Integer)/m(Integer,long) | ref.ambiguous |
| N16 | byte small=60; byte other=small+1; | prob.found.req / possible.loss.of.precision |

## 5. Exact technical source + published CI

Restored ConversionsDemo.java manually from complete fetch, git hash-object returned dae91516b7c666c783d42b89d451346215f10745, exact match. javac --release 21 -Xlint:all ConversionsDemo.java exit 0 no diagnostics; java ConversionsDemo.java exit 0. Actual stdout:

~~~text
cache 127: true
mixed Integer/int: true
overflow: -2147483648, correct: 2147483648
CHECK PASSED
~~~

Source L1–14 paths/check helpers and overload declarations; L15–26 constant narrowing and +=; L28–43 127 identity/value/mixed/unboxing NPE; L45–47 phase1 and disambiguation; L49–56 overflow/division/NaN/+Inf int; L58–62 intern; L64–69 stdout. 12 explicit check calls at L19,25,30,33,34,43,45,51,53,54,55,61. Negative lines comments, not CI compiler tests; no byte/short/char FP boundary test or full phase3 test, no stdout fixture. Historical evidence correct for scope, cannot prove generalization L64. ValuesAndReferencesDemo A01 exact runs/7 stdout lines: primitive copy and alias/reassignment useful prerequisite, does not test overload or conversion specifics. No re-run in A02. 

[Published push CI #45390717871](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45390717871) exact frozen SHA, success 34/34, job 138433360807. Raw log independently read again: 12:09:26.4624732–4662682Z four lines identical to local, Temurin 21.0.13-9, source-file step. A01 audit-only CI #45394430710 now success; not new instructional test. New temporary probes not CI. 

## 6. A/I/B/C evidence

A retrieved 2026-11-24: [JLS Ch5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html), directly read §5.1.2 (exact vs precision-loss widening), §5.1.3 (two-step FP narrowing), §5.1.7 (constant boxing), §5.2 (narrow+box), §5.6 (numeric arithmetic/array/choice); [JLS Ch15](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html), directly read §15.12.2 (phases & fixed arity), §15.26.2 (LHS once), §15.18.1 (fresh nonconstant concatenation), [JLS Ch3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html) page, [Integer API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Integer.html) value-based class and valueOf(int) cache, [String API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#intern()) canonical representation. Other subsection references are normative locators/static knowledge supported by probes, not claim every anchor read. I pinned [Integer.java L996–1069](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/lang/Integer.java#L996-L1069) actual fetched raw, L996 flag, 1016 default high=127, 1018 saved property, 1067–68 valueOf bounds check; local flag experiment confirms observation, not same build provenance or Java-wide policy. B no separate performance/methodology claim under test. 

C new direct retrieval of pinned readmes at revisions confirmed during A01, not ref resolution refreshed this turn; questions paraphrased:
- [teamlead](https://github.com/teamlead/java-interview-questions/blob/4c0784024412904625e1455189d3c05cba07bebf/README.md) L98/146/148 pool, 478/479 wrappers/boxing, 781/1066 autoboxing, 1187 overload, 1299 CCE.
- [Hruday](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/ced3cf4f91084e9b45366f7a81a70526e7a4be7d/README.md) L92–96 casting/promotion, 120–121 boxing, 241–244 overload, 479–488 intern/pool; 635–660 unboxing null/cache/performance, 664–678 parse/valueOf/NumberFormatException, 682–691 mixed comparisons. L653 assumes false at 200, do NOT reproduce; A contradicts universal claim, local flag evidence. Additional wrapper methods and complete wrapper map justify N04; not require API encyclopedia.
- [Chernyshov](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/597e08112ab6a57d6eac8e3c70c070da606dae73/eng/README.md) L59 generic overload (R19 deferred), L263/278/280 pool/intern, L288–289 memory/OOME (JVM boundary).
- [Baeldung CORE](https://www.baeldung.com/java-interview-questions), updated 2024-05-11, beginners Q12 NPE and Q13 casting/CCE/avoid, advanced Q2 binding. Directly retrieved actual headings; use question provenance only, not full answer validation. No claim boxing questions in that specific page. C finite independent sample, not frequency. New B01 arises from A, not a C popularity claim.

## 7. Findings register

Stable IDs; all open until 00. B01 blocks recommendation for final closure; continue other independent audit stages. No new remediation issue.

| ID / severity | Location and issue | Evidence, consequence, acceptance criterion, owner |
|---|---|---|
| JC-A02-B01 BLOCKER, R11 mixed | L64 says all integral destinations clamp to target endpoints | JLS §5.1.3: first int (or long for long), truncate toward zero, NaN zero, saturation at intermediate bound; then narrow int to byte/short/char by discarding high bits. B01 actual 44/-1/-1/0/65535/-9223372036854775808/-3. Existing examples int correct, prose false. 02 fix rule with two-step counterexamples, 03 add deterministic checks if 00 accepts, 04 review. Preserve plan mixed label. |
| JC-A02-N01 P1, R09 | L21–31 five formal contexts wording, target/context distinction L17/115 | JLS intro distinguishes strict and loose invocation (six contexts). Five groups pedagogically fine if explicitly named groups, identity conversion possible, poly expression target type distinct from converting already typed expression; schematic uninitialized byte a,b not compilable as literal standalone. 02 clarify and link GENERICS/FUNCTIONS. |
| JC-A02-N02 P1, R10/13/17 | L68–81/129–135 no complete common composition rules | §5.2/5.3: narrow constant then box Byte/Short/Character in assignment, not invocation; box+widen ref, unbox+widen primitive, no widen primitive+box. P18,N06/N08–10/N13–14. 02 compact allow/deny table; final Integer not constant variable (A01-N06). Explain value-based wrappers not interchangeable numeric classes. |
| JC-A02-N03 P1, R12 P0 depth | L35–55/83–115 only one loss-prone widening and arithmetic examples | §5.1.2 exact except int→float,long→float,long→double; §5.6 promotion double/float/long/int after unboxing in ordinary binary arithmetic, distinct shift and numeric choice; §15.26.2 LHS once P19. P23 byte choice, int addition, shift; P24 unboxing NPE. Add worked cases/precise links OPERATORS/NUMBERS, not duplicate A01 precision. 02. |
| JC-A02-N04 P1, R13 P0 depth/R14 | L117–169 only Integer examples, incomplete other-wrapper guarantees/runtime distinctions | §5.1.7 constant boolean/byte/char/short/int/long small range; runtime variable boxing not universally guaranteed, Integer.valueOf(int) API covers regardless constant; I flag changes observed identities (F11/P21), value-based Integer no synchronization on identity. C Hruday 635–691; add wrapper list, parseInt vs valueOf vs intValue/NumberFormatException short contrast or link (not full parsing chapter), no performance blanket. 02, cross A01-N04/EQUALITY/CONC future. |
| JC-A02-N05 P1, R15 P0 depth | L180/193 varargs named but no worked phase3 or fixed-arity varargs | §15.12.2/2.5 P20 and N15. First phase with applicable method stops; choose most specific in that phase, ambiguity not fallback; varargs declaration may apply as fixed-arity array parameter; ref(Object) wins over unbox to int for Integer arg. Add example of box vs varargs and fixed array/null, contrast unrelated vs related null overload. 02; A03/A06. |
| JC-A02-N06 P1, R16 | L195–208 upcast not explicit, no impossible cast/null or mixed reference→primitive cast | §5.1.5/6/5.5, F15/P25/N11/P18b. Upcast widening ref no new object; cast null ref succeeds (not instanceof true), statically disjoint final types fail compile; Object(Integer) to long compiles but checks Long then fails vs explicit Integer cast+unbox+widen. Generic unchecked deferred. 02 add short table/link actual patterns/inheritance. |
| JC-A02-N07 P1, R18 | L216–233 runtime concat described vaguely, no contrast with left-associated arithmetic | §5.1.11/15.18.1/15.29, P22. Distinguish constant string expression interned vs genuinely runtime concat fresh object semantics, optimizations may eliminate physical allocation. '+' left-to-right, char+int arithmetic before string, null reference converted to text; reference toString not reference cast. strings L30–49 actual existing material; add compact runtime example/link, 02. |
| JC-A02-N08 P2 | L19 compile rejection vs conversion; L205 future CORE-PATTERNS and L260 CORE-NUMBERS stale | patterns.md and numbers.md exist at frozen tree, actual excerpts read. Fix prose and local links, distinguish compile error vs runtime failure. 05. |
| JC-A02-N09 P2 | Plan L21 self-referential negative spelling; L235–246 self-check links, runner limited | Clarify negative as declared byte then assignment (N05/N16), not declaration reading itself; keep original historical plan unless 00 authorizes erratum. 02/05 add worked follow-ups; 03 consider B01 check and P20 in scope; do not pretend all commented negatives in CI. |

## 8. Navigation, teaching quality, limits, cursor

16/16 article relative occurrences and 3/3 plan resolve against exact frozen tree (499 entries, nontruncated); 2 source Theory paths exist; common examples index 53/53 A01 evidence unchanged. Neighbor targeted checks parameters L153,170–172,191–204; operators L87/132/145/240; strings L30–49,166–184; numbers L58/72/85–87; inheritance L47/51/112–118; patterns L7–9. Not full neighbor review, not HTTP or GUI. A01 N04 correct answer here; repair types question later not convert to new cache rule. Natural pedagogical structure, good initial puzzle and compile/runtime separation; B01 undercuts trustworthy generalization, phase3/number promotion should explain mechanism with worked cases, no wholesale rewrite or AI-authorship inference.

NOT RUN: clean clone, local Gradle/JUnit, mixed inline permutations beyond listed, javap lowering, GC/JIT/benchmark/allocation, every JDK/vendor/OS, GUI/HTTP/anchors, generic overload and unsafe casts beyond scope. Actual one source-file and one contextual Probe with 17 F cases + B01 + P18–25/P18b, second flag run and 16 isolated negatives, not independent positives for each fence. Initial exploratory CCE reported; final run success. A02 complete with unresolved B01 and recommendations. 

Cumulative 2/41 articles, 2/42 curricular IDs, 23/925 original rows; 1 BLOCKER, 14 P1, 4 P2 (A01+A02). Next A03 CORE-PARAMS: frozen parameters.md blob 2f196462d910f9f0f31be1ecadecc7035220eb1a, L1–272; CORE-PARAMS.md blob 6b5be5c3c3313c2b0781b706fea3fd3de3856de0, L1–24, matrix L9–20 (12 rows, no IDs); ParametersDemo.java and shared Values evidence A01. No automatic remediation, Concurrency not started.

### Передача эстафеты
Кому: **04 — Independent Reviewer (продолжение аудита)**.
Что сделать: выполнить A03 CORE-PARAMS на frozen source f04f1eb5ee3a400a988757414bcae09113bd885b, начать parameters.md L1, проверить весь план/матрицу, примеры, A/I/C, локальные probes и сохранить findings, включая cross-check A02-N05. Сохранить новый отчёт и обновить индекс; PR #253 Draft, исходники не менять.
Передать: оператор пересылает прямую ссылку на опубликованный checkpoint Issue #252.
