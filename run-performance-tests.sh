#!/bin/bash

# Store overall test status
TESTS_PASSED=0

echo "Starting performance test suite..."

# Create base directory for reports
mkdir -p performance-reports/runs

# Set timestamp for this test run
TEST_RUN_TIMESTAMP=$(date +%Y%m%d_%H%M%S)
export TEST_RUN_TIMESTAMP
RUN_DIR="performance-reports/runs/${TEST_RUN_TIMESTAMP}"
mkdir -p "${RUN_DIR}"

echo "Test run timestamp: ${TEST_RUN_TIMESTAMP}"
echo "Reports will be generated in: ${RUN_DIR}"

# Create main index.html
cat > "${RUN_DIR}/index.html" << EOL
<!DOCTYPE html>
<html>
<head>
    <title>Performance Test Results - ${TEST_RUN_TIMESTAMP}</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; max-width: 1200px; margin: 0 auto; padding: 20px; }
        h1, h2 { color: #333; }
        .section { margin: 20px 0; padding: 20px; border: 1px solid #ddd; border-radius: 8px; }
        .timestamp { color: #666; margin-bottom: 20px; }
        .report-group { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; }
        .report-card {
            padding: 20px;
            background: #f8f9fa;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .report-card h3 { margin-top: 0; color: #2c3e50; }
        .report-link {
            display: block;
            margin: 10px 0;
            padding: 10px;
            background: #fff;
            text-decoration: none;
            color: #333;
            border-radius: 4px;
            border: 1px solid #ddd;
            transition: all 0.2s ease;
        }
        .report-link:hover {
            background: #f0f0f0;
            transform: translateY(-2px);
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }
    </style>
</head>
<body>
    <h1>Performance Test Results</h1>
    <div class="timestamp">Test Run: ${TEST_RUN_TIMESTAMP}</div>
    
    <div class="report-group">
        <div class="report-card">
            <h3>Backend</h3>
            <a class="report-link" href="backend/index.html">
                Performance Report
                <small style="display: block; color: #666;">API performance metrics and resource usage</small>
            </a>
        </div>
        
        <div class="report-card">
            <h3>Original Frontend</h3>
            <a class="report-link" href="frontend/performance-report.html">
                Performance Report
                <small style="display: block; color: #666;">Core Web Vitals, Lighthouse scores, and API metrics</small>
            </a>
        </div>
        
        <div class="report-card">
            <h3>React Frontend</h3>
            <a class="report-link" href="frontend-react/performance-report.html">
                Performance Report
                <small style="display: block; color: #666;">Core Web Vitals, Lighthouse scores, and API metrics</small>
            </a>
        </div>
    </div>
</body>
</html>
EOL

# Create subdirectories for each service
mkdir -p "${RUN_DIR}/backend" "${RUN_DIR}/frontend" "${RUN_DIR}/frontend-react"

# Build and start all services with performance profile
TEST_RUN_TIMESTAMP=${TEST_RUN_TIMESTAMP} docker compose --profile performance build
TEST_RUN_TIMESTAMP=${TEST_RUN_TIMESTAMP} docker compose --profile performance up -d || { echo "Failed to start services"; exit 1; }

# Wait for services to be ready
echo "Waiting for services to be ready..."
sleep 30

# Run backend performance tests
echo "Running backend performance tests..."
if TEST_RUN_TIMESTAMP=${TEST_RUN_TIMESTAMP} docker compose --profile performance run -e TEST_RUN_TIMESTAMP=${TEST_RUN_TIMESTAMP} -e REPORT_SUBDIR=backend --rm backend-performance-tests; then
    echo "Backend tests completed successfully"
else
    echo "Backend tests failed"
    TESTS_PASSED=1
fi

# Run original frontend performance tests
echo "Running original frontend performance tests..."
if TEST_RUN_TIMESTAMP=${TEST_RUN_TIMESTAMP} docker compose --profile performance run -e TEST_RUN_TIMESTAMP=${TEST_RUN_TIMESTAMP} -e REPORT_SUBDIR=frontend --rm frontend-performance-tests; then
    echo "Original frontend tests completed successfully"
else
    echo "Original frontend tests failed"
    TESTS_PASSED=1
fi

# Run React frontend performance tests
echo "Running React frontend performance tests..."
if TEST_RUN_TIMESTAMP=${TEST_RUN_TIMESTAMP} docker compose --profile performance run -e TEST_RUN_TIMESTAMP=${TEST_RUN_TIMESTAMP} -e REPORT_SUBDIR=frontend-react --rm frontend-react-performance-tests; then
    echo "React frontend tests completed successfully"
else
    echo "React frontend tests failed"
    TESTS_PASSED=1
fi

# Update latest symlink
cd performance-reports
rm -f latest
ln -s "runs/${TEST_RUN_TIMESTAMP}" latest
cd ..

# Clean up containers
echo "Cleaning up..."
TEST_RUN_TIMESTAMP=${TEST_RUN_TIMESTAMP} docker compose --profile performance down

echo "Performance tests completed. Reports available at:"
echo "- Latest results: performance-reports/latest/index.html"
echo "- This run: performance-reports/runs/${TEST_RUN_TIMESTAMP}/index.html"

# Exit with proper status code
exit $TESTS_PASSED 