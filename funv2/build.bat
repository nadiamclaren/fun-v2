@echo off
echo Building Fun compiler...

echo Compiling...
call antlr.bat

javac -d out src\ast\*.java
if errorlevel 1 goto error

javac -d out src\util\*.java
if errorlevel 1 goto error

javac -d out -cp "out;lib\antlr.jar;lib\SVM.jar" src\antlr\*.java
if errorlevel 1 goto error

javac -d out -cp "out;lib\antlr.jar;lib\SVM.jar" src\typecheck\*.java
if errorlevel 1 goto error

javac -d out -cp "out;lib\antlr.jar;lib\SVM.jar" src\interp\*.java
if errorlevel 1 goto error

javac -d out -cp "out;lib\antlr.jar;lib\SVM.jar" src\ir\*.java
if errorlevel 1 goto error

javac -d out -cp "out;lib\antlr.jar;lib\SVM.jar" src\*.java
if errorlevel 1 goto error

echo Packaging fat JAR...

if exist staging rmdir /s /q staging
mkdir staging
mkdir staging\META-INF

pushd staging
for %%j in (..\lib\*.jar) do (
    jar xf %%j
)
popd

if exist staging\META-INF rmdir /s /q staging\META-INF
mkdir staging\META-INF

xcopy /s /e /q out\* staging\

echo Manifest-Version: 1.0> staging\META-INF\MANIFEST.MF
echo Main-Class: Main>> staging\META-INF\MANIFEST.MF

jar cfm funcc.jar staging\META-INF\MANIFEST.MF -C staging .
if errorlevel 1 goto error

rmdir /s /q staging

echo.
echo Done - run with: funcc ^<source.fun^>
goto end

:error
echo.
echo Build failed
exit /b 1

:end