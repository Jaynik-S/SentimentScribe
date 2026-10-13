[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidatePattern('^\d{12}$')][string]$ExpectedAccountId,
    [ValidateScript({ $_ -and $_ -ne 'default' })][string]$Profile = 'sentimentscribe-new'
)
. (Join-Path $PSScriptRoot 'common.ps1')
Initialize-Deployment -ExpectedAccountId $ExpectedAccountId -Profile $Profile
$foundation = Get-FoundationOutputs
$certificate = Get-Content -Raw (Join-Path $script:DeploymentState 'certificate.json') | ConvertFrom-Json
if ($certificate.Account -ne $ExpectedAccountId -or $certificate.Region -ne 'us-east-1' -or -not $certificate.Arn.StartsWith("arn:aws:acm:us-east-1:${ExpectedAccountId}:certificate/")) {
    throw 'Certificate state must match the target account and us-east-1.'
}
$details = Invoke-DeploymentAws -Arguments @('acm', 'describe-certificate', '--certificate-arn', $certificate.Arn) -Region 'us-east-1'
if ($details.Certificate.Status -ne 'ISSUED') { throw "Certificate status is $($details.Certificate.Status). No frontend deployment was attempted." }
$certificate.Status = $details.Certificate.Status
Save-DeploymentJson 'certificate.json' $certificate | Out-Null
$health = Invoke-RestMethod https://api.sentimentscribe.cloud/api/health
if ($health.status -ne 'ok') { throw 'Backend HTTPS health must pass before frontend deployment.' }
$bucket = $foundation.FrontendBucket
Invoke-DeploymentAws @('s3api', 'head-bucket', '--bucket', $bucket, '--expected-bucket-owner', $ExpectedAccountId) | Out-Null
$privacy = Invoke-DeploymentAws @('s3api', 'get-public-access-block', '--bucket', $bucket, '--expected-bucket-owner', $ExpectedAccountId)
if (@($privacy.PublicAccessBlockConfiguration.PSObject.Properties | Where-Object { $_.Value -ne $true }).Count -gt 0) { throw 'Frontend bucket must block all public access.' }
$stackName = 'sentimentscribe-frontend'
$stacks = @(Invoke-DeploymentAws @('cloudformation', 'list-stacks', '--query', "StackSummaries[?StackName=='$stackName' && StackStatus!='DELETE_COMPLETE']"))
if ($stacks.Count -gt 0 -and $stacks[0].StackStatus -notin @('CREATE_COMPLETE', 'UPDATE_COMPLETE')) {
    throw "Existing frontend stack is $($stacks[0].StackStatus). Review it; no automatic recovery or deletion will occur."
}
if ($stacks.Count -eq 0) {
    $distributions = @(Invoke-DeploymentAws @('cloudfront', 'list-distributions', '--query', "DistributionList.Items[?contains(to_string(Origins.Items), '$bucket') || contains(to_string(Aliases.Items), 'sentimentscribe.cloud')]"))
    if ($distributions.Count -gt 0) { throw 'A matching unmanaged distribution exists. Review reuse/import first.' }
    try {
        Invoke-DeploymentAws @('s3api', 'get-bucket-policy', '--bucket', $bucket, '--expected-bucket-owner', $ExpectedAccountId) | Out-Null
        throw 'An existing bucket policy must be reviewed before CloudFormation manages it.'
    } catch {
        if ($_.Exception.Message -notmatch 'NoSuchBucketPolicy') { throw }
    }
}
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$frontend = Join-Path $root 'frontend'
$previousApi = $env:VITE_API_BASE_URL
Push-Location $frontend
try {
    $env:VITE_API_BASE_URL = 'https://api.sentimentscribe.cloud'
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw 'Production frontend build failed.' }
} finally {
    $env:VITE_API_BASE_URL = $previousApi
    Pop-Location
}
$dist = Join-Path $frontend 'dist'
if (-not (Test-Path (Join-Path $dist 'index.html'))) { throw 'Production index.html is missing.' }
foreach ($upload in @(
    @('s3', 'sync', $dist, "s3://$bucket/", '--exclude', 'assets/*', '--exclude', 'index.html', '--cache-control', 'public,max-age=3600'),
    @('s3', 'sync', (Join-Path $dist 'assets'), "s3://$bucket/assets/", '--cache-control', 'public,max-age=31536000,immutable'),
    @('s3', 'cp', (Join-Path $dist 'index.html'), "s3://$bucket/index.html", '--cache-control', 'no-cache,max-age=0,must-revalidate', '--content-type', 'text/html')
)) {
    & $script:DeploymentAws @upload --profile $Profile --region ca-central-1 --no-cli-pager
    if ($LASTEXITCODE -ne 0) { throw 'Frontend upload failed. No old S3 objects were deleted.' }
}
Invoke-DeploymentAws @('s3api', 'head-object', '--bucket', $bucket, '--key', 'index.html', '--expected-bucket-owner', $ExpectedAccountId) | Out-Null
$template = Join-Path $PSScriptRoot 'frontend.yaml'
Invoke-DeploymentAws @('cloudformation', 'validate-template', '--template-body', "file://$template") | Out-Null
$parameters = @(
    @{ ParameterKey = 'FrontendBucketName'; ParameterValue = $bucket },
    @{ ParameterKey = 'CertificateArn'; ParameterValue = $certificate.Arn },
    @{ ParameterKey = 'AttachDomain'; ParameterValue = 'false' }
)
$parameterPath = Save-DeploymentJson 'frontend-parameters.json' $parameters
if ($stacks.Count -eq 0) {
    Invoke-DeploymentAws @('cloudformation', 'create-stack', '--stack-name', $stackName, '--template-body', "file://$template", '--parameters', "file://$parameterPath", '--disable-rollback', '--enable-termination-protection', '--tags', 'Key=Application,Value=SentimentScribe') | Out-Null
    Invoke-DeploymentAws @('cloudformation', 'wait', 'stack-create-complete', '--stack-name', $stackName) | Out-Null
}
$stack = Invoke-DeploymentAws @('cloudformation', 'describe-stacks', '--stack-name', $stackName)
$outputs = @{}
foreach ($item in $stack.Stacks[0].Outputs) { $outputs[$item.OutputKey] = $item.OutputValue }
$state = @{ Account = $ExpectedAccountId; DistributionId = $outputs.DistributionId; Domain = $outputs.DistributionDomain; Bucket = $bucket; CertificateArn = $certificate.Arn; AliasAttached = $false }
Save-DeploymentJson 'frontend.json' $state | Out-Null
$invalidation = Invoke-DeploymentAws @('cloudfront', 'create-invalidation', '--distribution-id', $state.DistributionId, '--paths', '/*')
Invoke-DeploymentAws @('cloudfront', 'wait', 'invalidation-completed', '--distribution-id', $state.DistributionId, '--id', $invalidation.Invalidation.Id) | Out-Null
foreach ($route in @('/', '/register')) {
    $page = Invoke-WebRequest -UseBasicParsing "https://$($state.Domain)$route"
    if ($page.StatusCode -ne 200 -or $page.Content -notmatch '<div id="root">') { throw "CloudFront SPA check failed for $route." }
}
try {
    Invoke-WebRequest -UseBasicParsing "https://$bucket.s3.ca-central-1.amazonaws.com/index.html" | Out-Null
    throw 'Private S3 origin was publicly readable.'
} catch {
    if (-not $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 403) { throw }
}
Write-Host "Verified private S3 and CloudFront SPA routes at https://$($state.Domain)."
$distribution = Invoke-DeploymentAws @('cloudfront', 'get-distribution-config', '--id', $state.DistributionId)
if ($distribution.DistributionConfig.Aliases.Items -notcontains 'sentimentscribe.cloud') {
    $parameters[2].ParameterValue = 'true'
    $attachPath = Save-DeploymentJson 'frontend-domain-parameters.json' $parameters
    $changeName = 'attach-domain-' + (Get-Date).ToUniversalTime().ToString('yyyyMMddHHmmss')
    Invoke-DeploymentAws @('cloudformation', 'create-change-set', '--stack-name', $stackName, '--change-set-name', $changeName, '--change-set-type', 'UPDATE', '--template-body', "file://$template", '--parameters', "file://$attachPath") | Out-Null
    Invoke-DeploymentAws @('cloudformation', 'wait', 'change-set-create-complete', '--stack-name', $stackName, '--change-set-name', $changeName) | Out-Null
    $preview = Invoke-DeploymentAws @('cloudformation', 'describe-change-set', '--stack-name', $stackName, '--change-set-name', $changeName)
    $changes = @($preview.Changes)
    if ($changes.Count -ne 1 -or $changes[0].ResourceChange.LogicalResourceId -ne 'FrontendDistribution' -or $changes[0].ResourceChange.Action -ne 'Modify' -or $changes[0].ResourceChange.Replacement -ne 'False') {
        throw 'Domain change is not a single non-replacing CloudFront modification. Review before executing.'
    }
    Save-DeploymentJson 'frontend-domain-change-set.json' $preview | Out-Null
    Invoke-DeploymentAws @('cloudformation', 'execute-change-set', '--stack-name', $stackName, '--change-set-name', $changeName, '--disable-rollback') | Out-Null
    try {
        Invoke-DeploymentAws @('cloudformation', 'wait', 'stack-update-complete', '--stack-name', $stackName) | Out-Null
    } catch {
        $events = Invoke-DeploymentAws @('cloudformation', 'describe-stack-events', '--stack-name', $stackName, '--query', 'StackEvents[0:10].{Resource:LogicalResourceId,Status:ResourceStatus,Reason:ResourceStatusReason}')
        $events | Format-Table -Wrap
        throw 'Domain attachment did not complete. Do not delete or move any distribution/domain; review the events and obtain approval for any required cross-account alias move.'
    }
}
$actual = Invoke-DeploymentAws @('cloudfront', 'get-distribution', '--id', $state.DistributionId)
if ($actual.Distribution.Status -ne 'Deployed' -or $actual.Distribution.DistributionConfig.Aliases.Items -notcontains 'sentimentscribe.cloud' -or $actual.Distribution.DistributionConfig.ViewerCertificate.ACMCertificateArn -ne $certificate.Arn) {
    throw 'CloudFront domain/certificate verification did not pass.'
}
$state.AliasAttached = $true
Save-DeploymentJson 'frontend.json' $state | Out-Null
Write-Host "CloudFront domain/certificate verified. Porkbun root ALIAS target: $($state.Domain). Keep API A and certificate CNAME unchanged."
