#!/usr/bin/env bash
# 校验 Release APK 内含生产可观测实现类（未引入外部服务）。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK_DIR="$ROOT/app/build/outputs/apk/release"

if [[ ! -d "$APK_DIR" ]]; then
  echo "Release APK directory not found: $APK_DIR (run :app:assembleRelease first)" >&2
  exit 1
fi

APK="$(find "$APK_DIR" -maxdepth 1 -name '*.apk' | head -n 1)"
if [[ -z "$APK" ]]; then
  echo "No release APK under $APK_DIR" >&2
  exit 1
fi

echo "Checking release APK: $APK"

REQUIRED_MARKERS=(
  "ReleaseTaskFlowAnalytics"
  "ReleaseTaskFlowPerformanceReporter"
  "ReleaseTaskFlowCrashReporter"
  "TaskFlowLocalLogStore"
)

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

unzip -q -o "$APK" "classes*.dex" -d "$TMP_DIR"

DEX_FILES=( "$TMP_DIR"/classes*.dex )
if [[ ! -e "${DEX_FILES[0]}" ]]; then
  echo "Failed to extract classes*.dex from APK" >&2
  exit 1
fi

MISSING=0
for marker in "${REQUIRED_MARKERS[@]}"; do
  FOUND=0
  for dex in "${DEX_FILES[@]}"; do
    if strings "$dex" | grep -q "$marker"; then
      FOUND=1
      break
    fi
  done
  if [[ "$FOUND" -eq 0 ]]; then
    echo "Missing release observability marker in DEX: $marker" >&2
    MISSING=1
  else
    echo "OK: $marker"
  fi
done

if [[ "$MISSING" -ne 0 ]]; then
  exit 1
fi

echo "Release observability artifact check passed."
