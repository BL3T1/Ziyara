# Ziyarah Database Dockerfile
# Custom PostgreSQL image with initialization scripts

FROM postgres:15-alpine

LABEL maintainer="Ziyarah Team"
LABEL description="Ziyarah PostgreSQL Database"

# Set environment variables (can be overridden)
ENV POSTGRES_DB=ziyarah
ENV POSTGRES_USER=ziyarah_user
ENV POSTGRES_PASSWORD=ziyarah_password

# Copy initialization scripts
COPY schema.sql /docker-entrypoint-initdb.d/01-schema.sql

# Expose PostgreSQL port
EXPOSE 5432

# Health check
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
    CMD pg_isready -U $POSTGRES_USER -d $POSTGRES_DB || exit 1
