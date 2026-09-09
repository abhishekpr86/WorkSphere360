# WorkFlow360 — PostgreSQL Application User and Flyway Recovery

## Purpose

This guide explains and completes the current database task:

1. Understand why pgAdmin shows `postgres` as the current user.
2. Ensure Spring Boot connects as `workflow360_app`.
3. Correct ownership and schema privileges.
4. Recover from the Flyway V2 checksum mismatch.
5. Run Flyway migrations V1 and V2 cleanly.
6. Verify the database, application user, migrations, and tables.

> This procedure assumes WorkFlow360 is still a new local project and the database contains no important data.

---

# 1. Understand the Two Separate Database Connections

pgAdmin and Spring Boot are two independent PostgreSQL clients.

```text
pgAdmin
└── Administrative connection
    ├── Database: workflow360
    └── User: postgres

Spring Boot
└── Application connection
    ├── Database: workflow360
    └── User: workflow360_app
```

When this query is run in the existing pgAdmin Query Tool:

```sql
SELECT current_database(),
       current_user,
       current_schema();
```

this result is acceptable:

```text
workflow360 | postgres | public
```

It means only that the current **pgAdmin session** uses the `postgres` administrator account.

The desired Spring Boot connection is:

```text
workflow360 | workflow360_app | public
```

Do not permanently configure Spring Boot to use the `postgres` superuser.

---

# 2. Understand the Flyway Checksum Error

The error:

```text
Migration checksum mismatch for migration version 2
Applied to database : 0
Resolved locally    : -1507805393
```

means that Flyway already recorded migration V2 and the local V2 SQL file was changed afterward.

The likely sequence was:

```text
V2 file was created while empty or incomplete
        ↓
Spring Boot was started
        ↓
Flyway recorded V2 with checksum 0
        ↓
CREATE TABLE statements were added to V2
        ↓
The local checksum changed
        ↓
Flyway validation stopped the application
```

Because V2 may have been recorded without creating its tables, do not merely update the checksum. For this new local project, the safest approach is to recreate the local `workflow360` database and allow Flyway to run V1 and the completed V2 from the beginning.

---

# 3. Stop the Applications

Before resetting the database:

1. Stop Spring Boot in Eclipse.
2. Stop the React development server if desired.
3. Close Query Tool tabs connected specifically to the `workflow360` database.

The pgAdmin Query Tool used for dropping the database must be connected to the administrative `postgres` database.

---

# 4. Connect pgAdmin to the `postgres` Database

In pgAdmin:

1. Expand **Servers**.
2. Expand the local PostgreSQL server.
3. Expand **Databases**.
4. Right-click the database named `postgres`.
5. Select **Query Tool**.

Run:

```sql
SELECT current_database();
```

Expected result:

```text
postgres
```

This is correct. A database cannot be dropped from a session connected to that same database.

---

# 5. Verify or Repair the Application Role

Run from the Query Tool connected to `postgres`:

```sql
SELECT rolname,
       rolcanlogin,
       rolsuper,
       rolcreatedb,
       rolcreaterole
FROM pg_roles
WHERE rolname = 'workflow360_app';
```

Expected values:

```text
rolname       = workflow360_app
rolcanlogin   = true
rolsuper      = false
rolcreatedb   = false
rolcreaterole = false
```

## If the role does not exist

Create it:

```sql
CREATE ROLE workflow360_app
    WITH LOGIN
    PASSWORD 'replace-with-a-strong-local-password';
```

## If the role exists but login/password is uncertain

Reset it:

```sql
ALTER ROLE workflow360_app
    WITH LOGIN
    PASSWORD 'replace-with-a-strong-local-password';
```

Remember the chosen password. The same value must be configured as `DB_PASSWORD` in Eclipse.

---

# 6. Terminate Existing WorkFlow360 Connections

While still connected to the `postgres` database, run:

```sql
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'workflow360'
  AND pid <> pg_backend_pid();
```

Possible outcomes:

- One or more `true` rows: active connections were terminated.
- Zero rows: no active connections existed, which is also fine.

---

# 7. Drop and Recreate the Local Database

Run:

```sql
DROP DATABASE IF EXISTS workflow360;
```

Then recreate it with the application role as owner:

```sql
CREATE DATABASE workflow360
    WITH OWNER = workflow360_app
    ENCODING = 'UTF8';
```

Verify ownership:

```sql
SELECT datname,
       pg_catalog.pg_get_userbyid(datdba) AS owner
FROM pg_database
WHERE datname = 'workflow360';
```

Expected:

```text
workflow360 | workflow360_app
```

---

# 8. Configure the `public` Schema

Open a new Query Tool connected specifically to:

```text
Databases → workflow360
```

Confirm:

```sql
SELECT current_database(),
       current_user,
       current_schema();
```

A result like this is okay for pgAdmin:

```text
workflow360 | postgres | public
```

Now assign the public schema to the application role:

```sql
ALTER SCHEMA public
OWNER TO workflow360_app;

GRANT USAGE, CREATE
ON SCHEMA public
TO workflow360_app;
```

Verify:

```sql
SELECT schema_name,
       schema_owner
FROM information_schema.schemata
WHERE schema_name = 'public';
```

Expected:

```text
public | workflow360_app
```

---

# 9. Verify Spring Boot Configuration

File:

```text
src/main/resources/application-local.properties
```

Use:

```properties
# PostgreSQL datasource
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/workflow360}
spring.datasource.username=${DB_USERNAME:workflow360_app}
spring.datasource.password=${DB_PASSWORD}

# JPA and Hibernate
spring.jpa.open-in-view=false
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true
```

Important rules:

```text
Correct Spring Boot user: workflow360_app
Incorrect Spring Boot user: postgres
```

Do not disable Flyway validation and do not change `ddl-auto` to `update` to hide schema problems.

---

# 10. Configure Eclipse Environment Variables

In Eclipse:

```text
Run
→ Run Configurations
→ Select Workflow360BackendApplication
→ Environment
```

Add or update:

```text
Name: DB_PASSWORD
Value: password assigned to workflow360_app
```

If this exists:

```text
DB_USERNAME=postgres
```

remove it or change it to:

```text
DB_USERNAME=workflow360_app
```

If `DB_URL` is present, verify that it is:

```text
jdbc:postgresql://localhost:5432/workflow360
```

Click:

```text
Apply
```

---

# 11. Verify Migration Files Before Restarting

The exact directory must be:

```text
backend/
└── src/
    └── main/
        └── resources/
            └── db/
                └── migration/
                    ├── V1__initialize_database.sql
                    └── V2__create_identity_rbac_tables.sql
```

## Filename rules

Correct:

```text
V1__initialize_database.sql
V2__create_identity_rbac_tables.sql
```

Each filename requires:

- Uppercase `V`
- A version number
- Two underscores
- A description
- A real `.sql` extension

Ensure that Windows did not create:

```text
V2__create_identity_rbac_tables.sql.txt
```

## V1 content

A simple V1 is acceptable:

```sql
-- WorkFlow360 database foundation.
SELECT 1;
```

## V2 content

The completed V2 must contain all of these structures:

```text
users
roles
permissions
user_roles
role_permissions
three indexes
```

Once V2 is applied successfully to the recreated database, do not edit V2 again.

---

# 12. Clean and Update the Eclipse Project

In Eclipse:

```text
Project
→ Clean
→ Select the WorkFlow360 backend
→ Clean
```

Then:

```text
Right-click the project
→ Maven
→ Update Project
→ Select the project
→ OK
```

This ensures Maven dependencies and migration resources are refreshed.

---

# 13. Start Spring Boot

Run:

```text
Workflow360BackendApplication.java
→ Run As
→ Spring Boot App
```

Expected log flow:

```text
HikariPool starts
        ↓
Flyway connects to workflow360
        ↓
Flyway creates flyway_schema_history
        ↓
Flyway applies V1
        ↓
Flyway applies V2
        ↓
Hibernate validates the schema
        ↓
Spring Boot starts on port 8080
```

Look for messages similar to:

```text
Migrating schema "public" to version "1 - initialize database"
Migrating schema "public" to version "2 - create identity rbac tables"
Successfully applied 2 migrations
Started Workflow360BackendApplication
```

Exact wording can differ by version.

---

# 14. Verify Flyway History

In pgAdmin, open the Query Tool for `workflow360` and run:

```sql
SELECT installed_rank,
       version,
       description,
       script,
       checksum,
       installed_by,
       success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Expected rows:

```text
1 | 1 | initialize database          | V1__initialize_database.sql          | ... | workflow360_app | true
2 | 2 | create identity rbac tables  | V2__create_identity_rbac_tables.sql  | ... | workflow360_app | true
```

The important checks are:

```text
Version 1 exists
Version 2 exists
Both success values are true
installed_by is workflow360_app
```

---

# 15. Verify the Tables

Run:

```sql
SELECT table_schema,
       table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
```

Expected tables include:

```text
flyway_schema_history
permissions
role_permissions
roles
user_roles
users
```

In pgAdmin, refresh:

```text
workflow360
→ Schemas
→ public
→ Tables
→ Refresh
```

Then expand **Tables**.

---

# 16. Verify the Spring Boot Database Account

Keep Spring Boot running, then execute this query from pgAdmin:

```sql
SELECT datname,
       usename,
       application_name,
       client_addr,
       state
FROM pg_stat_activity
WHERE datname = 'workflow360'
ORDER BY usename, application_name;
```

You may see multiple rows:

```text
workflow360 | postgres        | pgAdmin          | ... | active
workflow360 | workflow360_app | PostgreSQL JDBC  | ... | idle
```

This is correct:

```text
pgAdmin uses postgres
Spring Boot uses workflow360_app
```

An `idle` Spring Boot connection is normal. It means the connection pool is waiting for work.

---

# 17. Optional Direct Login Test

From PowerShell:

```powershell
psql -h localhost -p 5432 -U workflow360_app -d workflow360
```

Enter the `workflow360_app` password.

Then run:

```sql
SELECT current_database(),
       current_user,
       current_schema();
```

Expected:

```text
workflow360 | workflow360_app | public
```

Exit:

```sql
\q
```

---

# 18. Verify Application Endpoints

Test:

```text
http://localhost:8080/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

Test:

```text
http://localhost:8080/api/v1/system/status
```

Then start React:

```powershell
cd C:\Development\workflow360\frontend
npm run dev
```

Open:

```text
http://localhost:5173
```

The frontend should display the backend system status.

---

# Important Rules Going Forward

## Rule 1 — Never edit an applied migration

After V2 succeeds, do not change:

```text
V2__create_identity_rbac_tables.sql
```

If a new column is required, create a new migration, for example:

```text
V3__add_last_login_to_users.sql
```

```sql
ALTER TABLE users
ADD COLUMN last_login_at TIMESTAMP WITH TIME ZONE;
```

## Rule 2 — Never use `postgres` as the application user

Use:

```text
postgres
```

only for administrative actions such as creating databases, changing owners, and troubleshooting.

Use:

```text
workflow360_app
```

for Spring Boot.

## Rule 3 — Never create `flyway_schema_history` manually

Flyway creates and manages this table.

## Rule 4 — Keep validation enabled

Keep:

```properties
spring.flyway.validate-on-migrate=true
spring.jpa.hibernate.ddl-auto=validate
```

Do not disable validation to hide migration or schema mismatches.

---

# Definition of Done

- [ ] Query Tool connected to `postgres` was used for database reset
- [ ] `workflow360_app` exists and can log in
- [ ] `workflow360_app` is not a superuser
- [ ] `workflow360` database is owned by `workflow360_app`
- [ ] `public` schema is owned by `workflow360_app`
- [ ] Spring Boot username is `workflow360_app`
- [ ] Eclipse supplies the correct `DB_PASSWORD`
- [ ] V1 and V2 files exist in `db/migration`
- [ ] V1 succeeds
- [ ] V2 succeeds
- [ ] Flyway history contains versions 1 and 2
- [ ] `installed_by` is `workflow360_app`
- [ ] Identity/RBAC tables exist
- [ ] Spring Boot starts successfully
- [ ] `/actuator/health` returns `UP`
- [ ] React still displays backend status

---

# Next Task

After every checkbox passes, continue with the Java identity module:

```text
UserStatus enum
        ↓
UserEntity
        ↓
RoleEntity
        ↓
PermissionEntity
        ↓
Spring Data repositories
        ↓
Hibernate schema validation
```
