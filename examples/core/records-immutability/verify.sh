#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/../../.." && pwd)"
example="$root/examples/core/records-immutability"
build="$example/build"
rm -rf "$build"
mkdir -p "$build/classes"

if ! javac --release 21 -Xlint:all -d "$build/classes" \
  "$example/src/RecordsProbe.java" \
  "$example/src/ImmutableProbe.java" 2>"$build/javac.stderr"; then
  cat "$build/javac.stderr" >&2
  exit 1
fi
if [[ -s "$build/javac.stderr" ]]; then
  echo "CHECK FAILED: javac emitted diagnostics (expected zero warnings)" >&2
  cat "$build/javac.stderr" >&2
  exit 1
fi

java -cp "$build/classes" RecordsProbe > "$build/records.actual.stdout"
java -cp "$build/classes" ImmutableProbe > "$build/immutable.actual.stdout"
diff -u "$example/records.expected.stdout" "$build/records.actual.stdout"
diff -u "$example/immutable.expected.stdout" "$build/immutable.actual.stdout"
cat "$build/records.actual.stdout"
cat "$build/immutable.actual.stdout"
echo "CHECK PASSED: both stdout files match expected; javac emitted zero warnings"
