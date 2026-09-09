# WorkFlow360 — Identity and RBAC Database Foundation

## Objective

Create the first real business foundation for WorkFlow360:

- Stable user identity using UUIDs
- Users
- Roles
- Permissions
- User-role assignments
- Role-permission assignments
- Audit timestamps
- JPA mappings and repositories

This milestone prepares the database for authentication, authorization, employees, notifications, audit logs, and future private chat.

> Do not implement login, passwords, JWT, refresh tokens, or Google authentication in this milestone.

---

## Prerequisite checkpoint

Before continuing, confirm:

- Spring Boot starts with `workflow360_app`
- PostgreSQL connection succeeds
- Flyway creates `flyway_schema_history`
- Migration V1 has `success=true`
- `/actuator/health` returns `UP`
- React still displays backend status

If any prerequisite is incomplete, repair it before creating V2.

---

## 1. Identity design

Use these relationships:

```text
users
  |
  +---- user_roles ---- roles
                         |
                         +---- role_permissions ---- permissions
```

Meaning:

- One user can have multiple roles.
- One role can belong to multiple users.
- One role can contain multiple permissions.
- One permission can belong to multiple roles.

Examples:

```text
ADMIN
├── USER_CREATE
├── USER_UPDATE
├── ROLE_ASSIGN
└── AUDIT_VIEW

MANAGER
├── TEAM_VIEW
├── TASK_CREATE
├── TASK_ASSIGN
└── LEAVE_APPROVE

EMPLOYEE
├── TASK_VIEW
├── TASK_UPDATE
├── TIMESHEET_SUBMIT
└── LEAVE_APPLY
```

---

## 2. Why use UUID for user IDs?

Use PostgreSQL `UUID` for stable user identifiers.

This supports:

- Public API identifiers that are difficult to enumerate
- Future chat membership and message ownership
- Future event payloads
- Future service extraction
- Data import and cross-system integration

The user ID will later be referenced by:

```text
employees
project_members
task_assignees
notifications
conversation_members
messages
message_reads
audit_logs
```

---

## 3. Create Flyway migration V2

Create:

```text
src/main/resources/db/migration/V2__create_identity_rbac_tables.sql
```

Add:

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (
        status IN ('PENDING', 'ACTIVE', 'LOCKED', 'DISABLED')
    )
);

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_roles_code UNIQUE (code)
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_permissions_code UNIQUE (code)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE RESTRICT
);

CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);
CREATE INDEX idx_role_permissions_permission_id
    ON role_permissions (permission_id);
```

### Important

Do not add `password_hash` yet. Credential storage will be designed in the authentication milestone so local passwords and external identity providers remain properly separated.

---

## 4. Run migration V2

Restart Spring Boot.

Expected Flyway activity:

```text
Migrating schema public to version 2 - create identity rbac tables
Successfully applied 1 migration
```

In pgAdmin, refresh:

```text
workflow360
└── Schemas
    └── public
        └── Tables
```

Expected tables:

```text
flyway_schema_history
users
roles
permissions
user_roles
role_permissions
```

If V1 created `database_metadata`, that table will also remain.

Verify:

```sql
SELECT version,
       description,
       success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Expected:

```text
1 | initialize database           | true
2 | create identity rbac tables   | true
```

---

## 5. Create the identity module packages

Under:

```text
src/main/java/com/workflow360
```

create:

```text
identity/
├── api/
├── application/
├── domain/
└── infrastructure/
```

For this milestone, place domain enums in `domain`, persistence models and repositories in `infrastructure`.

More detailed structure:

```text
identity/
├── api/
├── application/
├── domain/
│   └── UserStatus.java
└── infrastructure/
    └── persistence/
        ├── entity/
        │   ├── UserEntity.java
        │   ├── RoleEntity.java
        │   └── PermissionEntity.java
        └── repository/
            ├── UserRepository.java
            ├── RoleRepository.java
            └── PermissionRepository.java
```

---

## 6. Create `UserStatus`

Create:

```text
com.workflow360.identity.domain.UserStatus
```

```java
package com.workflow360.identity.domain;

public enum UserStatus {
    PENDING,
    ACTIVE,
    LOCKED,
    DISABLED
}
```

---

## 7. Create `UserEntity`

Create:

```text
com.workflow360.identity.infrastructure.persistence.entity.UserEntity
```

```java
package com.workflow360.identity.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import com.workflow360.identity.domain.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected UserEntity() {
    }

    public UserEntity(UUID id, String email, String displayName, UserStatus status) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.status = status;
        this.emailVerified = false;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
```

Do not expose this JPA entity directly from a controller.

---

## 8. Create `RoleEntity`

```java
package com.workflow360.identity.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "system_role", nullable = false)
    private boolean systemRole;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected RoleEntity() {
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSystemRole() {
        return systemRole;
    }
}
```

---

## 9. Create `PermissionEntity`

```java
package com.workflow360.identity.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "permissions")
public class PermissionEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected PermissionEntity() {
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
```

---

## 10. Create repositories

### `UserRepository`

```java
package com.workflow360.identity.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow360.identity.infrastructure.persistence.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
```

### `RoleRepository`

```java
package com.workflow360.identity.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow360.identity.infrastructure.persistence.entity.RoleEntity;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByCode(String code);
}
```

### `PermissionRepository`

```java
package com.workflow360.identity.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow360.identity.infrastructure.persistence.entity.PermissionEntity;

public interface PermissionRepository
        extends JpaRepository<PermissionEntity, UUID> {

    Optional<PermissionEntity> findByCode(String code);
}
```

---

## 11. Why relationships are not mapped yet

Although the database contains `user_roles` and `role_permissions`, do not immediately add broad bidirectional `@ManyToMany` mappings.

Reasons:

- They can hide join-table operations.
- They can cause accidental large graph loading.
- They can contribute to serialization cycles.
- Assignment records may later need actor, reason, expiry, or audit fields.

The next RBAC application-service milestone will model assignments explicitly and query only the data required.

---

## 12. Run and validate

Restart Spring Boot.

Because this project uses:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

startup succeeds only when the JPA mappings match the Flyway-created tables.

Verify:

```text
http://localhost:8080/actuator/health
```

Expected:

```json
{"status":"UP"}
```

If Hibernate reports a missing table or column, compare the entity annotations with V2. Do not switch `ddl-auto` to `update` to hide the mismatch.

---

## 13. Database verification queries

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
```

Check constraints:

```sql
SELECT constraint_name,
       table_name,
       constraint_type
FROM information_schema.table_constraints
WHERE table_schema = 'public'
  AND table_name IN (
      'users',
      'roles',
      'permissions',
      'user_roles',
      'role_permissions'
  )
ORDER BY table_name, constraint_type, constraint_name;
```

---

## 14. Rules for this milestone

Do not:

- Return entities from controllers
- Add passwords to the `users` table without authentication design
- Add JWT
- Add Spring Security yet
- Seed a real admin password
- Use the PostgreSQL superuser from Spring Boot
- Use `ddl-auto=update`
- Create chat tables yet
- Add broad bidirectional `@ManyToMany` mappings
- Edit migration V2 after it succeeds

---

## Definition of Done

- [ ] V2 migration exists
- [ ] V2 runs successfully
- [ ] `users` table exists
- [ ] `roles` table exists
- [ ] `permissions` table exists
- [ ] `user_roles` table exists
- [ ] `role_permissions` table exists
- [ ] UUID is used for identity primary keys
- [ ] Email has a unique constraint
- [ ] User status has a database check constraint
- [ ] Foreign keys exist
- [ ] Required indexes exist
- [ ] `UserStatus` exists
- [ ] Three JPA entities exist
- [ ] Three repositories exist
- [ ] Hibernate schema validation succeeds
- [ ] Spring Boot starts
- [ ] Actuator health is `UP`
- [ ] No password, JWT, or login code has been added

---

## Next milestone

After completing this foundation:

```text
Seed default roles and permissions
        ↓
Create user DTOs
        ↓
Create user application service
        ↓
Build admin-created user API
        ↓
Add validation and exception handling
        ↓
Add tests
        ↓
Prepare local authentication credentials
```

The next migration will seed stable role and permission codes without storing any passwords.
