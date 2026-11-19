#!/usr/bin/env bash
set -euo pipefail

# Compile and run the positive example, then compile each negative case alone.
# A failing javac exit code must still be interpreted against the source/JLS.
repo_root="$(cd "$(dirname "$0")/../../.." && pwd)"
example="$repo_root/examples/core/access"
build="$example/build"
classes="$build/classes"

rm -rf "$build"
mkdir -p "$classes"
printf '%s\n' '== Positive case =='
javac --release 21 -d "$classes" -sourcepath "$example/src" "$example/src/client/AccessDemo.java"
java -cp "$classes" client.AccessDemo

printf '%s\n' '== Independent negative compilation cases =='
for name in \
    BadParentValue BadParentCast BadSibling BadPackageValue \
    BadNewParent BadStaticUnrelated \
    BadPrivateInterfaceMember BadProtectedInterfaceMember; do
    mkdir -p "$build/negative/$name"
    if javac --release 21 -cp "$classes" -d "$build/negative/$name" \
        "$example/negative/$name.java" >"$build/negative/$name.stdout" 2>"$build/negative/$name.stderr"; then
        echo "UNEXPECTED SUCCESS: $name" >&2
        exit 1
    fi
    if [[ ! -s "$build/negative/$name.stderr" ]]; then
        echo "FAILED WITHOUT COMPILER DIAGNOSTICS: $name" >&2
        exit 1
    fi
    printf 'EXPECTED REJECTION: %s\n' "$name"
    cat "$build/negative/$name.stderr"
done
printf '%s\n' 'ALL EXPECTED OUTCOMES OBSERVED'
