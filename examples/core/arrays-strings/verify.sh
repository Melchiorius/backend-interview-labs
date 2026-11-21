#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/../../.." && pwd)"
example="$root/examples/core/arrays-strings"
build="$example/build"
rm -rf "$build"
mkdir -p "$build/classes"

javac --release 21 -d "$build/classes" \
  "$example/src/ArraysG1.java" \
  "$example/src/StringsG1.java"
java -cp "$build/classes" ArraysG1 > "$build/arrays.actual.stdout"
java -cp "$build/classes" StringsG1 > "$build/strings.actual.stdout"
diff -u "$example/arrays.expected.stdout" "$build/arrays.actual.stdout"
diff -u "$example/strings.expected.stdout" "$build/strings.actual.stdout"
cat "$build/arrays.actual.stdout"
cat "$build/strings.actual.stdout"
echo "CHECK PASSED: both stdout files match expected"
