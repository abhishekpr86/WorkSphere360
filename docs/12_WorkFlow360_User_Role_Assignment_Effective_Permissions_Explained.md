# WorkFlow360 — User Role Assignment and Effective Permissions, Explained

## Goal

Connect users to the roles seeded in Flyway V3.

By the end of this task, WorkFlow360 will support:

- Assigning a role to a user
- Removing a role from a user
- Listing the roles assigned to a user
- Listing a user's effective permissions
- Preventing duplicate role assignments
- Using the existing `user_roles` join table correctly
- Mapping a composite primary key with JPA
- Returning consistent API errors
- Testing the complete RBAC assignment flow

This task does not yet add Spring Security, login, passwords, or JWT. This task prepares the authorization data that Spring Security will use later.

---

# 1. Prerequisite Check

Before starting, confirm:

```text
Task 11 user search works
Pagination works
Sorting works
Display-name update works
Status update works
V1, V2, and V3 succeeded
ADMIN, MANAGER, and EMPLOYEE roles exist
At least one user exists
```

Verify migrations:

```sql
SELECT version,
       description,
       installed_by,
       success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Verify roles:

```sql
SELECT id, code, name
FROM roles
ORDER BY code;
```

Verify users:

```sql
SELECT id, email, display_name, status
FROM users
ORDER BY created_at DESC;
```

---

# 2. Understand the Relationship

The database already contains:

```text
users
  |
  | user_id
  v
user_roles
  ^
  | role_id
  |
roles
```

The `user_roles` table is:

```sql
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
```

One row means one assignment:

```text
user_id = Abhishek's user UUID
role_id = EMPLOYEE role UUID
```

The combination of `user_id` and `role_id` is unique.

That means:

```text
The same user can have ADMIN and EMPLOYEE
The same role can belong to many users
The same role cannot be assigned twice to the same user
```

---

# 3. What Is a Composite Primary Key?

Most entities have one primary-key field:

```java
@Id
private UUID id;
```

But `user_roles` is identified by two columns together:

```text
user_id + role_id
```

Neither column is unique by itself.

Example:

```text
User A + EMPLOYEE  -> one unique assignment
User A + MANAGER   -> another unique assignment
User B + EMPLOYEE  -> another unique assignment
```

Therefore, JPA needs one Java type representing both key values.

We will use:

```java
@Embeddable
```

for the key class and:

```java
@EmbeddedId
```

inside the entity.

---

# 4. Package Structure

Add:

```text
com.workflow360.identity/
├── api/
│   ├── UserRoleController.java
│   └── dto/
│       ├── AssignRoleRequest.java
│       ├── PermissionResponse.java
│       ├── RoleResponse.java
│       └── UserAccessResponse.java
├── application/
│   ├── UserAccessMapper.java
│   └── UserRoleService.java
└── infrastructure/
    └── persistence/
        ├── entity/
        │   ├── UserRoleEntity.java
        │   └── UserRoleId.java
        └── repository/
            └── UserRoleRepository.java
```

Also update:

```text
RoleRepository.java
PermissionRepository.java
GlobalExceptionHandler.java
```

No Flyway migration is required because V2 already created `user_roles`.

---

# 5. Create `UserRoleId`

Create:

```text
com.workflow360.identity.infrastructure.persistence.entity.UserRoleId
```

```java
package com.workflow360.identity.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class UserRoleId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    protected UserRoleId() {
    }

    public UserRoleId(UUID userId, UUID roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof UserRoleId that)) {
            return false;
        }

        return Objects.equals(userId, that.userId)
                && Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }
}
```

## Why `@Embeddable`?

`@Embeddable` tells JPA that this type may be embedded inside an entity.

It is not a separate database table. It is a reusable value object containing columns that belong to the owning entity.

## Why `Serializable`?

JPA composite-key types must be serializable so persistence providers can safely use them for identity management, caching, and detached entities.

## Why `equals()` and `hashCode()`?

JPA and Java collections must know whether two key objects represent the same database row.

```java
new UserRoleId(userId, roleId)
```

and another object with the same two UUID values should be equal.

---

# 6. Create `UserRoleEntity`

Create:

```text
com.workflow360.identity.infrastructure.persistence.entity.UserRoleEntity
```

```java
package com.workflow360.identity.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_roles")
public class UserRoleEntity {

    @EmbeddedId
    private UserRoleId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    protected UserRoleEntity() {
    }

    public UserRoleEntity(UserEntity user, RoleEntity role) {
        this.id = new UserRoleId(user.getId(), role.getId());
        this.user = user;
        this.role = role;
    }

    @PrePersist
    void onCreate() {
        assignedAt = Instant.now();
    }

    public UserRoleId getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public RoleEntity getRole() {
        return role;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}
```

## Why `@EmbeddedId`?

`@EmbeddedId` marks `UserRoleId` as the composite primary key of `UserRoleEntity`.

## Why `@MapsId("userId")`?

It tells JPA:

```text
UserRoleId.userId
```

and:

```text
UserRoleEntity.user
```

use the same database column, `user_id`.

The same idea applies to `roleId` and `role`.

## Why `FetchType.LAZY`?

Loading an assignment should not automatically load every field from both the user and role unless the application needs them.

## Why not use `@ManyToMany`?

`user_roles` is not merely a hidden join table. It has business data:

```text
assigned_at
```

Later it may include:

```text
assigned_by
expires_at
reason
```

An explicit entity gives us control over assignment behavior and auditing.

---

# 7. Confirm Required Getters in `RoleEntity`

`UserRoleEntity` needs:

```java
role.getId()
```

Ensure `RoleEntity` contains:

```java
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
```

---

# 8. Create `UserRoleRepository`

Create:

```text
com.workflow360.identity.infrastructure.persistence.repository.UserRoleRepository
```

```java
package com.workflow360.identity.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow360.identity.infrastructure.persistence.entity.UserRoleEntity;
import com.workflow360.identity.infrastructure.persistence.entity.UserRoleId;

public interface UserRoleRepository
        extends JpaRepository<UserRoleEntity, UserRoleId> {

    boolean existsByIdUserIdAndIdRoleId(
            UUID userId,
            UUID roleId
    );

    List<UserRoleEntity> findByIdUserIdOrderByRoleCodeAsc(
            UUID userId
    );

    long deleteByIdUserIdAndIdRoleId(
            UUID userId,
            UUID roleId
    );

    @Query("""
        SELECT DISTINCT permission.code
        FROM UserRoleEntity userRole
        JOIN RolePermissionEntity rolePermission
          ON rolePermission.id.roleId = userRole.id.roleId
        JOIN rolePermission.permission permission
        WHERE userRole.id.userId = :userId
        ORDER BY permission.code
        """)
    List<String> findEffectivePermissionCodes(
            @Param("userId") UUID userId
    );
}
```

## Important pause

The effective-permission query above requires a `RolePermissionEntity`, which has not yet been created. To keep this task clear, create the role-permission mapping in the next section before running the application.

## Understanding the derived method names

```java
existsByIdUserIdAndIdRoleId
```

means:

```text
id.userId equals userId
AND
id.roleId equals roleId
```

Spring Data follows the nested Java property path inside `UserRoleId`.

---

# 9. Create `RolePermissionId`

Because `role_permissions` also has a composite key, create:

```text
com.workflow360.identity.infrastructure.persistence.entity.RolePermissionId
```

```java
package com.workflow360.identity.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class RolePermissionId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;

    protected RolePermissionId() {
    }

    public RolePermissionId(UUID roleId, UUID permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public UUID getPermissionId() {
        return permissionId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof RolePermissionId that)) {
            return false;
        }

        return Objects.equals(roleId, that.roleId)
                && Objects.equals(permissionId, that.permissionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, permissionId);
    }
}
```

---

# 10. Create `RolePermissionEntity`

Create:

```text
com.workflow360.identity.infrastructure.persistence.entity.RolePermissionEntity
```

```java
package com.workflow360.identity.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "role_permissions")
public class RolePermissionEntity {

    @EmbeddedId
    private RolePermissionId id;

    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @MapsId("permissionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private PermissionEntity permission;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    protected RolePermissionEntity() {
    }

    public RolePermissionId getId() {
        return id;
    }

    public RoleEntity getRole() {
        return role;
    }

    public PermissionEntity getPermission() {
        return permission;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}
```

V3 already inserted these rows. This entity maps existing data so JPA can query it.

---

# 11. Create `RolePermissionRepository`

```java
package com.workflow360.identity.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow360.identity.infrastructure.persistence.entity.RolePermissionEntity;
import com.workflow360.identity.infrastructure.persistence.entity.RolePermissionId;

public interface RolePermissionRepository
        extends JpaRepository<RolePermissionEntity, RolePermissionId> {

    List<RolePermissionEntity> findByIdRoleIdOrderByPermissionCodeAsc(
            UUID roleId
    );
}
```

Ensure `PermissionEntity` has `getId()`, `getCode()`, `getName()`, and `getDescription()`.

---

# 12. Simplify the Effective-Permission Query

Now that `RolePermissionEntity` exists, use this final `UserRoleRepository`:

```java
package com.workflow360.identity.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow360.identity.infrastructure.persistence.entity.UserRoleEntity;
import com.workflow360.identity.infrastructure.persistence.entity.UserRoleId;

public interface UserRoleRepository
        extends JpaRepository<UserRoleEntity, UserRoleId> {

    boolean existsByIdUserIdAndIdRoleId(
            UUID userId,
            UUID roleId
    );

    List<UserRoleEntity> findByIdUserIdOrderByRoleCodeAsc(
            UUID userId
    );

    long deleteByIdUserIdAndIdRoleId(
            UUID userId,
            UUID roleId
    );

    @Query("""
        SELECT DISTINCT permission.code
        FROM UserRoleEntity userRole,
             RolePermissionEntity rolePermission
        JOIN rolePermission.permission permission
        WHERE userRole.id.userId = :userId
          AND rolePermission.id.roleId = userRole.id.roleId
        ORDER BY permission.code
        """)
    List<String> findEffectivePermissionCodes(
            @Param("userId") UUID userId
    );
}
```

`DISTINCT` is important because two roles may grant the same permission. The API should return that permission only once.

---

# 13. Create API DTOs

## `AssignRoleRequest`

```java
package com.workflow360.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AssignRoleRequest(

        @NotBlank(message = "Role code is required")
        @Pattern(
            regexp = "^[A-Z][A-Z0-9_]{1,49}$",
            message = "Role code must contain uppercase letters, numbers, or underscores"
        )
        String roleCode
) {
}
```

The client sends `EMPLOYEE`, not a role UUID. Stable codes are easier and safer for API clients.

## `RoleResponse`

```java
package com.workflow360.identity.api.dto;

import java.time.Instant;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String code,
        String name,
        String description,
        boolean systemRole,
        Instant assignedAt
) {
}
```

## `PermissionResponse`

```java
package com.workflow360.identity.api.dto;

public record PermissionResponse(
        String code
) {
}
```

## `UserAccessResponse`

```java
package com.workflow360.identity.api.dto;

import java.util.List;
import java.util.UUID;

public record UserAccessResponse(
        UUID userId,
        List<RoleResponse> roles,
        List<PermissionResponse> effectivePermissions
) {
}
```

---

# 14. Create `UserAccessMapper`

```java
package com.workflow360.identity.application;

import java.util.List;

import org.springframework.stereotype.Component;

import com.workflow360.identity.api.dto.PermissionResponse;
import com.workflow360.identity.api.dto.RoleResponse;
import com.workflow360.identity.infrastructure.persistence.entity.UserRoleEntity;

@Component
public class UserAccessMapper {

    public RoleResponse toRoleResponse(UserRoleEntity assignment) {
        return new RoleResponse(
                assignment.getRole().getId(),
                assignment.getRole().getCode(),
                assignment.getRole().getName(),
                assignment.getRole().getDescription(),
                assignment.getRole().isSystemRole(),
                assignment.getAssignedAt()
        );
    }

    public List<PermissionResponse> toPermissionResponses(
            List<String> permissionCodes) {

        return permissionCodes.stream()
                .map(PermissionResponse::new)
                .toList();
    }
}
```

The mapper converts persistence objects into API-safe response objects.

---

# 15. Create `RoleAssignmentAlreadyExistsException`

```java
package com.workflow360.common.exception;

public class RoleAssignmentAlreadyExistsException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RoleAssignmentAlreadyExistsException(String message) {
        super(message);
    }
}
```

This exception is more specific than the general resource conflict and makes the business failure clear.

---

# 16. Create `RoleAssignmentNotFoundException`

```java
package com.workflow360.common.exception;

public class RoleAssignmentNotFoundException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RoleAssignmentNotFoundException(String message) {
        super(message);
    }
}
```

This is used when a client tries to remove a role the user does not have.

---

# 17. Extend `GlobalExceptionHandler`

Add:

```java
@ExceptionHandler(RoleAssignmentAlreadyExistsException.class)
public ResponseEntity<ApiErrorResponse> handleDuplicateRoleAssignment(
        RoleAssignmentAlreadyExistsException exception,
        HttpServletRequest request) {

    ApiErrorResponse response = new ApiErrorResponse(
            Instant.now(),
            HttpStatus.CONFLICT.value(),
            "ROLE_ALREADY_ASSIGNED",
            exception.getMessage(),
            request.getRequestURI(),
            List.of()
    );

    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
}

@ExceptionHandler(RoleAssignmentNotFoundException.class)
public ResponseEntity<ApiErrorResponse> handleMissingRoleAssignment(
        RoleAssignmentNotFoundException exception,
        HttpServletRequest request) {

    ApiErrorResponse response = new ApiErrorResponse(
            Instant.now(),
            HttpStatus.NOT_FOUND.value(),
            "ROLE_ASSIGNMENT_NOT_FOUND",
            exception.getMessage(),
            request.getRequestURI(),
            List.of()
    );

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
}
```

---

# 18. Create `UserRoleService`

```java
package com.workflow360.identity.application;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow360.common.exception.ResourceNotFoundException;
import com.workflow360.common.exception.RoleAssignmentAlreadyExistsException;
import com.workflow360.common.exception.RoleAssignmentNotFoundException;
import com.workflow360.identity.api.dto.AssignRoleRequest;
import com.workflow360.identity.api.dto.RoleResponse;
import com.workflow360.identity.api.dto.UserAccessResponse;
import com.workflow360.identity.infrastructure.persistence.entity.RoleEntity;
import com.workflow360.identity.infrastructure.persistence.entity.UserEntity;
import com.workflow360.identity.infrastructure.persistence.entity.UserRoleEntity;
import com.workflow360.identity.infrastructure.persistence.repository.RoleRepository;
import com.workflow360.identity.infrastructure.persistence.repository.UserRepository;
import com.workflow360.identity.infrastructure.persistence.repository.UserRoleRepository;

@Service
@Transactional(readOnly = true)
public class UserRoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserAccessMapper userAccessMapper;

    public UserRoleService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            UserAccessMapper userAccessMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.userAccessMapper = userAccessMapper;
    }

    @Transactional
    public RoleResponse assignRole(
            UUID userId,
            AssignRoleRequest request) {

        UserEntity user = findUser(userId);
        String normalizedRoleCode = request.roleCode()
                .trim()
                .toUpperCase(Locale.ROOT);
        RoleEntity role = findRole(normalizedRoleCode);

        if (userRoleRepository.existsByIdUserIdAndIdRoleId(
                user.getId(),
                role.getId())) {

            throw new RoleAssignmentAlreadyExistsException(
                    "Role " + role.getCode()
                    + " is already assigned to user " + userId
            );
        }

        UserRoleEntity assignment = new UserRoleEntity(user, role);
        UserRoleEntity saved = userRoleRepository.save(assignment);

        return userAccessMapper.toRoleResponse(saved);
    }

    @Transactional
    public void removeRole(UUID userId, String roleCode) {
        UserEntity user = findUser(userId);
        RoleEntity role = findRole(
                roleCode.trim().toUpperCase(Locale.ROOT)
        );

        long deleted = userRoleRepository.deleteByIdUserIdAndIdRoleId(
                user.getId(),
                role.getId()
        );

        if (deleted == 0) {
            throw new RoleAssignmentNotFoundException(
                    "Role " + role.getCode()
                    + " is not assigned to user " + userId
            );
        }
    }

    public UserAccessResponse getUserAccess(UUID userId) {
        findUser(userId);

        List<RoleResponse> roles = userRoleRepository
                .findByIdUserIdOrderByRoleCodeAsc(userId)
                .stream()
                .map(userAccessMapper::toRoleResponse)
                .toList();

        var permissions = userAccessMapper.toPermissionResponses(
                userRoleRepository.findEffectivePermissionCodes(userId)
        );

        return new UserAccessResponse(
                userId,
                roles,
                permissions
        );
    }

    private UserEntity findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID " + userId
                ));
    }

    private RoleEntity findRole(String roleCode) {
        return roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role not found with code " + roleCode
                ));
    }
}
```

## Why normalize role code?

These values should all find the same role:

```text
employee
EMPLOYEE
 Employee 
```

The stored stable code is uppercase.

## Why are writes transactional?

Assignment and removal change PostgreSQL data. A transaction guarantees each use case either completes or rolls back.

---

# 19. Create `UserRoleController`

```java
package com.workflow360.identity.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow360.identity.api.dto.AssignRoleRequest;
import com.workflow360.identity.api.dto.RoleResponse;
import com.workflow360.identity.api.dto.UserAccessResponse;
import com.workflow360.identity.application.UserRoleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users/{userId}")
public class UserRoleController {

    private final UserRoleService userRoleService;

    public UserRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @PostMapping("/roles")
    public ResponseEntity<RoleResponse> assignRole(
            @PathVariable UUID userId,
            @Valid @RequestBody AssignRoleRequest request) {

        RoleResponse response = userRoleService.assignRole(
                userId,
                request
        );

        URI location = URI.create(
                "/api/v1/users/" + userId
                + "/roles/" + response.code()
        );

        return ResponseEntity.created(location).body(response);
    }

    @DeleteMapping("/roles/{roleCode}")
    public ResponseEntity<Void> removeRole(
            @PathVariable UUID userId,
            @PathVariable String roleCode) {

        userRoleService.removeRole(userId, roleCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/access")
    public ResponseEntity<UserAccessResponse> getUserAccess(
            @PathVariable UUID userId) {

        return ResponseEntity.ok(
                userRoleService.getUserAccess(userId)
        );
    }
}
```

## Endpoint meanings

```http
POST /api/v1/users/{userId}/roles
```

Assigns one role.

```http
DELETE /api/v1/users/{userId}/roles/{roleCode}
```

Removes one role.

```http
GET /api/v1/users/{userId}/access
```

Returns assigned roles and the combined effective permissions.

---

# 20. End-to-End Assignment Flow

Request:

```http
POST /api/v1/users/{userId}/roles
Content-Type: application/json
```

```json
{
  "roleCode": "EMPLOYEE"
}
```

Flow:

```text
1. Controller reads userId from the path
2. @Valid validates the request body
3. Service loads the user
4. Service normalizes EMPLOYEE
5. Service loads the role by code
6. Repository checks whether the assignment exists
7. Service creates UserRoleEntity
8. UserRoleEntity creates UserRoleId
9. Repository inserts into user_roles
10. @PrePersist sets assignedAt
11. Mapper creates RoleResponse
12. Controller returns 201 Created
```

---

# 21. Effective Permission Flow

Suppose a user has:

```text
EMPLOYEE
MANAGER
```

EMPLOYEE grants:

```text
TASK_VIEW
TASK_UPDATE
TIMESHEET_SUBMIT
LEAVE_APPLY
NOTIFICATION_VIEW
CHAT_USE
```

MANAGER grants some of the same permissions plus management permissions.

The effective permission query returns the union:

```text
All permissions from EMPLOYEE
+
All permissions from MANAGER
-
Duplicates
```

`DISTINCT` removes repeated permission codes such as `TASK_VIEW` and `CHAT_USE`.

---

# 22. Test in Swagger

## Assign EMPLOYEE

```http
POST /api/v1/users/{actual-user-uuid}/roles
```

Body:

```json
{
  "roleCode": "EMPLOYEE"
}
```

Expected:

```text
201 Created
```

## Assign the same role again

Expected:

```text
409 Conflict
code = ROLE_ALREADY_ASSIGNED
```

## Assign unknown role

```json
{
  "roleCode": "UNKNOWN_ROLE"
}
```

Expected:

```text
404 Not Found
code = RESOURCE_NOT_FOUND
```

## View user access

```http
GET /api/v1/users/{actual-user-uuid}/access
```

Expected response:

```json
{
  "userId": "actual-user-uuid",
  "roles": [
    {
      "id": "10000000-0000-0000-0000-000000000003",
      "code": "EMPLOYEE",
      "name": "Employee",
      "description": "Standard employee access role",
      "systemRole": true,
      "assignedAt": "..."
    }
  ],
  "effectivePermissions": [
    { "code": "CHAT_USE" },
    { "code": "LEAVE_APPLY" },
    { "code": "NOTIFICATION_VIEW" },
    { "code": "TASK_UPDATE" },
    { "code": "TASK_VIEW" },
    { "code": "TIMESHEET_SUBMIT" }
  ]
}
```

## Remove role

```http
DELETE /api/v1/users/{actual-user-uuid}/roles/EMPLOYEE
```

Expected:

```text
204 No Content
```

---

# 23. Verify PostgreSQL

```sql
SELECT ur.user_id,
       r.code AS role_code,
       r.name AS role_name,
       ur.assigned_at
FROM user_roles ur
JOIN roles r ON r.id = ur.role_id
ORDER BY ur.assigned_at DESC;
```

For one user:

```sql
SELECT ur.user_id,
       r.code,
       ur.assigned_at
FROM user_roles ur
JOIN roles r ON r.id = ur.role_id
WHERE ur.user_id = 'REPLACE-WITH-USER-UUID'::UUID
ORDER BY r.code;
```

Effective permissions in SQL:

```sql
SELECT DISTINCT p.code
FROM user_roles ur
JOIN role_permissions rp ON rp.role_id = ur.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE ur.user_id = 'REPLACE-WITH-USER-UUID'::UUID
ORDER BY p.code;
```

---

# 24. Important Rules

Do not:

- Insert user-role assignments manually in pgAdmin
- Accept role IDs from normal API clients when stable role codes are clearer
- Add a direct `@ManyToMany` mapping
- Return JPA entities from controllers
- Delete built-in roles
- Add Spring Security before this access data is verified
- Edit V2 or V3

---

# 25. Definition of Done

- [ ] `UserRoleId` exists
- [ ] Composite key implements `Serializable`
- [ ] Composite key defines `equals` and `hashCode`
- [ ] `UserRoleEntity` maps `user_roles`
- [ ] `RolePermissionId` exists
- [ ] `RolePermissionEntity` maps `role_permissions`
- [ ] `UserRoleRepository` exists
- [ ] `RolePermissionRepository` exists
- [ ] Assignment DTOs and responses exist
- [ ] Assignment service exists
- [ ] Assignment controller exists
- [ ] Assign role returns 201
- [ ] Duplicate assignment returns 409
- [ ] Unknown user returns 404
- [ ] Unknown role returns 404
- [ ] Remove role returns 204
- [ ] Missing assignment returns 404
- [ ] User access endpoint returns roles
- [ ] User access endpoint returns distinct effective permissions
- [ ] PostgreSQL contains expected assignments
- [ ] Unit tests pass
- [ ] Actuator health remains UP

---

# 26. Interview Explanation

> I modeled user-role and role-permission assignments as explicit JPA entities because the join tables contain assignment metadata and may gain additional audit information. Both tables use composite primary keys, mapped with `@Embeddable` and `@EmbeddedId`, while `@MapsId` connects key fields to lazy relationships. The application service assigns and removes roles transactionally, prevents duplicate assignments, and calculates effective permissions as the distinct union of permissions granted by all assigned roles. The REST API uses stable role codes and consistent 201, 204, 404, and 409 responses.

---

# 27. Next Task

After this task:

```text
V4 local credential table
        ↓
Password hashing using BCrypt or Argon2
        ↓
Spring Security filter chain
        ↓
Authentication service
        ↓
Login endpoint
        ↓
JWT access tokens
        ↓
Refresh-token strategy
        ↓
Permission-based method authorization
        ↓
Authenticated user endpoint
```
