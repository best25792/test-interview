#!/bin/bash
# ============================================
# Toxiproxy Setup Script for Load Testing
# ============================================
#
# This script configures Toxiproxy to create proxies for each service
# and adds various "toxics" to simulate network failures.
#
# Usage:
#   ./load-tests/setup-toxiproxy.sh [command]
#
# Commands:
#   setup     - Create proxies for all services (default)
#   latency   - Add 500ms latency to payment-service
#   timeout   - Add timeout toxic (connection hangs)
#   reset     - Remove all toxics (back to normal)
#   status    - Show current proxy status
#   cleanup   - Remove all proxies

set -e

TOXIPROXY_HOST="${TOXIPROXY_HOST:-localhost}"
TOXIPROXY_API="http://${TOXIPROXY_HOST}:8474"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Toxiproxy is reachable
check_toxiproxy() {
    if ! curl -s "${TOXIPROXY_API}/version" > /dev/null 2>&1; then
        log_error "Cannot connect to Toxiproxy at ${TOXIPROXY_API}"
        log_error "Make sure Toxiproxy is running: docker compose --profile loadtest up -d toxiproxy"
        exit 1
    fi
    log_info "Toxiproxy is reachable at ${TOXIPROXY_API}"
}

# Create a proxy
create_proxy() {
    local name=$1
    local listen=$2
    local upstream=$3
    
    log_info "Creating proxy: ${name} (${listen} -> ${upstream})"
    
    curl -s -X POST "${TOXIPROXY_API}/proxies" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"${name}\",\"listen\":\"0.0.0.0:${listen}\",\"upstream\":\"${upstream}\"}" \
        > /dev/null 2>&1 || true
}

# Setup all proxies
setup_proxies() {
    log_info "Setting up Toxiproxy proxies for all services..."
    
    create_proxy "user-proxy" "18081" "user-service:8081"
    create_proxy "wallet-proxy" "18082" "wallet-service:8082"
    create_proxy "payment-proxy" "18083" "payment-service:8083"
    create_proxy "qr-proxy" "18084" "qr-service:8084"
    create_proxy "order-proxy" "18085" "order-service:8085"
    
    log_info "All proxies created successfully!"
    echo ""
    log_info "Proxy ports:"
    echo "  - User Service:    localhost:18081 -> user-service:8081"
    echo "  - Wallet Service:  localhost:18082 -> wallet-service:8082"
    echo "  - Payment Service: localhost:18083 -> payment-service:8083"
    echo "  - QR Service:      localhost:18084 -> qr-service:8084"
    echo "  - Order Service:   localhost:18085 -> order-service:8085"
}

# Add latency toxic
add_latency() {
    local proxy="${1:-payment-proxy}"
    local latency="${2:-500}"
    
    log_info "Adding ${latency}ms latency to ${proxy}..."
    
    curl -s -X POST "${TOXIPROXY_API}/proxies/${proxy}/toxics" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"latency_downstream\",\"type\":\"latency\",\"stream\":\"downstream\",\"attributes\":{\"latency\":${latency},\"jitter\":100}}" \
        > /dev/null 2>&1
    
    log_info "Latency toxic added to ${proxy}"
}

# Add timeout toxic (simulates connection hang)
add_timeout() {
    local proxy="${1:-payment-proxy}"
    local timeout="${2:-5000}"
    
    log_info "Adding ${timeout}ms timeout to ${proxy}..."
    
    curl -s -X POST "${TOXIPROXY_API}/proxies/${proxy}/toxics" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"timeout_downstream\",\"type\":\"timeout\",\"stream\":\"downstream\",\"attributes\":{\"timeout\":${timeout}}}" \
        > /dev/null 2>&1
    
    log_info "Timeout toxic added to ${proxy}"
}

# Add bandwidth limit (slow connection)
add_bandwidth_limit() {
    local proxy="${1:-payment-proxy}"
    local rate="${2:-1024}"  # bytes per second
    
    log_info "Adding bandwidth limit (${rate} bytes/s) to ${proxy}..."
    
    curl -s -X POST "${TOXIPROXY_API}/proxies/${proxy}/toxics" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"bandwidth_downstream\",\"type\":\"bandwidth\",\"stream\":\"downstream\",\"attributes\":{\"rate\":${rate}}}" \
        > /dev/null 2>&1
    
    log_info "Bandwidth limit added to ${proxy}"
}

# Add connection reset (simulates network error)
add_reset() {
    local proxy="${1:-payment-proxy}"
    
    log_info "Adding connection reset to ${proxy}..."
    
    curl -s -X POST "${TOXIPROXY_API}/proxies/${proxy}/toxics" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"reset_peer\",\"type\":\"reset_peer\",\"stream\":\"downstream\",\"attributes\":{}}" \
        > /dev/null 2>&1
    
    log_info "Reset toxic added to ${proxy}"
}

# Remove all toxics from a proxy
reset_proxy() {
    local proxy="${1:-payment-proxy}"
    
    log_info "Removing all toxics from ${proxy}..."
    
    # Get list of toxics
    toxics=$(curl -s "${TOXIPROXY_API}/proxies/${proxy}/toxics" | grep -o '"name":"[^"]*"' | cut -d'"' -f4)
    
    for toxic in $toxics; do
        curl -s -X DELETE "${TOXIPROXY_API}/proxies/${proxy}/toxics/${toxic}" > /dev/null 2>&1
        log_info "  Removed toxic: ${toxic}"
    done
    
    log_info "All toxics removed from ${proxy}"
}

# Reset all proxies
reset_all() {
    log_info "Resetting all proxies to normal state..."
    
    for proxy in user-proxy wallet-proxy payment-proxy qr-proxy order-proxy; do
        reset_proxy "$proxy" 2>/dev/null || true
    done
    
    log_info "All proxies reset to normal"
}

# Show status
show_status() {
    log_info "Current Toxiproxy status:"
    echo ""
    curl -s "${TOXIPROXY_API}/proxies" | python3 -m json.tool 2>/dev/null || \
    curl -s "${TOXIPROXY_API}/proxies"
}

# Cleanup - remove all proxies
cleanup() {
    log_info "Removing all proxies..."
    
    for proxy in user-proxy wallet-proxy payment-proxy qr-proxy order-proxy; do
        curl -s -X DELETE "${TOXIPROXY_API}/proxies/${proxy}" > /dev/null 2>&1 || true
        log_info "  Removed proxy: ${proxy}"
    done
    
    log_info "All proxies removed"
}

# Pre-configured test scenarios
scenario_high_latency() {
    log_info "Applying scenario: High Latency (500ms on all services)"
    reset_all
    for proxy in user-proxy wallet-proxy payment-proxy qr-proxy order-proxy; do
        add_latency "$proxy" 500
    done
}

scenario_payment_slow() {
    log_info "Applying scenario: Payment Service Slow (1000ms latency)"
    reset_all
    add_latency "payment-proxy" 1000
}

scenario_intermittent() {
    log_info "Applying scenario: Intermittent Failures"
    reset_all
    # Add slow close to simulate intermittent issues
    curl -s -X POST "${TOXIPROXY_API}/proxies/payment-proxy/toxics" \
        -H "Content-Type: application/json" \
        -d '{"name":"slow_close","type":"slow_close","stream":"downstream","attributes":{"delay":1000}}' \
        > /dev/null 2>&1
    log_info "Intermittent failure toxic added"
}

# Main
main() {
    check_toxiproxy
    
    case "${1:-setup}" in
        setup)
            setup_proxies
            ;;
        latency)
            add_latency "${2:-payment-proxy}" "${3:-500}"
            ;;
        timeout)
            add_timeout "${2:-payment-proxy}" "${3:-5000}"
            ;;
        bandwidth)
            add_bandwidth_limit "${2:-payment-proxy}" "${3:-1024}"
            ;;
        reset)
            reset_all
            ;;
        status)
            show_status
            ;;
        cleanup)
            cleanup
            ;;
        scenario-high-latency)
            scenario_high_latency
            ;;
        scenario-payment-slow)
            scenario_payment_slow
            ;;
        scenario-intermittent)
            scenario_intermittent
            ;;
        *)
            echo "Usage: $0 {setup|latency|timeout|bandwidth|reset|status|cleanup|scenario-*}"
            echo ""
            echo "Commands:"
            echo "  setup                    - Create proxies for all services"
            echo "  latency [proxy] [ms]     - Add latency (default: payment-proxy, 500ms)"
            echo "  timeout [proxy] [ms]     - Add timeout (default: payment-proxy, 5000ms)"
            echo "  bandwidth [proxy] [rate] - Add bandwidth limit (default: 1024 bytes/s)"
            echo "  reset                    - Remove all toxics from all proxies"
            echo "  status                   - Show current proxy configuration"
            echo "  cleanup                  - Remove all proxies"
            echo ""
            echo "Scenarios:"
            echo "  scenario-high-latency    - 500ms latency on all services"
            echo "  scenario-payment-slow    - 1000ms latency on payment service only"
            echo "  scenario-intermittent    - Intermittent connection issues"
            exit 1
            ;;
    esac
}

main "$@"
