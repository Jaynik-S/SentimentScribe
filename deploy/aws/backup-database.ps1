[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidatePattern('^\d{12}$')][string]$ExpectedAccountId,
    [ValidateScript({ $_ -and $_ -ne 'default' })][string]$Profile = 'sentimentscribe-new'
)
. (Join-Path $PSScriptRoot 'common.ps1')
Initialize-Deployment -ExpectedAccountId $ExpectedAccountId -Profile $Profile
$foundation = Get-FoundationOutputs
$instance = Invoke-DeploymentAws @('ec2', 'describe-instances', '--instance-ids', $foundation.InstanceId)
if ($instance.Reservations[0].Instances[0].PublicIpAddress -ne $foundation.ElasticIp) { throw 'Backup target IP does not match the verified instance.' }
$connection = @('-i', (Join-Path $script:DeploymentState 'sentimentscribe-prod.pem'), '-o', 'StrictHostKeyChecking=yes', '-o', "UserKnownHostsFile=$(Join-Path $script:DeploymentState 'known_hosts')", '-o', 'BatchMode=yes', '-o', 'ConnectTimeout=15')
$hostName = "ubuntu@$($foundation.ElasticIp)"
$stamp = (Get-Date).ToUniversalTime().ToString('yyyyMMddHHmmssfff')
$remote = "/home/ubuntu/.local/share/sentimentscribe-backups/sentimentscribe-$stamp.dump"
$local = Join-Path $script:DeploymentState "sentimentscribe-$stamp.dump"
$imageId = & docker.exe image inspect postgres:15 --format '{{.Id}}'
if ($LASTEXITCODE -ne 0 -or $imageId -notmatch '^sha256:[0-9a-f]{64}$') { throw 'The existing local PostgreSQL 15 image is required for the isolated restore test.' }
$export = @"
set -eu
test "`$(aws sts get-caller-identity --query Account --output text --region ca-central-1)" = '$ExpectedAccountId'
umask 077
install -d -m 700 /home/ubuntu/.local/share/sentimentscribe-backups
set -C
sudo docker exec sentimentscribe-postgres pg_dump -U diary -d sentiment_scribe -Fc > '$remote'
test -s '$remote'
test "`$(stat -c '%a' '$remote')" = 600
"@
& ssh.exe @connection $hostName $export
if ($LASTEXITCODE -ne 0) { throw 'Private database dump failed.' }
# Protect the empty destination before any backup bytes are transferred.
$stream = [System.IO.File]::Open($local, [System.IO.FileMode]::CreateNew)
$stream.Dispose()
$acl = New-Object System.Security.AccessControl.FileSecurity
$acl.SetAccessRuleProtection($true, $false)
$acl.AddAccessRule((New-Object System.Security.AccessControl.FileSystemAccessRule([System.Security.Principal.WindowsIdentity]::GetCurrent().Name, 'FullControl', 'Allow')))
Set-Acl -LiteralPath $local -AclObject $acl
& scp.exe @connection "${hostName}:$remote" $local
if ($LASTEXITCODE -ne 0) { throw 'Private off-instance backup transfer failed.' }
$hashOutput = & ssh.exe @connection $hostName "sha256sum '$remote'"
if ($LASTEXITCODE -ne 0 -or ($hashOutput -split '\s+')[0] -ne (Get-FileHash -Algorithm SHA256 $local).Hash.ToLowerInvariant()) { throw 'Off-instance backup hash verification failed.' }
$stream = [System.IO.File]::OpenRead($local)
try {
    $header = New-Object byte[] 5
    if ($stream.Read($header, 0, 5) -ne 5 -or [Text.Encoding]::ASCII.GetString($header) -ne 'PGDMP') { throw 'Backup is not a PostgreSQL custom-format dump.' }
} finally { $stream.Dispose() }
$name = "sentimentscribe-restore-check-$stamp"
$created = $false
try {
    & docker.exe run --detach --pull never --name $name --label Application=SentimentScribe --label Purpose=backup-restore-verification --network none --memory 512m --cpus 1 --env POSTGRES_DB=restore_check --env POSTGRES_USER=restore_check --env POSTGRES_HOST_AUTH_METHOD=trust --mount "type=bind,source=$local,target=/backup.dump,readonly" $imageId | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Isolated local restore container creation failed.' }
    $created = $true
    $ready = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        & docker.exe exec $name pg_isready -U restore_check -d restore_check *> $null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) { throw 'Isolated restore database did not become ready.' }
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $restoreOutput = & docker.exe exec $name pg_restore --exit-on-error --no-owner --no-privileges -U restore_check -d restore_check /backup.dump 2>&1
        $restoreCode = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previousPreference }
    $restoreOutput = $null
    if ($restoreCode -ne 0) { throw 'Isolated backup restoration failed; private dump retained. Restore output was suppressed to protect account data.' }
    $counts = & docker.exe exec $name psql -U restore_check -d restore_check -Atc 'SELECT (SELECT count(*) FROM users), (SELECT count(*) FROM diary_entries), (SELECT count(*) FROM flyway_schema_history WHERE success);'
    if ($LASTEXITCODE -ne 0 -or $counts -notmatch '^\d+\|\d+\|\d+$') { throw 'Restored database verification failed.' }
    $numbers = $counts -split '\|'
    if ([int]$numbers[0] -lt 1 -or [int]$numbers[1] -lt 1 -or [int]$numbers[2] -lt 1) { throw 'Restored test account, entry, or migration is missing.' }
    Save-DeploymentJson 'backup-restore.json' @{ Account = $ExpectedAccountId; LocalDump = $local; RemoteDump = $remote; Bytes = (Get-Item $local).Length; Users = [int]$numbers[0]; Entries = [int]$numbers[1]; SuccessfulMigrations = [int]$numbers[2]; RestoreContainer = $name; RestoreVerified = $true; VerifiedAt = (Get-Date).ToUniversalTime().ToString('o') } | Out-Null
    Write-Host 'Private off-instance dump hash and isolated restore passed. User/entry/migration counts were verified without printing account data. This is a one-time backup, not a recurring schedule.'
} finally {
    if ($created) {
        & docker.exe stop $name | Out-Null
        if ($LASTEXITCODE -ne 0) { Write-Warning "Could not stop the isolated restore container $name. Review its status." }
        else { Write-Host "Stopped isolated restore container $name; its private data and dump were retained. No production container was stopped or deleted." }
    }
}
