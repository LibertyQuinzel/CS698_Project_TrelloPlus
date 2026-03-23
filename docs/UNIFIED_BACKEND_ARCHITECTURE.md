# Unified Backend Architecture
## FlowBoard - AI-Powered Workflow Management System

**Date:** March 17, 2026  
**Version:** 1.0  
**Status:** Final  

---

## Executive Summary

This document describes the unified backend architecture that serves all three user stories of the FlowBoard system. The architecture is built on a single Spring Boot application backed by PostgreSQL, implemented with a clear separation of concerns that allows each workflow to operate independently while sharing a unified data model and infrastructure layer.

The key architectural principle is **layered data flow**: User Story 1 establishes the kanban board structure, User Story 2 generates changes from meeting discussions, and User Story 3 provides team control over which changes get applied. This creates a natural progression with clear integration points.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Core Design Principles](#core-design-principles)
3. [Layered Architecture](#layered-architecture)
4. [Data Flow Architecture](#data-flow-architecture)
5. [Cross-Workflow Integration](#cross-workflow-integration)
6. [Technology Stack](#technology-stack)
7. [Design Justifications](#design-justifications)
8. [Deployment Model](#deployment-model)
9. [Security Architecture](#security-architecture)
10. [Scalability Considerations](#scalability-considerations)

---

## Architecture Overview

### High-Level System Composition

The FlowBoard backend is organized into **four primary architectural layers**:

```
┌─────────────────────────────────────────────────────────────────┐
│                      REST API Layer                              │
│  /api/v1/projects  /api/v1/boards  /api/v1/meetings            │
│  /api/v1/changes   /api/v1/approvals  /api/v1/auth             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Application Layer (Services)                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │WF1: AI Board │  │WF2: Meeting  │  │WF3: Change   │          │
│  │Generation   │  │Summary       │  │Review        │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                 Domain Layer (Business Logic)                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Core Services: AIEngine, ApprovalService, etc.          │   │
│  │ Gateways: KanbanBoardGateway, MeetingGateway            │   │
│  │ Utilities: PromptBuilder, DiffCalculator, etc.          │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                  Data Access Layer (Repositories)                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐ │
│  │ UserRepository│  │BoardRepository│  │ChangeRepository     │ │
│  │ ProjectRepository│ CardRepository │ MeetingRepository     │ │
│  └──────────────┘  └──────────────┘  └──────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                  Persistence Layer (PostgreSQL)                  │
│  users | projects | boards | stages | cards | meetings |        │
│  changes | approval_requests | approval_decisions | ...         │
└─────────────────────────────────────────────────────────────────┘
```

---

## Core Design Principles

### 1. **Unified Identity Model**

A single `User` entity is shared across all three workflows:
- **Role-based access control** (ADMIN, MANAGER, MEMBER, VIEWER) applied consistently
- **JWT authentication** via a single AuthService
- **Audit trail** tracks which user performed each action

**Rationale:** This prevents role inconsistencies and ensures that team permissions remain synchronized across all features. A user approved as a MANAGER in WF2 is immediately trusted as a MANAGER in WF3.

### 2. **Unified Data Model**

Three core entities are established by WF1 and used by all workflows:

```
Project (created by WF1) ──┬─→ used by WF2 (meetings) 
                           ├─→ used by WF3 (change approval)
                           └─→ spans multiple boards

Board (created by WF1) ────┬─→ target for changes in WF3
                           ├─→ referenced by cards
                           └─→ contains stages and cards

Card (created by WF1) ─────┬─→ moved/updated by WF3
                           ├─→ created from AI analysis
                           └─→ belongs to a Stage
```

**Rationale:** WF1 establishes the foundational structure (boards and cards). WF2 and WF3 operate on the same entities without duplication. This prevents data inconsistency and ensures all workflows work with the same "source of truth."

### 3. **Layered Data Flow**

Each workflow owns specific responsibilities in a clear sequence:

```
WF1 writes: projects, boards, stages, cards
     ↓ (reads)
WF2 reads: projects, boards, cards (for context)
WF2 writes: changes, approval_requests (summary approval)
     ↓ (reads)
WF3 reads: changes (from WF2), boards, cards (for impact analysis)
WF3 writes: approval_decisions (change approval)
WF3 applies: changes back to cards
```

**Rationale:** This prevents circular dependencies and makes data dependencies explicit. Each workflow has clear input and output, making the system easier to reason about and test.

### 4. **Micro-service-like Service Layer**

Despite being a monolith, the application is organized with service boundaries that mimic microservices:

```
ProjectService (WF1)          ← stateless, handles project CRUD
AIEngine (WF1)                ← stateless, calls LLM APIs
BoardGenerator (WF1)          ← stateless, creates board from AI results

MeetingService (WF2)          ← stateless, manages meeting lifecycle
SummaryService (WF2)          ← orchestrates summary generation
ApprovalService (WF2 & WF3)   ← shared approval workflow logic

ChangePreviewService (WF3)    ← prepares changes for review
ChangeApplicationService (WF3)← applies approved changes to board
```

**Rationale:** This structure allows WF2 and WF3 to be developed, tested, and deployed as loosely-coupled components. The shared approval pattern (used by both WF2 for summary approval and WF3 for change approval) is abstracted into `ApprovalService`, reducing code duplication.

### 5. **Gateway Pattern for External Integration**

External dependencies are abstracted behind gateway interfaces:

```
LLMClient (gateway to OpenAI/Anthropic)
KanbanBoardGateway (local integration with boards)
MeetingGateway (integration with meeting system)
```

**Rationale:** Gateways decouple the core business logic from external dependencies. If we need to switch LLM providers, only the LLMClient changes, not the AIEngine or PromptBuilder.

### 6. **Explicit Approval Workflow Pattern**

Both WF2 and WF3 have approval workflows, but they are independent:

```
WF2 Approval: 
  User creates meeting → AI generates summary → 
  Team reviews and approves summary (consensus check) → 
  Summary approved, triggers change creation

WF3 Approval:
  Changes are loaded → Team reviews each change → 
  Team approves changes (quorum check) → 
  Changes applied to board
```

**Rationale:** Different workflows may have different approval rules. WF2 might require consensus from all attendees, while WF3 might require approval from only project leaders. By making approval pluggable, each workflow can implement its own business rules.

---

## Layered Architecture

### Layer 1: REST API Layer

**Responsibility:** HTTP routing and request/response translation

**Endpoints by Workflow:**

```
WF1 (AI Board Generation)
  POST   /api/v1/projects              → Create project
  GET    /api/v1/projects              → List projects
  GET    /api/v1/projects/{id}         → Get project details
  PUT    /api/v1/projects/{id}         → Update project
  POST   /api/v1/projects/{id}/analyze → Trigger AI analysis
  POST   /api/v1/boards                → Create board
  GET    /api/v1/boards/{id}           → Get board with cards
  PUT    /api/v1/boards/{id}           → Update board
  POST   /api/v1/cards                 → Create card
  PUT    /api/v1/cards/{id}            → Update card
  PUT    /api/v1/cards/{id}/move       → Move card to stage

WF2 (Meeting Summary & Checklist)
  POST   /api/v1/meetings              → Create meeting
  GET    /api/v1/meetings              → List meetings
  GET    /api/v1/meetings/{id}         → Get meeting details
  POST   /api/v1/meetings/{id}/end     → End meeting
  POST   /api/v1/summaries             → Generate summary from notes
  GET    /api/v1/summaries/{id}        → Get generated summary
  POST   /api/v1/approvals/summary     → Approve/reject summary
  GET    /api/v1/approvals/summary/{id}→ Get approval request status

WF3 (Change Review & Approval)
  GET    /api/v1/changes               → List pending changes
  GET    /api/v1/changes/{id}          → Get change details
  GET    /api/v1/changes/{id}/diff     → Get before/after diff
  GET    /api/v1/changes/{id}/impact   → Get impact analysis
  POST   /api/v1/changes/{id}/approve  → Approve change
  POST   /api/v1/changes/{id}/reject   → Reject change
  POST   /api/v1/changes/{id}/apply    → Apply approved change
  GET    /api/v1/changes/{id}/history  → Get audit trail

Shared Endpoints
  POST   /api/v1/auth/login            → Authenticate user
  POST   /api/v1/auth/register         → Create user account
  POST   /api/v1/auth/logout           → Invalidate session
  GET    /api/v1/users/{id}            → Get user profile
  PUT    /api/v1/users/{id}            → Update user profile
```

**Technology:** Spring Web (REST controllers)

### Layer 2: Application/Orchestration Layer

**Responsibility:** Request validation, orchestration, and response formatting

**Controllers and Services by Workflow:**

```
WF1 Components:
  ProjectInputController → ProjectService → databases
  BoardGenerator → StageFactory, WorkItemGenerator → databases
  
WF2 Components:
  MeetingController → MeetingService → databases
  SummaryInputController → SummaryService → AIEngine → databases
  ApprovalController (WF2) → ApprovalService → databases
  
WF3 Components:
  ChangePreviewController → ChangePreviewService → databases
  ApprovalController (WF3) → ApprovalService (shared) → databases
  DiffController → DiffCalculator → utilities
  ImpactController → ImpactAnalyzer → utilities
```

**Design Pattern:** Command/Orchestrator pattern - each controller delegates to a service that orchestrates the workflow

**Technology:** Spring Service components with Spring Dependency Injection

### Layer 3: Domain/Business Logic Layer

**Responsibility:** Core business logic, validation, and enterprise rules

**Core Services:**

```
AIEngine
  - Calls LLMs to analyze project descriptions and meeting summaries
  - Returns structured data (board configs, action items, decisions)
  - Uses PromptBuilder to optimize prompts
  - Implements multi-provider fallback (OpenAI → Anthropic)

ApprovalService (shared by WF2 and WF3)
  - Implements consensus/quorum logic
  - Evaluates approval rules
  - Records approval decisions
  - Notifies observers

ChangeApplicationService
  - Applies approved changes to board
  - Transactional - all or nothing
  - Creates snapshots for rollback
  - Validates board integrity after application

ContentStructurer
  - Parses raw LLM output into structured objects
  - Validates consistency
  - Creates domain model instances
```

**Utility Services:**

```
PromptBuilder
  - Constructs optimized prompts for LLMs
  - Incorporates few-shot examples
  - Handles prompt templating

DiffCalculator
  - Computes before/after diffs
  - Identifies specific field changes
  - Highlights key differences for UI

ImpactAnalyzer
  - Analyzes change impact on cards/stages
  - Identifies potential conflicts
  - Assesses risk levels
  - Estimates effort needed

ConflictResolver
  - Detects conflicts between concurrent changes
  - Suggests resolutions
  - Prevents invalid state transitions
```

**Gateways (Adapters):**

```
LLMClient
  - Wraps OpenAI and Anthropic APIs
  - Handles retries, rate limiting, timeouts
  - Implements circuit breaker pattern

KanbanBoardGateway
  - Integrates with local board state
  - Ensures referential integrity
  - Validates board constraints

MeetingGateway
  - Loads meeting context for change generation
  - Retrieves meeting participants
  - Validates meeting state
```

**Technology:** Plain Java classes with Spring annotation-based dependency injection

### Layer 4: Data Access Layer (Repository Pattern)

**Responsibility:** Persistence operations and database abstraction

**Repositories:**

```
UserRepository
  - CRUD operations for users
  - Query by email, username
  - Password hashing via Spring Security

ProjectRepository
  - CRUD for projects
  - Query by owner, team
  - Soft delete support

BoardRepository
  - CRUD for boards
  - Query by project
  - Includes stages and cards on load

CardRepository
  - CRUD for cards
  - Bulk operations for batch changes
  - Position/ordering support

MeetingRepository & related
  - MeetingSessionRepository
  - MeetingNoteRepository
  - MeetingSummaryRepository

ChangeRepository
  - CRUD for changes
  - Query by status, meeting
  - Workflow state persistence

ApprovalRepository
  - Approval requests
  - Approval decisions
  - Query by change, approver, status

AuditRepository
  - Immutable audit trail
  - Query by entity ID, action, user, date range
```

**Technology:** Spring Data JPA with custom query methods

### Layer 5: Persistence Layer (PostgreSQL)

**Responsibility:** Data durability and consistency

**Core Tables:**

```
Schema: public

users (shared by all workflows)
  - id (UUID)
  - email (unique)
  - password_hash
  - username
  - role (ADMIN, MANAGER, MEMBER, VIEWER)
  - created_at, updated_at

projects (created by WF1, used by WF2, WF3)
  - id (UUID)
  - name
  - description
  - owner_id (FK: users.id)
  - created_at, updated_at

boards (created by WF1, modified by WF3)
  - id (UUID)
  - project_id (FK: projects.id)
  - title
  - description
  - created_at, updated_at

stages (created by WF1, referenced by WF3)
  - id (UUID)
  - board_id (FK: boards.id)
  - name (todo, in_progress, done, etc.)
  - position (ordering)
  - created_at

cards (created by WF1, modified by WF3)
  - id (UUID)
  - board_id (FK: boards.id)
  - stage_id (FK: stages.id)
  - title
  - description
  - created_at, updated_at

meeting_sessions (created by WF2)
  - id (UUID)
  - project_id (FK: projects.id)
  - created_by (FK: users.id)
  - participants (JSON array of user IDs)
  - status (IN_PROGRESS, COMPLETED)
  - created_at, ended_at

changes (created by WF2, approved/rejected by WF3)
  - id (UUID)
  - meeting_id (FK: meeting_sessions.id)
  - change_type (MOVE_CARD, UPDATE_CARD, CREATE_CARD, DELETE_CARD)
  - current_state (JSON)
  - proposed_state (JSON)
  - status (PENDING, APPROVED, REJECTED, APPLIED)
  - created_at, applied_at

approval_requests (WF2)
  - id (UUID)
  - summary_id
  - status (PENDING, APPROVED, REJECTED)
  - created_at, deadline

approval_responses (WF2)
  - id (UUID)
  - approval_id (FK: approval_requests.id)
  - responder_id (FK: users.id)
  - decision (APPROVE, REJECT)
  - feedback
  - responded_at

approval_decisions (WF3)
  - id (UUID)
  - change_id (FK: changes.id)
  - approver_id (FK: users.id)
  - decision (APPROVE, REJECT)
  - feedback
  - decided_at

audit_log (all workflows)
  - id (UUID)
  - entity_type (Change, ApprovalRequest, etc.)
  - entity_id (UUID)
  - action (CREATE, UPDATE, DELETE, APPROVE, REJECT, APPLY)
  - actor_id (FK: users.id)
  - details (JSON)
  - created_at
```

**Indices:**
- `idx_users_email` on `users(email)` for auth
- `idx_projects_owner` on `projects(owner_id)` for user dashboard
- `idx_boards_project` on `boards(project_id)` for project boards
- `idx_cards_board_stage` on `cards(board_id, stage_id)` for board rendering
- `idx_changes_status` on `changes(status)` for pending changes list
- `idx_audit_entity` on `audit_log(entity_type, entity_id)` for history

---

## Data Flow Architecture

### Workflow 1: AI Board Generation

```
User submits project description
  ↓
ProjectInputController validates input
  ↓
ProjectService.createProject() creates Project record
  ↓
AIEngine.analyzeProject() calls LLM
  ↓
LLMClient makes HTTP request to OpenAI/Anthropic
  ↓
ContentStructurer parses LLM response → BoardTemplate
  ↓
BoardGenerator creates Board, Stages, and Cards
  ↓
[Database: projects, boards, stages, cards tables updated]
  ↓
Response returned: Board with 3 stages and 12 pre-populated cards
```

### Workflow 2: Meeting Summary & Checklist

```
Meeting facilitator creates meeting
  ↓
MeetingController creates Meeting record
  ↓
[Team discusses, notes captured]
  ↓
Meeting facilitator ends meeting + submits summary
  ↓
SummaryInputController validates summary text
  ↓
SummaryService.generateSummary()
  ↓
AIEngine.analyzeSummary() calls LLM
  ↓
ContentStructurer parses response → ActionItems, Decisions, Changes
  ↓
[Database: meeting_sessions, changes tables created]
  ↓
ApprovalService creates approval request
  ↓
Team members review and vote
  ↓
[Database: approval_responses records created]
  ↓
Once consensus reached:
  - Summary marked as APPROVED
  - Changes marked as READY_FOR_WF3
```

### Workflow 3: Change Review & Approval

```
Team member navigates to "Review Changes"
  ↓
ChangePreviewController loads changes from WF2
  ↓
[SELECT * FROM changes WHERE status = 'READY_FOR_WF3']
  ↓
For each change:
  - DiffCalculator computes before/after state
  - ImpactAnalyzer determines affected cards/stages
  - ConflictResolver detects conflicts
  ↓
UI displays changes with visualization
  ↓
Team member reviews and approves/rejects individual changes
  ↓
ApprovalController records decision
  ↓
[Database: approval_decisions records created]
  ↓
When all approvals ready:
  - ChangeApplicationService.prepareApplication()
  - TransactionManager.beginTransaction()
  - For each approved change:
    - Apply to database
    - Validate constraint
  - On any failure → rollback
  - On success → commit
  ↓
[Database: cards table updated, changes marked as APPLIED]
  ↓
Team member sees "Changes Applied Successfully"
  ↓
Kanban board now reflects changes from meeting
```

### Cross-Workflow Data Dependencies

```
┌─────────────────────────────────────────────────────────────────┐
│ WF1: AI Board Generation                                        │
│ Outputs:                                                        │
│  • projects (1 record)                                          │
│  • boards (1-N records)                                         │
│  • stages (3-5 records per board)                               │
│  • cards (N records per stage)                                  │
└──────────────────────────┬──────────────────────────────────────┘
                           │ WF2 reads project context
                           │ WF3 reads board/stage/card state
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│ WF2: Meeting Summary & Checklist                                │
│ Inputs:                                                         │
│  • projects (for context)                                       │
│  • boards (for impact assessment)                               │
│ Outputs:                                                        │
│  • meeting_sessions (1 record)                                  │
│  • changes (N records, each has current_state + proposed_state)│
│  • approval_requests (1 record)                                 │
│  • approval_responses (M records)                               │
└──────────────────────────┬──────────────────────────────────────┘
                           │ WF3 reads changes
                           │ WF3 writes back to cards
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│ WF3: Change Review & Approval                                   │
│ Inputs:                                                         │
│  • changes (from WF2)                                           │
│  • cards (from WF1, for current state)                          │
│  • boards (from WF1, for constraints)                           │
│ Outputs:                                                        │
│  • approval_decisions (N records)                               │
│  • cards (updated)                                              │
│  • changes (marked as APPLIED)                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Cross-Workflow Integration

### Integration Point 1: WF1 → WF2

**Connection:** Project and Board Reference

```
WF1 creates Project and Board
  ↓
WF2 MeetingService accepts project_id
  ↓
AIEngine uses board structure for context:
  "Here are the current stages: [todo, in_progress, done]
   Here are the current cards: [Card A, Card B, ...]
   In your meeting, which cards need to move? Any new cards needed?"
  ↓
This gives the LLM context about the current kanban state
```

**Why:** The LLM generates appropriate changes only if it understands the existing board structure.

### Integration Point 2: WF2 → WF3

**Connection:** Change Records

```
WF2 SummaryService.generateSummary()
  ↓
ContentStructurer creates Change objects:
  {
    id: UUID,
    meeting_id: UUID,
    change_type: "MOVE_CARD",
    current_state: {card_id: "abc", stage_id: "todo"},
    proposed_state: {card_id: "abc", stage_id: "in_progress"}
  }
  ↓
[INSERT INTO changes ...]
  ↓
WF3 queries: SELECT * FROM changes WHERE status = 'READY_FOR_WF3'
  ↓
Same Change records are displayed to user for approval/rejection
```

**Why:** Changes are the contract between workflows. WF2 says "I suggest this change" and WF3 says "I approve/reject it."

### Integration Point 3: WF3 → WF1

**Connection:** Applied Changes Back to Board

```
WF3 ChangeApplicationService.applyChanges()
  ↓
For each approved change:
  - Extract card_id, new_stage_id from proposed_state
  - UPDATE cards SET stage_id = ? WHERE id = ?
  ↓
Board in WF1 now reflects the changes
  ↓
Next time user views the board, they see the changes
```

**Why:** The cycle completes - meeting decisions become actual board changes.

---

## Technology Stack

### Backend Framework

| Component | Technology | Version | Justification |
|-----------|-----------|---------|---------------|
| Language | Java | 21 LTS | Enterprise adoption, strong typing, performance |
| Framework | Spring Boot | 3.2.0 | Industry standard, extensive ecosystem, production-proven |
| Dependency Injection | Spring Core | 6.1.0 | Enables loose coupling, testability |
| Web Framework | Spring Web | 6.1.0 | RESTful API support, proven in production |
| Persistence | Spring Data JPA | 3.2.0 | Reduces boilerplate, supports repositories pattern |
| Database | PostgreSQL | 16.1 | ACID compliance, JSON support, advanced indexing |
| Migrations | Flyway | 10.x | Version-controlled migrations, reliable |
| Authentication | Spring Security | 6.2.0 | Industry standard, flexible, well-documented |
| Token Management | jose4j | 0.9.3 | JWT standards-compliant, well-tested |

### AI/ML Integration

| Component | Technology | Justification |
|-----------|-----------|---------------|
| Primary LLM | OpenAI GPT-4 Turbo | Best-in-class performance, structured output |
| Fallback LLM | Anthropic Claude 3 | High-quality output, long context window |
| LLM Abstraction | LangChain4j | 0.24.0 | Multi-provider support, reduces vendor lock-in |
| Prompt Organization | Custom PromptBuilder | Type-safe, testable prompts |

### Testing

| Component | Technology | Justification |
|-----------|-----------|---------------|
| Unit Tests | JUnit 5 | Industry standard, parameterized tests |
| Integration Tests | Testcontainers | Real dependencies, isolated tests |
| Mocking | Mockito | Simple, popular, easy to learn |

### Build & Deployment

| Component | Technology | Justification |
|-----------|-----------|---------------|
| Build Tool | Gradle 8.5 | Fast, flexible, Kotlin DSL |
| Containerization | Docker | Industry standard, registry ecosystem |
| Orchestration | Kubernetes (optional) | Scales horizontally, self-healing |

---

## Design Justifications

### Why a Monolithic Architecture (Not Microservices)?

At inception, a monolith is the correct choice because:

1. **Operational Simplicity:** Single deployment, single database, single monitoring dashboard
2. **Data Consistency:** ACID transactions across workflows prevent inconsistencies
3. **Reduced Latency:** No network calls between services
4. **Easier Debugging:** Call stacks are simpler, logs are co-located
5. **Cost:** Single database, single server (initially)

**Escape Plan:** The service-oriented design (clear service boundaries, gateways) makes it straightforward to split into microservices later if needed. Each workflow (WF1, WF2, WF3) could become its own service.

### Why Unified User Model (Not Multiple Auth Systems)?

Storing roles in a single `users` table ensures:

1. **Single Source of Truth:** Role changes are immediately visible everywhere
2. **Simpler Access Control:** Permission checks use the same RBAC logic
3. **Audit Trail:** Single audit log captures all decisions
4. **No Sync Issues:** No separate user provisioning needed

Consequence: WF2 and WF3 must trust the role assigned in the unified system.

### Why Shared Approval Service?

Both WF2 (summary approval) and WF3 (change approval) need approval workflows:

```
WF2: All meeting participants must approve the summary
WF3: Project leaders must approve changes
```

Rather than duplicate code, we have a single `ApprovalService` with pluggable rules:

```java
ApprovalService {
  evaluateApprovalRules(ApprovalRequest request) 
    → Depends on rule configuration
    → WF2 uses rule: "all_attendees_approve"
    → WF3 uses rule: "quorum_of_leaders"
}
```

This reduces code duplication and makes business rule changes easier.

### Why the Change Record (Not Direct Board Updates)?

When WF2 generates a change, it could directly update the board. Instead, it creates a Change record that WF3 must approve first:

```
Option A: WF2 directly updates board
  Pros: Faster, fewer DB tables
  Cons: User loses control, errors can't be caught, audit trail harder

Option B: WF2 creates Change, WF3 approves (chosen)
  Pros: User control, error catching, clear audit trail
  Cons: Extra DB table, extra API call
```

The design prioritizes **user control** and **auditability**. A mistake in meeting summary should be caught before the board changes.

### Why Immutable Audit Log?

The `audit_log` table never has UPDATE or DELETE operations:

```sql
-- Allowed:
INSERT INTO audit_log (action, entity_id, actor_id, ...)

-- Never:
UPDATE audit_log SET ...  -- NOT ALLOWED
DELETE FROM audit_log WHERE ...  -- NOT ALLOWED
```

This ensures that the complete history is preserved forever. If a user claims "I didn't approve that change," we can prove they did (or didn't) from the audit log.

### Why JSON Columns for Change State?

Changes store `current_state` and `proposed_state` as JSON:

```json
{
  "id": "card-123",
  "stage_id": "todo"
}
```

Rather than strict columns:

```sql
current_stage_id VARCHAR,
proposed_stage_id VARCHAR
```

**Rationale:** A change might involve multiple fields:
- Move card to new stage
- Update card title
- Update card description
- All in one atomic change

JSON provides flexibility without creating a complex schema.

---

## Deployment Model

### Development Environment

```
Docker Compose with:
  - Spring Boot app (localhost:8080)
  - PostgreSQL database (localhost:5432)
  - Optional: pgAdmin for database browsing

Run: docker-compose up
```

### Staging Environment

```
AWS ECS with:
  - 1 Spring Boot container (can scale to 3)
  - RDS PostgreSQL (multi-AZ for availability)
  - ALB for load balancing
  - CloudWatch for monitoring
  - CloudFormation for infrastructure as code
```

### Production Environment

```
AWS ECS on Fargate with:
  - 3+ Spring Boot containers (auto-scales 1-10 based on load)
  - RDS PostgreSQL (Multi-AZ + Read Replicas)
  - Application Load Balancer with TLS
  - CloudFront CDN for static assets
  - CloudWatch + DataDog for monitoring
  - VPC with private subnets for database
  - Secrets Manager for API keys
  - Auto-scaling based on CPU/memory
```

### Database Backup Strategy

```
Production PostgreSQL:
  - Automated snapshots: every 6 hours
  - Retention: 30 days
  - Cross-region replication: US East and US West
  - Point-in-time recovery: 7 days of WAL files

Recovery Time Objective: 1 hour
Recovery Point Objective: 6 hours
```

---

## Security Architecture

### Authentication Flow

```
User submits credentials (email, password)
  ↓
AuthController.login()
  ↓
AuthService.authenticate()
  ├─ Fetch user from database
  ├─ Verify password (bcrypt)
  └─ Generate JWT token
  ↓
Return: JWT token with claims {user_id, email, role}
  ↓
Client stores JWT in secure httpOnly cookie
  ↓
Subsequent requests include JWT in Authorization header
  ↓
Spring Security filter validates token signature
  ↓
Request proceeds with authenticated user context
```

### Authorization

```
@Secured("ROLE_MANAGER")  // Only MANAGER and ADMIN can call
public BoardDTO generateBoard(CreateBoardRequest request) { ... }

Permission checks happen at:
  1. REST controller layer (coarse-grained)
  2. Service layer (fine-grained business rules)
  3. Repository layer (never return unauthorized data)
```

### API Security

| Threat | Mitigation |
|--------|-----------|
| CSRF | SameSite cookie attribute, CSRF tokens in POST body |
| SQL Injection | Parameterized queries via Spring Data JPA |
| XSS | Content Security Policy headers, output encoding |
| Unauthorized access | JWT validation, role-based access control |
| Replay attacks | Nonce validation, timestamp checking |
| Rate limiting | Spring Security rate limiting, IP-based throttling |
| Password attacks | bcrypt hashing with salt, login attempt limiting |

### Data Security

| Component | Protection |
|-----------|-----------|
| Passwords | bcrypt (cost factor 12) |
| JSON Web Tokens | HMAC-SHA256 signing, 24-hour expiration |
| Database | PostgreSQL SSL connections, least-privilege accounts |
| API keys (LLM) | Stored in AWS Secrets Manager, rotated quarterly |
| Sensitive fields | Encrypted at rest in database (future: full-disk encryption) |

---

## Scalability Considerations

### Current Capacity

```
Single Spring Boot instance (2 CPU, 4GB RAM) can handle:
  - ~500 concurrent users
  - ~10,000 requests/second peak
  - 50 project boards with 1000s of cards

Bottleneck scenarios:
  - LLM response times (OpenAI API rate limits)
  - Database connection pool exhaustion
  - Large dataset imports
```

### Scaling Strategy for 10x Growth

```
Tier 1: Caching
  - Redis for session storage, reducing database hits
  - Application-level caching for board/project queries
  - LLM response caching (same project description = same board)

Tier 2: Database
  - Read replicas for reporting/analytics
  - Connection pooling optimization
  - Materialized views for expensive queries

Tier 3: Horizontal Scaling
  - 3-5 Spring Boot instances behind ALB
  - Stateless services (sessions in Redis)
  - Database becomes bottleneck, move to Tier 4

Tier 4: Sharding/Partitioning
  - Shard by tenant/project
  - Separate database per customer tier
  - Microservices split (WF1 ← WF2 → WF3)
```

### Load Specifications (SLA)

```
P95 Latency: < 500ms for API endpoints
P99 Latency: < 2s for API endpoints
Availability: 99.9% uptime

During peak (end of week):
  - 1000 concurrent users
  - 500 LLM calls/hour
  - 10,000 board card movements/hour
```

---

## Monitoring & Observability

### Key Metrics

```
Application Metrics:
  - API latency (by endpoint)
  - Request count (by status code)
  - LLM call duration and success rate
  - Database query duration

Infrastructure Metrics:
  - CPU usage
  - Memory usage
  - Disk I/O

Alerts:
  - API error rate > 1% → investigate
  - LLM timeout > 10% of calls → degraded mode
  - Database CPU > 80% → scale
  - Latency P95 > 1s → investigate
```

### Logging

```
Log Levels:
  - ERROR: Exceptions, failed operations, security issues
  - WARN: Retries, timeouts, unexpected conditions
  - INFO: Workflow milestones (user login, board created, changes applied)
  - DEBUG: Object state, decision points (dev only)

Log Aggregation:
  - All logs → CloudWatch Logs
  - CloudWatch Logs → DataDog for visualization
  - Retention: 30 days hot, 1 year archived
```

---

## Future Enhancements

### Potential Extensions

1. **Multi-Tenancy:** Separate data per customer, shared infrastructure
2. **Real-time Collaboration:** WebSockets for live board updates
3. **Advanced Scheduling:** Integration with calendar systems for meeting times
4. **Custom Workflows:** Configurable approval chains, custom approval rules
5. **Analytics:** Dashboard showing which changes are approved most frequently
6. **Notifications:** Email/Slack alerts when approval needed, change applied
7. **Undo/Rollback:** Manual change rollback after application
8. **API Versioning:** Multiple API versions for backward compatibility

---

## Conclusion

This unified architecture provides a single, cohesive backend that serves all three user stories while maintaining clear separation of concern and preparing for future growth. The design prioritizes:

1. **User Control** - Explicit approval workflows prevent errors
2. **Data Consistency** - ACID transactions ensure no corrupted states
3. **Auditability** - Complete history of all decisions
4. **Extensibility** - Service boundaries make it easy to add features
5. **Simplicity** - Monolith is operationally simpler than microservices (for now)

The architecture has been validated against the specifications and is ready for implementation.
