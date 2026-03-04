#!/usr/bin/env bash
set -e
JAR="funcc.jar"
echo "Building Fun compiler..."
echo "Compiling..."
./antlr.sh
javac -d out src/ast/*.java
javac -d out src/util/*.java
javac -d out -cp "out:lib/antlr.jar:lib/SVM.jar" src/antlr/*.java
javac -d out -cp "out:lib/antlr.jar:lib/SVM.jar" src/typecheck/*.java
javac -d out -cp "out:lib/antlr.jar:lib/SVM.jar" src/interp/*.java
javac -d out -cp "out:lib/antlr.jar:lib/SVM.jar" src/ir/*.java
javac -d out -cp "out:lib/antlr.jar:lib/SVM.jar" src/*.java
echo "Packaging fat JAR..."
STAGE="$(mktemp -d)"
trap "rm -rf $STAGE" EXIT
for jar in lib/*.jar; do
    unzip -q -o "$jar" -d "$STAGE" -x "META-INF/*" 2>/dev/null || true
done
cp -r out/. "$STAGE"/
mkdir -p "$STAGE/META-INF"
printf 'Manifest-Version: 1.0\nMain-Class: Main\n' > "$STAGE/META-INF/MANIFEST.MF"
jar cfm "$JAR" "$STAGE/META-INF/MANIFEST.MF" -C "$STAGE" .
echo "Done — run with ./funcc <source.fun>"