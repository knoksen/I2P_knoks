Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$implementation = Join-Path $PSScriptRoot 'Publish-PortfolioReleaseV2.ps1'
if (-not (Test-Path -LiteralPath $implementation -PathType Leaf)) {
    throw "Release publisher implementation not found: $implementation"
}

& $implementation @args
exit $LASTEXITCODE
