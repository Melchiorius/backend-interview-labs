#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
export LC_ALL=C
work=$(mktemp -d)
trap 'rm -rf -- "$work"' EXIT
mkdir -p "$work/positive"
javac --release 21 -encoding UTF-8 -Xlint:all -d "$work/positive" src/SequentialIteratorProbe.java
java -cp "$work/positive" SequentialIteratorProbe > "$work/actual.stdout"
diff -u expected.stdout "$work/actual.stdout"
for case in N01 N02; do
  mkdir -p "$work/$case"
  status=0
  javac --release 21 -encoding UTF-8 -XDrawDiagnostics -d "$work/$case" "negative/$case.java" > "$work/$case.log" 2>&1 || status=$?
  cat "$work/$case.log"
  printf '%s exit=%s\n' "$case" "$status"
  if [[ "$case" == N01 ]]; then
    diagnostic=compiler.err.foreach.not.applicable.to.type
  else
    diagnostic=compiler.err.cant.resolve.location.args
  fi
  if [[ "$status" != 1 ]] || ! grep -Fq "$diagnostic" "$work/$case.log"; then
    echo "Unexpected compiler result for $case" >&2
    exit 1
  fi
done
printf 'PASS: %s stdout lines, 2 isolated compiler-negative cases\n' "$(wc -l < "$work/actual.stdout" | tr -d ' ')"
