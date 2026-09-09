# WorkFlow360 — Java + React Development Master Plan

> **Project type:** Enterprise Employee, Project, Resource, Task, Timesheet, Leave and Reporting Platform  
> **Primary stack:** Java + Spring Boot + React + TypeScript + PostgreSQL  
> **Architecture path:** Modular Monolith → Production Monolith → Event-Driven Architecture → Microservices  
> **Learning method:** Learn → Implement → Break → Debug → Test → Improve → Document

---

## 1. Project Goal

Build **WorkFlow360** as a portfolio-quality enterprise application that demonstrates:

- Java and Spring Boot backend engineering
- React and TypeScript frontend development
- Relational database design with PostgreSQL
- REST API design and documentation
- Authentication, RBAC and permission-based authorization
- Unit, integration and end-to-end testing
- Real-time communication and notifications
- Redis caching and Kafka-based event processing
- Docker, CI/CD, AWS and production observability

The project must begin as a **modular monolith**. Microservices will be introduced only after the business modules and boundaries are stable.

---

## 2. Final Technology Stack

### Backend

- Java LTS
- Maven
- Spring Boot
- Spring Web
- Spring Data JPA and Hibernate
- Spring Security
- Bean Validation
- Spring Actuator
- Spring WebSocket and STOMP
- Spring Cache and Redis
- Spring Kafka
- OpenAPI/Swagger
- JUnit 5, Mockito and Testcontainers

### Frontend

- React
- TypeScript
- Vite
- React Router
- TanStack Query
- React Hook Form
- Zod
- Axios or Fetch API
- WebSocket/STOMP client
- Vitest and React Testing Library
- Playwright

### Data and infrastructure

- PostgreSQL
- Redis
- Kafka
- Docker and Docker Compose
- GitHub Actions
- AWS

### Observability

- Spring Boot Actuator
- Micrometer
- Prometheus
- Grafana
- OpenTelemetry
- Centralized logging

---

## 3. Architecture Rules

1. Do not start with microservices.
2. Keep business logic out of controllers.
3. Do not expose JPA entities from REST controllers.
4. Validate on the backend even when React also validates.
5. Enforce authorization on the backend; protected React routes are not the security boundary.
6. Never commit passwords, tokens or keys.
7. Introduce Redis, Kafka and WebSockets only when a completed feature needs them.
8. Add tests and documentation as part of each feature—not at the end.
9. Measure performance before optimizing.
10. Every new technology must solve a clearly stated engineering problem.

---

## 4. Repository Structure

```text
workflow360/
├── frontend/
├── backend/
├── infrastructure/
│   ├── docker/
│   ├── postgres/
│   ├── redis/
│   └── kafka/
├── docs/
│   ├── plans/
│   ├── progress/
│   ├── architecture/
│   ├── api/
│   ├── database/
│   ├── security/
│   ├── testing/
│   └── deployment/
├── .github/
│   └── workflows/
├── docker-compose.yml
├── README.md
└── .gitignore
```

### Required planning files

```text
docs/
├── plans/
│   ├── 00-master-roadmap.md
│   ├── 01-project-foundation.md
│   ├── 02-authentication.md
│   ├── 03-rbac-permissions.md
│   ├── 04-employee-department.md
│   ├── 05-project-team.md
│   ├── 06-task-management.md
│   ├── 07-timesheet-leave.md
│   ├── 08-resource-allocation.md
│   ├── 09-notification-reporting.md
│   ├── 10-realtime-features.md
│   ├── 11-redis-performance.md
│   ├── 12-kafka-event-driven.md
│   ├── 13-microservices.md
│   ├── 14-devops-aws.md
│   └── 15-production-hardening.md
└── progress/
    ├── development-log.md
    ├── decisions.md
    ├── bugs-and-learning.md
    └── release-notes.md
```

---

## 5. Backend Package Structure

```text
backend/src/main/java/com/workflow360/
├── common/
│   ├── config/
│   ├── exception/
│   ├── security/
│   ├── validation/
│   ├── audit/
│   └── util/
├── auth/
├── user/
├── organization/
├── department/
├── employee/
├── role/
├── permission/
├── skill/
├── project/
├── team/
├── task/
├── timesheet/
├── leave/
├── allocation/
├── notification/
├── chat/
├── document/
├── report/
└── audit/
```

Use this internal structure only when the module needs each layer:

```text
employee/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
│   ├── request/
│   └── response/
├── mapper/
├── validation/
└── exception/
```

---

## 6. Frontend Structure

```text
frontend/src/
├── app/
├── routes/
├── layouts/
├── components/
├── features/
│   ├── auth/
│   ├── dashboard/
│   ├── employees/
│   ├── departments/
│   ├── projects/
│   ├── tasks/
│   ├── timesheets/
│   ├── leave/
│   ├── allocations/
│   ├── notifications/
│   ├── chat/
│   ├── reports/
│   └── admin/
├── api/
├── hooks/
├── services/
├── types/
├── utils/
└── styles/
```

Prefer feature-based organization. Shared UI components belong in `components/`; business-specific components stay inside their feature.

---

## 7. Development Roadmap

## Level 1 — Foundation

**Objective:** Establish a working React → Spring Boot → PostgreSQL flow.

Build:

- Repository and folder structure
- Spring Boot application
- React + TypeScript application
- PostgreSQL connection
- Environment-specific configuration
- Health endpoint
- Basic frontend routing and layout
- Git workflow and documentation structure

**Exit criteria:** React can call a Spring Boot API, Spring Boot can access PostgreSQL, and the project starts using documented commands.

## Level 2 — Professional Backend and Frontend Practices

Build:

- Request and response DTOs
- Bean Validation and Zod validation
- Mapping between DTOs and entities
- Centralized exception handling
- Consistent API error schema
- Logging
- Pagination, sorting, filtering and search
- OpenAPI/Swagger
- Unit and integration tests
- Frontend loading, error and empty states

**Exit criteria:** APIs are validated, documented, tested and do not expose entities.

## Level 3 — Authentication and Authorization

Build in this order:

1. Local email/password authentication
2. Password hashing
3. Login and logout
4. Access and refresh/session strategy
5. Protected APIs
6. User, Role and Permission models
7. RBAC and permission checks
8. Protected React routes
9. Google OAuth 2.0/OpenID Connect
10. Account lockout, failed-login tracking and audit events

**Exit criteria:** Admin, Manager and Employee users see and perform only authorized actions.

## Level 4 — Core Enterprise Features

Build:

- Organizations
- Employees
- Departments
- Skills
- Projects
- Teams and project membership
- Tasks, comments and attachments
- Timesheets
- Leave
- Resource allocation
- Notifications
- Reports and dashboards
- Audit logs

**Exit criteria:** The first production-style monolith supports the main Employee, Manager and Admin journeys.

## Level 5 — Real-Time Features

Build:

- WebSocket/STOMP foundation
- Live task notifications
- Live comments
- Direct and group chat
- Project conversations
- Online presence
- Typing indicators
- Read receipts

**Exit criteria:** Authorized users receive reliable real-time updates, and reconnect/error scenarios are tested.

## Level 6 — Performance and Reliability

Build:

- Database indexes
- Query analysis and N+1 prevention
- Redis caches with documented TTL and invalidation
- Rate limiting
- Asynchronous processing
- Optimistic locking
- Idempotency where required
- Load and performance tests

**Exit criteria:** Improvements have before/after measurements and do not compromise correctness.

## Level 7 — Event-Driven Architecture

Build:

- Stable business-event definitions
- Kafka topics and consumers
- Outbox pattern
- Retry strategy
- Dead-letter topics
- Idempotent consumers
- Event-driven notifications, audit and reporting

**Exit criteria:** Events are observable, retryable and safe under duplicate delivery.

## Level 8 — Microservices

Extract in this recommended order:

1. Notification Service
2. Chat Service
3. Identity/Auth Service
4. Employee Service
5. Project and Task Services
6. Timesheet, Leave and Allocation Services
7. Reporting and Audit Services

Then add:

- API Gateway
- Service-specific databases
- Centralized configuration where justified
- HTTP clients or messaging between services
- Resilience4j
- Distributed tracing
- Saga/eventual consistency patterns

**Exit criteria:** Extraction is justified by an actual boundary; the system remains observable and testable.

## Level 9 — DevOps and Cloud

Build:

- Dockerfiles
- Docker Compose development environment
- GitHub Actions build and test pipeline
- Security and dependency scans
- Container image publishing
- Staging deployment
- AWS deployment
- Smoke tests and production approval

## Level 10 — Production Hardening

Build:

- Metrics, logs and traces
- Liveness and readiness checks
- Dashboards and alerts
- Secret management
- Backups and restore testing
- Security review
- Load testing
- Disaster recovery plan
- Performance tuning

---

## 8. Release Plan

### Release 1 — MVP

- Email/password authentication
- Google login
- Users
- Roles and permissions
- Employees
- Departments
- Projects and teams
- Tasks
- Dashboard
- Basic notifications
- PostgreSQL
- REST APIs
- React UI
- Backend and frontend tests

### Release 2 — Enterprise Operations

- Timesheets
- Leave
- Resource allocation
- Task comments and attachments
- Audit logs
- Email
- Advanced reports

### Release 3 — Real-Time Collaboration

- WebSocket notifications
- Chat
- Presence
- Typing indicators
- Read receipts
- Redis support

### Release 4 — Event-Driven System

- Kafka
- Outbox pattern
- Async notifications
- Event-driven audit
- Retry and dead-letter handling
- Idempotency

### Release 5 — Distributed Architecture

- Microservices
- API Gateway
- Independent databases
- Resilience patterns
- Containerized services

### Release 6 — Cloud and Production

- AWS
- CI/CD
- Monitoring and tracing
- Load testing
- Production hardening

---

## 9. Immediate Starting Plan — Sprint 0

**Duration:** 3–5 focused study/development days  
**Purpose:** Set up the environment and create a reproducible project skeleton. Do not build authentication yet.

### Day 1 — Prerequisites

Check or install:

- Java LTS
- Maven
- Node.js LTS and npm
- Git
- Docker and Docker Compose
- PostgreSQL or PostgreSQL container
- IntelliJ IDEA or another Java IDE
- VS Code
- API client such as Bruno or Postman

Record exact installed versions in `docs/progress/development-log.md`.

### Day 2 — Repository foundation

Create:

```text
workflow360/
├── frontend/
├── backend/
├── docs/
└── infrastructure/
```

Then add:

- Root `README.md`
- Root `.gitignore`
- `develop` branch
- Initial documentation folders
- First conventional commit

Suggested commit:

```text
chore(project): initialize workflow360 repository
```

### Day 3 — Spring Boot foundation

Create the backend with:

- Spring Web
- Spring Data JPA
- PostgreSQL Driver
- Validation
- Actuator
- Spring Boot Test

Initial backend tasks:

- Set application name
- Add `local`, `test` and `prod` configuration approach
- Read database settings from environment variables
- Expose a health endpoint
- Add a simple `/api/v1/system/status` endpoint
- Confirm startup
- Add one basic controller test

### Day 4 — React foundation

Create React + TypeScript with Vite.

Initial frontend tasks:

- Configure React Router
- Create application and authentication layouts
- Add placeholder Login and Dashboard pages
- Create API client configuration
- Read API base URL from environment settings
- Call `/api/v1/system/status`
- Add loading, success and error states
- Add one component test

### Day 5 — PostgreSQL and integration

- Start PostgreSQL locally or through Docker
- Create a dedicated application database and user
- Connect Spring Boot to PostgreSQL
- Introduce database migration tooling before creating business tables
- Create one migration for a harmless foundation table or schema marker
- Run backend and frontend together
- Document all commands
- Commit the completed foundation

Suggested commit:

```text
feat(foundation): connect react spring boot and postgres
```

---

## 10. Sprint 0 Definition of Done

- [ ] Git repository created
- [ ] Required folder structure exists
- [ ] Backend starts successfully
- [ ] Frontend starts successfully
- [ ] PostgreSQL starts successfully
- [ ] Spring Boot connects to PostgreSQL
- [ ] React calls a versioned backend endpoint
- [ ] Health endpoint works
- [ ] Environment variables are documented
- [ ] No secrets are committed
- [ ] At least one backend test passes
- [ ] At least one frontend test passes
- [ ] Setup commands are documented in `README.md`
- [ ] Changes are committed using meaningful messages

---

## 11. Next Milestone — Security Foundation

After Sprint 0, implement this sequence:

```text
User registration or admin-created user
           ↓
Email/password login
           ↓
Password hashing
           ↓
Authenticated session/token strategy
           ↓
User → Role → Permission
           ↓
Backend authorization
           ↓
Protected React routes
           ↓
Dashboard by role
           ↓
Google OpenID Connect
```

Do not begin Employee, Project or Task functionality until the basic authentication and authorization flow is working and tested.

---

## 12. Feature Development Workflow

For every feature, follow this sequence:

```text
Requirement
    ↓
Acceptance criteria
    ↓
Database design
    ↓
API contract
    ↓
Backend implementation
    ↓
Backend tests
    ↓
Frontend implementation
    ↓
Frontend tests
    ↓
Integration testing
    ↓
Security review
    ↓
Documentation
    ↓
Git commit
```

### Feature Definition of Done

- [ ] Requirement and acceptance criteria
- [ ] Database design and migration
- [ ] Entity/model
- [ ] Repository
- [ ] Service and business rules
- [ ] Request and response DTOs
- [ ] Mapping
- [ ] Validation
- [ ] Controller/API
- [ ] Consistent exception handling
- [ ] Authentication and authorization
- [ ] Logging and audit requirements
- [ ] Unit tests
- [ ] Integration tests
- [ ] React UI
- [ ] Frontend validation
- [ ] Loading, error and empty states
- [ ] API documentation
- [ ] Security review
- [ ] Manual acceptance test
- [ ] Documentation updated

---

## 13. Git Workflow

### Branches

```text
main
└── develop
    ├── feature/*
    ├── bugfix/*
    └── release/*
```

Examples:

```text
feature/project-foundation
feature/local-authentication
feature/rbac-permissions
feature/employee-management
feature/task-management
```

### Commit examples

```text
feat(auth): add local login
feat(employee): add employee creation API
feat(task): add task assignment
fix(task): reject invalid status transition
test(employee): add employee service tests
docs(auth): document login flow
refactor(project): extract project mapper
```

---

## 14. How Future Markdown Files Should Be Created

For every new development stage, create a dedicated `.md` file containing:

1. Objective
2. Concepts to learn
3. Business requirements
4. Acceptance criteria
5. Database changes
6. API contracts
7. Backend tasks
8. Frontend tasks
9. Security checks
10. Test plan
11. Step-by-step implementation order
12. Definition of Done
13. Suggested Git branches and commits
14. Common mistakes
15. Interview explanations
16. Completion notes

### Naming convention

```text
NN-topic-name.md
```

Examples:

```text
01-project-foundation.md
02-local-authentication.md
03-rbac-permissions.md
04-employee-department-management.md
```

Each newly created plan should build on completed work rather than repeating the complete master plan.

---

## 15. What to Learn Right Now

Only learn enough to complete Sprint 0:

### Java/Spring

- Spring Boot project structure
- Dependency injection basics
- REST controller basics
- Configuration properties and environment variables
- Application profiles
- Basic testing

### PostgreSQL

- Database and user creation
- Connection URL
- Schema basics
- Primary keys and constraints
- Database migrations

### React/TypeScript

- Components and props
- TypeScript types and interfaces
- React Router basics
- API calls
- Loading and error state
- Environment variables in Vite
- Basic component testing

### Git

- Repository initialization
- Branching
- Staging and committing
- Meaningful commit messages
- `.gitignore`

Do not study Kafka, Redis, WebSockets, Kubernetes or microservices during Sprint 0.

---

## 16. First Action Checklist

Start with these actions today:

1. Create the `workflow360` repository.
2. Create the root folder structure.
3. Create `README.md` and `.gitignore`.
4. Record all tool versions.
5. Generate the Spring Boot application.
6. Generate the React + TypeScript application.
7. Run both applications independently.
8. Commit the clean foundation.

**Immediate target:** A React page should display a successful response from `/api/v1/system/status`, with Spring Boot connected to PostgreSQL.

---

## 17. Portfolio Outcome

By completing the project in this order, you should be able to explain:

> I designed WorkFlow360 as a modular Spring Boot monolith with a React and TypeScript frontend. I implemented versioned REST APIs using DTO validation and centralized exception handling, secured the system with authentication, RBAC and permission-based authorization, optimized PostgreSQL access using pagination and indexing, added Redis caching and Kafka-based asynchronous processing, tested the system at multiple levels, containerized it, and evolved stable modules into independently deployable services with CI/CD and production observability.

---

## 18. Current Status

```text
[ ] Sprint 0 — Project Foundation
[ ] Sprint 1 — Local Authentication
[ ] Sprint 2 — RBAC and Permissions
[ ] Sprint 3 — Employee and Department Management
[ ] Sprint 4 — Project and Team Management
[ ] Sprint 5 — Task Management
[ ] Sprint 6 — MVP Integration and Dashboard
```

Update this section whenever a sprint is completed.
