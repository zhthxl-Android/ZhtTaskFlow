# 校验 Release APK 内含生产可观测实现类。
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$ApkDir = Join-Path $Root "app\build\outputs\apk\release"

if (-not (Test-Path $ApkDir)) {
    throw "Release APK directory not found: $ApkDir (run :app:assembleRelease first)"
}

$Apk = Get-ChildItem -Path $ApkDir -Filter "*.apk" | Select-Object -First 1
if (-not $Apk) {
    throw "No release APK under $ApkDir"
}

Write-Host "Checking release APK: $($Apk.FullName)"

$RequiredMarkers = @(
    "ReleaseAnalytics",
    "ReleasePerformanceReporter",
    "ReleaseCrashReporter",
    "LocalLogStore"
)

Add-Type -AssemblyName System.IO.Compression.FileSystem
$Zip = [System.IO.Compression.ZipFile]::OpenRead($Apk.FullName)
try {
    $DexEntries = $Zip.Entries | Where-Object { $_.Name -like "classes*.dex" }
    if (-not $DexEntries) {
        throw "No classes.dex in APK"
    }
    $Buffer = New-Object System.Collections.Generic.List[byte]
    foreach ($Entry in $DexEntries) {
        $Stream = $Entry.Open()
        try {
            $Bytes = New-Object byte[] $Entry.Length
            [void]$Stream.Read($Bytes, 0, $Entry.Length)
            $Buffer.AddRange($Bytes)
        } finally {
            $Stream.Dispose()
        }
    }
    $Ascii = [System.Text.Encoding]::ASCII.GetString($Buffer.ToArray())
    foreach ($Marker in $RequiredMarkers) {
        if ($Ascii -notmatch [regex]::Escape($Marker)) {
            throw "Missing release observability marker in DEX: $Marker"
        }
        Write-Host "OK: $Marker"
    }
} finally {
    $Zip.Dispose()
}

Write-Host "Release observability artifact check passed."
