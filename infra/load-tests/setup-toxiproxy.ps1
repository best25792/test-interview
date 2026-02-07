# ============================================
# Toxiproxy Setup Script for Load Testing (PowerShell)
# ============================================
#
# This script configures Toxiproxy to create proxies for each service
# and adds various "toxics" to simulate network failures.
#
# Usage:
#   .\load-tests\setup-toxiproxy.ps1 -Command <command>
#
# Commands:
#   setup     - Create proxies for all services (default)
#   latency   - Add 500ms latency to payment-service
#   timeout   - Add timeout toxic (connection hangs)
#   reset     - Remove all toxics (back to normal)
#   status    - Show current proxy status
#   cleanup   - Remove all proxies

param(
    [Parameter(Position=0)]
    [ValidateSet("setup", "latency", "timeout", "bandwidth", "reset", "status", "cleanup", 
                 "scenario-high-latency", "scenario-payment-slow", "scenario-intermittent")]
    [string]$Command = "setup",
    
    [string]$Proxy = "payment-proxy",
    [int]$Value = 500
)

$ToxiproxyHost = if ($env:TOXIPROXY_HOST) { $env:TOXIPROXY_HOST } else { "localhost" }
$ToxiproxyAPI = "http://${ToxiproxyHost}:8474"
$UserAgent = "toxiproxy-client"

function Write-Info($message) {
    Write-Host "[INFO] $message" -ForegroundColor Green
}

function Write-Warn($message) {
    Write-Host "[WARN] $message" -ForegroundColor Yellow
}

function Write-Err($message) {
    Write-Host "[ERROR] $message" -ForegroundColor Red
}

function Test-Toxiproxy {
    try {
        $response = Invoke-RestMethod -Uri "$ToxiproxyAPI/version" -Method Get -UserAgent $UserAgent -ErrorAction Stop
        Write-Info "Toxiproxy is reachable at $ToxiproxyAPI"
        return $true
    }
    catch {
        Write-Err "Cannot connect to Toxiproxy at $ToxiproxyAPI"
        Write-Err "Make sure Toxiproxy is running: docker compose --profile loadtest up -d toxiproxy"
        return $false
    }
}

function New-Proxy($name, $listen, $upstream) {
    Write-Info "Creating proxy: $name ($listen -> $upstream)"
    
    $body = @{
        name = $name
        listen = "0.0.0.0:$listen"
        upstream = $upstream
    } | ConvertTo-Json
    
    try {
        Invoke-RestMethod -Uri "$ToxiproxyAPI/proxies" -Method Post -Body $body -ContentType "application/json" -UserAgent $UserAgent -ErrorAction SilentlyContinue
    }
    catch {
        # Proxy might already exist, ignore
    }
}

function Initialize-Proxies {
    Write-Info "Setting up Toxiproxy proxies for all services..."
    
    New-Proxy "user-proxy" "18081" "user-service:8081"
    New-Proxy "wallet-proxy" "18082" "wallet-service:8082"
    New-Proxy "payment-proxy" "18083" "payment-service:8083"
    New-Proxy "qr-proxy" "18084" "qr-service:8084"
    New-Proxy "order-proxy" "18085" "order-service:8085"
    
    Write-Info "All proxies created successfully!"
    Write-Host ""
    Write-Info "Proxy ports:"
    Write-Host "  - User Service:    localhost:18081 -> user-service:8081"
    Write-Host "  - Wallet Service:  localhost:18082 -> wallet-service:8082"
    Write-Host "  - Payment Service: localhost:18083 -> payment-service:8083"
    Write-Host "  - QR Service:      localhost:18084 -> qr-service:8084"
    Write-Host "  - Order Service:   localhost:18085 -> order-service:8085"
}

function Add-Latency($proxy, $latency) {
    Write-Info "Adding ${latency}ms latency to $proxy..."
    
    $body = @{
        name = "latency_downstream"
        type = "latency"
        stream = "downstream"
        attributes = @{
            latency = $latency
            jitter = 100
        }
    } | ConvertTo-Json -Depth 3
    
    try {
        Invoke-RestMethod -Uri "$ToxiproxyAPI/proxies/$proxy/toxics" -Method Post -Body $body -ContentType "application/json" -UserAgent $UserAgent -ErrorAction Stop
        Write-Info "Latency toxic added to $proxy"
    }
    catch {
        Write-Warn "Failed to add latency toxic: $_"
    }
}

function Add-Timeout($proxy, $timeout) {
    Write-Info "Adding ${timeout}ms timeout to $proxy..."
    
    $body = @{
        name = "timeout_downstream"
        type = "timeout"
        stream = "downstream"
        attributes = @{
            timeout = $timeout
        }
    } | ConvertTo-Json -Depth 3
    
    try {
        Invoke-RestMethod -Uri "$ToxiproxyAPI/proxies/$proxy/toxics" -Method Post -Body $body -ContentType "application/json" -UserAgent $UserAgent -ErrorAction Stop
        Write-Info "Timeout toxic added to $proxy"
    }
    catch {
        Write-Warn "Failed to add timeout toxic: $_"
    }
}

function Add-BandwidthLimit($proxy, $rate) {
    Write-Info "Adding bandwidth limit ($rate bytes/s) to $proxy..."
    
    $body = @{
        name = "bandwidth_downstream"
        type = "bandwidth"
        stream = "downstream"
        attributes = @{
            rate = $rate
        }
    } | ConvertTo-Json -Depth 3
    
    try {
        Invoke-RestMethod -Uri "$ToxiproxyAPI/proxies/$proxy/toxics" -Method Post -Body $body -ContentType "application/json" -UserAgent $UserAgent -ErrorAction Stop
        Write-Info "Bandwidth limit added to $proxy"
    }
    catch {
        Write-Warn "Failed to add bandwidth toxic: $_"
    }
}

function Reset-Proxy($proxy) {
    Write-Info "Removing all toxics from $proxy..."
    
    try {
        $toxics = Invoke-RestMethod -Uri "$ToxiproxyAPI/proxies/$proxy/toxics" -Method Get -UserAgent $UserAgent -ErrorAction SilentlyContinue
        foreach ($toxic in $toxics) {
            Invoke-RestMethod -Uri "$ToxiproxyAPI/proxies/$proxy/toxics/$($toxic.name)" -Method Delete -UserAgent $UserAgent -ErrorAction SilentlyContinue
            Write-Info "  Removed toxic: $($toxic.name)"
        }
    }
    catch {
        # Ignore errors
    }
    
    Write-Info "All toxics removed from $proxy"
}

function Reset-AllProxies {
    Write-Info "Resetting all proxies to normal state..."
    
    foreach ($proxy in @("user-proxy", "wallet-proxy", "payment-proxy", "qr-proxy", "order-proxy")) {
        Reset-Proxy $proxy
    }
    
    Write-Info "All proxies reset to normal"
}

function Get-Status {
    Write-Info "Current Toxiproxy status:"
    Write-Host ""
    
    try {
        $proxies = Invoke-RestMethod -Uri "$ToxiproxyAPI/proxies" -Method Get -UserAgent $UserAgent
        $proxies | ConvertTo-Json -Depth 5
    }
    catch {
        Write-Err "Failed to get status: $_"
    }
}

function Remove-AllProxies {
    Write-Info "Removing all proxies..."
    
    foreach ($proxy in @("user-proxy", "wallet-proxy", "payment-proxy", "qr-proxy", "order-proxy")) {
        try {
            Invoke-RestMethod -Uri "$ToxiproxyAPI/proxies/$proxy" -Method Delete -UserAgent $UserAgent -ErrorAction SilentlyContinue
            Write-Info "  Removed proxy: $proxy"
        }
        catch {
            # Ignore
        }
    }
    
    Write-Info "All proxies removed"
}

function Invoke-ScenarioHighLatency {
    Write-Info "Applying scenario: High Latency (500ms on all services)"
    Reset-AllProxies
    foreach ($proxy in @("user-proxy", "wallet-proxy", "payment-proxy", "qr-proxy", "order-proxy")) {
        Add-Latency $proxy 500
    }
}

function Invoke-ScenarioPaymentSlow {
    Write-Info "Applying scenario: Payment Service Slow (1000ms latency)"
    Reset-AllProxies
    Add-Latency "payment-proxy" 1000
}

function Invoke-ScenarioIntermittent {
    Write-Info "Applying scenario: Intermittent Failures"
    Reset-AllProxies
    
    $body = @{
        name = "slow_close"
        type = "slow_close"
        stream = "downstream"
        attributes = @{
            delay = 1000
        }
    } | ConvertTo-Json -Depth 3
    
    try {
        Invoke-RestMethod -Uri "$ToxiproxyAPI/proxies/payment-proxy/toxics" -Method Post -Body $body -ContentType "application/json" -UserAgent $UserAgent
        Write-Info "Intermittent failure toxic added"
    }
    catch {
        Write-Warn "Failed to add intermittent toxic: $_"
    }
}

# Main
if (-not (Test-Toxiproxy)) {
    exit 1
}

switch ($Command) {
    "setup" { Initialize-Proxies }
    "latency" { Add-Latency $Proxy $Value }
    "timeout" { Add-Timeout $Proxy $Value }
    "bandwidth" { Add-BandwidthLimit $Proxy $Value }
    "reset" { Reset-AllProxies }
    "status" { Get-Status }
    "cleanup" { Remove-AllProxies }
    "scenario-high-latency" { Invoke-ScenarioHighLatency }
    "scenario-payment-slow" { Invoke-ScenarioPaymentSlow }
    "scenario-intermittent" { Invoke-ScenarioIntermittent }
    default {
        Write-Host "Usage: .\setup-toxiproxy.ps1 -Command <command> [-Proxy <proxy>] [-Value <value>]"
        Write-Host ""
        Write-Host "Commands:"
        Write-Host "  setup                    - Create proxies for all services"
        Write-Host "  latency                  - Add latency (default: payment-proxy, 500ms)"
        Write-Host "  timeout                  - Add timeout (default: payment-proxy, 5000ms)"
        Write-Host "  bandwidth                - Add bandwidth limit (default: 1024 bytes/s)"
        Write-Host "  reset                    - Remove all toxics from all proxies"
        Write-Host "  status                   - Show current proxy configuration"
        Write-Host "  cleanup                  - Remove all proxies"
        Write-Host ""
        Write-Host "Scenarios:"
        Write-Host "  scenario-high-latency    - 500ms latency on all services"
        Write-Host "  scenario-payment-slow    - 1000ms latency on payment service only"
        Write-Host "  scenario-intermittent    - Intermittent connection issues"
    }
}
