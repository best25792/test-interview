import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

/**
 * Network Fault Injection Test
 * 
 * This test runs through Toxiproxy to simulate network failures.
 * Before running, configure Toxiproxy with the setup script.
 * 
 * Test scenarios:
 * 1. Baseline (no faults) - measure normal performance
 * 2. High latency - test timeout handling
 * 3. Intermittent failures - test retry logic
 * 4. Service unavailable - test graceful degradation
 */

// Custom metrics
const requestsWithLatency = new Trend('requests_with_latency');
const timeoutErrors = new Counter('timeout_errors');
const connectionErrors = new Counter('connection_errors');
const successfulRecovery = new Rate('successful_recovery_rate');

export const options = {
  scenarios: {
    // Scenario 1: Baseline test (through proxy, no faults)
    baseline: {
      executor: 'constant-vus',
      vus: 3,
      duration: '30s',
      env: { SCENARIO: 'baseline' },
      tags: { scenario: 'baseline' },
    },
    // Scenario 2: High latency test
    high_latency: {
      executor: 'constant-vus',
      vus: 3,
      duration: '30s',
      startTime: '35s',
      env: { SCENARIO: 'high_latency' },
      tags: { scenario: 'high_latency' },
    },
    // Scenario 3: Intermittent failures
    intermittent: {
      executor: 'constant-vus',
      vus: 3,
      duration: '30s',
      startTime: '70s',
      env: { SCENARIO: 'intermittent' },
      tags: { scenario: 'intermittent' },
    },
  },
  
  thresholds: {
    // Baseline should have low latency
    'http_req_duration{scenario:baseline}': ['p(95)<500'],
    // High latency scenario - expect higher latency but should still complete
    'http_req_duration{scenario:high_latency}': ['p(95)<3000'],
    // Intermittent - some failures expected but recovery should work
    'successful_recovery_rate': ['rate>0.7'],
  },
};

// Service URLs - use Toxiproxy ports for fault injection
const PROXY_HOST = __ENV.PROXY_HOST || 'toxiproxy';
const DIRECT_HOST = __ENV.DIRECT_HOST || 'payment-service';

// Toxiproxy ports
const PAYMENT_PROXY_PORT = 18083;
const USER_PROXY_PORT = 18081;

const headers = { 'Content-Type': 'application/json' };

export default function () {
  const scenario = __ENV.SCENARIO || 'baseline';
  
  group(`Network Fault Test - ${scenario}`, function () {
    // Use proxy URLs for fault injection
    const paymentUrl = `http://${PROXY_HOST}:${PAYMENT_PROXY_PORT}`;
    const userUrl = `http://${PROXY_HOST}:${USER_PROXY_PORT}`;
    
    // Test 1: User service call (may be affected by latency)
    group('User Service Call', function () {
      const startTime = Date.now();
      const res = http.get(`${userUrl}/api/v1/users/1`, { 
        headers,
        timeout: '5s',  // 5 second timeout
      });
      const duration = Date.now() - startTime;
      
      requestsWithLatency.add(duration);
      
      const success = check(res, {
        'user request completed': (r) => r.status === 200 || r.status === 504,
      });

      if (res.status === 0) {
        connectionErrors.add(1);
      } else if (res.status === 504) {
        timeoutErrors.add(1);
      }
      
      // Track recovery
      successfulRecovery.add(res.status === 200 ? 1 : 0);
    });

    // Test 2: Payment service call
    group('Payment Service Call', function () {
      const startTime = Date.now();
      const res = http.get(`${paymentUrl}/api/v1/payments`, {
        headers,
        timeout: '5s',
      });
      const duration = Date.now() - startTime;
      
      requestsWithLatency.add(duration);

      check(res, {
        'payment request completed': (r) => r.status === 200 || r.status === 504 || r.status === 0,
      });

      if (res.status === 0) {
        connectionErrors.add(1);
      } else if (res.status === 504) {
        timeoutErrors.add(1);
      }
      
      successfulRecovery.add(res.status === 200 ? 1 : 0);
    });

    // Test 3: Payment initiation (tests inter-service communication)
    group('Payment Initiation (Inter-service)', function () {
      const res = http.post(
        `${paymentUrl}/api/v1/payments/initiate`,
        JSON.stringify({ userId: 1 }),
        { 
          headers,
          timeout: '10s',  // Longer timeout for inter-service calls
        }
      );

      check(res, {
        'payment initiation handled': (r) => {
          // Accept success, timeout, or graceful error
          return r.status === 201 || r.status === 200 || 
                 r.status === 504 || r.status === 503 || 
                 r.status === 500;
        },
      });

      // Track if the system handles the fault gracefully
      if (res.status === 201 || res.status === 200) {
        successfulRecovery.add(1);
      } else if (res.status === 503 || res.status === 504) {
        // Graceful degradation - system knows it's unhealthy
        successfulRecovery.add(0.5);  // Partial credit
      } else {
        successfulRecovery.add(0);
      }
    });
  });

  sleep(0.5);
}

export function setup() {
  console.log('='.repeat(50));
  console.log('Network Fault Injection Test');
  console.log('='.repeat(50));
  console.log('');
  console.log('Before running this test, configure Toxiproxy:');
  console.log('1. Run: ./load-tests/setup-toxiproxy.sh');
  console.log('2. Or manually configure via Toxiproxy API');
  console.log('');
  console.log('Scenarios:');
  console.log('- baseline: No faults (0-30s)');
  console.log('- high_latency: 500ms added latency (35-65s)');
  console.log('- intermittent: Random failures (70-100s)');
  console.log('');
  
  return { startTime: Date.now() };
}

export function teardown(data) {
  const duration = (Date.now() - data.startTime) / 1000;
  console.log('');
  console.log('='.repeat(50));
  console.log(`Test completed in ${duration.toFixed(2)} seconds`);
  console.log('='.repeat(50));
  console.log('');
  console.log('Review metrics:');
  console.log('- timeout_errors: Number of timeout failures');
  console.log('- connection_errors: Number of connection failures');
  console.log('- successful_recovery_rate: How well the system recovered');
  console.log('- requests_with_latency: Latency distribution');
}
