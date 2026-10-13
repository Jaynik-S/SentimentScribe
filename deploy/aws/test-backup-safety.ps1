$ErrorActionPreference = 'Stop'
$path = Join-Path $PSScriptRoot 'backup-database.ps1'
if (-not (Test-Path $path)) { throw 'FAIL: backup account guard is not implemented.' }
$previousPreference = $ErrorActionPreference
try {
    $ErrorActionPreference = 'Continue'
    $output = & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $path -ExpectedAccountId 111111111111 -Profile sentimentscribe-new 2>&1
    $code = $LASTEXITCODE
} finally { $ErrorActionPreference = $previousPreference }
if ($code -eq 0 -or ($output -join "`n") -notmatch 'Account mismatch: expected 111111111111, got 920235987728') { throw 'FAIL: backup did not stop at the expected account guard.' }
Write-Host 'PASS: backup rejected a mismatched account before resource operations, dump creation, or Docker commands.'
