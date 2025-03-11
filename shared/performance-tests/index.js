import puppeteer from 'puppeteer';
import lighthouse from 'lighthouse';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

class PerformanceTestRunner {
    constructor(config) {
        this.config = {
            url: config.url,
            reportDir: config.reportDir,
            buildId: config.buildId,
            appName: config.appName
        };
    }

    async runAllTests() {
        console.log(`Starting performance tests for ${this.config.appName}`);
        
        const metrics = {
            timestamp: new Date().toISOString(),
            buildId: this.config.buildId,
            appName: this.config.appName,
            lighthouse: await this.runLighthouseTests(),
            webVitals: await this.measureWebVitals(),
            resourceMetrics: await this.measureResourceMetrics(),
            apiPerformance: await this.measureApiPerformance()
        };

        await this.generateReport(metrics);
        return metrics;
    }

    async runLighthouseTests() {
        const browser = await puppeteer.launch({
            args: ['--no-sandbox', '--disable-gpu']
        });
        const results = await lighthouse(this.config.url, {
            port: (new URL(browser.wsEndpoint())).port,
            output: 'json',
            logLevel: 'info',
            onlyCategories: ['performance', 'accessibility', 'best-practices', 'seo']
        });

        await browser.close();
        return results.lhr;
    }

    async measureWebVitals() {
        const browser = await puppeteer.launch({
            args: ['--no-sandbox', '--disable-gpu']
        });
        const page = await browser.newPage();
        
        try {
            // Navigate to the page first
            console.log('Navigating to URL:', this.config.url);
            await page.goto(this.config.url, { 
                waitUntil: ['networkidle0', 'domcontentloaded', 'load'],
                timeout: 30000 
            });

            // Inject web-vitals script and wait for it to load
            await page.addScriptTag({
                url: 'https://unpkg.com/web-vitals@3.1.0/dist/web-vitals.iife.js',
                type: 'text/javascript'
            });

            // Wait for the script to be available
            await page.waitForFunction(() => typeof webVitals !== 'undefined');

            // Set up data collection
            await page.evaluate(() => {
                window.webVitalsData = {};
                window.webVitalsPromises = {};

                ['LCP', 'FID', 'CLS', 'TTFB'].forEach(metric => {
                    window.webVitalsData[metric] = null;
                    window.webVitalsPromises[metric] = new Promise(resolve => {
                        window.webVitalsPromises[`${metric}Resolve`] = resolve;
                    });
                });

                // Start collecting metrics
                webVitals.onLCP(metric => {
                    window.webVitalsData.LCP = metric;
                    window.webVitalsPromises.LCPResolve(metric);
                    console.log('LCP collected:', metric);
                });

                webVitals.onFID(metric => {
                    window.webVitalsData.FID = metric;
                    window.webVitalsPromises.FIDResolve(metric);
                    console.log('FID collected:', metric);
                });

                webVitals.onCLS(metric => {
                    window.webVitalsData.CLS = metric;
                    window.webVitalsPromises.CLSResolve(metric);
                    console.log('CLS collected:', metric);
                });

                webVitals.onTTFB(metric => {
                    window.webVitalsData.TTFB = metric;
                    window.webVitalsPromises.TTFBResolve(metric);
                    console.log('TTFB collected:', metric);
                });
            });

            // Wait for page to be interactive
            await page.waitForSelector('body', { timeout: 10000 });

            // Perform interactions to trigger FID
            for (let i = 0; i < 3; i++) {
                await page.evaluate(() => {
                    // Create and dispatch multiple events
                    ['click', 'mousedown', 'mouseup', 'keydown', 'keyup'].forEach(eventType => {
                        const event = new Event(eventType, { bubbles: true });
                        document.body.dispatchEvent(event);
                    });
                    
                    // Scroll actions for CLS
                    window.scrollTo(0, document.body.scrollHeight / 2);
                    setTimeout(() => window.scrollTo(0, 0), 100);
                });
                await page.waitForTimeout(1000);
            }

            // Wait for metrics to be collected (with timeout)
            const metrics = await page.evaluate(async () => {
                const timeout = ms => new Promise(resolve => setTimeout(resolve, ms));
                const waitForMetric = async (metric, maxWait = 10000) => {
                    const start = Date.now();
                    while (!window.webVitalsData[metric] && Date.now() - start < maxWait) {
                        await timeout(100);
                    }
                    return window.webVitalsData[metric];
                };

                const results = {};
                for (const metric of ['LCP', 'FID', 'CLS', 'TTFB']) {
                    results[metric] = await waitForMetric(metric);
                    console.log(`${metric} final value:`, results[metric]);
                }
                return results;
            });

            console.log('Final Web Vitals:', metrics);
            return metrics;
        } catch (error) {
            console.error('Error measuring Web Vitals:', error);
            return {
                LCP: { value: null },
                FID: { value: null },
                CLS: { value: null },
                TTFB: { value: null }
            };
        } finally {
            await browser.close();
        }
    }

    async measureResourceMetrics() {
        const browser = await puppeteer.launch({
            args: ['--no-sandbox', '--disable-gpu']
        });
        const page = await browser.newPage();
        
        await page.goto(this.config.url, { waitUntil: 'networkidle0' });
        
        const resourceMetrics = await page.evaluate(() => ({
            resources: performance.getEntriesByType('resource').map(entry => ({
                name: entry.name,
                type: entry.initiatorType,
                size: entry.transferSize,
                duration: entry.duration
            })),
            timing: {
                domInteractive: performance.timing.domInteractive - performance.timing.navigationStart,
                domComplete: performance.timing.domComplete - performance.timing.navigationStart,
                loadComplete: performance.timing.loadEventEnd - performance.timing.navigationStart
            }
        }));

        await browser.close();
        return resourceMetrics;
    }

    async measureApiPerformance() {
        const browser = await puppeteer.launch({
            args: ['--no-sandbox', '--disable-gpu']
        });
        const page = await browser.newPage();
        let createdMemberId = null;
        
        try {
            const appPageUrl = process.env.APP_PAGE_URL || 'http://frontend:80';
            console.log('Navigating to application page:', appPageUrl);
            
            try {
                await page.goto(appPageUrl, { 
                    waitUntil: 'networkidle0',
                    timeout: 30000
                });
            } catch (error) {
                console.error('Failed to navigate to application page:', error);
                throw new Error(`Failed to access ${appPageUrl}: ${error.message}`);
            }
            
            await page.setRequestInterception(true);
            const apiMetrics = {};
            const apiBaseUrl = process.env.VITE_API_URL || 'http://backend:8080';

            console.log('Testing API endpoints with base URL:', apiBaseUrl);

            // Define API endpoints to test
            const endpoints = [
                { 
                    name: 'Health Check', 
                    method: 'GET', 
                    path: '/api/v1/health',
                    skipBody: true
                },
                { 
                    name: 'Create Member', 
                    method: 'POST', 
                    path: '/api/v1/members',
                    body: {
                        name: 'Test User',
                        email: `test${Date.now()}@example.com`,
                        phoneNumber: '+1234567890'
                    }
                },
                { 
                    name: 'Get Members', 
                    method: 'GET', 
                    path: '/api/v1/members',
                    skipBody: true
                },
                { 
                    name: 'Search Members', 
                    method: 'GET', 
                    path: '/api/v1/members/search?name=Test',
                    skipBody: true
                }
            ];

            // Dynamic endpoints that depend on created member (excluding delete)
            const memberSpecificEndpoints = [
                { 
                    name: 'Get Member by ID', 
                    method: 'GET',
                    getPath: (id) => `/api/v1/members/${id}`,
                    skipBody: true
                },
                { 
                    name: 'Update Member', 
                    method: 'PUT',
                    getPath: (id) => `/api/v1/members/${id}`,
                    getBody: (id) => ({
                        name: 'Updated Test User',
                        email: `updated${Date.now()}@example.com`,
                        phoneNumber: '+9876543210'
                    })
                }
            ];

            // Add request monitoring
            page.on('request', request => {
                if (request.resourceType() === 'xhr' || request.resourceType() === 'fetch') {
                    const startTime = Date.now();
                    const url = request.url();
                    console.log('Intercepted API request:', url);
                    if (url.startsWith(apiBaseUrl)) {
                        apiMetrics[url] = { 
                            startTime,
                            method: request.method(),
                            endpoint: url.replace(apiBaseUrl, '')
                        };
                    }
                }
                request.continue();
            });

            page.on('requestfailed', request => {
                console.error('Request failed:', request.url(), request.failure().errorText);
            });

            page.on('response', async response => {
                const url = response.url();
                console.log('Response received:', url, response.status());
                
                // Store the member ID from create response
                if (url.endsWith('/api/v1/members') && response.request().method() === 'POST' && response.status() === 200) {
                    try {
                        const responseData = await response.json();
                        createdMemberId = responseData.id;
                        console.log('Created member ID:', createdMemberId);
                    } catch (error) {
                        console.error('Failed to parse create member response:', error);
                    }
                }
            });

            page.on('console', msg => {
                console.log('Browser console:', msg.text());
            });

            // Test each endpoint
            const endpointMetrics = {};
            
            // Test initial endpoints
            for (const endpoint of endpoints) {
                try {
                    console.log(`Testing endpoint: ${endpoint.method} ${endpoint.path}`);
                    
                    const response = await page.evaluate(async (apiUrl, endpoint) => {
                        console.log(`Browser: Testing ${endpoint.method} ${endpoint.path}`);
                        const startTime = performance.now();
                        try {
                            const response = await fetch(`${apiUrl}${endpoint.path}`, {
                                method: endpoint.method,
                                headers: { 
                                    'Content-Type': 'application/json',
                                    'Accept': 'application/json'
                                },
                                body: endpoint.skipBody ? undefined : JSON.stringify(endpoint.body),
                                mode: 'cors'
                            });
                            
                            const endTime = performance.now();
                            console.log(`Browser: Response status for ${endpoint.path}:`, response.status);
                            
                            if (response.status === 200) {
                                const data = await response.json().catch(() => null);
                                return {
                                    duration: endTime - startTime,
                                    status: response.status,
                                    ok: response.ok,
                                    statusText: response.statusText,
                                    data
                                };
                            }
                            
                            return {
                                duration: endTime - startTime,
                                status: response.status,
                                ok: response.ok,
                                statusText: response.statusText
                            };
                        } catch (error) {
                            console.error(`Browser: Fetch error for ${endpoint.path}:`, error.message);
                            throw error;
                        }
                    }, apiBaseUrl, endpoint);

                    endpointMetrics[endpoint.name] = {
                        path: endpoint.path,
                        method: endpoint.method,
                        duration: response.duration,
                        status: response.status,
                        statusText: response.statusText,
                        success: response.ok
                    };

                    // Store created member ID if available
                    if (endpoint.name === 'Create Member' && response.data && response.data.id) {
                        createdMemberId = response.data.id;
                        console.log('Stored created member ID:', createdMemberId);
                    }

                    console.log(`Endpoint result for ${endpoint.name}:`, endpointMetrics[endpoint.name]);

                } catch (error) {
                    console.error(`Error testing endpoint ${endpoint.name}:`, error);
                    endpointMetrics[endpoint.name] = {
                        path: endpoint.path,
                        method: endpoint.method,
                        error: error.message,
                        success: false
                    };
                }
                
                await new Promise(resolve => setTimeout(resolve, 500));
            }

            // Test member-specific endpoints if we have a member ID
            if (createdMemberId) {
                for (const endpoint of memberSpecificEndpoints) {
                    try {
                        const path = endpoint.getPath(createdMemberId);
                        const body = endpoint.getBody ? endpoint.getBody(createdMemberId) : undefined;
                        
                        console.log(`Testing endpoint: ${endpoint.method} ${path}`);
                        
                        const response = await page.evaluate(async (apiUrl, method, path, body, skipBody) => {
                            console.log(`Browser: Testing ${method} ${path}`);
                            const startTime = performance.now();
                            try {
                                const response = await fetch(`${apiUrl}${path}`, {
                                    method: method,
                                    headers: { 
                                        'Content-Type': 'application/json',
                                        'Accept': 'application/json'
                                    },
                                    body: skipBody ? undefined : JSON.stringify(body),
                                    mode: 'cors'
                                });
                                
                                const endTime = performance.now();
                                console.log(`Browser: Response status for ${path}:`, response.status);
                                
                                return {
                                    duration: endTime - startTime,
                                    status: response.status,
                                    ok: response.ok,
                                    statusText: response.statusText
                                };
                            } catch (error) {
                                console.error(`Browser: Fetch error for ${path}:`, error.message);
                                throw error;
                            }
                        }, apiBaseUrl, endpoint.method, path, body, endpoint.skipBody);

                        endpointMetrics[endpoint.name] = {
                            path: path,
                            method: endpoint.method,
                            duration: response.duration,
                            status: response.status,
                            statusText: response.statusText,
                            success: response.ok
                        };

                        console.log(`Endpoint result for ${endpoint.name}:`, endpointMetrics[endpoint.name]);

                    } catch (error) {
                        console.error(`Error testing endpoint ${endpoint.name}:`, error);
                        endpointMetrics[endpoint.name] = {
                            path: endpoint.getPath(createdMemberId),
                            method: endpoint.method,
                            error: error.message,
                            success: false
                        };
                    }
                    
                    await new Promise(resolve => setTimeout(resolve, 500));
                }
            } else {
                console.warn('No member ID available for member-specific endpoints');
            }

            return {
                endpoints: endpointMetrics,
                generalApiMetrics: apiMetrics
            };
        } finally {
            // Cleanup: Delete the test member if it was created
            if (createdMemberId) {
                console.log('Cleaning up: Deleting test member with ID:', createdMemberId);
                try {
                    const apiBaseUrl = process.env.VITE_API_URL || 'http://backend:8080';
                    const deletePath = `/api/v1/members/${createdMemberId}`;
                    
                    await page.evaluate(async (apiUrl, path) => {
                        console.log(`Browser: Cleaning up - Deleting member at ${path}`);
                        const response = await fetch(`${apiUrl}${path}`, {
                            method: 'DELETE',
                            headers: { 
                                'Accept': 'application/json'
                            },
                            mode: 'cors'
                        });
                        console.log(`Cleanup response status:`, response.status);
                    }, apiBaseUrl, deletePath);
                    
                    console.log('Successfully deleted test member');
                } catch (error) {
                    console.error('Failed to delete test member:', error);
                }
            }
            
            await browser.close();
        }
    }

    async generateReport(metrics) {
        const reportDir = process.env.REPORT_DIR;
        const subDir = process.env.REPORT_SUBDIR || '';
        const fullReportDir = path.join(reportDir, subDir);
        
        // Create the report directory if it doesn't exist
        await fs.promises.mkdir(fullReportDir, { recursive: true });
        
        // Generate HTML report
        const htmlPath = path.join(fullReportDir, 'performance-report.html');
        const htmlContent = this.generateHtmlReport(metrics);
        await fs.promises.writeFile(htmlPath, htmlContent);
        
        console.log(`Report generated at: ${htmlPath}`);
    }

    generateMainIndex(runDir, timestamp) {
        const indexHtml = `<!DOCTYPE html>
<html>
<head>
    <title>Performance Test Results - ${timestamp}</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { font-family: Arial, sans-serif; }
        .timestamp { color: #666; font-size: 0.9rem; }
        .report-link { 
            display: block; 
            margin: 10px 0;
            padding: 15px;
            background: #f8f9fa;
            text-decoration: none;
            color: #333;
            border-radius: 8px;
            border: 1px solid #dee2e6;
            transition: all 0.3s ease;
        }
        .report-link:hover { 
            background: #e9ecef;
            transform: translateY(-2px);
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
        }
        .app-name {
            font-size: 1.2rem;
            font-weight: 500;
            margin-bottom: 5px;
        }
        .description {
            color: #666;
            font-size: 0.9rem;
        }
    </style>
</head>
<body>
    <div class="container mt-4 mb-5">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1 class="mb-0">Performance Test Results</h1>
            <span class="timestamp">Test Run: ${timestamp}</span>
        </div>
        
        <div class="row">
            <div class="col-md-4 mb-3">
                <a class="report-link" href="frontend.html">
                    <div class="app-name">Original Frontend</div>
                    <div class="description">
                        View detailed performance metrics including:
                        <ul class="mb-0 mt-2">
                            <li>Core Web Vitals</li>
                            <li>Lighthouse Scores</li>
                            <li>Resource Usage</li>
                            <li>API Response Times</li>
                        </ul>
                    </div>
                </a>
            </div>
            <div class="col-md-4 mb-3">
                <a class="report-link" href="frontend-react.html">
                    <div class="app-name">React Frontend</div>
                    <div class="description">
                        View detailed performance metrics including:
                        <ul class="mb-0 mt-2">
                            <li>Core Web Vitals</li>
                            <li>Lighthouse Scores</li>
                            <li>Resource Usage</li>
                            <li>API Response Times</li>
                        </ul>
                    </div>
                </a>
            </div>
            <div class="col-md-4 mb-3">
                <a class="report-link" href="backend.html">
                    <div class="app-name">Backend</div>
                    <div class="description">
                        View detailed performance metrics including:
                        <ul class="mb-0 mt-2">
                            <li>API Response Times</li>
                            <li>Database Performance</li>
                            <li>Resource Usage</li>
                            <li>Error Rates</li>
                        </ul>
                    </div>
                </a>
            </div>
        </div>
    </div>
</body>
</html>`;

        fs.writeFileSync(path.join(runDir, 'index.html'), indexHtml);
    }

    generateHtmlReport(metrics) {
        return `<!DOCTYPE html>
<html>
<head>
    <title>Performance Test Report - ${metrics.appName}</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        .metric-card { 
            margin-bottom: 1rem;
            transition: all 0.3s ease;
        }
        .metric-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
        }
        .good-metric { color: #198754; }
        .warning-metric { color: #ffc107; }
        .bad-metric { color: #dc3545; }
        .endpoint-metric {
            border: 1px solid #dee2e6;
            padding: 15px;
            margin-bottom: 15px;
            border-radius: 8px;
            transition: all 0.3s ease;
        }
        .endpoint-metric:hover {
            transform: translateY(-2px);
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
        }
        .endpoint-metric.success { border-left: 4px solid #198754; }
        .endpoint-metric.error { border-left: 4px solid #dc3545; }
        .metric-value {
            font-size: 1.5rem;
            font-weight: 500;
        }
        .metric-label {
            color: #666;
            font-size: 0.9rem;
        }
    </style>
</head>
<body>
    <div class="container mt-4 mb-5">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1 class="mb-0">Performance Report - ${metrics.appName}</h1>
            <a href="../index.html" class="btn btn-outline-primary">Back to Main Index</a>
        </div>

        <div class="alert alert-info mb-4">
            <div class="row">
                <div class="col-md-6">
                    <strong>Test Run:</strong> ${metrics.timestamp}
                </div>
                <div class="col-md-6">
                    <strong>Build ID:</strong> ${metrics.buildId}
                </div>
            </div>
        </div>

        ${this.generateMetricsContent(metrics)}
    </div>
</body>
</html>`;
    }

    generateMetricsContent(metrics) {
        return `
        <!-- Core Web Vitals -->
        <div class="card metric-card mb-4">
            <div class="card-header bg-primary text-white">
                <h5 class="mb-0">Core Web Vitals</h5>
            </div>
            <div class="card-body">
                <div class="row">
                    <div class="col-md-3 mb-3">
                        <div class="metric-label">LCP (Largest Contentful Paint)</div>
                        <div class="metric-value ${metrics.webVitals?.LCP?.value < 2500 ? 'good-metric' : metrics.webVitals?.LCP?.value < 4000 ? 'warning-metric' : 'bad-metric'}">
                            ${metrics.webVitals?.LCP?.value?.toFixed(2) || 'N/A'} <small>ms</small>
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <div class="metric-label">FID (First Input Delay)</div>
                        <div class="metric-value ${metrics.webVitals?.FID?.value < 100 ? 'good-metric' : metrics.webVitals?.FID?.value < 300 ? 'warning-metric' : 'bad-metric'}">
                            ${metrics.webVitals?.FID?.value?.toFixed(2) || 'N/A'} <small>ms</small>
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <div class="metric-label">CLS (Cumulative Layout Shift)</div>
                        <div class="metric-value ${metrics.webVitals?.CLS?.value < 0.1 ? 'good-metric' : metrics.webVitals?.CLS?.value < 0.25 ? 'warning-metric' : 'bad-metric'}">
                            ${metrics.webVitals?.CLS?.value?.toFixed(4) || 'N/A'}
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <div class="metric-label">TTFB (Time to First Byte)</div>
                        <div class="metric-value ${metrics.webVitals?.TTFB?.value < 600 ? 'good-metric' : metrics.webVitals?.TTFB?.value < 1000 ? 'warning-metric' : 'bad-metric'}">
                            ${metrics.webVitals?.TTFB?.value?.toFixed(2) || 'N/A'} <small>ms</small>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Lighthouse Scores -->
        <div class="card metric-card mb-4">
            <div class="card-header bg-success text-white">
                <h5 class="mb-0">Lighthouse Scores</h5>
            </div>
            <div class="card-body">
                <div class="row">
                    <div class="col-md-3 mb-3">
                        <div class="metric-label">Performance</div>
                        <div class="metric-value ${metrics.lighthouse?.categories?.performance?.score > 0.89 ? 'good-metric' : metrics.lighthouse?.categories?.performance?.score > 0.49 ? 'warning-metric' : 'bad-metric'}">
                            ${(metrics.lighthouse?.categories?.performance?.score * 100).toFixed(0) || 'N/A'}
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <div class="metric-label">Accessibility</div>
                        <div class="metric-value ${metrics.lighthouse?.categories?.accessibility?.score > 0.89 ? 'good-metric' : metrics.lighthouse?.categories?.accessibility?.score > 0.49 ? 'warning-metric' : 'bad-metric'}">
                            ${(metrics.lighthouse?.categories?.accessibility?.score * 100).toFixed(0) || 'N/A'}
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <div class="metric-label">Best Practices</div>
                        <div class="metric-value ${metrics.lighthouse?.categories?.['best-practices']?.score > 0.89 ? 'good-metric' : metrics.lighthouse?.categories?.['best-practices']?.score > 0.49 ? 'warning-metric' : 'bad-metric'}">
                            ${(metrics.lighthouse?.categories?.['best-practices']?.score * 100).toFixed(0) || 'N/A'}
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <div class="metric-label">SEO</div>
                        <div class="metric-value ${metrics.lighthouse?.categories?.seo?.score > 0.89 ? 'good-metric' : metrics.lighthouse?.categories?.seo?.score > 0.49 ? 'warning-metric' : 'bad-metric'}">
                            ${(metrics.lighthouse?.categories?.seo?.score * 100).toFixed(0) || 'N/A'}
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Resource Metrics -->
        <div class="card metric-card mb-4">
            <div class="card-header bg-info text-white">
                <h5 class="mb-0">Resource Metrics</h5>
            </div>
            <div class="card-body">
                <div class="row">
                    <div class="col-md-3 mb-3">
                        <div class="metric-label">Total Resources</div>
                        <div class="metric-value">
                            ${metrics.resourceMetrics?.resources?.length || 0}
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <div class="metric-label">Total Transfer Size</div>
                        <div class="metric-value">
                            ${((metrics.resourceMetrics?.resources || []).reduce((acc, r) => acc + (r.size || 0), 0) / 1024).toFixed(2)} <small>KB</small>
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <div class="metric-label">DOM Interactive</div>
                        <div class="metric-value">
                            ${metrics.resourceMetrics?.timing?.domInteractive || 'N/A'} <small>ms</small>
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <div class="metric-label">Load Complete</div>
                        <div class="metric-value">
                            ${metrics.resourceMetrics?.timing?.loadComplete || 'N/A'} <small>ms</small>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- API Performance -->
        <div class="card metric-card mb-4">
            <div class="card-header bg-warning">
                <h5 class="mb-0">API Performance Metrics</h5>
            </div>
            <div class="card-body">
                ${Object.entries(metrics.apiPerformance.endpoints || {}).map(([name, data]) => `
                    <div class="endpoint-metric ${data.success ? 'success' : 'error'}">
                        <div class="row">
                            <div class="col-md-4">
                                <h6 class="mb-2">${name}</h6>
                                <div class="metric-label">Path: ${data.path}</div>
                                <div class="metric-label">Method: ${data.method}</div>
                            </div>
                            <div class="col-md-4">
                                <div class="metric-label">Duration</div>
                                <div class="metric-value ${data.duration < 300 ? 'good-metric' : data.duration < 1000 ? 'warning-metric' : 'bad-metric'}">
                                    ${data.duration?.toFixed(2) || 'N/A'} <small>ms</small>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="metric-label">Status</div>
                                <div class="metric-value ${data.success ? 'good-metric' : 'bad-metric'}">
                                    ${data.status || 'N/A'} ${data.success ? '✅' : '❌'}
                                </div>
                                ${data.error ? `<div class="text-danger mt-2">Error: ${data.error}</div>` : ''}
                            </div>
                        </div>
                    </div>
                `).join('')}
            </div>
        </div>`;
    }
}

export default PerformanceTestRunner; 