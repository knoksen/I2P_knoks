$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$RepoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$CheckScript = Join-Path $RepoRoot 'scripts/check-release-claims.ps1'
$PwshCommand = Get-Command pwsh -ErrorAction SilentlyContinue
if ($PwshCommand) {
    $Pwsh = $PwshCommand.Source
} else {
    $Pwsh = (Get-Command powershell -ErrorAction Stop).Source
}

function New-ClaimTestRepo {
    param(
        [string]$Name
    )

    $Root = Join-Path ([System.IO.Path]::GetTempPath()) ("i2p-claims-$Name-" + [System.Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Force -Path $Root | Out-Null

    $RequiredFiles = @(
        'README.md',
        'AGENTS.md',
        'DIGITAL_AUTONOMY_DOCTRINE.md',
        'SECURITY.md',
        'metadata.json',
        'docs/ARCHITECTURE.md',
        'docs/SECURITY_BOUNDARIES.md',
        'docs/ANDROID_PERMISSIONS.md',
        'docs/RESPONSIBLE_USE.md',
        'docs/ROADMAP.md',
        'docs/CLAIMS_REGISTER.md',
        'docs/CLAIM_SAFE_WRITING.md',
        'docs/VALIDATION.md',
        'docs/ANDROID_REAL_ALPHA_TEST_MATRIX.md',
        'product/03_UX_ONBOARDING_SPEC.md',
        '.github/PULL_REQUEST_TEMPLATE.md',
        '.github/ISSUE_TEMPLATE.md',
        'scripts/check-release-claims.ps1',
        'scripts/local-release-verify.ps1'
    )

    foreach ($RelativePath in $RequiredFiles) {
        $Path = Join-Path $Root $RelativePath
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Path) | Out-Null
        Set-Content -LiteralPath $Path -Value "# Placeholder`n" -Encoding utf8
    }

    Set-Content -LiteralPath (Join-Path $Root 'README.md') -Value @'
# Test README

This Android real-alpha does not provide anonymity by itself and is not independently audited.

See docs/CLAIMS_REGISTER.md, docs/CLAIM_SAFE_WRITING.md, and docs/VALIDATION.md.
'@ -Encoding utf8

    Set-Content -LiteralPath (Join-Path $Root 'metadata.json') -Value '{"description":"Android real-alpha for local I2P diagnostics."}' -Encoding utf8
    Set-Content -LiteralPath (Join-Path $Root 'docs/CLAIMS_REGISTER.md') -Value "# Claims Register`n`nNo unsupported current claims." -Encoding utf8
    Set-Content -LiteralPath (Join-Path $Root 'docs/CLAIM_SAFE_WRITING.md') -Value "# Claim-Safe Writing`n`nPlanned work must be labeled." -Encoding utf8

    return $Root
}

function Invoke-ClaimCheck {
    param(
        [string]$Root
    )

    $Output = & $Pwsh -NoProfile -File $CheckScript -RepoRoot $Root 2>&1
    [pscustomobject]@{
        ExitCode = $LASTEXITCODE
        Output = ($Output | Out-String)
    }
}

function Assert-ClaimCheckPasses {
    param(
        [string]$Name,
        [string]$Root
    )

    $Result = Invoke-ClaimCheck -Root $Root
    if ($Result.ExitCode -ne 0) {
        throw "Expected '$Name' to pass, but it failed:`n$($Result.Output)"
    }
    Write-Host "PASS: $Name"
}

function Assert-ClaimCheckFails {
    param(
        [string]$Name,
        [string]$Root,
        [string]$ExpectedText
    )

    $Result = Invoke-ClaimCheck -Root $Root
    if ($Result.ExitCode -eq 0) {
        throw "Expected '$Name' to fail, but it passed."
    }
    if ($Result.Output -notmatch [regex]::Escape($ExpectedText)) {
        throw "Expected '$Name' failure to mention '$ExpectedText', got:`n$($Result.Output)"
    }
    Write-Host "PASS: $Name"
}

$TempRoots = [System.Collections.Generic.List[string]]::new()
try {
    $Root = New-ClaimTestRepo -Name 'prohibited'
    $TempRoots.Add($Root)
    Set-Content -LiteralPath (Join-Path $Root 'README.md') -Value @'
# Test README

This Android real-alpha provides complete anonymity.

See docs/CLAIMS_REGISTER.md, docs/CLAIM_SAFE_WRITING.md, and docs/VALIDATION.md.
'@ -Encoding utf8
    Assert-ClaimCheckFails -Name 'clearly prohibited release claim fails' -Root $Root -ExpectedText 'complete anonymity claim'

    $Root = New-ClaimTestRepo -Name 'qualified'
    $TempRoots.Add($Root)
    Add-Content -LiteralPath (Join-Path $Root 'SECURITY.md') -Value 'The app does not provide anonymity by itself.'
    Assert-ClaimCheckPasses -Name 'approved qualified wording passes' -Root $Root

    $Root = New-ClaimTestRepo -Name 'planned'
    $TempRoots.Add($Root)
    Set-Content -LiteralPath (Join-Path $Root 'product/03_UX_ONBOARDING_SPEC.md') -Value 'Planned encrypted audio notes require implementation and tests before release wording.' -Encoding utf8
    Assert-ClaimCheckPasses -Name 'planned feature wording passes' -Root $Root

    $Root = New-ClaimTestRepo -Name 'audit'
    $TempRoots.Add($Root)
    Add-Content -LiteralPath (Join-Path $Root 'README.md') -Value 'The app has audited encryption.'
    Assert-ClaimCheckFails -Name 'independent audit wording cannot be implied accidentally' -Root $Root -ExpectedText 'audit claim'

    $Root = New-ClaimTestRepo -Name 'case'
    $TempRoots.Add($Root)
    Add-Content -LiteralPath (Join-Path $Root 'README.md') -Value 'The app is PRODUCTION READY.'
    Assert-ClaimCheckFails -Name 'case variations are handled' -Root $Root -ExpectedText 'production-ready claim'

    $Root = New-ClaimTestRepo -Name 'spaces'
    $TempRoots.Add($Root)
    $SpacedPath = Join-Path $Root 'docs/Claim Test With Spaces.md'
    Set-Content -LiteralPath $SpacedPath -Value 'This app is private by default.' -Encoding utf8
    Assert-ClaimCheckFails -Name 'file paths containing spaces work' -Root $Root -ExpectedText 'private-by-default claim'

    $Root = New-ClaimTestRepo -Name 'interpolation'
    $TempRoots.Add($Root)
    $env:CLAIM_TEST_SECRET = 'must-not-appear'
    Add-Content -LiteralPath (Join-Path $Root 'README.md') -Value 'The app is production-ready with $env:CLAIM_TEST_SECRET.'
    $Result = Invoke-ClaimCheck -Root $Root
    if ($Result.ExitCode -eq 0) {
        throw 'Expected PowerShell interpolation safety test to fail on production-ready wording.'
    }
    if ($Result.Output -match [regex]::Escape($env:CLAIM_TEST_SECRET)) {
        throw "PowerShell interpolation safety test leaked expanded environment value:`n$($Result.Output)"
    }
    Write-Host 'PASS: PowerShell interpolation remains safe'

    Write-Host 'Release claim script tests passed.'
} finally {
    foreach ($Root in $TempRoots) {
        if ([string]::IsNullOrWhiteSpace($Root)) {
            continue
        }
        $FullRoot = [System.IO.Path]::GetFullPath($Root)
        $TempRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
        if ($FullRoot.StartsWith($TempRoot, [System.StringComparison]::OrdinalIgnoreCase) -and
            (Split-Path -Leaf $FullRoot).StartsWith('i2p-claims-', [System.StringComparison]::OrdinalIgnoreCase)) {
            Remove-Item -LiteralPath $FullRoot -Recurse -Force
        }
    }
}

exit 0
