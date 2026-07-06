$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$RepoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$Failures = [System.Collections.Generic.List[string]]::new()

$RequiredFiles = @(
    'README.md',
    'AGENTS.md',
    'DIGITAL_AUTONOMY_DOCTRINE.md',
    'docs/ARCHITECTURE.md',
    'docs/SECURITY_BOUNDARIES.md',
    'docs/ANDROID_PERMISSIONS.md',
    'docs/RESPONSIBLE_USE.md',
    'docs/ROADMAP.md',
    'docs/CLAIMS_REGISTER.md',
    'docs/VALIDATION.md',
    'product/03_UX_ONBOARDING_SPEC.md',
    'SECURITY.md',
    'scripts/check-release-claims.ps1',
    'scripts/local-release-verify.ps1'
)

foreach ($RelativePath in $RequiredFiles) {
    $Path = Join-Path $RepoRoot $RelativePath
    if (-not (Test-Path -LiteralPath $Path)) {
        [void]$Failures.Add("Missing required file: $RelativePath")
    }
}

$ReadmePath = Join-Path $RepoRoot 'README.md'
if (Test-Path -LiteralPath $ReadmePath) {
    $Readme = Get-Content -LiteralPath $ReadmePath -Raw
    $RequiredReadmePatterns = @(
        @{ Name = 'foundation-only state'; Pattern = '(?i)foundation-only' },
        @{ Name = 'no runtime communication code disclaimer'; Pattern = '(?i)no runtime communication code' },
        @{ Name = 'not a Zoom clone disclaimer'; Pattern = '(?i)not a Zoom clone' },
        @{ Name = 'no guaranteed anonymity disclaimer'; Pattern = '(?i)does not promise guaranteed anonymity' },
        @{ Name = 'no full E2EE/SASE/zero-trust disclaimer'; Pattern = '(?i)does not claim full E2EE, SASE, or zero-trust' },
        @{ Name = 'cloud storage prohibition'; Pattern = '(?i)does not store private I2P keys, private destinations, private messages, contact graphs, raw router logs, or deanonymizing metadata in cloud services' }
    )

    foreach ($Requirement in $RequiredReadmePatterns) {
        if ($Readme -notmatch $Requirement.Pattern) {
            [void]$Failures.Add("README missing required statement: $($Requirement.Name)")
        }
    }
}

$RiskPatterns = @(
    @{ Name = 'Zoom-like or Zoom clone claim'; Pattern = '(?i)\bZoom(?:-like|\s+clone|\s+replacement)\b' },
    @{ Name = 'real-time video claim'; Pattern = '(?i)\breal[- ]time\s+video\b' },
    @{ Name = 'guaranteed anonymity claim'; Pattern = '(?i)\bguaranteed\s+anonymity\b' },
    @{ Name = 'full E2EE claim'; Pattern = '(?i)\bfull\s+E2EE\b' },
    @{ Name = 'SASE claim'; Pattern = '(?i)\bSASE\b' },
    @{ Name = 'zero-trust claim'; Pattern = '(?i)\bzero[- ]trust\b' },
    @{ Name = 'production-ready claim'; Pattern = '(?i)\bproduction[- ]ready\b' },
    @{ Name = 'release-ready claim'; Pattern = '(?i)\brelease[- ]ready\b' }
)

$AllowedContextPattern = '(?i)\b(must not|do not|does not|not|never|no|avoid|unsupported|prohibited|out of scope|unless|without|cannot|should not|must not currently claim|before implementation|after implementation and tests|after validation|until proven)\b'

$MarkdownFiles = Get-ChildItem -LiteralPath $RepoRoot -Recurse -File -Filter '*.md' |
    Where-Object { $_.FullName -notmatch '\\.git\\' }

foreach ($File in $MarkdownFiles) {
    $RelativeFile = [System.IO.Path]::GetRelativePath($RepoRoot, $File.FullName)
    $Lines = Get-Content -LiteralPath $File.FullName
    for ($Index = 0; $Index -lt $Lines.Count; $Index++) {
        $Line = $Lines[$Index]
        foreach ($Risk in $RiskPatterns) {
            if ($Line -match $Risk.Pattern -and $Line -notmatch $AllowedContextPattern) {
                [void]$Failures.Add("$RelativeFile:$($Index + 1) contains unsupported $($Risk.Name): $Line")
            }
        }
    }
}

if ($Failures.Count -gt 0) {
    Write-Host 'Release claim check failed:'
    foreach ($Failure in $Failures) {
        Write-Host " - $Failure"
    }
    exit 1
}

Write-Host 'Release claim check passed.'
