. (Join-Path $PSScriptRoot 'Environment.ps1')
$env:FIRESTORE_EMULATOR_HOST='127.0.0.1:8080'
$env:FIREBASE_AUTH_EMULATOR_HOST='127.0.0.1:9099'
$env:FIREBASE_STORAGE_EMULATOR_HOST='127.0.0.1:9199'
& $nodeExe backend/lib/maintenance.js
exit $LASTEXITCODE
