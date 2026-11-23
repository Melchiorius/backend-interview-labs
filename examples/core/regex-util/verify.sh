#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf -- "$WORK"' EXIT

for utility in javac java diff wc sha256sum cut; do
  command -v "$utility" >/dev/null || {
    echo "FAIL: required executable not found: $utility" >&2
    exit 1
  }
done

mkdir -p "$WORK/classes"
if ! javac --release 21 -encoding UTF-8 -Xlint:all -d "$WORK/classes" \
    "$ROOT/src/RegexProbe.java" "$ROOT/src/UtilProbe.java" \
    2>"$WORK/javac.stderr"; then
  cat "$WORK/javac.stderr" >&2
  echo 'FAIL: javac compilation' >&2
  exit 1
fi
if [[ -s "$WORK/javac.stderr" ]]; then
  cat "$WORK/javac.stderr" >&2
  echo 'FAIL: javac warnings or stderr' >&2
  exit 1
fi

verify() {
  local label="$1"
  local java_class="$2"
  local expected_lines="$3"
  local reference_sha="$4"
  local expected="$ROOT/$label.expected.stdout"
  local actual="$WORK/$label.actual.stdout"
  local errors="$WORK/$label.java.stderr"

  if ! java -Dfile.encoding=UTF-8 -cp "$WORK/classes" "$java_class" >"$actual" 2>"$errors"; then
    cat "$errors" >&2
    echo "FAIL: JVM $java_class" >&2
    exit 1
  fi
  if [[ -s "$errors" ]]; then
    cat "$errors" >&2
    echo "FAIL: JVM stderr: $java_class" >&2
    exit 1
  fi

  diff -u "$expected" "$actual"

  local lines actual_sha fixture_sha
  lines="$(wc -l <"$actual")"
  [[ "$lines" -eq "$expected_lines" ]] || {
    echo "FAIL: $label stdout has $lines lines, expected $expected_lines" >&2
    exit 1
  }
  actual_sha="$(sha256sum "$actual" | cut -d ' ' -f1)"
  fixture_sha="$(sha256sum "$expected" | cut -d ' ' -f1)"
  [[ "$actual_sha" == "$reference_sha" && "$fixture_sha" == "$reference_sha" ]] || {
    echo "FAIL: $label stdout SHA-256 actual=$actual_sha fixture=$fixture_sha accepted_G1=$reference_sha" >&2
    exit 1
  }
}

verify regex RegexProbe 33 aee7f4eb5f2e17050773b3ec4a07033bbc866040a6bffcaef7d142ff0b693f12
verify util UtilProbe 38 57cff41cdf848efe2a0092c3fe938efd91e1a3f537adc1213327609253eddbb7

echo 'CHECK PASSED: Java 21 Regex/Util; 1 clean javac, 2 JVM processes, 33+38=71 stdout lines, exact G1 SHA-256'
