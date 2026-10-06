param([string]$Drive = 'M:', [switch]$RefreshAudit)
$ErrorActionPreference = 'Stop'
if (-not ('ValidationDriveMapping' -as [type])) {
    Add-Type -TypeDefinition @'
using System.Runtime.InteropServices;
using System.Text;
public static class ValidationDriveMapping {
    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    public static extern uint QueryDosDevice(string name, StringBuilder target, uint capacity);
}
'@
}
function Get-SubstTarget([string]$Name) {
    $buffer = [Text.StringBuilder]::new(32768)
    if ([ValidationDriveMapping]::QueryDosDevice($Name, $buffer, 32768) -eq 0) { return $null }
    $target = $buffer.ToString()
    if ($target.StartsWith('\??\', [StringComparison]::Ordinal)) { return $target.Substring(4) }
    return $null
}
$repo = Split-Path -Parent $PSScriptRoot
$repoDrive = Split-Path -Qualifier $repo
$repoMapping = Get-SubstTarget $repoDrive
if ($repoMapping) {
    $physicalRoot = $repoMapping
    $repo = if ($repo.Length -gt 3) { Join-Path $physicalRoot $repo.Substring(3) } else { $physicalRoot }
}
if ($Drive -notmatch '^[A-Za-z]:$') { throw 'Drive must be a single drive letter followed by colon.' }
$existing = Get-SubstTarget $Drive
if ($existing) {
    $mapped = $existing
    if (-not $mapped.Equals($repo, [StringComparison]::OrdinalIgnoreCase)) { throw "$Drive is already mapped to another project; choose an unused drive." }
} else {
    if (Test-Path -LiteralPath "$Drive\") { throw "$Drive is occupied; choose an unused drive." }
    & subst $Drive $repo
    if ($LASTEXITCODE) { throw 'Failed to map test drive.' }
}
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$tasks = @(':app:testDebugUnitTest', ':app:assembleDebug', ':app:lintDebug', ':app:validationDependencyInventory')
$command = "gradlew -p $Drive\ $($tasks -join ' ') --console=plain"
Push-Location "$Drive\"
try {
    & "$Drive\gradlew.bat" -p "$Drive\" @tasks --console=plain
    $code = $LASTEXITCODE
} finally { Pop-Location }
& (Join-Path $PSScriptRoot 'update-inventory.ps1') -Command $command -ExitCode $code
if ($RefreshAudit) { & (Join-Path $PSScriptRoot 'audit-dependencies.ps1') }
Write-Output "Mapped drive retained for independent reruns: $Drive => $repo"
