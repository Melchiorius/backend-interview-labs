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

# Independent private-interface-member contrast; do not change expected.stdout.
mkdir -p "$build/private-classes"
javac --release 21 -XDrawDiagnostics -d "$build/private-classes" \
  "$example/src/client/InterfacePrivateAccessDemo.java"
java -cp "$build/private-classes" client.InterfacePrivateAccessDemo > "$build/private.actual.stdout"
printf '%s\n' 'nestPrivate=policy' > "$build/private.expected.stdout"
diff -u "$build/private.expected.stdout" "$build/private.actual.stdout"
cat "$build/private.actual.stdout"

# Match the intended reason, not merely a failed compilation.
for name in BadExternalPrivateAccess BadTypeQualifiedInstance; do
  case "$name" in
    BadExternalPrivateAccess) expected='compiler.err.report.access: helper(), private, client.InterfacePrivateAccessDemo.Policy';;
    BadTypeQualifiedInstance) expected='compiler.err.non-static.cant.be.ref: kindname.method, helper()';;
  esac
  output="$build/negative/$name"
  mkdir -p "$output"
  status=0
  javac --release 21 -XDrawDiagnostics -cp "$build/private-classes" -d "$output" \
    "$example/negative/$name.java" > "$output/stdout.log" 2> "$output/stderr.log" || status=$?
  if [[ "$status" != 1 ]] || [[ -s "$output/stdout.log" ]] || \
    ! grep -Fq -- "$expected" "$output/stderr.log" || \
    ! grep -Fxq '1 error' "$output/stderr.log"; then
    echo "UNEXPECTED COMPILER RESULT: $name (exit=$status)" >&2
    cat "$output/stdout.log" "$output/stderr.log" >&2
    exit 1
  fi
  echo "EXPECTED REJECTION: $name (exit=$status)"
  cat "$output/stderr.log"
done

echo "CHECK PASSED: stdout matches expected.stdout"
