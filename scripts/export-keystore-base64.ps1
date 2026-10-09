param (
    [Parameter(Mandatory=$true)]
    [string]$KeystorePath
)

if (-not (Test-Path $KeystorePath)) {
    Write-Error "File nahi mili: $KeystorePath"
    exit 1
}

$base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($KeystorePath))
Set-Clipboard -Value $base64

Write-Host "==========================================================" -ForegroundColor Green
Write-Host "Success! Keystore file Base64 me encode ho chuki hai." -ForegroundColor Green
Write-Host "Aur ye automatic aapke CLIPBOARD par COPY ho gayi hai!" -ForegroundColor Yellow
Write-Host "Ab GitHub Repo -> Settings -> Secrets -> Actions me jayein" -ForegroundColor Cyan
Write-Host "Aur 'KEYSTORE_BASE64' secret create karke Paste kar dein." -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Green
