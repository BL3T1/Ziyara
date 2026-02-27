# Ziyarah Platform - Running the Backend and Frontend

This document provides comprehensive instructions for running the Ziyarah booking platform's backend and frontend services.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Option 1: Docker Deployment (Recommended)](#option-1-docker-deployment-recommended)
- [Option 2: Local Development Setup](#option-2-local-development-setup)
- [Accessing the Application](#accessing-the-application)
- [API Documentation](#api-documentation)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### For Docker Deployment
- **Docker Engine** 20.10 or higher
- **Docker Compose** 2.0 or higher
- At least **4GB RAM** available for containers

### For Local Development
- **Java** 17 (JDK 17)
- **Maven** 3.9+
- **Node.js** 20+ and **npm**
- **PostgreSQL** 15+
- **Redis** 7+ (optional, for session management)

---

## Option 1: Docker Deployment (Recommended)

The easiest way to run the entire platform is using Docker Compose, which orchestrates all services automatically.

### Architecture Overview

```
Frontend (React/Nginx)  --->  Backend (Spring Boot)  --->  PostgreSQL
        :80                      :8080                       :5432
                                    |
                                    +---> Redis :6379
```

### Quick Start

1. **Navigate to the project directory:**
   ```bash
   cd ziyarah-backend
   ```

2. **Build and start all services:**
   ```bash
   docker-compose up -d --build
   ```

3. **Verify all containers are running:**
   ```bash
   docker-compose ps
   ```

   Expected output:
   ```
   NAME              STATUS          PORTS
   ziyarah-backend   healthy         0.0.0.0:8080->8080/tcp
   ziyarah-db        healthy         0.0.0.0:5432->5432/tcp
   ziyarah-frontend  healthy         0.0.0.0:80->80/tcp
   ziyarah-redis     healthy         0.0.0.0:6379->6379/tcp
   ziyarah-adminer   healthy         0.0.0.0:8081->8080/tcp
   ```

4. **View logs (optional):**
   ```bash
   # All services
   docker-compose logs -f

   # Specific service
   docker-compose logs -f backend
   docker-compose logs -f frontend
   ```

### Stopping the Application

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (reset database)
docker-compose down -v
```

---

## Option 2: Local Development Setup

This section covers running each service individually without Docker, useful for development and debugging.

### Step 1: Database Setup (PostgreSQL)

1. **Install PostgreSQL 15+** if not already installed.

2. **Create the database and user:**
   ```sql
   -- Connect to PostgreSQL as superuser
   psql -U postgres

   -- Create user and database
   CREATE USER ziyarah_user WITH PASSWORD 'ziyarah_password';
   CREATE DATABASE ziyarah OWNER ziyarah_user;
   GRANT ALL PRIVILEGES ON DATABASE ziyarah TO ziyarah_user;

   -- Exit psql
   \q
   ```

3. **Initialize the schema:**
   ```bash
   psql -U ziyarah_user -d ziyarah -f database/schema.sql
   ```

   Or on Windows:
   ```cmd
   psql -U ziyarah_user -d ziyarah -f database\schema.sql
   ```

### Step 2: Redis Setup (Optional)

Redis is used for session management and caching.

1. **Install Redis** (or run via Docker):
   ```bash
   # Using Docker for Redis only
   docker run -d --name ziyarah-redis -p 6379:6379 redis:7-alpine
   ```

2. **Verify Redis is running:**
   ```bash
   redis-cli ping
   # Expected output: PONG
   ```

### Step 3: Running the Backend (Spring Boot)

1. **Navigate to the backend directory:**
   ```bash
   cd ziyarah-backend/backend
   ```

2. **Configure environment variables (optional):**

   The default configuration in [`application.yml`](backend/src/main/resources/application.yml) uses:
   - Database: `localhost:5432/ziyarah`
   - Redis: `localhost:6379`

   You can override these via environment variables:
   ```bash
   # Linux/macOS
   export JWT_SECRET=your-secret-key-here
   export JWT_EXPIRATION=86400000

   # Windows (Command Prompt)
   set JWT_SECRET=your-secret-key-here
   set JWT_EXPIRATION=86400000

   # Windows (PowerShell)
   $env:JWT_SECRET="your-secret-key-here"
   $env:JWT_EXPIRATION="86400000"
   ```

3. **Build the project:**
   ```bash
   mvn clean package -DskipTests
   ```

4. **Run the application:**

   **Option A - Using Maven:**
   ```bash
   mvn spring-boot:run
   ```

   **Option B - Using the JAR file:**
   ```bash
   java -jar target/ziyarah-backend-1.0.0-SNAPSHOT.jar
   ```

5. **Verify the backend is running:**
   ```bash
   curl http://localhost:8080/api/v1/actuator/health
   ```
   
   Expected response:
   ```json
   {"status":"UP"}
   ```

### Step 4: Running the Frontend (React)

1. **Navigate to the frontend directory:**
   ```bash
   cd ziyarah-backend/frontend
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Configure the API URL:**

   Create a `.env` file in the frontend directory:
   ```env
   REACT_APP_API_URL=http://localhost:8080/api/v1
   ```

4. **Start the development server:**
   ```bash
   npm start
   ```

5. **Access the application:**
   
   The development server will automatically open `http://localhost:3000` in your browser.

---

## Accessing the Application

### Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| **Frontend** | http://localhost (Docker) or http://localhost:3000 (Local) | React web application |
| **Backend API** | http://localhost:8080/api/v1 | Spring Boot REST API |
| **Swagger UI** | http://localhost:8080/api/v1/swagger-ui.html | API documentation |
| **API Docs** | http://localhost:8080/api/v1/api-docs | OpenAPI JSON spec |
| **Adminer** | http://localhost:8081 | Database management UI (Docker only) |

### Default Credentials

**Database:**
- Host: `localhost`
- Port: `5432`
- Database: `ziyarah`
- Username: `ziyarah_user`
- Password: `ziyarah_password`

**Adminer (Database UI):**
- System: PostgreSQL
- Server: `postgres` (Docker) or `localhost` (Local)
- Username: `ziyarah_user`
- Password: `ziyarah_password`
- Database: `ziyarah`

---

## API Documentation

The backend provides interactive API documentation via Swagger/OpenAPI.

### Accessing Swagger UI

1. Start the backend service
2. Navigate to: http://localhost:8080/api/v1/swagger-ui.html
3. Browse and test API endpoints directly from the browser

### Available API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/auth/login` | POST | User authentication |
| `/api/v1/auth/register` | POST | User registration |
| `/api/v1/bookings` | GET, POST | Booking operations |
| `/api/v1/bookings/{id}` | GET, PUT, DELETE | Single booking operations |
| `/api/v1/tickets` | GET, POST | Support ticket operations |
| `/api/v1/tickets/{id}` | GET, PUT | Single ticket operations |
| `/api/v1/users` | GET | User management |
| `/api/v1/users/{id}` | GET, PUT | Single user operations |

---

## Troubleshooting

### Common Issues

#### 1. Database Connection Failed

**Symptoms:** Backend fails to start with connection errors.

**Solutions:**
```bash
# Check if PostgreSQL is running
docker-compose ps postgres
# Or locally
pg_isready -h localhost -p 5432

# Check database logs
docker-compose logs postgres

# Verify credentials
psql -U ziyarah_user -d ziyarah -h localhost
```

#### 2. Port Already in Use

**Symptoms:** Error: "Port 8080 is already in use"

**Solutions:**
```bash
# Find process using the port (Linux/macOS)
lsof -i :8080
kill -9 <PID>

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Or change the port in application.yml
server:
  port: 8081
```

#### 3. Frontend Can't Connect to Backend

**Symptoms:** CORS errors or network failures in browser console.

**Solutions:**
- Ensure backend is running on port 8080
- Check `REACT_APP_API_URL` environment variable
- Verify CORS configuration in [`SecurityConfig.java`](backend/src/main/java/com/ziyarah/infrastructure/config/SecurityConfig.java)

#### 4. Docker Containers Not Starting

**Symptoms:** Containers exit immediately or show unhealthy status.

**Solutions:**
```bash
# Check container logs
docker-compose logs backend
docker-compose logs postgres

# Rebuild containers
docker-compose down -v
docker-compose up -d --build

# Check Docker resources
docker system df
docker system prune  # Clean up unused resources
```

#### 5. Maven Build Fails

**Symptoms:** Compilation errors or dependency resolution failures.

**Solutions:**
```bash
# Clean Maven cache
mvn clean

# Force update dependencies
mvn clean install -U

# Skip tests if needed
mvn clean package -DskipTests
```

### Health Check Commands

```bash
# Backend health
curl http://localhost:8080/api/v1/actuator/health

# Database health (Docker)
docker-compose exec postgres pg_isready -U ziyarah_user -d ziyarah

# Redis health
redis-cli ping

# All container health (Docker)
docker-compose ps
```

### Reset Everything

```bash
# Stop and remove all containers, networks, and volumes
docker-compose down -v

# Remove all images
docker-compose down --rmi all -v

# Rebuild from scratch
docker-compose up -d --build
```

---

## Development Tips

### Hot Reload for Frontend

The React development server supports hot reload. Any changes to the source files will automatically refresh the browser.

### Backend Debug Mode

Enable debug logging in [`application.yml`](backend/src/main/resources/application.yml):
```yaml
logging:
  level:
    com.ziyarah: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
```

### Running Tests

**Backend:**
```bash
cd backend
mvn test
```

**Frontend:**
```bash
cd frontend
npm test
```

---

## Production Considerations

Before deploying to production:

1. **Change all default passwords** in `docker-compose.yml` and `application.yml`
2. **Use secrets management** instead of environment variables for sensitive data
3. **Configure SSL/TLS** for all external endpoints
4. **Set up database backups** for PostgreSQL
5. **Configure resource limits** in Docker Compose
6. **Review JWT secret** and use a strong, unique key
7. **Disable Swagger UI** in production (set `springdoc.swagger-ui.enabled: false`)

---

## Additional Resources

- [Docker Documentation](DOCKER.md) - Detailed Docker deployment guide
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)