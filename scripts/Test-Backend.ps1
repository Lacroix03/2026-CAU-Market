. (Join-Path $PSScriptRoot 'Environment.ps1')
& $nodeExe $npmCli --prefix backend run build
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$env:CI='true'
& $nodeExe $firebaseCli emulators:exec --project demo-cau-market --only auth,firestore,storage,functions 'node --test backend/test/*.test.cjs'
exit $LASTEXITCODE
