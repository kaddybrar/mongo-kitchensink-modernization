import PerformanceTestRunner from './index.js';

const url = process.env.TEST_URL;
const reportDir = process.env.REPORT_DIR;
const buildId = process.env.BUILD_ID;
const appName = process.env.APP_NAME;

if (!url || !reportDir || !buildId || !appName) {
    console.error('Missing required environment variables:');
    console.error('TEST_URL:', url);
    console.error('REPORT_DIR:', reportDir);
    console.error('BUILD_ID:', buildId);
    console.error('APP_NAME:', appName);
    process.exit(1);
}

console.log('Starting performance tests with:');
console.log('URL:', url);
console.log('Report Directory:', reportDir);
console.log('Build ID:', buildId);
console.log('App Name:', appName);

const runner = new PerformanceTestRunner({
    url,
    reportDir,
    buildId,
    appName
});

try {
    await runner.runAllTests();
    console.log('Performance tests completed successfully');
} catch (error) {
    console.error('Error running performance tests:', error);
    process.exit(1);
} 