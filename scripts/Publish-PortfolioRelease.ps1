[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter()]
    [ValidatePattern('^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$')]
    [string]$Repository = 'knoksen/I2P_knoks',

    [Parameter()]
    [ValidatePattern('^[A-Za-z0-9._-]+$')]
    [string]$Tag = 'portfolio-v1.0.0',

    [Parameter(Mandatory)]
    [ValidateNotNullOrEmpty()]
    [string]$PackageRoot,

    [Parameter()]
    [ValidateNotNullOrEmpty()]
    [string]$Target = 'main',

    [Parameter()]
    [string]$ReleaseTitle = 'Secure Systems & I2P Portfolio v1.0.0',

    [Parameter()]
    [string]$NotesFile,

    [Parameter()]
    [switch]$Draft,

    [Parameter()]
    [switch]$PreRelease
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$expectedAssetNames = @(
    'Secure_Systems_I2P_Portfolio_Executive.pdf',
    'Secure_Systems_I2P_Portfolio_Complete.pdf',
    'Secure_Systems_I2P_Portfolio_Publish.zip'
)

function Assert-CommandAvailable {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command is not available: $Name"
    }
}

function Read-ChecksumManifest {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string]$Path)

    $checksums = @{}
    foreach ($rawLine in Get-Content -LiteralPath $Path -ErrorAction Stop) {
        $line = $rawLine.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) {
            continue
        }

        if ($line -notmatch '^([a-fA-F0-9]{64})\s+\*?(.+)$') {
            throw "Invalid checksum entry: $rawLine"
        }

        $checksums[$Matches[2].Trim()] = $Matches[1].ToLowerInvariant()
    }

    return $checksums
}

function Resolve-UniqueAsset {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Root,
        [Parameter(Mandatory)][string]$Name
    )

    $matches = @(Get-ChildItem -LiteralPath $Root -Recurse -File -ErrorAction Stop |
        Where-Object { $_.Name -ceq $Name })

    if ($matches.Count -eq 0) {
        throw "Required release asset was not found below PackageRoot: $Name"
    }

    if ($matches.Count -gt 1) {
        $paths = $matches.FullName -join [Environment]::NewLine
        throw "Multiple files match release asset '$Name':$([Environment]::NewLine)$paths"
    }

    return $matches[0]
}

function Invoke-GitHubCli {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string[]]$Arguments,
        [switch]$IgnoreExitCode
    )

    & gh @Arguments
    $exitCode = $LASTEXITCODE

    if (-not $IgnoreExitCode -and $exitCode -ne 0) {
        throw "GitHub CLI failed with exit code $exitCode: gh $($Arguments -join ' ')"
    }

    return $exitCode
}

try {
    Assert-CommandAvailable -Name 'gh'

    $resolvedRoot = (Resolve-Path -LiteralPath $PackageRoot -ErrorAction Stop).Path
    $canonicalManifest = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../docs/portfolio/RELEASE_SHA256SUMS') -ErrorAction Stop).Path
    $expectedChecksums = Read-ChecksumManifest -Path $canonicalManifest

    $resolvedAssets = [System.Collections.Generic.List[System.IO.FileInfo]]::new()
    foreach ($assetName in $expectedAssetNames) {
        if (-not $expectedChecksums.ContainsKey($assetName)) {
            throw "Canonical checksum is missing for expected asset: $assetName"
        }

        $asset = Resolve-UniqueAsset -Root $resolvedRoot -Name $assetName
        $actualHash = (Get-FileHash -LiteralPath $asset.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        $expectedHash = $expectedChecksums[$assetName]

        if ($actualHash -ne $expectedHash) {
            throw "Integrity verification failed for $assetName. Expected $expectedHash but received $actualHash."
        }

        Write-Host "VERIFIED $assetName"
        $resolvedAssets.Add($asset)
    }

    Invoke-GitHubCli -Arguments @('auth', 'status') | Out-Null

    $temporaryDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ("i2p-portfolio-release-" + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $temporaryDirectory -Force | Out-Null

    try {
        $releaseChecksumPath = Join-Path $temporaryDirectory 'SHA256SUMS'
        $checksumLines = foreach ($asset in $resolvedAssets) {
            $hash = (Get-FileHash -LiteralPath $asset.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
            "$hash  $($asset.Name)"
        }
        Set-Content -LiteralPath $releaseChecksumPath -Value $checksumLines -Encoding utf8NoBOM

        $resolvedNotesFile = $null
        if (-not [string]::IsNullOrWhiteSpace($NotesFile)) {
            $resolvedNotesFile = (Resolve-Path -LiteralPath $NotesFile -ErrorAction Stop).Path
        }
        else {
            $resolvedNotesFile = Join-Path $temporaryDirectory 'RELEASE_NOTES.md'
            @"
# Secure Systems & I2P Portfolio

This release contains the Executive and Complete portfolio editions plus the complete publication package.

## Verification

Download all assets and verify them with `SHA256SUMS` before distribution or archival use.

## Scope boundary

The material is intended for lawful privacy engineering, defensive security, systems-resilience research, and professional portfolio review. Prototype, simulation, research, and verified implementation claims are explicitly distinguished.
"@ | Set-Content -LiteralPath $resolvedNotesFile -Encoding utf8NoBOM
        }

        $assetPaths = @($resolvedAssets.FullName) + @($releaseChecksumPath)
        $viewExitCode = Invoke-GitHubCli -Arguments @('release', 'view', $Tag, '--repo', $Repository) -IgnoreExitCode

        if ($viewExitCode -eq 0) {
            if ($PSCmdlet.ShouldProcess("$Repository release $Tag", 'Upload and replace validated portfolio assets')) {
                Invoke-GitHubCli -Arguments (@('release', 'upload', $Tag, '--repo', $Repository, '--clobber') + $assetPaths) | Out-Null
            }
        }
        else {
            $createArguments = @(
                'release', 'create', $Tag,
                '--repo', $Repository,
                '--target', $Target,
                '--title', $ReleaseTitle,
                '--notes-file', $resolvedNotesFile
            )

            if ($Draft) {
                $createArguments += '--draft'
            }

            if ($PreRelease) {
                $createArguments += '--prerelease'
            }

            $createArguments += $assetPaths

            if ($PSCmdlet.ShouldProcess("$Repository release $Tag", 'Create release and upload validated portfolio assets')) {
                Invoke-GitHubCli -Arguments $createArguments | Out-Null
            }
        }

        Write-Host "Portfolio release publication completed: $Repository@$Tag"
    }
    finally {
        if (Test-Path -LiteralPath $temporaryDirectory) {
            Remove-Item -LiteralPath $temporaryDirectory -Recurse -Force
        }
    }
}
catch {
    Write-Error $_
    exit 1
}
