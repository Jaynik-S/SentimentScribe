[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidatePattern('^\d{12}$')][string]$ExpectedAccountId,
    [Parameter(Mandatory = $true)][string]$Email,
    [ValidateScript({ $_ -and $_ -ne 'default' })][string]$Profile = 'sentimentscribe-new'
)
. (Join-Path $PSScriptRoot 'common.ps1')

function Get-BudgetAlertRequest {
    param(
        [ValidatePattern('^\d{12}$')][string]$AccountId,
        [string]$Email,
        [ValidateRange(1, 3)][int]$Threshold
    )
    if (-not $Email -or $Email -match '\s') { throw 'Provide one plain email address without whitespace.' }
    try { $address = New-Object System.Net.Mail.MailAddress($Email) }
    catch { throw 'Provide a valid email address.' }
    if ($address.Address -cne $Email) { throw 'Provide a plain email address without a display name.' }
    @{
        AccountId = $AccountId
        BudgetName = 'sentimentscribe-monthly'
        Notification = @{
            NotificationType = 'ACTUAL'
            ComparisonOperator = 'GREATER_THAN'
            Threshold = $Threshold
            ThresholdType = 'ABSOLUTE_VALUE'
        }
        Subscribers = @(@{ SubscriptionType = 'EMAIL'; Address = $Email })
    }
}

$requests = @(1, 2, 3 | ForEach-Object { Get-BudgetAlertRequest -AccountId $ExpectedAccountId -Email $Email -Threshold $_ })
Initialize-Deployment -ExpectedAccountId $ExpectedAccountId -Profile $Profile
$name = 'sentimentscribe-monthly'
$budget = Invoke-DeploymentAws -Arguments @('budgets', 'describe-budget', '--account-id', $ExpectedAccountId, '--budget-name', $name) -Region us-east-1
if ($budget.Budget.BudgetLimit.Unit -ne 'USD' -or [decimal]$budget.Budget.BudgetLimit.Amount -ne 3 -or $budget.Budget.TimeUnit -ne 'MONTHLY' -or $budget.Budget.BudgetType -ne 'COST') {
    throw 'The expected $3 USD monthly cost budget was not verified. No notifications changed.'
}
$tags = Invoke-DeploymentAws -Arguments @('budgets', 'list-tags-for-resource', '--resource-arn', "arn:aws:budgets::${ExpectedAccountId}:budget/$name") -Region us-east-1
if (-not ($tags.ResourceTags | Where-Object { $_.Key -eq 'Application' -and $_.Value -eq 'SentimentScribe' }) -or -not ($tags.ResourceTags | Where-Object { $_.Key -eq 'ManagedBy' -and $_.Value -eq 'CodexRehosting' })) {
    throw 'Budget ownership tags did not match. No notifications changed.'
}
$existing = Invoke-DeploymentAws -Arguments @('budgets', 'describe-notifications-for-budget', '--account-id', $ExpectedAccountId, '--budget-name', $name) -Region us-east-1
foreach ($request in $requests) {
    $threshold = $request.Notification.Threshold
    $notification = @($existing.Notifications | Where-Object {
        $_.NotificationType -eq 'ACTUAL' -and $_.ComparisonOperator -eq 'GREATER_THAN' -and $_.ThresholdType -eq 'ABSOLUTE_VALUE' -and $_.Threshold -eq $threshold
    })
    if ($notification.Count -gt 1) { throw 'Duplicate target alert found; review before changes.' }
    $notificationPath = Save-DeploymentJson "budget-notification-$threshold.json" $request.Notification
    if ($notification.Count -eq 0) {
        $requestPath = Save-DeploymentJson "budget-alert-$threshold.json" $request
        Invoke-DeploymentAws -Arguments @('budgets', 'create-notification', '--cli-input-json', "file://$requestPath") -Region us-east-1 | Out-Null
    }
    $subscribers = Invoke-DeploymentAws -Arguments @('budgets', 'describe-subscribers-for-notification', '--account-id', $ExpectedAccountId, '--budget-name', $name, '--notification', "file://$notificationPath") -Region us-east-1
    if (@($subscribers.Subscribers).Count -ne 1 -or $subscribers.Subscribers[0].SubscriptionType -ne 'EMAIL' -or $subscribers.Subscribers[0].Address -cne $Email) {
        throw 'Alert recipient verification failed. No existing recipient was overwritten; review before further changes.'
    }
    Write-Host "Verified actual-spend email alert above USD $threshold."
}
$verified = Invoke-DeploymentAws -Arguments @('budgets', 'describe-notifications-for-budget', '--account-id', $ExpectedAccountId, '--budget-name', $name) -Region us-east-1
foreach ($threshold in @(1, 2, 3)) {
    $matches = @($verified.Notifications | Where-Object { $_.NotificationType -eq 'ACTUAL' -and $_.ComparisonOperator -eq 'GREATER_THAN' -and $_.ThresholdType -eq 'ABSOLUTE_VALUE' -and $_.Threshold -eq $threshold })
    if ($matches.Count -ne 1) { throw 'Final notification threshold verification failed.' }
}
Save-DeploymentJson 'budget-alerts-result.json' @{ Account = $ExpectedAccountId; Budget = $name; Email = $Email; ThresholdsUSD = @(1, 2, 3); VerifiedAt = (Get-Date).ToUniversalTime().ToString('o') } | Out-Null
Write-Host 'All three monthly actual-dollar alerts and recipients verified. Email delivery is not yet tested. No hard cap, stop action, or resource modification was configured.'
