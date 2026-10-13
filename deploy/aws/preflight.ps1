[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^\d{12}$')]
    [string]$ExpectedAccountId,

    [ValidateScript({ $_ -and $_ -ne 'default' })]
    [string]$Profile = 'sentimentscribe-new',

    [ValidatePattern('^[a-z0-9.-]+$')]
    [string]$Domain = 'sentimentscribe.cloud'
)

$ErrorActionPreference = 'Stop'
$aws = (Get-Command aws -ErrorAction Stop).Source

function Invoke-AwsJson {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [string]$Region = 'ca-central-1'
    )

    # Windows PowerShell treats native stderr differently from PowerShell errors.
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = & $aws @Arguments --profile $Profile --region $Region --output json --no-cli-pager 2>&1
        $code = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($code -ne 0) {
        throw "AWS command failed ($($Arguments[0]) $($Arguments[1])): $($output -join [Environment]::NewLine)"
    }
    $parsed = ($output -join [Environment]::NewLine) | ConvertFrom-Json
    # Enumerate arrays explicitly to avoid Windows PowerShell's JSON wrappers.
    if ($parsed -is [array]) {
        foreach ($item in $parsed) { $item }
    } elseif ($null -ne $parsed) {
        $parsed
    }
}

# No resource API calls occur until the identity matches the supplied new account.
$identity = Invoke-AwsJson -Arguments @('sts', 'get-caller-identity')
if ($identity.Account -ne $ExpectedAccountId) {
    throw "Account mismatch: expected $ExpectedAccountId, authenticated as $($identity.Account). No resource inventory was performed."
}
Write-Host "Verified account $($identity.Account), identity $($identity.Arn), profile $Profile."

$inventory = [ordered]@{
    CapturedAt = (Get-Date).ToUniversalTime().ToString('o')
    Profile = $Profile
    Identity = $identity
    ApplicationRegion = 'ca-central-1'
    CertificateRegion = 'us-east-1'
    Domain = $Domain
    Resources = [ordered]@{}
    Errors = [ordered]@{}
}

$checks = @(
    @{ Name = 'Ecr'; Args = @('ecr', 'describe-repositories', '--query', "repositories[?repositoryName=='sentimentscribe-backend'].{Name:repositoryName,Uri:repositoryUri,Arn:repositoryArn}") },
    @{ Name = 'Instances'; Args = @('ec2', 'describe-instances', '--filters', 'Name=tag:Name,Values=sentimentscribe-prod', '--query', 'Reservations[].Instances[].{Id:InstanceId,State:State.Name,Type:InstanceType,ImageId:ImageId,VpcId:VpcId,SubnetId:SubnetId,PublicIp:PublicIpAddress,Role:IamInstanceProfile.Arn,Volumes:BlockDeviceMappings[].Ebs.VolumeId,Tags:Tags}') },
    @{ Name = 'SecurityGroups'; Args = @('ec2', 'describe-security-groups', '--filters', 'Name=group-name,Values=sentimentscribe-web', '--query', 'SecurityGroups[].{Id:GroupId,VpcId:VpcId,Inbound:IpPermissions,Outbound:IpPermissionsEgress,Tags:Tags}') },
    @{ Name = 'ElasticIps'; Args = @('ec2', 'describe-addresses', '--query', 'Addresses[].{AllocationId:AllocationId,AssociationId:AssociationId,Ip:PublicIp,InstanceId:InstanceId,Tags:Tags}') },
    @{ Name = 'DefaultVpc'; Args = @('ec2', 'describe-vpcs', '--filters', 'Name=is-default,Values=true', '--query', 'Vpcs[].{Id:VpcId,Cidr:CidrBlock,State:State}') },
    @{ Name = 'DefaultSubnets'; Args = @('ec2', 'describe-subnets', '--filters', 'Name=default-for-az,Values=true', '--query', 'Subnets[].{Id:SubnetId,VpcId:VpcId,Az:AvailabilityZone,PublicIpOnLaunch:MapPublicIpOnLaunch}') },
    @{ Name = 'RouteTables'; Args = @('ec2', 'describe-route-tables', '--query', 'RouteTables[].{Id:RouteTableId,VpcId:VpcId,Associations:Associations,Routes:Routes}') },
    @{ Name = 'IamRole'; Args = @('iam', 'list-roles', '--query', "Roles[?RoleName=='SentimentScribeEc2Role'].{Name:RoleName,Arn:Arn,Trust:AssumeRolePolicyDocument}") },
    @{ Name = 'Buckets'; Args = @('s3api', 'list-buckets', '--query', "Buckets[?starts_with(Name, 'sentimentscribe')].{Name:Name,Created:CreationDate}") },
    @{ Name = 'CloudFront'; Args = @('cloudfront', 'list-distributions', '--query', "DistributionList.Items[?contains(to_string(Aliases.Items), '$Domain') || contains(Comment, 'sentimentscribe') || contains(to_string(Origins.Items), 'sentimentscribe')].{Id:Id,Domain:DomainName,Status:Status,Enabled:Enabled,Aliases:Aliases,Origins:Origins,Certificate:ViewerCertificate}") },
    @{ Name = 'Certificates'; Region = 'us-east-1'; Args = @('acm', 'list-certificates', '--query', "CertificateSummaryList[?DomainName=='$Domain' || DomainName=='*.$Domain'].{Arn:CertificateArn,Domain:DomainName,Status:Status}") },
    @{ Name = 'HostedZones'; Args = @('route53', 'list-hosted-zones-by-name', '--dns-name', $Domain, '--query', "HostedZones[?Name=='$Domain.'].{Id:Id,Name:Name,Private:Config.PrivateZone}") },
    @{ Name = 'SsmParameterMetadata'; Args = @('ssm', 'describe-parameters', '--parameter-filters', 'Key=Name,Option=BeginsWith,Values=/sentimentscribe/', '--query', 'Parameters[].{Name:Name,Type:Type,Version:Version}') },
    @{ Name = 'ManagedInstances'; Args = @('ssm', 'describe-instance-information', '--query', 'InstanceInformationList[].{Id:InstanceId,Ping:PingStatus,Platform:PlatformName,AgentVersion:AgentVersion}') }
)

foreach ($check in $checks) {
    Write-Host "Inspecting $($check.Name)..."
    try {
        $region = if ($check.Region) { $check.Region } else { 'ca-central-1' }
        $inventory.Resources[$check.Name] = @(Invoke-AwsJson -Arguments $check.Args -Region $region)
    } catch {
        $inventory.Errors[$check.Name] = $_.Exception.Message
        Write-Warning "$($check.Name) could not be inspected. This is not evidence that its resources are absent."
    }
}

$stateDirectory = Join-Path $PSScriptRoot '.state'
New-Item -ItemType Directory -Path $stateDirectory -Force | Out-Null
$path = Join-Path $stateDirectory 'inventory.json'
$inventory | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath $path -Encoding UTF8
Write-Host "Saved read-only inventory to $path. No AWS resources were changed."
if ($inventory.Errors.Count -gt 0) {
    throw "$($inventory.Errors.Count) inventory checks failed. Resolve the recorded errors before provisioning."
}
