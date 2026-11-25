# R0-11a — primary fidelity, A01–A04

Incoming R0-10 HEAD 338a83a7c52a6b6757d1d6b47da43ed229a1fa34; historical audit BASE c7e223fd2230a776921fa142fab39ff7fbc019f2; frozen G1 f04f1eb5ee3a400a988757414bcae09113bd885b. L1/L2/L3 under #7149581298, #7149649529, #7149686866. No new audit/implementation or closure.

## Method and coverage

Fetched complete primary audit reports, frozen G1 plans, findings/requirements ledgers for A01–A04, and canonical B01 from findings-B. Compared complete original question text (not a substring), primary ID, original priority (including A02 mixed P0/P1), line, audit verdict, primary finding ID/severity/locus/acceptance, and matrix evidence at cited lines. For A01/A02 finding cells only whitespace between field separators normalized; otherwise literal exact text. Source tables of these four stages use spaced cell separators; no embedded spaced literal pipe encountered in the compared cells. Do not generalize parsing strategy to later stages. A04 initial check used wrong original priority column; corrected to actual third column and rerun, all 17 rows matched. A01/A03/A04 existing matrix detail matched the locus and verdict/evidence cells; A02 initially used links and mapped findings rather than copied matrix detail. Technical Nxx probe labels are not automatically stable finding IDs. Historical sources not edited.

| Stage | Findings incl blockers | Requirements | OK | DEPTH | ERROR | BOUND | P0 DEPTH | Outcome |
|---|---|---|---|---|---|---|---|---|
| A01 | 9 | 12 | 8 | 3 | 0 | 1 | 2 | Exact match |
| A02 | 10 (1 B01) | 11 | 4 | 5 | 1 | 1 | 3 | Matrix evidence added; exact match on readback |
| A03 | 7 | 12 | 8 | 3 | 0 | 1 | 1 | Exact match |
| A04 | 9 | 17 | 8 | 7 | 0 | 2 | 4 | Exact match; retrospective rows excluded |
| Total | 35 | 52 | 28 | 18 | 1 | 5 | 10 | Bounded pass only |

## Exact ledger delta

Commit [3f5f07ae](https://github.com/Melchiorius/backend-interview-labs/commit/3f5f07ae98d1922b791c747dfb97c3cbcc784237) modifies only requirements-A02.md: R09–R19 (all 11 keys), adds verbatim primary matrix topic, article locus and audit evidence after existing mapped findings. R17 primary N06 is technical negative `Long wrong=67;` (A02 report negative table), not JC-A02-N06 (R16 casting criterion); existing N02 related mapping retained. Original question, key, priority, verdict, anchor, package unchanged. Readback of all 11 full added text cells and 7-column structure passed. No changes to A01/A03/A04, canonical B01 or accepted briefs. No additional semantic loss identified in compared entries. Global 499/925 registration counts unchanged. Local Java/Gradle/GUI NOT RUN; source traceability only.

## Remaining

35/499 findings and 52/925 original rows checked in bounded source-fidelity pass; 464 findings and 873 rows remain. Next A05/A06/A07 (including canonical B findings), then A08 onward. R0 remains Draft, not Ready. Full L1–L3, actual single owner for each finding/requirement, residual D scopes, mandatory rows and acyclic schedule, Q/C gates required before final 00 decision.
