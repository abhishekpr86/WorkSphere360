# WorkFlow360 — Identity, Roles and Permissions Database Foundation

## Objective

Create the first real WorkFlow360 business schema for:

- Users
- Roles
- Permissions
- User-role assignments
- Role-permission assignments

This milestone creates the database model and JPA persistence layer only. It does **not** implement registration, login, password hashing, JWT, refresh tokens or Spring Security yet.

---

## 1. Architecture boundary

Create the identity module under:

```text
src/main/java/com/workflow360/identity/
├── domain/
├── application/
├── api/
└── infrastructure/
    └── persistence/
```

For this milestone, only `domain` and `infrastructure.persistence` are required.

The identity module owns:

```text
users
roles
permissions
user_roles
role_permissions
```

Other modules must not modify these tables directly.

---

## 2. Stable user identity decision

Use PostgreSQL's native `uuid` data type for user, role and permission primary keys.

This is important because the same stable user ID will later be referenced by:

- Employee profiles
- Project memberships
- Task assignments
- Notifications
- Audit logs
- Private chat conversation memberships
- Messages and read receipts

Generate UUID values in Java so that an entity receives its identity before persistence.

---

## 3. Database model

```text
users
├── id UUID PK
├── email VARCHAR(320) UNIQUE
├── password_hash VARCHAR(255)
├── display_name VARCHAR(120)
├── status VARCHAR(30)
├── email_verified BOOLEAN
├── failed_login_attempts INTEGER
├── locked_until TIMESTAMPTZ
├── created_at TIMESTAMPTZ
├── updated_at TIMESTAMPTZ
└── version BIGINT

roles
├── id UUID PK
├── code VARCHAR(50) UNIQUE
├── name VARCHAR(100)
├── description VARCHAR(255)
├── system_role BOOLEAN
├── created_at TIMESTAMPTZ
├── updated_at TIMESTAMPTZ
└── version BIGINT

permissions
├── id UUID PK
├── code VARCHAR(100) UNIQUE
├── name VARCHAR(120)
├── description VARCHAR(255)
├── created_at TIMESTAMPTZ
└── updated_at TIMESTAMPTZ

user_roles
├── user_id UUID FK
├── role_id UUID FK
└── assigned_at TIMESTAMPTZ

role_permissions
├── role_id UUID FK
├── permission_id UUID FK
└── assigned_at TIMESTAMPTZ
```

---

## 4. Create Flyway migration V2

Do not modify V1 after it has been applied.

Create:

```text
src/main/resources/db/migration/V2__create_identity_rbac_tables.sql
```

Add:

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255),
    display_name VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_lowercase CHECK (email = LOWER(email)),
    CONSTRAINT ck_users_failed_login_attempts CHECK (failed_login_attempts >= 0),
    CONSTRAINT ck_users_status CHECK (
        status IN ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED', 'DISABLED')
    )
);

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_roles_code UNIQUE (code),
    CONSTRAINT ck_roles_code_uppercase CHECK (code = UPPER(code))
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_permissions_code UNIQUE (code),
    CONSTRAINT ck_permissions_code_uppercase CHECK (code = UPPER(code))
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions (permission_id);
```

### Why no database-generated UUID default?

For now, Java will generate UUIDs. This makes the identity available before persistence and keeps entity creation behavior explicit.

### Why nullable `password_hash`?

A future Google or Microsoft OIDC user may not have a local password. Local-password rules will be enforced by the authentication application layer.

---

## 5. Create Flyway migration V3 for seed data

Create:

```text
src/main/resources/db/migration/V3__seed_default_roles_permissions.sql
```

Add:

```sql
INSERT INTO roles (
    id, code, name, description, system_role,
    created_at, updated_at, version
)
VALUES
(
    '00000000-0000-0000-0000-000000000001',
    'ADMIN',
    'Administrator',
    'System administrator role',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),
(
    '00000000-0000-0000-0000-000000000002',
    'MANAGER',
    'Manager',
    'People and project manager role',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),
(
    '00000000-0000-0000-0000-000000000003',
    'EMPLOYEE',
    'Employee',
    'Standard employee role',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

INSERT INTO permissions (
    id, code, name, description, created_at, updated_at
)
VALUES
(
    '10000000-0000-0000-0000-000000000001',
    'USER_VIEW',
    'View users',
    'View user accounts',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '10000000-0000-0000-0000-000000000002',
    'USER_CREATE',
    'Create users',
    'Create user accounts',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '10000000-0000-0000-0000-000000000003',
    'ROLE_MANAGE',
    'Manage roles',
    'Create and modify role assignments',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '10000000-0000-0000-0000-000000000004',
    'PERMISSION_VIEW',
    'View permissions',
    'View permission definitions',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '10000000-0000-0000-0000-000000000005',
    'PROFILE_VIEW_SELF',
    'View own profile',
    'View the authenticated user profile',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT
    '00000000-0000-0000-0000-000000000001',
    id,
    CURRENT_TIMESTAMP
FROM permissions;

INSERT INTO role_permissions (role_id, permission_id, assigned_at)
VALUES
(
    '00000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000001',
    CURRENT_TIMESTAMP
),
(
    '00000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000005',
    CURRENT_TIMESTAMP
),
(
    '00000000-0000-0000-0000-000000000003',
    '10000000-0000-0000-0000-000000000005',
    CURRENT_TIMESTAMP
);
```

Seed only reference data. Do not seed a real administrator password or personal user account in a migration.

---

## 6. Create `UserStatus`

Create:

```text
com.workflow360.identity.domain.UserStatus
```

```java
package com.workflow360.identity.domain;

public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    LOCKED,
    DISABLED
}
```

---

## 7. Create `UserEntity`

Create:

```text
com.workflow360.identity.infrastructure.persistence.UserEntity
```

```java
package com.workflow360.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Locale;
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

    @Column(nullable = false, length = 320, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected UserEntity() {
    }

    public UserEntity(String email, String displayName, String passwordHash) {
        this.id = UUID.randomUUID();
        this.email = normalizeEmail(email);
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.status = UserStatus.PENDING_VERIFICATION;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    private static String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
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

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
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

Do not create a public `setId`. Identity must not change after construction.

---

## 8. Create `RoleEntity`

```java
package com.workflow360.identity.infrastructure.persistence;

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

    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
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
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
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
package com.workflow360.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "permissions")
public class PermissionEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100, unique = true)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PermissionEntity() {
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

Seeded reference entities do not require public constructors during this milestone.

---

## 10. Create repositories

### `UserRepository`

```java
package com.workflow360.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
```

### `RoleRepository`

```java
package com.workflow360.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByCode(String code);
}
```

### `PermissionRepository`

```java
package com.workflow360.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {

    Optional<PermissionEntity> findByCode(String code);
}
```

Do not expose repositories directly from REST controllers.

---

## 11. Why mappings are not added to entities yet

This milestone deliberately avoids a direct JPA `@ManyToMany` between users, roles and permissions.

Reasons:

- Assignment tables contain `assigned_at`.
- Future assignment metadata may include `assigned_by` or organization scope.
- Explicit assignment entities are easier to audit.
- Large bidirectional collections can cause unwanted loading and serialization.

The next RBAC application milestone will introduce explicit `UserRoleEntity` and `RolePermissionEntity` only when needed by use cases.

---

## 12. Run Flyway and Hibernate validation

Restart the Spring Boot application.

Expected sequence:

```text
Flyway validates V1
Flyway applies V2
Flyway applies V3
Hibernate validates entity mappings
Application starts successfully
```

If Hibernate reports a schema mismatch, do not switch to:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Correct either the migration or the entity mapping.

---

## 13. Verify PostgreSQL

Run in pgAdmin:

```sql
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Expected:

```text
1 | initialize database            | true
2 | create identity rbac tables    | true
3 | seed default roles permissions | true
```

Verify tables:

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
```

Verify roles:

```sql
SELECT code, name, system_role
FROM roles
ORDER BY code;
```

Expected:

```text
ADMIN
EMPLOYEE
MANAGER
```

Verify permissions:

```sql
SELECT code, name
FROM permissions
ORDER BY code;
```

Verify assignments:

```sql
SELECT r.code AS role_code,
       p.code AS permission_code
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
ORDER BY r.code, p.code;
```

---

## 14. Do not build these yet

- Registration endpoint
- Login endpoint
- BCrypt password hashing
- JWT tokens
- Refresh tokens
- Spring Security filter chain
- Admin user creation
- Role-management REST endpoint
- User-role REST endpoint
- Google login
- Chat APIs

Database correctness comes first.

---

## Definition of Done

- [ ] V2 migration exists
- [ ] V3 seed migration exists
- [ ] Existing migrations were not edited
- [ ] `users`, `roles`, `permissions`, `user_roles`, and `role_permissions` exist
- [ ] UUID primary keys are used
- [ ] Email uniqueness and lowercase constraints exist
- [ ] User status constraint exists
- [ ] Foreign keys and indexes exist
- [ ] Default roles are seeded
- [ ] Initial permissions are seeded
- [ ] `UserStatus` exists
- [ ] User, Role and Permission entities exist
- [ ] Repository interfaces exist
- [ ] Hibernate schema validation succeeds
- [ ] Spring Boot starts successfully
- [ ] React still displays backend status
- [ ] No passwords or secrets were added to migrations

---

## Next milestone

Build the identity application layer:

```text
CreateUserRequest DTO
        ↓
Bean Validation
        ↓
Password hashing
        ↓
User application service
        ↓
Default EMPLOYEE role assignment
        ↓
Centralized exception handling
        ↓
POST /api/v1/users
        ↓
Integration tests
```

Spring Security and login will follow only after safe user creation is working.
