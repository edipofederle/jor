# Build stage
FROM node:22-bookworm AS builder

# Install Java (required by shadow-cljs)
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install npm dependencies first (layer cache)
COPY package.json ./
RUN npm install

# Copy source and config
COPY shadow-cljs.edn ./
COPY src/ ./src/
COPY public/ ./public/

# Production build
ENV JAVA_TOOL_OPTIONS="-Xmx512m"
RUN ./node_modules/.bin/shadow-cljs release app

# Serve stage
FROM nginx:alpine

COPY --from=builder /app/public /usr/share/nginx/html

EXPOSE 80
