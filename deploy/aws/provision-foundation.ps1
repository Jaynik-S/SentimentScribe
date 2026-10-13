[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidatePattern('^\d{12}$')][string]$ExpectedAccountId,
    [ValidateScript({ $_ -and $_ -ne 'default' })][string]$Profile = 'sentimentscribe-new'
)
. (Join-Path $PSScriptRoot 'common.ps1')
Initialize-Deployment -ExpectedAccountId $ExpectedAccountId -Profile $Profile

$stacks = Invoke-DeploymentAws @('cloudformation', 'list-stacks', '--query', "StackSummaries[?StackName=='sentimentscribe-foundation' && StackStatus!='DELETE_COMPLETE']")
if ($stacks) {
    $outputs = Get-FoundationOutputs
    Save-DeploymentJson 'foundation.json' $outputs | Out-Null
    Write-Host 'Existing completed foundation found. No update or replacement was attempted.'
    exit
}

$inventoryPath = Join-Path $script:DeploymentState 'inventory.json'
$inventory = Get-Content -Raw -LiteralPath $inventoryPath | ConvertFrom-Json
if ($inventory.Identity.Account -ne $ExpectedAccountId -or @($inventory.Errors.PSObject.Properties).Count -gt 0) {
    throw 'Run a successful read-only preflight for this account first.'
}
foreach ($name in @('Ecr', 'Instances', 'SecurityGroups', 'IamRole', 'Buckets', 'CloudFront')) {
    if (@($inventory.Resources.$name).Count -gt 0) {
        throw "$name already exists. Review reuse/import before creating a new stack."
    }
}
$vpc = @($inventory.Resources.DefaultVpc)
if ($vpc.Count -ne 1) { throw 'Expected one verified default VPC.' }
$subnet = @($inventory.Resources.DefaultSubnets | Where-Object { $_.VpcId -eq $vpc[0].Id -and $_.PublicIpOnLaunch } | Sort-Object Az)[0]
if (-not $subnet) { throw 'No public default subnet found.' }
$routes = @($inventory.Resources.RouteTables | Where-Object { $_.VpcId -eq $vpc[0].Id })
if (-not ($routes.Routes | Where-Object { $_.DestinationCidrBlock -eq '0.0.0.0/0' -and $_.GatewayId -like 'igw-*' -and $_.State -eq 'active' })) {
    throw 'No active internet gateway route found. Review networking before launch.'
}
$publicIp = (Invoke-RestMethod 'https://checkip.amazonaws.com').Trim()
$ip = $null
if (-not [System.Net.IPAddress]::TryParse($publicIp, [ref]$ip) -or $ip.AddressFamily -ne [System.Net.Sockets.AddressFamily]::InterNetwork) {
    throw 'Could not establish this computer public IPv4 address.'
}
$ami = Invoke-DeploymentAws @('ssm', 'get-parameter', '--name', '/aws/service/canonical/ubuntu/server/24.04/stable/current/amd64/hvm/ebs-gp3/ami-id')
$image = Invoke-DeploymentAws @('ec2', 'describe-images', '--image-ids', $ami.Parameter.Value)
if ($image.Images[0].OwnerId -ne '099720109477' -or $image.Images[0].Architecture -ne 'x86_64' -or $image.Images[0].RootDeviceName -ne '/dev/sda1') {
    throw 'Resolved AMI is not the expected official Ubuntu x86-64 root layout.'
}
$template = Join-Path $PSScriptRoot 'foundation.yaml'
Invoke-DeploymentAws @('cloudformation', 'validate-template', '--template-body', "file://$template") | Out-Null

$keyName = 'sentimentscribe-prod'
$keyPath = Join-Path $script:DeploymentState 'sentimentscribe-prod.pem'
$keys = Invoke-DeploymentAws @('ec2', 'describe-key-pairs', '--filters', "Name=key-name,Values=$keyName")
if ($keys.KeyPairs.Count -eq 0) {
    if (Test-Path -LiteralPath $keyPath) { throw 'Local key already exists but AWS key is absent. Review before proceeding.' }
    $key = Invoke-DeploymentAws @('ec2', 'create-key-pair', '--key-name', $keyName, '--key-type', 'rsa', '--key-format', 'pem', '--tag-specifications', 'ResourceType=key-pair,Tags=[{Key=Application,Value=SentimentScribe}]')
    [System.IO.File]::WriteAllText($keyPath, $key.KeyMaterial, (New-Object System.Text.UTF8Encoding($false)))
    $key = $null
} elseif (-not (Test-Path -LiteralPath $keyPath)) {
    throw 'AWS key exists but its private key is missing locally. Do not replace it without approval.'
}
$acl = New-Object System.Security.AccessControl.FileSecurity
$acl.SetAccessRuleProtection($true, $false)
$rule = New-Object System.Security.AccessControl.FileSystemAccessRule([System.Security.Principal.WindowsIdentity]::GetCurrent().Name, 'FullControl', 'Allow')
$acl.AddAccessRule($rule)
Set-Acl -LiteralPath $keyPath -AclObject $acl

$parameters = @(
    @{ ParameterKey = 'VpcId'; ParameterValue = $vpc[0].Id },
    @{ ParameterKey = 'SubnetId'; ParameterValue = $subnet.Id },
    @{ ParameterKey = 'ImageId'; ParameterValue = $ami.Parameter.Value },
    @{ ParameterKey = 'KeyName'; ParameterValue = $keyName },
    @{ ParameterKey = 'SshCidr'; ParameterValue = "$publicIp/32" }
)
$parameterPath = Save-DeploymentJson 'foundation-parameters.json' $parameters
Write-Host "Creating t3.medium, encrypted 30 GiB gp3, Elastic IP, ECR, IAM, and private S3 in ca-central-1; SSH $publicIp/32."
$created = Invoke-DeploymentAws @('cloudformation', 'create-stack', '--stack-name', 'sentimentscribe-foundation', '--template-body', "file://$template", '--parameters', "file://$parameterPath", '--capabilities', 'CAPABILITY_NAMED_IAM', '--disable-rollback', '--enable-termination-protection', '--tags', 'Key=Application,Value=SentimentScribe')
Save-DeploymentJson 'foundation-stack.json' $created | Out-Null
Invoke-DeploymentAws @('cloudformation', 'wait', 'stack-create-complete', '--stack-name', 'sentimentscribe-foundation') | Out-Null
$outputs = Get-FoundationOutputs
Save-DeploymentJson 'foundation.json' $outputs | Out-Null
Invoke-DeploymentAws @('ec2', 'wait', 'instance-status-ok', '--instance-ids', $outputs.InstanceId) | Out-Null
Write-Host "Foundation verified. Instance $($outputs.InstanceId), Elastic IP $($outputs.ElasticIp), bucket $($outputs.FrontendBucket)."
