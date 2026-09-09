# WorkFlow360 — V2 Identity and RBAC Foundation from Scratch

## Goal

Create the first business database migration for WorkFlow360 and then map the resulting tables using JPA.

This task creates:

- `users`
- `roles`
- `permissions`
- `user_roles`
- `role_permissions`
- `UserStatus`
- `UserEntity`
- `RoleEntity`
- `PermissionEntity`
- Spring Data JPA repositories

This task does **not** implement login, passwords, Spring Security, JWT, or React screens.

---

## Part 0 — Required starting condition

Complete this task only after all of the following work:

```text
PostgreSQL database: workflow360
Application user: workflow360_app
Flyway migration V1: successful
Spring Boot: starts successfully
Actuator health: UP
```

Run this query in the `workflow360` database:

```sql
SELECT version,
       description,
       installed_by,
       success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Before creating V2, the expected result is only V1:

```text
1 | initialize database | workflow360_app | true
```

If V2 is already present in `flyway_schema_history`, do not create or edit V2 again. Follow the recovery section at the end of this guide.

---

# Part 1 — Understand the V2 migration

Flyway migration filenames follow this pattern:

```text
V<version>__<description>.sql
```

For this task:

```text
V2__create_identity_rbac_tables.sql
```

Meaning:

```text
V2                             Database migration version 2
__                             Two underscores required by Flyway
create_identity_rbac_tables    Description
.sql                           SQL migration file
```

After V2 succeeds, never modify the file. Future changes must use V3, V4, and so on.

---

# Part 2 — Create the V2 migration file

In Eclipse, expand:

```text
src/main/resources
└── db
    └── migration
```

Right-click `migration` and select:

```text
New → File
```

Enter the exact filename:

```text
V2__create_identity_rbac_tables.sql
```

The final path must be:

```text
backend/src/main/resources/db/migration/V2__create_identity_rbac_tables.sql
```

Ensure Windows has not created this incorrect filename:

```text
V2__create_identity_rbac_tables.sql.txt
```

---

# Part 3 — Add the complete V2 SQL before starting Spring Boot

Paste the following complete script into V2 and save it before running the application:

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
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles (id)
        ON DELETE RESTRICT
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id)
        REFERENCES roles (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id)
        REFERENCES permissions (id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_users_status
    ON users (status);

CREATE INDEX idx_user_roles_role_id
    ON user_roles (role_id);

CREATE INDEX idx_role_permissions_permission_id
    ON role_permissions (permission_id);
```

Save the file:

```text
Ctrl + S
```

Do not start Spring Boot while the V2 file is empty or partially written.

---

# Part 4 — Understand the database design

The relationships are:

```text
users
  |
  +---- user_roles ---- roles
                         |
                         +---- role_permissions ---- permissions
```

This means:

- One user can have several roles.
- One role can be assigned to several users.
- One role can include several permissions.
- One permission can be included in several roles.

UUID is used for stable identity. The user UUID will later be referenced by employees, project memberships, task assignments, notifications, audit records, conversations, messages, and read receipts.

---

# Part 5 — Run V2 with Flyway

Before starting, verify the datasource configuration in:

```text
src/main/resources/application-local.properties
```

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/workflow360}
spring.datasource.username=${DB_USERNAME:workflow360_app}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.open-in-view=false
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true
```

In the Eclipse run configuration, ensure:

```text
DB_PASSWORD = password of workflow360_app
```

Remove `DB_USERNAME=postgres` if it exists.

In Eclipse, run:

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

Expected Flyway log sequence:

```text
Validating migrations
Migrating schema public to version 2
Successfully applied migration
Started Workflow360BackendApplication
```

---

# Part 6 — Verify V2 in pgAdmin

Open the Query Tool for the `workflow360` database.

Run:

```sql
SELECT version,
       description,
       script,
       checksum,
       installed_by,
       success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Expected result:

```text
1 | initialize database          | V1__initialize_database.sql          | ... | workflow360_app | true
2 | create identity rbac tables  | V2__create_identity_rbac_tables.sql  | ... | workflow360_app | true
```

Verify the tables:

```sql
SELECT table_name
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

---

# Part 7 — Create the Java identity package structure

Under:

```text
src/main/java/com/workflow360
```

create these packages in Eclipse:

```text
com.workflow360.identity.domain
com.workflow360.identity.infrastructure.persistence.entity
com.workflow360.identity.infrastructure.persistence.repository
```

The structure becomes:

```text
identity/
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

Do not create controllers or services yet.

---

# Part 8 — Create `UserStatus`

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

The enum values must exactly match the database check constraint.

---

# Part 9 — Create `UserEntity`

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

    public UserEntity(
            UUID id,
            String email,
            String displayName,
            UserStatus status) {
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

Do not return this entity directly from a REST controller.

---

# Part 10 — Create `RoleEntity`

Create:

```text
com.workflow360.identity.infrastructure.persistence.entity.RoleEntity
```

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

# Part 11 — Create `PermissionEntity`

Create:

```text
com.workflow360.identity.infrastructure.persistence.entity.PermissionEntity
```

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

# Part 12 — Create Spring Data repositories

## `UserRepository.java`

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

## `RoleRepository.java`

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

## `PermissionRepository.java`

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

# Part 13 — Restart and validate the JPA schema

Restart Spring Boot after creating the entities and repositories.

Because the configuration contains:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Hibernate verifies that the Java mappings agree with the Flyway-created schema.

Expected result:

```text
Spring Boot starts successfully
```

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

If Hibernate reports a missing table or column, compare the entity annotation with the V2 SQL. Do not change `ddl-auto` to `update`.

---

# Part 14 — Why join-table entities are postponed

The V2 database includes:

```text
user_roles
role_permissions
```

Do not add broad bidirectional `@ManyToMany` mappings yet.

Later, assignment records may need:

```text
assigned_by
assigned_at
expires_at
reason
```

Explicit assignment entities and services will give better control and avoid accidental loading of large relationship graphs.

---

# Recovery only if V2 is already recorded incorrectly

If the database already contains V2 with a checksum mismatch and the project has no valuable data:

1. Stop Spring Boot.
2. Connect pgAdmin Query Tool to the `postgres` database.
3. Terminate `workflow360` connections.
4. Drop and recreate `workflow360` with `workflow360_app` as owner.
5. Confirm V1 and the completed V2 files are saved.
6. Start Spring Boot and let Flyway apply both migrations from scratch.

Do not run Flyway repair when V2 was previously recorded while empty. Repair updates migration metadata but does not execute the newly added SQL statements.

---

# Rules for this task

Do not:

- Start Spring Boot before V2 is complete and saved
- Edit V2 after it succeeds
- Put a password column in `users` during this task
- Add Spring Security or JWT yet
- Use `postgres` as the Spring Boot datasource user
- Use `spring.jpa.hibernate.ddl-auto=update`
- Return JPA entities from controllers
- Add `@ManyToMany` mappings yet
- Create chat tables yet

---

# Definition of Done

- [ ] V1 exists and is successful
- [ ] V2 file exists with the exact correct name
- [ ] V2 contains the complete SQL before application startup
- [ ] V2 is successful in `flyway_schema_history`
- [ ] V2 was installed by `workflow360_app`
- [ ] `users` table exists
- [ ] `roles` table exists
- [ ] `permissions` table exists
- [ ] `user_roles` table exists
- [ ] `role_permissions` table exists
- [ ] Three indexes exist
- [ ] `UserStatus` exists
- [ ] `UserEntity` exists
- [ ] `RoleEntity` exists
- [ ] `PermissionEntity` exists
- [ ] Three repositories exist
- [ ] Hibernate validation succeeds
- [ ] Spring Boot starts
- [ ] Actuator health returns `UP`
- [ ] No authentication code has been added yet

---

# Next Task

After this task is complete:

```text
V3 seed default roles and permissions
        ↓
User request/response DTOs
        ↓
User application service
        ↓
Admin-created user API
        ↓
Centralized validation and error handling
        ↓
Tests
```
