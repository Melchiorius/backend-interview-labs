#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf -- "$WORK"' EXIT

for utility in javac java diff wc sha256sum cut; do
  command -v "$utility" >/dev/null || { echo "Missing tool: $utility" >&2; exit 1; }
done

mkdir -p "$WORK/classes"
if ! javac --release 21 -Xlint:all -d "$WORK/classes" \
    "$ROOT/src/NumbersProbe.java" "$ROOT/src/DateTimeProbe.java" \
    2>"$WORK/javac.stderr"; then
  cat "$WORK/javac.stderr" >&2
  echo "FAIL: javac" >&2
  exit 1
fi
if [[ -s "$WORK/javac.stderr" ]]; then
  cat "$WORK/javac.stderr" >&2
  echo "FAIL: javac warnings or diagnostics" >&2
  exit 1
fi

verify() {
  local label="$1"
  local class_name="$2"
  local required_lines="$3"
  local required_sha="$4"
  local expected="$ROOT/$label.expected.stdout"
  local actual="$WORK/$label.actual.stdout"
  local errors="$WORK/$label.jvm.stderr"

  if ! java -cp "$WORK/classes" "$class_name" >"$actual" 2>"$errors"; then
    cat "$errors" >&2
    echo "FAIL: JVM $class_name" >&2
    exit 1
  fi
  if [[ -s "$errors" ]]; then
    cat "$errors" >&2
    echo "FAIL: stderr $class_name" >&2
    exit 1
  fi
  diff -u "$expected" "$actual"

  local lines actual_sha fixture_sha
  lines="$(wc -l < "$actual")"
  [[ "$lines" -eq "$required_lines" ]] || { echo "FAIL: $label stdout lines $lines, expected $required_lines" >&2; exit 1; }
  actual_sha="$(sha256sum "$actual" | cut -d ' ' -f1)"
  fixture_sha="$(sha256sum "$expected" | cut -d ' ' -f1)"
  [[ "$actual_sha" == "$required_sha" && "$fixture_sha" == "$required_sha" ]] || {
    echo "FAIL: SHA-256 for $label; actual=$actual_sha fixture=$fixture_sha expected=$required_sha" >&2
    exit 1
  }
}

verify numbers NumbersProbe 33 9c9e8000292ee4face04197388feadb50ba8cdc1e7a9099c3b89ecbdd4b6ea83
verify datetime DateTimeProbe 38 71a4bb9934201ec65ba8e7d89658dd19a9b815d40a6c2218b6dd125d61b4d908

echo 'CHECK PASSED: Java 21 Numbers/DateTime; 1 clean javac, 2 JVM, 33+38=71 stdout lines, exact G1 SHA-256'
