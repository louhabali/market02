# --- Stage 1: Build & Test Angular App ---
FROM node:20-alpine AS build

# Install Chromium for headless testing in Alpine
RUN apk add --no-cache chromium
ENV CHROME_BIN=/usr/bin/chromium-browser

WORKDIR /app

# Copy dependency manifests and install
COPY package*.json ./
RUN npm ci

# Copy source code
COPY . .

# Run Unit Tests (Fails build if tests fail)
RUN npx ng test --watch=false --browsers=ChromeHeadlessNoSandbox

# Build for production
RUN npm run build -- --configuration production

# --- Stage 2: Serve with Nginx ---
FROM nginx:alpine

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy SSL certificates into Nginx directory
COPY certs/ /etc/nginx/certs/

# Copy built Angular static assets from Stage 1
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

# Expose HTTP and HTTPS ports
EXPOSE 8080 8443

CMD ["nginx", "-g", "daemon off;"]