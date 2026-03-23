# P4 Backend Concurrency Verification (10 Users)

This document verifies the backend requirement of supporting 10 simultaneous frontend users.

## Scope

- Target concurrency: 10 simultaneous users
- Workflows covered:
  - Authentication (register/login)
  - Board/project write operations
  - Approval write operations
- Expected behavior:
  - No server crash
  - No data corruption
  - Concurrent update collisions produce HTTP 409 conflict when optimistic locking triggers

## Preconditions

1. Start database:

```bash
docker compose -f docker-compose.db.yml up -d
```

2. Start backend with development profile:

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

3. Confirm backend reachable at http://localhost:8080/api/v1.

## Test A: 10-User Concurrent Authentication Smoke Test

Run:

```bash
bash backend/scripts/concurrency_10_users_smoke.sh
```

Pass criteria:
- 10 concurrent login attempts complete successfully
- No 5xx responses
- Backend remains responsive after test

## Test B: Concurrent Board Updates

1. Create one shared project and board with 10 test users.
2. Trigger 10 concurrent board mutation requests (move/update/create card) from 10 sessions.
3. Validate all successful writes are consistent and no duplicate or corrupt card state appears.

Pass criteria:
- Mutations complete without data corruption
- Conflict scenarios return deterministic errors instead of partial writes

## Test C: Concurrent Approval Decisions

1. Create a meeting summary/change approval requiring multiple responses.
2. Submit concurrent approval responses from 10 users.
3. Re-submit one response race intentionally from two clients for same user.

Pass criteria:
- One response wins; stale update path returns HTTP 409 conflict
- Meeting/change status transitions remain valid
- Audit entries remain consistent with final state

## Observability Checklist

- Check backend logs for exceptions and 5xx spikes.
- Confirm no optimistic lock conflict is surfaced as 500.
- Confirm conflict responses include a retry-friendly message.

## Notes for P4 Deliverable

- External AI services remain mocked for this sprint.
- The same tests can be repeated in P5 with AWS-backed infrastructure.
