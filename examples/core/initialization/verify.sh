#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/../../.." && pwd)"
example="$root/examples/core/initialization"
build="$example/build"

rm -rf "$build"
mkdir -p "$build/classes"
javac --release 21 -d "$build/classes" "$example/src/InitializationDemo.java"
java -cp "$build/classes" InitializationDemo > "$build/actual.stdout"
diff -u "$example/expected.stdout" "$build/actual.stdout"
cat "$build/actual.stdout"

# Independent implicit-super contrast; keep the original fixture unchanged.
mkdir -p "$build/implicit-super-classes"
javac --release 21 -XDrawDiagnostics -d "$build/implicit-super-classes" "$example/src/ImplicitSuperDemo.java"
java -cp "$build/implicit-super-classes" ImplicitSuperDemo > "$build/implicit-super.actual.stdout"
printf '%s\n' 0 0 > "$build/implicit-super.expected.stdout"
diff -u "$build/implicit-super.expected.stdout" "$build/implicit-super.actual.stdout"
cat "$build/implicit-super.actual.stdout"

echo "CHECK PASSED: stdout matches expected.stdout"
