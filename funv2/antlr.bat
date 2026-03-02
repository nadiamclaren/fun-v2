@echo off
if not exist src\antlr mkdir src\antlr
java -jar lib/antlr.jar -no-listener -visitor -package antlr -o src/antlr Fun.g4
