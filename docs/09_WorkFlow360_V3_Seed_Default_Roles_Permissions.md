# WorkFlow360 — V3 Seed Default Roles and Permissions

## Goal

Populate the RBAC tables created by V2 with stable default roles, permissions, and role-permission assignments.

This task creates configuration data for:

- `ADMIN`
- `MANAGER`
- `EMPLOYEE`
- Initial permission codes
- Default role-permission mappings

This task does **not** yet implement:

- User creation APIs
- Passwords
- Login
- Spring Security
- JWT
- Role assignment to users
- React administration screens

---

## 1. Prerequisite Check

Before starting V3, confirm that V1 and V2 succeeded.

Run in pgAdmin against the `workflow360` database:

```sql
SELECT version,
       description,
       installed_by,
       success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Expected:

```text
1 | initialize database          | workflow360_app | true
2 | create identity rbac tables  | workflow360_app | true
```

Also verify the RBAC tables:

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
      'users',
      'roles',
      'permissions',
      'user_roles',
      'role_permissions'
  )
ORDER BY table_name;
```

Expected:

```text
permissions
role_permissions
roles
user_roles
users
```

Do not continue if V2 is missing or unsuccessful.

---

## 2. Why Seed Roles and Permissions Through Flyway?

Roles and permission codes are part of the application's controlled security configuration.

Using a migration provides:

- The same initial roles in every environment
- Stable UUID values
- Repeatable environment setup
- A documented security baseline
- Predictable references in later migrations and tests

Do not insert these rows manually through pgAdmin.

---

## 3. Default Roles

The initial roles are:

```text
ADMIN
MANAGER
EMPLOYEE
```

### ADMIN

Responsible for platform administration, users, roles, reports, and audit access.

### MANAGER

Responsible for teams, tasks, approvals, and manager reports.

### EMPLOYEE

Responsible for personal tasks, timesheets, leave, notifications, and future chat.

These roles are an initial baseline. More focused roles such as HR and PROJECT_MANAGER can be introduced later through new migrations.

---

## 4. Default Permissions

Use domain-oriented permission codes:

```text
USER_VIEW
USER_CREATE
USER_UPDATE
ROLE_VIEW
ROLE_ASSIGN
EMPLOYEE_VIEW
EMPLOYEE_CREATE
EMPLOYEE_UPDATE
TEAM_VIEW
TASK_VIEW
TASK_CREATE
TASK_ASSIGN
TASK_UPDATE
TIMESHEET_SUBMIT
TIMESHEET_APPROVE
LEAVE_APPLY
LEAVE_APPROVE
REPORT_VIEW
AUDIT_VIEW
NOTIFICATION_VIEW
CHAT_USE
```

Permission codes must remain stable because Spring Security annotations and authorization services will eventually refer to them.

---

## 5. Create Migration V3

Create this file:

```text
backend/src/main/resources/db/migration/V3__seed_default_roles_permissions.sql
```

The filename must contain two underscores after `V3`.

Paste the complete SQL below before starting Spring Boot:

```sql
-- =============================================================
-- Default system roles
-- =============================================================

INSERT INTO roles (
    id,
    code,
    name,
    description,
    system_role
)
VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        'ADMIN',
        'Administrator',
        'Full platform administration role',
        TRUE
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        'MANAGER',
        'Manager',
        'Team, task and approval management role',
        TRUE
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        'EMPLOYEE',
        'Employee',
        'Standard employee access role',
        TRUE
    );


-- =============================================================
-- Default permissions
-- =============================================================

INSERT INTO permissions (
    id,
    code,
    name,
    description
)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'USER_VIEW', 'View Users', 'View user accounts'),
    ('20000000-0000-0000-0000-000000000002', 'USER_CREATE', 'Create Users', 'Create user accounts'),
    ('20000000-0000-0000-0000-000000000003', 'USER_UPDATE', 'Update Users', 'Update user accounts'),
    ('20000000-0000-0000-0000-000000000004', 'ROLE_VIEW', 'View Roles', 'View roles and permissions'),
    ('20000000-0000-0000-0000-000000000005', 'ROLE_ASSIGN', 'Assign Roles', 'Assign roles to users'),
    ('20000000-0000-0000-0000-000000000006', 'EMPLOYEE_VIEW', 'View Employees', 'View employee records'),
    ('20000000-0000-0000-0000-000000000007', 'EMPLOYEE_CREATE', 'Create Employees', 'Create employee records'),
    ('20000000-0000-0000-0000-000000000008', 'EMPLOYEE_UPDATE', 'Update Employees', 'Update employee records'),
    ('20000000-0000-0000-0000-000000000009', 'TEAM_VIEW', 'View Team', 'View assigned team members'),
    ('20000000-0000-0000-0000-000000000010', 'TASK_VIEW', 'View Tasks', 'View authorized tasks'),
    ('20000000-0000-0000-0000-000000000011', 'TASK_CREATE', 'Create Tasks', 'Create project tasks'),
    ('20000000-0000-0000-0000-000000000012', 'TASK_ASSIGN', 'Assign Tasks', 'Assign tasks to employees'),
    ('20000000-0000-0000-0000-000000000013', 'TASK_UPDATE', 'Update Tasks', 'Update authorized tasks'),
    ('20000000-0000-0000-0000-000000000014', 'TIMESHEET_SUBMIT', 'Submit Timesheets', 'Submit personal timesheets'),
    ('20000000-0000-0000-0000-000000000015', 'TIMESHEET_APPROVE', 'Approve Timesheets', 'Approve team timesheets'),
    ('20000000-0000-0000-0000-000000000016', 'LEAVE_APPLY', 'Apply Leave', 'Submit personal leave requests'),
    ('20000000-0000-0000-0000-000000000017', 'LEAVE_APPROVE', 'Approve Leave', 'Approve team leave requests'),
    ('20000000-0000-0000-0000-000000000018', 'REPORT_VIEW', 'View Reports', 'View authorized reports'),
    ('20000000-0000-0000-0000-000000000019', 'AUDIT_VIEW', 'View Audit Logs', 'View security and business audit events'),
    ('20000000-0000-0000-0000-000000000020', 'NOTIFICATION_VIEW', 'View Notifications', 'View personal notifications'),
    ('20000000-0000-0000-0000-000000000021', 'CHAT_USE', 'Use Chat', 'Use authorized private and group conversations');


-- =============================================================
-- ADMIN permissions
-- =============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT
    '10000000-0000-0000-0000-000000000001'::UUID,
    id
FROM permissions;


-- =============================================================
-- MANAGER permissions
-- =============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT
    '10000000-0000-0000-0000-000000000002'::UUID,
    id
FROM permissions
WHERE code IN (
    'USER_VIEW',
    'EMPLOYEE_VIEW',
    'TEAM_VIEW',
    'TASK_VIEW',
    'TASK_CREATE',
    'TASK_ASSIGN',
    'TASK_UPDATE',
    'TIMESHEET_SUBMIT',
    'TIMESHEET_APPROVE',
    'LEAVE_APPLY',
    'LEAVE_APPROVE',
    'REPORT_VIEW',
    'NOTIFICATION_VIEW',
    'CHAT_USE'
);


-- =============================================================
-- EMPLOYEE permissions
-- =============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT
    '10000000-0000-0000-0000-000000000003'::UUID,
    id
FROM permissions
WHERE code IN (
    'TASK_VIEW',
    'TASK_UPDATE',
    'TIMESHEET_SUBMIT',
    'LEAVE_APPLY',
    'NOTIFICATION_VIEW',
    'CHAT_USE'
);
```

Save the file before running the application.

---

## 6. Why Use Fixed UUIDs for Seed Data?

The migrations use fixed UUID values for built-in roles and permissions.

Example:

```text
ADMIN    → 10000000-0000-0000-0000-000000000001
MANAGER  → 10000000-0000-0000-0000-000000000002
EMPLOYEE → 10000000-0000-0000-0000-000000000003
```

This provides stable references across:

- Local development
- Automated tests
- Staging
- Production
- Future service extraction

Runtime-created users will use generated UUID values. Fixed UUIDs are used only for controlled built-in seed data.

---

## 7. Run Migration V3

In Eclipse:

```text
Project → Clean
```

Then:

```text
Right-click project
→ Maven
→ Update Project
→ OK
```

Start Spring Boot:

```text
Workflow360BackendApplication.java
→ Run As
→ Spring Boot App
```

Expected Flyway behavior:

```text
Validate V1 and V2
        ↓
Detect pending V3
        ↓
Apply V3
        ↓
Hibernate validates the schema
        ↓
Spring Boot starts
```

Look for output similar to:

```text
Migrating schema public to version 3 - seed default roles permissions
Successfully applied 1 migration
Started Workflow360BackendApplication
```

---

## 8. Verify Flyway History

Run in pgAdmin:

```sql
SELECT version,
       description,
       script,
       installed_by,
       success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Expected:

```text
1 | initialize database                 | V1__initialize_database.sql                 | workflow360_app | true
2 | create identity rbac tables         | V2__create_identity_rbac_tables.sql         | workflow360_app | true
3 | seed default roles permissions      | V3__seed_default_roles_permissions.sql      | workflow360_app | true
```

---

## 9. Verify Seeded Roles

```sql
SELECT id,
       code,
       name,
       system_role
FROM roles
ORDER BY code;
```

Expected role codes:

```text
ADMIN
EMPLOYEE
MANAGER
```

Verify the count:

```sql
SELECT COUNT(*) AS role_count
FROM roles;
```

Expected:

```text
3
```

---

## 10. Verify Seeded Permissions

```sql
SELECT code,
       name
FROM permissions
ORDER BY code;
```

Verify the count:

```sql
SELECT COUNT(*) AS permission_count
FROM permissions;
```

Expected:

```text
21
```

---

## 11. Verify Role-Permission Assignments

Run:

```sql
SELECT r.code AS role_code,
       p.code AS permission_code
FROM role_permissions rp
JOIN roles r
  ON r.id = rp.role_id
JOIN permissions p
  ON p.id = rp.permission_id
ORDER BY r.code, p.code;
```

Count permissions by role:

```sql
SELECT r.code AS role_code,
       COUNT(*) AS permission_count
FROM role_permissions rp
JOIN roles r
  ON r.id = rp.role_id
GROUP BY r.code
ORDER BY r.code;
```

Expected counts:

```text
ADMIN     21
EMPLOYEE   6
MANAGER   14
```

---

## 12. Add Constructors Needed for Later Application Services

The current `RoleEntity` and `PermissionEntity` may only have JPA-required protected constructors and getters.

Add this constructor to `RoleEntity`:

```java
public RoleEntity(
        UUID id,
        String code,
        String name,
        String description,
        boolean systemRole) {
    this.id = id;
    this.code = code;
    this.name = name;
    this.description = description;
    this.systemRole = systemRole;
}
```

Add this constructor to `PermissionEntity`:

```java
public PermissionEntity(
        UUID id,
        String code,
        String name,
        String description) {
    this.id = id;
    this.code = code;
    this.name = name;
    this.description = description;
}
```

These constructors will support tests and controlled application-service creation later. The built-in data itself remains seeded through Flyway.

---

## 13. Improve Repository Queries

Update `RoleRepository`:

```java
package com.workflow360.identity.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow360.identity.infrastructure.persistence.entity.RoleEntity;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByCode(String code);

    List<RoleEntity> findAllByOrderByCodeAsc();

    boolean existsByCode(String code);
}
```

Update `PermissionRepository`:

```java
package com.workflow360.identity.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow360.identity.infrastructure.persistence.entity.PermissionEntity;

public interface PermissionRepository
        extends JpaRepository<PermissionEntity, UUID> {

    Optional<PermissionEntity> findByCode(String code);

    List<PermissionEntity> findAllByOrderByCodeAsc();

    boolean existsByCode(String code);
}
```

Do not create REST endpoints in this task.

---

## 14. Validate the Application

Restart Spring Boot after the repository updates.

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

Also verify the existing React status page continues to work.

---

## 15. Important Rules

Do not:

- Edit V1, V2, or V3 after V3 succeeds
- Assign a role to a real user manually in pgAdmin
- Add passwords to the users table yet
- Add Spring Security or JWT yet
- Put permission checks only in React
- Use role names directly as a substitute for permission checks everywhere
- Return entities from controllers
- Add `@ManyToMany` relationships yet

Use stable codes:

```text
ADMIN
MANAGER
EMPLOYEE
USER_CREATE
TASK_ASSIGN
CHAT_USE
```

Display names may change later, but codes should remain stable.

---

## 16. Definition of Done

- [ ] V1 and V2 are successful
- [ ] V3 file has the correct name
- [ ] V3 was complete before Spring Boot startup
- [ ] V3 is successful in Flyway history
- [ ] Three roles exist
- [ ] Twenty-one permissions exist
- [ ] ADMIN has all twenty-one permissions
- [ ] MANAGER has fourteen permissions
- [ ] EMPLOYEE has six permissions
- [ ] `CHAT_USE` exists for ADMIN, MANAGER, and EMPLOYEE
- [ ] Role and permission codes are unique
- [ ] Role repository query methods are updated
- [ ] Permission repository query methods are updated
- [ ] Spring Boot starts successfully
- [ ] Actuator health returns `UP`
- [ ] React still displays backend status
- [ ] No authentication code has been added

---

## Next Task

The next task will build the first database-backed REST feature:

```text
Create User Request DTO
        ↓
Create User Response DTO
        ↓
Create User Mapper
        ↓
Create User Application Service
        ↓
Create POST /api/v1/users
        ↓
Create GET /api/v1/users/{id}
        ↓
Validation
        ↓
Centralized exception handling
        ↓
Service and controller tests
```

The first user will initially be created without a password. Local credentials and authentication will be introduced in the following dedicated security milestone.
