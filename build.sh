#!/bin/bash
# ─────────────────────────────────────────────────────────────────
#  build.sh  —  Compile & run the Stock Candlestick Analyser
#  Requirements: Java 17+   (no other dependencies needed)
# ─────────────────────────────────────────────────────────────────
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

SRC_DIR="src"
OUT_DIR="out"

mkdir -p "$OUT_DIR"
echo "Compiling..."
find "$SRC_DIR" -name "*.java" > sources.txt
javac -sourcepath "$SRC_DIR" -d "$OUT_DIR" @sources.txt
rm sources.txt
echo "Compiled OK — Launching..."
java -cp "$OUT_DIR" Main
