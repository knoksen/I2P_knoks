$ErrorActionPreference = "Stop"

$patterns = @(
    "VPN active",
    "encrypted messenger",
    "guaranteed anonymity",
    "secure tunnel",
    "quantum-safe",
    "military-grade",
    "garlic secured"
)

$paths = @(
    "README.md",
    "docs",
    "app/src/main/java"
)

$found = $false
foreach ($pattern in $patterns) {
    $matches = & rg -n --fixed-strings --ignore-case --glob "!app/src/main/java/no/knoksen/i2pbrowser/data/DarkBERTService.kt" $pattern $paths 2>$null
    if ($LASTEXITCODE -eq 0) {
        $found = $true
        Write-Host "Forbidden release claim found: $pattern" -ForegroundColor Red
        $matches | ForEach-Object { Write-Host $_ }
    } elseif ($LASTEXITCODE -gt 1) {
        throw "rg failed while checking pattern: $pattern"
    }
}

if ($found) {
    throw "Release claim check failed."
}

Write-Host "Release claim check passed." -ForegroundColor Green
