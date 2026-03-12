# add-devices.ps1 — Generate dummy Android devices via the simulation API.
#
# Usage:
#   .\add-devices.ps1 [-Count <int>] [-BaseUrl <string>]
#
# Examples:
#   .\add-devices.ps1                          # Create 5 devices
#   .\add-devices.ps1 -Count 10               # Create 10 devices
#   .\add-devices.ps1 -Count 3 -BaseUrl http://192.168.1.50:5911

param(
    [int]$Count = 5,
    [string]$BaseUrl = "http://localhost:5911"
)

Write-Host "Creating $Count dummy device(s) on $BaseUrl ..."

try {
    $response = Invoke-RestMethod `
        -Uri "$BaseUrl/api/simulation/add-device?count=$Count" `
        -Method Post `
        -ContentType "application/json"

    Write-Host "Success:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 5
}
catch {
    $status = $_.Exception.Response.StatusCode.value__
    Write-Host "Error (HTTP $status):" -ForegroundColor Red
    Write-Host $_.ErrorDetails.Message
    exit 1
}
