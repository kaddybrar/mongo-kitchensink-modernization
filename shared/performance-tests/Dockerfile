FROM node:20-slim

# Install Chrome and dependencies
RUN apt-get update && apt-get install -y \
    chromium \
    chromium-sandbox \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set Chrome executable path for Puppeteer
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true

# Create app directory
WORKDIR /app

# Copy package files first (for better caching)
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy all source files
COPY index.js ./
COPY run-tests.js ./
COPY docker-performance-test.sh ./

# Make the test script executable
RUN chmod +x docker-performance-test.sh

# Set environment
ENV NODE_ENV=test

ENTRYPOINT ["sh"]
CMD ["docker-performance-test.sh"] 