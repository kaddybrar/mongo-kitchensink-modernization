#!/bin/bash

# Set up environment variables
export TIMESTAMP=${TIMESTAMP:-$(date +%Y%m%d_%H%M%S)}
export DOCKER_BUILD_ID=${DOCKER_BUILD_ID:-$TIMESTAMP}
export NODE_ENV=test

# Debug information
echo "Node version: $(node -v)"
echo "NPM version: $(npm -v)"
echo "Current directory: $(pwd)"
echo "Directory contents: $(ls -la)"

# Install dependencies
echo "Installing dependencies..."
npm install

# Create reports directory
REPORT_DIR="/app/performance-reports/runs/${TIMESTAMP}/frontend"
mkdir -p "$REPORT_DIR"
echo "Will save reports to: ${REPORT_DIR}"

# Wait for frontend to be ready
echo "Waiting for frontend service to be ready..."
while ! curl -s http://frontend:80 > /dev/null; do
    sleep 5
done

# Create a temporary JavaScript file in the app directory
cat << 'JAVASCRIPT' > ./performance-test.js
const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

console.log('Starting performance test script...');
console.log('Current directory:', process.cwd());
console.log('Directory contents:', fs.readdirSync('.'));

async function loadTestScripts() {
    try {
        // Read the test script files
        const performanceTestsJs = fs.readFileSync(path.join(__dirname, 'resources/js/performance-tests.js'), 'utf8');
        const reportServiceJs = fs.readFileSync(path.join(__dirname, 'resources/js/performance-report-service.js'), 'utf8');
        const reportAnalyzerJs = fs.readFileSync(path.join(__dirname, 'resources/js/performance-report-analyzer.js'), 'utf8');
        return { performanceTestsJs, reportServiceJs, reportAnalyzerJs };
    } catch (error) {
        console.error('Error loading test scripts:', error);
        throw error;
    }
}

async function generateHtmlReport(report) {
    // Add debug logging
    console.log('Report data:', JSON.stringify(report, null, 2));

    const getMetricClass = (value, thresholds) => {
        if (!value) return '';
        if (value <= thresholds.good) return 'good-metric';
        if (value <= thresholds.warning) return 'warning-metric';
        return 'bad-metric';
    };

    const formatMetric = (value, unit = 'ms') => {
        if (!value) return 'N/A';
        return `${value.toFixed(2)}${unit}`;
    };

    const thresholds = {
        LCP: { good: 2500, warning: 4000 },
        FID: { good: 100, warning: 300 },
        CLS: { good: 0.1, warning: 0.25 }
    };

    // Safely access metrics with fallbacks
    const metrics = report.metrics || {};
    const summary = metrics.summary || {};
    const resourceMetrics = summary.resourceMetrics || {};

    return `
<!DOCTYPE html>
<html>
<head>
    <title>Performance Test Report - ${new Date(report.timestamp).toLocaleString()}</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        .metric-card { margin-bottom: 1rem; }
        pre { background: #f8f9fa; padding: 1rem; border-radius: 4px; }
        .good-metric { color: #198754; font-weight: bold; }
        .warning-metric { color: #ffc107; font-weight: bold; }
        .bad-metric { color: #dc3545; font-weight: bold; }
        .metric-label { color: #6c757d; }
        .metric-value { font-size: 1.1em; }
    </style>
</head>
<body>
    <div class="container mt-4">
        <h1>Performance Test Report</h1>
        <div class="alert alert-info">
            Test Run: ${new Date(report.timestamp).toLocaleString()}
            <br>
            Build ID: ${report.buildId || 'N/A'}
        </div>
        
        <div class="row">
            <div class="col-md-6">
                <div class="card mb-4">
                    <div class="card-header">
                        <h5 class="card-title">Core Web Vitals</h5>
                    </div>
                    <div class="card-body">
                        <div class="list-group">
                            <div class="list-group-item">
                                <div class="d-flex w-100 justify-content-between">
                                    <div>
                                        <h6 class="mb-1">Largest Contentful Paint (LCP)</h6>
                                        <small class="metric-label">Time until the largest content element is visible</small>
                                    </div>
                                    <span class="${getMetricClass(summary.LCP, thresholds.LCP)} metric-value">
                                        ${formatMetric(summary.LCP)}
                                    </span>
                                </div>
                            </div>
                            <div class="list-group-item">
                                <div class="d-flex w-100 justify-content-between">
                                    <div>
                                        <h6 class="mb-1">First Input Delay (FID)</h6>
                                        <small class="metric-label">Time from first interaction to response</small>
                                    </div>
                                    <span class="${getMetricClass(summary.FID, thresholds.FID)} metric-value">
                                        ${formatMetric(summary.FID)}
                                    </span>
                                </div>
                            </div>
                            <div class="list-group-item">
                                <div class="d-flex w-100 justify-content-between">
                                    <div>
                                        <h6 class="mb-1">Cumulative Layout Shift (CLS)</h6>
                                        <small class="metric-label">Measures visual stability</small>
                                    </div>
                                    <span class="${getMetricClass(summary.CLS, thresholds.CLS)} metric-value">
                                        ${summary.CLS?.toFixed(3) || 'N/A'}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="col-md-6">
                <div class="card mb-4">
                    <div class="card-header">
                        <h5 class="card-title">Page Load Metrics</h5>
                    </div>
                    <div class="card-body">
                        <div class="list-group">
                            <div class="list-group-item">
                                <div class="d-flex w-100 justify-content-between">
                                    <div>
                                        <h6 class="mb-1">Time to First Byte (TTFB)</h6>
                                        <small class="metric-label">Initial server response time</small>
                                    </div>
                                    <span class="metric-value">${formatMetric(summary.TTFB)}</span>
                                </div>
                            </div>
                            <div class="list-group-item">
                                <div class="d-flex w-100 justify-content-between">
                                    <div>
                                        <h6 class="mb-1">DOM Interactive</h6>
                                        <small class="metric-label">Time until DOM is ready</small>
                                    </div>
                                    <span class="metric-value">${formatMetric(summary.domInteractive)}</span>
                                </div>
                            </div>
                            <div class="list-group-item">
                                <div class="d-flex w-100 justify-content-between">
                                    <div>
                                        <h6 class="mb-1">Load Complete</h6>
                                        <small class="metric-label">Total page load time</small>
                                    </div>
                                    <span class="metric-value">${formatMetric(summary.loadComplete)}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="row">
            <div class="col-md-6">
                <div class="card mb-4">
                    <div class="card-header">
                        <h5 class="card-title">Resource Metrics</h5>
                    </div>
                    <div class="card-body">
                        <div class="list-group">
                            <div class="list-group-item">
                                <div class="d-flex w-100 justify-content-between">
                                    <div>
                                        <h6 class="mb-1">Total Resources</h6>
                                        <small class="metric-label">Number of loaded resources</small>
                                    </div>
                                    <span class="metric-value">${resourceMetrics.totalResources || 'N/A'}</span>
                                </div>
                            </div>
                            <div class="list-group-item">
                                <div class="d-flex w-100 justify-content-between">
                                    <div>
                                        <h6 class="mb-1">Total Transfer Size</h6>
                                        <small class="metric-label">Total bytes transferred</small>
                                    </div>
                                    <span class="metric-value">${resourceMetrics.totalTransferSize ? 
                                        `${(resourceMetrics.totalTransferSize / 1024).toFixed(2)} KB` : 'N/A'}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="col-md-6">
                <div class="card mb-4">
                    <div class="card-header">
                        <h5 class="card-title">Environment Info</h5>
                    </div>
                    <div class="card-body">
                        <div class="list-group">
                            <div class="list-group-item">
                                <div class="d-flex w-100 justify-content-between">
                                    <h6 class="mb-1">Browser</h6>
                                    <span>${report.environment?.browser || 'N/A'}</span>
                                </div>
                            </div>
                            <div class="list-group-item">
                                <div class="d-flex w-100 justify-content-between">
                                    <h6 class="mb-1">Platform</h6>
                                    <span>${report.environment?.platform || 'N/A'}</span>
                                </div>
                            </div>
                            <div class="list-group-item">
                                <div class="d-flex w-100 justify-content-between">
                                    <h6 class="mb-1">Node Version</h6>
                                    <span>${report.environment?.nodeVersion || 'N/A'}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="row mt-4">
            <div class="col-12">
                <div class="card mb-4">
                    <div class="card-header">
                        <h5 class="card-title">API Performance</h5>
                    </div>
                    <div class="card-body">
                        <div class="table-responsive">
                            <table class="table">
                                <thead>
                                    <tr>
                                        <th>Endpoint</th>
                                        <th>Total Time</th>
                                        <th>DNS</th>
                                        <th>TCP</th>
                                        <th>Request</th>
                                        <th>Response</th>
                                        <th>Processing</th>
                                        <th>Success Rate</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${Object.entries(report.metrics.summary.apiMetrics || {}).map(([endpoint, metrics]) => `
                                        <tr>
                                            <td>${endpoint}</td>
                                            <td>${formatMetric(metrics.timing.total)}</td>
                                            <td>${formatMetric(metrics.timing.dns)}</td>
                                            <td>${formatMetric(metrics.timing.tcp)}</td>
                                            <td>${formatMetric(metrics.timing.request)}</td>
                                            <td>${formatMetric(metrics.timing.response)}</td>
                                            <td>${formatMetric(metrics.timing.processing)}</td>
                                            <td>${metrics.successRate.toFixed(1)}%</td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>`;
}

async function runPerformanceTests() {
    const browser = await puppeteer.launch({
        executablePath: '/usr/bin/chromium',
        args: [
            '--no-sandbox',
            '--disable-setuid-sandbox',
            '--disable-dev-shm-usage',
            '--disable-gpu',
            '--disable-software-rasterizer',
            '--disable-extensions'
        ],
        headless: "new"
    });
    
    try {
        const page = await browser.newPage();
        await page.setViewport({ width: 1280, height: 800 });
        await page.setCacheEnabled(false);
        await page.coverage.startJSCoverage();
        
        const scripts = await loadTestScripts();
        
        console.log('Navigating to application...');
        await page.goto('http://frontend:80', {
            waitUntil: ['networkidle0', 'domcontentloaded'],
            timeout: 60000
        });

        console.log('Injecting test scripts...');
        await page.evaluate(scripts.performanceTestsJs);
        await page.evaluate(scripts.reportServiceJs);
        await page.evaluate(scripts.reportAnalyzerJs);
        
        console.log('Running performance tests...');
        const metrics = await page.evaluate(() => {
            // Add debug logging inside the browser context
            const results = PerformanceTestRunner.runPerformanceTests();
            console.log('Performance test results:', JSON.stringify(results, null, 2));
            return results;
        });
        
        // Log the metrics after collection
        console.log('Collected metrics:', JSON.stringify(metrics, null, 2));

        const jsCoverage = await page.coverage.stopJSCoverage();
        
        const fullReport = {
            metrics: {
                summary: metrics // Ensure metrics are properly structured
            },
            coverage: jsCoverage,
            timestamp: new Date().toISOString(),
            buildId: process.env.DOCKER_BUILD_ID,
            environment: {
                nodeVersion: process.version,
                platform: process.platform,
                dockerEnvironment: true,
                browser: 'Chromium'
            }
        };

        // Log the full report before generating HTML
        console.log('Full report:', JSON.stringify(fullReport, null, 2));

        // Generate and save HTML report
        const htmlContent = await generateHtmlReport(fullReport);
        const reportPath = process.env.REPORT_DIR + '/performance-report.html';
        fs.writeFileSync(reportPath, htmlContent);
        console.log(`HTML report saved to ${reportPath}`);

    } catch (error) {
        console.error('Error during performance tests:', error);
        process.exit(1);
    } finally {
        await browser.close();
    }
}

runPerformanceTests().catch(error => {
    console.error('Fatal error:', error);
    process.exit(1);
});
JAVASCRIPT

# Run the performance test script
echo "Running performance tests..."
REPORT_DIR="$REPORT_DIR" node ./performance-test.js

# Clean up
rm ./performance-test.js

# Create symlink to latest
cd /app/performance-reports
rm -f latest
ln -s "runs/${TIMESTAMP}" latest

echo "Performance tests completed. Reports available at: ${REPORT_DIR}" 