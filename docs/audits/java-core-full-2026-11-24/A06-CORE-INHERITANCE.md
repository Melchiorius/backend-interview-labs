# A06 — CORE-INHERITANCE: полный независимый аудит

Issue #252, роль 04, 2026-11-24. Frozen source f04f1eb5ee3a400a988757414bcae09113bd885b; [incoming A05](https://github.com/Melchiorius/backend-interview-labs/issues/252#issuecomment-7132152551); audit parent 76c98768675a54e8224ebd560113fe6de46d5bd7; Draft PR #253. **A06 COMPLETE / CHANGES: 1 new BLOCKER, 8 P1, 2 P2.** A02-B01 and A05-B01 remain open. No instructional edits or remediation until 00 decides after A25.

## 1. Scope/provenance

| Frozen file | Exact blob | Read |
|---|---|---|
| [inheritance.md](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/knowledge/01-java-core/inheritance.md) | 51f7138a528a0500c9d7bb0eb22276dc93821cdd | 519/519 lines; 1–229,230–519; tool output initially truncated middle, reread 182–356 to close gap |
| [CORE-INHERITANCE.md](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/docs/plans/CORE-INHERITANCE.md) | 43d55dd8d663ebd8a1d63222482a904479e640cb | 200/200 lines, 25 original rows L63–87, separate normative table |
| [CORE-INHERITANCE_POST_DRAFT_AUDIT.md](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/docs/plans/CORE-INHERITANCE_POST_DRAFT_AUDIT.md) | 11a3b9ad0f55294ce7938ad5e3bb488211f29f4f | 80/80 lines, separate 02 retrospective claims for same IDs |
| examples/core/inheritance | see §5 | 17/17 blobs; 3 positive Java, 12 negative Java, README and runner |

Article history at frozen: 86e67b756395373b34cbd9c4c8a7c7435c7d090e publication #43, 261f195a199f61e16abed5b0eaf16605840907b3 INIT, 4680b057aeabcd3e4d36e15b13b4db0d032c944c OBJECT/EQUALITY, 7f4b83b1dfc6f3871389faa2b70f663b3bbb71b5 INTERFACES/COMPOSITION, bc22138a9590a24f87191b8c637492c3f763e75f GENERICS/ERASURE, 64dfbabdc76995ae289a7cd5f8ea5d917b757564 EXCEPTIONS/RESOURCES, 69d61c427e7efcab89a55652830050ef16041c8e SEALED/PATTERNS. No substantive 04 authorship indication from path history titles; common Git identity and squash not exhaustive attribution. Prior G1/G4 PASS and 01/02 tests historical, not new evidence. A05-B01 about accessible inherited public members remains separate; article L37 already mentions inheritance restrictions.

## 2. Continuous claim review — all 519 lines

A normative JLS/API; I actual observed compiler/runtime. §6 probes. Neighbor source excerpts not full audits.

| Lines | Claim / example | Judgment |
|---|---|---|
| 1–16 | subtype, contract, three-stage method/field distinction | correct framing; distinguish ordinary static/instance constraints from independent declarations B01. |
| 17–40 | one direct superclass, interfaces, Object no superclass, private/package and ctor noninheritance | positive Report probe. L35 explicitly ordinary class (enum/record separate). L37 class members restrictions correct but local minimum distinguish inherited member vs accessibility, transitive member types and inherited public members from inaccessible declaring type A05-B01/N01. Constructors not inherited. |
| 41–52 | declared vs actual type, upcast no copy, downcast/child-only access | Probe upcast=true; child_only negative. L51 needs null cast vs NPE and runtime checked incompatible cast, not blanket “object must be compatible”; N02. |
| 53–115 | standalone Dispatch, six lines, two stages, super/cast/fields | complete exact textual Java block (excluding fence) compiled/run, output matches all six lines; no claim blob since embedded. Static search and runtime implementation for selected virtual signature correct, poly/inference deeper CONVERSIONS. |
| 116–131 | overload vs override, return-only negative, subsignature | correct basic examples, but L128 says corresponding inherited method rather than overridden declaration, inherited implementing method can override from subclass. L130 signature definition imprecise (not return type), N03. |
| 132–154 | class method hiding, Base.kind via ref, static/instance signature conflict | main examples and both original negatives correct. **L145 false generalization**: independent same-signature declaration across private/package inaccessible boundary allowed; B01 tested. L153 explicitly accessible instance method, but does not fix L145/IH06 categorical rule. N05 hiding return/access/throws restrictions. |
| 155–173 | fields resolved statically, distinct instance fields in one object, mixed static/instance allowed and types differ | Dispatch actual, Probe fields=67:68:67:sub. No universal static storage claim; field hiding distinct from lexical shadowing. |
| 174–185 | super method vs cast, field, static context forbidden, super ctor distinct | exact Dispatch output and super_static negative. Ordinary class syntax correct; qualified outer/interface super detailed elsewhere. |
| 186–229 | private nonoverride; two-package direct package example | original runner verifies base vs child labels. Correct **direct** examples, not all transitive cases; intermediate same-package override and return-to-package actual probes child, N04. Do not repeat plan IH09/L53 unqualified negative. |
| 230–251 | @Override detects mismatch, not runtime switch; new overload without annotation valid | original N06; JLS §9.6.4.4 special record accessor and public Object signature allowed, L232 blanket rule incomplete, N06. API Override page has two general conditions, JLS adds record. |
| 252–300 | final class/method/static, access narrowing | original three final negatives, access negative, Probe Good. Correct when actual override/hide; private final independence B01. N05 extend access table to hiding and public/package. |
| 301–327 | covariant Number→Integer, compile-time return type, primitive no covariance | actual runner and Probe covariant=Integer:68:67; wider return and primitive result negatives. Correct ordinary non-generic rule, deeper unchecked/erasure deferred. N05 note applies to static hiding too. |
| 328–356 | checked throws narrower/none vs broader, unchecked, caller declared checked exceptions | actual runner, wider_throws/checked_caller negative; Probe unchecked accepted. Rule applies hiding as well, N05. For each new checked type at least one allowed supertype in parent; generic erasure caveat deferred. |
| 357–375 | override one signature, add overload, String/Integer null ambiguity | actual null_ambiguity rejection, explicit cast result String. Existing CONVERSIONS link, no duplicate full overload algorithm. |
| 376–402 | abstract class/constructor, abstract final/private abstract, concrete class vs default and default conflict | actual Probe abstract/default=67:class, abstract_final/private_abstract/default_conflict rejections. L389 explanation abstract need not have abstract methods; abstract superclass method can suppress default without implementation, N07. Neighbor INTERFACES L178–184 explicitly handles. |
| 403–424 | virtual call before subclass field initialized | Probe early=0, matches article; INIT L235/562 confirms. No concurrency/physical layout claim. |
| 425–471 | LSP vs type validity, SilentStore violation and OptionalSaver composition, fragile base hook | Probe lsp=0:false:true:1 for valid delegate; bad-delegate=true:0 when passing SilentStore. L462 promise only if nonnull conforming delegate; composition not proof of contract, N08. Existing COMPOSITION L128/134/138. |
| 472–494 | isolated negatives and runnable scope | exact runner verifies 12 stored negatives; other listed negatives (abstract final, multiple extends, default conflict, super static, null ambiguity) separately executed audit probes, not historical package cases. Actual CI read, N09 minor coverage label. |
| 495–511 | reasoning strategy, adjacent topics, vtable/JIT not JLS guarantee | sound, link A05-B01 for inaccessible declaring types, N06 special annotation. P2 deferred justified; existing erasure L132–161 actual excerpts. |
| 512–519 | primary sources and caveat | normative JLS8/9 directly retrieved relevant sections; additional locators N10, no exhaustive link crawler. |

## 3. Original matrix 25/25, IH-01..25

14 P0 / 7 P1 / 4 P2; retain original IDs. New assessment, not historical rewriting.

| ID | P | Plan L | Article L | Verdict |
|---|---|---|---|---|
| IH-01 | P0 | 63 | 17–40 | DEPTH N01: inheritance/access/declaring type distinctions |
| IH-02 | P0 | 64 | 41–52 | DEPTH N02 cast null and call distinction |
| IH-03 | P0 | 65 | 116–130 | DEPTH N03 precise signature/subsignature and inherited-vs-overridden |
| IH-04 | P0 | 66 | 53–115 | OK exact Dispatch runtime |
| IH-05 | P0 | 67 | 110–114,359–374 | OK compile-time overload vs override |
| IH-06 | P0 | 68 | 132–154 | ERROR B01 unqualified static/instance conflict |
| IH-07 | P0 | 69 | 155–173 | OK field hiding both directions/types |
| IH-08 | P0 | 70 | 174–185 | OK super vs cast, static negative |
| IH-09 | P0 | 71 | 186–229 | DEPTH N04: direct case correct, transitive/package return missing |
| IH-10 | P0 | 72 | 252–279 | OK final restriction scoped to actual override/hide; B01 cross |
| IH-11 | P0 | 73 | 281–300 | DEPTH N05 access table/hiding |
| IH-12 | P0 | 74 | 301–327 | OK ordinary covariance and primitive boundary |
| IH-13 | P0 | 75 | 328–356 | DEPTH N05 checked set and static hiding restrictions |
| IH-14 | P0 | 76 | 230–251 | DEPTH N06 @Override special cases |
| IH-15 | P1 | 77 | 376–389 | DEPTH N07 abstract need not contain abstract methods |
| IH-16 | P1 | 78 | 357–374 | OK overload coexistence and null |
| IH-17 | P1 | 79 | 186–205 | OK private base dispatch |
| IH-18 | P1 | 80 | 403–424 | OK early=0, INIT actual |
| IH-19 | P1 | 81 | 425–471 | DEPTH N08 conforming delegate premise |
| IH-20 | P1 | 82 | 17–35,391–402 | DEPTH N07 abstract class vs default; direct conflict correct |
| IH-21 | P1 | 83 | 145–154,252–279 | ERROR same B01 (not second blocker) |
| IH-22 | P2 | 84 | 130,506,510 | BOUND explicit GENERICS/ERASURE |
| IH-23 | P2 | 85 | 508–510 | BOUND explicit SEALED/RECORDS |
| IH-24 | P2 | 86 | 279,510 | BOUND JLS vs JVM/JIT, no performance evidence |
| IH-25 | P2 | 87 | 504,510 | BOUND OBJECT/EQUALITY/framework |

Totals **9 OK / 10 DEPTH / 2 ERROR / 4 BOUND**. P0 6 OK/7 DEPTH/1 ERROR, P1 3 OK/3 DEPTH/1 ERROR, P2 4 BOUND. IH06 and IH21 same defect B01. No entirely missing original row. No claim final content PASS.

## 4. Historical plan table + separate post-draft audit

Plan L47–53 five comparison rows: compile-time/virtual L49 correct (instance virtual condition), static L50 correct, fields L51 correct, super L52 correct, L53 package-private negative only direct case, not universal (N04). Plan L63–87 assessed above, L95–125 two code blocks match Dispatch. Plan L135–162 original R01–17/N01–18 and 35 local results belong to 01, not newly run in this audit. L166–200 scope/historical gates read; preserve history.

Post-draft 80/80 lines: 14 P0 rows and 7 P1 rows + four P2 bounds refer to same 25 IDs, not new requirements. New cross-check of all claims:
| Post-draft ID(s) | Prior claim | New evidence |
|---|---|---|
| IH01 | single class, private/package/ctor | correct ordinary class; N01 |
| IH02 | upcast/static type | correct, null distinction N02 |
| IH03 | signature/subsignature/return-only | examples correct, N03 |
| IH04–05 | runtime override and new overload | exact Dispatch, OK both |
| IH06 | static hiding + static/instance conflict | positives and negatives correct; B01 missing applicability |
| IH07 | fields mixed/static hiding | OK |
| IH08 | super/cast distinction | OK |
| IH09 | private/direct package no override | correct direct, N04 transitive |
| IH10 | final rules | OK, cross B01 independent methods |
| IH11 | access | correct instance example, N05 hiding |
| IH12 | return | OK |
| IH13 | checked/unchecked | correct basic, N05 |
| IH14 | @Override | N06 exceptions absent |
| IH15 | abstract | N07, concrete class implementation |
| IH16–18 | null ambiguity, private, early init | OK each |
| IH19 | LSP and composition | N08 conforming delegate premise |
| IH20 | class/interface defaults | N07 abstract superclass suppression |
| IH21 | invalid combinations | B01 same defect |
| IH22–25 | deferred boundaries | BOUND all four |

Author's “no P0 gaps in considered C sample” not exhaustive guarantee; new normative gap B01 outside prior positives. Seven positive and twelve negative temporary 02 cases described historically; not counted as our tests. Original post-draft external pages Baeldung structure, GFG, Baeldung type system not fetched afresh here, no new evidence claimed from them; fresh nonplan Hruday sample §7. Gate history preserved.

## 5. Complete practical package

| [README.md](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/inheritance/README.md) | 7322d67c7cabfb79ee1ca0fd34cde47e8294e752 | 100644 | read; not restored for execution |
| [negative/BadCheckedThrows.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/inheritance/negative/BadCheckedThrows.java) | 69bda17cf4ce085c77f23759abe2d8825f02dca7 | 100644 | read; exact local hash matched |
| [negative/BadCovariantReturn.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/inheritance/negative/BadCovariantReturn.java) | eef2b0177fb8f3c6a1abbdaa1fb36535d34d87ba | 100644 | read; exact local hash matched |
| [negative/BadExtendsFinal.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/inheritance/negative/BadExtendsFinal.java) | 486bc1fa26157b056ff14d87a2388d48ada2b23d | 100644 | read; exact local hash matched |
| [negative/BadHideFinalStatic.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/inheritance/negative/BadHideFinalStatic.java) | c71de2b8463fdd303d26c7e36156afa62021f501 | 100644 | read; exact local hash matched |
| [negative/BadInstanceForStatic.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/inheritance/negative/BadInstanceForStatic.java) | 7b628c23ea80b2ea17e1ed2ca8ff062e6b482527 | 100644 | read; exact local hash matched |
| [negative/BadNarrowAccess.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/inheritance/negative/BadNarrowAccess.java) | bdf8ecc9701aa8e6f23c8c758034d1ae92188693 | 100644 | read; exact local hash matched |
| [negative/BadOverrideFinal.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/inheritance/negative/BadOverrideFinal.java) | 9ada12f2671426f310663c1da78a60e63d9dc825 | 100644 | read; exact local hash matched |
| [negative/BadOverrideOverload.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/inheritance/negative/BadOverrideOverload.java) | 73c8636d3fec992f17c625f278dd84fb69506e28 | 100644 | read; exact local hash matched |
| [negative/BadOverridePackage.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/inheritance/negative/BadOverridePackage.java) | 17304723c1f91fbcd8dd52e46e93e13ea21db60b | 100644 | read; exact local hash matched |
| [negative/BadOverridePrivate.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/inheritance/negative/BadOverridePrivate.java) | b79bf3eb41846b3ab9eb02f090c17b087d9dfbfa | 100644 | read; exact local hash matched |
| [negative/BadReturnOnlyOverload.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/inheritance/negative/BadReturnOnlyOverload.java) | 6d94397f342fce767ec1b005fa433f9dd15dcfca | 100644 | read; exact local hash matched |
| [negative/BadStaticForInstance.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/inheritance/negative/BadStaticForInstance.java) | 95d69581bc1743097102d5a849bf69dacad075db | 100644 | read; exact local hash matched |
| [src/base/Base.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/inheritance/src/base/Base.java) | 5f7083d77810f75f58fa87e9abdbd7a70fdc0901 | 100644 | read; exact local hash matched |
| [src/client/Derived.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/inheritance/src/client/Derived.java) | a463ccc213507709d861a8503e7b5ae306b75440 | 100644 | read; exact local hash matched |
| [src/client/InheritanceDemo.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/inheritance/src/client/InheritanceDemo.java) | fdc482c341436a13c944f90c205d22d225ba0f82 | 100644 | read; exact local hash matched |
| [verify.sh](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/inheritance/verify.sh) | d545ff60f4ee1b440b615a6e64941bfbc8da238a | 100644 | read; exact local hash matched |

Actual local Debian OpenJDK/javac 21.0.12.1; reconstructed /tmp/jc-a06/exact/examples/core/inheritance from fetched bytes. 15 Java files + runner: **16/16 git hash-object match**, runner hash d545ff60f4ee1b440b615a6e64941bfbc8da238a, mode in Git 100644, bash invocation; README read but not restored. bash examples/core/inheritance/verify.sh from reconstructed root exit0. No physical expected.stdout fixture, README has expected eight lines. Actual stdout first eight:
~~~text
Base reference + String argument: derived:object
Derived reference + String argument: derived:string
super vs cast: base:object / derived:object
static method: base:static / derived:static
field: 67 / 68 / 67
covariant + throws: 68 / derived:read
private/package: base:private / base:package
CHECK PASSED
~~~
Then all 12 EXPECTED REJECTION and final ALL EXPECTED OUTCOMES OBSERVED. Positive source 16 explicit check calls, no -ea requirement. Base fields/returns/throws and private/package calls; Derived final, override/overload/covariance, super/cast/protected, independent names; main proves 16 conditions. The check labeled inherited final static calls Base.frozenKind() (not Derived.frozenKind()), N09 evidence wording; actual inherited final instance tested via Base ref with Derived object. Runner checks error: and specific English substring; documents locale limitations. BadCovariantReturn emits two errors, intended return incompatibility present. No exact stdout automated diff here, actual output agrees README. 12 isolated negatives each correctly rejected:

| Negative | Actual line and reason |
|---|---|
| BadExtendsFinal | 6 cannot inherit from final String |
| BadOverrideFinal | 7 overridden method is final |
| BadHideFinalStatic | 7 overridden method is static,final |
| BadStaticForInstance | 7 overriding method is static |
| BadInstanceForStatic | 7 overridden method is static |
| BadOverrideOverload | 7 method does not override or implement |
| BadCovariantReturn | 7 return type Object incompatible Number; additional annotation error |
| BadCheckedThrows | 7 overridden method does not throw Exception |
| BadNarrowAccess | 7 weaker access; protected |
| BadOverridePrivate | 7 method does not override or implement |
| BadOverridePackage | 7 method does not override or implement |
| BadReturnOnlyOverload | 8 method convert(Object) already defined |

[Published exact frozen push CI #45390717871](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45390717871), job 138433360807 raw log reread 12:09:31.0920459Z–12:09:36.5551628Z: same eight positive lines and all twelve actual diagnostics + final marker, Temurin21.0.13-9. A05 audit CI #45398507573 success. No new probes run in CI; no JUnit/Gradle count. No full clone. Positive classes not asserted all methods or all article fences.

## 6. New actual local probes and fence map

/tmp/jc-a06/probes.py generated **20 distinct isolated compilation directories** (5 positive, 15 negative). javac --release 21 -XDrawDiagnostics -d <out> <sources>; successful java -cp <out> Main. All positive compile exit0/run exit0, 15 negatives exit1 with expected diagnostics. Separate from 16 exact package hashes and original runner. Dispatch copied complete text of L58–89, actual six lines:
~~~text
derived:object
derived:string
base:static/derived:static
67/68
base:object/derived:object
67
~~~

Other four positive compilation units:
- transitive: api.Base package internal() and public run(){return internal();}; api.Mid extends Base @Override public internal()->mid; client.Child extends Mid @Override public internal()->child; new Child().run() stdout child. JLS §8.4.8.1, N04.
- return_to_package: api.Base as above; other.Mid public internal()->mid not overriding inaccessible Base; api.Child extends other.Mid @Override public internal()->child; run stdout child. Confirms same-package return distinction, not universal propagation claim.
- foreign_static: api.Base package static final int m(){return 67;} public int run(){return m();}; client.Child public int m(){return 68;}; stdout 67:68. **B01**.
- Probe.java nested contextual classes, 14 labeled stdout lines. Probe includes Document/Report implements two interfaces; same null/incorrect casts; mixed fields two directions; private methods; private final static→instance and private final instance→static independent declarations; protected override expansion; covariant/unchecked; null overload cast; abstract/default; early init; LSP + valid/invalid delegate; record R explicit accessor @Override; interface Hash @Override hashCode declaration compiles. Actual:
~~~text
upcast=true
cast-null=true
cast-null-call=NPE
cast-wrong=CCE
fields=67:68:67:sub
private=base-private:child-public
private-final=67:68:67:68
covariant=Integer:68:67
null-overload=String
abstract/default=67:class
early=0
lsp=0:false:true:1
bad-delegate=true:0
record-override=67
~~~
The list has **14** lines (the label count, not independent test count); all shown. private-final proves B01 both directions, parent call=67 vs child method=68. OptionalSaver(silent) returns true without storing, N08. Record R(67).x returns 67; Hash declaration legal. Contextual wrappers add observation prints, not literal all article code units. No javap run. 

| Independent negative | Actual category (compiler.err.) |
|---|---|
| null_ambiguity String/Integer m(null) | ref.ambiguous |
| abstract_final | illegal.combination.of.modifiers abstract,final |
| multiple_extends | expected '{' |
| default_conflict unrelated defaults | types.incompatible / incompatible.unrelated.defaults |
| super_static | non-static.cant.be.ref super |
| primitive_result int→long | override.incompatible.ret |
| wider_throws FileNotFoundException→IOException | override.meth.doesnt.throw |
| static_throws same widening in hiding | override.meth.doesnt.throw |
| static_access protected→package | override.weaker.access |
| static_result Number→Object | override.incompatible.ret / cant.hide |
| abstract_beats_default concrete class inherits abstract superclass m + interface default | does.not.override.abstract |
| private_abstract | illegal.combination.of.modifiers abstract,private |
| interface_clone_override @Override Object clone(); | method.does.not.override.superclass |
| child_only A ref, B-only method | cant.resolve.location.args |
| checked_caller A ref, IOException not caught though B override no throws | unreported.exception.need.to.catch.or.throw |

27 Java fences plus one text output and two shell blocks (30 total): all read and mapped, not all verbatim separately compiled. 21–31 Report contextual; 43–45 upcast; 57–90 exact Dispatch; 120–126 return-only exact runner; 138–141 Dispatch static output; 147–151 runner static/instance; 161–168 Probe mixed fields; 190–203 Probe private; 209–224 runner package contrast and transitive probes; 234–239 exact runner correct override (equivalent types); 243–248 BadOverrideOverload; 260–277 three final negatives; 285–299 Probe Good and BadNarrowAccess; 305–324 runner/Probe covariance and BadCovariantReturn; 332–353 Probe and wider_throws; 361–370 Printer/null; 380–387 Formatter; 393–399 default class precedence; 407–419 early=0; 431–466 SilentStore/OptionalSaver. Commands L105–108 compile/run Dispatch and L489–491 exact bash runner. The source command for Dispatch used -d and -XDrawDiagnostics, equivalent not verbatim. Read README PowerShell commands; not run PowerShell. Plan's two code fences covered by equivalent Dispatch. No claim every inline permutation.

## 7. A/I/B/C evidence

A directly read [JLS SE21 §8.4.8.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.1), explicit package override chains and inherited implementing method; [§8.4.8.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.2) accessible and subsignature criteria for hiding; [§8.4.8.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.3) compatibility and private independence; [§8.4.3.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.3.3) final, [§9.6.4.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4.4) actual three annotation conditions, [Override API 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Override.html) two general conditions, JLS additional record rule. Other normative locators §8.1.1/8.1.4/8.2/8.3/8.4.2/8.4.5/8.4.9/15.11/15.12/12.5 used with existing article citations and observed evidence; no assertion all sections newly retrieved exhaustively this turn. I observed compiler/runtime and raw CI, no JVM-specific vtable/bridge claim. B dev.java historical explanation not freshly fetched, not relied on.

C fresh exact raw readmes, pinned revisions prior stages (not current default ref refresh):
- [in28](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/9ba8736f44fdc175dac5046b7d55c94fd74416cd/readme.md) L66–68 inheritance/overload/override, L70 multiple class inheritance, L91 polymorphism.
- [teamlead](https://github.com/teamlead/java-interview-questions/blob/4c0784024412904625e1455189d3c05cba07bebf/README.md) L126/181 static override, L132/182 multiple inheritance, L470 dynamic polymorphism, L481 method/constructor, L655/729 static and overload, L984/1064 composition; L1187 overload.
- [Chernyshov](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/597e08112ab6a57d6eac8e3c70c070da606dae73/eng/README.md) L59/61 generic overload/inheritance, L103–104 LSP, L108–109 composition, L262 getter override, L338–339 equals/hashCode boundaries, L582 decorator. No frequency inference.
- [Hruday](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/ced3cf4f91084e9b45366f7a81a70526e7a4be7d/README.md) **nonplan** C, L55 multiple classes, L153/154 ctor, L172 constructor calls override, L215–232 inheritance/composition, L237–248 polymorphism/static/private/return-only. Questions support relevance, not answers as authority.
Four actual question banks; historical post-draft Baeldung/GFG pages not re-fetched, not counted, no frequency. B01/N04/N06 arise from normative review and probes.

## 8. Findings registry (all open pending 00)

| ID / severity | Exact locus | Evidence / acceptance criterion / proposed role |
|---|---|---|
| JC-A06-B01 BLOCKER (IH06 P0; IH21 P1 same defect) | L145 and plan L68/83; unqualified static/instance override-equivalent signature conflict | JLS §8.4.8.1–3 requires actual override/hide relation; private method not overridden/hidden in technical sense. Probe private-final=67:68:67:68 and foreign_static=67:68 compile/run. Existing accessible-parent negatives remain correct. State prohibition only where actual overriding/hiding relation or other applicable declaration constraint exists; same-signature independent methods across private and foreign package boundaries legal. Include worked positive contrast, preserve final/return/access/throws constraints. 02, 03 optional regression per 00, 04 verify. |
| JC-A06-N01 P1 (IH01 P0 depth) | L19–39 member inheritance/access shorthand | Distinguish inherited member vs lexical accessibility vs subclass state; private field remains in object state but not inherited member, constructors not inherited; public inherited members from inaccessible declaring type (A05-B01) valid, no duplicate full ACCESS article. Explicit ordinary class scope (enum/record separate). JLS §8.2, cross A04/A05. 02. |
| JC-A06-N02 P1 (IH02 P0 depth) | L51 downcast described only compatible actual object | Actual cast-null=true, subsequent call NPE, incompatible actual object CCE, child_only compile error. Distinguish permitted cast at compile time, null reference cast always null for reference target, runtime check and dereference; statically impossible casts rejected. Link A02-N06. 02. |
| JC-A06-N03 P1 (IH03 P0 depth) | L118/128/130 “соответствующего наследуемому методу”; signature description | JLS §8.4.2/8.4.8.1: name/type parameters/formal parameter types after adaptation, return/throws not part of signature; subsignature includes erasure case. Override is relation to ancestor declaration, overridden method not inherited by C, inherited concrete method may implement interface from C. Keep ordinary examples; explicit generic boundary and ERASURE link, no full duplication. 02. |
| JC-A06-N04 P1 (IH09 P0 depth) | L207–229 direct case, plan L53/71 categorical “нет” | Direct example correct; actual transitive and return_to_package both child. JLS §8.4.8.1 same-package/chain conditions. Add 3-class two-package example, explain why original direct case differs, don't equate noninheritance with no possible transitive override. Historical plan erratum only if 00 authorizes; cross A05-N04. 02. |
| JC-A06-N05 P1 (IH11/13 P0 depth) | L281–300,328–355 and static section | §8.4.8.3 compatibility applies to hiding too: public must public, protected→protected/public, package cannot private; checked type each permitted by parent throws, no blanket same-count rule; return substitutability also static, primitive identical. Actual static_throws/static_access/static_result reject, checked_caller reject; ordinary non-generic scope, erasure warnings linked. No full exception duplication. 02. |
| JC-A06-N06 P1 (IH14 P0 depth) | L232/250 blanket @Override requires override/implementation | JLS §9.6.4.4 additional public Object signature and record component accessor conditions; record R explicit @Override compiles and 67, interface Hash @Override hashCode legal, clone interface @Override fails. Add concise Java21 exceptions and source link, mention SOURCE retention (API) without claiming runtime switch. Cross RECORDS/INTERFACES. 02. |
| JC-A06-N07 P1 (IH15/20) | L389 abstract final explanation; L391–401 class vs default | abstract class may contain no abstract methods; restriction is direct instantiation, not necessarily incomplete implementation. Concrete class method priority correct; abstract superclass can suppress default without providing body, actual abstract_beats_default rejects. Existing INTERFACES L178–184 covers, add one contrast/link. 02. |
| JC-A06-N08 P1 (IH19) | L452–470 OptionalSaver delegate and composition | Valid delegate actual lsp=0:false:true:1; OptionalSaver(silent).trySave true yet silent.count=0. Composition does not repair broken behavioral contract of delegate; require nonnull contract-conforming delegate, clarify caller premise and null check if appropriate. Fragile superclass risk example sound, do not guarantee composition removes all coupling. COMPOSITION L128/134/138. 02. |
| JC-A06-N09 P2 | InheritanceDemo final static check and README verification | Calls Base.frozenKind, not inherited access through Derived; label overstates that specific check; optional use Derived.frozenKind or change label. 16 actual checks and 12 diagnostic cases are useful; do not count omitted article negatives as CI cases; optional add B01 regression if 00 authorizes. 03/05. |
| JC-A06-N10 P2 | article L485/493 old integration-branch wording, README intro and last paragraph future CORE-INIT, source anchors | Published frozen material, current exact CI actual. Update present-tense evidence without rewriting historical plan/post-draft; link INIT/COMPOSITION/INTERFACES, add §9.6.4.4 and §8.4.8.1–2 precise locators. 05. |

Severity: B01 one blocker affecting two rows, true examples don't rescue false general rule P0; N04 direct article example true while historical plan generalization needs caveat, N06 explicit special-case gap in ordinary-class article with deferred records. A02/A05 blockers unchanged. No remediation until 00. 

## 9. Navigation, limits, next cursor

39/39 article, 11/11 plan, 2/2 post-draft, 22/22 README relative file targets resolve against exact frozen 499 entries, nontruncated tree. Total 74/74 occurrences, no anchor/HTTP guarantee. All Theory/Research source comments visibly reference existing articles/plan. Targeted actual neighbor excerpts: RECORDS L11/48/182–186; INTERFACES L9/174/178–184; COMPOSITION L11/128/134/138/142; ERASURE L132–161; INIT L235/560–562. No full neighbor audits. Main Dispatch natural Russian, excellent staged explanation; revise false premise and add bounded counterexamples rather than rewrite. No AI-authorship inference.

NOT RUN full clean clone/Gradle/JUnit, all inline variants separately, all alternative JVM/OS, javap/bytecode/bridge/JIT/vtable measurements, races/GC/benchmark, GUI/PowerShell/Obsidian/anchors/exhaustive HTTP, historical Baeldung/GFG/dev.java refresh. Actual 16 exact local hashes, exact runner, five positive+15 negative independent compiles with actual outputs. Initial first article retrieval truncation corrected by reread; no lost range. No external execution blocker. 

**A06 COMPLETE**, cumulative 6/41 articles, 6/42 curricular IDs, 97/925 original matrix rows; 3 BLOCKER / 40 P1 / 12 P2. A07 CORE-INIT next: initialization.md blob 6a59e971c8501084089e13881d6ca7e19661708f L1–885, chunks 1–248/249–442/443–662/663–885; CORE-INIT.md blob 80a428a25305aa5f09844c6254a004945b404227 L1–276, CI-01..20 L120–139; entire examples/core/initialization (4 blobs: README 22c3ec516a4a35af14f4da55997726080a96f791, src/InitializationDemo.java 89777df4cf02018c0cf2381bbf0ca01f54efeb49, expected.stdout 53a990195e07dde9eca9a250bef2e1017f71c8da, verify.sh a6af428dcee3ff7e4c7c794ce080055ab7f76ee9 mode100644). Cross-check A04-N03/N04 and A06 IH18. For this larger article use exact partial checkpoint if necessary, don't mark COMPLETE before all stages. No source edits.

### Передача эстафеты
Кому: **04 — Independent Reviewer (продолжение аудита)**.
Что сделать: выполнить A07 CORE-INIT на frozen source f04f1eb5ee3a400a988757414bcae09113bd885b, initialization.md начать L1, все 885 lines, plan 276 lines, 20 original rows, whole initialization example incl actual fixture. Проверить A/I/C и практические evidence, зафиксировать findings и следующий точный cursor; при необходимости промежуточный checkpoint непрерывного диапазона. PR #253 Draft, учебные материалы не менять.
Передать: оператор пересылает прямую ссылку на опубликованный checkpoint Issue #252.
