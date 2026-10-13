[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidatePattern('^\d{12}$')][string]$ExpectedAccountId,
    [ValidateScript({ $_ -and $_ -ne 'default' })][string]$Profile = 'sentimentscribe-new'
)
. (Join-Path $PSScriptRoot 'common.ps1')
Initialize-Deployment -ExpectedAccountId $ExpectedAccountId -Profile $Profile
$health = Get-Content -Raw (Join-Path $script:DeploymentState 'backend-health.json') | ConvertFrom-Json
if ($health.Account -ne $ExpectedAccountId -or $health.Status -ne 'ok') { throw 'Verify backend HTTPS in this account first.' }
$domain = 'sentimentscribe.cloud'
$region = 'us-east-1'
$certificates = @(Invoke-DeploymentAws -Arguments @('acm', 'list-certificates', '--certificate-statuses', 'ISSUED', 'PENDING_VALIDATION', '--query', "CertificateSummaryList[?DomainName=='$domain']") -Region $region)
if ($certificates.Count -gt 1) { throw 'Multiple matching certificates exist; review before selecting one.' }
if ($certificates.Count -eq 1) {
    $arn = $certificates[0].CertificateArn
} else {
    $created = Invoke-DeploymentAws -Arguments @('acm', 'request-certificate', '--domain-name', $domain, '--validation-method', 'DNS', '--key-algorithm', 'RSA_2048', '--idempotency-token', 'SentimentScribeRehosting', '--tags', 'Key=Application,Value=SentimentScribe') -Region $region
    $arn = $created.CertificateArn
    Save-DeploymentJson 'certificate-request.json' $created | Out-Null
}
if (-not $arn.StartsWith("arn:aws:acm:us-east-1:${ExpectedAccountId}:certificate/")) {
    throw 'Certificate is not in the expected account and us-east-1.'
}
$record = $null
for ($attempt = 0; $attempt -lt 20; $attempt++) {
    $details = Invoke-DeploymentAws -Arguments @('acm', 'describe-certificate', '--certificate-arn', $arn) -Region $region
    $record = @($details.Certificate.DomainValidationOptions | Where-Object { $_.DomainName -eq $domain })[0].ResourceRecord
    if ($record) { break }
    Start-Sleep -Seconds 3
}
if (-not $record) { throw 'ACM has not supplied the validation record yet. Rerun this script; do not request a duplicate certificate.' }
$state = @{
    Account = $ExpectedAccountId
    Region = $region
    Arn = $arn
    Domain = $domain
    Status = $details.Certificate.Status
    Record = @{ Type = $record.Type; Name = $record.Name.TrimEnd('.'); Value = $record.Value.TrimEnd('.'); Host = ($record.Name.TrimEnd('.') -replace '\.sentimentscribe\.cloud$', '') }
}
Save-DeploymentJson 'certificate.json' $state | Out-Null
Write-Host "Certificate $arn; status $($state.Status); region us-east-1."
Write-Host "Porkbun validation record: Type $($state.Record.Type), Host $($state.Record.Host), Answer $($state.Record.Value), TTL 600."
Write-Host 'Keep this CNAME for automatic renewal. It does not point the website to CloudFront.'
