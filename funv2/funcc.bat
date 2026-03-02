@echo off
REM If -o flag is not provided, default output to outputs\<filename>.s
echo %* | findstr /c:"-o" >nul
if errorlevel 1 (
    if not exist outputs mkdir outputs
    for %%f in (%1) do (
        java -cp "out;lib/antlr.jar;lib/SVM.jar" Main %* -o outputs\%%~nf.s
    )
) else (
    java -cp "out;lib/antlr.jar;lib/SVM.jar" Main %*
)