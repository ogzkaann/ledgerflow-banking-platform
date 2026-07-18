$ErrorActionPreference = "Stop"

foreach ($commandName in @("java", "git")) {
    if (-not (Get-Command $commandName -ErrorAction SilentlyContinue)) {
        throw "Missing required command: $commandName"
    }
}

$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$javaSettings = java -XshowSettings:properties -version 2>&1 | ForEach-Object { $_.ToString() }
$javaExitCode = $LASTEXITCODE
$ErrorActionPreference = $previousErrorActionPreference

if ($javaExitCode -ne 0) {
    throw "Unable to read the installed Java version."
}

$javaMajorLine = $javaSettings | Select-String "java.specification.version =" | Select-Object -First 1
$javaMajor = ($javaMajorLine -split "=")[-1].Trim()

if ($javaMajor -ne "25") {
    throw "Java 25 is required; found Java $javaMajor."
}

if (-not (Test-Path -LiteralPath ".\mvnw.cmd")) {
    throw "Run this script from the repository root."
}

$javaVersion = $javaSettings | Where-Object { $_ -match "^(openjdk|java) version" } | Select-Object -First 1
Write-Output "Java: $javaVersion"
Write-Output (git --version)
& .\mvnw.cmd --version
Write-Output "Environment check completed."
