#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf -- "$WORK"' EXIT

compile() {
  local label="$1"
  shift
  if ! javac --release 21 -Xlint:all "$@" 2>"$WORK/$label.javac.stderr"; then
    cat "$WORK/$label.javac.stderr" >&2
    echo "FAIL: javac $label" >&2
    exit 1
  fi
  if [[ -s "$WORK/$label.javac.stderr" ]]; then
    cat "$WORK/$label.javac.stderr" >&2
    echo "FAIL: javac emitted warnings/diagnostics: $label" >&2
    exit 1
  fi
}

run() {
  local label="$1"
  local expected="$2"
  shift 2
  if ! java "$@" >"$WORK/$label.actual.stdout" 2>"$WORK/$label.jvm.stderr"; then
    cat "$WORK/$label.jvm.stderr" >&2
    echo "FAIL: JVM $label" >&2
    exit 1
  fi
  if [[ -s "$WORK/$label.jvm.stderr" ]]; then
    cat "$WORK/$label.jvm.stderr" >&2
    echo "FAIL: JVM wrote to stderr: $label" >&2
    exit 1
  fi
  diff -u "$expected" "$WORK/$label.actual.stdout"
}

mkdir -p "$WORK/modules.base" "$WORK/modules.opened" "$WORK/modules.static" "$WORK/modules.static-runtime" "$WORK/loader.classes"

# JPMS: two independent named-module compilation contexts (3 modules each).
compile base -d "$WORK/modules.base" --module-source-path "$ROOT/modules/base/src" -m m.lib,m.mid,m.client
run base "$ROOT/modules/base/expected.stdout" --module-path "$WORK/modules.base" -m m.client/client.Main

compile opened -d "$WORK/modules.opened" --module-source-path "$ROOT/modules/opened/src" -m m.lib,m.mid,m.client
run opened "$ROOT/modules/opened/expected.stdout" --module-path "$WORK/modules.opened" -m m.client/client.Main

# One requires-static compilation; two independent module-resolution JVM contexts.
compile static -d "$WORK/modules.static" --module-source-path "$ROOT/modules/static/src" -m m.opt,m.consumer
cp -R "$WORK/modules.static/m.consumer" "$WORK/modules.static-runtime/"
run static-absent "$ROOT/modules/static/absent.expected.stdout" \
  --module-path "$WORK/modules.static-runtime" -m m.consumer/consumer.StaticMain
run static-present "$ROOT/modules/static/present.expected.stdout" \
  --module-path "$WORK/modules.static" --add-modules m.opt -m m.consumer/consumer.StaticMain

# ClassLoader identity, explicit initialization and missing-superclass runtime copy.
compile classloader -d "$WORK/loader.classes" \
  "$ROOT/classloaders/src/LoaderProbe.java" \
  "$ROOT/classloaders/src/MissingProbe.java" \
  "$ROOT/classloaders/src/sample/Token.java" \
  "$ROOT/classloaders/src/sample/Lazy.java" \
  "$ROOT/classloaders/src/sample/Parent.java" \
  "$ROOT/classloaders/src/sample/Dependent.java"
run loader "$ROOT/classloaders/loader.expected.stdout" \
  -cp "$WORK/loader.classes" LoaderProbe "$WORK/loader.classes/sample/Token.class"
cp -R "$WORK/loader.classes" "$WORK/loader.bad"
rm -- "$WORK/loader.bad/sample/Parent.class"
run missing "$ROOT/classloaders/missing.expected.stdout" \
  -cp "$WORK/loader.bad" MissingProbe

echo 'CHECK PASSED: Java 21 JPMS/ClassLoader; 4 compilations, 6 JVM processes, 11+11+2+2+21+2=49 stdout lines'
