# R0-12a — global integrity and residual D scopes

Вход [#7155018719](https://github.com/Melchiorius/backend-interview-labs/pull/256#issuecomment-7155018719). Source HEAD 5460b71dfa9b9aea3e0fe951e643c2619568e8aa, previous integration BASE 8312d4e11af55c1afdc96e140d49c665cb49b3c4; historical audit c7e223fd2230a776921fa142fab39ff7fbc019f2; frozen G1 f04f1eb5ee3a400a988757414bcae09113bd885b. Previous exact [CI #45562162026](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45562162026) completed/success, build 34/34 REST run/jobs verified. R0-12 divided into 12a global check/residual proposal and 12b exact owner assignment, DAG, Q/C and final coordinator handoff.

## Global rescan

Refetched all 49 full ledgers at source HEAD in four disjoint groups (B+A01–06, A07–12, A13–18, A19–24). Initial single large call exceeded tool call limit before writes; bounded reads completed. Schema-aware parser uses each header width, not a universal width (findings-B/A02=10; A05/A06/A07/A09=9; other findings=8; requirements=7). Initial fixed-width attempt falsely flagged early tables; corrected before conclusions. Header/separator/data all checked, zero malformed rows. ID uniqueness per group, disjoint stage namespaces and B/N IDs; no unknown explicit JC-Axx-N/B in association cells (B IDs canonical). Source locator and frozen original link on every row; 2521 primary/frozen link occurrences in source/criterion/question/matrix fields checked against immutable recursive tree paths, missing=0. Exact line anchors previously checked during primary fidelity, not new exhaustive HTTP/GUI validation.

| Group | Files | Findings | Rows | OK | DEPTH | ERROR | BOUND | Mandatory | Links |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| B+A01–06 | 13 | 59 | 97 | 46 | 35 | 4 | 12 | 26 | 253 |
| A07–12 | 12 | 101 | 166 | 47 | 84 | 4 | 31 | 57 | 433 |
| A13–18 | 12 | 165 | 287 | 63 | 180 | 8 | 36 | 139 | 746 |
| A19–24 | 12 | 174 | 375 | 109 | 207 | 9 | 50 | 153 | 1089 |
| Total | 49 | 499 | 925 | 265 | 506 | 25 | 129 | 375 | 2521 |

7 BLOCKER/410 P1/82 P2; 1 FIXED/498 OPEN. 583 P0/212 P1/129 P2/1 mixed P0/P1. 25 ERROR and 350 P0 DEPTH disjoint, mixed R11 included as ERROR. All findings have proposed owner and package, canonical final accountable row owner remains to reconcile. Exact frozen CURRICULUM.md bullet-link ID set versus finding CORE columns: 42/42 missing=extra=0. Initial regex excluding digits incorrectly split CORE-I18N; corrected on both sides and reread A24; CORE-BUILDERS shares strings.md.

Historical ERROR list: A02 R11 mixed; A05 AC-03; A06 IH-06/IH-21; A07 CI-08; A09 IC-03/IC-17; A10 NF-12; A13 ST-03/ST-25; A14 AR-14/TX-09; A18 SY-10/OP-06/OP-10/SY-21; A20 AN-05/RF-15; A21 JM-17/CL-08; A22 DT-22; A23 RG-21/UT-32/UT-35; A24 S-10. NM-14 DEPTH per A22.3 and E-03 P0 DEPTH (B4b) remain. Historical classifications do not prove closure.

## Base synchronization while working

During scan main advanced: [G5 B2a #7155086759](https://github.com/Melchiorius/backend-interview-labs/pull/266#issuecomment-7155086759), PR #266 CLOSED/MERGED exact reviewed HEAD b195ff79323e8e3f2d3df7ab141a3e89f3908f0c, published main fdda125a39946212a04d95b863988f88e2ec447f, sole parent 8312d4e11af55c1afdc96e140d49c665cb49b3c4. Guard stopped first write before tree creation. Read old/new tree and compare: new main one commit ahead, eight files only access.md and examples/core/access/, no overlap with 72 planning blobs. Original 392/392 blobs on old R0 matched old main. Ordinary two-parent merge commit 900bb697ead442db7f7785f878280eef6159cc85, tree exact union independently checked: 396/396 new main blobs and 72/72 old planning blobs, no extras, full trees untruncated (628/559/632 entries). Preserve immutable audit and G1, not role 01 teaching changes. [Postmerge push CI #45562623742](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45562623742) in_progress at synchronization read, no final G6 decision observed. Keep JC-A05-B01 OPEN until explicit 00 G6, all N01–N08 open. B1 final G6 [#7154384017](https://github.com/Melchiorius/backend-interview-labs/pull/262#issuecomment-7154384017) and R0 [#7154399145](https://github.com/Melchiorius/backend-interview-labs/pull/256#issuecomment-7154399145) unchanged; JC-A02-B01 FIXED. D1–D6 accepted/queued decisions #7149479380, #7149541309, #7149581298, #7149649529, #7149686866, #7149835794. Other B/D not authorized. C/CON HOLD.

## Residual proposal

[D-RESIDUAL-B-GROUPS](D-RESIDUAL-B-GROUPS.md): 94 non-blockers (75 P1/19 P2), 63 proposed mandatory final closure rows (3 ERROR/60 P0 DEPTH), existing B criteria unchanged. R11→D-A02, CI-08→D-A07, IC-17→D-A09 final accountable owner proposal, B contributes prerequisite evidence only. Six B-exclusive mandatory rows; 375 = 6 + 63 + 306 from preaccepted D waves. No reclassification, no ledger closure. Next R0-12b verify all 499 and exact 375 row assignments, accepted B/D scopes, explicit acyclic DAG and ordering, shared writer collision prevention, Q/C. Final coordinator handoff only when fully ready. Local Java/javac/Gradle/runner, GUI and new external HTTP NOT RUN. R0 IN PROGRESS / FINAL HOLD.
