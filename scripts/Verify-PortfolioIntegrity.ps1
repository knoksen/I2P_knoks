[CmdletBinding()]
param(
    [Parameter()]
    [ValidateNotNullOrEmpty()]
    [string]$PackageRoot = (Get-Location).Path,

    [Parameter()]
    [string]$ChecksumFile
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Resolve-ChecksumManifest {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [string]$Root,

        [Parameter()]
        [string]$ExplicitPath
    )

    if (-not [string]::IsNullOrWhiteSpace($ExplicitPath)) {
        return (Resolve-Path -LiteralPath $ExplicitPath -ErrorAction Stop).Path
    }

    $releaseManifest = Join-Path $Root 'SHA256SUMS'
    if (Test-Path -LiteralPath $releaseManifest -PathType Leaf) {
        return (Resolve-Path -LiteralPath $releaseManifest).Path
    }

    $repositoryManifest = Join-Path $PSScriptRoot '../docs/portfolio/RELEASE_SHA256SUMS'
    if (Test-Path -LiteralPath $repositoryManifest -PathType Leaf) {
        return (Resolve-Path -LiteralPath $repositoryManifest).Path
    }

    throw 'No checksum manifest found. Supply -ChecksumFile or place SHA256SUMS in PackageRoot.'
}

try {
    $resolvedRoot = (Resolve-Path -LiteralPath $PackageRoot -ErrorAction Stop).Path
    $manifestPath = Resolve-ChecksumManifest -Root $resolvedRoot -ExplicitPath $ChecksumFile
    $failures = [System.Collections.Generic.List[string]]::new()
    $checked = 0

    foreach ($rawLine in Get-Content -LiteralPath $manifestPath -ErrorAction Stop) {
        $line = $rawLine.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) {
            continue
        }

        if ($line -notmatch '^([a-fA-F0-9]{64})\s+\*?(.+)$') {
            throw "Invalid checksum entry: $rawLine"
        }

        $expected = $Matches[1].ToLowerInvariant()
        $relativePath = $Matches[2].Trim()

        if ([System.IO.Path]::IsPathRooted($relativePath)) {
            throw "Absolute paths are not permitted in checksum manifests: $relativePath"
        }

        $target = Join-Path $resolvedRoot $relativePath
        $normalizedTarget = [System.IO.Path]::GetFullPath($target)
        $normalizedRoot = [System.IO.Path]::GetFullPath($resolvedRoot + [System.IO.Path]::DirectorySeparatorChar)

        if (-not $normalizedTarget.StartsWith($normalizedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Checksum path escapes PackageRoot: $relativePath"
        }

        $checked++
        if (-not (Test-Path -LiteralPath $normalizedTarget -PathType Leaf)) {
            $failures.Add("MISSING  $relativePath")
            continue
        }

        $actual = (Get-FileHash -LiteralPath $normalizedTarget -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($actual -eq $expected) {
            Write-Host "OK       $relativePath"
        }
        else {
            $failures.Add("FAILED   $relativePath`n         expected: $expected`n         actual:   $actual")
        }
    }

    if ($checked -eq 0) {
        throw "Checksum manifest contains no verifiable entries: $manifestPath"
    }

    if ($failures.Count -gt 0) {
        foreach ($failure in $failures) {
            Write-Error $failure
        }
        exit 1
    }

    Write-Host "Portfolio integrity verification completed successfully ($checked files)."
}
catch {
    Write-Error $_
    exit 1
}
