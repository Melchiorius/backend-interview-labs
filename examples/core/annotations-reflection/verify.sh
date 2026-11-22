#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf -- "$OUT"' EXIT

check_compile() {
  local label="$1"
  shift
  if ! javac --release 21 -Xlint:all "$@" 2>"$OUT/$label.javac.stderr"; then
    cat "$OUT/$label.javac.stderr" >&2
    echo "FAIL: javac failed: $label" >&2
    exit 1
  fi
  if [[ -s "$OUT/$label.javac.stderr" ]]; then
    cat "$OUT/$label.javac.stderr" >&2
    echo "FAIL: javac emitted warnings/diagnostics: $label" >&2
    exit 1
  fi
}

check_run() {
  local label="$1"
  local expected="$2"
  shift 2
  if ! java "$@" >"$OUT/$label.actual.stdout" 2>"$OUT/$label.jvm.stderr"; then
    cat "$OUT/$label.jvm.stderr" >&2
    echo "FAIL: JVM exited unsuccessfully: $label" >&2
    exit 1
  fi
  if [[ -s "$OUT/$label.jvm.stderr" ]]; then
    cat "$OUT/$label.jvm.stderr" >&2
    echo "FAIL: JVM wrote stderr: $label" >&2
    exit 1
  fi
  diff -u "$expected" "$OUT/$label.actual.stdout"
}

mkdir -p "$OUT/classes" "$OUT/mods.exportonly" "$OUT/mods.opens"

check_compile "plain" -d "$OUT/classes" \
  "$ROOT/src/AnnotationsProbe.java" "$ROOT/src/ReflectionProbe.java"
check_run "annotations" "$ROOT/annotations.expected.stdout" \
  -cp "$OUT/classes" AnnotationsProbe
check_run "reflection" "$ROOT/reflection.expected.stdout" \
  -cp "$OUT/classes" ReflectionProbe

check_compile "exportonly" -d "$OUT/mods.exportonly" \
  --module-source-path "$ROOT/jpms/exportonly/src" -m m.target,m.client
check_run "exportonly" "$ROOT/jpms/exportonly/expected.stdout" \
  --module-path "$OUT/mods.exportonly" -m m.client/client.Inspect

check_compile "opens" -d "$OUT/mods.opens" \
  --module-source-path "$ROOT/jpms/opens/src" -m m.target,m.client
check_run "opens" "$ROOT/jpms/opens/expected.stdout" \
  --module-path "$OUT/mods.opens" -m m.client/client.Inspect

echo 'CHECK PASSED: Java 21 annotations/reflection and JPMS exports/opens; 23+34+3+3 stdout lines (4 JVM processes)'
