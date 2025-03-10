#!/bin/sh

# Exit on any error
set -e

echo "Starting backend performance tests..."

# Wait for MongoDB and PostgreSQL to be ready
echo "Waiting for databases to be ready..."
sleep 10

# Get timestamp for this run
TIMESTAMP=${TIMESTAMP:-$(date +%Y%m%d_%H%M%S)}
TEST_REPORT_DIR="/app/performance-reports/runs/${TIMESTAMP}/backend"
mkdir -p "${TEST_REPORT_DIR}"

echo "Will save reports to: ${TEST_REPORT_DIR}"

# Run performance tests with specific test classes and generate reports
echo "Running performance tests..."
./mvnw test \
  -Dtest=DatabaseStrategyComparisonTest,StartupPerformanceTest \
  -Dspring.profiles.active=test \
  -DfailIfNoTests=false \
  -Djacoco.skip=true \
  -Dspring.data.mongodb.uri=${MONGODB_URI:-mongodb://mongodb:27017/kitchensink_test} \
  -Dspring.datasource.url=${POSTGRES_URL:-jdbc:postgresql://postgres:5432/kitchensink} \
  -Dspring.datasource.username=${POSTGRES_USER:-postgres} \
  -Dspring.datasource.password=${POSTGRES_PASSWORD:-mysecretpassword}

echo "Test execution completed. Processing reports..."

# Copy performance reports
echo "Copying performance reports..."
if [ -d "/app/target/performance-reports" ]; then
    echo "Found performance reports in target directory"
    cp -rv /app/target/performance-reports/* "${TEST_REPORT_DIR}/" || echo "No performance reports found to copy"
fi


# Generate summary report
echo "Generating summary report..."
cat > "${TEST_REPORT_DIR}/index.html" << EOL
<!DOCTYPE html>
<html>
<head>
    <title>Backend Performance Test Results - ${TIMESTAMP}</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        .section { margin: 20px 0; padding: 10px; border: 1px solid #ddd; }
        .timestamp { color: #666; }
    </style>
</head>
<body>
    <h1>Backend Performance Test Results</h1>
    <div class="timestamp">Test Run: ${TIMESTAMP}</div>
    
    <div class="section">
        <h2>Test Configuration</h2>
        <pre>
MongoDB URI: ${MONGODB_URI:-mongodb://mongodb:27017/kitchensink_test}
PostgreSQL URL: ${POSTGRES_URL:-jdbc:postgresql://postgres:5432/kitchensink}
Spring Profile: test
        </pre>
    </div>

    <div class="section">
        <h2>Available Reports</h2>
        <ul>
$(find "${TEST_REPORT_DIR}" -type f -name "*.html" -not -name "index.html" | while read -r file; do
    filename=$(basename "$file")
    echo "            <li><a href=\"./${filename}\">${filename}</a></li>"
done)
        </ul>
    </div>
</body>
</html>
EOL

# List final contents
echo "Final directory structure:"
find "${TEST_REPORT_DIR}" -type f | sort

echo "Performance tests completed. Reports available at: ${TEST_REPORT_DIR}" 