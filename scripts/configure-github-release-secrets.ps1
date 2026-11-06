[CmdletBinding()]
param(
    [string]$Repository = "zxl13939888272-hub/YeXuPDF"
)

$ErrorActionPreference = "Stop"

function Find-GitHubCli {
    $command = Get-Command gh.exe -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }

    $repoRoot = Split-Path -Parent $PSScriptRoot
    $portable = Join-Path (Split-Path -Parent $repoRoot) ".toolchains\gh-portable\bin\gh.exe"
    if (Test-Path -LiteralPath $portable) { return $portable }

    throw "gh.exe was not found. Install GitHub CLI or place the portable build in .toolchains/gh-portable."
}

function Set-SecretFromMemory {
    param(
        [Parameter(Mandatory)][string]$GitHubCli,
        [Parameter(Mandatory)][string]$RepositoryName,
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$Value
    )

    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $GitHubCli
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.ArgumentList.Add("secret")
    $startInfo.ArgumentList.Add("set")
    $startInfo.ArgumentList.Add($Name)
    $startInfo.ArgumentList.Add("--repo")
    $startInfo.ArgumentList.Add($RepositoryName)

    $process = [Diagnostics.Process]::Start($startInfo)
    try {
        # Write without a trailing newline so passwords are stored byte-for-byte.
        $process.StandardInput.Write($Value)
        $process.StandardInput.Close()
        $stdout = $process.StandardOutput.ReadToEnd()
        $stderr = $process.StandardError.ReadToEnd()
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) {
            throw "Failed to set GitHub secret $Name. $stderr$stdout"
        }
    } finally {
        $process.Dispose()
    }
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$propertiesPath = Join-Path $repoRoot "key.properties"
if (-not (Test-Path -LiteralPath $propertiesPath)) {
    throw "key.properties is missing. Run scripts/setup-release-signing.ps1 first."
}

$properties = @{}
foreach ($line in [IO.File]::ReadAllLines($propertiesPath)) {
    if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith("#")) { continue }
    $separator = $line.IndexOf("=")
    if ($separator -le 0) { continue }
    $properties[$line.Substring(0, $separator).Trim()] = $line.Substring($separator + 1)
}

$requiredKeys = @("storeFile", "storePassword", "keyAlias", "keyPassword")
foreach ($key in $requiredKeys) {
    if ([string]::IsNullOrWhiteSpace($properties[$key])) {
        throw "Required signing property '$key' is missing."
    }
}

$keyFile = Join-Path $repoRoot $properties["storeFile"]
if (-not (Test-Path -LiteralPath $keyFile)) {
    throw "Release keystore was not found at $keyFile."
}

$gh = Find-GitHubCli
& $gh auth status | Out-Null
if ($LASTEXITCODE -ne 0) { throw "GitHub CLI is not authenticated." }

$keystoreBase64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keyFile))
try {
    Set-SecretFromMemory $gh $Repository "YEXUPDF_UPLOAD_STORE_FILE_BASE64" $keystoreBase64
    Set-SecretFromMemory $gh $Repository "YEXUPDF_UPLOAD_STORE_PASSWORD" $properties["storePassword"]
    Set-SecretFromMemory $gh $Repository "YEXUPDF_UPLOAD_KEY_ALIAS" $properties["keyAlias"]
    Set-SecretFromMemory $gh $Repository "YEXUPDF_UPLOAD_KEY_PASSWORD" $properties["keyPassword"]
} finally {
    $keystoreBase64 = $null
}

Write-Host "GitHub release secrets are configured for $Repository." -ForegroundColor Green
Write-Host "Secret values were sent through standard input and were not printed."
