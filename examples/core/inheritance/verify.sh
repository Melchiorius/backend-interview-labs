#!/usr/bin/env bash
set -euo pipefail

# Each intentionally invalid compilation is independent of every other one.
root="$(cd "$(dirname "$0")/../../.." && pwd)"
example="$root/examples/core/inheritance"
classes="$example/build/classes"
rm -rf "$example/build"
mkdir -p "$classes"

javac --release 21 -d "$classes" -sourcepath "$example/src" "$example/src/client/InheritanceDemo.java"
java -cp "$classes" client.InheritanceDemo

# Expected English OpenJDK 21 messages, not JLS-guaranteed diagnostic wording.
# BadExtendsFinal: cannot inherit from final
# BadOverrideFinal: overridden method is final
# BadHideFinalStatic: overridden method is static,final
# BadStaticForInstance: overriding method is static
# BadInstanceForStatic: overridden method is static
# BadOverrideOverload: method does not override or implement
# BadCovariantReturn: return type Object is not compatible with Number
# BadCheckedThrows: overridden method does not throw Exception
# BadNarrowAccess: attempting to assign weaker access privileges
# BadOverridePrivate: method does not override or implement
# BadOverridePackage: method does not override or implement
# BadReturnOnlyOverload: method convert(Object) is already defined
for name in BadExtendsFinal BadOverrideFinal BadHideFinalStatic BadStaticForInstance BadInstanceForStatic BadOverrideOverload BadCovariantReturn BadCheckedThrows BadNarrowAccess BadOverridePrivate BadOverridePackage BadReturnOnlyOverload; do
  case "$name" in
    BadExtendsFinal) expected='cannot inherit from final';;
    BadOverrideFinal) expected='overridden method is final';;
    BadHideFinalStatic) expected='overridden method is static,final';;
    BadStaticForInstance) expected='overriding method is static';;
    BadInstanceForStatic) expected='overridden method is static';;
    BadOverrideOverload) expected='method does not override or implement';;
    BadCovariantReturn) expected='return type Object is not compatible with Number';;
    BadCheckedThrows) expected='overridden method does not throw Exception';;
    BadNarrowAccess) expected='attempting to assign weaker access privileges';;
    BadOverridePrivate) expected='method does not override or implement';;
    BadOverridePackage) expected='method does not override or implement';;
    BadReturnOnlyOverload) expected='method convert(Object) is already defined';;
  esac
  output="$example/build/negative/$name"
  mkdir -p "$output"
  if javac --release 21 -cp "$classes" -d "$output" "$example/negative/$name.java" \
      >"$output/stdout.log" 2>"$output/stderr.log"; then
    echo "UNEXPECTED COMPILE SUCCESS: $name" >&2
    exit 1
  fi
  if ! grep -q 'error:' "$output/stderr.log" || ! grep -Fq -- "$expected" "$output/stderr.log"; then
    echo "WRONG COMPILER REJECTION REASON: $name (expected: $expected)" >&2
    cat "$output/stderr.log" >&2
    exit 1
  fi
  echo "EXPECTED REJECTION: $name"
  cat "$output/stderr.log"
done
echo "ALL EXPECTED OUTCOMES OBSERVED"
