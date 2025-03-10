const express = require('express');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = 3000;

// Serve static files
app.use(express.static('performance-reports'));
app.use(express.static('.'));

// Get the latest report
app.get('/performance-reports/latest', (req, res) => {
    const reportsDir = path.join(__dirname, 'performance-reports');
    const files = fs.readdirSync(reportsDir)
        .filter(file => file.endsWith('.json'))
        .sort((a, b) => {
            return fs.statSync(path.join(reportsDir, b)).mtime.getTime() - 
                   fs.statSync(path.join(reportsDir, a)).mtime.getTime();
        });

    if (files.length === 0) {
        return res.status(404).json({ error: 'No reports found' });
    }

    const latestReport = JSON.parse(
        fs.readFileSync(path.join(reportsDir, files[0]), 'utf8')
    );
    res.json(latestReport);
});

app.listen(PORT, () => {
    console.log(`Report viewer running at http://localhost:${PORT}/performance-viewer.html`);
}); 