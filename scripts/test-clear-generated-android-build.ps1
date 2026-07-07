$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$RepoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$CleanupScript = Join-Path $RepoRoot 'scripts/clear-generated-android-build.ps1'
$CleanupModule = Join-Path $RepoRoot 'scripts/GeneratedAndroidBuildCleanup.psm1'

Import-Module -Name $CleanupModule -Force

$PwshCommand = Get-Command pwsh -ErrorAction SilentlyContinue
if ($PwshCommand) {
    $Pwsh = $PwshCommand.Source
} else {
    $Pwsh = (Get-Command powershell -ErrorAction Stop).Source
}

function New-CleanupTestRepo {
    param(
        [string]$Name,
        [switch]$WithBuild,
        [switch]$GradleStopFails
    )

    $Root = Join-Path ([System.IO.Path]::GetTempPath()) ("i2p cleanup $Name " + [System.Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Force -Path $Root | Out-Null
    New-Item -ItemType Directory -Force -Path (Join-Path $Root '.git') | Out-Null
    New-Item -ItemType Directory -Force -Path (Join-Path $Root 'app\src\main') | Out-Null
    Set-Content -LiteralPath (Join-Path $Root 'settings.gradle.kts') -Value 'rootProject.name = "I2P Knoks Browser"; include(":app")' -Encoding utf8
    Set-Content -LiteralPath (Join-Path $Root 'app\build.gradle.kts') -Value 'plugins { id("com.android.application") }' -Encoding utf8
    Set-Content -LiteralPath (Join-Path $Root 'app\src\main\AndroidManifest.xml') -Value '<manifest />' -Encoding utf8

    if ($GradleStopFails) {
        Set-Content -LiteralPath (Join-Path $Root 'gradlew.bat') -Value "@echo off`r`necho simulated wrapper failure`r`nexit /b 7`r`n" -Encoding ascii
    } else {
        Set-Content -LiteralPath (Join-Path $Root 'gradlew.bat') -Value "@echo off`r`necho gradle wrapper stop marker > gradle-stop-marker.txt`r`nexit /b 0`r`n" -Encoding ascii
    }

    if ($WithBuild) {
        New-Item -ItemType Directory -Force -Path (Join-Path $Root 'app\build\nested\deep') | Out-Null
        Set-Content -LiteralPath (Join-Path $Root 'app\build\nested\deep\generated.txt') -Value 'generated' -Encoding utf8
    }

    return $Root
}

function Invoke-CleanupScript {
    param(
        [string[]]$Arguments
    )

    $Output = & $Pwsh -NoProfile -File $CleanupScript @Arguments 2>&1
    return [pscustomobject]@{
        ExitCode = $LASTEXITCODE
        Output = ($Output | Out-String)
    }
}

function Assert-Passes {
    param(
        [string]$Name,
        [string[]]$Arguments
    )

    $Result = Invoke-CleanupScript -Arguments $Arguments
    if ($Result.ExitCode -ne 0) {
        throw "Expected '$Name' to pass, but it failed:`n$($Result.Output)"
    }
    Write-Host "PASS: $Name"
    return $Result
}

function Assert-Fails {
    param(
        [string]$Name,
        [string[]]$Arguments,
        [string]$ExpectedText
    )

    $Result = Invoke-CleanupScript -Arguments $Arguments
    if ($Result.ExitCode -eq 0) {
        throw "Expected '$Name' to fail, but it passed."
    }
    if ($ExpectedText -and $Result.Output -notmatch [regex]::Escape($ExpectedText)) {
        throw "Expected '$Name' failure to mention '$ExpectedText', got:`n$($Result.Output)"
    }
    Write-Host "PASS: $Name"
    return $Result
}

function Assert-BoundaryFails {
    param(
        [string]$Name,
        [string]$RepositoryRoot,
        [string]$AppPath,
        [string]$TargetPath,
        [string]$ExpectedText
    )

    try {
        [void](Test-GeneratedAndroidBuildCleanupBoundary -RepositoryRoot $RepositoryRoot -AppPath $AppPath -TargetPath $TargetPath)
        throw "Expected '$Name' to fail, but it passed."
    } catch {
        if ($_.Exception.Message -eq "Expected '$Name' to fail, but it passed.") {
            throw
        }
        if ($ExpectedText -and $_.Exception.Message -notmatch [regex]::Escape($ExpectedText)) {
            throw "Expected '$Name' failure to mention '$ExpectedText', got:`n$($_.Exception.Message)"
        }
    }
    Write-Host "PASS: $Name"
}

function Remove-CleanupTestRoot {
    param(
        [string]$Root
    )

    if ([string]::IsNullOrWhiteSpace($Root)) {
        return
    }

    $FullRoot = [System.IO.Path]::GetFullPath($Root)
    $TempRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
    $Leaf = Split-Path -Leaf $FullRoot
    if (-not $FullRoot.StartsWith($TempRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove test root outside temp: $FullRoot"
    }
    if (-not $Leaf.StartsWith('i2p cleanup ', [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove unexpected test root: $FullRoot"
    }

    if (Test-Path -LiteralPath $FullRoot) {
        Get-ChildItem -LiteralPath $FullRoot -Force -Recurse -ErrorAction SilentlyContinue |
            Where-Object { ($_.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0 } |
            Sort-Object FullName -Descending |
            ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force -ErrorAction SilentlyContinue }
        Remove-Item -LiteralPath $FullRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
}

function Test-IsWindowsHost {
    return ($env:OS -eq 'Windows_NT')
}

$TempRoots = [System.Collections.Generic.List[string]]::new()
$OpenHandle = $null

try {
    $Root = New-CleanupTestRepo -Name 'valid layout'
    $TempRoots.Add($Root)
    [void](Assert-Passes -Name 'valid temporary repository layout is recognized' -Arguments @('-DryRun', '-RepositoryRoot', $Root))

    $Root = New-CleanupTestRepo -Name 'missing build'
    $TempRoots.Add($Root)
    [void](Assert-Passes -Name 'missing app/build returns success without mutation' -Arguments @('-RepositoryRoot', $Root))
    if (Test-Path -LiteralPath (Join-Path $Root 'app\build')) {
        throw 'Missing-target test created app/build unexpectedly.'
    }

    $Root = New-CleanupTestRepo -Name 'dry run' -WithBuild
    $TempRoots.Add($Root)
    [void](Assert-Passes -Name 'dry-run validates but does not delete' -Arguments @('-DryRun', '-RepositoryRoot', $Root))
    if (-not (Test-Path -LiteralPath (Join-Path $Root 'app\build\nested\deep\generated.txt'))) {
        throw 'Dry-run removed generated build content.'
    }

    $Root = New-CleanupTestRepo -Name 'delete nested' -WithBuild
    $TempRoots.Add($Root)
    [void](Assert-Passes -Name 'valid generated app/build is removed' -Arguments @('-RepositoryRoot', $Root))
    if (Test-Path -LiteralPath (Join-Path $Root 'app\build')) {
        throw 'Generated app/build still exists after successful cleanup.'
    }
    Write-Host 'PASS: files and nested directories inside temporary build directory are removed'

    $Root = New-CleanupTestRepo -Name 'path with spaces' -WithBuild
    $TempRoots.Add($Root)
    [void](Assert-Passes -Name 'repository path containing spaces works' -Arguments @('-RepositoryRoot', $Root))

    if (Test-IsWindowsHost) {
        $Root = New-CleanupTestRepo -Name 'mixed case' -WithBuild
        $TempRoots.Add($Root)
        $UpperRoot = $Root.ToUpperInvariant()
        $Plan = Resolve-GeneratedAndroidBuildCleanupPlan -RepositoryRoot $UpperRoot
        if (-not (Test-Path -LiteralPath $Plan.TargetPath)) {
            throw 'Mixed-case path did not resolve to existing generated build target.'
        }
        Write-Host 'PASS: mixed-case Windows path comparisons behave safely'
    } else {
        Write-Host 'SKIP: mixed-case Windows path comparison requires Windows filesystem semantics'
    }

    [void](Assert-Fails -Name 'empty repository-root input is refused' -Arguments @('-RepositoryRoot', '   ') -ExpectedText 'RepositoryRoot must not be empty')
    [void](Assert-Fails -Name 'nonexistent repository root is refused' -Arguments @('-RepositoryRoot', (Join-Path ([System.IO.Path]::GetTempPath()) ('missing-' + [System.Guid]::NewGuid().ToString('N')))) -ExpectedText 'could not be resolved')

    $Root = Join-Path ([System.IO.Path]::GetTempPath()) ('i2p cleanup no markers ' + [System.Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Force -Path $Root | Out-Null
    $TempRoots.Add($Root)
    [void](Assert-Fails -Name 'directory without repository markers is refused' -Arguments @('-RepositoryRoot', $Root) -ExpectedText 'missing .git marker')

    $Root = New-CleanupTestRepo -Name 'boundary checks'
    $TempRoots.Add($Root)
    $AppPath = [System.IO.Path]::GetFullPath((Join-Path $Root 'app'))
    Assert-BoundaryFails -Name 'target resolving to repository root is refused' -RepositoryRoot $Root -AppPath $AppPath -TargetPath $Root -ExpectedText 'target equals repository root'
    Assert-BoundaryFails -Name 'target resolving to app is refused' -RepositoryRoot $Root -AppPath $AppPath -TargetPath $AppPath -ExpectedText 'target equals the app module'
    Assert-BoundaryFails -Name 'canonical target outside the repository is refused' -RepositoryRoot $Root -AppPath $AppPath -TargetPath (Join-Path ([System.IO.Path]::GetTempPath()) 'outside-build') -ExpectedText 'outside repository root'

    $Root = New-CleanupTestRepo -Name 'malformed'
    $TempRoots.Add($Root)
    Remove-Item -LiteralPath (Join-Path $Root 'app\build.gradle.kts') -Force
    [void](Assert-Fails -Name 'malformed repository layout is refused' -Arguments @('-RepositoryRoot', $Root) -ExpectedText 'missing app/build.gradle')

    $Root = New-CleanupTestRepo -Name 'traversal'
    $TempRoots.Add($Root)
    [void](Assert-Fails -Name 'attempted path traversal is refused' -Arguments @('-RepositoryRoot', (Join-Path $Root 'app\..')) -ExpectedText 'parent-directory traversal')

    $FilesystemRoot = [System.IO.Path]::GetPathRoot([System.IO.Path]::GetTempPath())
    [void](Assert-Fails -Name 'filesystem-root-like input is refused' -Arguments @('-RepositoryRoot', $FilesystemRoot) -ExpectedText 'filesystem root')

    if ($env:USERPROFILE) {
        [void](Assert-Fails -Name 'user-profile-like input is refused' -Arguments @('-RepositoryRoot', $env:USERPROFILE) -ExpectedText 'user profile')
    } else {
        Write-Host 'SKIP: user-profile-like input requires USERPROFILE'
    }

    if (Test-IsWindowsHost) {
        $Root = New-CleanupTestRepo -Name 'app junction base'
        $TempRoots.Add($Root)
        $ExternalApp = Join-Path ([System.IO.Path]::GetTempPath()) ('i2p cleanup external app ' + [System.Guid]::NewGuid().ToString('N'))
        $TempRoots.Add($ExternalApp)
        New-Item -ItemType Directory -Force -Path (Join-Path $ExternalApp 'src\main') | Out-Null
        Set-Content -LiteralPath (Join-Path $ExternalApp 'build.gradle.kts') -Value 'plugins {}' -Encoding utf8
        Set-Content -LiteralPath (Join-Path $ExternalApp 'src\main\AndroidManifest.xml') -Value '<manifest />' -Encoding utf8
        Remove-Item -LiteralPath (Join-Path $Root 'app') -Recurse -Force
        New-Item -ItemType Junction -Path (Join-Path $Root 'app') -Target $ExternalApp | Out-Null
        [void](Assert-Fails -Name 'app represented by a junction is refused' -Arguments @('-RepositoryRoot', $Root) -ExpectedText 'app module directory')

        $Root = New-CleanupTestRepo -Name 'build junction'
        $TempRoots.Add($Root)
        $ExternalBuild = Join-Path ([System.IO.Path]::GetTempPath()) ('i2p cleanup external build ' + [System.Guid]::NewGuid().ToString('N'))
        $TempRoots.Add($ExternalBuild)
        New-Item -ItemType Directory -Force -Path $ExternalBuild | Out-Null
        New-Item -ItemType Junction -Path (Join-Path $Root 'app\build') -Target $ExternalBuild | Out-Null
        [void](Assert-Fails -Name 'build represented by a junction is refused' -Arguments @('-RepositoryRoot', $Root) -ExpectedText 'Generated app/build directory')
    } else {
        Write-Host 'SKIP: junction refusal tests require Windows'
    }

    $Root = New-CleanupTestRepo -Name 'dry run stop gradle' -WithBuild
    $TempRoots.Add($Root)
    [void](Assert-Passes -Name 'dry-run with StopGradle does not invoke Gradle' -Arguments @('-DryRun', '-StopGradle', '-RepositoryRoot', $Root))
    if (Test-Path -LiteralPath (Join-Path $Root 'gradle-stop-marker.txt')) {
        throw 'Dry-run invoked Gradle wrapper unexpectedly.'
    }

    $Root = New-CleanupTestRepo -Name 'no implicit gradle' -WithBuild
    $TempRoots.Add($Root)
    [void](Assert-Passes -Name 'cleanup does not stop Gradle implicitly' -Arguments @('-RepositoryRoot', $Root))
    if (Test-Path -LiteralPath (Join-Path $Root 'gradle-stop-marker.txt')) {
        throw 'Cleanup invoked Gradle wrapper without -StopGradle.'
    }

    if (Test-IsWindowsHost) {
        $Root = New-CleanupTestRepo -Name 'gradle stop failure' -WithBuild -GradleStopFails
        $TempRoots.Add($Root)
        [void](Assert-Fails -Name 'Gradle-stop failure is reported consistently' -Arguments @('-StopGradle', '-RepositoryRoot', $Root) -ExpectedText 'Gradle wrapper stop failed')
        if (-not (Test-Path -LiteralPath (Join-Path $Root 'app\build'))) {
            throw 'Gradle-stop failure removed app/build unexpectedly.'
        }
    } else {
        Write-Host 'SKIP: Gradle-stop failure test requires Windows batch execution'
    }

    if (Test-IsWindowsHost) {
        $Root = New-CleanupTestRepo -Name 'locked delete' -WithBuild
        $TempRoots.Add($Root)
        $LockedFile = Join-Path $Root 'app\build\locked.txt'
        Set-Content -LiteralPath $LockedFile -Value 'locked' -Encoding utf8
        $OpenHandle = [System.IO.File]::Open($LockedFile, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
        try {
            [void](Assert-Fails -Name 'deletion failure returns non-zero' -Arguments @('-RepositoryRoot', $Root) -ExpectedText 'Could not remove generated Android build directory')
        } finally {
            $OpenHandle.Dispose()
            $OpenHandle = $null
        }
    } else {
        Write-Host 'SKIP: deletion lock failure test requires Windows file-lock semantics'
    }

    Write-Host 'Generated Android build cleanup tests passed.'
} finally {
    if ($OpenHandle) {
        $OpenHandle.Dispose()
    }

    foreach ($Root in $TempRoots) {
        Remove-CleanupTestRoot -Root $Root
    }
}

exit 0
