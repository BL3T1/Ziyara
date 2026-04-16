# Ziyara Dashboard (Vite) - multi-stage build

FROM node:20.20-alpine3.23 AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci --silent

ARG VITE_API_URL=/api/v1
ENV VITE_API_URL=$VITE_API_URL

ARG VITE_APP_SURFACE=company
ENV VITE_APP_SURFACE=$VITE_APP_SURFACE

# Landing CTAs: absolute bases for company vs provider apps (set per service in compose)
ARG VITE_COMPANY_APP_URL=
ENV VITE_COMPANY_APP_URL=$VITE_COMPANY_APP_URL
ARG VITE_PROVIDER_APP_URL=
ENV VITE_PROVIDER_APP_URL=$VITE_PROVIDER_APP_URL

COPY . .
RUN npm run build

# Production: nginx
FROM nginx:1.28.2-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

# Use 127.0.0.1 (not localhost) — on some hosts localhost → ::1 while nginx listens on IPv4 only.
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1:80/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
