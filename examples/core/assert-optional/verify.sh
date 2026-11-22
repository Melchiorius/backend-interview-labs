#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
CLASSES="$(mktemp -d)"
trap 'rm -rf -- "$CLASSES"' EXIT

if ! javac --release 21 -Xlint:all -d "$CLASSES" \
  "$ROOT/src/AssertProbe.java" \
  "$ROOT/src/OptionalProbe.java" \
  2>"$CLASSES/javac.stderr"; then
  cat "$CLASSES/javac.stderr" >&2
  exit 1
fi
if [[ -s "$CLASSES/javac.stderr" ]]; then
  cat "$CLASSES/javac.stderr" >&2
  echo 'FAIL: javac emitted warnings/diagnostics' >&2
  exit 1
fi

java -cp "$CLASSES" AssertProbe >"$CLASSES/assert.default.actual" 2>"$CLASSES/assert.default.stderr"
[[ ! -s "$CLASSES/assert.default.stderr" ]] || { cat "$CLASSES/assert.default.stderr" >&2; exit 1; }
diff -u "$ROOT/AssertProbe.default.expected.stdout" "$CLASSES/assert.default.actual"

java -ea -cp "$CLASSES" AssertProbe >"$CLASSES/assert.ea.actual" 2>"$CLASSES/assert.ea.stderr"
[[ ! -s "$CLASSES/assert.ea.stderr" ]] || { cat "$CLASSES/assert.ea.stderr" >&2; exit 1; }
diff -u "$ROOT/AssertProbe.ea.expected.stdout" "$CLASSES/assert.ea.actual"

java -cp "$CLASSES" OptionalProbe >"$CLASSES/optional.actual" 2>"$CLASSES/optional.stderr"
[[ ! -s "$CLASSES/optional.stderr" ]] || { cat "$CLASSES/optional.stderr" >&2; exit 1; }
diff -u "$ROOT/OptionalProbe.expected.stdout" "$CLASSES/optional.actual"

echo 'CHECK PASSED: Java 21 assertions/Optional; default 15 + -ea 15 + Optional 36 stdout lines (3 JVM processes)'
