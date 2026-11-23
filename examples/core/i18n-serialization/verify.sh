#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf -- "$WORK"' EXIT

for utility in javac java diff wc sha256sum cut; do
  command -v "$utility" >/dev/null || {
    echo "FAIL: missing executable: $utility" >&2
    exit 1
  }
done

mkdir -p "$WORK/classes"
if ! javac --release 21 -encoding UTF-8 -Xlint:all -d "$WORK/classes" \
    "$ROOT/src/I18nProbe.java" "$ROOT/src/SerializationProbe.java" \
    2>"$WORK/javac.stderr"; then
  cat "$WORK/javac.stderr" >&2
  echo 'FAIL: javac compilation' >&2
  exit 1
fi
if [[ -s "$WORK/javac.stderr" ]]; then
  cat "$WORK/javac.stderr" >&2
  echo 'FAIL: javac emitted warnings or diagnostics' >&2
  exit 1
fi

verify() {
  local label="$1"
  local class_name="$2"
  local expected_lines="$3"
  local expected_sha="$4"
  shift 4
  local expected="$ROOT/$label.expected.stdout"
  local actual="$WORK/$label.actual.stdout"
  local errors="$WORK/$label.jvm.stderr"

  if ! java "$@" -cp "$WORK/classes" "$class_name" >"$actual" 2>"$errors"; then
    cat "$errors" >&2
    echo "FAIL: JVM $class_name" >&2
    exit 1
  fi
  if [[ -s "$errors" ]]; then
    cat "$errors" >&2
    echo "FAIL: JVM stderr for $class_name" >&2
    exit 1
  fi

  diff -u "$expected" "$actual"

  local lines actual_sha fixture_sha
  lines="$(wc -l <"$actual")"
  [[ "$lines" -eq "$expected_lines" ]] || {
    echo "FAIL: $label has $lines stdout lines; expected $expected_lines" >&2
    exit 1
  }
  actual_sha="$(sha256sum "$actual" | cut -d ' ' -f1)"
  fixture_sha="$(sha256sum "$expected" | cut -d ' ' -f1)"
  [[ "$actual_sha" == "$expected_sha" && "$fixture_sha" == "$expected_sha" ]] || {
    echo "FAIL: $label SHA-256 actual=$actual_sha fixture=$fixture_sha accepted_G1=$expected_sha" >&2
    exit 1
  }
}

verify i18n I18nProbe 29 620ac75eb8c4baf0366e17ebd89384fd51b1b505007bceabf9b4c7cf9213b332 \
  -Djava.locale.providers=CLDR -Dsun.stdout.encoding=UTF-8
verify serial SerializationProbe 20 fa00d160ce87c1c241f42948140086edd43636e3b30949e27de97affb6535608 \
  -Dsun.stdout.encoding=UTF-8

echo 'CHECK PASSED: Java 21 I18N/SERIAL; 1 clean javac, 2 trusted-only JVM processes, 29+20=49 stdout lines, exact G1 SHA-256'
