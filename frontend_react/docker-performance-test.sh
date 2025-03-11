#!/bin/sh

set -e  # Exit on error

# Set timestamp
TIMESTAMP=${TIMESTAMP:-$(date +%Y%m%d_%H%M%S)}
REPORT_DIR="/app/performance-reports/${TIMESTAMP}"

echo "Creating performance reports directory at ${REPORT_DIR}"
mkdir -p "${REPORT_DIR}"

# Function to check if a command exists
command_exists() {
  command -v "$1" >/dev/null 2>&1
}

# Run k6 performance tests
if command_exists k6; then
  echo "Running k6 performance tests..."
  k6 run /app/tests/performance/load-test.js --out json="${REPORT_DIR}/k6-results.json"
else
  echo "Warning: k6 is not installed, skipping k6 tests"
  echo '{"metrics":{"http_reqs":{"values":{"count":0}},"http_req_duration":{"values":{"avg":0,"p95":0}},"errors":{"values":{"rate":0}}}}' > "${REPORT_DIR}/k6-results.json"
fi

# Run Lighthouse performance tests
if command_exists lighthouse; then
  echo "Running Lighthouse performance tests..."
  lighthouse http://frontend-new:80 \
    --chrome-flags="--headless --no-sandbox --disable-gpu" \
    --output json --output html \
    --output-path "${REPORT_DIR}/lighthouse-report"
else
  echo "Warning: lighthouse is not installed, skipping lighthouse tests"
  echo '{"categories":{"performance":{"score":0},"accessibility":{"score":0},"best-practices":{"score":0},"seo":{"score":0}},"audits":{"first-contentful-paint":{"numericValue":0},"interactive":{"numericValue":0}}}' > "${REPORT_DIR}/lighthouse-report.json"
fi

# Run React performance tests
echo "Running React performance tests..."
PERFORMANCE_TEST=true node --experimental-modules /app/tests/performance/react-perf.js > "${REPORT_DIR}/react-perf.log" 2>&1

# Generate summary report
echo "Generating summary report..."
node --experimental-modules /app/tests/performance/generate-report.js \
  --k6-results="${REPORT_DIR}/k6-results.json" \
  --lighthouse-results="${REPORT_DIR}/lighthouse-report.json" \
  --react-results="${REPORT_DIR}/react-perf.log" \
  --output="${REPORT_DIR}/summary.json"

echo "Performance tests completed. Results are available in ${REPORT_DIR}"

# Create a symlink to the latest results
cd /app/performance-reports
ln -sf "${TIMESTAMP}" latest 