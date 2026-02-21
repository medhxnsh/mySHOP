# =============================================================================
# Frontend Dockerfile — Multi-stage build
# Stage 1: Build React app with Node.js
# Stage 2: Serve with Nginx (much smaller than a Node.js server)
# =============================================================================

# ── Stage 1: Build React + Vite ──────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files first for Docker layer caching
# (same optimization as the backend Dockerfile)
COPY package.json package-lock.json ./
RUN npm ci --silent  # ci = clean install (uses package-lock.json exactly)

# Copy source and build
COPY . .
RUN npm run build
# Output: /app/dist/ — contains index.html, assets/*, etc.


# ── Stage 2: Nginx to serve static files ─────────────────────────────────────
FROM nginx:1.25-alpine

# Remove default Nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy our custom Nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy the Vite build output into Nginx web root
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
# "daemon off" keeps Nginx in the foreground so Docker can track the process
