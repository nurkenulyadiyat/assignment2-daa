#!/usr/bin/env bash
# Compile, test, benchmark and plot. Usage: ./run.sh
set -euo pipefail
cd "$(dirname "$0")"

rm -rf out
javac -d out src/*.java

echo "=== Tests ==="
java -cp out Tests

echo "=== Benchmark ==="
java -XX:+UseSerialGC -Xms1g -Xmx1g -cp out Benchmark results/tables | tee results/benchmark_log.txt

echo "=== Plots ==="
python3 scripts/plot_results.py
python3 scripts/update_readme.py
