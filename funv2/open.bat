@echo off
if "%~1"=="" (
    echo usage: open.bat ^<source.fun^>
    exit /b 1
)

set SOURCE=%~1
set RARS=lib\rars1_6.jar

if not exist outputs mkdir outputs
set ASM=outputs\%~n1.s

if not exist "%RARS%" (
    echo error: rars1_6.jar not found at %RARS%
    exit /b 1
)

echo Compiling %SOURCE%...
call funcc.bat "%SOURCE%" -o "%ASM%"
if errorlevel 1 exit /b 1

echo Opening %ASM% in RARS...
java -jar "%RARS%"