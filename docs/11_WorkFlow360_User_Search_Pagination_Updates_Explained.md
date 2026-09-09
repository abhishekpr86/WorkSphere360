# WorkFlow360 — User Search, Pagination, Sorting, and Updates, Explained

## Goal

Improve the first User Management API so it remains usable when the `users` table grows.

This task adds:

- Paginated user listing
- Sorting with an approved-field allowlist
- Search by email or display name
- Optional filtering by user status
- Updating a user's display name
- Changing a user's status through controlled business rules
- Optimistic-locking checks using the `version` field
- Better handling of malformed request parameters
- Unit and API testing guidance

This task does **not** add:

- Passwords or login
- JWT or Spring Security
- User-role assignment
- Employee records
- React user-management screens
- Chat implementation

Role assignment will be the next separate task because `user_roles` has its own model, authorization rules, transactions, and tests.

---

# 1. Why the Existing List API Must Change

The current endpoint returns every user:

```http
GET /api/v1/users
```

The repository method is similar to:

```java
List<UserEntity> findAllByOrderByCreatedAtDesc();
```

This is acceptable with five users, but dangerous with thousands of users.

Without pagination:

```text
PostgreSQL reads every matching row
        ↓
Hibernate creates every entity
        ↓
The backend keeps every user in memory
        ↓
Jackson serializes the full collection
        ↓
The network transfers a large response
        ↓
The browser loads too much data
```

Pagination asks PostgreSQL for only one small section at a time.

Spring Data represents a pagination request with `Pageable` and the result with `Page<T>`. `Pageable` carries page number, page size, offset, and sorting information. Page numbering is zero-based, so page `0` is the first page. citeturn49search19turn49search24

---

# 2. API Design

## Paginated search endpoint

```http
GET /api/v1/users?page=0&size=20&sortBy=createdAt&direction=desc
```

Optional search:

```http
GET /api/v1/users?query=abhishek&page=0&size=20
```

Optional status filter:

```http
GET /api/v1/users?status=PENDING&page=0&size=20
```

Combined search and status filter:

```http
GET /api/v1/users?query=example.com&status=ACTIVE&page=0&size=20
```

## Update display name

```http
PUT /api/v1/users/{id}
```

```json
{
  "displayName": "Abhishek P R",
  "version": 0
}
```

## Change user status

```http
PATCH /api/v1/users/{id}/status
```

```json
{
  "status": "ACTIVE",
  "version": 1
}
```

`@PatchMapping` is Spring's shortcut for a controller method that handles HTTP PATCH requests. PATCH is appropriate here because only one part of the user resource, the status, is being changed. citeturn49search25turn49search30

---

# 3. Why Separate Profile Update and Status Update?

A display-name update and a status transition are different use cases.

```text
Update display name
└── Changes profile information

Change status
└── Changes account lifecycle and authorization behavior
```

If the same generic update endpoint accepted every property, a client might accidentally or deliberately change:

```text
id
email verification
created timestamp
status
version
```

Separate request DTOs make the allowed changes explicit.

---

# 4. New Files and Updated Structure

```text
com.workflow360/
├── common/
│   ├── api/
│   │   └── PageResponse.java
│   └── exception/
│       ├── InvalidRequestParameterException.java
│       └── OptimisticConflictException.java
│
└── identity/
    ├── api/
    │   ├── UserController.java
    │   └── dto/
    │       ├── ChangeUserStatusRequest.java
    │       ├── UpdateUserRequest.java
    │       └── UserResponse.java
    ├── application/
    │   ├── UserMapper.java
    │   └── UserService.java
    ├── domain/
    │   └── UserStatus.java
    └── infrastructure/
        └── persistence/
            ├── entity/
            │   └── UserEntity.java
            └── repository/
                └── UserRepository.java
```

No Flyway migration is required for this task because the required columns and indexes already exist in V2.

---

# 5. Create `PageResponse`

## Why this file exists

Returning Spring's `Page<UserEntity>` directly would expose framework and persistence details in the REST contract.

A custom response keeps the API stable and understandable.

Create:

```text
com.workflow360.common.api.PageResponse
```

```java
package com.workflow360.common.api;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
```

## Field meanings

```text
content        Records in the current page
page           Current zero-based page number
size           Requested page size
totalElements  Total matching rows in PostgreSQL
totalPages     Number of pages at the requested size
first          Whether this is the first page
last           Whether this is the final page
```

Example response:

```json
{
  "content": [
    {
      "id": "...",
      "email": "abhishek@example.com",
      "displayName": "Abhishek",
      "status": "PENDING",
      "emailVerified": false,
      "createdAt": "...",
      "updatedAt": "...",
      "version": 0
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

# 6. Update `UserRepository`

## Why the repository needs a paginated search query

The repository should ask PostgreSQL to perform filtering, sorting, and pagination. Fetching every user and filtering in Java would waste memory and database capability.

Replace the previous unpaged list method with:

```java
package com.workflow360.identity.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow360.identity.domain.UserStatus;
import com.workflow360.identity.infrastructure.persistence.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
        SELECT user
        FROM UserEntity user
        WHERE (
            :query IS NULL
            OR LOWER(user.email) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(user.displayName) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        AND (
            :status IS NULL
            OR user.status = :status
        )
        """)
    Page<UserEntity> search(
            @Param("query") String query,
            @Param("status") UserStatus status,
            Pageable pageable
    );
}
```

## Why return `Page<UserEntity>`?

`Page` contains the current records plus pagination metadata, including total matching rows and total pages.

## Why accept `Pageable`?

`Pageable` tells Spring Data:

```text
which page
how many records
what sorting
```

A repository method that accepts `Pageable` can return a `Page` or `Slice`; a `Page` additionally provides total-count metadata. citeturn49search20turn49search24

## Why use JPQL instead of native SQL here?

JPQL uses the entity and Java property names:

```text
UserEntity
user.email
user.displayName
user.status
```

It is translated by Hibernate into PostgreSQL SQL.

## Why `:query IS NULL`?

When no search text is supplied, the text-search condition is disabled.

## Why use `LOWER`?

Search should not depend on capitalization.

```text
Abhishek
abhishek
ABHISHEK
```

all match the same text.

## Why use `LIKE '%value%'`?

It performs a contains search.

For large production datasets, this may require PostgreSQL trigram indexes or a search-specific design. The current implementation is appropriate for the foundation stage.

---

# 7. Create `UpdateUserRequest`

Create:

```text
com.workflow360.identity.api.dto.UpdateUserRequest
```

```java
package com.workflow360.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @NotBlank(message = "Display name is required")
        @Size(
            min = 2,
            max = 150,
            message = "Display name must contain between 2 and 150 characters"
        )
        String displayName,

        @PositiveOrZero(message = "Version must be zero or greater")
        long version
) {
}
```

## Why include `version`?

Imagine two administrators open the same user at version `0`.

```text
Administrator A changes the display name
Administrator B changes the display name
```

Without version checking, the second update silently overwrites the first.

The client sends the version it originally read. The backend compares it with the current entity version.

```text
Request version equals entity version
└── Update is allowed

Request version differs from entity version
└── Return 409 Conflict
```

Hibernate's `@Version` still provides the final concurrent-update protection at flush time.

---

# 8. Create `ChangeUserStatusRequest`

Create:

```text
com.workflow360.identity.api.dto.ChangeUserStatusRequest
```

```java
package com.workflow360.identity.api.dto;

import com.workflow360.identity.domain.UserStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ChangeUserStatusRequest(

        @NotNull(message = "User status is required")
        UserStatus status,

        @PositiveOrZero(message = "Version must be zero or greater")
        long version
) {
}
```

## Why accept the enum instead of a free-form String?

The JSON value must match one of:

```text
PENDING
ACTIVE
LOCKED
DISABLED
```

Jackson converts the JSON value to `UserStatus`. An unrecognized value is rejected instead of reaching the service as an arbitrary string.

---

# 9. Add Controlled Behavior to `UserEntity`

## Why entity behavior is useful

The service should orchestrate use cases, but the entity should protect its own state changes.

Add these methods to `UserEntity`:

```java
public void updateDisplayName(String displayName) {
    this.displayName = displayName;
}

public void changeStatus(UserStatus newStatus) {
    if (this.status == newStatus) {
        return;
    }

    if (this.status == UserStatus.DISABLED
            && newStatus != UserStatus.DISABLED) {
        throw new IllegalStateException(
                "A disabled user cannot be reactivated directly"
        );
    }

    this.status = newStatus;
}
```

## Why not use `setDisplayName` and `setStatus`?

Method names should describe business operations.

```text
setStatus        Generic property manipulation
changeStatus     Business action
```

## Initial status rule

This first rule prevents a disabled account from being directly reactivated. The rule can evolve later when reactivation requires an explicit administrative workflow.

---

# 10. Create New Exception Types

## `InvalidRequestParameterException`

Used when the client asks for an unsupported sort field, invalid page size, or other invalid query parameter.

```java
package com.workflow360.common.exception;

public class InvalidRequestParameterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidRequestParameterException(String message) {
        super(message);
    }
}
```

## `OptimisticConflictException`

Used when the request carries an outdated version.

```java
package com.workflow360.common.exception;

public class OptimisticConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OptimisticConflictException(String message) {
        super(message);
    }
}
```

---

# 11. Extend `GlobalExceptionHandler`

Add handlers for invalid parameters and stale versions:

```java
@ExceptionHandler(InvalidRequestParameterException.class)
public ResponseEntity<ApiErrorResponse> handleInvalidParameter(
        InvalidRequestParameterException exception,
        HttpServletRequest request) {

    ApiErrorResponse response = new ApiErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "INVALID_REQUEST_PARAMETER",
            exception.getMessage(),
            request.getRequestURI(),
            List.of()
    );

    return ResponseEntity.badRequest().body(response);
}

@ExceptionHandler(OptimisticConflictException.class)
public ResponseEntity<ApiErrorResponse> handleOptimisticConflict(
        OptimisticConflictException exception,
        HttpServletRequest request) {

    ApiErrorResponse response = new ApiErrorResponse(
            Instant.now(),
            HttpStatus.CONFLICT.value(),
            "STALE_RESOURCE_VERSION",
            exception.getMessage(),
            request.getRequestURI(),
            List.of()
    );

    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(response);
}
```

Also handle Hibernate/Spring optimistic-locking failures that occur during flush:

```java
import org.springframework.orm.ObjectOptimisticLockingFailureException;
```

```java
@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
public ResponseEntity<ApiErrorResponse> handleOptimisticLockingFailure(
        ObjectOptimisticLockingFailureException exception,
        HttpServletRequest request) {

    ApiErrorResponse response = new ApiErrorResponse(
            Instant.now(),
            HttpStatus.CONFLICT.value(),
            "STALE_RESOURCE_VERSION",
            "The user was modified by another request. Reload and try again.",
            request.getRequestURI(),
            List.of()
    );

    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(response);
}
```

## Why handle both conflict types?

```text
Explicit version comparison
└── Produces OptimisticConflictException early

Concurrent database update during the transaction
└── Produces ObjectOptimisticLockingFailureException at flush
```

Both should become an HTTP `409 Conflict`.

---

# 12. Update `UserMapper`

Add a method that converts a Spring `Page` into the WorkFlow360 page contract:

```java
package com.workflow360.identity.application;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.workflow360.common.api.PageResponse;
import com.workflow360.identity.api.dto.UserResponse;
import com.workflow360.identity.infrastructure.persistence.entity.UserEntity;

@Component
public class UserMapper {

    public UserResponse toResponse(UserEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.getStatus(),
                entity.isEmailVerified(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    public PageResponse<UserResponse> toPageResponse(
            Page<UserEntity> page) {

        return new PageResponse<>(
                page.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
```

## Why convert page content separately?

The page contains entities, but the API must return DTOs. Each entity is mapped, while pagination metadata is preserved.

---

# 13. Update `UserService`

Add imports:

```java
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
```

Add DTO and exception imports as required.

Use this complete service structure:

```java
package com.workflow360.identity.application;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow360.common.api.PageResponse;
import com.workflow360.common.exception.InvalidRequestParameterException;
import com.workflow360.common.exception.OptimisticConflictException;
import com.workflow360.common.exception.ResourceAlreadyExistsException;
import com.workflow360.common.exception.ResourceNotFoundException;
import com.workflow360.identity.api.dto.ChangeUserStatusRequest;
import com.workflow360.identity.api.dto.CreateUserRequest;
import com.workflow360.identity.api.dto.UpdateUserRequest;
import com.workflow360.identity.api.dto.UserResponse;
import com.workflow360.identity.domain.UserStatus;
import com.workflow360.identity.infrastructure.persistence.entity.UserEntity;
import com.workflow360.identity.infrastructure.persistence.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
            "createdAt", "createdAt",
            "updatedAt", "updatedAt",
            "email", "email",
            "displayName", "displayName",
            "status", "status"
    );

    private static final Set<String> ALLOWED_DIRECTIONS = Set.of(
            "asc",
            "desc"
    );

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        String normalizedDisplayName = request.displayName().trim();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResourceAlreadyExistsException(
                    "A user with email " + normalizedEmail + " already exists"
            );
        }

        UserEntity user = new UserEntity(
                UUID.randomUUID(),
                normalizedEmail,
                normalizedDisplayName,
                UserStatus.PENDING
        );

        return userMapper.toResponse(userRepository.save(user));
    }

    public UserResponse getUser(UUID id) {
        return userMapper.toResponse(findUser(id));
    }

    public PageResponse<UserResponse> searchUsers(
            String query,
            UserStatus status,
            int page,
            int size,
            String sortBy,
            String direction) {

        validatePageRequest(page, size, sortBy, direction);

        String normalizedQuery = normalizeQuery(query);
        String entitySortField = ALLOWED_SORT_FIELDS.get(sortBy);

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, entitySortField)
        );

        Page<UserEntity> users = userRepository.search(
                normalizedQuery,
                status,
                pageable
        );

        return userMapper.toPageResponse(users);
    }

    @Transactional
    public UserResponse updateUser(
            UUID id,
            UpdateUserRequest request) {

        UserEntity user = findUser(id);
        verifyVersion(user, request.version());

        user.updateDisplayName(request.displayName().trim());

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse changeStatus(
            UUID id,
            ChangeUserStatusRequest request) {

        UserEntity user = findUser(id);
        verifyVersion(user, request.version());

        try {
            user.changeStatus(request.status());
        } catch (IllegalStateException exception) {
            throw new InvalidRequestParameterException(
                    exception.getMessage()
            );
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    private UserEntity findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID " + id
                ));
    }

    private void verifyVersion(
            UserEntity user,
            long requestVersion) {

        if (user.getVersion() != requestVersion) {
            throw new OptimisticConflictException(
                    "The user has changed since it was loaded. "
                    + "Reload the user and try again."
            );
        }
    }

    private void validatePageRequest(
            int page,
            int size,
            String sortBy,
            String direction) {

        if (page < 0) {
            throw new InvalidRequestParameterException(
                    "Page number must be zero or greater"
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestParameterException(
                    "Page size must be between 1 and " + MAX_PAGE_SIZE
            );
        }

        if (!ALLOWED_SORT_FIELDS.containsKey(sortBy)) {
            throw new InvalidRequestParameterException(
                    "Unsupported sort field: " + sortBy
            );
        }

        if (!ALLOWED_DIRECTIONS.contains(
                direction.toLowerCase(Locale.ROOT))) {
            throw new InvalidRequestParameterException(
                    "Sort direction must be asc or desc"
            );
        }
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        return query.trim();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
```

---

# 14. Why Use a Sort-Field Allowlist?

Do not pass arbitrary client input directly as a JPA sort property.

Bad design:

```java
Sort.by(direction, sortBy)
```

with unrestricted `sortBy`.

Problems:

- Invalid property names fail at runtime.
- Internal entity fields become part of the API accidentally.
- Refactoring entity fields can break clients.
- Clients may request expensive or unintended sorting.

The allowlist provides an API-to-entity mapping:

```java
private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
        "createdAt", "createdAt",
        "updatedAt", "updatedAt",
        "email", "email",
        "displayName", "displayName",
        "status", "status"
);
```

Later, the public name and entity property could differ:

```java
"name" → "displayName"
```

---

# 15. Why Limit Page Size?

A client could request:

```http
GET /api/v1/users?size=1000000
```

That would defeat pagination.

The service enforces:

```text
minimum size = 1
maximum size = 100
```

The API remains predictable even if the database grows.

---

# 16. Update `UserController`

Use request parameters for search and pagination, while keeping defaults simple.

```java
package com.workflow360.identity.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workflow360.common.api.PageResponse;
import com.workflow360.identity.api.dto.ChangeUserStatusRequest;
import com.workflow360.identity.api.dto.CreateUserRequest;
import com.workflow360.identity.api.dto.UpdateUserRequest;
import com.workflow360.identity.api.dto.UserResponse;
import com.workflow360.identity.application.UserService;
import com.workflow360.identity.domain.UserStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        UserResponse response = userService.createUser(request);
        URI location = URI.create("/api/v1/users/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable UUID id) {

        return ResponseEntity.ok(userService.getUser(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> searchUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(userService.searchUsers(
                query,
                status,
                page,
                size,
                sortBy,
                direction
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {

        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeUserStatusRequest request) {

        return ResponseEntity.ok(userService.changeStatus(id, request));
    }
}
```

## Why use `@RequestParam`?

These values modify how the collection is viewed. They do not identify a specific user resource.

```text
Path variable   Identifies resource: /users/{id}
Query parameter Controls collection view: ?page=0&size=20
```

---

# 17. End-to-End Pagination Flow

Request:

```http
GET /api/v1/users?query=abhi&status=PENDING&page=0&size=10&sortBy=email&direction=asc
```

Flow:

```text
1. Controller reads query parameters
2. Spring converts PENDING to UserStatus.PENDING
3. Service validates page, size, sort field, and direction
4. Service creates PageRequest
5. Repository executes filtered JPQL
6. Hibernate translates JPQL to PostgreSQL SQL
7. PostgreSQL returns at most 10 rows
8. Spring Data creates Page<UserEntity>
9. Mapper converts entities to UserResponse
10. Mapper preserves page metadata
11. Controller returns PageResponse<UserResponse>
```

---

# 18. End-to-End Update Flow

Request:

```http
PUT /api/v1/users/{id}
```

```json
{
  "displayName": "Abhishek P R",
  "version": 0
}
```

Flow:

```text
1. @Valid validates displayName and version
2. Service loads the user by UUID
3. Service compares request version with entity version
4. Entity changes the display name
5. Transaction commits
6. @PreUpdate refreshes updatedAt
7. Hibernate includes version in the UPDATE condition
8. PostgreSQL updates the row
9. Hibernate increments version
10. Response contains the new version
```

Conceptual SQL generated by Hibernate:

```sql
UPDATE users
SET display_name = ?,
    updated_at = ?,
    version = version + 1
WHERE id = ?
  AND version = ?;
```

If no row matches the old version, optimistic locking detects a conflict.

---

# 19. API Tests

## Paginated list

```http
GET /api/v1/users?page=0&size=10
```

Expected:

```text
200 OK
content size <= 10
page = 0
size = 10
```

## Search by email or display name

```http
GET /api/v1/users?query=abhishek&page=0&size=20
```

## Filter by status

```http
GET /api/v1/users?status=PENDING&page=0&size=20
```

## Sort ascending by email

```http
GET /api/v1/users?sortBy=email&direction=asc
```

## Invalid sort field

```http
GET /api/v1/users?sortBy=password
```

Expected:

```text
400 Bad Request
code = INVALID_REQUEST_PARAMETER
```

## Excessive page size

```http
GET /api/v1/users?size=1000
```

Expected:

```text
400 Bad Request
```

## Update display name

First retrieve the user to obtain the latest version:

```http
GET /api/v1/users/{id}
```

Then:

```http
PUT /api/v1/users/{id}
```

```json
{
  "displayName": "Abhishek P R",
  "version": 0
}
```

Expected:

```text
200 OK
version = 1
```

## Repeat with old version

Send version `0` again.

Expected:

```text
409 Conflict
code = STALE_RESOURCE_VERSION
```

## Activate pending user

Use the latest version:

```http
PATCH /api/v1/users/{id}/status
```

```json
{
  "status": "ACTIVE",
  "version": 1
}
```

Expected:

```text
200 OK
status = ACTIVE
version = 2
```

---

# 20. Database Verification

```sql
SELECT id,
       email,
       display_name,
       status,
       created_at,
       updated_at,
       version
FROM users
ORDER BY created_at DESC;
```

After updates, verify:

```text
display_name changed
status changed as requested
updated_at increased
version increased
created_at did not change
```

---

# 21. Unit Test Scenarios

Add service tests for:

```text
searchUsers creates the expected PageRequest
searchUsers rejects page below zero
searchUsers rejects size above 100
searchUsers rejects unsupported sort field
updateUser changes display name
updateUser rejects stale version
changeStatus updates status
changeStatus rejects forbidden transition
```

Example stale-version test:

```java
@Test
void shouldRejectUpdateWithStaleVersion() {
    UUID userId = UUID.randomUUID();

    UserEntity user = new UserEntity(
            userId,
            "abhishek@example.com",
            "Abhishek",
            UserStatus.PENDING
    );

    when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));

    UpdateUserRequest request = new UpdateUserRequest(
            "Abhishek P R",
            5
    );

    assertThrows(
            OptimisticConflictException.class,
            () -> userService.updateUser(userId, request)
    );
}
```

The entity starts at version `0`, while the request claims version `5`, so the service rejects the stale or incorrect request.

---

# 22. Common Mistakes

## Returning every user

An unpaged endpoint eventually creates performance and memory problems.

## Trusting arbitrary sort fields

Use an allowlist instead of passing raw client input directly to Spring Data.

## Using page number 1 as the first page

Spring Data page numbers are zero-based:

```text
page=0 → first page
page=1 → second page
```

## Using PATCH and PUT without a clear contract

In this project:

```text
PUT /users/{id}          Updates the editable profile representation
PATCH /users/{id}/status Changes only lifecycle status
```

## Omitting version from updates

Without version checks, concurrent edits can silently overwrite each other.

## Updating entity fields through reflection or generic setters

Use controlled methods such as `updateDisplayName` and `changeStatus`.

## Filtering in Java after loading all rows

Filtering belongs in PostgreSQL through the repository query.

---

# 23. Definition of Done

- [ ] `PageResponse<T>` exists
- [ ] Repository returns `Page<UserEntity>`
- [ ] Search supports email and display name
- [ ] Search optionally filters by status
- [ ] Page numbers are zero-based
- [ ] Page size is limited to 100
- [ ] Sort fields use an allowlist
- [ ] Sort direction accepts only asc or desc
- [ ] `UpdateUserRequest` exists
- [ ] `ChangeUserStatusRequest` exists
- [ ] Entity has controlled update methods
- [ ] User service supports paginated search
- [ ] User service supports display-name update
- [ ] User service supports status change
- [ ] Explicit version mismatch returns 409
- [ ] Hibernate optimistic-lock conflict returns 409
- [ ] Controller returns paginated response
- [ ] PUT user endpoint works
- [ ] PATCH status endpoint works
- [ ] Invalid parameters return consistent errors
- [ ] Unit tests cover pagination and update rules
- [ ] Database version increments after updates
- [ ] Actuator health remains UP

---

# 24. Interview Explanation

> I improved the User Management API with database-level pagination, sorting, case-insensitive text search, and optional status filtering. I used Spring Data `Pageable` and `Page`, but mapped the framework result into an application-owned page response so the public API is not coupled to Spring internals. I protected sorting with an approved-field allowlist and capped page size. For updates, I separated profile updates from status transitions, used entity methods to model state changes, and used JPA `@Version` plus request version checks to prevent lost updates. Stale updates return HTTP 409 through centralized exception handling.

---

# 25. Next Task

After this task is complete:

```text
Explicit UserRoleEntity
        ↓
Composite key mapping
        ↓
Assign role to user
        ↓
Remove role from user
        ↓
List user roles and effective permissions
        ↓
Prevent removal of required access
        ↓
RBAC service and controller tests
        ↓
Spring Security preparation
```
