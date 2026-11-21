#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../../.." && pwd)"
example="$root/examples/core/generics-erasure"
build="$example/build"
rm -rf "$build"
mkdir -p "$build/classes"
# This approved source intentionally emits javac [varargs] and [unchecked] warnings.
# Warning wording is compiler-dependent; only stdout is compared verbatim.
javac --release 21 -Xlint:unchecked -Xlint:varargs -d "$build/classes" "$example/src/GenericsProbes.java"
java -cp "$build/classes" GenericsProbes > "$build/actual.stdout"
diff -u "$example/expected.stdout" "$build/actual.stdout"
cat "$build/actual.stdout"
echo "CHECK PASSED: stdout matches expected.stdout"
