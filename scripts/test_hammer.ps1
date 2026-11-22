#Requires -Version 7.0
# Run both Hammer client phases in a disposable copy of a closed Minecraft world.
[CmdletBinding()]
param(
    [Parameter(Mandatory)] [string]$SourceWorld,
    [ValidateSet('hammer','stair-outline','powers','radial','progression')][string]$Scenario = 'hammer',
    [switch]$Offline,
    [ValidateSet('26.1','26.3')][string]$Target = '26.1',
    [ValidateSet('neoforge','fabric')][string]$Loader = 'neoforge'
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$checkout = Split-Path $PSScriptRoot -Parent
$world = (Resolve-Path -LiteralPath $SourceWorld).Path
$level = Join-Path $world 'level.dat'
if (!(Test-Path -LiteralPath $level -PathType Leaf)) { throw "Missing level.dat in $world" }
$runId = [guid]::NewGuid().ToString('N')
$runRoot = Join-Path $checkout ".tmp/world-playtests/$runId"
if ($runRoot.StartsWith($world + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'The test output cannot be inside the source save.'
}
if ((Get-Item -LiteralPath $world).Attributes.HasFlag([IO.FileAttributes]::ReparsePoint) -or
        (Get-ChildItem -LiteralPath $world -Recurse -Force -Attributes ReparsePoint | Select-Object -First 1)) {
    throw 'Choose a self-contained save without symbolic links or junctions.'
}
$options = @('--console=plain')
if ($Offline) { $options += '--offline' }
$hash = (Get-FileHash -LiteralPath $level).Hash
Write-Host "Source save: $world"
Write-Host "Test worlds, logs and results: $runRoot"

Push-Location -LiteralPath $checkout
try {
    $lock = $null
    try {
        $lockPath = Join-Path $world 'session.lock'
        if (Test-Path -LiteralPath $lockPath) {
            try { $lock = [IO.File]::Open($lockPath, 'Open', 'Read', 'Read') }
            catch [IO.IOException] { throw "Close the source world before copying it: $world" }
        }
        $gameDir = Join-Path $runRoot 'hammer'
        $copy = Join-Path $gameDir 'saves/playtest'
        [IO.Directory]::CreateDirectory($copy) | Out-Null
        & robocopy $world $copy /E /XJ /XF session.lock /R:0 /W:0 /NFL /NDL /NJH /NJS /NP | Out-Null
        if ($LASTEXITCODE -ge 8) { throw "Could not copy $world into $copy (robocopy exit $LASTEXITCODE)." }
        if ((Get-FileHash -LiteralPath (Join-Path $copy 'level.dat')).Hash -ne $hash) {
            throw 'Source save changed during the copy.'
        }
        Set-Content -LiteralPath (Join-Path $gameDir '.florafauna-playtest') -Value $Scenario
        $optionsVersion = if ($Target -eq '26.1') { 4786 } else { 5023 }
        $gameOptions = @("version:$optionsVersion",'pauseOnLostFocus:false','key_key.florafauna.throw_item:key.keyboard.comma','skipMultiplayerWarning:true','onboardAccessibility:false','tutorialStep:none','autoJump:false','renderDistance:4','simulationDistance:5')
        if ($Scenario -eq 'radial') { $gameOptions += 'key_key.florafauna.cycle_mining_mode:key.keyboard.v' }
        $gameOptions | Set-Content -LiteralPath (Join-Path $gameDir 'options.txt')
    } finally { if ($lock) { $lock.Dispose() } }

    $phases = if ($Scenario -in 'hammer', 'progression') { 1, 2 } else { 1 }
    foreach ($phase in $phases) {
        Write-Host "Launching Hammer phase $phase ..."
        $log = Join-Path $gameDir "phase-$phase.log"
        & .\gradlew.bat @options "-PplaytestId=$runId" "-PplaytestPhase=$phase" "-PplaytestScenario=$Scenario" ":$Target-${Loader}:runWorldPlaytest" *> $log
        $resultPath = Join-Path $gameDir "phase-$phase.result"
        $result = if (Test-Path -LiteralPath $resultPath) { (Get-Content -Raw -LiteralPath $resultPath).Trim() } else { 'Missing client result' }
        if ($LASTEXITCODE -ne 0 -or $result -ne 'PASS') { throw "Hammer phase $phase failed: $result. See $log" }
        Write-Host "Hammer phase $phase passed."
    }
} finally { Pop-Location }
