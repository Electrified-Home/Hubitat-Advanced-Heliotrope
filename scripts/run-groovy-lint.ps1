param(
    [string]$Path = (Split-Path -Parent $PSScriptRoot)
)

if (-not (Test-Path -Path $Path -PathType Container)) {
    throw "Specified path '$Path' does not exist or is not a directory."
}

Push-Location -Path $Path
try {
    Write-Host "Running npm-groovy-lint in $Path" -ForegroundColor Cyan
    npx npm-groovy-lint lint apps drivers
} finally {
    Pop-Location
}
