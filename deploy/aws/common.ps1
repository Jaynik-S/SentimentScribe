$ErrorActionPreference = 'Stop'
$script:DeploymentAws = (Get-Command aws -ErrorAction Stop).Source
$script:DeploymentState = Join-Path $PSScriptRoot '.state'

function Invoke-DeploymentAws {
    param([string[]]$Arguments, [string]$Region = 'ca-central-1')
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $result = & $script:DeploymentAws @Arguments --profile $script:DeploymentProfile --region $Region --output json --no-cli-pager 2>&1
        $code = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($code -ne 0) {
        throw "AWS $($Arguments[0]) $($Arguments[1]) failed: $($result -join [Environment]::NewLine)"
    }
    if (-not $result) { return }
    $parsed = ($result -join [Environment]::NewLine) | ConvertFrom-Json
    if ($parsed -is [array]) {
        foreach ($item in $parsed) { $item }
    } elseif ($null -ne $parsed) {
        $parsed
    }
}

function Initialize-Deployment {
    param(
        [ValidatePattern('^\d{12}$')][string]$ExpectedAccountId,
        [ValidateScript({ $_ -and $_ -ne 'default' })][string]$Profile
    )
    $script:DeploymentProfile = $Profile
    $identity = Invoke-DeploymentAws @('sts', 'get-caller-identity')
    if ($identity.Account -ne $ExpectedAccountId) {
        throw "Account mismatch: expected $ExpectedAccountId, got $($identity.Account). No resources were changed."
    }
    $script:DeploymentAccount = $ExpectedAccountId
    New-Item -ItemType Directory -Path $script:DeploymentState -Force | Out-Null
    Write-Host "Verified account $ExpectedAccountId with $Profile ($($identity.Arn))."
}

function Save-DeploymentJson {
    param([string]$Name, $Value)
    $path = Join-Path $script:DeploymentState $Name
    [System.IO.File]::WriteAllText($path, ($Value | ConvertTo-Json -Depth 40), (New-Object System.Text.UTF8Encoding($false)))
    $path
}

function Get-FoundationOutputs {
    $stack = Invoke-DeploymentAws @('cloudformation', 'describe-stacks', '--stack-name', 'sentimentscribe-foundation')
    if ($stack.Stacks[0].StackStatus -ne 'CREATE_COMPLETE') {
        throw "Foundation is not ready: $($stack.Stacks[0].StackStatus). No update will be attempted."
    }
    $outputs = @{}
    foreach ($item in $stack.Stacks[0].Outputs) { $outputs[$item.OutputKey] = $item.OutputValue }
    $outputs
}
