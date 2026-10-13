[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidatePattern('^\d{12}$')][string]$ExpectedAccountId,
    [ValidateScript({ $_ -and $_ -ne 'default' })][string]$Profile = 'sentimentscribe-new'
)
. (Join-Path $PSScriptRoot 'common.ps1')
Initialize-Deployment -ExpectedAccountId $ExpectedAccountId -Profile $Profile
$outputs = Get-FoundationOutputs
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$key = Join-Path $script:DeploymentState 'sentimentscribe-prod.pem'
$knownHosts = Join-Path $script:DeploymentState 'known_hosts'
$archive = Join-Path $script:DeploymentState 'backend-source.tar.gz'
& tar.exe -czf $archive -C (Join-Path $root 'backend') Dockerfile pom.xml src .dockerignore
if ($LASTEXITCODE -ne 0) { throw 'Backend source archive failed.' }
$hash = (Get-FileHash -Algorithm SHA256 $archive).Hash.ToLowerInvariant()
$statePath = Join-Path $script:DeploymentState 'backend.json'
$state = if (Test-Path -LiteralPath $statePath) { Get-Content -Raw -LiteralPath $statePath | ConvertFrom-Json } else { $null }
if (-not $state -or $state.Account -ne $ExpectedAccountId -or $state.SourceHash -ne $hash) {
    $state = @{ Account = $ExpectedAccountId; SourceHash = $hash; Tag = ('rehost-' + (Get-Date).ToUniversalTime().ToString('yyyyMMddHHmmss') + '-' + $hash.Substring(0, 8)) }
    Save-DeploymentJson 'backend.json' $state | Out-Null
}
$existing = @(Invoke-DeploymentAws @('ecr', 'list-images', '--repository-name', 'sentimentscribe-backend', '--query', "imageIds[?imageTag=='$($state.Tag)']"))
if ($existing.Count -gt 0) {
    Write-Host "Previously pushed image $($state.Tag) exists; no rebuild attempted."
    exit
}
$connection = @('-i', $key, '-o', 'StrictHostKeyChecking=accept-new', '-o', "UserKnownHostsFile=$knownHosts", '-o', 'BatchMode=yes', '-o', 'ConnectTimeout=15')
$hostName = "ubuntu@$($outputs.ElasticIp)"
& ssh.exe @connection $hostName 'mkdir -p /home/ubuntu/sentimentscribe/backend'
if ($LASTEXITCODE -ne 0) { throw 'Remote backend directory creation failed.' }
& scp.exe @connection $archive "${hostName}:/home/ubuntu/sentimentscribe/backend-source.tar.gz"
if ($LASTEXITCODE -ne 0) { throw 'Backend source upload failed.' }
& scp.exe @connection (Join-Path $PSScriptRoot 'build-backend.sh') "${hostName}:/home/ubuntu/sentimentscribe/build-backend.sh"
if ($LASTEXITCODE -ne 0) { throw 'Build script upload failed.' }
& ssh.exe @connection $hostName 'tar -xzf /home/ubuntu/sentimentscribe/backend-source.tar.gz -C /home/ubuntu/sentimentscribe/backend'
if ($LASTEXITCODE -ne 0) { throw 'Backend source extraction failed.' }
& ssh.exe @connection $hostName "sudo bash /home/ubuntu/sentimentscribe/build-backend.sh $ExpectedAccountId $($state.Tag)"
if ($LASTEXITCODE -ne 0) { throw 'Backend Docker build/push failed. Check the preceding build output.' }
$image = Invoke-DeploymentAws @('ecr', 'describe-images', '--repository-name', 'sentimentscribe-backend', '--image-ids', "imageTag=$($state.Tag)", '--query', 'imageDetails[0].{Digest:imageDigest,Size:imageSizeInBytes,Tags:imageTags}')
Save-DeploymentJson 'backend-image.json' $image | Out-Null
Write-Host "Verified ECR image $($state.Tag), digest $($image.Digest)."
