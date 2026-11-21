#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/../../.." && pwd)"
example="$root/examples/core/generics-erasure"
build="$example/build"
rm -rf "$build"
mkdir -p "$build/classes"

javac --release 21 -Xlint:all -Werror -d "$build/classes" "$example/src/GenericsErasureDemo.java"
java -cp "$build/classes" GenericsErasureDemo > "$build/actual.stdout"
diff -u "$example/expected.stdout" "$build/actual.stdout"
cat "$build/actual.stdout"
echo "CHECK PASSED: stdout matches expected.stdout"
