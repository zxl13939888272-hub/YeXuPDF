[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

function ConvertFrom-SecureValue {
    param([Parameter(Mandatory)][Security.SecureString]$Value)
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try {
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$signingDir = Join-Path $repoRoot ".signing"
$keyFile = Join-Path $signingDir "yexupdf-release.jks"
$propertiesFile = Join-Path $repoRoot "key.properties"
$passwordFile = Join-Path $signingDir ".key-password.tmp"

$keytool = $null
if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin\keytool.exe"
    if (Test-Path -LiteralPath $candidate) { $keytool = $candidate }
}
if (-not $keytool) {
    $toolchainRoot = Join-Path (Split-Path -Parent $repoRoot) ".toolchains\jdk"
    if (Test-Path -LiteralPath $toolchainRoot) {
        $keytool = Get-ChildItem -LiteralPath $toolchainRoot -Filter keytool.exe -Recurse |
            Select-Object -First 1 -ExpandProperty FullName
    }
}
if (-not $keytool) {
    $keytool = Get-Command keytool.exe -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source
}
if (-not $keytool) {
    throw "keytool.exe was not found. Install JDK 17 or set JAVA_HOME first."
}

if (Test-Path -LiteralPath $keyFile) {
    throw "Release key already exists at $keyFile. It was not overwritten."
}

New-Item -ItemType Directory -Force -Path $signingDir | Out-Null

Write-Host ""
Write-Host "YeXuPDF release signing setup" -ForegroundColor Cyan
Write-Host "The password stays on this computer and is never committed to Git."
Write-Host "Use at least 12 characters and save it in your password manager."
Write-Host ""

while ($true) {
    $secureOne = Read-Host "Enter a new release-key password" -AsSecureString
    $secureTwo = Read-Host "Enter the same password again" -AsSecureString
    $plainOne = ConvertFrom-SecureValue $secureOne
    $plainTwo = ConvertFrom-SecureValue $secureTwo
    if ($plainOne.Length -lt 12) {
        Write-Host "Password must contain at least 12 characters. Try again." -ForegroundColor Yellow
        $plainOne = $null
        $plainTwo = $null
        continue
    }
    if ($plainOne -cne $plainTwo) {
        Write-Host "The passwords did not match. Try again." -ForegroundColor Yellow
        $plainOne = $null
        $plainTwo = $null
        continue
    }
    break
}

try {
    [IO.File]::WriteAllText($passwordFile, $plainOne, [Text.UTF8Encoding]::new($false))

    & $keytool -genkeypair -v `
        -storetype JKS `
        -keystore $keyFile `
        -storepass:file $passwordFile `
        -keypass:file $passwordFile `
        -alias yexupdf-release `
        -keyalg RSA `
        -keysize 4096 `
        -validity 30000 `
        -dname "CN=xile,OU=YeXuPDF,O=YeXuPDF,C=CN"
    if ($LASTEXITCODE -ne 0) { throw "keytool failed with exit code $LASTEXITCODE" }

    $properties = @(
        "storeFile=.signing/yexupdf-release.jks"
        "storePassword=$plainOne"
        "keyAlias=yexupdf-release"
        "keyPassword=$plainOne"
    )
    [IO.File]::WriteAllLines($propertiesFile, $properties, [Text.UTF8Encoding]::new($false))

    Write-Host ""
    Write-Host "Release signing is ready." -ForegroundColor Green
    Write-Host "Key: $keyFile"
    Write-Host "Local Gradle config: $propertiesFile"
    Write-Host ""
    Write-Host "IMPORTANT: Back up the .jks file and save the password in a password manager." -ForegroundColor Yellow
    Write-Host "Losing either one means future APKs cannot update existing installations."
} finally {
    $plainOne = $null
    $plainTwo = $null
    if (Test-Path -LiteralPath $passwordFile) {
        Remove-Item -LiteralPath $passwordFile -Force
    }
}

Write-Host ""
Read-Host "Press Enter to close this window"
