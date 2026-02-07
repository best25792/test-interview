# Observability API Collection

This Bruno collection contains API requests to test and verify the observability stack functionality.

## Prerequisites

1. **Start Docker Compose services:**
   ```bash
   docker-compose up -d
   ```

2. **Install Bruno:**
   - Download from: https://www.usebruno.com/
   - Or install via npm: `npm install -g @usebruno/cli`

## Services

### Prometheus (Port 9090)
- **Get Metrics**: Query Prometheus using PromQL
- **Query Range**: Get metrics over a time range
- **List Targets**: View all scrape targets

### Loki (Port 3100)
- **Query Logs**: Query logs using LogQL
- **Label Values**: Get available label values
- **Push Logs**: Send logs directly to Loki

### Tempo (Port 3200)
- **Query Traces**: Search traces by tags
- **Get Trace by ID**: Retrieve specific trace
- **Get Trace Metrics**: Query trace metrics

### Grafana Alloy (Port 4318)
- **Health Check**: Verify Alloy is running and get status
- **Send Trace**: Send trace via OTLP HTTP
- **Send Logs**: Send logs via OTLP HTTP

### Grafana (Port 3001)
- **Health Check**: Verify Grafana is running
- **List Datasources**: View configured datasources
- **Create Datasources**: Create Prometheus, Loki, and Tempo datasources via API

### Application (Port 8080)
- **Generate Telemetry**: Make requests to generate observability data
- **Get Payment**: Retrieve payment data

## Usage

1. **Open Bruno** and load this collection
2. **Select Environment**: Choose "Local" environment
3. **Run Requests**: Execute requests to test observability endpoints
4. **Verify in Grafana**: Check http://localhost:3001 to see telemetry data

## Environment Variables

The `Local.bru` environment file contains:
- `prometheus_url`: http://localhost:9090
- `loki_url`: http://localhost:3100
- `tempo_url`: http://localhost:3200
- `grafana_url`: http://localhost:3001
- `alloy_url`: http://localhost:4318
- `app_url`: http://localhost:8080
- `grafana_user`: admin
- `grafana_password`: admin

## Testing Workflow

1. **Setup Datasources**: Run Grafana datasource creation requests
2. **Generate Data**: Make application requests to generate telemetry
3. **Query Data**: Use Prometheus, Loki, and Tempo queries to verify data
4. **View in Grafana**: Open Grafana and explore the data

## Notes

- Timestamps use Bruno's `{{$timestamp}}` variable (Unix timestamp in seconds)
- Trace IDs use `{{$uuid}}` for generating unique identifiers
- Some requests require authentication (Grafana uses basic auth)
- Grafana Alloy receives OTLP and routes to Tempo (traces), Loki (logs), and Prometheus (metrics)
