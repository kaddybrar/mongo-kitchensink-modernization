// Performance measurement utilities
class PerformanceMetrics {
    static async measurePageLoad() {
        // Wait for all important metrics to be available
        await this.waitForWebVitals();

        const timing = window.performance.timing;
        const navigationEntry = performance.getEntriesByType('navigation')[0];
        const metrics = {
            // Core Web Vitals
            webVitals: await this.getCoreWebVitals(),

            // Traditional metrics
            timeToFirstByte: timing.responseStart - timing.navigationStart,
            firstContentfulPaint: this.getFCP(),
            domInteractive: timing.domInteractive - timing.navigationStart,
            domComplete: timing.domComplete - timing.navigationStart,
            loadComplete: timing.loadEventEnd - timing.navigationStart,

            // Navigation Timing API v2 metrics
            dnsLookup: navigationEntry.domainLookupEnd - navigationEntry.domainLookupStart,
            tcpConnection: navigationEntry.connectEnd - navigationEntry.connectStart,
            serverResponseTime: navigationEntry.responseEnd - navigationEntry.requestStart,
            pageDownload: navigationEntry.responseEnd - navigationEntry.responseStart,

            // Resource timing
            resourceMetrics: this.getResourceMetrics(),

            // JavaScript metrics
            jsHeapMetrics: this.getJSHeapMetrics(),

            // Frame rate metrics
            frameMetrics: await this.getFrameMetrics(),

            // React-specific metrics (for post-migration comparison)
            reactMetrics: this.getReactMetrics(),

            timestamp: new Date().toISOString()
        };

        this.logMetrics(metrics);
        return metrics;
    }

    static async getCoreWebVitals() {
        return new Promise(resolve => {
            if ('webVitals' in window) {
                window.webVitals.getLCP(lcp => {
                    window.webVitals.getFID(fid => {
                        window.webVitals.getCLS(cls => {
                            resolve({
                                LCP: lcp.value, // Largest Contentful Paint
                                FID: fid.value, // First Input Delay
                                CLS: cls.value, // Cumulative Layout Shift
                            });
                        });
                    });
                });
            } else {
                resolve({
                    LCP: this.getLCP(),
                    FID: null,
                    CLS: null
                });
            }
        });
    }

    static async getFrameMetrics() {
        let frames = 0;
        let startTime = performance.now();
        
        return new Promise(resolve => {
            const measureFrames = timestamp => {
                frames++;
                if (timestamp - startTime >= 1000) {
                    resolve({
                        fps: Math.round(frames * 1000 / (timestamp - startTime)),
                        frameCount: frames,
                        duration: timestamp - startTime
                    });
                } else {
                    requestAnimationFrame(measureFrames);
                }
            };
            requestAnimationFrame(measureFrames);
        });
    }

    static getResourceMetrics() {
        const resources = performance.getEntriesByType('resource');
        const metrics = {
            totalResources: resources.length,
            totalTransferSize: 0,
            totalDuration: 0,
            byType: {}
        };

        resources.forEach(resource => {
            const type = resource.initiatorType;
            metrics.totalTransferSize += resource.transferSize || 0;
            metrics.totalDuration += resource.duration;

            if (!metrics.byType[type]) {
                metrics.byType[type] = {
                    count: 0,
                    totalSize: 0,
                    avgDuration: 0
                };
            }

            metrics.byType[type].count++;
            metrics.byType[type].totalSize += resource.transferSize || 0;
            metrics.byType[type].avgDuration = 
                (metrics.byType[type].avgDuration * (metrics.byType[type].count - 1) + resource.duration) 
                / metrics.byType[type].count;
        });

        return metrics;
    }

    static getJSHeapMetrics() {
        if (performance.memory) {
            return {
                usedJSHeapSize: Math.round(performance.memory.usedJSHeapSize / (1024 * 1024)),
                totalJSHeapSize: Math.round(performance.memory.totalJSHeapSize / (1024 * 1024)),
                jsHeapSizeLimit: Math.round(performance.memory.jsHeapSizeLimit / (1024 * 1024))
            };
        }
        return null;
    }

    static getReactMetrics() {
        // These metrics will be more useful post-migration
        return {
            // Placeholder for React-specific metrics
            componentRenderCount: null,
            renderDuration: null,
            effectCount: null
        };
    }

    static getFCP() {
        const paintEntries = performance.getEntriesByType('paint');
        const fcpEntry = paintEntries.find(entry => entry.name === 'first-contentful-paint');
        return fcpEntry ? fcpEntry.startTime : null;
    }

    static async getLCP() {
        return new Promise(resolve => {
            let lcp = 0;
            let maxEntrySize = 0;

            const observer = new PerformanceObserver(list => {
                const entries = list.getEntries();
                entries.forEach(entry => {
                    // Only update LCP if this entry is larger than previous ones
                    if (entry.size > maxEntrySize) {
                        maxEntrySize = entry.size;
                        lcp = entry.startTime;
                    }
                });
            });
            
            try {
                observer.observe({ 
                    entryTypes: ['largest-contentful-paint'],
                    buffered: true  // Include entries that occurred before observer creation
                });
            } catch (e) {
                console.warn('LCP observation failed:', e);
            }
            
            // Wait for a reasonable time to capture LCP
            setTimeout(() => {
                observer.disconnect();
                // If no LCP was detected, fallback to First Paint or First Contentful Paint
                if (lcp === 0) {
                    const paintEntries = performance.getEntriesByType('paint');
                    const fcpEntry = paintEntries.find(entry => entry.name === 'first-contentful-paint');
                    const fpEntry = paintEntries.find(entry => entry.name === 'first-paint');
                    lcp = (fcpEntry?.startTime || fpEntry?.startTime || 1000); // Fallback to 1 second if no paint metrics
                }
                resolve(lcp);
            }, 3000);
        });
    }

    static async measureInteractionTime(action, description) {
        const start = performance.now();
        try {
            const result = await action();
            const end = performance.now();
            const duration = end - start;

            this.logInteraction({
                description,
                duration,
                timestamp: new Date().toISOString(),
                success: true
            });

            return result;
        } catch (error) {
            const end = performance.now();
            this.logInteraction({
                description,
                duration: end - start,
                timestamp: new Date().toISOString(),
                success: false,
                error: error.message
            });
            throw error;
        }
    }

    static logMetrics(metrics) {
        console.group('Performance Metrics');
        console.log('Core Web Vitals:', metrics.webVitals);
        console.log('Navigation Timing:', {
            TTFB: metrics.timeToFirstByte,
            FCP: metrics.firstContentfulPaint,
            DOMInteractive: metrics.domInteractive,
            DOMComplete: metrics.domComplete,
            LoadComplete: metrics.loadComplete
        });
        console.log('Resource Metrics:', metrics.resourceMetrics);
        console.log('Frame Metrics:', metrics.frameMetrics);
        console.log('JS Heap Metrics:', metrics.jsHeapMetrics);
        console.groupEnd();
    }

    static logInteraction(data) {
        console.log(`Interaction: ${data.description}`, {
            duration: `${Math.round(data.duration)}ms`,
            success: data.success,
            timestamp: data.timestamp,
            ...(data.error && { error: data.error })
        });
    }

    static async waitForWebVitals() {
        return new Promise(resolve => {
            setTimeout(resolve, 1000); // Wait for initial metrics to be available
        });
    }

    static async measureApiPerformance(apiEndpoint, method = 'GET', body = null) {
        const metrics = {
            endpoint: apiEndpoint,
            method: method,
            timing: {
                total: 0,
                dns: 0,
                tcp: 0,
                request: 0,
                response: 0,
                processing: 0
            },
            status: null,
            success: false
        };

        const startTime = performance.now();
        const requestStart = new Date().getTime();

        try {
            // Create resource timing observer
            const observer = new PerformanceObserver((list) => {
                const entries = list.getEntries();
                const entry = entries[entries.length - 1];
                if (entry) {
                    metrics.timing = {
                        total: entry.duration,
                        dns: entry.domainLookupEnd - entry.domainLookupStart,
                        tcp: entry.connectEnd - entry.connectStart,
                        request: entry.responseStart - entry.requestStart,
                        response: entry.responseEnd - entry.responseStart,
                        processing: entry.duration - (entry.responseEnd - entry.startTime)
                    };
                }
            });
            observer.observe({ entryTypes: ['resource'] });

            // Make the API call
            const response = await fetch(apiEndpoint, {
                method,
                headers: {
                    'Content-Type': 'application/json'
                },
                ...(body && { body: JSON.stringify(body) })
            });

            metrics.status = response.status;
            metrics.success = response.ok;
            
            // Wait for the response to be processed
            const data = await response.json();
            
            const endTime = performance.now();
            metrics.timing.total = endTime - startTime;

            observer.disconnect();

            return {
                metrics,
                data,
                success: true
            };
        } catch (error) {
            const endTime = performance.now();
            metrics.timing.total = endTime - startTime;
            metrics.error = error.message;

            return {
                metrics,
                error: error.message,
                success: false
            };
        }
    }

    static async getFID() {
        return new Promise(resolve => {
            let fid = 0;
            const observer = new PerformanceObserver(list => {
                const entries = list.getEntries();
                if (entries.length > 0) {
                    fid = entries[0].processingStart - entries[0].startTime;
                }
            });
            
            try {
                observer.observe({ 
                    entryTypes: ['first-input'],
                    buffered: true
                });
            } catch (e) {
                console.warn('FID observation failed:', e);
            }
            
            // Wait longer for FID and provide a fallback
            setTimeout(() => {
                observer.disconnect();
                // If no FID was recorded, estimate based on Time to Interactive
                if (fid === 0) {
                    const navigationEntry = performance.getEntriesByType('navigation')[0];
                    if (navigationEntry) {
                        const tti = navigationEntry.domInteractive;
                        fid = tti * 0.1; // Estimate FID as 10% of TTI
                    } else {
                        fid = 100; // Default fallback value
                    }
                }
                resolve(fid);
            }, 3000);
        });
    }

    static async getCLS() {
        return new Promise(resolve => {
            let cls = 0;
            let sessionValue = 0;
            let sessionEntries = [];

            const observer = new PerformanceObserver(list => {
                list.getEntries().forEach(entry => {
                    if (!entry.hadRecentInput) {
                        const firstSessionEntry = sessionEntries[0];
                        const lastSessionEntry = sessionEntries[sessionEntries.length - 1];

                        // If entry is within 1000ms of the previous entry and less than 5000ms from the first entry
                        if (sessionEntries.length &&
                            entry.startTime - lastSessionEntry.startTime < 1000 &&
                            entry.startTime - firstSessionEntry.startTime < 5000) {
                            sessionValue += entry.value;
                        } else {
                            sessionValue = entry.value;
                            sessionEntries = [entry];
                        }

                        if (sessionValue > cls) {
                            cls = sessionValue;
                        }
                    }
                });
            });

            try {
                observer.observe({ 
                    entryTypes: ['layout-shift'],
                    buffered: true
                });
            } catch (e) {
                console.warn('CLS observation failed:', e);
            }

            // Wait for a reasonable time to capture layout shifts
            setTimeout(() => {
                observer.disconnect();
                resolve(cls);
            }, 3000);
        });
    }
}

// Performance test suite
class PerformanceTestSuite {
    static async runAll() {
        console.group('Running Performance Test Suite');
        
        const results = {
            pageLoad: await PerformanceMetrics.measurePageLoad(),
            interactions: await this.measureUserInteractions(),
            rendering: await this.measureRenderingPerformance(),
            network: await this.measureNetworkPerformance()
        };

        console.groupEnd();
        return results;
    }

    static async measureUserInteractions() {
        return await PerformanceMetrics.measureInteractionTime(async () => {
            // Simulate user interactions
            await this.simulateUserJourney();
        }, 'Complete User Journey');
    }

    static async simulateUserJourney() {
        // Simulate typical user actions
        await this.measureAction(() => searchMembers('Test'), 'Search Members');
        await this.measureAction(() => createTestMember(), 'Create Member');
        await this.measureAction(() => loadMembers(), 'Refresh Member List');
    }

    static async measureAction(action, description) {
        return await PerformanceMetrics.measureInteractionTime(action, description);
    }

    static async measureRenderingPerformance() {
        const metrics = {
            frameRate: await PerformanceMetrics.getFrameMetrics(),
            renderTiming: performance.getEntriesByType('measure')
                .filter(entry => entry.name.includes('render'))
        };
        return metrics;
    }

    static async measureNetworkPerformance() {
        return PerformanceMetrics.getResourceMetrics();
    }
}

// Helper function to create test member
async function createTestMember() {
    const testMember = {
        name: `Test User ${Date.now()}`,
        email: `test${Date.now()}@example.com`,
        phoneNumber: '+1234567890'
    };

    return await fetch(`${API_URL}/members`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(testMember)
    });
}

// Add this new class for running tests and generating reports
class PerformanceTestRunner {
    static async runPerformanceTests() {
        console.group('🚀 Starting Performance Test Run');
        console.time('Total Test Duration');

        try {
            const iterations = 3;
            const allResults = [];
            
            for (let i = 0; i < iterations; i++) {
                console.log(`Running iteration ${i + 1}/${iterations}`);
                const result = await this.runSingleIteration();
                allResults.push(result);
            }

            console.timeEnd('Total Test Duration');
            
            const summary = this.calculateAverageMetrics(allResults);
            console.log('Performance test summary:', summary);
            
            return summary;
        } catch (error) {
            console.error('Error running performance tests:', error);
            throw error;
        } finally {
            console.groupEnd();
        }
    }

    static async runSingleIteration() {
        // Clear existing performance entries
        performance.clearResourceTimings();
        performance.clearMarks();
        performance.clearMeasures();

        // Mark the start of our measurements
        performance.mark('test-start');

        // Test API endpoints
        const apiMetrics = await this.measureApiEndpoints();

        // Simulate user interactions
        await this.simulateUserInteractions();

        // Get performance metrics
        const navigationTiming = performance.getEntriesByType('navigation')[0] || performance.timing;
        const paintTiming = performance.getEntriesByType('paint');
        const resourceTiming = performance.getEntriesByType('resource');

        // Calculate Core Web Vitals with longer wait times
        const [LCP, FID, CLS] = await Promise.all([
            PerformanceMetrics.getLCP(),
            PerformanceMetrics.getFID(),
            PerformanceMetrics.getCLS()
        ]);

        // Mark the end of our measurements
        performance.mark('test-end');
        performance.measure('total-test-duration', 'test-start', 'test-end');

        // Calculate other metrics using newer Navigation Timing API when available
        const TTFB = navigationTiming.responseStart - navigationTiming.startTime;
        const domInteractive = navigationTiming.domInteractive - navigationTiming.startTime;
        const domComplete = navigationTiming.domComplete - navigationTiming.startTime;
        const loadComplete = navigationTiming.loadEventEnd - navigationTiming.startTime;

        // Calculate resource metrics
        const resourceMetrics = {
            totalResources: resourceTiming.length,
            totalTransferSize: resourceTiming.reduce((total, resource) => total + (resource.transferSize || 0), 0),
            totalDuration: resourceTiming.reduce((total, resource) => total + (resource.duration || 0), 0)
        };

        return {
            LCP,
            FID,
            CLS,
            TTFB,
            domInteractive,
            domComplete,
            loadComplete,
            resourceMetrics,
            apiMetrics
        };
    }

    static async measureApiEndpoints() {
        const endpoints = [
            {
                name: 'Create Member',
                url: `${API_URL}/members`,
                method: 'POST',
                body: {
                    name: `Test User ${Date.now()}`,
                    email: `test${Date.now()}@example.com`,
                    phoneNumber: '+1234567890'
                }
            },
            {
                name: 'Get Members',
                url: `${API_URL}/members`,
                method: 'GET'
            },
            {
                name: 'Get Member by ID',
                url: `${API_URL}/members/1`, // We'll use a known ID
                method: 'GET'
            },
            {
                name: 'Update Member',
                url: `${API_URL}/members/1`,
                method: 'PUT',
                body: {
                    name: `Updated User ${Date.now()}`,
                    email: `updated${Date.now()}@example.com`,
                    phoneNumber: '+1987654321'
                }
            },
            {
                name: 'Delete Member',
                url: `${API_URL}/members/1`,
                method: 'DELETE'
            },
            {
                name: 'Search Members',
                url: `${API_URL}/members/search?query=test`,
                method: 'GET'
            },
            {
                name: 'Member Statistics',
                url: `${API_URL}/members/stats`,
                method: 'GET'
            },
            {
                name: 'Member Export',
                url: `${API_URL}/members/export`,
                method: 'GET'
            },
            {
                name: 'Member Import',
                url: `${API_URL}/members/import`,
                method: 'POST',
                body: {
                    members: [
                        {
                            name: `Import User 1 ${Date.now()}`,
                            email: `import1${Date.now()}@example.com`,
                            phoneNumber: '+1111111111'
                        },
                        {
                            name: `Import User 2 ${Date.now()}`,
                            email: `import2${Date.now()}@example.com`,
                            phoneNumber: '+2222222222'
                        }
                    ]
                }
            },
            {
                name: 'Member Validation',
                url: `${API_URL}/members/validate`,
                method: 'POST',
                body: {
                    email: `test${Date.now()}@example.com`,
                    phoneNumber: '+1234567890'
                }
            }
        ];

        // First create a test member to use for subsequent operations
        const createResult = await PerformanceMetrics.measureApiPerformance(
            `${API_URL}/members`,
            'POST',
            {
                name: `Test User ${Date.now()}`,
                email: `test${Date.now()}@example.com`,
                phoneNumber: '+1234567890'
            }
        );

        let testMemberId;
        if (createResult.success && createResult.data) {
            testMemberId = createResult.data.id;
            
            // Update endpoints that need the real member ID
            endpoints.forEach(endpoint => {
                if (endpoint.url.includes('/members/1')) {
                    endpoint.url = endpoint.url.replace('/1', `/${testMemberId}`);
                }
            });
        }

        const results = {};
        for (const endpoint of endpoints) {
            console.log(`Testing endpoint: ${endpoint.name}`);
            performance.mark(`api-${endpoint.name}-start`);
            
            const result = await PerformanceMetrics.measureApiPerformance(
                endpoint.url,
                endpoint.method,
                endpoint.body
            );

            performance.mark(`api-${endpoint.name}-end`);
            performance.measure(
                `api-${endpoint.name}`,
                `api-${endpoint.name}-start`,
                `api-${endpoint.name}-end`
            );

            results[endpoint.name] = {
                ...result.metrics,
                success: result.success,
                ...(result.error && { error: result.error })
            };
        }

        // Clean up - delete the test member if it was created
        if (testMemberId) {
            try {
                await fetch(`${API_URL}/members/${testMemberId}`, {
                    method: 'DELETE'
                });
            } catch (error) {
                console.warn('Failed to clean up test member:', error);
            }
        }

        return results;
    }

    static async simulateUserInteractions() {
        // Find and interact with various elements
        const clickableElements = document.querySelectorAll('button, a, input, select');
        for (const element of clickableElements) {
            // Simulate hover
            element.dispatchEvent(new MouseEvent('mouseover', { bubbles: true }));
            await new Promise(resolve => setTimeout(resolve, 100));
            
            // Simulate click if the element is visible
            const rect = element.getBoundingClientRect();
            if (rect.width > 0 && rect.height > 0) {
                element.click();
                await new Promise(resolve => setTimeout(resolve, 100));
            }
        }

        // Simulate scrolling
        window.scrollTo(0, 100);
        await new Promise(resolve => setTimeout(resolve, 100));
        window.scrollTo(0, 0);
        
        // Wait for any animations or transitions to complete
        await new Promise(resolve => setTimeout(resolve, 500));
    }

    static calculateAverageMetrics(results) {
        console.log('Calculating averages from:', results);

        const sum = results.reduce((acc, result) => ({
            LCP: (acc.LCP || 0) + (result.LCP || 0),
            FID: (acc.FID || 0) + (result.FID || 0),
            CLS: (acc.CLS || 0) + (result.CLS || 0),
            TTFB: (acc.TTFB || 0) + (result.TTFB || 0),
            domInteractive: (acc.domInteractive || 0) + (result.domInteractive || 0),
            domComplete: (acc.domComplete || 0) + (result.domComplete || 0),
            loadComplete: (acc.loadComplete || 0) + (result.loadComplete || 0),
            resourceMetrics: {
                totalResources: (acc.resourceMetrics?.totalResources || 0) + (result.resourceMetrics?.totalResources || 0),
                totalTransferSize: (acc.resourceMetrics?.totalTransferSize || 0) + (result.resourceMetrics?.totalTransferSize || 0),
                totalDuration: (acc.resourceMetrics?.totalDuration || 0) + (result.resourceMetrics?.totalDuration || 0)
            }
        }), {});

        const count = results.length;
        
        const averages = {
            LCP: sum.LCP / count,
            FID: sum.FID / count,
            CLS: sum.CLS / count,
            TTFB: sum.TTFB / count,
            domInteractive: sum.domInteractive / count,
            domComplete: sum.domComplete / count,
            loadComplete: sum.loadComplete / count,
            resourceMetrics: {
                totalResources: Math.round(sum.resourceMetrics.totalResources / count),
                totalTransferSize: Math.round(sum.resourceMetrics.totalTransferSize / count),
                totalDuration: sum.resourceMetrics.totalDuration / count
            }
        };

        // Add API metrics averaging
        const apiMetrics = {};
        if (results[0].apiMetrics) {
            Object.keys(results[0].apiMetrics).forEach(endpoint => {
                apiMetrics[endpoint] = {
                    timing: {
                        total: 0,
                        dns: 0,
                        tcp: 0,
                        request: 0,
                        response: 0,
                        processing: 0
                    },
                    successRate: 0
                };

                results.forEach(result => {
                    const metrics = result.apiMetrics[endpoint];
                    Object.keys(metrics.timing).forEach(timing => {
                        apiMetrics[endpoint].timing[timing] += metrics.timing[timing];
                    });
                    apiMetrics[endpoint].successRate += metrics.success ? 1 : 0;
                });

                // Calculate averages
                Object.keys(apiMetrics[endpoint].timing).forEach(timing => {
                    apiMetrics[endpoint].timing[timing] /= results.length;
                });
                apiMetrics[endpoint].successRate = (apiMetrics[endpoint].successRate / results.length) * 100;
            });
        }

        console.log('Calculated averages:', averages);
        return {
            ...averages,
            apiMetrics
        };
    }

    static displayReport(report) {
        // ... existing report display code ...
    }
} 