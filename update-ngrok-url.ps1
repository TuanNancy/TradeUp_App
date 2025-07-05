$ngrokApi = "http://localhost:4040/api/tunnels"

try {
    Write-Host "Dang lay ngrok URL..." -ForegroundColor Yellow

    $response = Invoke-RestMethod -Uri $ngrokApi -Method Get
    $httpsUrl = $response.tunnels | Where-Object { $_.proto -eq "https" } | Select-Object -ExpandProperty public_url

    if ($httpsUrl) {
        Write-Host "Tim thay ngrok URL: $httpsUrl" -ForegroundColor Green

        # Doc file StripePaymentService.java
        $filePath = "app\src\main\java\com\example\tradeup_app\services\StripePaymentService.java"
        $content = Get-Content $filePath -Raw

        # Thay the URL cu bang URL moi
        $oldPattern = 'private static final String BACKEND_URL = "https://YOUR-NGROK-URL\.ngrok\.io";'
        $newLine = "private static final String BACKEND_URL = `"$httpsUrl`";"

        $updatedContent = $content -replace $oldPattern, $newLine

        # Ghi lai file
        Set-Content -Path $filePath -Value $updatedContent -Encoding UTF8

        Write-Host "Da cap nhat ngrok URL vao StripePaymentService.java" -ForegroundColor Green
        Write-Host "Bay gio ban co the test thanh toan thuc trong app!" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Thong tin test:" -ForegroundColor Magenta
        Write-Host "   So the: 4242424242424242" -ForegroundColor White
        Write-Host "   CVC: 123" -ForegroundColor White
        Write-Host "   Het han: 12/25" -ForegroundColor White
        Write-Host ""
        Write-Host "Backend URL: $httpsUrl" -ForegroundColor Blue
        Write-Host "Health check: $httpsUrl" -ForegroundColor Blue
        Write-Host "Stripe Dashboard: https://dashboard.stripe.com" -ForegroundColor Blue

    } else {
        Write-Host "Khong tim thay ngrok HTTPS URL. Vui long:" -ForegroundColor Red
        Write-Host "   1. Kiem tra ngrok dang chay: ngrok http 3000" -ForegroundColor Yellow
        Write-Host "   2. Mo http://localhost:4040 de xem dashboard" -ForegroundColor Yellow
    }

} catch {
    Write-Host "Loi khi lay ngrok URL: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Cach khac phuc:" -ForegroundColor Yellow
    Write-Host "   1. Mo trinh duyet: http://localhost:4040" -ForegroundColor White
    Write-Host "   2. Copy URL HTTPS tu dashboard" -ForegroundColor White
    Write-Host "   3. Thay the 'YOUR-NGROK-URL.ngrok.io' trong StripePaymentService.java" -ForegroundColor White
}
