@echo off
REM =========================================================================
REM Secure Banking Application - Build and Execution Script
REM Author : Robert Kadyamusuma
REM Reg No : H250298W
REM =========================================================================

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

echo [1/3] Creating output directory...
if not exist "bin" mkdir bin

echo [2/3] Compiling Java source files...
javac -d bin -sourcepath src src\com\securebank\Main.java
if %ERRORLEVEL% NEQ 0 (
    echo [!] Compilation failed! Please check Java installation (JDK 17+ recommended).
    pause
    exit /b %ERRORLEVEL%
)

echo [3/3] Launching Secure Banking Application...
echo.
java -cp bin com.securebank.Main
pause
