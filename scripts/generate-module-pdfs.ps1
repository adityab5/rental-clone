param(
    [string]$OutputRoot = "docs\meeting"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$workspaceRoot = (Get-Location).Path
$outputRootPath = Join-Path $workspaceRoot $OutputRoot
$htmlDir = Join-Path $outputRootPath "html"
$pdfDir = Join-Path $outputRootPath "pdf"

New-Item -ItemType Directory -Path $htmlDir -Force | Out-Null
New-Item -ItemType Directory -Path $pdfDir -Force | Out-Null

$edgeCandidates = @(
    "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    "C:\Program Files\Microsoft\Edge\Application\msedge.exe"
)
$edgePath = $edgeCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1

if (-not $edgePath) {
    throw "Could not find Microsoft Edge. Install Edge or update edge path in scripts/generate-module-pdfs.ps1"
}

function Escape-Html {
    param([string]$Value)

    if ($null -eq $Value) {
        return ""
    }

    return $Value.Replace('&', '&amp;').Replace('<', '&lt;').Replace('>', '&gt;').Replace('"', '&quot;')
}

function Build-EndpointRows {
    param([array]$Endpoints)

    $rows = foreach ($ep in $Endpoints) {
        "<tr><td><code>{0}</code></td><td><code>{1}</code></td><td>{2}</td><td>{3}</td></tr>" -f (Escape-Html $ep.Method), (Escape-Html $ep.Path), (Escape-Html $ep.Role), (Escape-Html $ep.Description)
    }

    return ($rows -join "`n")
}

function Build-SampleBlocks {
    param([array]$Samples)

    if (-not $Samples -or $Samples.Count -eq 0) {
        return "<p>No dedicated sample payload required for this module.</p>"
    }

    $blocks = foreach ($s in $Samples) {
        "<h3>{0}</h3><p><strong>Endpoint:</strong> <code>{1}</code></p><pre>{2}</pre>" -f (Escape-Html $s.Title), (Escape-Html $s.Endpoint), (Escape-Html $s.Body)
    }

    return ($blocks -join "`n")
}

function Build-FlowSteps {
    param([array]$Flow)

    $items = foreach ($step in $Flow) {
        "<li>{0}</li>" -f (Escape-Html $step)
    }

    return "<ol>{0}</ol>" -f ($items -join "`n")
}

function Build-Html {
    param([hashtable]$Module)

    $endpointRows = Build-EndpointRows -Endpoints $Module.Endpoints
    $sampleBlocks = Build-SampleBlocks -Samples $Module.Samples
    $flowList = Build-FlowSteps -Flow $Module.Flow

    @"
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8" />
<title>$($Module.Title) API</title>
<style>
body { font-family: Segoe UI, Arial, sans-serif; margin: 36px; color: #1f2937; }
h1 { margin: 0 0 6px 0; font-size: 28px; }
.meta { color: #4b5563; margin-bottom: 18px; }
h2 { margin-top: 24px; font-size: 20px; border-bottom: 1px solid #e5e7eb; padding-bottom: 4px; }
h3 { margin-top: 18px; font-size: 16px; }
table { border-collapse: collapse; width: 100%; margin-top: 10px; }
th, td { border: 1px solid #d1d5db; text-align: left; padding: 8px; vertical-align: top; }
th { background: #f3f4f6; }
code { background: #f9fafb; padding: 2px 4px; border-radius: 4px; }
pre { background: #111827; color: #f9fafb; padding: 12px; border-radius: 8px; white-space: pre-wrap; }
.note { background: #eff6ff; border-left: 4px solid #3b82f6; padding: 10px 12px; border-radius: 4px; }
.footer { margin-top: 26px; font-size: 12px; color: #6b7280; }
</style>
</head>
<body>
<h1>$($Module.Title) Module</h1>
<div class="meta"><strong>Base Path:</strong> <code>$($Module.BasePath)</code></div>
<div class="note">$($Module.Overview)</div>

<h2>Endpoints</h2>
<table>
<thead><tr><th>Method</th><th>Path</th><th>Role</th><th>Description</th></tr></thead>
<tbody>
$endpointRows
</tbody>
</table>

<h2>Request Samples</h2>
$sampleBlocks

<h2>Flow (Service-Level)</h2>
$flowList

<h2>Error Handling</h2>
<p>Validation and business exceptions are returned in a consistent error format from <code>GlobalExceptionHandler</code> with fields: timestamp, status, error, message, path, and validationErrors.</p>

<div class="footer">Generated on $(Get-Date -Format "yyyy-MM-dd HH:mm:ss") from current backend implementation.</div>
</body>
</html>
"@
}

$modules = @(
    @{
        FileName = "admin"
        Title = "Admin"
        BasePath = "/api/admin"
        Overview = "Administrative APIs for user management and dashboard-level operational metrics."
        Endpoints = @(
            @{ Method = "GET"; Path = "/users"; Role = "ADMIN"; Description = "List all users sorted by createdAt desc." },
            @{ Method = "GET"; Path = "/users/{id}"; Role = "ADMIN"; Description = "Get a single user detail." },
            @{ Method = "DELETE"; Path = "/users/{id}"; Role = "ADMIN"; Description = "Soft-deactivate user account (isDeleted = true)." },
            @{ Method = "GET"; Path = "/dashboard"; Role = "ADMIN"; Description = "Get summary metrics: users, rentals, payments, revenue." }
        )
        Samples = @(
            @{ Title = "Deactivate user"; Endpoint = "DELETE /api/admin/users/2"; Body = "No request body required." }
        )
        Flow = @(
            "Controller delegates to AdminService.",
            "AdminService aggregates counts from UserRepository, RentalRepository, and PaymentRepository.",
            "Deactivate operation marks the user as deleted without physical delete.",
            "Dashboard revenue uses payment status based sums (SUCCESS and PENDING)."
        )
    },
    @{
        FileName = "rental"
        Title = "Rental"
        BasePath = "/api/rentals"
        Overview = "Rental lifecycle APIs: create rental, fetch details, return equipment, and track overdue records."
        Endpoints = @(
            @{ Method = "POST"; Path = "/"; Role = "USER"; Description = "Create rental after user/equipment checks and date validation." },
            @{ Method = "GET"; Path = "/{id}"; Role = "USER"; Description = "Get rental detail by id." },
            @{ Method = "POST"; Path = "/{id}/return"; Role = "USER"; Description = "Mark rental returned and set equipment available." },
            @{ Method = "GET"; Path = "/"; Role = "ADMIN"; Description = "List all rentals ordered by createdAt desc." },
            @{ Method = "GET"; Path = "/overdue"; Role = "ADMIN"; Description = "List overdue ACTIVE rentals (endDate before today)." }
        )
        Samples = @(
            @{ Title = "Create rental"; Endpoint = "POST /api/rentals/"; Body = '{
  "userId": 2,
  "equipmentId": 1,
  "startDate": "2026-03-28",
  "endDate": "2026-03-30"
}' }
        )
        Flow = @(
            "Validate start and end date range.",
            "Ensure user and equipment are present and not soft-deleted.",
            "Check equipment availability and overlapping active rentals.",
            "Compute totalCost as dailyRate x inclusive rental days.",
            "Persist rental as ACTIVE and flip equipment.isAvailable to false."
        )
    },
    @{
        FileName = "payment"
        Title = "Payment"
        BasePath = "/api/payments"
        Overview = "Razorpay order creation and payment verification with HMAC SHA256 signature validation."
        Endpoints = @(
            @{ Method = "POST"; Path = "/create-order"; Role = "USER"; Description = "Create Razorpay order and persist payment as PENDING." },
            @{ Method = "POST"; Path = "/verify"; Role = "USER"; Description = "Verify Razorpay signature and finalize payment SUCCESS/FAILED." }
        )
        Samples = @(
            @{ Title = "Create order"; Endpoint = "POST /api/payments/create-order"; Body = '{
  "rentalId": 1
}' },
            @{ Title = "Verify payment"; Endpoint = "POST /api/payments/verify"; Body = '{
  "rentalId": 1,
  "razorpayOrderId": "order_9A33XWu170gUtm",
  "razorpayPaymentId": "pay_29QQoUBi66xm2f",
  "razorpaySignature": "generated_hmac_signature"
}' }
        )
        Flow = @(
            "Create-order fetches rental and validates amount > 0.",
            "PaymentService calls Razorpay client to create order using amount in paise.",
            "Order id is persisted as gatewayRef and status is set to PENDING.",
            "Verify endpoint recomputes HMAC of orderId|paymentId with keySecret.",
            "Invalid signature marks payment FAILED and throws 400.",
            "Valid signature marks payment SUCCESS, stores paidAt, and triggers success email notification."
        )
    },
    @{
        FileName = "review"
        Title = "Review"
        BasePath = "/api/reviews"
        Overview = "Review APIs to submit ratings and manage moderation views."
        Endpoints = @(
            @{ Method = "POST"; Path = "/"; Role = "USER"; Description = "Submit review and rating for equipment." },
            @{ Method = "GET"; Path = "/equipment/{id}"; Role = "PUBLIC"; Description = "Get all reviews for one equipment." },
            @{ Method = "GET"; Path = "/"; Role = "ADMIN"; Description = "Get all reviews sorted by createdAt desc." },
            @{ Method = "DELETE"; Path = "/{id}"; Role = "ADMIN"; Description = "Delete inappropriate review." }
        )
        Samples = @(
            @{ Title = "Submit review"; Endpoint = "POST /api/reviews/"; Body = '{
  "userId": 2,
  "equipmentId": 1,
  "rating": 5,
  "comment": "Excellent condition and smooth pickup experience."
}' }
        )
        Flow = @(
            "Service validates user and equipment existence.",
            "Prevents duplicate review per user+equipment pair.",
            "Persists review with rating (1-5) and optional comment.",
            "Equipment reviews are returned newest-first for display.",
            "Admin can remove a review by id when moderation is needed."
        )
    },
    @{
        FileName = "notification"
        Title = "Notification"
        BasePath = "/api/notifications"
        Overview = "Email notification API for payment success confirmation using configured SMTP."
        Endpoints = @(
            @{ Method = "POST"; Path = "/"; Role = "USER"; Description = "Send payment success email for a rental with SUCCESS status." }
        )
        Samples = @(
            @{ Title = "Send payment success notification"; Endpoint = "POST /api/notifications/"; Body = '{
  "rentalId": 1
}' }
        )
        Flow = @(
            "Fetch payment by rentalId.",
            "Reject request unless payment status is SUCCESS.",
            "If notification.mail.enabled=false, email send is skipped safely.",
            "If enabled, JavaMailSender sends message to payer email.",
            "Returns delivery metadata in NotificationResponse."
        )
    }
)

foreach ($module in $modules) {
    $htmlPath = Join-Path $htmlDir ("{0}.html" -f $module.FileName)
    $pdfPath = Join-Path $pdfDir ("{0}.pdf" -f $module.FileName)

    $htmlContent = Build-Html -Module $module
    Set-Content -Path $htmlPath -Value $htmlContent -Encoding UTF8

    $fileUri = "file:///" + (($htmlPath -replace "\\", "/") -replace " ", "%20")
    $args = @(
        "--headless=new",
        "--disable-gpu",
        "--print-to-pdf=$pdfPath",
        "--print-to-pdf-no-header",
        "$fileUri"
    )

    Start-Process -FilePath $edgePath -ArgumentList $args -Wait -NoNewWindow

    if (-not (Test-Path $pdfPath)) {
        throw "PDF generation failed for module: $($module.FileName)"
    }

    Write-Host "Generated: $pdfPath"
}
