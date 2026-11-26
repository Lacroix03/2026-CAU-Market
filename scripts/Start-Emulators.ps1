param([switch]$Lan)
. (Join-Path $PSScriptRoot 'Environment.ps1')
& $nodeExe $npmCli --prefix backend run build
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$configFile='firebase.json'
if ($Lan) {
 $config=Get-Content -Raw -LiteralPath 'firebase.json' | ConvertFrom-Json
 foreach ($service in @('auth','firestore','storage','functions','ui')) { $config.emulators.$service.host='0.0.0.0' }
 $configFile='.emulator-config.lan.json'
 [IO.File]::WriteAllText((Join-Path $projectRoot $configFile),($config | ConvertTo-Json -Depth 20),(New-Object System.Text.UTF8Encoding($false)))
}
$arguments=@($firebaseCli,'emulators:exec','--project','demo-cau-market','--config',$configFile,'--only','auth,firestore,storage,functions','--export-on-exit','.emulator-data')
if (Test-Path -LiteralPath '.emulator-data\firebase-export-metadata.json') { $arguments += @('--import','.emulator-data') }
$arguments += 'node backend/lib/demo.js'
& $nodeExe @arguments
exit $LASTEXITCODE
