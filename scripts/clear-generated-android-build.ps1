param(
    [switch]$DryRun,
    [switch]$StopGradle,
    [string]$RepositoryRoot
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$ModulePath = Join-Path $PSScriptRoot 'GeneratedAndroidBuildCleanup.psm1'
Import-Module -Name $ModulePath -Force

try {
    if ($PSBoundParameters.ContainsKey('RepositoryRoot')) {
        if ([string]::IsNullOrWhiteSpace($RepositoryRoot)) {
            throw 'RepositoryRoot must not be empty.'
        }

        if (Test-PathContainsParentTraversal -Path $RepositoryRoot) {
            throw "RepositoryRoot must not contain parent-directory traversal segments: $RepositoryRoot"
        }
    } else {
        $RepositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
    }

    Invoke-GeneratedAndroidBuildCleanup -RepositoryRoot $RepositoryRoot -DryRun:$DryRun -StopGradle:$StopGradle
    exit 0
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
