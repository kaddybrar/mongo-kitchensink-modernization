#!/bin/bash

# Exit on any error
set -e

echo "Starting performance test suite..."

# Create base directory for reports
mkdir -p performance-reports/runs

# Build and start all services with performance profile
docker compose --profile performance build
docker compose --profile performance up -d

# Wait for services to be ready
echo "Waiting for services to be ready..."
sleep 30

# Create timestamped directory for this test run
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RUN_DIR="performance-reports/runs/${TIMESTAMP}"
mkdir -p "${RUN_DIR}"

# Run backend performance tests
echo "Running backend performance tests..."
TIMESTAMP=${TIMESTAMP} docker compose --profile performance run --rm backend-performance-tests

# Run frontend performance tests and copy results
echo "Running frontend performance tests..."
FRONTEND_DIR="${RUN_DIR}/frontend"
mkdir -p "${FRONTEND_DIR}"

# Run frontend tests
TIMESTAMP=${TIMESTAMP} docker compose --profile performance run --rm frontend-performance-tests

# Generate frontend index.html
cat > "${FRONTEND_DIR}/index.html" << EOL
<!DOCTYPE html>
<html>
<head>
    <title>Frontend Performance Test Results - ${TIMESTAMP}</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        .section { margin: 20px 0; padding: 10px; border: 1px solid #ddd; }
        .timestamp { color: #666; }
    </style>
</head>
<body>
    <h1>Frontend Performance Test Results</h1>
    <div class="timestamp">Test Run: ${TIMESTAMP}</div>
    <div class="section">
        <h2>Test Configuration</h2>
        <pre>
Node Environment: test
Build ID: ${TIMESTAMP}
        </pre>
    </div>
    <div class="section">
        <h2>Test Results</h2>
        <pre>
$(cat "${FRONTEND_DIR}/test-results.txt" 2>/dev/null || echo "No test results available")
        </pre>
    </div>
</body>
</html>
EOL

# Generate combined index
echo "Generating combined index..."
cat > "${RUN_DIR}/index.html" << EOL
<!DOCTYPE html>
<html>
<head>
    <title>Performance Test Results - ${TIMESTAMP}</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        .section { margin: 20px 0; padding: 10px; border: 1px solid #ddd; }
        .timestamp { color: #666; }
        .report-link { 
            display: block; 
            margin: 10px 0;
            padding: 10px;
            background: #f5f5f5;
            text-decoration: none;
            color: #333;
            border-radius: 4px;
        }
        .report-link:hover {
            background: #e5e5e5;
        }
    </style>
</head>
<body>
    <h1>Performance Test Results</h1>
    <div class="timestamp">Test Run: ${TIMESTAMP}</div>
    
    <div class="section">
        <h2>Test Reports</h2>
        <a class="report-link" href="backend/index.html">
            Backend Performance Tests
            <small style="display: block; color: #666;">Database strategy comparison and startup performance</small>
        </a>
        <a class="report-link" href="frontend/performance-report.html">
            Frontend Performance Tests
            <small style="display: block; color: #666;">Page load times and Core Web Vitals</small>
        </a>
    </div>
</body>
</html>
EOL

# Update latest symlink
cd performance-reports
rm -f latest
ln -s "runs/${TIMESTAMP}" latest
cd ..

# Clean up containers
echo "Cleaning up..."
docker compose --profile performance down

echo "Performance tests completed. Reports available at:"
echo "- Latest results: performance-reports/latest/index.html"
echo "- This run: performance-reports/runs/${TIMESTAMP}/index.html" 