#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/../../.." && pwd)"
example="$root/examples/core/interfaces-composition"
build="$example/build"
rm -rf "$build"
mkdir -p "$build/classes"

javac --release 21 -d "$build/classes" -sourcepath "$example/src" \
  "$example/src/client/InterfaceCompositionDemo.java"
java -cp "$build/classes" client.InterfaceCompositionDemo > "$build/actual.stdout"
diff -u "$example/expected.stdout" "$build/actual.stdout"
cat "$build/actual.stdout"
echo "CHECK PASSED: stdout matches expected.stdout"
