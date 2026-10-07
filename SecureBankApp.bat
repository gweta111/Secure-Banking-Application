@echo off
title Secure Banking Application - Robert Kadyamusuma (H250298W)
color 0B

cd /d "%~dp0"

REM Auto-detect Java runtime if not on standard PATH
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    if exist "%USERPROFILE%\jdk\jdk-21.0.12.1+1\bin\java.exe" set "PATH=%USERPROFILE%\jdk\jdk-21.0.12.1+1\bin;%PATH%"
    if exist "C:\Program Files\JetBrains\PyCharm 2026.1.2\jbr\bin\java.exe" set "PATH=C:\Program Files\JetBrains\PyCharm 2026.1.2\jbr\bin;%PATH%"
)
where javac >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    if exist "%USERPROFILE%\jdk\jdk-21.0.12.1+1\bin\javac.exe" set "PATH=%USERPROFILE%\jdk\jdk-21.0.12.1+1\bin;%PATH%"
    if exist "C:\Program Files\JetBrains\PyCharm 2026.1.2\jbr\bin\javac.exe" set "PATH=C:\Program Files\JetBrains\PyCharm 2026.1.2\jbr\bin;%PATH%"
)

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
