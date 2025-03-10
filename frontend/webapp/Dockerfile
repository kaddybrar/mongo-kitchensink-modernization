# Use Node.js base image
FROM node:20

# Install Chromium and its dependencies
RUN apt-get update && apt-get install -y \
    chromium \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libatspi2.0-0 \
    libcups2 \
    libdbus-1-3 \
    libdrm2 \
    libgbm1 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    xdg-utils \
    --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Set environment variables
ENV NODE_ENV=test
ENV DOCKER_BUILD_ID=${DOCKER_BUILD_ID:-latest}
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
ENV CHROME_BIN=/usr/bin/chromium
ENV CHROME_PATH=/usr/bin/chromium
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium

# Install puppeteer globally
RUN npm install -g puppeteer@21.1.1

# Copy all application files
COPY . .

# Install local dependencies
RUN npm install

# Create directory structure
RUN mkdir -p /app/performance-reports /app/resources/js

# Copy test files and scripts
COPY resources/js/performance-tests.js ./resources/js/
COPY resources/js/performance-report-service.js ./resources/js/
COPY resources/js/performance-report-analyzer.js ./resources/js/
COPY docker-performance-test.sh ./

# Make performance test script executable
RUN chmod +x ./docker-performance-test.sh

# Create volume for performance reports
VOLUME /app/performance-reports

# Default command to run performance tests
CMD ["./docker-performance-test.sh"] 