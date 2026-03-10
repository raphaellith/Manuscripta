# submit-responses.ps1 — Submit dummy responses to questions.
#
# Usage:
#   .\submit-responses.ps1 -DeviceId <guid> -QuestionId <guid> [-Answer <string>] [-BaseUrl <string>]
#                          [-ResponseCount <int>] [-IsCorrect <bool>] [-ResponseId <guid>] [-Timestamp <string>]
#
# Examples:
#   # Written answer
#   .\submit-responses.ps1 -DeviceId "a1b2c3d4-..." -QuestionId "11111111-..." -Answer "Photosynthesis"
#
#   # Multiple-choice answer (select option index 2)
#   .\submit-responses.ps1 -DeviceId "a1b2c3d4-..." -QuestionId "11111111-..." -Answer "2"
#
#   # Submit 5 responses with unique IDs
#   .\submit-responses.ps1 -DeviceId "a1b2c3d4-..." -QuestionId "11111111-..." -Answer "My answer" -ResponseCount 5
#
#   # With correctness flag
#   .\submit-responses.ps1 -DeviceId "a1b2c3d4-..." -QuestionId "11111111-..." -Answer "1" -IsCorrect $true

param(
    [Parameter(Mandatory = $true)]
    [string]$DeviceId,

    [Parameter(Mandatory = $true)]
    [string]$QuestionId,

    [string]$Answer = "Sample answer from device",

    [string]$BaseUrl = "http://localhost:5911",

    [int]$ResponseCount = 1,

    [Nullable[bool]]$IsCorrect = $null,

    [string]$ResponseId = "",

    [string]$Timestamp = ""
)

function Submit-One {
    param(
        [string]$rid,
        [string]$ts
    )

    $body = @{
        Id         = $rid
        QuestionId = $QuestionId
        DeviceId   = $DeviceId
        Answer     = $Answer
        Timestamp  = $ts
    }

    if ($null -ne $IsCorrect) {
        $body["IsCorrect"] = $IsCorrect
    }

    $json = $body | ConvertTo-Json

    Write-Host "Submitting response $rid ..."
    Write-Host "  Device:   $DeviceId"
    Write-Host "  Question: $QuestionId"
    Write-Host "  Answer:   $Answer"
    Write-Host "  Time:     $ts"

    try {
        Invoke-RestMethod `
            -Uri "$BaseUrl/api/v1/responses" `
            -Method Post `
            -ContentType "application/json" `
            -Body $json | Out-Null

        Write-Host "  -> Success" -ForegroundColor Green
        return $true
    }
    catch {
        $status = $_.Exception.Response.StatusCode.value__
        Write-Host "  -> Error (HTTP $status): $($_.ErrorDetails.Message)" -ForegroundColor Red
        return $false
    }
}

Write-Host "Submitting $ResponseCount response(s) to $BaseUrl ...`n"

$failures = 0

for ($i = 1; $i -le $ResponseCount; $i++) {
    # Generate a unique ID per iteration unless the caller pinned one (only sensible for count=1)
    $rid = if ($ResponseId -and $ResponseCount -eq 1) { $ResponseId } else { [guid]::NewGuid().ToString() }
    $ts  = if ($Timestamp) { $Timestamp } else { (Get-Date).ToUniversalTime().ToString("o") }

    $ok = Submit-One -rid $rid -ts $ts
    if (-not $ok) { $failures++ }
    Write-Host ""
}

Write-Host "Done. $ResponseCount submitted, $failures failed."
if ($failures -gt 0) { exit 1 }
