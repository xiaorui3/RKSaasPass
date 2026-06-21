[CmdletBinding()]
param(
  [string]$AndroidSdk = '',
  [string]$JavaHome = '',
  [string]$VersionName = '0.0.43',
  [int]$VersionCode = 44
)

$ErrorActionPreference = 'Stop'

$OriginalRootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$OriginalDistDir = Join-Path $OriginalRootDir 'dist'
$RelativeApkPath = 'dist/rk-club-debug.apk'
$FinalApk = Join-Path $OriginalRootDir $RelativeApkPath
$StageDir = Join-Path ([System.IO.Path]::GetTempPath()) ("rkclub-android-build-" + [Guid]::NewGuid().ToString('N'))

function Resolve-AndroidSdk {
  param([string]$Requested)
  if ($Requested) { return $Requested }
  if ($env:ANDROID_HOME) { return $env:ANDROID_HOME }
  if ($env:ANDROID_SDK_ROOT) { return $env:ANDROID_SDK_ROOT }
  return (Join-Path $env:LOCALAPPDATA 'Android\Sdk')
}

function Test-JavaHome {
  param([string]$Path)
  if (-not $Path) { return $false }
  $java = Join-Path $Path 'bin\java.exe'
  $javac = Join-Path $Path 'bin\javac.exe'
  $jar = Join-Path $Path 'bin\jar.exe'
  return (Test-Path $java) -and (Test-Path $javac) -and (Test-Path $jar)
}

function Resolve-JavaHome {
  param([string]$Requested)

  $candidates = New-Object System.Collections.Generic.List[string]
  if ($Requested) { $candidates.Add($Requested) }
  if ($env:JAVA_HOME) { $candidates.Add($env:JAVA_HOME) }

  $knownRoots = @(
    (Join-Path $env:USERPROFILE '.jdks'),
    (Join-Path $env:ProgramFiles 'Java'),
    (Join-Path $env:ProgramFiles 'Microsoft\jdk'),
    (Join-Path $env:ProgramFiles 'Eclipse Adoptium')
  )
  foreach ($root in $knownRoots) {
    if (Test-Path $root) {
      Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        ForEach-Object { $candidates.Add($_.FullName) }
    }
  }

  foreach ($candidate in $candidates) {
    if (Test-JavaHome $candidate) {
      return (Resolve-Path $candidate).Path
    }
  }

  $javac = Get-Command 'javac.exe' -ErrorAction SilentlyContinue
  if ($javac) {
    $bin = Split-Path -Parent $javac.Source
    $home = Split-Path -Parent $bin
    if (Test-JavaHome $home) {
      return (Resolve-Path $home).Path
    }
  }

  throw "Valid JDK not found. Pass -JavaHome or install a JDK with java.exe, javac.exe, and jar.exe."
}

function Resolve-Exe {
  param(
    [string]$Name,
    [string]$PreferredPath
  )
  if ($PreferredPath -and (Test-Path $PreferredPath)) {
    return (Resolve-Path $PreferredPath).Path
  }
  $command = Get-Command $Name -ErrorAction SilentlyContinue
  if ($command) {
    return $command.Source
  }
  throw "Required tool not found: $Name"
}

function Invoke-Tool {
  param(
    [string]$Exe,
    [object[]]$Arguments
  )
  & $Exe @Arguments
  if ($LASTEXITCODE -ne 0) {
    throw "Command failed with exit code ${LASTEXITCODE}: $Exe $($Arguments -join ' ')"
  }
}

try {
  $AndroidSdk = Resolve-AndroidSdk $AndroidSdk
  if (-not (Test-Path $AndroidSdk)) {
    throw "Android SDK not found: $AndroidSdk"
  }

  $ResolvedJavaHome = Resolve-JavaHome $JavaHome
  $env:JAVA_HOME = $ResolvedJavaHome
  $env:PATH = (Join-Path $ResolvedJavaHome 'bin') + [System.IO.Path]::PathSeparator + $env:PATH
  $JavaHome = $ResolvedJavaHome

  $PlatformDir = Get-ChildItem (Join-Path $AndroidSdk 'platforms') -Directory |
    Sort-Object Name -Descending |
    Where-Object { Test-Path (Join-Path $_.FullName 'android.jar') } |
    Select-Object -First 1
  if (-not $PlatformDir) {
    throw "No Android platform with android.jar found under $AndroidSdk"
  }

  $BuildToolsDir = Get-ChildItem (Join-Path $AndroidSdk 'build-tools') -Directory |
    Sort-Object Name -Descending |
    Select-Object -First 1
  if (-not $BuildToolsDir) {
    throw "No Android build-tools found under $AndroidSdk"
  }

  $AndroidJar = Join-Path $PlatformDir.FullName 'android.jar'
  $Aapt2 = Resolve-Exe 'aapt2.exe' (Join-Path $BuildToolsDir.FullName 'aapt2.exe')
  $D8 = Resolve-Exe 'd8.bat' (Join-Path $BuildToolsDir.FullName 'd8.bat')
  $ZipAlign = Resolve-Exe 'zipalign.exe' (Join-Path $BuildToolsDir.FullName 'zipalign.exe')
  $ApkSigner = Resolve-Exe 'apksigner.bat' (Join-Path $BuildToolsDir.FullName 'apksigner.bat')
  $JavacPreferred = if ($JavaHome) { Join-Path $JavaHome 'bin\javac.exe' } else { '' }
  $JarPreferred = if ($JavaHome) { Join-Path $JavaHome 'bin\jar.exe' } else { '' }
  $Javac = Resolve-Exe 'javac.exe' $JavacPreferred
  $Jar = Resolve-Exe 'jar.exe' $JarPreferred

  New-Item -ItemType Directory -Force -Path $StageDir, $OriginalDistDir | Out-Null
  Copy-Item -LiteralPath (Join-Path $OriginalRootDir 'app') -Destination $StageDir -Recurse -Force

  $RootDir = $StageDir
  $AppDir = Join-Path $RootDir 'app'
  $BuildDir = Join-Path $RootDir 'build'
  $DistDir = Join-Path $RootDir 'dist'
  $StageApk = Join-Path $DistDir 'rk-club-debug.apk'

  New-Item -ItemType Directory -Force -Path $BuildDir, $DistDir | Out-Null

  $FlatDir = Join-Path $BuildDir 'compiled-res'
  $GenDir = Join-Path $BuildDir 'generated'
  $ClassesDir = Join-Path $BuildDir 'classes'
  $DexDir = Join-Path $BuildDir 'dex'
  $UnsignedApk = Join-Path $BuildDir 'rk-club-unsigned.apk'
  $AlignedApk = Join-Path $BuildDir 'rk-club-aligned.apk'
  $Keystore = Join-Path $OriginalRootDir 'signing\rkclub-release.keystore'
  if (-not (Test-Path $Keystore)) {
    throw "Stable signing keystore not found: $Keystore"
  }

  New-Item -ItemType Directory -Force -Path $FlatDir, $GenDir, $ClassesDir, $DexDir | Out-Null

  Invoke-Tool $Aapt2 @('compile', '--dir', (Join-Path $AppDir 'src\main\res'), '-o', $FlatDir)
  $FlatFiles = Get-ChildItem $FlatDir -Filter '*.flat' -Recurse | ForEach-Object { $_.FullName }
  $LinkArgs = @(
    'link',
    '-I', $AndroidJar,
    '--manifest', (Join-Path $AppDir 'src\main\AndroidManifest.xml'),
    '--java', $GenDir,
    '--min-sdk-version', '23',
    '--target-sdk-version', '36',
    '--version-name', $VersionName,
    '--version-code', $VersionCode,
    '-o', $UnsignedApk
  ) + $FlatFiles
  Invoke-Tool $Aapt2 $LinkArgs

  $JavaSources = @()
  $JavaSources += Get-ChildItem (Join-Path $AppDir 'src\main\java') -Filter '*.java' -Recurse | ForEach-Object { $_.FullName }
  $JavaSources += Get-ChildItem $GenDir -Filter '*.java' -Recurse | ForEach-Object { $_.FullName }
  $SourcesFile = Join-Path $BuildDir 'sources.txt'
  $JavaSources | Set-Content -Encoding ASCII $SourcesFile

  Invoke-Tool $Javac @('-encoding', 'UTF-8', '-source', '8', '-target', '8', '-bootclasspath', $AndroidJar, '-d', $ClassesDir, "@$SourcesFile")

  $ClassFiles = Get-ChildItem $ClassesDir -Filter '*.class' -Recurse | ForEach-Object { $_.FullName }
  $ClassFilesFile = Join-Path $BuildDir 'class-files.txt'
  $ClassFiles | Set-Content -Encoding ASCII $ClassFilesFile
  Invoke-Tool $D8 @('--lib', $AndroidJar, '--min-api', '23', '--output', $DexDir, "@$ClassFilesFile")
  Invoke-Tool $Jar @('uf', $UnsignedApk, '-C', $DexDir, 'classes.dex')
  Invoke-Tool $ZipAlign @('-f', '-p', '4', $UnsignedApk, $AlignedApk)

  Invoke-Tool $ApkSigner @(
    'sign',
    '--ks', $Keystore,
    '--ks-key-alias', 'rkclub',
    '--ks-pass', 'pass:rkclub2026',
    '--key-pass', 'pass:rkclub2026',
    '--out', $StageApk,
    $AlignedApk
  )
  Invoke-Tool $ApkSigner @('verify', $StageApk)

  Copy-Item -LiteralPath $StageApk -Destination $FinalApk -Force
  Write-Host "APK built: $FinalApk"
  Write-Host "Version: $VersionName ($VersionCode)"
}
finally {
  if (Test-Path $StageDir) {
    Remove-Item -LiteralPath $StageDir -Recurse -Force
  }
}
