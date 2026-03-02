#!/usr/bin/env bash
if [ -z "$1" ]; then
    echo "usage: ./simulate.sh <source.fun>"
    exit 1
fi

SOURCE="$1"
ASM="${SOURCE%.fun}.s"
RARS="lib/rars1_6.jar"

if [ ! -f "$RARS" ]; then
    echo "error: rars1_6.jar not found at $RARS"
    exit 1
fi

echo "Compiling $SOURCE..."
./funcc "$SOURCE" -o "$ASM" || exit 1

echo "Simulating $ASM..."
echo "---"
java -jar "$RARS" nc me 200000 "$ASM"