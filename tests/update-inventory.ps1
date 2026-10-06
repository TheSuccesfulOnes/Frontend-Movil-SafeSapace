param([string]$Command = 'gradlew -p M:\ :app:testDebugUnitTest :app:assembleDebug :app:lintDebug', [int]$ExitCode = 0)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$manifest = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'validation-owners.json') -Raw | ConvertFrom-Json
$reports = Join-Path $repo 'app/build/test-results/testDebugUnitTest'
$suiteResults = @{}
foreach ($file in Get-ChildItem -LiteralPath $reports -Filter 'TEST-*.xml') {
    [xml]$result = Get-Content -LiteralPath $file.FullName -Raw
    $suite = $result.testsuite
    $cases = @($suite.testcase | ForEach-Object {
        $name = [string]$_.name
        $kind = if ($name -match '\[(unit|integration):') { $Matches[1] } else { 'unit' }
        $status = if ($_.failure) { 'failed' } elseif ($_.error) { 'error' } elseif ($_.skipped) { 'skipped' } else { 'passed' }
        [pscustomobject][ordered]@{ name = $name; type = $kind; status = $status }
    })
    $suiteResults[[string]$suite.name] = [pscustomobject][ordered]@{
        suite = [string]$suite.name; actual_case_count = $cases.Count
        unit_case_count = @($cases | Where-Object type -eq 'unit').Count
        integration_case_count = @($cases | Where-Object type -eq 'integration').Count
        failures = [int]$suite.failures; errors = [int]$suite.errors; skipped = [int]$suite.skipped
        cases = $cases
    }
}
$owners = @($manifest.owners | ForEach-Object {
    $owner = $_
    $suites = @($owner.suites | ForEach-Object {
        if (-not $suiteResults.ContainsKey($_.name)) { throw "Missing executed suite: $($_.name)" }
        $suiteResults[$_.name]
    })
    [pscustomobject][ordered]@{
        source = $owner.source; scope = $owner.scope
        actual_case_count = [int](($suites | Measure-Object actual_case_count -Sum).Sum)
        unit_case_count = [int](($suites | Measure-Object unit_case_count -Sum).Sum)
        integration_case_count = [int](($suites | Measure-Object integration_case_count -Sum).Sum)
        failures = [int](($suites | Measure-Object failures -Sum).Sum)
        errors = [int](($suites | Measure-Object errors -Sum).Sum)
        skipped = [int](($suites | Measure-Object skipped -Sum).Sum)
        suites = $suites
    }
})
$knownSources = @($manifest.owners.source) + @($manifest.excluded_candidates.source)
$scan = @(Get-ChildItem -LiteralPath (Join-Path $repo 'app/src/main/java') -Recurse -Filter '*.kt' | ForEach-Object { [IO.Path]::GetRelativePath($repo, $_.FullName).Replace('\', '/') })
$unclassified = @($scan | Where-Object { $_ -notin $knownSources })
$orphaned = @($knownSources | Where-Object { $_ -notin $scan })
$attributed = @($manifest.owners.suites.name)
$additionalSuites = @($suiteResults.Values | Where-Object { $_.suite -notin $attributed })
$output = [ordered]@{
    schema_version = 1; generated_at_utc = [DateTime]::UtcNow.ToString('o')
    command = $Command; command_exit_code = $ExitCode; source_files_scanned = $scan.Count
    minimum_cases_per_owner = $manifest.minimum_cases_per_owner; owner_count = $owners.Count
    validation_case_count = [int](($owners | Measure-Object actual_case_count -Sum).Sum)
    total_executed_case_count = [int](($suiteResults.Values | Measure-Object actual_case_count -Sum).Sum)
    failures = [int](($suiteResults.Values | Measure-Object failures -Sum).Sum)
    errors = [int](($suiteResults.Values | Measure-Object errors -Sum).Sum)
    skipped = [int](($suiteResults.Values | Measure-Object skipped -Sum).Sum)
    owners = $owners; additional_suites = $additionalSuites
    excluded_candidates = @($manifest.excluded_candidates); unclassified_sources = $unclassified; orphaned_sources = $orphaned
    limitations = @('No instrumented coverage percentages', 'No Compose UI interaction/device/emulator E2E', 'No Android Keystore encryption or system photo picker', 'No real backend/Firebase/Gemini/Render calls', 'No release build/security assessment implied', 'In-memory SharedPreferences do not model disk durability/threading/Keystore', 'Robolectric SDK 35 only for Android email pattern', 'UI animation/feedback timers are not exercised')
}
$output | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'validation-inventory.json') -Encoding utf8
$lines = @('<!-- validation-results:start -->', '', "Executed: $($output.total_executed_case_count) cases; validation cases: $($output.validation_case_count); owners: $($output.owner_count); failures: $($output.failures); errors: $($output.errors); skipped: $($output.skipped). Command exit code: $ExitCode.", '', '| Source owner | Executed cases | Unit | Local integration | Suite(s) |', '| --- | ---: | ---: | ---: | --- |')
foreach ($owner in $owners) {
    $source = $owner.source.Replace('app/src/main/java/com/experimentos/mobile/', '')
    $suiteNames = ($owner.suites.suite | ForEach-Object { $_.Replace('com.experimentos.mobile.validation.', '').Replace('com.experimentos.mobile.authentication.domain.', '') }) -join ', '
    $lines += "| $source | $($owner.actual_case_count) | $($owner.unit_case_count) | $($owner.integration_case_count) | $suiteNames |"
}
$lines += @('', 'All individual executed case names and their outcomes/types are saved in [validation-inventory.json](tests/validation-inventory.json). Counts are JUnit testcase nodes, never assertions or iterations within a test method.', '', 'Excluded scan candidates:', '', '| Candidate | Reason |', '| --- | --- |')
foreach ($candidate in $manifest.excluded_candidates) { $lines += "| $($candidate.source.Replace('app/src/main/java/com/experimentos/mobile/', '')) | $($candidate.reason) |" }
$lines += @('', "Unclassified source files: $($unclassified.Count). Orphaned manifest paths: $($orphaned.Count).", '', '<!-- validation-results:end -->')
$docPath = Join-Path $repo 'TESTING.md'
$doc = Get-Content -LiteralPath $docPath -Raw
$doc = [regex]::Replace($doc, '(?s)<!-- validation-results:start -->.*?<!-- validation-results:end -->', { param($match) $lines -join "`n" })
Set-Content -LiteralPath $docPath -Value ($doc.TrimEnd() + "`n") -Encoding utf8 -NoNewline
Write-Output "Owners=$($output.owner_count); executed=$($output.total_executed_case_count); validation=$($output.validation_case_count); failures=$($output.failures); errors=$($output.errors); skipped=$($output.skipped); unclassified=$($unclassified.Count)"
if ($unclassified.Count -or $orphaned.Count -or $ExitCode -ne 0 -or $output.failures -or $output.errors -or $output.skipped -or @($owners | Where-Object { $_.actual_case_count -lt $manifest.minimum_cases_per_owner }).Count) { throw 'Validation gate failed; see saved inventory and Gradle reports.' }
