@echo off
title Secure Banking Application - Robert Kadyamusuma (H250298W)
color 0B

cd /d "%~dp0"

REM Check if JAR exists, if not, check if bin exists; otherwise compile
if not exist "SecureBankApp.jar" (
    if not exist "bin\com\securebank\Main.class" (
        echo ===============================================================
        echo  First-time setup: Compiling Java source files...
        echo ===============================================================
        if not exist "bin" mkdir bin
        javac -d bin -sourcepath src src\com\securebank\Main.java
        if %ERRORLEVEL% NEQ 0 (
            echo.
            echo [!] Compilation failed. Please make sure JDK is installed.
            pause
            exit /b 1
        )
    )
    java -cp bin com.securebank.Main
) else (
    java -jar SecureBankApp.jar
)

echo.
echo Press any key to exit...
pause >nul
