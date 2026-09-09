<#
.SYNOPSIS
  向 TaskFlow 本地可观测日志仓灌入测试 JSONL（埋点 / 性能 / 崩溃），用于日志 Tab 压测。

.DESCRIPTION
  - 生成与 LocalLogStore.encodeRecord 兼容的 JSON Lines 文件。
  - 默认通过 adb + run-as 写入 Debug 包私有目录（不进入 Release 产物）。
  - 首次打开日志 Tab 时由应用按 .jsonl 自动重建 .idx 侧车索引。

.PARAMETER Count
  生成条数，默认 5000。

.PARAMETER PackageName
  应用包名，默认 com.example.zhttaskflow（须为 debuggable 以便 run-as）。

.PARAMETER ClearExisting
  写入前清空应用内 taskflow_observability/logs 下已有 .jsonl / .idx。

.PARAMETER GenerateOnly
  仅在本机输出种子文件到 build/log-seed/，不连接设备。

.PARAMETER OutputDir
  与 -GenerateOnly 联用时的输出目录。

.EXAMPLE
  .\scripts\generate_test_logs.ps1 -Count 5000

.EXAMPLE
  .\scripts\generate_test_logs.ps1 -GenerateOnly -Count 5000
#>
[CmdletBinding()]
param(
    [int]$Count = 5000,
    [string]$PackageName = "com.example.zhttaskflow",
    [switch]$ClearExisting,
    [switch]$GenerateOnly,
    [string]$OutputDir = ""
)

$ErrorActionPreference = "Stop"

function Resolve-AdbPath {
    if ($env:ANDROID_HOME) {
        $candidate = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
        if (Test-Path $candidate) { return $candidate }
    }
    if ($env:ANDROID_SDK_ROOT) {
        $candidate = Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe"
        if (Test-Path $candidate) { return $candidate }
    }
    $local = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $local) { return $local }
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    return $null
}

function Get-IsoTimestamp([long]$epochMs) {
    $dto = [DateTimeOffset]::FromUnixTimeMilliseconds($epochMs)
    return $dto.ToString("yyyy-MM-dd'T'HH:mm:ss.fffzzz").Replace(":", "")
}

function Get-DayKey([long]$epochMs) {
    $dto = [DateTimeOffset]::FromUnixTimeMilliseconds($epochMs)
    return $dto.ToLocalTime().ToString("yyyy-MM-dd")
}

function New-LogJsonLine {
    param(
        [int]$Index,
        [long]$TimestampMs,
        [string]$LogType,
        [string]$LogTypeLabel,
        [string]$Event,
        [string]$PageId,
        [string]$ActionId,
        [hashtable]$ExtraFields = @{}
    )
    $params = @{
        seedIndex = "$Index"
        channel   = $LogType
    }
    foreach ($key in $ExtraFields.Keys) {
        $params[$key] = $ExtraFields[$key]
    }
    $paramsObj = @{}
    $params.GetEnumerator() | ForEach-Object { $paramsObj[$_.Key] = $_.Value }

    $obj = [ordered]@{
        timestamp      = $TimestampMs
        timestampIso   = (Get-IsoTimestamp $TimestampMs)
        logType        = $LogType
        logTypeLabel   = $LogTypeLabel
        pageId         = $PageId
        actionId       = $ActionId
        event          = $Event
        anomaly        = $false
        params         = $paramsObj
    }
    if ($LogType -eq "crash") {
        $obj.stackTrace = "com.example.stress.TestCrash: synthetic crash #$Index`n    at stress.LogSeed.generate(LogSeed.kt:1)"
        $obj.deviceInfo = "brand=stress;model=seed;sdk=34"
        $obj.anomaly = $true
    }
    if ($LogType -eq "performance") {
        $obj.params.fps = "58"
        $obj.params.metric = "scroll_fps"
    }
    return ($obj | ConvertTo-Json -Compress -Depth 5)
}

function Get-RecordSpec([int]$index) {
    $mod = $index % 3
    switch ($mod) {
        0 {
            return @{
                LogType      = "analytics"
                LogTypeLabel = "埋点"
                Event        = "stress_analytics_$index"
                PageId       = "StressAnalytics"
                ActionId     = "seed_click"
            }
        }
        1 {
            return @{
                LogType      = "performance"
                LogTypeLabel = "性能"
                Event        = "stress_perf_$index"
                PageId       = "StressPerf"
                ActionId     = "scroll_fps"
            }
        }
        default {
            return @{
                LogType      = "crash"
                LogTypeLabel = "崩溃"
                Event        = "stress_crash_$index"
                PageId       = "StressCrash"
                ActionId     = "synthetic_crash"
            }
        }
    }
}

function Write-LogSeedFiles {
    param(
        [string]$TargetDir,
        [int]$TotalCount
    )
    if (-not (Test-Path $TargetDir)) {
        New-Item -ItemType Directory -Path $TargetDir -Force | Out-Null
    }
    $baseMs = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $linesByDay = @{}
    for ($i = 0; $i -lt $TotalCount; $i++) {
        $ts = $baseMs - ($i * 1000L)
        $day = Get-DayKey $ts
        $spec = Get-RecordSpec $i
        $line = New-LogJsonLine -Index $i -TimestampMs $ts @spec
        if (-not $linesByDay.ContainsKey($day)) {
            $linesByDay[$day] = New-Object System.Collections.Generic.List[string]
        }
        [void]$linesByDay[$day].Add($line)
    }
    $writtenFiles = @()
    foreach ($day in $linesByDay.Keys) {
        $path = Join-Path $TargetDir "$day.jsonl"
        $utf8NoBom = New-Object System.Text.UTF8Encoding $false
        [System.IO.File]::WriteAllLines($path, $linesByDay[$day], $utf8NoBom)
        $writtenFiles += $path
    }
    return $writtenFiles
}

function Invoke-Adb {
    param(
        [string]$AdbPath,
        [string[]]$Args
    )
    & $AdbPath @Args
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: adb $($Args -join ' ')"
    }
}

$Root = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $Root "build\log-seed"
}

if ($Count -lt 1) {
    throw "Count must be >= 1"
}

Write-Host "==> Generating $Count test log records..."
$files = Write-LogSeedFiles -TargetDir $OutputDir -TotalCount $Count
$lineCount = 0
foreach ($f in $files) {
    $lineCount += (Get-Content $f | Measure-Object -Line).Lines
}
Write-Host "    Output: $OutputDir"
Write-Host "    Files : $($files -join ', ')"
Write-Host "    Lines : $lineCount (expected $Count)"

if ($lineCount -ne $Count) {
    throw "Line count mismatch: got $lineCount, expected $Count"
}

if ($GenerateOnly) {
    Write-Host "GenerateOnly: skipped device install."
    Write-Host "Done."
    exit 0
}

$adb = Resolve-AdbPath
if (-not $adb) {
    throw "adb not found. Install Android SDK platform-tools or use -GenerateOnly."
}

Invoke-Adb $adb @("devices")
$deviceLines = & $adb devices | Where-Object { $_ -match "device$" -and $_ -notmatch "List of devices" }
if (-not $deviceLines) {
    throw "No adb device/emulator connected."
}

if ($ClearExisting) {
    Write-Host "==> Clearing existing logs in app storage..."
    Invoke-Adb $adb @(
        "shell", "run-as", $PackageName,
        "sh", "-c", "rm -rf files/taskflow_observability/logs && mkdir -p files/taskflow_observability/logs"
    )
}

$remoteStaging = "/sdcard/Download/taskflow_log_seed"
Invoke-Adb $adb @("shell", "mkdir", "-p", $remoteStaging)

foreach ($localFile in $files) {
    $name = Split-Path $localFile -Leaf
    $remoteFile = "$remoteStaging/$name"
    Write-Host "==> Pushing $name ..."
    Invoke-Adb $adb @("push", $localFile, $remoteFile)
    Invoke-Adb $adb @(
        "shell", "run-as", $PackageName,
        "sh", "-c",
        "mkdir -p files/taskflow_observability/logs && cp $remoteFile files/taskflow_observability/logs/$name && rm -f files/taskflow_observability/logs/${name}.idx"
    )
}

Invoke-Adb $adb @("shell", "rm", "-rf", $remoteStaging)

Write-Host ""
Write-Host "Seed install complete."
Write-Host "  1. Cold start app, open Log tab (first open may rebuild .idx)."
Write-Host "  2. Run manual perf steps in docs/LOG_PERFORMANCE_REPORT.md."
Write-Host "  3. Clear via Log tab UI or re-run with -ClearExisting."
Write-Host "Done."
