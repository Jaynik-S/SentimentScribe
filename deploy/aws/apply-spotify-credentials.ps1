[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidatePattern('^\d{12}$')][string]$ExpectedAccountId,
    [ValidateScript({ $_ -and $_ -ne 'default' })][string]$Profile = 'sentimentscribe-new',
    [switch]$DeferSpotify
)
. (Join-Path $PSScriptRoot 'common.ps1')
Initialize-Deployment -ExpectedAccountId $ExpectedAccountId -Profile $Profile
$foundation = Get-FoundationOutputs
$instance = Invoke-DeploymentAws @('ec2', 'describe-instances', '--instance-ids', $foundation.InstanceId)
$actual = $instance.Reservations[0].Instances[0]
if ($actual.State.Name -ne 'running' -or $actual.PublicIpAddress -ne $foundation.ElasticIp) { throw 'Target instance/IP verification failed.' }
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$environment = Join-Path $root 'deploy/ec2/.env.prod'
& git -C $root check-ignore --quiet deploy/ec2/.env.prod
if ($LASTEXITCODE -ne 0) { throw 'Production environment must be ignored by Git.' }
$connection = @('-i', (Join-Path $script:DeploymentState 'sentimentscribe-prod.pem'), '-o', 'StrictHostKeyChecking=yes', '-o', "UserKnownHostsFile=$(Join-Path $script:DeploymentState 'known_hosts')", '-o', 'BatchMode=yes', '-o', 'ConnectTimeout=15')
$hostName = "ubuntu@$($foundation.ElasticIp)"
$remoteDirectory = '/home/ubuntu/.local/share/sentimentscribe-secrets'
$stamp = (Get-Date).ToUniversalTime().ToString('yyyyMMddHHmmssfff')
$candidate = "$remoteDirectory/spotify-candidate-$stamp.env"
$prepare = @"
set -eu
test "`$(aws sts get-caller-identity --query Account --output text --region ca-central-1)" = '$ExpectedAccountId'
umask 077
install -d -m 700 '$remoteDirectory'
set -C
: > '$candidate'
"@
& ssh.exe @connection $hostName $prepare
if ($LASTEXITCODE -ne 0) { throw 'Could not prepare a private candidate on the verified instance.' }
& scp.exe @connection $environment "${hostName}:$candidate"
if ($LASTEXITCODE -ne 0) { throw 'Private candidate transfer failed.' }
$hashOutput = & ssh.exe @connection $hostName "sha256sum '$candidate'"
if ($LASTEXITCODE -ne 0 -or ($hashOutput -split '\s+')[0] -ne (Get-FileHash -Algorithm SHA256 $environment).Hash.ToLowerInvariant()) { throw 'Private candidate transfer verification failed.' }
foreach ($name in @('apply-spotify-credentials.sh', 'verify-providers.sh')) {
    & scp.exe @connection (Join-Path $PSScriptRoot $name) "${hostName}:/home/ubuntu/sentimentscribe/$name"
    if ($LASTEXITCODE -ne 0) { throw "Script upload failed: $name." }
}
$mode = if ($DeferSpotify) { ' --defer-spotify' } else { '' }
& ssh.exe @connection $hostName "bash /home/ubuntu/sentimentscribe/apply-spotify-credentials.sh $ExpectedAccountId '$candidate'$mode"
if ($LASTEXITCODE -ne 0) { throw 'Spotify-only update or backend health failed. Review before further changes.' }
$health = Invoke-RestMethod https://api.sentimentscribe.cloud/api/health
if ($health.status -ne 'ok') { throw 'Public backend HTTPS health failed.' }
& ssh.exe @connection $hostName "bash /home/ubuntu/sentimentscribe/verify-providers.sh $ExpectedAccountId$mode"
if ($LASTEXITCODE -ne 0) { throw 'Provider verification did not pass; no database/JWT/TMDb settings were changed.' }
$spotifyResult = if ($DeferSpotify) { 'deferred; both fields blank' } else { 'passed' }
Save-DeploymentJson 'provider-health.json' @{ Account = $ExpectedAccountId; Spotify = $spotifyResult; TMDb = 'passed'; Backend = $health.status; VerifiedAt = (Get-Date).ToUniversalTime().ToString('o') } | Out-Null
Write-Host "Public backend HTTPS and TMDb passed. Spotify: $spotifyResult. No credentials printed."
