# TrelloPlus Project - Setup and Run Commands

## 1. Start PostgreSQL with Docker

```powershell
docker run --name flowboard-postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres
```

Verify it's running:
```powershell
docker ps
```

## 2. Start Backend (Java/Spring Boot)

Navigate to backend directory:
```powershell
cd c:\Users\vishe\OneDrive\Desktop\Project\CS698_Project_TrelloPlus\flowboard-backend
```

Run with Maven:
```powershell
mvn spring-boot:run
```

Or if you prefer to build first:
```powershell
mvn clean install
mvn spring-boot:run
```

Backend will start at: `http://localhost:8080`

## 3. Start Frontend (React/TypeScript)

In a new terminal, navigate to frontend directory:
```powershell
cd c:\Users\vishe\OneDrive\Desktop\Project\CS698_Project_TrelloPlus\frontend
```

Install dependencies (if not already done):
```powershell
npm install
```

Run development server:
```powershell
npm run dev
```

Frontend will start at: `http://localhost:5173` (or similar, check terminal output)

## 4. Stop Services

Stop PostgreSQL:
```powershell
docker stop flowboard-postgres
```

Stop backend and frontend: Press `Ctrl+C` in their respective terminals

## Summary

| Service | Command | Port |
|---------|---------|------|
| PostgreSQL | `docker run --name flowboard-postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres` | 5432 |
| Backend | `mvn spring-boot:run` | 8080 |
| Frontend | `npm run dev` | 5173 |