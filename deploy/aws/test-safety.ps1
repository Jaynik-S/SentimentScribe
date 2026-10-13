$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')

# Replace AWS with an offline identity stub; this test makes no network calls.
$script:GuardCalls = 0
function Invoke-DeploymentAws {
    param([string[]]$Arguments, [string]$Region)
    $script:GuardCalls++
    if ($Arguments[0] -ne 'sts') { throw 'Unexpected resource API call.' }
    [pscustomobject]@{ Account = '111111111111'; Arn = 'offline-test' }
}
try {
    Initialize-Deployment -ExpectedAccountId '920235987728' -Profile sentimentscribe-new
    throw 'Account guard did not reject the wrong account.'
} catch {
    if ($_.Exception.Message -notlike 'Account mismatch:*') { throw }
    if ($script:GuardCalls -ne 1) { throw 'Unexpected AWS calls after account mismatch.' }
}
try {
    Initialize-Deployment -ExpectedAccountId '920235987728' -Profile default
    throw 'Default profile was not rejected.'
} catch {
    if ($_.Exception.Message -notlike '*validation*') { throw }
    if ($script:GuardCalls -ne 1) { throw 'AWS called despite an invalid profile.' }
}
Write-Host 'PASS: wrong account and default profile rejected before resource operations.'
