#!/usr/bin/env bash
mkdir -p src/antlr
java -jar lib/antlr.jar -no-listener -visitor -package antlr -o src/antlr Fun.g4