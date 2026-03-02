@echo off
call antlr.bat
javac -d out src\ast\*.java
javac -d out src\util\*.java
javac -d out -cp "out;lib/antlr.jar;lib/SVM.jar" src\antlr\*.java
javac -d out -cp "out;lib/antlr.jar;lib/SVM.jar" src\typecheck\*.java
javac -d out -cp "out;lib/antlr.jar;lib/SVM.jar" src\interp\*.java
javac -d out -cp "out;lib/antlr.jar;lib/SVM.jar" src\ir\*.java
javac -d out -cp "out;lib/antlr.jar;lib/SVM.jar" src\*.java