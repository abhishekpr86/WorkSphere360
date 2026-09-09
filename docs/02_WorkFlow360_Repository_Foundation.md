# WorkFlow360 — Step 2: Repository and Project Structure

> **Current architecture:** React + TypeScript → Spring Boot modular monolith → PostgreSQL  
> **Docker status:** Deferred. Docker is not required for this step.  
> **Goal:** Create a clean Git repository and the initial project/documentation structure.

---

## 1. Choose the Project Location

Open PowerShell and move to the folder where you keep development projects.

Example:

```powershell
cd C:\Development
```

If the directory does not exist:

```powershell
New-Item -ItemType Directory -Path C:\Development -Force
cd C:\Development
```

Avoid placing the project inside temporary folders. If possible, also avoid a OneDrive-synchronized folder to reduce file-locking and path issues.

---

## 2. Create the Root Repository

Run:

```powershell
New-Item -ItemType Directory -Name workflow360
cd workflow360
git init
git branch -M main
```

Verify:

```powershell
git status
git branch
```

Expected branch:

```text
* main
```

---

## 3. Create the Main Directories

Run these commands from inside `workflow360`:

```powershell
New-Item -ItemType Directory -Path frontend
New-Item -ItemType Directory -Path backend
New-Item -ItemType Directory -Path infrastructure
New-Item -ItemType Directory -Path docs
New-Item -ItemType Directory -Path .github

New-Item -ItemType Directory -Path docs\plans
New-Item -ItemType Directory -Path docs\progress
New-Item -ItemType Directory -Path docs\architecture
New-Item -ItemType Directory -Path docs\api
New-Item -ItemType Directory -Path docs\database
New-Item -ItemType Directory -Path docs\security
New-Item -ItemType Directory -Path docs\testing
New-Item -ItemType Directory -Path docs\deployment

New-Item -ItemType Directory -Path infrastructure\postgres
New-Item -ItemType Directory -Path infrastructure\docker
New-Item -ItemType Directory -Path infrastructure\redis
New-Item -ItemType Directory -Path infrastructure\kafka

New-Item -ItemType Directory -Path .github\workflows
```

The Redis, Kafka and Docker directories are placeholders only. Do not configure those technologies now.

---

## 4. Add Placeholder Files

Git does not track empty directories. Add `.gitkeep` files to directories that will remain empty for now:

```powershell
New-Item -ItemType File -Path frontend\.gitkeep
New-Item -ItemType File -Path backend\.gitkeep
New-Item -ItemType File -Path infrastructure\postgres\.gitkeep
New-Item -ItemType File -Path infrastructure\docker\.gitkeep
New-Item -ItemType File -Path infrastructure\redis\.gitkeep
New-Item -ItemType File -Path infrastructure\kafka\.gitkeep
New-Item -ItemType File -Path docs\architecture\.gitkeep
New-Item -ItemType File -Path docs\api\.gitkeep
New-Item -ItemType File -Path docs\database\.gitkeep
New-Item -ItemType File -Path docs\security\.gitkeep
New-Item -ItemType File -Path docs\testing\.gitkeep
New-Item -ItemType File -Path docs\deployment\.gitkeep
New-Item -ItemType File -Path .github\workflows\.gitkeep
```

These placeholders can be deleted as real files are introduced.

---

## 5. Create the Root `.gitignore`

Run:

```powershell
@'
# Operating system
.DS_Store
Thumbs.db

# IDEs and editors
.idea/
*.iml
.classpath
.project
.settings/
.vscode/*
!.vscode/extensions.json
!.vscode/settings.json

# Java and Maven
backend/target/
*.class
*.jar
*.war
*.ear
.mvn/timing.properties

# Node and React
frontend/node_modules/
frontend/dist/
frontend/coverage/
frontend/.vite/
npm-debug.log*
yarn-debug.log*
yarn-error.log*
pnpm-debug.log*

# Environment files and secrets
.env
.env.*
!.env.example
frontend/.env
frontend/.env.*
!frontend/.env.example
backend/.env
backend/.env.*
!backend/.env.example

# Logs
*.log
logs/

# Test and build output
coverage/
test-results/
playwright-report/

# Local databases and temporary files
*.db
*.sqlite
*.sqlite3
*.tmp
*.temp

# Certificates, keys and secrets
*.pem
*.key
*.p12
*.pfx
secrets/

# Docker local overrides
docker-compose.override.yml
'@ | Set-Content -Encoding UTF8 .gitignore
```

Never commit passwords, JWT secrets, database passwords, OAuth secrets or cloud credentials.

---

## 6. Create the Root `README.md`

Run:

```powershell
@'
# WorkFlow360

WorkFlow360 is an enterprise employee, project, resource, task, timesheet, leave, notification, reporting and collaboration platform.

## Initial Architecture

```text
React + TypeScript
        |
        | REST
        v
Spring Boot Modular Monolith
        |
        v
PostgreSQL
```

The application will start as a modular monolith. Stable modules may later be extracted into microservices.

## Planned Modules

- Identity and authentication
- Users, roles and permissions
- Organizations and departments
- Employees and skills
- Projects and teams
- Tasks and comments
- Timesheets and leave
- Resource allocation
- Notifications
- Private and group chat
- Reports
- Audit logs

## Repository Structure

```text
workflow360/
|-- frontend/
|-- backend/
|-- infrastructure/
|-- docs/
|-- .github/
|-- .gitignore
`-- README.md
```

## Current Status

- [x] Development prerequisites checked
- [x] Repository foundation created
- [ ] Spring Boot backend generated
- [ ] React frontend generated
- [ ] PostgreSQL connected
- [ ] React-to-backend status request working

## Development Principle

Learn → Implement → Break → Debug → Test → Improve → Document
'@ | Set-Content -Encoding UTF8 README.md
```

---

## 7. Create the Progress Files

### `docs/progress/development-log.md`

```powershell
@'
# WorkFlow360 Development Log

## Environment

- Operating system: Windows 11
- Java: 25
- Javac: 25
- Maven: 3.x — replace with exact version
- Node.js: replace with selected LTS version
- npm: replace with exact version
- Git: 2.54
- Backend IDE: IntelliJ IDEA
- Frontend editor: Visual Studio Code
- Docker: deferred

## Progress

### Step 1 — Prerequisites

Status: Completed, except Docker installation.

### Step 2 — Repository Foundation

Status: In progress.

## Notes

- Start with a modular monolith.
- Keep private two-user chat as a future architectural requirement.
- Do not add Redis, Kafka or microservices during the foundation stage.
'@ | Set-Content -Encoding UTF8 docs\progress\development-log.md
```

### `docs/progress/decisions.md`

```powershell
@'
# Architecture Decision Log

## ADR-001 — Begin as a Modular Monolith

**Status:** Accepted

### Decision

WorkFlow360 will begin as one Spring Boot deployment organized into explicit business modules.

### Reason

Business boundaries can be developed and tested without the distributed-system complexity of microservices. Stable modules can be extracted later.

### Consequences

- One backend application initially
- Clear package and module boundaries
- No direct access to another module's internal implementation
- Module interactions through public interfaces or domain events
- Microservices introduced only after boundaries are justified

## ADR-002 — Preserve a Future Chat Boundary

**Status:** Accepted

### Decision

Private two-user chat will be a first-class future module with conversation-level authorization and stable user identifiers.

### Initial Consequences

- User identifiers must remain stable
- Chat will own conversations, memberships, messages and read receipts
- Other modules must not modify chat data directly
- Chat begins inside the modular monolith and may later become a separate service

## ADR-003 — Defer Docker

**Status:** Accepted temporarily

### Decision

Repository and application development may begin without Docker. PostgreSQL may be installed locally until Docker is available.

### Consequences

- Local setup must be documented
- Secrets remain outside Git
- Docker-based reproducibility will be added later
'@ | Set-Content -Encoding UTF8 docs\progress\decisions.md
```

### `docs/progress/bugs-and-learning.md`

```powershell
@'
# Bugs and Learning Log

Use this file to record problems instead of only fixing and forgetting them.

## Entry Template

### Date — Short title

- **Feature:**
- **Problem:**
- **Cause:**
- **Fix:**
- **How it was tested:**
- **What I learned:**
- **Prevention:**
'@ | Set-Content -Encoding UTF8 docs\progress\bugs-and-learning.md
```

---

## 8. Copy the Planning Files

Place the previously created master roadmap and prerequisite guide inside:

```text
docs/plans/
```

Recommended names:

```text
docs/plans/00-master-roadmap.md
docs/plans/01-project-foundation-prerequisites.md
```

If the downloaded files are in your Downloads directory, you can copy them manually using File Explorer.

---

## 9. Verify the Structure

From the root of `workflow360`, run:

```powershell
Get-ChildItem -Force
Get-ChildItem -Recurse -Force | Select-Object FullName
```

The important structure should resemble:

```text
workflow360/
├── .github/
│   └── workflows/
├── backend/
├── docs/
│   ├── api/
│   ├── architecture/
│   ├── database/
│   ├── deployment/
│   ├── plans/
│   ├── progress/
│   ├── security/
│   └── testing/
├── frontend/
├── infrastructure/
│   ├── docker/
│   ├── kafka/
│   ├── postgres/
│   └── redis/
├── .gitignore
└── README.md
```

---

## 10. Review Before Committing

Run:

```powershell
git status
git diff -- .gitignore README.md
git check-ignore -v .env
```

The final command should show that `.env` is ignored.

Check that no passwords, tokens, personal keys or generated build directories are staged.

---

## 11. Create the First Commit

Run:

```powershell
git add .
git status
git commit -m "chore(project): initialize workflow360 repository"
```

Verify:

```powershell
git log --oneline -1
git status
```

Expected status:

```text
nothing to commit, working tree clean
```

---

## 12. Create the Development Branch

Run:

```powershell
git checkout -b develop
```

Verify:

```powershell
git branch
```

Expected:

```text
* develop
  main
```

All upcoming foundation work will be performed on `develop` or a feature branch created from it.

---

## Optional: Create the GitHub Repository

This can be done now or after the backend and frontend are generated.

If creating it now:

1. Create an empty GitHub repository named `workflow360`.
2. Do not initialize it with a README, `.gitignore` or license because those already exist locally.
3. Copy its HTTPS URL.
4. Run:

```powershell
git remote add origin https://github.com/YOUR-USERNAME/workflow360.git
git push -u origin main
git push -u origin develop
```

Verify:

```powershell
git remote -v
```

Never place GitHub passwords or personal access tokens inside project files.

---

## Definition of Done

- [ ] `workflow360` root directory exists
- [ ] Local Git repository is initialized
- [ ] `main` branch exists
- [ ] `develop` branch exists
- [ ] `frontend` and `backend` directories exist
- [ ] Documentation structure exists
- [ ] Infrastructure placeholders exist
- [ ] Root `.gitignore` exists
- [ ] Root `README.md` exists
- [ ] Development log exists
- [ ] Architecture decision log exists
- [ ] Future private chat requirement is recorded
- [ ] No secrets are committed
- [ ] First commit exists
- [ ] Working tree is clean

---

## What Not to Do Yet

Do not generate or configure these during Step 2:

- Spring Security
- JWT
- User entities
- Employee entities
- React components
- PostgreSQL tables
- WebSocket chat
- Redis
- Kafka
- API Gateway
- Microservices

The next step will generate the Spring Boot backend carefully with the correct metadata and dependencies.

---

## Next Step

**Step 3 — Generate the Spring Boot modular-monolith backend.**

The initial backend will contain only:

- Spring Boot application
- Spring Web
- Validation
- Actuator
- Spring Data JPA
- PostgreSQL driver
- Flyway database migrations
- Test support
- A versioned `/api/v1/system/status` endpoint

Docker can be added later without blocking the repository or backend foundation.
