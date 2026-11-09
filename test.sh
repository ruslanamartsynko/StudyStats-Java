#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out
javac --release 17 -encoding UTF-8 -d out src/main/java/ua/studystats/*.java src/test/java/ua/studystats/*.java
java -Dsun.stdout.encoding=UTF-8 -cp out ua.studystats.StatisticsTest
