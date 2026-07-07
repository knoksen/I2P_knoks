param(
    [string]$RepoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..')),
    [switch]$SkipRequiredFileCheck
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$RepoRoot = [System.IO.Path]::GetFullPath($RepoRoot)
$Failures = [System.Collections.Generic.List[string]]::new()

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

if (-not $SkipRequiredFileCheck) {
    foreach ($RelativePath in $RequiredFiles) {
        $Path = Join-Path $RepoRoot $RelativePath
        if (-not (Test-Path -LiteralPath $Path)) {
            [void]$Failures.Add("Missing required file: $RelativePath")
        }
    }
}

$ReadmePath = Join-Path $RepoRoot 'README.md'
if (Test-Path -LiteralPath $ReadmePath) {
    $Readme = Get-Content -LiteralPath $ReadmePath -Raw
    $RequiredReadmePatterns = @(
        @{ Name = 'real-alpha maturity'; Pattern = '(?i)real-alpha' },
        @{ Name = 'no anonymity guarantee'; Pattern = '(?i)does not (currently )?(provide|guarantee|promise).{0,80}anonym' },
        @{ Name = 'not independently audited'; Pattern = '(?i)not independently (security-)?audited|not externally audited' },
        @{ Name = 'claims register link'; Pattern = [regex]::Escape('docs/CLAIMS_REGISTER.md') },
        @{ Name = 'claim-safe writing link'; Pattern = [regex]::Escape('docs/CLAIM_SAFE_WRITING.md') },
        @{ Name = 'validation link'; Pattern = [regex]::Escape('docs/VALIDATION.md') }
    )

    foreach ($Requirement in $RequiredReadmePatterns) {
        if ($Readme -notmatch $Requirement.Pattern) {
            [void]$Failures.Add("README missing required statement or link: $($Requirement.Name)")
        }
    }
}

$ReleaseFacingRelativeFiles = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
$StaticReleaseFacingFiles = @(
    'README.md',
    'SECURITY.md',
    'DIGITAL_AUTONOMY_DOCTRINE.md',
    'metadata.json',
    'docs/ARCHITECTURE.md',
    'docs/SECURITY_BOUNDARIES.md',
    'docs/ANDROID_PERMISSIONS.md',
    'docs/RESPONSIBLE_USE.md',
    'docs/ROADMAP.md',
    'docs/CLAIMS_REGISTER.md',
    'docs/CLAIM_SAFE_WRITING.md',
    'docs/VALIDATION.md',
    'docs/RELEASE_PROCESS.md',
    'docs/RELEASE_CANDIDATE_CHECKLIST.md',
    'docs/CHANGELOG_TEMPLATE.md',
    'docs/REAL_ALPHA_TEST_PLAN.md',
    'docs/REAL_DEVICE_TESTING.md',
    'docs/CI_LESSONS_M9_3.md',
    'product/03_UX_ONBOARDING_SPEC.md',
    'product/i2p-connect/README.md',
    'product/i2p-connect/PRODUCT_SPEC.md',
    'product/i2p-connect/MVP_SCOPE.md',
    'product/i2p-connect/SECURITY_MODEL.md',
    'product/i2p-connect/RESPONSIBLE_USE.md',
    'product/i2p-connect/ROADMAP.md',
    'product/i2p-connect/ARCHITECTURE.md',
    'product/i2p-connect/codex/05_add_audio_message_prototype.md',
    'app/src/main/res/values/strings.xml',
    'app/src/main/java/no/knoksen/i2pbrowser/ui/I2PScreens.kt',
    'app/src/main/java/no/knoksen/i2pbrowser/ui/I2PViewModel.kt',
    'app/src/main/java/no/knoksen/i2pbrowser/data/I2PRepository.kt',
    '.github/PULL_REQUEST_TEMPLATE.md',
    '.github/ISSUE_TEMPLATE.md',
    '.github/workflows/release-claims.yml',
    '.github/workflows/release-draft.yml',
    '.github/workflows/release-candidate-readiness.yml',
    'scripts/new-release-notes-template.ps1'
)

foreach ($RelativePath in $StaticReleaseFacingFiles) {
    $Path = Join-Path $RepoRoot $RelativePath
    if (Test-Path -LiteralPath $Path) {
        [void]$ReleaseFacingRelativeFiles.Add($RelativePath.Replace('\', '/'))
    }
}

$ReleaseFacingDirectories = @(
    'docs',
    'product',
    '.github'
)

foreach ($RelativeDirectory in $ReleaseFacingDirectories) {
    $DirectoryPath = Join-Path $RepoRoot $RelativeDirectory
    if (-not (Test-Path -LiteralPath $DirectoryPath)) {
        continue
    }
    Get-ChildItem -LiteralPath $DirectoryPath -Recurse -File |
        Where-Object { $_.Extension -in @('.md', '.yml', '.yaml') } |
        ForEach-Object {
            $RelativePath = [System.IO.Path]::GetRelativePath($RepoRoot, $_.FullName).Replace('\', '/')
            [void]$ReleaseFacingRelativeFiles.Add($RelativePath)
        }
}

$RiskPatterns = @(
    @{ Name = 'complete anonymity claim'; Pattern = '(?i)\b(provides?|offers?|guarantees?)\s+(complete\s+|full\s+|guaranteed\s+)?anonymity\b' },
    @{ Name = 'privacy guarantee claim'; Pattern = '(?i)\bguarantees?\s+(user\s+)?privacy\b' },
    @{ Name = 'untraceable claim'; Pattern = '(?i)\b(untraceable|cannot\s+be\s+traced|can''t\s+be\s+traced|can\s+not\s+be\s+traced)\b' },
    @{ Name = 'metadata-free claim'; Pattern = '(?i)\b(metadata[- ]free|zero\s+metadata|stores?\s+no\s+metadata|no\s+metadata)\b' },
    @{ Name = 'no-logs claim'; Pattern = '(?i)\b(zero\s+logs|no\s+logs|stores?\s+no\s+logs)\b' },
    @{ Name = 'fully secure claim'; Pattern = '(?i)\b(fully|completely|100%)\s+secure\b' },
    @{ Name = 'production-ready claim'; Pattern = '(?i)\bproduction[- ]ready\b' },
    @{ Name = 'release-ready claim'; Pattern = '(?i)\brelease[- ]ready\b' },
    @{ Name = 'audit claim'; Pattern = '(?i)\b(audited\s+(encryption|cryptography|crypto|secure chat)|independently\s+audited\s+(encryption|cryptography|security)|externally\s+audited)\b' },
    @{ Name = 'private-by-default claim'; Pattern = '(?i)\bprivate\s+by\s+default\b' },
    @{ Name = 'E2EE claim'; Pattern = '(?i)\b(end[- ]to[- ]end encrypted|E2EE\s+(session\s+)?active|full\s+E2EE)\b' },
    @{ Name = 'secure-chat claim'; Pattern = '(?i)\bsecure\s+chat\b' },
    @{ Name = 'active VPN claim'; Pattern = '(?i)\bVPN\b.{0,40}\b(active|provided|enabled|connected|protects|protection)\b' },
    @{ Name = 'Tor routing claim'; Pattern = '(?i)\bTor\s+routing\b' },
    @{ Name = 'embedded router claim'; Pattern = '(?i)\b((embedded|built[- ]in)\s+(I2P\s+)?router|embedded-router)\b' },
    @{ Name = 'browser isolation claim'; Pattern = '(?i)\b(full\s+)?browser\s+isolation\b' },
    @{ Name = 'censorship-proof claim'; Pattern = '(?i)\bcensorship[- ]proof\b' }
)

$AllowedContextPattern = '(?i)\b(no|not|does not|do not|without|unsupported|prohibited|out of scope|unless|until|before|after|planned|future|experimental|prototype|lab|demo|sample|simulated|local preview|not currently|cannot|should not|must not|avoid|non[- ]goals?|known limits?|still LAB|gated|not available|not implemented|requires implementation|requires.*tests|claim[- ]safe|prohibited or premature|restricted wording)\b'

foreach ($RelativePath in $ReleaseFacingRelativeFiles) {
    $Path = Join-Path $RepoRoot $RelativePath
    if (-not (Test-Path -LiteralPath $Path)) {
        continue
    }

    $Lines = @(Get-Content -LiteralPath $Path)
    for ($Index = 0; $Index -lt $Lines.Count; $Index++) {
        $Line = $Lines[$Index]
        foreach ($Risk in $RiskPatterns) {
            if ($Line -match $Risk.Pattern -and $Line -notmatch $AllowedContextPattern) {
                [void]$Failures.Add(
                    "$RelativePath`:$($Index + 1) contains unsupported $($Risk.Name): $Line. " +
                    "Add a clear qualifier such as no/not/lab/planned/prototype, or register and validate the stronger claim."
                )
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
exit 0
