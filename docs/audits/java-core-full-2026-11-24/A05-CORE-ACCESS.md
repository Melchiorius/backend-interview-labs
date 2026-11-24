# A05 — CORE-ACCESS: полный независимый аудит

Issue #252, 04 Independent Reviewer, 2026-11-24. Frozen source BASE f04f1eb5ee3a400a988757414bcae09113bd885b; [incoming A04 checkpoint](https://github.com/Melchiorius/backend-interview-labs/issues/252#issuecomment-7131968293), incoming audit HEAD ff9a15059f56914c71c0dc327f589020319ccd67; Draft PR #253. **A05 COMPLETE / CHANGES: 1 new BLOCKER, 6 P1, 2 P2.** A02-B01 retained. No instructional edits, no remediation issue until decision 00 after A25. Scope only frozen source, not live future revisions.

## 1. Scope, provenance

[access.md](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/knowledge/01-java-core/access.md) blob dac21a3d4009e590c755354300f191aa4105fa7e, 421/421 lines (read in ranges 1–210 and 211–421); [CORE-ACCESS.md](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/docs/plans/CORE-ACCESS.md) blob ee5bc3765377541b0759eefe5aaacf7bc47de980, 167/167 lines; 20 original AC rows 60–79, separate normative table six rows 45–50 (header 43–44), and plan counterexample protocol in §5, independently reviewed. No double count. Entire practical package 16 blobs read below; six positive Java sources, eight isolated negatives, runner and README; no checked-in expected stdout fixture. History of article through frozen ref: 68a70d22308bbc944d57528a4ec5a8aa599d4644 (G5 #29), 86e67b756395373b34cbd9c4c8a7c7435c7d090e inheritance, 7f4b83b1dfc6f3871389faa2b70f663b3bbb71b5 interfaces/composition, dac4ff90888301a9821b340475a84f17ab5ba540 nested/functions, aa183b3b1bdba3c5eed8163e4b10aee82a043092 reflection, edf68df067fe4c4583965af002e225d6371796d2 modules/loader. Earlier plan L136 explicitly mentions prior 04 local review, not article authorship. Shared Git identity/squash not exhaustive attribution. No substantial article authorship by 04 identified; earlier review/CI not used as substitute for new review.

## 2. Continuous claims review (421/421)

A = normative JLS/API; I = actual local compiler/runtime; P/N local cases in §5. Direct citations in §7. Article's strongest part is qualified protected access; preserve it.

| Article L | Claims | Independent judgment |
|---|---|---|
| 1–20 | access supports encapsulation, not authorization; three keywords/four modes; access vs scope and type/member/ctor | correct framing; L15 direct naming distinction, revise alongside B01 so it is not interpreted as requiring originally declaring type accessible. |
| 21–33 | four-mode table scoped to member of accessible class and nonmodule code | correct as orientation, not complete. Conditions on protected instance; constructor and nested distinctions explicit. Private special lexical contexts beyond body not needed in baseline table. N03. |
| 34–43 | package boundary, subpackages independent of parent, source directory not rule | correct for ordinary named packages; actual runtime package identity additionally depends on defining loader, N04 and CORE-CLASSLOADER boundary. |
| 44–76 | Parent, Neighbor, same-package protected ctor/static/instance/package field, private rejected | exact CI/local positive plus independent protected_positive=141; private_neighbor rejects. Protected not only inheritance; subpackage rejects. OK. |
| 77–92 | top-level public/package-private; ordinary file naming; Hidden.Visible | top-level private/protected rejected, direct Hidden.Visible rejects. **L81 false generalization**: originally declaring type need not be accessible if member inherited into accessible type. B01 actual field/method/member type access, JLS §8.2 Example 8.2-5. |
| 93–129 | public Token/private ctor/factory; default ctor | private_ctor negative and factory analogue, code correct. L129 misleading without direct same-modifier rule for normal class, N01; distinguish enum/record/special types and legal super invocation, A04-N03. |
| 130–166 | cross-package protected, subclass body and static qualifier S/subtype; this/otherChild and super | §6.6.2.1, local protected_positive, correct; not physical object identity restriction. Child final in actual runner only; article Child can have subclasses. |
| 167–209 | Parent qualifier, cast to Parent, Sibling, package field, parent.touch; note method refs | all independent rejections expected; receiver restriction on fields/methods, not static, correct. Add explicit method ref example N02. |
| 210–221 | protected static Parent.total legal in Child; not outsider; not thread-safe | correct. Local inc method compiled; exact runner reads 67, not concurrency experiment. |
| 222–247 | protected constructor rules and anonymous subclass | §6.6.2.2 correct. ordinary new rejects, anonymous creation works outside package; qualifier super and constructor method reference separately N02. No implication final/sealed can be subclassed. |
| 248–267 | private nest source access, not implementation; adjacent top-level denied | positive nested access and private_neighbor rejection; private member inheritance vs access not explicit locally; N03, inheritance L37/188. No JVM nest proof. |
| 268–296 | class vs interface member types, private helper/default, protected forbidden | §9.3–5; correct examples. Missing compact field/public static method distinction and noninheritance of private helpers N05; existing interfaces article target. |
| 297–315 | overriding cannot narrow access; direct foreign-package same-signature package method not override | correct for displayed direct relationship; independent narrow_override/package_override rejections. Do not generalize to transitive overriding through intermediate override; §8.4.8.1, A06 check. N04. |
| 316–325 | access before dispatch, Parent declared type rejects despite public Child override | actual public_override_parent fails; dispatch positive child:child. L316 absolute wording should be source-level compile checks only, JVM resolution checks also exist (JVMS §5.4.4), later L407 acknowledges linkage. N04. |
| 326–356 | CreditAccount invariant, API vs fields, final list reference, unsafeEntries and List.copyOf snapshot | alias and snapshot behavior observed [A]:[]; immutable String elements, no deep copy. add(null) allowed, snapshot throws NPE; missing explicit null policy and no-fresh-identity caveat, N06. |
| 357–374 | scope vs access, shadowed value, imports do not grant access | observed 67:5, correct. Private access not authorization, no confusing visibility/JMM. |
| 375–386 | decision procedure locus, static qualifier, member kind, access vs dispatch; diagnostic not spec | useful, fix step 2/owner wording with B01; do not infer declaring type accessibility always required. |
| 387–398 | exact package and verification claim | all 16 files read; byte-identical executable subset run, eight intended diagnostics checked, original runner honest re nonzero alone. N08 limited coverage optional. |
| 399–412 | boundaries JPMS/reflection/linkage; actual links to MODULES/CLASSLOADER/REFLECTION | correct distinctions, no full future topic certification; SERIAL already published, N07. |
| 413–421 | JLS and AccessibleObject sources, C research plan | direct relevant JLS sections checked; API reference to reflection retained as boundary; not all anchors crawled. |

## 3. Original G1 matrix, 20/20

11 P0, 6 P1, 3 P2. Original IDs preserved; judgments are new, not rewriting original historical plan.

| ID | P | Plan L | Article L | Verdict |
|---|---|---|---|---|
| AC-01 | P0 | 60 | 11,21–33 | OK, 3 keywords / 4 regimes |
| AC-02 | P0 | 61 | 77–80,268–293 | OK, top-level/member type and independent negatives |
| AC-03 | P0 | 62 | 13–18,77–92,375–386 | ERROR B01, true direct case but false general rule for inherited member |
| AC-04 | P0 | 63 | 34–76 | OK, 141 and private_neighbor rejection |
| AC-05 | P0 | 64 | 34–39,74–76,188–196 | OK, package_field/subpackage rejects |
| AC-06 | P0 | 65 | 130–190 | OK, S qualifier and contrast, static type correct |
| AC-07 | P0 | 66 | 142–165,198–220,316–324 | DEPTH N02: method ref qualification and actual subtype example; ordinary rule correct |
| AC-08 | P0 | 67 | 222–246 | DEPTH N02: constructor ref and other forms; shown C1–C3 correct |
| AC-09 | P0 | 68 | 248–266 | DEPTH N03: private noninheritance explicitly promised but not stated locally, link inheritance |
| AC-10 | P0 | 69 | 5–9,326–355 | DEPTH N06: null policy inconsistency in Tasks, no deep immutability claim |
| AC-11 | P0 | 70 | 93–129 | DEPTH N01: private ctor correct, default access not specified and misleading |
| AC-12 | P1 | 71 | 297–312 | OK, widening legal/narrowing rejected |
| AC-13 | P1 | 72 | 314 | OK for direct scenario, N04 do not generalize |
| AC-14 | P1 | 73 | 274–295 | DEPTH N05: ordinary private/default example correct, add static/private body/noninheritance contrast |
| AC-15 | P1 | 74 | 268–293 | OK, interface member types and class member boundary |
| AC-16 | P1 | 75 | 181–190 | OK, Sibling not subtype of S |
| AC-17 | P1 | 76 | 316–324 | DEPTH N04: clarify source vs JVM access; direct call/dispatch correct |
| AC-18 | P2 | 77 | 407–409 | BOUND, JPMS explicit actual follow-up |
| AC-19 | P2 | 78 | 407–411 | BOUND, reflection/serialization/framework boundaries, N07 stale |
| AC-20 | P2 | 79 | 407 | BOUND, binary linkage deferred, N04 scope clarification |

Totals **9 OK / 7 DEPTH / 1 ERROR / 3 BOUND**; P0 5 OK / 5 DEPTH / 1 ERROR; P1 4 OK/2 DEPTH; P2 3 BOUND. A05 is complete as an audit, not PASS content.

### Separate plan normative table and counterexample protocol

L45 public member of accessible type correct when type through which selected is accessible, not necessarily declaring type; L46 protected per §6.6.2.1 correct; L47 package access same package; L48 private same enclosing top-level context (rare permits/component-list extensions not covered by orientation); L49 top-level package access correct; L50 top-level public with JPMS caveat correct. Six rows reviewed, not extra curriculum questions. L52 statement “public member does not make the type itself accessible” is correct; B01 concerns article's different, stronger proposition. Plan L83–130 reviewed: P1–P6 field/access/static, C1–C3 ctor, N1–N2 nested/private, T1 top-level/type, O1–O3 overriding, I1–I4 interface; actual local cases and source package below. Plan's P1/P2 labels mix positive baseline and negative variant labels in L98/118, no changes to historical plan. Historical 18+5 local 01 and 24 previous 04 checks L134/136 are historical, not counted as new or source CI. L138–167 design contract, role handoff and boundaries read, no discrepancy requiring changes to workflow. 

## 4. All practical package files and runner evidence

| [README.md](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/access/README.md) | 8b3238777fadac411cc2b8ee1fa00311d37abca9 | 100644 | read complete; not needed for local execution |
| [negative/BadNewParent.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/access/negative/BadNewParent.java) | 342541fa5829e0a9eac840b0b787c7d7bc7735ea | 100644 | read complete; exact local git hash-object match |
| [negative/BadPackageValue.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/access/negative/BadPackageValue.java) | d8ef6e0a3461cbe516edabc86271cc3e3721c9c5 | 100644 | read complete; exact local git hash-object match |
| [negative/BadParentCast.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/access/negative/BadParentCast.java) | d40ff3bca1bc281f4046704241ea96ad030e2253 | 100644 | read complete; exact local git hash-object match |
| [negative/BadParentValue.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/access/negative/BadParentValue.java) | 44be97e2bf412663add016e3d265a04957081399 | 100644 | read complete; exact local git hash-object match |
| [negative/BadPrivateInterfaceMember.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/access/negative/BadPrivateInterfaceMember.java) | 4a5309d5a310f6fd7652233b6b25cbc4808b35da | 100644 | read complete; exact local git hash-object match |
| [negative/BadProtectedInterfaceMember.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/access/negative/BadProtectedInterfaceMember.java) | b55b164534b778b848385076f56d348e17a697ec | 100644 | read complete; exact local git hash-object match |
| [negative/BadSibling.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/access/negative/BadSibling.java) | 59295aa11cfb6d433ef2cac3f8bc0beb7ad56ff9 | 100644 | read complete; exact local git hash-object match |
| [negative/BadStaticUnrelated.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/access/negative/BadStaticUnrelated.java) | b0daed8a925e02ddb73a4542c0cdee1ae9963d95 | 100644 | read complete; exact local git hash-object match |
| [src/api/InterfaceTypes.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/access/src/api/InterfaceTypes.java) | 322c6141ba376041bbf544a685bed7dc4ebdfa8c | 100644 | read complete; exact local git hash-object match |
| [src/api/Neighbor.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/access/src/api/Neighbor.java) | 37b4face1c2a461a4b820b8448589573dbc183aa | 100644 | read complete; exact local git hash-object match |
| [src/api/Parent.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/access/src/api/Parent.java) | d865f3d08dc3016b8cbdd9933e7645a134df6cfd | 100644 | read complete; exact local git hash-object match |
| [src/client/AccessDemo.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/access/src/client/AccessDemo.java) | 2a9dec643998553e23cb6102c68ec134f32c6a9f | 100644 | read complete; exact local git hash-object match |
| [src/client/Child.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/access/src/client/Child.java) | cbe7060ef59ab80d2a522a2d33045eb9c5a8699f | 100644 | read complete; exact local git hash-object match |
| [src/client/Sibling.java](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/access/src/client/Sibling.java) | e16ac3bf73406d60ff9e528d5df6826bb55e2cf5 | 100644 | read complete; exact local git hash-object match |
| [verify.sh](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/examples/core/access/verify.sh) | fb8f2035e857959a6e98c3b7f0df2caca4c1c69d | 100644 | read complete; exact local git hash-object match |

Six positive sources: Parent declares fields and constructor, no secret field (article's illustrative parent has secret, not claimed identical); Neighbor 141; Child super.touch/this/otherChild/Parent.total; Sibling accesses own field, independent final subclass; InterfaceTypes declares Member and Nested; AccessDemo makes **9 explicit check calls** (own/other/super/second/total/neighbor/sibling/anonymous/member), no Java -ea dependency, only CHECK PASSED stdout. No assertions for public inherited members from hidden parent, nested private access, default ctor or overriding; these are covered by article and/or separate audit probes, not historical runner tests. README accurately distinguishes runnable from exercise and stderr review from automated reason check, names all eight cases. No fixture in tree; shell checks positive exit, negatives each in own output directory, requires nonempty stderr, outputs all diagnostics, exits on unexpected success/missing diagnostics. Mode 100644 and invoked via bash; no chmod necessary.

Exact local OpenJDK/javac 21.0.12.1 Debian. Restored all 14 Java files and runner manually in /tmp/jc-a05/exact, verified git hash-object **15/15** matches above. First runner restoration used equivalent printf form but hash differed; did not run until corrected exact bytes using base64 fetch; final runner hash fb8f2035e857959a6e98c3b7f0df2caca4c1c69d, size 1345. Executed from reconstructed repo root: bash examples/core/access/verify.sh, exit0. Not a full repository clone; README read but not restored for execution. Actual CHECK PASSED, eight EXPECTED REJECTION, ALL EXPECTED OUTCOMES OBSERVED. Independently read diagnostics, both local and [exact published push run #45390717871](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45390717871) job 138433360807 raw log timestamps 12:09:27.3053179Z to 12:09:31.0890074Z, Temurin 21.0.13-9. Nine outcomes, not nine JUnit tests.

| Negative file | Line | Local and CI reason |
|---|---|---|
| BadParentValue | 11 | value has protected access in Parent |
| BadParentCast | 11 | value has protected access in Parent |
| BadSibling | 11 | value has protected access in Parent |
| BadPackageValue | 11 | packageValue not public in Parent, outside package |
| BadNewParent | 11 | Parent() protected |
| BadStaticUnrelated | 11 | total protected |
| BadPrivateInterfaceMember | 8 | illegal combination public/private |
| BadProtectedInterfaceMember | 8 | illegal combination public/protected |

All 8 messages point to expected expressions and one error each, not missing path/dependency. A04 audit-only run #45397203858 success confirmed. Exact package CI not evidence of all article statements; B01 specifically outside runner. No new audit CI claims until checkpoint.

## 5. New local independent probes

Actual 30 distinct case directories in /tmp/jc-a05/probes; javac --release 21 -XDrawDiagnostics -d <case/out> <case sources>; six accepted and executed java -cp <out> <Main> exit0, 24 isolated rejected exit1. All output checked. Reconstructed snippets adapted into appropriate files, not verbatim complete compilation units of every fence. Initial positive public_default diagnostic harness lacked throws on reflective getConstructor; corrected harness and reran whole sequence, final six pass; no false failure attributed to article. No code committed. Categories below are actual javac diagnostics, not JLS text.

| Positive case | Exact scenario / observed stdout |
|---|---|
| public_inherited | package api class Hidden { public int value=67; public int get(){return value;} public static class Visible{} }; public Exported extends Hidden; client accesses e.value/e.get/new Exported.Visible(), stdout 67:67:Visible. **B01** |
| public_default | public class PublicClass {} in api; client new api.PublicClass().getClass().getConstructor(); stdout public api.PublicClass() |
| protected_positive | Parent with value67,total67,packageValue7,protected ctor/touch; Neighbor.read 141; Child.ok touches twice and reads other Child 136; bound(Child c) returns c::touch; runtime value70; anonymous Parent value67. Actual 141 / 136 / 70 / 67 |
| interface_positive | Rules member class, private helper returns67, default result(), field VALUE=67, static number(); actual 67:67:PublicNested |
| nest_factory_snapshot_scope | Outer private ctor/static of, Nested read private secret=67; Tasks add A, snapshot then clear leaked entries -> [A]:[]; add null -> caught NPE “null rejected”; Counter prints 67:5. |
| dispatch | api.Base protected act() and public call() returns act(); client.Child public override; c.act and Base b=c; b.call => child:child. |

| Negative case | Expression or declaration | Diagnostic category |
|---|---|---|
| hidden_name | api.Hidden.Visible v | not.def.access.class.intf.cant.access |
| package_default | new api.PublicClass() with explicitly package-private ctor | not.def.public.cant.access |
| parent_field | p.value where Parent p | report.access protected |
| cast | ((Parent)p).value inside Bad extends Parent | report.access protected |
| sibling | Sibling p, p.value in Bad | report.access protected |
| package_field | packageValue in subclass different package | not.def.public.cant.access |
| parent_method | Parent p; p.touch() | report.access protected |
| parent_methodref | Parent p; Runnable r=p::touch (in return) | prob.found.req / invalid.mref / report.access |
| ctor | new api.Parent() in Bad extends Parent | report.access protected |
| ctor_ref | Supplier<api.Parent>, api.Parent::new in Bad extends Parent | prob.found.req / invalid.mref / report.access |
| unrelated_static | api.Parent.total in unrelated client.Bad | report.access protected |
| private_neighbor | new Parent().secret in api.Bad | report.access private |
| subpackage | package api.internal extends api.Parent; packageValue | not.def.public.cant.access |
| top_private | private class Hidden{} | mod.not.allowed.here private |
| top_protected | protected class Hidden{} | mod.not.allowed.here protected |
| private_ctor | new api.Token("67") from client; private Token(String) | report.access private |
| interface_private_type | interface Bad {private class Hidden{}} | illegal.combination.of.modifiers public/private |
| interface_protected_type | interface Bad {protected interface Nested{}} | illegal.combination.of.modifiers public/protected |
| interface_protected_method | protected int helper(); | mod.not.allowed.here protected |
| interface_private_abstract | private int helper(); | missing.meth.body.or.decl.abstract |
| interface_private_field | private int x=67; | mod.not.allowed.here private |
| narrow_override | Base protected act; Bad @Override void act | override.weaker.access |
| package_override | api.Base package internal; client.Bad @Override public internal | method.does.not.override.superclass |
| public_override_parent | unrelated client: Parent p=new Child(); p.touch() even public override | report.access protected |

The two successful inherited member calls and nested type construction prove the false generalization, not that Hidden itself becomes accessible. Runtime probe uses source-level Java; no reflective accessibility claim from this case. Note method/field/type selection through qualified type may need accessible member signatures and additional syntax conditions; no blanket permission. Do not assert access from any subtype without inspecting member inheritance/hiding and actual access.

## 6. Fence inventory and scope of evidence

21 Java fences plus one shell. All read statically; positive and negative contextual equivalents mapped below, no claim all snippets compiled verbatim. Original package and probes together cover major behavior. Plan's 3 Java fences 85–95,97–107,111–116 reviewed through protected cases. README 3 command fences L20–23,48–50,56–58, final bash exact run. Individual manual command lines not separately repeated.

| Article fence | Verification |
|---|---|
| 44–57 | Parent basis (contextual + exact runner minus secret; secret in probe) |
| 61–71 | Neighbor exact/positive 141 |
| 83–90 | Hidden direct rejection, contrasting inherited public positive |
| 96–114 | Token source static review; private ctor negative; factory analogous Outer.of actual, not exact Token compile |
| 116–125 | UseToken static review, factory analogue; direct new negative |
| 142–159 | Child protected_positive, exact runner split methods |
| 167–172 | parent_field, BadParentValue |
| 174–179 | cast, BadParentCast |
| 181–186 | sibling, BadSibling |
| 192–196 | package_field, BadPackageValue |
| 198–202 | parent_method |
| 213–218 | Parent.total++ compiles in protected_positive (method not called); exact runner reads total |
| 226–235 | protected super via actual Child |
| 239–244 | anonymous Parent positive |
| 252–262 | Outer/Nested read 67 |
| 274–286 | interface_positive |
| 290–293 | independent protected method/private type negatives |
| 301–310 | dispatch positive (String return adaptation), narrow_override negative |
| 318–322 | public_override_parent rejection |
| 332–351 | Tasks alias/snapshot/null in actual local case |
| 361–371 | scope 67:5 |
| 393–395 | exact bash runner |

No local separate exact Token/UseToken positive, all syntactic variants, qualified super/new inner, subtype-of-S probe, generic protected bound, protected member type, private permits/record-component cases, changed binary linkage or JPMS/reflection runtime experiments. They are normative/adjacent topic boundaries, not invented executed tests. 

## 7. Independent source check A/I/B/C

A direct read JLS SE21 [§6.6.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6.1), [§6.6.2.1–2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6.2), [§7.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.6), [§8.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.2) **Example 8.2-5 explicitly supports B01** (public subtype Point4d inherits public z from inaccessible Point3d), [§8.4.8.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.1), [§8.8.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.9), [§9.3–5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.3), [List.copyOf API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html#copyOf(java.util.Collection)), [JVMS21 §5.4.4](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.4.4). Other source pointers e.g. §8.4.8.3, §6.3, §7.7 reviewed by relevant chapter and rule, not full external crawl. I local OpenJDK and actual Temurin CI; no private implementation source needed for language guarantees. B [Baeldung access modifiers](https://www.baeldung.com/java-access-modifiers) 2024-07-23 §5/6 actual static example correct, wording overly broad about protected constructor and subclasses, no qualifying-type guarantee from B; preserve article's better treatment. dev.java overview historical plan not re-fetched this stage; not relied on for evidence.

C fresh sample 2026-11-24: actual question headings/lines re-read, no frequency claims, independent nonplan source teamlead used for post-draft gap check:
- [in28minutes exact README](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/9ba8736f44fdc175dac5046b7d55c94fd74416cd/readme.md) actual branch master verified, L95 encapsulation and 103–106 four modes.
- [Hruday exact README](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/ced3cf4f91084e9b45366f7a81a70526e7a4be7d/README.md) L202–211 encapsulation/getters, L223 private inheritance, L248/256 override/access, L339–351 levels/top-level/constructors. Supports N01/N03/N05. Exact revision resolved A01, not refresh of default branch.
- [teamlead exact README](https://github.com/teamlead/java-interview-questions/blob/4c0784024412904625e1455189d3c05cba07bebf/README.md) L103/107/144 encapsulation/modifiers, L497–498 package private, L641/1091 narrowing, L1182 private interface. Nonplan C sample. Revision previously resolved A01.
- [Baeldung CORE Q3](https://www.baeldung.com/java-interview-questions) 2024-05-11 actual question; answer glosses nesting/qualifier and JPMS, not normative; no import of those inaccuracies.
- Chernyshov English README pinned 597e08112ab6a57d6eac8e3c70c070da606dae73 fetched/filtered, no direct access-modifier question identified in chosen pattern; GAP, not fifth successful C source. No exhaustive bank coverage.

## 8. Findings register

| ID / severity | Exact locus | Evidence, impact, acceptance criterion and proposed role |
|---|---|---|
| JC-A05-B01 BLOCKER (AC03 P0) | L81, related framing L15/375–382 | Claim that external code must access declaring type is false: JLS §6.6.1 + §8.2 Example 8.2-5; public_inherited local pass 67:67:Visible, hidden_name fails. Replace with accessible type through which member is selected plus member's own access rules, account for inherited members. Keep valid Hidden.Visible negative, add public Exported extends Hidden and actual field/method/member type contrast, don't promise declaring type itself accessible. 02; 03 add independent positive/negative if 00 authorizes; 04 verify. Must fix before final acceptance. |
| JC-A05-N01 P1 (AC11 P0 depth) | L129 default ctor warning | JLS §8.8.9 default constructor for ordinary class takes same access modifier as class (or package access), public_default passes; explicit package-private ctor of public class rejects. Distinguish ordinary default vs explicitly declared no-arg, special enum/record and legal implicit super invocation (A04-N03), enclosing type accessibility vs constructor modifier. Avoid implying compiler may choose arbitrary access. 02, cross A04/A07/A15. |
| JC-A05-N02 P1 (AC07/08 P0 depth) | L205–208,222–246 protected method refs/ctor extra forms | §6.6.2.1 applies to method refs as well; Child::touch / bound Child ref legal subject to ordinary typing, actual c::touch accepted, p::touch rejected. §6.6.2.2 ordinary ctor reference Parent::new outside package rejected, local ctor_ref; qualified super and anonymous creation rules separate. Add one worked reference contrast and link FUNCTIONS/NESTED; optionally S-subclass example; not duplicate entire section. 02. |
| JC-A05-N03 P1 (AC09 P0 depth) | L248–266 private access vs inherited member | §6.6.1 and §8.2/8.4.8: lexical permitted access does not imply inheritance; private member not inherited, private method not overridden; actual inheritance L37/188. Include concise contrast and link. For exact rules §6.6.1 includes permits and record component lists of enclosing top-level; orientation table not absolute body-only claim. No reflection-as-security guarantee. 02, cross A04-N07/A06. |
| JC-A05-N04 P1 (AC17) | L299–324/375–386 “Проверка доступа происходит до выполнения программы” | source compile check vs JVM resolution access checks (JVMS §5.4.4 may throw IllegalAccessError), later L407 already mentions binary linkage. Parent typed public override rejection correct. Direct package-private across package not overridden, do not extend to all transitive override paths (JLS §8.4.8.1); A06 verify independently. Define package language vs runtime package+defining loader with existing link, no full JPMS duplication. 02/05. |
| JC-A05-N05 P1 (AC14) | L268–295 interface modifier mini-map | JLS §9.2–5, fields implicitly public static final, methods without access modifier public incl static, private instance/static require body, not inherited/overridden, member types public static. interface_positive and private_field/private_abstract/protected_method negatives; avoid claiming private default. Short cross-link to INTERFACES. 02. |
| JC-A05-N06 P1 (AC10 P0 depth) | L336–353 Tasks.add permits null but snapshot uses List.copyOf | Actual add(null) then snapshot NPE, while snapshot A unaffected by later clear. API requires null-free collection, no guarantee fresh identity for arbitrary unmodifiable source. Set and explain null policy at ingress (and unsafeEntries caveat), or choose snapshot semantics preserving null; illustrate alias vs view vs snapshot via existing IMMUTABLE/COL-CONTRACTS, no deep-copy guarantee, no universal concurrent snapshot claim. 02/03. |
| JC-A05-N07 P2 | L405 CORE-SERIAL future, references L413–421 | published serialization.md exists, link instead of stale future; add direct §8.2/§8.8.9/§5.4.4 locators. Preserve correct JPMS/reflection boundaries, no broad rewrite. 05. |
| JC-A05-N08 P2 | practice L389–398, runner & plan L98/118 labels | existing runner passes and candidly requires inspection; no reason assertion in CI, not claim each false premise tested. Optional add machine diagnostic class/expected exit robustness under current standard only if 00 approves, especially B01 regression. Plan historical P1/P2 ambiguous labels not rewrite. Contextual Token exact positive not run; not evidence of code defect. 03/05. |

No new blocking technical infrastructure problem. B01 is a content blocker independent of runner success. A02-B01 remains. User did not authorize remediation; findings only.

## 9. Navigation, quality, limitations, next cursor

24/24 article + 8/8 plan + 20/20 practice README relative file targets resolve against frozen tree 499 entries, nontruncated. Repeated occurrences counted. All package Theory/Research paths visibly point to existing access/classes/types and plan (not count JUnit). Existing inheritance L37/186–213/283 consulted; earlier A04 links serialization and classloaders corroborate. Neighbor articles not audited here. Strong practical pedagogy: two actual packages, explicit location/qualifying type/member kind, correct core protected rule, CI negatives isolated and not overclaimed. Need fix B01 and null policy, add small worked contrasts rather than broad rewrite; no inference of AI authorship. 

NOT RUN full clean clone/Gradle/JUnit, local actual GUI/PowerShell/Obsidian/all anchors/HTTP, all possible contexts, every inline example verbatim (especially Token positive and qualified inner/ctor cases), alternate JVM/OS except local Debian and published Temurin, JPMS/reflection access experiments beyond default ctor introspection, changed binary linkage/bytecode/nestmates, classloader isolation or JMM/benchmarks. Actual exact runner and 30 local independent probes as specified, no fabricated extra tests. Source direct normative checks/finite C not exhaustive. First restoration hash mismatch and initial diagnostic harness issue corrected before final successful evidence. 

**A05 COMPLETE, overall IN PROGRESS.** Cumulative 5/41 articles, 5/42 curricular IDs, 72/925 original matrix rows; 2 BLOCKER / 32 P1 / 10 P2. Next A06 CORE-INHERITANCE: frozen inheritance.md blob 51f7138a528a0500c9d7bb0eb22276dc93821cdd lines 1–519, split 1–229 and 230–519 if needed; plan CORE-INHERITANCE.md blob 43d55dd8d663ebd8a1d63222482a904479e640cb 1–200, IH-01..25 L63–87; CORE-INHERITANCE_POST_DRAFT_AUDIT.md blob 11a3b9ad0f55294ce7938ad5e3bb488211f29f4f L1–80 separate, no double count; entire examples/core/inheritance (verify.sh d545ff60f4ee1b440b615a6e64941bfbc8da238a mode 100644 bash). Cross-check A05-B01/N03/N04 and A02/A03/A04 findings, do not fix sources. 

### Передача эстафеты
Кому: **04 — Independent Reviewer (продолжение аудита)**.
Что сделать: выполнить A06 CORE-INHERITANCE на frozen source f04f1eb5ee3a400a988757414bcae09113bd885b, inheritance.md начать строка 1, полный план и отдельный post-draft audit, 25 исходных строк матрицы, весь examples/core/inheritance; при необходимости сохранить промежуточный точный непрерывный cursor вместо неподтверждённого COMPLETE. PR #253 Draft, сохранить отчёт/индекс, исходники не менять.
Передать: оператор пересылает прямую ссылку на опубликованный checkpoint Issue #252.
