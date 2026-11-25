# B1 — CORE-CONVERSIONS: numeric narrowing blocker

**Proposed bounded brief for 00**, not G2 authorization or completed repair. Canonical finding [JC-A02-B01](findings-B.md), original requirement [A02/CORE-CONVERSIONS.md/L11](requirements-A02.md), mixed P0/P1 (ERROR). Primary [A02 report L152](../../audits/java-core-full-2026-11-24/A02-CORE-CONVERSIONS.md#L152). Frozen instructional snapshot f04f1eb5ee3a400a988757414bcae09113bd885b; R0 BASE c7e223fd2230a776921fa142fab39ff7fbc019f2. 00 must pin live BASE before issuing actual G2.

## Scope and exact acceptance

02 owns knowledge/01-java-core/conversions.md, particularly frozen L33–65 and erroneous L64. Distinguish floating-point narrowing to int/long (round toward zero, NaN→0, out-of-range saturates at **intermediate** int/long extrema) from floating-point narrowing to byte/short/char (first int, then integral narrowing discards all but low 8/16 bits; byte/short signed, char unsigned). Do not describe all integral destinations as clamping to their own extrema. Directly cite [JLS SE21 §5.1.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.3); [§5.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.2) only to preserve the boundary with widening. Preserve existing correct int examples and add worked two-step examples, including char conversion shown numerically. Avoid duplicate teaching in CORE-NUMBERS and CORE-OPERATORS; cross-reference existing material only as needed. No historical G1/audit edit.

03 proposed scope examples/core/ConversionsDemo.java; preserve existing source-file invocation and CHECK PASSED; add deterministic assertions for the actual disputed rule, not standalone Gradle module. If additional negative source files or CI change are justified, request 00 approval first; no change to runner semantics by default. Historical primary evidence from A02 B01 probe, NOT newly executed by 01:

| Expression | Expected Java21 result |
|---|---|
| (byte)300.0 | 44 |
| (byte)Double.POSITIVE_INFINITY | -1 |
| (short)Double.POSITIVE_INFINITY | -1 |
| (int)(char)Double.NEGATIVE_INFINITY | 0 |
| (int)(char)Double.POSITIVE_INFINITY | 65535 |
| (long)Double.NEGATIVE_INFINITY | -9223372036854775808 |
| (int)-3.9 | -3 |
| (byte)Double.NaN | 0 |

Also contrast direct illegal implicit narrowing (e.g. byte x = 300.0; separate complete class, javac --release 21 -XDrawDiagnostics expected type conversion rejection) with explicit valid cast. This is proposed regression, not actual diagnostics. Retain old negative evidence and note which cases are new local checks versus CI; do not count stdout as JUnit. Article must make the intermediate conversion and low-bit result independently understandable rather than merely table values.

## Dependencies and gates

B1 owns only JC-A02-B01 / R11 ERROR. [Nine other A02 findings](findings-A02.md) and five DEPTH rows remain OPEN, proposed D-A02 after B1 G6; no competing changes to conversions.md / ConversionsDemo.java. B2–B4 separate scope; other related requirements unaffected unless 00 authorizes explicit scope expansion. 02/03 coordinate one integrated author branch; technical PR targets author branch only by 00 instruction. 05 G3 clarity/links; 04 independent exact integrated HEAD, confirm JLS, regression and previous cases, no unaddressed blocker; 00 G5 guarded merge; 06 G6 exact published source and postmerge CI, update disposition with evidence as authorized. Re-audit R11 after publication; no Core GO/Verified claim. 01 planning only: local javac/java, negative checks, runner, GUI NOT RUN. No new CI result claimed here.
