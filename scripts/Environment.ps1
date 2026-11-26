$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Set-Location -LiteralPath $projectRoot
$nodeCommand=Get-Command node -ErrorAction SilentlyContinue
if ($nodeCommand) { $nodeExe=$nodeCommand.Source }
elseif (Test-Path -LiteralPath (Join-Path $projectRoot '.tools\node-path.txt')) { $nodeExe=[IO.File]::ReadAllText((Join-Path $projectRoot '.tools\node-path.txt')).Trim() }
else { throw 'Node 24 is required. Run from the configured development environment.' }
if (-not (Test-Path -LiteralPath $nodeExe)) { throw 'Configured Node executable is missing.' }
$javaDir='C:\Program Files\Android\Android Studio\jbr'
if (-not (Test-Path -LiteralPath (Join-Path $javaDir 'bin\java.exe'))) { throw 'Android Studio JDK is required.' }
$env:JAVA_HOME=$javaDir
$env:PATH=(Split-Path -Parent $nodeExe)+';'+(Join-Path $javaDir 'bin')+';'+$env:PATH
$env:FIREBASE_CLI_DISABLE_TELEMETRY='1'
$npmCli=Join-Path $projectRoot '.tools\package\bin\npm-cli.js'
if (-not (Test-Path -LiteralPath $npmCli)) {
 $npmCommand=Get-Command npm -ErrorAction SilentlyContinue
 if ($npmCommand) { $npmCli=Join-Path (Split-Path -Parent $npmCommand.Source) 'node_modules\npm\bin\npm-cli.js' }
}
if (-not (Test-Path -LiteralPath $npmCli)) { throw 'Install Node 24 with npm, or restore the local .tools npm installation.' }
$firebaseCli=Join-Path $projectRoot 'backend\node_modules\firebase-tools\lib\bin\firebase.js'
if (-not (Test-Path -LiteralPath $firebaseCli)) { throw 'Backend dependencies must be installed with npm ci first.' }
