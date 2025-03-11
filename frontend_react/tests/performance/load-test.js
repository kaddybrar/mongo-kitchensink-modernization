import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');

// Test configuration
export const options = {
  stages: [
    { duration: '1m', target: 10 },  // Ramp up to 10 users
    { duration: '3m', target: 10 },  // Stay at 10 users
    { duration: '1m', target: 0 },   // Ramp down to 0 users
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500'], // 95% of requests should be below 500ms
    'errors': ['rate<0.1'],            // Error rate should be below 10%
  },
};

// Main test function
export default function () {
  const BASE_URL = 'http://frontend-new:80';

  // Test homepage load
  const homeResponse = http.get(BASE_URL);
  check(homeResponse, {
    'homepage status is 200': (r) => r.status === 200,
    'homepage loads in under 1s': (r) => r.timings.duration < 1000,
  });

  // Test member list API
  const membersResponse = http.get(`${BASE_URL}/api/members`);
  check(membersResponse, {
    'members API status is 200': (r) => r.status === 200,
    'members API response time < 500ms': (r) => r.timings.duration < 500,
  });

  // Test search functionality
  const searchResponse = http.get(`${BASE_URL}/api/members?name=test`);
  check(searchResponse, {
    'search API status is 200': (r) => r.status === 200,
    'search API response time < 500ms': (r) => r.timings.duration < 500,
  });

  // Record errors
  errorRate.add(homeResponse.status !== 200);
  errorRate.add(membersResponse.status !== 200);
  errorRate.add(searchResponse.status !== 200);

  // Wait between iterations
  sleep(1);
} 