FROM nginx:alpine

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy SSL certificates into Nginx directory
COPY certs/ /etc/nginx/certs/

# Copy the pre-built dist folder created by Jenkins in the workspace
COPY dist/frontend/browser /usr/share/nginx/html

# Expose HTTP and HTTPS ports
EXPOSE 8080 8443

CMD ["nginx", "-g", "daemon off;"]