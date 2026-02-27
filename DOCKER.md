# Ziyarah Docker Deployment

This document describes how to build and run the Ziyarah platform using Docker containers.

## Architecture


figma token: figd_CtG6Xwqfdbq5Ar5FhdbKsdNGUpK00Zjnr8B9CbwP


```
┌─────────────────────────────────────────────────────────────────┐
│                     Docker Network (ziyarah-network)            │
│                                                                 │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐        │
│  │  Frontend   │───▶│   Backend   │───▶│  PostgreSQL │        │
│  │   (Nginx)   │    │(Spring Boot)│    │   (Port 5432)│        │
│  │  Port: 80   │    │  Port: 8080 │    │             │        │
│  └─────────────┘    └──────┬──────┘    └─────────────┘        │
│                            │                                    │
│                     ┌──────▼──────┐                            │
│                     │    Redis    │                            │
│                     │ Port: 6379  │                            │
│                     └─────────────┘                            │
└─────────────────────────────────────────────────────────────────┘
```

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- At least 4GB RAM available for containers

## Quick Start

### Build and Run All Services

```bash
# Navigate to the project directory
cd ziyarah-backend

# Build and start all services
docker-compose up -d --build

# View logs
docker-compose logs -f
```

### Access the Application

| Service | URL | Description |
|---------|-----|-------------|
| Frontend | http://localhost | React web application |
| Backend API | http://localhost:8080 | Spring Boot REST API |
| API Docs | http://localhost:8080/swagger-ui.html | Swagger UI |
| Adminer | http://localhost:8081 | Database management UI |

## Individual Service Commands

### Build Individual Images

```bash
# Build database image
docker build -t ziyarah-db:latest ./database

# Build backend image
docker build -t ziyarah-backend:latest ./backend

# Build frontend image
docker build -t ziyarah-frontend:latest ./frontend
```

### Run Individual Containers

```bash
# Run PostgreSQL
docker run -d \
  --name ziyarah-db \
  -e POSTGRES_DB=ziyarah \
  -e POSTGRES_USER=ziyarah_user \
  -e POSTGRES_PASSWORD=ziyarah_password \
  -p 5432:5432 \
  ziyarah-db:latest

# Run Backend (after database is ready)
docker run -d \
  --name ziyarah-backend \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/ziyarah \
  -e SPRING_DATASOURCE_USERNAME=ziyarah_user \
  -e SPRING_DATASOURCE_PASSWORD=ziyarah_password \
  -p 8080:8080 \
  ziyarah-backend:latest

# Run Frontend (after backend is ready)
docker run -d \
  --name ziyarah-frontend \
  -p 80:80 \
  ziyarah-frontend:latest
```

## Container Communication

All containers are connected via the `ziyarah-network` bridge network:

- **Frontend** communicates with Backend via `http://backend:8080`
- **Backend** communicates with PostgreSQL via `postgres:5432`
- **Backend** communicates with Redis via `redis:6379`

## Environment Variables

### Backend Service

| Variable | Default | Description |
|----------|---------|-------------|
| SPRING_PROFILES_ACTIVE | docker | Spring profile |
| SPRING_DATASOURCE_URL | - | Database connection URL |
| SPRING_DATASOURCE_USERNAME | ziyarah_user | Database username |
| SPRING_DATASOURCE_PASSWORD | ziyarah_password | Database password |
| JWT_SECRET | - | JWT signing secret |
| JWT_EXPIRATION | 86400000 | JWT token expiration (ms) |

### Database Service

| Variable | Default | Description |
|----------|---------|-------------|
| POSTGRES_DB | ziyarah | Database name |
| POSTGRES_USER | ziyarah_user | Database username |
| POSTGRES_PASSWORD | ziyarah_password | Database password |

## Health Checks

All services include health checks:

```bash
# Check container health status
docker-compose ps

# Check backend health endpoint
curl http://localhost:8080/actuator/health

# Check frontend health endpoint
curl http://localhost/health
```

## Volumes

| Volume | Purpose |
|--------|---------|
| postgres_data | PostgreSQL data persistence |
| redis_data | Redis data persistence |

## Troubleshooting

### View Container Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f postgres
docker-compose logs -f frontend
```

### Restart Services

```bash
# Restart all services
docker-compose restart

# Restart specific service
docker-compose restart backend
```

### Reset Everything

```bash
# Stop and remove containers, networks, volumes
docker-compose down -v

# Rebuild and start
docker-compose up -d --build
```

### Database Connection Issues

```bash
# Check if database is ready
docker-compose exec postgres pg_isready -U ziyarah_user -d ziyarah

# Connect to database
docker-compose exec postgres psql -U ziyarah_user -d ziyarah
```

## Production Considerations

1. **Change default passwords** in docker-compose.yml
2. **Use secrets** instead of environment variables for sensitive data
3. **Configure SSL/TLS** for production deployments
4. **Set up proper backup** for PostgreSQL volumes
5. **Configure resource limits** in docker-compose.yml
6. **Use external Redis** for production if needed

## Resource Limits (Optional)

Add to docker-compose.yml for production:

```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 1G
        reservations:
          cpus: '1'
          memory: 512M
```
