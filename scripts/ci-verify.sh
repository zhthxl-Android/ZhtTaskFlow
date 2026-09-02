#!/usr/bin/env bash
# 本地 / CI 强制校验（Linux / macOS / Git Bash）。与 .github/workflows/ci.yml 对齐。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

chmod +x gradlew 2>/dev/null || true

echo "==> clean :app:compileDebugKotlin"
./gradlew clean :app:compileDebugKotlin --no-daemon

echo "==> :app:lintVitalRelease"
./gradlew :app:lintVitalRelease --no-daemon

echo "==> :component_nav:testDebugUnitTest"
./gradlew :component_nav:testDebugUnitTest --no-daemon

echo "==> Release assemble + observability artifact check"
./gradlew :app:assembleRelease --no-daemon
bash scripts/verify-release-observability.sh

echo "CI verify: all steps passed."
