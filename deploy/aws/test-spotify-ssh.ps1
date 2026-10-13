$ErrorActionPreference = 'Stop'
$path = Join-Path $PSScriptRoot 'apply-spotify-credentials.ps1'
$source = [System.IO.File]::ReadAllText($path).Replace("`r`n", "`n")
foreach ($newline in @("`n", "`r`n")) {
    $fixture = $source.Replace("`n", $newline)
    $tokens = $null
    $parseErrors = $null
    $ast = [System.Management.Automation.Language.Parser]::ParseInput($fixture, [ref]$tokens, [ref]$parseErrors)
    if ($parseErrors.Count) { throw 'SSH helper fixture failed to parse.' }
    $assignments = @($ast.FindAll({ param($node)
        $node -is [System.Management.Automation.Language.AssignmentStatementAst] -and $node.Left.Extent.Text -eq '$prepare'
    }, $true))
    if (-not $assignments.Count) { throw 'SSH preparation command not found.' }
    $ExpectedAccountId = '920235987728'
    $candidate = '/tmp/nonsecret-ssh-fixture.env'
    foreach ($assignment in $assignments) { Invoke-Expression $assignment.Extent.Text }
    if ($prepare.Contains("`r")) { throw 'Windows checkout adds CR characters to the Linux SSH command.' }
    if (-not $prepare.StartsWith("set -eu`n") -or -not $prepare.Contains("umask 077`n")) {
        throw 'SSH preparation lost its fail-fast or private-file permissions.'
    }
}
Write-Host 'PASS: LF and CRLF checkouts produce LF-only, fail-fast private SSH preparation. No AWS calls or secrets used.'
