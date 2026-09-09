# WorkFlow360 — Create the Project Structure from Scratch

> This stage creates only the directory and documentation structure. Git and Docker are deferred.

## 1. Create the root folder

Open PowerShell:

```powershell
cd C:\Development
New-Item -ItemType Directory -Path workflow360 -Force
cd workflow360
```

If `C:\Development` does not exist:

```powershell
New-Item -ItemType Directory -Path C:\Development -Force
cd C:\Development
New-Item -ItemType Directory -Path workflow360 -Force
cd workflow360
```

## 2. Create the top-level structure

```powershell
New-Item -ItemType Directory -Path backend -Force
New-Item -ItemType Directory -Path frontend -Force
New-Item -ItemType Directory -Path docs -Force
New-Item -ItemType Directory -Path infrastructure -Force
```

## 3. Create documentation folders

```powershell
New-Item -ItemType Directory -Path docs\plans -Force
New-Item -ItemType Directory -Path docs\architecture -Force
New-Item -ItemType Directory -Path docs\api -Force
New-Item -ItemType Directory -Path docs\database -Force
New-Item -ItemType Directory -Path docs\security -Force
New-Item -ItemType Directory -Path docs\testing -Force
New-Item -ItemType Directory -Path docs\deployment -Force
New-Item -ItemType Directory -Path docs\progress -Force
```

## 4. Create future infrastructure folders

These are placeholders only:

```powershell
New-Item -ItemType Directory -Path infrastructure\postgres -Force
New-Item -ItemType Directory -Path infrastructure\docker -Force
New-Item -ItemType Directory -Path infrastructure\redis -Force
New-Item -ItemType Directory -Path infrastructure\kafka -Force
```

Do not install or configure Redis, Kafka or Docker now.

## 5. Create initial documentation files

```powershell
New-Item -ItemType File -Path README.md -Force
New-Item -ItemType File -Path docs\progress\development-log.md -Force
New-Item -ItemType File -Path docs\progress\decisions.md -Force
New-Item -ItemType File -Path docs\progress\bugs-and-learning.md -Force
```

Copy the existing planning Markdown files into `docs\plans`.

## 6. Verify the structure

```powershell
Get-ChildItem -Recurse | Select-Object FullName
```

Expected foundation:

```text
workflow360/
├── backend/
├── frontend/
├── docs/
│   ├── plans/
│   ├── architecture/
│   ├── api/
│   ├── database/
│   ├── security/
│   ├── testing/
│   ├── deployment/
│   └── progress/
├── infrastructure/
│   ├── postgres/
│   ├── docker/
│   ├── redis/
│   └── kafka/
└── README.md
```

## 7. Backend structure — do not create all Java packages manually yet

The `backend` directory is currently empty. In the next stage, generate the Spring Boot application directly into this directory. Spring Boot will create:

```text
backend/
├── .mvn/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/workflow360/
│   │   │       └── WorkFlow360Application.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   └── test/
│       └── java/
│           └── com/workflow360/
├── mvnw
├── mvnw.cmd
└── pom.xml
```

After generation, create modules gradually:

```text
com.workflow360/
├── common/
├── identity/
├── organization/
├── department/
├── employee/
├── skill/
├── project/
├── task/
├── timesheet/
├── leave/
├── allocation/
├── notification/
├── chat/
├── report/
└── audit/
```

Do not create every controller, service, repository and entity now. Create them only when implementing that module.

Recommended internal module structure later:

```text
employee/
├── api/
├── application/
├── domain/
└── infrastructure/
```

Meaning:

- `api` — REST controllers and request/response DTOs
- `application` — use cases and orchestration
- `domain` — business models, rules and domain events
- `infrastructure` — JPA entities, repositories and adapters

This structure provides stronger module boundaries than one global controller/service/repository folder.

## 8. Frontend structure — create after Vite generation

Do not manually build the complete React structure inside an empty folder. First generate React + TypeScript with Vite into `frontend`, then arrange `src` like this:

```text
frontend/
├── public/
├── src/
│   ├── app/
│   ├── routes/
│   ├── layouts/
│   ├── components/
│   ├── features/
│   │   ├── auth/
│   │   ├── dashboard/
│   │   ├── employees/
│   │   ├── departments/
│   │   ├── projects/
│   │   ├── tasks/
│   │   ├── notifications/
│   │   └── chat/
│   ├── api/
│   ├── hooks/
│   ├── types/
│   ├── utils/
│   ├── styles/
│   └── main.tsx
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

Again, create feature folders when each feature begins. `chat` may remain a planned feature until authentication, users and notifications are stable.

## 9. Final architecture for the first milestone

```text
Browser
   ↓
React + TypeScript frontend
   ↓ REST / JSON
Spring Boot modular monolith
   ↓
PostgreSQL
```

One backend application, one frontend application and one database.

## 10. What to do next

Once the root folders exist, the next action is to generate the Spring Boot project in `backend` with:

- Maven
- Java 25
- Spring Web
- Validation
- Spring Boot Actuator
- Spring Data JPA
- PostgreSQL Driver
- Flyway Migration

Do not select Spring Security yet. Authentication will be a separate milestone after the foundation works.
