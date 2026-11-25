# B3 — CORE-INIT: implicit super() and constructor applicability

**Предложение 00**, не разрешение на G2. Program #254 / R0 #255. Canonical [JC-A07-B01](findings-B.md), [CI-08 P0 ERROR](requirements-A07.md), [primary A07 L132](../../audits/java-core-full-2026-11-24/A07-CORE-INIT.md#L132). Frozen instructional source f04f1eb5ee3a400a988757414bcae09113bd885b. R0 baseline c7e223fd2230a776921fa142fab39ff7fbc019f2. Before assigning G2 coordinator checks live BASE and B2 G6, preserving access/inheritance/interface alignment; planning may proceed in parallel.

## Обязательный критерий

02: only knowledge/01-java-core/initialization.md (frozen L500–502, surrounding constructor chain L459–559). Replace misleading requirement for zero formal parameters with **a legal implicit invocation super() with zero actual arguments**: selected constructor may be variable-arity, applicable to zero actuals, accessible in this invocation context, unambiguous, and satisfy checked-exception requirements. Distinguish empty formal list, ordinary no-arg constructor (explicit or compiler-provided default) and invocation with zero arguments. Explicit Child(){} and implicitly declared default Child constructor are separate cases. Java21 ordinary class construction only; no change to first-statement rule in that language version, no blanket extension to enum/record/Object special cases or qualified superclass constructor invocation.

Normative anchors: [JLS21 §8.8.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.7), [§8.8.7.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.7.1), [§8.8.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.9), [§15.12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.2), [§6.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6), [§11.2.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2.3). Distinguish class-default-constructor accessibility and exception rules from explicitly declared constructors. For an ordinary default constructor JLS §8.8.9 explicitly states it has no throws clause and a compile-time error condition concerning accessible no-argument superclass constructor with no throws clause; discuss precise applicability in light of the **actual varargs acceptance** from A07, not claim spec wording literally says zero-formal variable-arity constructor is no-arg. 04 should independently check the wording and compiler observation; if tension remains, report it explicitly, not infer portability solely from one javac run.

## Planned regression and historical evidence

Positive (complete independent source; can integrate a small labelled probe if 00 authorizes):
```java
class Parent {
    Parent(int... xs) { System.out.println(xs.length); }
}
class Explicit extends Parent { Explicit() {} }
class Implicit extends Parent {}
class Main {
    public static void main(String[] args) {
        new Explicit();
        new Implicit();
    }
}
```
Expected historical observation two lines 0 / 0; A07 L97 recorded both actual positive cases. No new run by 01. Explain synthesized empty varargs array; no guarantee on unrelated constructors.

| Isolated case | Proposed criterion | Evidence |
|---|---|---|
| Parent(int), Child(){} | reject implicit super(): no applicable constructor | historical no_super: cant.apply.symbol / arg.length.mismatch |
| Parent(String...), Parent(Integer...), Child(){} | reject ambiguity at zero actual arguments | historical ambiguous_super: ref.ambiguous |
| Parent() throws IOException; Child(){} | reject unhandled checked exception | historical checked_super: unreported.exception.need.to.catch.or.throw |
| Parent private constructor in separate top-level class; Child(){} | reject accessibility (avoid nestmate false positive) | new proposed negative; exact diagnostic NOT RUN |
| Parent() throws IOException; Child() throws IOException {} | positive explicit declaration, caller handles exception | new proposed positive; NOT RUN |
| Preserve original first-statement/cyclic this negative and construction order | no regression, existing examples intact | historical A07 probes; re-run only as authorized |

03 proposed scope examples/core/initialization/ (actual src, expected.stdout, verify.sh, README; no original negative directory). Original runner compiles Java21 and compares 29 stdout lines, CHECK PASSED; not 29 independent tests. Preserve existing sequence and comparison. Prefer isolated local javac --release 21 -XDrawDiagnostics and java in temporary directories for new cases; tracked runner/fixture changes only if approved by 00. No mandatory new module, JUnit or CI. Distinguish failure diagnostic category from exact wording, complete separate source for each case, ensure failure reason is intended. 05 G3, 04 independent G4 exact integrated SHA, 00 G5 guarded merge, 06 G6 published exact postmerge evidence. 02/03 coordinate one author branch as 00 directs.

## Boundaries and collision control

B3 owns B01 only; all N01–N10 remain OPEN D-A07 after B3 G6, including CI-08 N05 (argument evaluation, abrupt completion, Object root/qualified invocation). Cross A04-N03/A05-N01 and A25.2 constructor/serialization cluster; do not silently alter serialization-specific no-arg requirements or historical plans. No claim B01 fixed or CI-08 fully satisfied until appropriate checks and separate N05 review; Q re-audit final row. Local Java/negative/runner/GUI NOT RUN by 01; source-based brief and historical audit only. No teaching change in R0.
