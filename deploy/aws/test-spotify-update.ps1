$ErrorActionPreference = 'Stop'
$path = Join-Path $PSScriptRoot 'update-spotify-credentials.ps1'
if (-not (Test-Path $path)) { throw 'FAIL: targeted Spotify credential update is not implemented.' }
$errors = $null
$ast = [System.Management.Automation.Language.Parser]::ParseFile($path, [ref]$null, [ref]$errors)
if ($errors) { throw 'Credential helper syntax failed.' }
$definition = $ast.Find({ param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Get-SpotifyEnvironmentUpdate' }, $true)
if (-not $definition) { throw 'FAIL: targeted Spotify credential update is not implemented.' }
Invoke-Expression $definition.Extent.Text

$original = "POSTGRES_PASSWORD=db-fixture`nSENTIMENTSCRIBE_JWT_SECRET=jwt-fixture`nSPOTIFY_CLIENT_ID=old-id`nSPOTIFY_CLIENT_SECRET=old-secret`nTMDB_API_KEY=tmdb-fixture`n"
$expected = "POSTGRES_PASSWORD=db-fixture`nSENTIMENTSCRIBE_JWT_SECRET=jwt-fixture`nSPOTIFY_CLIENT_ID=new-id`nSPOTIFY_CLIENT_SECRET=new-secret`nTMDB_API_KEY=tmdb-fixture`n"
if ((Get-SpotifyEnvironmentUpdate $original 'new-id' 'new-secret') -cne $expected) { throw 'FAIL: unrelated secrets changed or Spotify values were not updated.' }
if ((Get-SpotifyEnvironmentUpdate ($original.Replace("`n", "`r`n")) 'new-id' 'new-secret') -cne $expected.Replace("`n", "`r`n")) { throw 'FAIL: line endings changed.' }
foreach ($invalid in @($original.Replace("SPOTIFY_CLIENT_ID=old-id`n", ''), ($original + "SPOTIFY_CLIENT_ID=duplicate`n"))) {
    try {
        Get-SpotifyEnvironmentUpdate $invalid 'new-id' 'new-secret' | Out-Null
        throw 'FAIL: missing or duplicate credential key was accepted.'
    } catch { if ($_.Exception.Message -notlike 'Expected exactly one*') { throw } }
}
try {
    Get-SpotifyEnvironmentUpdate $original 'new-id' 'secret with spaces' | Out-Null
    throw 'FAIL: unsafe provider value was accepted.'
} catch { if ($_.Exception.Message -notlike 'Provider credentials must*') { throw } }
$blank = $original.Replace('SPOTIFY_CLIENT_ID=old-id', 'SPOTIFY_CLIENT_ID=').Replace('SPOTIFY_CLIENT_SECRET=old-secret', 'SPOTIFY_CLIENT_SECRET=')
if ((Get-SpotifyEnvironmentUpdate $original '' '' -DeferSpotify) -cne $blank) { throw 'FAIL: explicit deferral must blank only the Spotify pair.' }
if ((Get-SpotifyEnvironmentUpdate $blank 'new-id' 'new-secret') -cne $expected) { throw 'FAIL: credentials cannot be restored after deferral.' }
foreach ($values in @(@('', ''), @('new-id', ''))) {
    try {
        Get-SpotifyEnvironmentUpdate $original $values[0] $values[1] | Out-Null
        throw 'FAIL: empty credentials accepted without explicit deferral.'
    } catch { if ($_.Exception.Message -notlike 'Provider credentials must*') { throw } }
}
try {
    Get-SpotifyEnvironmentUpdate $original 'new-id' '' -DeferSpotify | Out-Null
    throw 'FAIL: partial deferral accepted.'
} catch { if ($_.Exception.Message -notlike 'Deferred Spotify credentials must*') { throw } }
Write-Host 'PASS: targeted updates and explicit deferral preserve unrelated secrets; invalid/partial credentials are rejected; reactivation works. No AWS resources changed.'
