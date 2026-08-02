# make-env.ps1
#
# Firebase servis hesabı JSON'unu okur, Supabase secrets için .env üretir.
# Private key'i elle kopyalamana gerek kalmaz — kaçış karakterleri
# otomatik doğru ayarlanır.
#
# KULLANIM:
#   .\make-env.ps1 -JsonPath "C:\Users\zeinx\Downloads\yeni-anahtar.json"
#
# Sonra:
#   supabase secrets set --env-file .env

param(
    [Parameter(Mandatory = $true)]
    [string]$JsonPath
)

if (-not (Test-Path $JsonPath)) {
    Write-Error "Dosya bulunamadi: $JsonPath"
    exit 1
}

$sa = Get-Content $JsonPath -Raw | ConvertFrom-Json

if (-not $sa.private_key) {
    Write-Error "JSON icinde private_key yok. Dogru dosya mi?"
    exit 1
}

# Gercek satir sonlarini \n kacisina cevir — .env tek satir olmali
$escapedKey = $sa.private_key -replace "`r`n", '\n' -replace "`n", '\n'

$content = @"
FCM_PROJECT_ID=$($sa.project_id)
FCM_CLIENT_EMAIL=$($sa.client_email)
FCM_PRIVATE_KEY="$escapedKey"
"@

Set-Content -Path ".env" -Value $content -Encoding UTF8 -NoNewline

Write-Host ""
Write-Host "  .env olusturuldu" -ForegroundColor Green
Write-Host "  project_id  : $($sa.project_id)"
Write-Host "  client_email: $($sa.client_email)"
Write-Host "  private_key : $($sa.private_key.Length) karakter"
Write-Host ""

# .gitignore koruması
$gi = ".gitignore"
$needed = @(".env", "*.json.key", "*firebase-adminsdk*.json", "test-setup.ps1")

if (-not (Test-Path $gi)) { New-Item $gi -ItemType File | Out-Null }
$current = Get-Content $gi -ErrorAction SilentlyContinue

foreach ($line in $needed) {
    if ($current -notcontains $line) {
        Add-Content $gi $line
        Write-Host "  .gitignore'a eklendi: $line" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "  Sonraki adim: supabase secrets set --env-file .env" -ForegroundColor Cyan
Write-Host ""
