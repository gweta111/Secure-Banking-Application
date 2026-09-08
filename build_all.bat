@echo off
title Build All - Secure Banking Application
color 0A

cd /d "%~dp0"

echo ===============================================================
echo  BUILDING SECURE BANKING APPLICATION
echo  Author : Robert Kadyamusuma
echo  Reg No : H250298W
echo ===============================================================
echo.

echo [1/4] Creating output directories...
if not exist "bin" mkdir bin
if not exist "data" mkdir data

echo [2/4] Compiling Java classes...
javac -d bin -sourcepath src src\com\securebank\Main.java
if %ERRORLEVEL% NEQ 0 (
    echo [!] Java compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

echo [3/4] Packaging executable JAR (SecureBankApp.jar)...
where jar >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    jar --create --file SecureBankApp.jar --main-class com.securebank.Main -C bin com
) else (
    if exist "C:\Program Files\Java\jdk-26.0.1\bin\jar.exe" (
        "C:\Program Files\Java\jdk-26.0.1\bin\jar.exe" --create --file SecureBankApp.jar --main-class com.securebank.Main -C bin com
    ) else (
        echo [WARNING] jar utility not found, skipping JAR packaging.
    )
)

echo [4/4] Building native Windows executable (SecureBankApp.exe)...
if exist "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe" (
    "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe" /nologo /target:exe /out:SecureBankApp.exe /platform:anycpu launcher\Launcher.cs
) else (
    echo [WARNING] C# compiler not found, skipping EXE rebuild.
)

echo.
echo ===============================================================
echo  BUILD COMPLETE! 
echo  You can now launch the application by double-clicking:
echo    - SecureBankApp.exe (Native Windows binary)
echo    - SecureBankApp.bat (Batch launcher)
echo    - SecureBankApp.jar (Executable JAR)
echo ===============================================================
echo.
pause
