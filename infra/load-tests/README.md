# Load Testing Guide

This directory contains load testing scripts for the payment system microservices.

## Prerequisites

1. Docker and Docker Compose installed
2. All services running via Docker Compose

## Quick Start

### 1. Start Infrastructure and Services

```bash
# Start everything (infrastructure + services)
docker compose up -d

# Or start with load testing tools
docker compose --profile loadtest up -d
```

### 2. Run Baseline Load Test

```bash
# Run payment flow load test
docker compose --profile loadtest run --rm k6 run /scripts/payment-flow.js
```

### 3. Run Network Fault Tests

```powershell
# Start Toxiproxy
docker compose --profile loadtest up -d toxiproxy

# Setup proxies (PowerShell)
.\load-tests\setup-toxiproxy.ps1 -Command setup

# Add latency to payment service
.\load-tests\setup-toxiproxy.ps1 -Command scenario-payment-slow

# Run network fault test
docker compose --profile loadtest run --rm k6 run /scripts/network-fault.js

# Reset to normal
.\load-tests\setup-toxiproxy.ps1 -Command reset
```

## Test Scripts

### payment-flow.js

Main load test that simulates the payment flow:
1. Verify user exists
2. Check wallet balance
3. Initiate payment
4. Check payment status

**Configuration:**
- Ramp up: 5 users → 10 users
- Duration: ~3.5 minutes
- Thresholds: 95% requests < 1s, < 10% failure rate

### network-fault.js

Tests system resilience under network failures:
- **Baseline scenario**: Normal operation (30s)
- **High latency scenario**: 500ms added latency (30s)
- **Intermittent scenario**: Random connection issues (30s)

## Toxiproxy Commands

### PowerShell (Windows)

```powershell
# Setup proxies
.\load-tests\setup-toxiproxy.ps1 -Command setup

# Add 500ms latency to payment service
.\load-tests\setup-toxiproxy.ps1 -Command latency -Proxy payment-proxy -Value 500

# Add 2 second timeout
.\load-tests\setup-toxiproxy.ps1 -Command timeout -Proxy payment-proxy -Value 2000

# Pre-configured scenarios
.\load-tests\setup-toxiproxy.ps1 -Command scenario-high-latency
.\load-tests\setup-toxiproxy.ps1 -Command scenario-payment-slow
.\load-tests\setup-toxiproxy.ps1 -Command scenario-intermittent

# Reset all toxics
.\load-tests\setup-toxiproxy.ps1 -Command reset

# View status
.\load-tests\setup-toxiproxy.ps1 -Command status

# Cleanup
.\load-tests\setup-toxiproxy.ps1 -Command cleanup
```

### Bash (Linux/Mac/WSL)

```bash
# Setup proxies
./load-tests/setup-toxiproxy.sh setup

# Add latency
./load-tests/setup-toxiproxy.sh latency payment-proxy 500

# Scenarios
./load-tests/setup-toxiproxy.sh scenario-high-latency

# Reset
./load-tests/setup-toxiproxy.sh reset
```

## Proxy Ports

When using Toxiproxy, connect to these ports instead of the service ports:

| Service | Direct Port | Proxy Port |
|---------|-------------|------------|
| User Service | 8081 | 18081 |
| Wallet Service | 8082 | 18082 |
| Payment Service | 8083 | 18083 |
| QR Service | 8084 | 18084 |
| Order Service | 8085 | 18085 |

## Network Fault Types

| Toxic Type | Description |
|------------|-------------|
| `latency` | Adds delay to responses |
| `timeout` | Stops response (simulates hang) |
| `bandwidth` | Limits data transfer rate |
| `slow_close` | Delays connection close |
| `reset_peer` | Resets TCP connection |

## Monitoring

During load tests, monitor your services using:

- **Grafana**: http://localhost:3001 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Kafka UI**: http://localhost:38080

## Resource Limits

All containers have resource limits to protect your local machine:

| Component | CPU | Memory |
|-----------|-----|--------|
| Each backend service | 1 core | 512MB |
| k6 | 1 core | 512MB |
| Toxiproxy | 0.5 core | 256MB |

## Troubleshooting

### Services not starting
```bash
# Check logs
docker compose logs user-service

# Rebuild images
docker compose build --no-cache user-service
```

### Toxiproxy not reachable
```bash
# Make sure it's running
docker compose --profile loadtest up -d toxiproxy

# Check if API is accessible
curl http://localhost:8474/version
```

### k6 can't reach services
```bash
# Verify network
docker compose --profile loadtest run --rm k6 run -e BASE_URL=http://payment-service /scripts/payment-flow.js
```
