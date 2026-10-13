[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidatePattern('^\d{12}$')][string]$ExpectedAccountId,
    [ValidateScript({ $_ -and $_ -ne 'default' })][string]$Profile = 'sentimentscribe-new'
)
. (Join-Path $PSScriptRoot 'common.ps1')
Initialize-Deployment -ExpectedAccountId $ExpectedAccountId -Profile $Profile
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$path = Join-Path $root 'deploy/ec2/.env.prod'
if (Test-Path -LiteralPath $path) { throw '.env.prod already exists. It will not be overwritten.' }
& git -C $root check-ignore --quiet deploy/ec2/.env.prod
if ($LASTEXITCODE -ne 0) { throw 'The production secret file is not ignored by Git. Refusing to create it.' }
$backendState = Get-Content -Raw (Join-Path $script:DeploymentState 'backend.json') | ConvertFrom-Json
if ($backendState.Account -ne $ExpectedAccountId) { throw 'Backend image state belongs to another account.' }

function Read-PrivateValue {
    param([string]$Prompt)
    $secure = Read-Host $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { $value = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        $secure.Dispose()
    }
    if ($value -notmatch '^[A-Za-z0-9._-]+$') { throw 'Use the provider client credential/API key, without spaces or quotes. No file was created.' }
    $value
}
function New-PrivateHex {
    $bytes = New-Object byte[] 32
    $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $random.GetBytes($bytes) } finally { $random.Dispose() }
    ([BitConverter]::ToString($bytes)).Replace('-', '').ToLowerInvariant()
}
Write-Host 'Enter provider credentials here only. Input is masked; values are never printed.'
$spotifyId = Read-PrivateValue 'Spotify Client ID'
$spotifySecret = Read-PrivateValue 'Spotify Client Secret'
$tmdb = Read-PrivateValue 'TMDb API key (v3, not the longer API Read Access Token)'
$content = @(
    "ECR_REGISTRY=$ExpectedAccountId.dkr.ecr.ca-central-1.amazonaws.com",
    "BACKEND_IMAGE_TAG=$($backendState.Tag)",
    'POSTGRES_USER=diary',
    "POSTGRES_PASSWORD=$(New-PrivateHex)",
    "SENTIMENTSCRIBE_JWT_SECRET=$(New-PrivateHex)",
    "SPOTIFY_CLIENT_ID=$spotifyId",
    "SPOTIFY_CLIENT_SECRET=$spotifySecret",
    "TMDB_API_KEY=$tmdb",
    'SENTIMENTSCRIBE_CORS_ORIGINS=https://sentimentscribe.cloud'
) -join "`n"
# Create an empty file with a user-only ACL before writing sensitive contents.
[System.IO.File]::WriteAllText($path, '', (New-Object System.Text.UTF8Encoding($false)))
$acl = New-Object System.Security.AccessControl.FileSecurity
$acl.SetAccessRuleProtection($true, $false)
$rule = New-Object System.Security.AccessControl.FileSystemAccessRule([System.Security.Principal.WindowsIdentity]::GetCurrent().Name, 'FullControl', 'Allow')
$acl.AddAccessRule($rule)
Set-Acl -LiteralPath $path -AclObject $acl
[System.IO.File]::WriteAllText($path, $content + "`n", (New-Object System.Text.UTF8Encoding($false)))
$content = $null
$spotifyId = $null
$spotifySecret = $null
$tmdb = $null
Write-Host 'Private .env.prod created and Git-ignore verified. Do not paste its contents into chat.'
