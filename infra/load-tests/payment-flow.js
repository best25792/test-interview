import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics
const paymentInitiated = new Counter('payments_initiated');
const paymentFailed = new Rate('payment_failure_rate');
const paymentDuration = new Trend('payment_duration');

// Test configuration
export const options = {
  // Stages for gradual ramp-up (gentle on local machine)
  stages: [
    { duration: '30s', target: 5 },   // Ramp up to 5 users
    { duration: '1m', target: 5 },    // Stay at 5 users
    { duration: '30s', target: 10 },  // Ramp up to 10 users
    { duration: '1m', target: 10 },   // Stay at 10 users
    { duration: '30s', target: 0 },   // Ramp down
  ],
  
  // Performance thresholds
  thresholds: {
    http_req_duration: ['p(95)<1000'],  // 95% of requests under 1s
    http_req_failed: ['rate<0.1'],       // Less than 10% failure rate
    payment_failure_rate: ['rate<0.1'],  // Custom payment failure rate
  },
};

// Base URLs (configurable via environment)
const USER_SERVICE = __ENV.USER_SERVICE_URL || 'http://user-service:8081';
const PAYMENT_SERVICE = __ENV.PAYMENT_SERVICE_URL || 'http://payment-service:8083';
const ORDER_SERVICE = __ENV.ORDER_SERVICE_URL || 'http://order-service:8085';

const headers = { 'Content-Type': 'application/json' };

export default function () {
  // Test user ID (assuming user 1 exists with sufficient balance)
  const userId = 1;

  group('Payment Flow', function () {
    // Step 1: Check user exists and has balance
    group('1. Verify User', function () {
      const userRes = http.get(`${USER_SERVICE}/api/v1/users/${userId}`, { headers });
      check(userRes, {
        'user exists': (r) => r.status === 200,
        'user response time OK': (r) => r.timings.duration < 500,
      });

      if (userRes.status !== 200) {
        paymentFailed.add(1);
        return;
      }
    });

    // Step 2: Check wallet balance
    group('2. Check Balance', function () {
      const balanceRes = http.get(
        `${USER_SERVICE}/api/v1/users/${userId}/wallet/balance`,
        { headers }
      );
      check(balanceRes, {
        'balance check OK': (r) => r.status === 200,
      });
    });

    // Step 3: Initiate payment
    let paymentId = null;
    group('3. Initiate Payment', function () {
      const startTime = Date.now();
      
      const initiateRes = http.post(
        `${PAYMENT_SERVICE}/api/v1/payments/initiate`,
        JSON.stringify({ userId: userId }),
        { headers }
      );

      const duration = Date.now() - startTime;
      paymentDuration.add(duration);

      const success = check(initiateRes, {
        'payment initiated': (r) => r.status === 201,
        'has payment ID': (r) => {
          try {
            const body = JSON.parse(r.body);
            return body.paymentId !== undefined || body.id !== undefined;
          } catch {
            return false;
          }
        },
      });

      if (success) {
        paymentInitiated.add(1);
        try {
          const body = JSON.parse(initiateRes.body);
          paymentId = body.paymentId || body.id;
        } catch {
          // ignore
        }
      } else {
        paymentFailed.add(1);
      }
    });

    // Step 4: Get payment status (if payment was created)
    if (paymentId) {
      group('4. Check Payment Status', function () {
        const statusRes = http.get(
          `${PAYMENT_SERVICE}/api/v1/payments/${paymentId}`,
          { headers }
        );
        check(statusRes, {
          'payment status OK': (r) => r.status === 200,
        });
      });
    }
  });

  // Think time between iterations
  sleep(1);
}

// Setup function - runs once before the test
export function setup() {
  console.log('Starting Payment Flow Load Test');
  console.log(`User Service: ${USER_SERVICE}`);
  console.log(`Payment Service: ${PAYMENT_SERVICE}`);
  
  // Verify services are reachable
  const userHealth = http.get(`${USER_SERVICE}/api/v1/users/1`);
  if (userHealth.status !== 200) {
    console.warn('Warning: User service may not be ready');
  }
  
  return { startTime: Date.now() };
}

// Teardown function - runs once after the test
export function teardown(data) {
  const duration = (Date.now() - data.startTime) / 1000;
  console.log(`Test completed in ${duration.toFixed(2)} seconds`);
}
