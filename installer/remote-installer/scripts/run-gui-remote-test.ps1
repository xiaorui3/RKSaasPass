param(
  [ValidateSet('validate', 'preflight', 'install')]
  [string] $Command = 'preflight',
  [string] $LogDir = ''
)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$PackageRoot = Resolve-Path (Join-Path $ScriptDir '..')
$NodeScript = Join-Path $ScriptDir 'run-gui-remote-test.mjs'

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
  throw 'node is required to run the remote GUI test runner.'
}

$secure = Read-Host -Prompt 'SSH password for RK-Web remote test machine' -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
$plain = ''

try {
  $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
  if ([string]::IsNullOrWhiteSpace($plain)) {
    throw 'SSH password is required.'
  }

  $env:RK_TEST_SSH_PASSWORD = $plain
  Push-Location $PackageRoot
  try {
    if ([string]::IsNullOrWhiteSpace($LogDir)) {
      & node $NodeScript $Command
    } else {
      & node $NodeScript $Command --log-dir $LogDir
    }
    exit $LASTEXITCODE
  } finally {
    Pop-Location
  }
} finally {
  Remove-Item Env:\RK_TEST_SSH_PASSWORD -ErrorAction SilentlyContinue
  if ($bstr -ne [IntPtr]::Zero) {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
  }
  $plain = ''
}
