# FlowBoard Backend

## Prerequisites

- Java 21 (or compatible)
- Maven 3.8+
- PostgreSQL 16.1
- Docker (optional, for running PostgreSQL)

## Database Setup

### Option 1: Docker (Recommended)

```bash
docker run --name flowboard-postgres \
  -e POSTGRES_USER=flowboard \
  -e POSTGRES_PASSWORD=flowboard_password \
  -e POSTGRES_DB=flowboard \
  -p 5432:5432 \
  -d postgres:16
```

### Option 2: Manual PostgreSQL Setup

```bash
# Create database and user
createdb flowboard
createuser flowboard
psql flowboard -c "ALTER USER flowboard WITH PASSWORD 'flowboard_password';"
```

## Building

```bash
cd backend
mvn clean install
```

## Running

```bash
# Run the application
mvn spring-boot:run

# Or use java directly
java -jar target/flowboard-backend-1.0.0.jar
```

The backend will start on `http://localhost:8080/api/v1`

## API Documentation

- **Auth Endpoints**
  - `POST /auth/register` - Register a new user
  - `POST /auth/login` - Login and get JWT token

- **Project Endpoints**
  - `POST /projects` - Create a new project (requires auth)
  - `GET /projects` - Get user's projects (requires auth)
  - `GET /projects/{projectId}` - Get project details
  - `DELETE /projects/{projectId}` - Delete project

- **Board Endpoints**
  - `POST /boards/{boardId}/stages` - Add stage to board
  - `PUT /boards/stages/{stageId}` - Update stage
  - `DELETE /boards/stages/{stageId}` - Delete stage
  - `POST /boards/stages/{stageId}/cards` - Create card
  - `PUT /boards/cards/{cardId}` - Update card
  - `PUT /boards/cards/{cardId}/move` - Move card to another stage
  - `DELETE /boards/cards/{cardId}` - Delete card

## Configuration

Edit `src/main/resources/application.yml` to configure:
- Database connection
- JWT secret
- AI engine (mock vs. real)
- Server port

## Testing

```bash
mvn test
```

## Troubleshooting

### Database Connection Failed
- Ensure PostgreSQL is running
- Check credentials in `application.yml`
- Verify database exists

### Port Already in Use
- Change `server.port` in `application.yml`
- Or kill process on port 8080: `lsof -ti:8080 | xargs kill`
