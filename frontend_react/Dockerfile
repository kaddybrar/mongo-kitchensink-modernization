# Build stage
FROM node:18-alpine as builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy source code
COPY . .

# Build the application with verbose output
RUN NODE_ENV=production npm run build

# Production stage
FROM nginx:alpine

# Install curl for health checks and envsubst for environment variable substitution
RUN apk add --no-cache curl gettext

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Create a shell script for environment variable substitution
RUN printf '#!/bin/sh\n\
echo "Replacing environment variables..."\n\
envsubst < /usr/share/nginx/html/assets/env-config.js.template > /usr/share/nginx/html/assets/env-config.js\n\
echo "Starting nginx..."\n\
nginx -g "daemon off;"\n' > /docker-entrypoint.sh \
    && chmod +x /docker-entrypoint.sh

# Create env-config.js template
RUN echo "window._env_ = { VITE_API_URL: '${VITE_API_URL}' };" > /usr/share/nginx/html/assets/env-config.js.template

EXPOSE 80

ENTRYPOINT ["/docker-entrypoint.sh"] 