# R0-10 — ledger integrity checkpoint (L1/L2)

Coordinator [#7149581298](https://github.com/Melchiorius/backend-interview-labs/pull/256#issuecomment-7149581298): D-WAVE-03 scope accepted/queued, ledger corrections required. Accepted briefs and historical audit/G1 untouched. BASE c7e223fd2230a776921fa142fab39ff7fbc019f2, frozen source f04f1eb5ee3a400a988757414bcae09113bd885b. Incoming R0-09 HEAD d0d12dc0a7ce25cbce587f92bdab3bf64ed07c53.

## L1 structural pass — complete at checkpoint, static only

All 49 ledger files (24 requirement tables, 24 non-blocker finding tables, 1 blocker table) checked for header/separator/data column counts using unescaped table delimiters. Eight malformed separator rows found and corrected from nine columns to eight: findings-A04, A08, A13, A14, A15, A16, A21, A22 (each L6). No data-row count mismatch at that scan. After initial OR repairs exact readback at b33bf83cfc85a1ab609ba4e711d2b2a08757ae98 confirmed all 49 tables consistent. GUI/render NOT RUN; static counts not proof of all Markdown rendering behavior.

## L2 — detected literal and markup corrections; exhaustive fidelity pass still OPEN

All 49 ledgers scanned for odd backtick count and doubled slash placeholders; exact source lines read for the detected candidates. This heuristic is not a substitute for per-row primary fidelity. Corrected semantic losses (six affected records) with literal Markdown-safe pipes:

| Record | Immutable source | Restored content |
|---|---|---|
| JC-A16-N12 | [A16.2 L71](../../audits/java-core-full-2026-11-24/A16.2-CORE-PATTERNS.md#L71) | `!(o instanceof String s) \|\| s.isEmpty()` instead of slash placeholders |
| A16 PM-03 | [G1 L89](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/docs/plans/CORE-SEALED-PATTERNS.md#L89) | original contrast `instanceof &&` vs `\|\|` — нет; truncated tail restored |
| A16 S-04 | [G1 L73](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/docs/plans/CORE-SEALED-PATTERNS.md#L73) | `final\|sealed\|non-sealed` явно или неявно |
| A17 C-16 | [G1 L111](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/docs/plans/CORE-ENUMS-CONTROL.md#L111) | `&&/\|\|` short-circuit, слева-направо, прогноз побочных эффектов |
| A18 OP-10 | [G1 L101](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/docs/plans/CORE-SYNTAX-OPERATORS.md#L101) | `&`/`\|` with boolean evaluate BOTH operands, в отличие от `&&`/`\|\|` (historical question, ERROR N19 and corrected acceptance unchanged) |
| A18 OP-11 | [G1 L102](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/docs/plans/CORE-SYNTAX-OPERATORS.md#L102) | `\|\|` short circuit при true, правая сторона не вызывается |

Two source-inherited markup defects repaired without semantic changes: A19 OT-23 (G1 L117, balance bold and code around Optional<T>); A22 NM-23 (G1 L400, close backtick after JLS 15.4). A23 RG-19 odd-backtick heuristic false positive caused by backslash adjacent to closing code delimiter; original question identical, preserved. No severity, priority, historical verdict, finding ID, ownership or OPEN disposition changed by L1/L2. Newly imported A23/A24 used escaped pipes rather than replacing literal pipes with slashes. Exact primary L2 batch attempt exceeded tool-call budget before writes; subsequent live HEAD check confirmed no partial writes from that attempt. This is an explicit residual verification gap, not L2 complete.

## Readback and residual obligations

At b33bf83cfc85a1ab609ba4e711d2b2a08757ae98, three disjoint stage groups counted: B+A01..08 84 findings / 143 rows; A09..16 189 / 297; A17..24 226 / 485. Aggregate 499 unique findings (7 BLOCKER/410 P1/82 P2, all OPEN), 925 unique rows (265 OK/506 DEPTH/25 ERROR/129 BOUND), 350 P0 DEPTH (31+125+194). Counts within each group unique, stage namespaces disjoint. Later literal corrections preserve all keys and categories. New A23/A24 P0 DEPTH 31+31. The count is ledger registration, not repair and not complete primary fidelity. All historical and accepted brief blobs remain unchanged.

Before Ready: run **full primary-fidelity comparison** against original G1 question cells and exact primary finding acceptance/matrix lines for all 499/925 entries in bounded batches. Parse literal pipes/escaped backslashes/inline code and relative links, do not normalize away semantics. Record per-file/per-ID actual source corrections and remaining ambiguities; preserve A22.3 NM-14 correction and A16.3 N15 supplemental evidence, historical exceptions, accepted scopes. Recheck all 49 tables after any correction; compare stable IDs, priorities, verdicts, source anchors, actual owners and mandatory rows. For ambiguous source Markdown use exact quoted source block/link rather than inventing semantic content. No new JC IDs; if a real additional teaching issue is found, report separately to 00 rather than silently changing audit counts. Full Q/C gate and dependency reconciliation still pending. No Java/Gradle/GUI or new runtime evidence by 01.
