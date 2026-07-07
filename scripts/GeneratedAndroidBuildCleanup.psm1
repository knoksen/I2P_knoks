$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function ConvertTo-ComparablePath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        throw 'Path is empty.'
    }

    $FullPath = [System.IO.Path]::GetFullPath($Path)
    $Root = [System.IO.Path]::GetPathRoot($FullPath)
    $Trimmed = $FullPath.TrimEnd([char]'\', [char]'/')

    if ($FullPath -eq $Root) {
        $RootTrimmed = $Root.TrimEnd([char]'\', [char]'/')
        if ([string]::IsNullOrWhiteSpace($RootTrimmed)) {
            return $Root
        }
        return $RootTrimmed
    }

    return $Trimmed
}

function Test-PathSame {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Left,
        [Parameter(Mandatory = $true)]
        [string]$Right
    )

    $ComparableLeft = ConvertTo-ComparablePath -Path $Left
    $ComparableRight = ConvertTo-ComparablePath -Path $Right
    return [string]::Equals($ComparableLeft, $ComparableRight, [System.StringComparison]::OrdinalIgnoreCase)
}

function Test-PathInside {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Parent,
        [Parameter(Mandatory = $true)]
        [string]$Child
    )

    if (Test-PathSame -Left $Parent -Right $Child) {
        return $false
    }

    $ComparableParent = ConvertTo-ComparablePath -Path $Parent
    $ComparableChild = ConvertTo-ComparablePath -Path $Child
    $ParentWithSeparator = $ComparableParent.TrimEnd([char]'\', [char]'/') + [System.IO.Path]::DirectorySeparatorChar

    return $ComparableChild.StartsWith($ParentWithSeparator, [System.StringComparison]::OrdinalIgnoreCase)
}

function Resolve-ExistingFilesystemPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$Description
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        throw "$Description is empty."
    }

    try {
        $Resolved = @(Resolve-Path -LiteralPath $Path -ErrorAction Stop)
    } catch {
        throw "$Description could not be resolved: $Path"
    }

    if ($Resolved.Count -ne 1) {
        throw "$Description did not resolve to exactly one path: $Path"
    }

    return [System.IO.Path]::GetFullPath($Resolved[0].ProviderPath)
}

function Test-ItemReparsePoint {
    param(
        [Parameter(Mandatory = $true)]
        [System.IO.FileSystemInfo]$Item
    )

    return (($Item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0)
}

function Assert-NoReparsePoint {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$Description
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    $Item = Get-Item -LiteralPath $Path -Force -ErrorAction Stop
    if (Test-ItemReparsePoint -Item $Item) {
        throw "$Description is a reparse point, symbolic link, or junction: $Path"
    }
}

function Find-ReparsePointUnderPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }

    $Stack = New-Object 'System.Collections.Generic.Stack[string]'
    $Stack.Push($Path)

    while ($Stack.Count -gt 0) {
        $CurrentPath = $Stack.Pop()
        $CurrentItem = Get-Item -LiteralPath $CurrentPath -Force -ErrorAction Stop
        if (Test-ItemReparsePoint -Item $CurrentItem) {
            return $CurrentItem.FullName
        }

        if (-not $CurrentItem.PSIsContainer) {
            continue
        }

        foreach ($Child in Get-ChildItem -LiteralPath $CurrentPath -Force -ErrorAction Stop) {
            if (Test-ItemReparsePoint -Item $Child) {
                return $Child.FullName
            }

            if ($Child.PSIsContainer) {
                $Stack.Push($Child.FullName)
            }
        }
    }

    return $null
}

function Test-PathContainsParentTraversal {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return $false
    }

    $Parts = $Path -split '[\\/]+' 
    foreach ($Part in $Parts) {
        if ($Part -eq '..') {
            return $true
        }
    }

    return $false
}

function Test-GeneratedAndroidBuildCleanupBoundary {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepositoryRoot,
        [Parameter(Mandatory = $true)]
        [string]$AppPath,
        [Parameter(Mandatory = $true)]
        [string]$TargetPath
    )

    if ([string]::IsNullOrWhiteSpace($RepositoryRoot)) {
        throw 'Repository root is empty.'
    }
    if ([string]::IsNullOrWhiteSpace($AppPath)) {
        throw 'App path is empty.'
    }
    if ([string]::IsNullOrWhiteSpace($TargetPath)) {
        throw 'Generated build target path is empty.'
    }

    $RootFull = [System.IO.Path]::GetFullPath($RepositoryRoot)
    $AppFull = [System.IO.Path]::GetFullPath($AppPath)
    $TargetFull = [System.IO.Path]::GetFullPath($TargetPath)
    $ExpectedApp = [System.IO.Path]::GetFullPath((Join-Path $RootFull 'app'))
    $ExpectedTarget = [System.IO.Path]::GetFullPath((Join-Path $ExpectedApp 'build'))
    $TargetFilesystemRoot = [System.IO.Path]::GetPathRoot($TargetFull)
    $RepositoryFilesystemRoot = [System.IO.Path]::GetPathRoot($RootFull)

    if (Test-PathSame -Left $RootFull -Right $RepositoryFilesystemRoot) {
        throw "Repository root must not be the filesystem root: $RootFull"
    }

    if ($env:USERPROFILE -and (Test-PathSame -Left $RootFull -Right $env:USERPROFILE)) {
        throw "Repository root must not be the user profile directory: $RootFull"
    }

    if (Test-PathSame -Left $TargetFull -Right $RootFull) {
        throw "Refusing cleanup because target equals repository root: $TargetFull"
    }

    if (Test-PathSame -Left $TargetFull -Right $TargetFilesystemRoot) {
        throw "Refusing cleanup because target equals filesystem root: $TargetFull"
    }

    if ($env:USERPROFILE -and (Test-PathSame -Left $TargetFull -Right $env:USERPROFILE)) {
        throw "Refusing cleanup because target equals user profile directory: $TargetFull"
    }

    if (Test-PathSame -Left $TargetFull -Right $AppFull) {
        throw "Refusing cleanup because target equals the app module directory: $TargetFull"
    }

    if (-not (Test-PathInside -Parent $RootFull -Child $TargetFull)) {
        throw "Refusing cleanup because target is outside repository root: $TargetFull"
    }

    if (-not (Test-PathSame -Left $AppFull -Right $ExpectedApp)) {
        throw "Refusing cleanup because app path is not the repository app module: $AppFull"
    }

    if (-not (Test-PathSame -Left $TargetFull -Right $ExpectedTarget)) {
        throw "Refusing cleanup because target is not exactly repository app/build: $TargetFull"
    }

    return [pscustomobject]@{
        RepositoryRoot = $RootFull
        AppPath = $AppFull
        TargetPath = $TargetFull
    }
}

function Assert-RepositoryIdentity {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepositoryRoot
    )

    if (-not (Test-Path -LiteralPath $RepositoryRoot -PathType Container)) {
        throw "Repository root is not a directory: $RepositoryRoot"
    }

    $GitMarker = Join-Path $RepositoryRoot '.git'
    $GradleWrapper = Join-Path $RepositoryRoot 'gradlew.bat'
    $SettingsGradle = Join-Path $RepositoryRoot 'settings.gradle'
    $SettingsGradleKts = Join-Path $RepositoryRoot 'settings.gradle.kts'
    $AppPath = Join-Path $RepositoryRoot 'app'
    $AppGradle = Join-Path $AppPath 'build.gradle'
    $AppGradleKts = Join-Path $AppPath 'build.gradle.kts'
    $AndroidManifest = Join-Path $AppPath 'src\main\AndroidManifest.xml'

    if (-not (Test-Path -LiteralPath $GitMarker)) {
        throw "Repository identity check failed: missing .git marker under $RepositoryRoot"
    }

    if (-not (Test-Path -LiteralPath $GradleWrapper -PathType Leaf)) {
        throw "Repository identity check failed: missing gradlew.bat under $RepositoryRoot"
    }

    if ((-not (Test-Path -LiteralPath $SettingsGradle -PathType Leaf)) -and
        (-not (Test-Path -LiteralPath $SettingsGradleKts -PathType Leaf))) {
        throw "Repository identity check failed: missing settings.gradle or settings.gradle.kts under $RepositoryRoot"
    }

    if (-not (Test-Path -LiteralPath $AppPath -PathType Container)) {
        throw "Repository identity check failed: missing app module directory under $RepositoryRoot"
    }

    Assert-NoReparsePoint -Path $AppPath -Description 'Repository app module directory'

    if ((-not (Test-Path -LiteralPath $AppGradle -PathType Leaf)) -and
        (-not (Test-Path -LiteralPath $AppGradleKts -PathType Leaf))) {
        throw "Repository identity check failed: missing app/build.gradle or app/build.gradle.kts under $RepositoryRoot"
    }

    if (-not (Test-Path -LiteralPath $AndroidManifest -PathType Leaf)) {
        throw "Repository identity check failed: missing app/src/main/AndroidManifest.xml under $RepositoryRoot"
    }
}

function Resolve-GeneratedAndroidBuildCleanupPlan {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepositoryRoot
    )

    $RootFull = Resolve-ExistingFilesystemPath -Path $RepositoryRoot -Description 'Repository root'
    $AppPath = [System.IO.Path]::GetFullPath((Join-Path $RootFull 'app'))
    $TargetPath = [System.IO.Path]::GetFullPath((Join-Path $AppPath 'build'))
    $Boundary = Test-GeneratedAndroidBuildCleanupBoundary -RepositoryRoot $RootFull -AppPath $AppPath -TargetPath $TargetPath
    Assert-RepositoryIdentity -RepositoryRoot $RootFull

    if (Test-Path -LiteralPath $Boundary.TargetPath) {
        if (-not (Test-Path -LiteralPath $Boundary.TargetPath -PathType Container)) {
            throw "Generated build target exists but is not a directory: $($Boundary.TargetPath)"
        }

        Assert-NoReparsePoint -Path $Boundary.TargetPath -Description 'Generated app/build directory'
        $NestedReparsePoint = Find-ReparsePointUnderPath -Path $Boundary.TargetPath
        if ($NestedReparsePoint) {
            throw "Generated build target contains a reparse point, symbolic link, or junction: $NestedReparsePoint"
        }
    }

    return [pscustomobject]@{
        RepositoryRoot = $Boundary.RepositoryRoot
        AppPath = $Boundary.AppPath
        TargetPath = $Boundary.TargetPath
        TargetExists = (Test-Path -LiteralPath $Boundary.TargetPath -PathType Container)
        GradleWrapperPath = [System.IO.Path]::GetFullPath((Join-Path $Boundary.RepositoryRoot 'gradlew.bat'))
    }
}

function Invoke-RepositoryGradleStop {
    param(
        [Parameter(Mandatory = $true)]
        [pscustomobject]$Plan
    )

    if (-not (Test-Path -LiteralPath $Plan.GradleWrapperPath -PathType Leaf)) {
        throw "Gradle wrapper could not be resolved: $($Plan.GradleWrapperPath)"
    }

    Write-Host "Stopping Gradle through repository wrapper: $($Plan.GradleWrapperPath)"
    Push-Location -LiteralPath $Plan.RepositoryRoot
    try {
        & $Plan.GradleWrapperPath --stop
        if ((-not $?) -or ($LASTEXITCODE -ne 0)) {
            throw "Gradle wrapper stop failed with exit code ${LASTEXITCODE}."
        }
    } finally {
        Pop-Location
    }
    Write-Host 'Gradle wrapper stop completed.'
}

function Remove-GeneratedAndroidBuildDirectory {
    param(
        [Parameter(Mandatory = $true)]
        [pscustomobject]$Plan
    )

    if (-not $Plan.TargetExists) {
        Write-Host "No generated Android build directory found: $($Plan.TargetPath)"
        return
    }

    Write-Host "Removing generated Android build directory: $($Plan.TargetPath)"
    try {
        Remove-Item -LiteralPath $Plan.TargetPath -Recurse -Force -ErrorAction Stop
    } catch {
        throw @"
Could not remove generated Android build directory: $($Plan.TargetPath)

Windows may still have an open handle under app/build. Close Android Studio windows, File Explorer windows, or terminals holding files there, then retry:
.\scripts\clear-generated-android-build.ps1 -DryRun
.\scripts\clear-generated-android-build.ps1 -StopGradle

No broader cleanup was attempted.
"@
    }

    if (Test-Path -LiteralPath $Plan.TargetPath) {
        throw "Generated Android build directory still exists after cleanup: $($Plan.TargetPath)"
    }

    Write-Host 'Generated Android build directory removed.'
}

function Invoke-GeneratedAndroidBuildCleanup {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepositoryRoot,
        [switch]$DryRun,
        [switch]$StopGradle
    )

    $Plan = Resolve-GeneratedAndroidBuildCleanupPlan -RepositoryRoot $RepositoryRoot

    Write-Host "Repository root: $($Plan.RepositoryRoot)"
    Write-Host "Generated build target: $($Plan.TargetPath)"

    if ($DryRun) {
        if ($StopGradle) {
            Write-Host 'Dry run: Gradle stop was requested but will not be invoked.'
        } else {
            Write-Host 'Dry run: Gradle will not be stopped.'
        }

        if ($Plan.TargetExists) {
            Write-Host "Dry run: would remove only $($Plan.TargetPath)"
        } else {
            Write-Host "Dry run: no generated Android build directory exists at $($Plan.TargetPath)"
        }
        return
    }

    if ($StopGradle) {
        Invoke-RepositoryGradleStop -Plan $Plan
    } else {
        Write-Host 'Gradle was not stopped. If a lock is present, rerun with -StopGradle after reviewing -DryRun output.'
    }

    Remove-GeneratedAndroidBuildDirectory -Plan $Plan
}

Export-ModuleMember -Function `
    Test-PathContainsParentTraversal, `
    Test-GeneratedAndroidBuildCleanupBoundary, `
    Resolve-GeneratedAndroidBuildCleanupPlan, `
    Invoke-GeneratedAndroidBuildCleanup
