$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$resolved = Get-Content -LiteralPath (Join-Path $repo 'app/build/reports/validation-dependencies.json') -Raw | ConvertFrom-Json
$production = @($resolved.debugRuntimeClasspath)
$testOnly = @($resolved.debugUnitTestRuntimeClasspath | Where-Object { $_ -notin $production })
$all = @($production + $testOnly | Sort-Object -Unique)
$findings = @()
$queryErrors = @()
for ($index = 0; $index -lt $all.Count; $index += 40) {
    $end = [Math]::Min($index + 39, $all.Count - 1)
    $batch = @($all[$index..$end])
    $queries = @($batch | ForEach-Object {
        $parts = $_ -split ':'
        @{ package = @{ ecosystem = 'Maven'; name = "$($parts[0]):$($parts[1])" }; version = $parts[2] }
    })
    try {
        $response = Invoke-RestMethod -Uri 'https://api.osv.dev/v1/querybatch' -Method Post -ContentType 'application/json' -Body (@{ queries = $queries } | ConvertTo-Json -Depth 6 -Compress) -TimeoutSec 45
        for ($offset = 0; $offset -lt $batch.Count; $offset++) {
            $result = $response.results[$offset]
            if ($result.next_page_token) { throw 'Unexpected paginated OSV result; audit is incomplete.' }
            $ids = @($result.vulns | ForEach-Object { $_.id } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
            $findings += [pscustomobject][ordered]@{
                coordinate = $batch[$offset]; scope = $(if ($batch[$offset] -in $production) { 'production' } else { 'test_only' })
                vulnerability_ids = $ids
            }
        }
    } catch { $queryErrors += "Batch at index $index failed: $($_.Exception.Message)" }
}
$report = [ordered]@{
    schema_version = 1; queried_at_utc = [DateTime]::UtcNow.ToString('o')
    database = 'OSV'; endpoint = 'https://api.osv.dev/v1/querybatch'
    scopes = @('debugRuntimeClasspath', 'debugUnitTestRuntimeClasspath minus production coordinates')
    new_direct_test_dependencies = @('com.squareup.okhttp3:mockwebserver:5.1.0', 'org.robolectric:robolectric:4.16.1')
    test_only_security_constraint = 'org.bouncycastle:bcprov-jdk18on:1.85.2'
    remediated_previous_test_coordinate = 'org.bouncycastle:bcprov-jdk18on:1.81'
    remediated_advisory_ids = @('GHSA-574f-3g2m-x479', 'GHSA-9pwp-9qqc-pr26', 'GHSA-c3fc-8qff-9hwx', 'GHSA-qp49-qgx5-5m26')
    production_coordinate_count = $production.Count; test_only_coordinate_count = $testOnly.Count
    queried_coordinate_count = $findings.Count
    production_flagged_coordinate_count = @($findings | Where-Object { $_.scope -eq 'production' -and $_.vulnerability_ids.Count -gt 0 }).Count
    test_only_flagged_coordinate_count = @($findings | Where-Object { $_.scope -eq 'test_only' -and $_.vulnerability_ids.Count -gt 0 }).Count
    query_errors = $queryErrors; findings = $findings
    limitations = @('Known advisory lookup, not a guarantee of absence of vulnerabilities', 'Debug runtime artifacts; build plugins, Gradle, Android SDK, dynamically fetched Robolectric SDK image and release-only graph not audited', 'Advisory IDs may overlap by alias; do not interpret IDs as distinct CVEs', 'Production dependency versions unchanged; only test dependencies added', 'Metadata calls to OSV only; no application providers contacted')
}
$report | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'dependency-audit.json') -Encoding utf8
$auditText = "<!-- dependency-audit:start -->`nOSV lookup: $($report.production_coordinate_count) production coordinates and $($report.test_only_coordinate_count) test-only coordinates; queried $($report.queried_coordinate_count). Flagged production coordinates: $($report.production_flagged_coordinate_count); flagged test-only coordinates: $($report.test_only_flagged_coordinate_count); query errors: $($queryErrors.Count). Exact coordinates and advisory IDs are in [dependency-audit.json](tests/dependency-audit.json).`n<!-- dependency-audit:end -->"
$flaggedLines = @($findings | Where-Object { $_.vulnerability_ids.Count -gt 0 } | ForEach-Object {
    $links = @($_.vulnerability_ids | ForEach-Object { "[$_](https://osv.dev/vulnerability/$_)" }) -join ', '
    "Flagged artifact: $($_.coordinate) ($($_.scope)): $links."
})
if ($flaggedLines.Count) {
    $auditText = $auditText.Replace('<!-- dependency-audit:end -->', "`n$($flaggedLines -join "`n")`nAny remaining findings are reported; production dependency versions are unchanged.`n<!-- dependency-audit:end -->")
}
$docPath = Join-Path $repo 'TESTING.md'
$doc = Get-Content -LiteralPath $docPath -Raw
$doc = [regex]::Replace($doc, '(?s)<!-- dependency-audit:start -->.*?<!-- dependency-audit:end -->', { param($match) $auditText })
Set-Content -LiteralPath $docPath -Value $doc -Encoding utf8
Write-Output "OSV: production=$($report.production_coordinate_count), test-only=$($report.test_only_coordinate_count), queried=$($report.queried_coordinate_count), flagged-production=$($report.production_flagged_coordinate_count), flagged-test-only=$($report.test_only_flagged_coordinate_count), errors=$($queryErrors.Count)"
if ($queryErrors.Count) { throw 'Dependency audit incomplete; see dependency-audit.json.' }
