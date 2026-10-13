[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidatePattern('^\d{12}$')][string]$ExpectedAccountId,
    [ValidateScript({ $_ -and $_ -ne 'default' })][string]$Profile = 'sentimentscribe-new'
)
. (Join-Path $PSScriptRoot 'common.ps1')
Initialize-Deployment -ExpectedAccountId $ExpectedAccountId -Profile $Profile
$outputs = Get-FoundationOutputs
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$envPath = Join-Path $root 'deploy/ec2/.env.prod'
if (-not (Test-Path -LiteralPath $envPath)) { throw 'Run the masked configure-secrets.ps1 script in your own terminal first.' }
& git -C $root check-ignore --quiet deploy/ec2/.env.prod
if ($LASTEXITCODE -ne 0) { throw '.env.prod is not ignored by Git. Refusing secret transfer.' }
$state = Get-Content -Raw (Join-Path $script:DeploymentState 'backend.json') | ConvertFrom-Json
if ($state.Account -ne $ExpectedAccountId) { throw 'Backend image state belongs to another account.' }
Invoke-DeploymentAws @('ecr', 'describe-images', '--repository-name', 'sentimentscribe-backend', '--image-ids', "imageTag=$($state.Tag)") | Out-Null
$addresses = @(Resolve-DnsName api.sentimentscribe.cloud -Type A -Server 1.1.1.1 -ErrorAction Stop | Where-Object { $_.IPAddress } | ForEach-Object { $_.IPAddress })
if ($addresses.Count -ne 1 -or $addresses[0] -ne $outputs.ElasticIp) {
    throw "API DNS must point only to $($outputs.ElasticIp) before Caddy starts. No secrets were transferred."
}
$key = Join-Path $script:DeploymentState 'sentimentscribe-prod.pem'
$knownHosts = Join-Path $script:DeploymentState 'known_hosts'
$connection = @('-i', $key, '-o', 'StrictHostKeyChecking=accept-new', '-o', "UserKnownHostsFile=$knownHosts", '-o', 'BatchMode=yes', '-o', 'ConnectTimeout=15')
$hostName = "ubuntu@$($outputs.ElasticIp)"
$remoteDirectory = '/home/ubuntu/sentimentscribe/deploy/ec2'
foreach ($name in @('docker-compose.prod.yml', 'Caddyfile')) {
    & scp.exe @connection (Join-Path $root "deploy/ec2/$name") "${hostName}:${remoteDirectory}/$name"
    if ($LASTEXITCODE -ne 0) { throw "Failed to upload $name." }
}
& scp.exe @connection (Join-Path $PSScriptRoot 'start-backend.sh') "${hostName}:/home/ubuntu/sentimentscribe/start-backend.sh"
if ($LASTEXITCODE -ne 0) { throw 'Startup script upload failed.' }
& ssh.exe @connection $hostName "test -f $remoteDirectory/.env.prod"
$remoteExists = $LASTEXITCODE -eq 0
if (-not $remoteExists) {
    # Noclobber plus umask makes the file private before any secret bytes arrive.
    $prepare = "umask 077`nset -C`n: > $remoteDirectory/.env.prod"
    & ssh.exe @connection $hostName $prepare
    if ($LASTEXITCODE -ne 0) { throw 'Could not create a private remote environment file.' }
    & scp.exe @connection $envPath "${hostName}:${remoteDirectory}/.env.prod"
    if ($LASTEXITCODE -ne 0) { throw 'Private environment transfer failed.' }
}
$remoteHashOutput = & ssh.exe @connection $hostName "sha256sum $remoteDirectory/.env.prod"
if ($LASTEXITCODE -ne 0) { throw 'Could not verify private environment transfer.' }
$remoteHash = ($remoteHashOutput -split '\s+')[0]
if ($remoteHash -ne (Get-FileHash -Algorithm SHA256 $envPath).Hash.ToLowerInvariant()) {
    throw 'Remote environment differs from the local file. It was not overwritten. Review before rotating secrets.'
}
& ssh.exe @connection $hostName "sudo bash /home/ubuntu/sentimentscribe/start-backend.sh $ExpectedAccountId $($state.Tag)"
if ($LASTEXITCODE -ne 0) { throw 'Backend startup or internal health verification failed.' }
$health = $null
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    $result = & curl.exe --resolve "api.sentimentscribe.cloud:443:$($outputs.ElasticIp)" --fail --silent --connect-timeout 10 --max-time 20 https://api.sentimentscribe.cloud/api/health
    if ($LASTEXITCODE -eq 0) {
        $health = $result | ConvertFrom-Json
        if ($health.status -eq 'ok') { break }
    }
    Start-Sleep -Seconds 10
}
if (-not $health -or $health.status -ne 'ok') { throw 'Caddy HTTPS health check did not pass. Do not proceed to frontend.' }
Save-DeploymentJson 'backend-health.json' @{ Account = $ExpectedAccountId; Ip = $outputs.ElasticIp; Tag = $state.Tag; Status = $health.status; VerifiedAt = (Get-Date).ToUniversalTime().ToString('o') } | Out-Null
Write-Host 'Verified https://api.sentimentscribe.cloud/api/health with valid TLS against the new instance.'
