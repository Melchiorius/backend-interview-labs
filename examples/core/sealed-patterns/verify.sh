#!/usr/bin/env bash
set -euo pipefail

example="$(cd "$(dirname "$0")" && pwd)"
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT

if ! javac --release 21 -Xlint:all -d "$classes" \
    "$example/src/SealedProbe.java" \
    "$example/src/PatternsProbe.java" 2>"$classes/javac.stderr"; then
  cat "$classes/javac.stderr" >&2
  exit 1
fi
if [[ -s "$classes/javac.stderr" ]]; then
  echo 'CHECK FAILED: javac emitted diagnostics (zero warnings expected)' >&2
  cat "$classes/javac.stderr" >&2
  exit 1
fi

java -cp "$classes" SealedProbe > "$classes/sealed.actual.stdout"
java -cp "$classes" PatternsProbe > "$classes/patterns.actual.stdout"
diff -u "$example/sealed.expected.stdout" "$classes/sealed.actual.stdout"
diff -u "$example/patterns.expected.stdout" "$classes/patterns.actual.stdout"

cat "$classes/sealed.actual.stdout"
cat "$classes/patterns.actual.stdout"
echo 'CHECK PASSED: 11 sealed + 20 patterns stdout lines; javac emitted zero diagnostics'
