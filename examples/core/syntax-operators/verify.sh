#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf -- "$OUT"' EXIT

if ! javac --release 21 -Xlint:all -d "$OUT" "$ROOT/src/SyntaxProbe.java" "$ROOT/src/OperatorsProbe.java" "$ROOT/src/p/Helper.java" "$ROOT/src/demo/PackageProbe.java" 2> "$OUT/javac.stderr"; then
  cat "$OUT/javac.stderr" >&2
  exit 1
fi
if [[ -s "$OUT/javac.stderr" ]]; then
  cat "$OUT/javac.stderr" >&2
  echo 'FAIL: javac emitted warnings or diagnostics' >&2
  exit 1
fi
for entry in "SyntaxProbe syntax" "OperatorsProbe operators" "demo.PackageProbe package"; do
  read -r class_name fixture <<< "$entry"
  java -cp "$OUT" "$class_name" > "$OUT/$fixture.actual.stdout" 2> "$OUT/$fixture.stderr"
  if [[ -s "$OUT/$fixture.stderr" ]]; then
    cat "$OUT/$fixture.stderr" >&2
    echo "FAIL: JVM wrote stderr: $class_name" >&2
    exit 1
  fi
  diff -u "$ROOT/$fixture.expected.stdout" "$OUT/$fixture.actual.stdout"
done

echo 'CHECK PASSED: Java 21 syntax/operators/package; 14+27+3 stdout lines (three JVM processes)'
