#!/bin/bash
# =========================================================================
# Secure Banking Application - Build and Execution Script
# Author : Robert Kadyamusuma
# Reg No : H250298W
# =========================================================================

set -e

echo "[1/3] Creating output directory..."
mkdir -p bin

echo "[2/3] Compiling Java source files..."
javac -d bin -sourcepath src src/com/securebank/Main.java

echo "[3/3] Launching Secure Banking Application..."
echo ""
java -cp bin com.securebank.Main
