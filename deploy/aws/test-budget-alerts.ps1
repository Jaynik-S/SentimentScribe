$ErrorActionPreference = 'Stop'
$path = Join-Path $PSScriptRoot 'configure-budget-alerts.ps1'
if (-not (Test-Path $path)) { throw 'FAIL: budget alert request builder is not implemented.' }
$errors = $null
$ast = [System.Management.Automation.Language.Parser]::ParseFile($path, [ref]$null, [ref]$errors)
if ($errors) { throw 'Budget helper syntax failed.' }
$definition = $ast.Find({ param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Get-BudgetAlertRequest' }, $true)
if (-not $definition) { throw 'FAIL: budget alert request builder is not implemented.' }
Invoke-Expression $definition.Extent.Text
foreach ($threshold in @(1, 2, 3)) {
    $request = Get-BudgetAlertRequest -AccountId '920235987728' -Email 'alerts@example.com' -Threshold $threshold
    $roundTrip = $request | ConvertTo-Json -Depth 8 | ConvertFrom-Json
    if ($roundTrip.AccountId -ne '920235987728' -or $roundTrip.BudgetName -ne 'sentimentscribe-monthly') { throw 'FAIL: wrong budget/account target.' }
    if ($roundTrip.Notification.NotificationType -ne 'ACTUAL' -or $roundTrip.Notification.ComparisonOperator -ne 'GREATER_THAN' -or $roundTrip.Notification.ThresholdType -ne 'ABSOLUTE_VALUE' -or $roundTrip.Notification.Threshold -ne $threshold) { throw 'FAIL: alert must compare actual dollars, not forecast percentages.' }
    if (@($roundTrip.Subscribers).Count -ne 1 -or $roundTrip.Subscribers[0].SubscriptionType -ne 'EMAIL' -or $roundTrip.Subscribers[0].Address -ne 'alerts@example.com') { throw 'FAIL: wrong email recipient or subscriber shape.' }
}
foreach ($email in @('', 'not-an-email', 'Name <alerts@example.com>', "alerts@example.com`nother@example.com")) {
    $rejected = $false
    try { Get-BudgetAlertRequest -AccountId '920235987728' -Email $email -Threshold 1 | Out-Null } catch { $rejected = $true }
    if (-not $rejected) { throw 'FAIL: malformed recipient accepted.' }
}
foreach ($threshold in @(0, 4)) {
    $rejected = $false
    try { Get-BudgetAlertRequest -AccountId '920235987728' -Email 'alerts@example.com' -Threshold $threshold | Out-Null } catch { $rejected = $true }
    if (-not $rejected) { throw 'FAIL: unexpected alert threshold accepted.' }
}
Write-Host 'PASS: $1/$2/$3 actual-dollar email requests serialize correctly; malformed recipients/thresholds rejected. No AWS calls or personal data used.'
