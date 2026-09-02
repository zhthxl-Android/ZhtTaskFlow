# 本地强制校验（Windows PowerShell）。与 .github/workflows/ci.yml 对齐。
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

function Invoke-Gradle {
    param([string[]]$Args)
    & .\gradlew.bat @Args
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed: gradlew.bat $($Args -join ' ')" }
}

Write-Host "==> checkDependencyRules"
Invoke-Gradle @("checkDependencyRules", "--no-daemon")

Write-Host "==> clean :app:compileDebugKotlin"
Invoke-Gradle @("clean", ":app:compileDebugKotlin", "--no-daemon")

Write-Host "==> :app:lintVitalRelease"
Invoke-Gradle @(":app:lintVitalRelease", "--no-daemon")

Write-Host "==> navigation + feature unit tests"
Invoke-Gradle @(
    ":component_nav:testDebugUnitTest",
    ":feature_article:testDebugUnitTest",
    ":feature_task:testDebugUnitTest",
    ":feature_log:testDebugUnitTest",
    "--no-daemon",
    "--parallel"
)

Write-Host "==> Release assemble + observability artifact check"
Invoke-Gradle @(":app:assembleRelease", "--no-daemon")
& "$PSScriptRoot\verify-release-observability.ps1"

Write-Host "CI verify: all steps passed."
