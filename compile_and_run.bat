@echo off
REM =========================================================================
REM Secure Banking Application - Build and Execution Script
REM Author : Robert Kadyamusuma
REM Reg No : H250298W
REM =========================================================================

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
