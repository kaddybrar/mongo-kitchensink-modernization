class PerformanceReportService {
    static async saveReport(report) {
        try {
            const response = await fetch('/api/v1/performance/reports', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    timestamp: new Date().toISOString(),
                    environment: process.env.NODE_ENV || 'development',
                    dockerBuild: process.env.DOCKER_BUILD_ID,
                    metrics: report
                })
            });

            if (!response.ok) {
                throw new Error(`Failed to save report: ${response.statusText}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error saving performance report:', error);
            // Fallback to local storage if API fails
            this.saveReportLocally(report);
        }
    }

    static saveReportLocally(report) {
        const reportData = {
            timestamp: new Date().toISOString(),
            metrics: report
        };

        // Save to localStorage as backup
        const existingReports = JSON.parse(localStorage.getItem('performanceReports') || '[]');
        existingReports.push(reportData);
        localStorage.setItem('performanceReports', JSON.stringify(existingReports));
    }

    static async exportReportToFile(report) {
        const reportData = JSON.stringify(report, null, 2);
        const blob = new Blob([reportData], { type: 'application/json' });
        
        try {
            // If running in Node.js environment (during Docker build)
            if (typeof window === 'undefined') {
                const fs = require('fs');
                const path = require('path');
                
                const reportsDir = path.join(__dirname, '../../../performance-reports');
                if (!fs.existsSync(reportsDir)) {
                    fs.mkdirSync(reportsDir, { recursive: true });
                }

                const fileName = `performance-report-${new Date().toISOString().replace(/[:.]/g, '-')}.json`;
                const filePath = path.join(reportsDir, fileName);
                
                fs.writeFileSync(filePath, reportData);
                console.log(`Performance report saved to: ${filePath}`);
            } else {
                // If running in browser
                const url = URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = url;
                link.download = `performance-report-${new Date().toISOString().replace(/[:.]/g, '-')}.json`;
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
                URL.revokeObjectURL(url);
            }
        } catch (error) {
            console.error('Error exporting report:', error);
        }
    }

    static generateHtmlReport(report) {
        const htmlContent = `
<!DOCTYPE html>
<html>
<head>
    <title>Performance Test Report - ${new Date(report.timestamp).toLocaleString()}</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        .metric-card { margin-bottom: 1rem; }
        pre { background: #f8f9fa; padding: 1rem; border-radius: 4px; }
        .good-metric { color: #198754; }
        .warning-metric { color: #ffc107; }
        .bad-metric { color: #dc3545; }
    </style>
</head>
<body>
    <div class="container mt-4">
        <h1>Performance Test Report</h1>
        <div class="row">
            <div class="col-md-12">
                <div class="card mb-4">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="card-title mb-0">Test Results</h5>
                        <small class="text-muted">${new Date(report.timestamp).toLocaleString()}</small>
                    </div>
                    <div class="card-body">
                        <div class="row">
                            <div class="col-md-6">
                                <h6>Core Web Vitals</h6>
                                <div class="list-group">
                                    <div class="list-group-item">
                                        <div class="d-flex w-100 justify-content-between">
                                            <h6 class="mb-1">LCP (Largest Contentful Paint)</h6>
                                            <span>${report.metrics.summary.LCP.toFixed(2)}ms</span>
                                        </div>
                                    </div>
                                    <div class="list-group-item">
                                        <div class="d-flex w-100 justify-content-between">
                                            <h6 class="mb-1">FID (First Input Delay)</h6>
                                            <span>${report.metrics.summary.FID.toFixed(2)}ms</span>
                                        </div>
                                    </div>
                                    <div class="list-group-item">
                                        <div class="d-flex w-100 justify-content-between">
                                            <h6 class="mb-1">CLS (Cumulative Layout Shift)</h6>
                                            <span>${report.metrics.summary.CLS?.toFixed(3) || 'N/A'}</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <h6>Page Load Metrics</h6>
                                <div class="list-group">
                                    <div class="list-group-item">
                                        <div class="d-flex w-100 justify-content-between">
                                            <h6 class="mb-1">Time to First Byte</h6>
                                            <span>${report.metrics.summary.TTFB.toFixed(2)}ms</span>
                                        </div>
                                    </div>
                                    <div class="list-group-item">
                                        <div class="d-flex w-100 justify-content-between">
                                            <h6 class="mb-1">DOM Interactive</h6>
                                            <span>${report.metrics.summary.domInteractive.toFixed(2)}ms</span>
                                        </div>
                                    </div>
                                    <div class="list-group-item">
                                        <div class="d-flex w-100 justify-content-between">
                                            <h6 class="mb-1">Load Complete</h6>
                                            <span>${report.metrics.summary.loadComplete.toFixed(2)}ms</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        
                        <div class="row mt-4">
                            <div class="col-md-6">
                                <h6>Resource Metrics</h6>
                                <div class="list-group">
                                    <div class="list-group-item">
                                        <div class="d-flex w-100 justify-content-between">
                                            <h6 class="mb-1">Total Resources</h6>
                                            <span>${report.metrics.summary.resourceMetrics.totalResources}</span>
                                        </div>
                                    </div>
                                    <div class="list-group-item">
                                        <div class="d-flex w-100 justify-content-between">
                                            <h6 class="mb-1">Total Transfer Size</h6>
                                            <span>${(report.metrics.summary.resourceMetrics.totalTransferSize / 1024).toFixed(2)} KB</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <h6>Environment Info</h6>
                                <div class="list-group">
                                    <div class="list-group-item">
                                        <div class="d-flex w-100 justify-content-between">
                                            <h6 class="mb-1">Node Version</h6>
                                            <span>${report.environment.nodeVersion}</span>
                                        </div>
                                    </div>
                                    <div class="list-group-item">
                                        <div class="d-flex w-100 justify-content-between">
                                            <h6 class="mb-1">Browser</h6>
                                            <span>${report.environment.browser}</span>
                                        </div>
                                    </div>
                                    <div class="list-group-item">
                                        <div class="d-flex w-100 justify-content-between">
                                            <h6 class="mb-1">Platform</h6>
                                            <span>${report.environment.platform}</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="row mt-4">
                            <div class="col-12">
                                <h6>Raw Report Data</h6>
                                <pre style="max-height: 400px; overflow-y: auto;">${JSON.stringify(report, null, 2)}</pre>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>`;

        return htmlContent;
    }
} 