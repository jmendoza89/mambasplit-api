param(
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

function Require-Command {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        Write-Error "$Name is not installed or not on PATH."
    }
}

Write-Host "Checking Docker CLI..."
Require-Command -Name "docker"

Write-Host "Checking Docker daemon..."
cmd /c "docker info >NUL 2>&1"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Docker daemon is not running. Start Docker Desktop, then run this script again."
}

Write-Host "Checking Docker Compose..."
cmd /c "docker compose version >NUL 2>&1"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Docker Compose is not available. Install or enable Docker Compose, then run this script again."
}

Write-Host "Starting Postgres container (docker compose up -d db)..."
docker compose up -d db

$mavenArgs = @("spring-boot:run", "-Dspring-boot.run.profiles=local")
if ($SkipTests) {
    $mavenArgs = @("-DskipTests") + $mavenArgs
}

Write-Host "Starting API with local profile..."
& ".\mvnw.cmd" @mavenArgs
