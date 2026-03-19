# FlowBoard Setup and Test Guide

## Quick Start

Use this if you want the fastest path to run the app locally.

```bash
# 1) Start PostgreSQL (Docker)
docker run --name flowboard-postgres \
  -e POSTGRES_USER=flowboard \
  -e POSTGRES_PASSWORD=flowboard_password \
  -e POSTGRES_DB=flowboard \
  -p 5432:5432 \
  -d postgres:16

# 2) Start backend (Terminal 1)
cd backend
mvn spring-boot:run

# 3) Start frontend (Terminal 2, repo root)
npm install
npm run dev
```

App URLs:
- Frontend: `http://localhost:5173`
- Backend base URL: `http://localhost:8080/api/v1`

Demo login:
- Email: `admin@flowboard.com`
- Password: `admin123`

## Project Structure

```text
CS698_Project_TrelloPlus/
|- backend/                    # Spring Boot backend (Java)
|  |- src/
|  |  |- main/java/com/flowboard/
|  |  |  |- config/            # Security and app config
|  |  |  |- controller/        # REST API endpoints
|  |  |  |- dto/               # Data transfer objects
|  |  |  |- entity/            # JPA entities
|  |  |  |- repository/        # Data access layer
|  |  |  |- service/           # Business logic
|  |  |  `- FlowBoardApplication.java
|  |  `- resources/
|  |     |- application.yml
|  |     `- db/migration/      # Flyway migrations
|  |- pom.xml
|  `- README.md
|- src/                        # React frontend
|- docs/
|- package.json
`- vite.config.ts
```

## Tech Stack

Backend:
- Java 21
- Spring Boot 3.2.0
- PostgreSQL 16
- Spring Security with JWT

Frontend:
- React 18
- TypeScript
- Vite
- Zustand
- Tailwind CSS and Radix UI

## Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL 16+ (or Docker)
- Node.js 18+

## Setup

### 1. Database Setup

Option A: Docker (recommended)

```bash
docker run --name flowboard-postgres \
  -e POSTGRES_USER=flowboard \
  -e POSTGRES_PASSWORD=flowboard_password \
  -e POSTGRES_DB=flowboard \
  -p 5432:5432 \
  -d postgres:16
```

Verify database container:

```bash
docker ps | grep flowboard-postgres
```

Option B: Manual PostgreSQL

```bash
createdb flowboard
createuser flowboard
psql flowboard -c "ALTER USER flowboard WITH PASSWORD 'flowboard_password';"
```

### 2. Backend Setup and Run

```bash
cd backend

# Build package and run backend tests
mvn clean install

# Start backend
mvn spring-boot:run
```

Backend runs at `http://localhost:8080/api/v1`.

Notes:
- Flyway runs automatically at startup.
- A default admin user is inserted by migration.

### 3. Frontend Setup and Run

From repository root:

```bash
npm install
npm run dev
```

Frontend runs at `http://localhost:5173`.

## Automated Testing

You can run tests without starting backend/frontend servers.

### Run All Backend Tests

```bash
cd backend
mvn test
```

### Run Focused User Story 1 + Auth Tests

```bash
cd backend
mvn -Dtest=AuthToUserStory1FlowApiTest,AuthControllerUserStory1ApiTest,ProjectControllerUserStory1ApiTest,BoardControllerUserStory1ApiTest,ProjectServiceUserStory1Test test
```

Focused suite coverage:
- Authentication API: register, login, logout
- User Story 1 API: create project
- Board API used by User Story 1: add stage, create card, move card
- Service path: AI analysis and board generation orchestration

### Test Classes in Focused Suite

- `backend/src/test/java/com/flowboard/controller/AuthToUserStory1FlowApiTest.java`
- `backend/src/test/java/com/flowboard/controller/AuthControllerUserStory1ApiTest.java`
- `backend/src/test/java/com/flowboard/controller/ProjectControllerUserStory1ApiTest.java`
- `backend/src/test/java/com/flowboard/controller/BoardControllerUserStory1ApiTest.java`
- `backend/src/test/java/com/flowboard/service/ProjectServiceUserStory1Test.java`

## Manual User Story 1 Verification

### 1. Start Servers

Terminal 1:

```bash
cd backend
mvn spring-boot:run
```

Terminal 2:

```bash
npm run dev
```

### 2. Login

Go to `http://localhost:5173/login` and sign in with:
- Email: `admin@flowboard.com`
- Password: `admin123`

### 3. Create an AI-Generated Board

1. Click `Create New Project`.
2. Project name: `Website Redesign`.
3. Description example:
   `We need to redesign our company website with modern UI/UX principles. The project involves wireframing, design system setup, high-fidelity mockups, and implementation.`
4. Click `Generate AI Board`.
5. Review and click `Create Board`.

### 4. Validate Board Operations

- Create a card
- Move a card between columns
- Add a stage
- Update task details

## API Endpoints

Base URL: `http://localhost:8080/api/v1`

Auth:

```text
POST   /auth/register
POST   /auth/login
POST   /auth/logout
GET    /auth/profile
PUT    /auth/profile
```

Projects:

```text
POST   /projects
GET    /projects
GET    /projects/{projectId}
PUT    /projects/{projectId}
DELETE /projects/{projectId}
```

Boards:

```text
POST   /boards/{boardId}/stages
PUT    /boards/stages/{stageId}
DELETE /boards/stages/{stageId}

POST   /boards/stages/{stageId}/cards
PUT    /boards/cards/{cardId}
PUT    /boards/cards/{cardId}/move
DELETE /boards/cards/{cardId}
```

## Troubleshooting

### Backend startup fails

`Connection to localhost:5432 refused`
- Ensure PostgreSQL is running: `docker ps` or `pg_isready`
- Check `backend/src/main/resources/application.yml`

`Address already in use`
- Change `server.port` in `backend/src/main/resources/application.yml`
- Or stop process on port 8080: `lsof -ti:8080 | xargs kill -9`

### Frontend API issues

`CORS error or API request failed`
- Ensure backend is running on port 8080
- Verify frontend API base URL in `src/app/services/api.ts`
- Verify CORS/security config in `backend/src/main/java/com/flowboard/config/SecurityConfig.java`

`401 Unauthorized`
- Log in again to refresh token
- Verify token storage in localStorage

### Flyway migration issues

`Relations already exist`
- Recreate the database and rerun backend startup.

`Checksum mismatch after editing applied migration`
- Run Flyway repair locally.
- Keep applied migrations immutable; add a new versioned migration for additional schema changes.

## Development Notes

AI generation is currently mocked in `backend/src/main/java/com/flowboard/service/AIEngine.java`.

To integrate a real LLM provider:
1. Replace mocked generation logic in `analyzeProjectDescription`.
2. Add provider API integration and error handling.
3. Keep deterministic fallback behavior for local/dev mode.

## References

- `backend/README.md`
- `docs/UNIFIED_BACKEND_ARCHITECTURE.md`
- `docs/BACKEND_MODULE_SPECIFICATIONS.md`
- `docs/dev_spec_1.md`
- `docs/dev_spec_2.md`
- `docs/dev_spec_3.md`
