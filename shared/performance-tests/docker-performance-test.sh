#!/bin/sh

set -e  # Exit on error

# Set environment variables
export APP_NAME=$1
export TEST_URL=$2
export TEST_RUN_TIMESTAMP=${TEST_RUN_TIMESTAMP:-$(date +%Y%m%d_%H%M%S)}
export BUILD_ID=${TEST_RUN_TIMESTAMP}
export REPORT_DIR="/app/performance-reports/runs/${TEST_RUN_TIMESTAMP}"

echo "Starting performance tests for ${APP_NAME}"
echo "Testing URL: ${TEST_URL}"
echo "Report directory: ${REPORT_DIR}"
echo "Test run timestamp: ${TEST_RUN_TIMESTAMP}"

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

# Run the unified performance tests
echo "Running performance tests..."
node --experimental-vm-modules --no-warnings run-tests.js

echo "Performance tests completed for ${APP_NAME}. Results available in ${REPORT_DIR}" 