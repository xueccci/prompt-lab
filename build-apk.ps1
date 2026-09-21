<#
.SYNOPSIS
  Download portable JDK + Android SDK + Gradle, then build debug APK.
.EXAMPLE
  powershell -ExecutionPolicy Bypass -File .\build-apk.ps1
#>
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$tools = Join-Path $root "tools"
New-Item -ItemType Directory -Force -Path $tools | Out-Null

function Say($msg) { Write-Host "==> $msg" -ForegroundColor Cyan }
function Ok($msg) { Write-Host "    $msg" -ForegroundColor Green }

function Get-File($url, $out) {
    if (Test-Path $out) {
        $len = (Get-Item $out).Length
        if ($len -gt 1MB) { Ok "reuse $(Split-Path $out -Leaf) ($([math]::Round($len/1MB,1)) MB)"; return }
    }
    Say "download $(Split-Path $out -Leaf)"
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing
    Ok "saved $out"
}

function Expand-Zip($zip, $dest) {
    New-Item -ItemType Directory -Force -Path $dest | Out-Null
    Say "unzip $(Split-Path $zip -Leaf)"
    Expand-Archive -Path $zip -DestinationPath $dest -Force
    Ok "ok $dest"
}

# ---- 1. JDK 17 (Temurin portable) ----
$jdkZip = Join-Path $tools "jdk17.zip"
$jdkDir = Join-Path $tools "jdk"
Get-File "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse" $jdkZip
if (-not (Test-Path $jdkDir)) {
    Expand-Zip $jdkZip $jdkDir
    # Adoptium zip contains a top-level folder
    $inner = Get-ChildItem $jdkDir -Directory | Select-Object -First 1
    if ($inner -and (Test-Path (Join-Path $inner.FullName "bin\java.exe"))) {
        $script:javaHome = $inner.FullName
    } else {
        $script:javaHome = $jdkDir
    }
} else {
    $inner = Get-ChildItem $jdkDir -Directory -EA SilentlyContinue | Select-Object -First 1
    if ($inner -and (Test-Path (Join-Path $inner.FullName "bin\java.exe"))) { $script:javaHome = $inner.FullName }
    else { $script:javaHome = $jdkDir }
}
$env:JAVA_HOME = $script:javaHome
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Ok "JAVA_HOME=$env:JAVA_HOME"
& "$env:JAVA_HOME\bin\java.exe" -version

# ---- 2. Android cmdline-tools ----
$cmdZip = Join-Path $tools "cmdline-tools.zip"
$cmdRoot = Join-Path $tools "android-cmdline"
Get-File "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip" $cmdZip
if (-not (Test-Path $cmdRoot)) {
    Expand-Zip $cmdZip $cmdRoot
}
$sdkRoot = Join-Path $tools "android-sdk"
New-Item -ItemType Directory -Force -Path $sdkRoot | Out-Null
$cmdlineDst = Join-Path $sdkRoot "cmdline-tools\latest"
if (-not (Test-Path (Join-Path $cmdlineDst "bin\sdkmanager.bat"))) {
    # extracted structure: cmdline-tools\latest or just tools
    $src = Join-Path $cmdRoot "cmdline-tools\latest"
    if (-not (Test-Path $src)) {
        $maybe = Get-ChildItem $cmdRoot -Recurse -Filter "sdkmanager.bat" -EA SilentlyContinue | Select-Object -First 1
        if ($maybe) { $src = Split-Path $maybe.FullName -Parent; $src = Split-Path $src -Parent }
    }
    New-Item -ItemType Directory -Force -Path (Split-Path $cmdlineDst -Parent) | Out-Null
    if (Test-Path $src) {
        Copy-Item $src $cmdlineDst -Recurse -Force
    } else {
        throw "sdkmanager.bat not found under $cmdRoot"
    }
}
$env:ANDROID_HOME = $sdkRoot
$env:ANDROID_SDK_ROOT = $sdkRoot
Ok "ANDROID_HOME=$sdkRoot"
$sdkmgr = Join-Path $sdkRoot "cmdline-tools\latest\bin\sdkmanager.bat"
if (-not (Test-Path $sdkmgr)) { throw "sdkmanager missing: $sdkmgr" }

Say "accept SDK licenses + install platform 34"
$yes = "y`n" * 30
$yes | & $sdkmgr --sdk_root=$sdkRoot --licenses | Out-Null
& $sdkmgr --sdk_root=$sdkRoot "platform-tools" "platforms;android-34" "build-tools;34.0.0"
Ok "SDK packages installed"

# ---- 3. Gradle 8.2 ----
$gradleZip = Join-Path $tools "gradle-8.2-bin.zip"
$gradleHome = Join-Path $tools "gradle-8.2"
Get-File "https://services.gradle.org/distributions/gradle-8.2-bin.zip" $gradleZip
if (-not (Test-Path (Join-Path $gradleHome "bin\gradle.bat"))) {
    Expand-Zip $gradleZip $tools
}
$gradleBat = Join-Path $gradleHome "bin\gradle.bat"
if (-not (Test-Path $gradleBat)) {
    $g = Get-ChildItem $tools -Recurse -Filter "gradle.bat" -EA SilentlyContinue | Select-Object -First 1
    if ($g) { $gradleBat = $g.FullName }
}
Ok "gradle=$gradleBat"

# ---- 4. Build APK ----
Say "assembleDebug"
Push-Location $root
try {
    & $gradleBat --no-daemon assembleDebug
    if ($LASTEXITCODE -ne 0) { throw "Gradle exit $LASTEXITCODE" }
} finally {
    Pop-Location
}

$apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) { throw "APK not found: $apk" }
$size = [math]::Round((Get-Item $apk).Length / 1MB, 2)
Write-Host ""
Write-Host "BUILD SUCCESS" -ForegroundColor Green
Write-Host "APK: $apk ($size MB)" -ForegroundColor Green
Write-Host "安装到手机：允许未知来源后安装 app-debug.apk" -ForegroundColor Yellow
