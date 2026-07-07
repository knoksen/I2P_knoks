[CmdletBinding(SupportsShouldProcess)]
param(
    [ValidatePattern('^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$')]
    [string]$Repository = 'knoksen/I2P_knoks',

    [ValidatePattern('^[A-Za-z0-9._-]+$')]
    [string]$Tag = 'portfolio-v1.0.0',

    [Parameter(Mandatory)]
    [ValidateNotNullOrEmpty()]
    [string]$PackageRoot,

    [ValidateNotNullOrEmpty()]
    [string]$Target = 'main',

    [string]$ReleaseTitle = 'Secure Systems & I2P Portfolio v1.0.0',
    [string]$NotesFile,
    [switch]$Draft,
    [switch]$PreRelease
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$assetNames = @(
    'Secure_Systems_I2P_Portfolio_Executive.pdf',
    'Secure_Systems_I2P_Portfolio_Complete.pdf',
    'Secure_Systems_I2P_Portfolio_Publish.zip'
)

function Read-Checksums([string]$Path) {
    $result = @{}
    foreach ($rawLine in Get-Content -LiteralPath $Path) {
        $line = $rawLine.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) { continue }
        if ($line -notmatch '^([a-fA-F0-9]{64})\s+\*?(.+)$') {
            throw "Invalid checksum entry: $rawLine"
        }
        $result[$Matches[2].Trim()] = $Matches[1].ToLowerInvariant()
    }
    return $result
}

function Find-UniqueAsset([string]$Root, [string]$Name) {
    $matches = @(Get-ChildItem -LiteralPath $Root -Recurse -File |
        Where-Object { $_.Name -ceq $Name })

    if ($matches.Count -ne 1) {
        throw "Expected exactly one '$Name' below '$Root'; found $($matches.Count)."
    }
    return $matches[0]
}

try {
    if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
        throw 'GitHub CLI (gh) is required.'
    }

    $root = (Resolve-Path -LiteralPath $PackageRoot).Path
    $manifest = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../docs/portfolio/RELEASE_SHA256SUMS')).Path
    $expected = Read-Checksums $manifest
    $assets = [System.Collections.Generic.List[System.IO.FileInfo]]::new()

    foreach ($name in $assetNames) {
        if (-not $expected.ContainsKey($name)) {
            throw "Missing canonical checksum for $name"
        }

        $asset = Find-UniqueAsset $root $name
        $actual = (Get-FileHash -LiteralPath $asset.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($actual -ne $expected[$name]) {
            throw "Checksum mismatch for $name"
        }

        Write-Host "VERIFIED $name"
        $assets.Add($asset)
    }

    & gh auth status
    if ($LASTEXITCODE -ne 0) { throw 'GitHub CLI authentication failed.' }

    $temp = Join-Path ([System.IO.Path]::GetTempPath()) ("i2p-portfolio-" + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $temp -Force | Out-Null

    try {
        $sumPath = Join-Path $temp 'SHA256SUMS'
        $sumLines = foreach ($asset in $assets) {
            $hash = (Get-FileHash -LiteralPath $asset.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
            "$hash  $($asset.Name)"
        }
        Set-Content -LiteralPath $sumPath -Value $sumLines -Encoding utf8NoBOM

        if ([string]::IsNullOrWhiteSpace($NotesFile)) {
            $resolvedNotes = Join-Path $temp 'RELEASE_NOTES.md'
            @'
# Secure Systems & I2P Portfolio

Executive and Complete portfolio editions, publication package, and SHA-256 checksum manifest.

The material is intended for lawful privacy engineering, defensive security, systems-resilience research, and professional portfolio review.
'@ | Set-Content -LiteralPath $resolvedNotes -Encoding utf8NoBOM
        }
        else {
            $resolvedNotes = (Resolve-Path -LiteralPath $NotesFile).Path
        }

        $uploadPaths = @($assets.FullName) + @($sumPath)
        & gh release view $Tag --repo $Repository --json tagName *> $null
        $releaseExists = ($LASTEXITCODE -eq 0)

        if ($releaseExists) {
            if ($PSCmdlet.ShouldProcess("$Repository@$Tag", 'Replace validated release assets')) {
                $arguments = @('release', 'upload', $Tag, '--repo', $Repository, '--clobber') + $uploadPaths
                & gh @arguments
                if ($LASTEXITCODE -ne 0) { throw 'Release asset upload failed.' }
            }
        }
        else {
            $arguments = @(
                'release', 'create', $Tag,
                '--repo', $Repository,
                '--target', $Target,
                '--title', $ReleaseTitle,
                '--notes-file', $resolvedNotes
            )
            if ($Draft) { $arguments += '--draft' }
            if ($PreRelease) { $arguments += '--prerelease' }
            $arguments += $uploadPaths

            if ($PSCmdlet.ShouldProcess("$Repository@$Tag", 'Create release with validated assets')) {
                & gh @arguments
                if ($LASTEXITCODE -ne 0) { throw 'Release creation failed.' }
            }
        }

        Write-Host "Publication complete: $Repository@$Tag"
    }
    finally {
        if (Test-Path -LiteralPath $temp) {
            Remove-Item -LiteralPath $temp -Recurse -Force
        }
    }
}
catch {
    Write-Error $_
    exit 1
}
