# WorkFlow360 — PostgreSQL, Spring Data JPA and Flyway Foundation

## Objective

Connect the existing Spring Boot backend to a locally installed PostgreSQL database, enable Spring Data JPA, and introduce Flyway for controlled database migrations.

> Docker is still deferred. PostgreSQL will run directly on Windows 11.

---

## 1. Install PostgreSQL on Windows

Download the Windows installer from the official PostgreSQL download page.

During installation, select:

- PostgreSQL Server
- pgAdmin 4
- Command Line Tools

Recommended installation choices:

```text
Port: 5432
Locale: Default locale
Superuser: postgres
Password: choose and record a strong local password
```

Stack Builder is optional for this project.

After installation, confirm that the PostgreSQL Windows service is running.

Open PowerShell:

```powershell
Get-Service *postgres*
```

The service status should be `Running`.

---

## 2. Verify PostgreSQL

If `psql` is available in `PATH`, run:

```powershell
psql --version
```

If it is not recognized, use pgAdmin for now or add PostgreSQL's `bin` directory to Windows `Path`.

Example path:

```text
C:\Program Files\PostgreSQL\18\bin
```

The actual version directory may differ.

Restart PowerShell after changing `Path`, then run:

```powershell
psql --version
```

---

## 3. Create a dedicated application user and database

Do not use the `postgres` superuser as the application account.

### Using pgAdmin

1. Open pgAdmin.
2. Connect to the local PostgreSQL server.
3. Open **Tools → Query Tool**.
4. Run the following SQL after replacing the example password:

```sql
CREATE ROLE workflow360_app
    WITH LOGIN
    PASSWORD 'replace-with-a-strong-local-password';

CREATE DATABASE workflow360
    WITH OWNER = workflow360_app
    ENCODING = 'UTF8';
```

Do not reuse your PostgreSQL superuser password.

### Verify the database

In pgAdmin, confirm that these objects exist:

```text
Login/Group Roles
└── workflow360_app

Databases
└── workflow360
```

---

## 4. Add Maven dependencies

Open the backend `pom.xml` and add these dependencies inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

Do not manually add dependency versions. Let the Spring Boot dependency management configure compatible versions.

Save `pom.xml`.

In Eclipse:

```text
Right-click project
→ Maven
→ Update Project
→ Select the project
→ OK
```

---

## 5. Create a local Spring profile

Keep common settings in:

```text
src/main/resources/application.properties
```

Add:

```properties
# Use the local profile by default during the current development stage
spring.profiles.default=local
```

Create:

```text
src/main/resources/application-local.properties
```

Add:

```properties
# ==================================================
# PostgreSQL datasource
# ==================================================

spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/workflow360}
spring.datasource.username=${DB_USERNAME:workflow360_app}
spring.datasource.password=${DB_PASSWORD}


# ==================================================
# JPA and Hibernate
# ==================================================

spring.jpa.open-in-view=false
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true


# ==================================================
# Flyway
# ==================================================

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true
```

### Why these settings?

- `ddl-auto=validate`: Hibernate checks mappings against the schema but does not create or change tables.
- Flyway owns database schema changes.
- `open-in-view=false`: database access should remain in controlled service/transaction boundaries rather than leaking into web response rendering.
- The database password has no default value and must come from an environment variable.

---

## 6. Add the database password to the Eclipse run configuration

Do not write the real password in `application.properties` or Java code.

In Eclipse:

1. Open **Run → Run Configurations**.
2. Select the Spring Boot or Java Application configuration used for `Workflow360BackendApplication`.
3. Open the **Environment** tab.
4. Click **Add**.
5. Add:

```text
Name: DB_PASSWORD
Value: the password assigned to workflow360_app
```

Optional variables, if your local configuration differs:

```text
DB_URL=jdbc:postgresql://localhost:5432/workflow360
DB_USERNAME=workflow360_app
```

Click **Apply**, then **Run**.

### PowerShell alternative

For the current PowerShell session only:

```powershell
$env:DB_PASSWORD="your-local-password"
cd C:\Development\workflow360\backend
.\mvnw.cmd spring-boot:run
```

Do not paste or save real passwords in documentation files.

---

## 7. Create the Flyway migration directory

Under:

```text
src/main/resources
```

create:

```text
db/migration
```

The path should become:

```text
src/main/resources/db/migration
```

---

## 8. Create the initial migration

Create:

```text
src/main/resources/db/migration/V1__initialize_database.sql
```

Content:

```sql
-- WorkFlow360 database foundation.
-- Business tables will be introduced with their owning modules.

SELECT 1;
```

This migration intentionally does not create premature business tables. It verifies that Flyway can connect, execute a versioned migration, and maintain its schema-history table.

Flyway migration naming rule:

```text
V<version>__<description>.sql
```

Important: there are two underscores between the version and description.

Examples for later:

```text
V2__create_identity_tables.sql
V3__seed_default_roles.sql
V4__create_employee_tables.sql
```

Never edit an already applied migration. Create a new migration for every schema change.

---

## 9. Run the application

In Eclipse:

```text
Workflow360BackendApplication.java
→ Run As
→ Spring Boot App
```

Or run with PowerShell after setting `DB_PASSWORD`:

```powershell
.\mvnw.cmd spring-boot:run
```

Look for messages indicating:

```text
HikariPool started
Successfully validated migration
Migrating schema to version "1 - initialize database"
Started Workflow360BackendApplication
```

Exact log wording may differ by dependency version.

---

## 10. Verify the database

In pgAdmin:

1. Select the `workflow360` database.
2. Refresh **Schemas → public → Tables**.
3. Confirm that Flyway created:

```text
flyway_schema_history
```

Run:

```sql
SELECT installed_rank,
       version,
       description,
       type,
       script,
       success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Expected migration:

```text
Version: 1
Description: initialize database
Success: true
```

---

## 11. Verify Actuator health

Open:

```text
http://localhost:8080/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

The datasource health contributor now participates in overall application health, although detailed components remain hidden because this project uses:

```properties
management.endpoint.health.show-details=never
```

Also confirm:

```text
http://localhost:8080/api/v1/system/status
```

and the React frontend:

```text
http://localhost:5173
```

---

## 12. Common errors

### `password authentication failed`

Confirm:

- `DB_USERNAME` is `workflow360_app`.
- `DB_PASSWORD` matches the role password.
- The Eclipse environment variable is attached to the correct run configuration.

### `database "workflow360" does not exist`

Create the database or correct `DB_URL`.

### `Connection refused`

Confirm that the PostgreSQL Windows service is running:

```powershell
Get-Service *postgres*
```

Also verify port 5432:

```powershell
Test-NetConnection localhost -Port 5432
```

### `No database found to handle jdbc:postgresql`

Confirm that both Flyway dependencies exist:

```xml
flyway-core
flyway-database-postgresql
```

### Migration checksum mismatch

Do not modify a migration after Flyway has applied it. Undo the accidental edit or add a new migration. Do not delete Flyway history to conceal migration mistakes.

### Existing context-load test fails

The test now needs database configuration because the application includes a datasource. For this milestone, run it with the local database and `DB_PASSWORD` configured in the Maven test environment. A separate test database/Testcontainers strategy will be introduced in the testing milestone.

---

## 13. Updated backend structure

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/com/workflow360/
│   │   │   ├── Workflow360BackendApplication.java
│   │   │   └── system/api/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-local.properties
│   │       └── db/migration/
│   │           └── V1__initialize_database.sql
│   └── test/
├── pom.xml
├── mvnw
└── mvnw.cmd
```

---

## Definition of Done

- [ ] PostgreSQL is installed and its Windows service is running
- [ ] pgAdmin opens and connects to the local server
- [ ] `workflow360_app` role exists
- [ ] `workflow360` database exists and is owned by `workflow360_app`
- [ ] Spring Data JPA dependency is added
- [ ] PostgreSQL JDBC driver is added
- [ ] Flyway core and PostgreSQL modules are added
- [ ] `application-local.properties` exists
- [ ] No real database password is stored in source files
- [ ] Eclipse run configuration provides `DB_PASSWORD`
- [ ] `V1__initialize_database.sql` exists
- [ ] Spring Boot starts successfully
- [ ] `flyway_schema_history` exists
- [ ] Migration version 1 shows `success=true`
- [ ] `/actuator/health` returns `UP`
- [ ] React still displays the backend status

---

## Next milestone

After this foundation works, begin the first real business foundation:

```text
Identity design
    ↓
Users
    ↓
Roles
    ↓
Permissions
    ↓
Local authentication preparation
```

Before writing authentication code, the next plan will define the identity database model, UUID strategy, status enums, audit columns, DTO boundaries, and future chat-compatible stable user identity.
