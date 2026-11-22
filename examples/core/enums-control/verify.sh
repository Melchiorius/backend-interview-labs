#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$(mktemp -d)"
trap 'rm -rf "$BUILD_DIR"' EXIT

javac --release 21 -Xlint:all -d "$BUILD_DIR" \
  "$SCRIPT_DIR/src/EnumProbe.java" \
  "$SCRIPT_DIR/src/ControlProbe.java" 2> "$BUILD_DIR/javac.stderr"
if [[ -s "$BUILD_DIR/javac.stderr" ]]; then
  cat "$BUILD_DIR/javac.stderr" >&2
  printf '%s\n' 'javac emitted warnings or diagnostics' >&2
  exit 1
fi

java -cp "$BUILD_DIR" EnumProbe > "$BUILD_DIR/enum.actual.stdout" 2> "$BUILD_DIR/enum.stderr"
java -cp "$BUILD_DIR" ControlProbe > "$BUILD_DIR/control.actual.stdout" 2> "$BUILD_DIR/control.stderr"
for stderr_file in "$BUILD_DIR/enum.stderr" "$BUILD_DIR/control.stderr"; do
  if [[ -s "$stderr_file" ]]; then
    cat "$stderr_file" >&2
    printf '%s\n' 'Java process emitted stderr' >&2
    exit 1
  fi
done

diff -u "$SCRIPT_DIR/enum.expected.stdout" "$BUILD_DIR/enum.actual.stdout"
diff -u "$SCRIPT_DIR/control.expected.stdout" "$BUILD_DIR/control.actual.stdout"

printf '%s\n' 'CORE-ENUMS + CORE-CONTROL: PASS (30 + 21 stdout lines; 2 JVM processes)'
