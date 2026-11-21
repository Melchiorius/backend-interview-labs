#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/../../.." && pwd)"
example="$root/examples/core/streams"
build="$example/build"
rm -rf "$build"
mkdir -p "$build/classes"

javac --release 21 -d "$build/classes" \
  "$example/src/StreamPipelineProbes.java" \
  "$example/src/StreamG1Clarifications.java"
java -cp "$build/classes" StreamPipelineProbes > "$build/actual.stdout"
java -cp "$build/classes" StreamG1Clarifications >> "$build/actual.stdout"
diff -u "$example/expected.stdout" "$build/actual.stdout"
cat "$build/actual.stdout"
echo "CHECK PASSED: stdout matches expected.stdout"
