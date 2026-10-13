[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidatePattern('^\d{12}$')][string]$ExpectedAccountId,
    [ValidateScript({ $_ -and $_ -ne 'default' })][string]$Profile = 'sentimentscribe-new',
    [switch]$DeferSpotify
)
. (Join-Path $PSScriptRoot 'common.ps1')

function Get-SpotifyEnvironmentUpdate {
    param([string]$Content, [string]$ClientId, [string]$ClientSecret, [switch]$DeferSpotify)
    if ($DeferSpotify -and ($ClientId -ne '' -or $ClientSecret -ne '')) {
        throw 'Deferred Spotify credentials must both be blank.'
    }
    if (-not $DeferSpotify -and ($ClientId -notmatch '^[A-Za-z0-9._-]+$' -or $ClientSecret -notmatch '^[A-Za-z0-9._-]+$')) {
        throw 'Provider credentials must contain no spaces, quotes, or shell characters.'
    }
    foreach ($key in @('SPOTIFY_CLIENT_ID', 'SPOTIFY_CLIENT_SECRET')) {
        if ([regex]::Matches($Content, "(?m)^$key=[^\r\n]*").Count -ne 1) {
            throw "Expected exactly one $key field. No file was changed."
        }
    }
    $updated = [regex]::Replace($Content, '(?m)^SPOTIFY_CLIENT_ID=[^\r\n]*', "SPOTIFY_CLIENT_ID=$ClientId")
    [regex]::Replace($updated, '(?m)^SPOTIFY_CLIENT_SECRET=[^\r\n]*', "SPOTIFY_CLIENT_SECRET=$ClientSecret")
}

function Read-SpotifyPrivateValue {
    param([string]$Prompt)
    $secure = Read-Host $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        $secure.Dispose()
    }
}

Initialize-Deployment -ExpectedAccountId $ExpectedAccountId -Profile $Profile
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$path = Join-Path $root 'deploy/ec2/.env.prod'
& git -C $root check-ignore --quiet deploy/ec2/.env.prod
if ($LASTEXITCODE -ne 0) { throw 'Production secrets must remain ignored by Git.' }
$original = [System.IO.File]::ReadAllText($path)
if ($original -notmatch "(?m)^ECR_REGISTRY=$ExpectedAccountId\.dkr\.ecr\.ca-central-1\.amazonaws\.com\r?$") {
    throw 'Environment file does not belong to the verified deployment account.'
}
if ($DeferSpotify) {
    $updated = Get-SpotifyEnvironmentUpdate $original '' '' -DeferSpotify
} else {
    Write-Host 'Enter the matching Spotify app client ID and client secret. Input is masked. Database, JWT, and TMDb settings will not change.'
    $clientId = Read-SpotifyPrivateValue 'Spotify Client ID'
    $clientSecret = Read-SpotifyPrivateValue 'Spotify Client Secret'
    $updated = Get-SpotifyEnvironmentUpdate $original $clientId $clientSecret

    # Authenticate before changing the local file; never print provider responses or tokens.
    $basic = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${clientId}:$clientSecret"))
    try {
        $token = Invoke-RestMethod -Method Post -Uri 'https://accounts.spotify.com/api/token' -Headers @{ Authorization = "Basic $basic" } -ContentType 'application/x-www-form-urlencoded' -Body @{ grant_type = 'client_credentials' }
        if (-not $token.access_token) { throw 'No token.' }
    } catch {
        throw 'Spotify authentication failed. No file was changed. Recheck the matching app Client ID and Client Secret; do not paste credentials into chat.'
    } finally {
        $token = $null
        $basic = $null
        $clientId = $null
        $clientSecret = $null
    }
}
$stamp = (Get-Date).ToUniversalTime().ToString('yyyyMMddHHmmssfff')
$temporary = Join-Path $script:DeploymentState "spotify-update-$stamp.env"
$backup = Join-Path $script:DeploymentState "before-spotify-$stamp.env"
if ((Test-Path $temporary) -or (Test-Path $backup)) { throw 'Private staging paths already exist. No file was changed.' }
if ([System.IO.File]::ReadAllText($path) -cne $original) { throw 'Environment changed during credential entry. No file was changed.' }
[System.IO.File]::WriteAllText($temporary, '', (New-Object System.Text.UTF8Encoding($false)))
Set-Acl -LiteralPath $temporary -AclObject (Get-Acl -LiteralPath $path)
[System.IO.File]::WriteAllText($temporary, $updated, (New-Object System.Text.UTF8Encoding($false)))
[System.IO.File]::Replace($temporary, $path, $backup)
$original = $null
$updated = $null
if ($DeferSpotify) {
    Write-Host 'Only the local Spotify fields were blanked explicitly. Other settings preserved; private rollback copy retained. Apply to EC2 with apply-spotify-credentials.ps1 -DeferSpotify.'
} else {
    Write-Host 'Spotify authentication passed. Only the local Spotify fields were updated; all other values were preserved. An ignored private rollback copy was retained. Apply to EC2 with apply-spotify-credentials.ps1 using the same account/profile.'
}
