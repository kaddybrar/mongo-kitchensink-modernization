# MongoDB Kitchensink Frontend

This directory contains the frontend application for the MongoDB Kitchensink Modernization project. The frontend is a modern web application that communicates with the Spring Boot backend via REST APIs.

## Overview

The frontend is built using:
- HTML5
- CSS3
- JavaScript (ES6+)
- Bootstrap 5 for responsive design
- Nginx for serving static content and proxying API requests

## Project Structure

```
frontend/
├── Dockerfile            # Docker configuration for the frontend
├── nginx.conf            # Nginx configuration
├── webapp/               # Static web content
│   ├── index.html        # Main HTML file
│   ├── resources/        # External resources
│   │   ├── css/          # CSS stylesheets
│   │   │   └── styles.css # Main stylesheet
│   │   └── js/           # JavaScript files
│   │       └── app.js    # Main application logic
│   └── WEB-INF/          # Web application configuration
└── README.md             # This file
```

## Features

- **Member Management**: Create, read, update, and delete member records
- **Search Functionality**: Search for members by name
- **Form Validation**: Client-side validation for all form inputs
- **Responsive Design**: Works on desktop and mobile devices
- **Error Handling**: Comprehensive error handling and user feedback

## Development

### Prerequisites

- Node.js and npm (for local development)
- Docker and Docker Compose (for containerized deployment)

### Local Development

1. Make changes to the files in the `webapp` directory
2. Test locally using a simple HTTP server:
   ```
   cd webapp
   npx http-server
   ```
3. Access the application at `http://localhost:8080`

### Building the Docker Image

```bash
docker build -t kitchensink-frontend .
```

### Running with Docker Compose

From the root directory of the project:

```bash
docker-compose up
```

This will start both the frontend and backend services.

## Performance Testing

The frontend includes automated performance testing using Puppeteer to measure:

### Metrics Collected
- Page load times
- First Contentful Paint (FCP)
- Largest Contentful Paint (LCP)
- Time to Interactive (TTI)
- Component render times
- Network request latencies

### Running Performance Tests

```bash
# Run performance tests in Docker
cd webapp
./docker-performance-test.sh

# View results
open ../../performance-reports/latest/frontend/performance-report.html
```

### Test Configuration
Tests can be configured through environment variables:
```properties
# Browser type (chromium/firefox)
PERFORMANCE_TEST_BROWSER=chromium

# Number of test iterations
PERFORMANCE_TEST_ITERATIONS=3

# Network throttling (fast 3G, slow 3G)
PERFORMANCE_TEST_NETWORK=fast3G

# CPU throttling factor
PERFORMANCE_TEST_CPU_THROTTLE=4
```

### Test Reports
- Performance metrics visualization
- Waterfall charts for page loads
- Component-level timing breakdown
- Network request analysis
- Screenshots and traces

## Deployment

The frontend is designed to be deployed as a Docker container. The included Dockerfile creates an Nginx-based image that:

1. Serves the static content from the `webapp` directory
2. Proxies API requests to the backend service
3. Handles CORS and other HTTP headers

## API Integration

The frontend communicates with the backend using the following API endpoints:

- `GET /api/v1/members` - Get all members
- `GET /api/v1/members/{id}` - Get a specific member
- `POST /api/v1/members` - Create a new member
- `PUT /api/v1/members/{id}` - Update a member
- `DELETE /api/v1/members/{id}` - Delete a member
- `GET /api/v1/members/search?name={name}` - Search for members by name

## Browser Compatibility

The application is compatible with:
- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Notes on Migration from JSF

This frontend is a complete rewrite of the original JSF-based frontend. Key differences include:

- **Architecture**: Changed from server-rendered JSF to client-side JavaScript with REST API
- **Styling**: Updated from older CSS to modern Bootstrap 5
- **Validation**: Moved from server-side to client-side validation with immediate feedback
- **Deployment**: Changed from WAR deployment to containerized Nginx

## Troubleshooting

### API Connection Issues

If you encounter "Error loading members" or similar API connection issues:

1. Check that the backend service is running
2. Verify the Nginx configuration in `nginx.conf`
3. Check browser console for specific error messages
4. Ensure Docker network is properly configured if running in containers

### CORS Issues

If you see CORS-related errors in the browser console:

1. Verify the CORS configuration in the backend application
2. Check the Nginx proxy configuration in `nginx.conf`
3. Ensure the `Access-Control-Allow-Origin` headers are properly set

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details. 