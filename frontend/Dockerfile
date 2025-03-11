FROM nginx:alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Copy the static web content
COPY webapp /usr/share/nginx/html

# Copy the nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80 