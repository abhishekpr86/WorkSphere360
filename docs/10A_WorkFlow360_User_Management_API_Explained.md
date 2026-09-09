# WorkFlow360 — User Management API Foundation, Explained

## Purpose of This Guide

This guide explains not only **what code to write**, but also:

- Why each file exists
- Which architectural layer owns each responsibility
- How data moves from an HTTP request to PostgreSQL
- Why DTOs are different from JPA entities
- Why validation exists in multiple layers
- Why services use transactions
- Why exceptions are handled centrally
- How to test each behavior
- How this foundation prepares WorkFlow360 for authentication, employees, notifications, and chat

The objective is to understand the architecture rather than copy code mechanically.

---

# 1. What We Are Building

We are building the first database-backed WorkFlow360 feature:

```text
API client sends user information
        ↓
Spring validates the JSON request
        ↓
Controller receives a valid request DTO
        ↓
Service applies business rules
        ↓
Repository stores a JPA entity
        ↓
Mapper converts the entity to a response DTO
        ↓
Controller returns an HTTP response
```

The initial endpoints are:

```http
POST /api/v1/users
GET  /api/v1/users/{id}
GET  /api/v1/users
```

This milestone does **not** create passwords or login. A user account and a login credential are related, but they are not the same concept.

```text
User identity
├── ID
├── Email
├── Display name
├── Status
└── Verification state

Local credential — later
├── Password hash
├── Failed login count
├── Password changed time
└── Lock information

External identity — later
├── Provider
├── Provider user ID
└── Link to WorkFlow360 user
```

Keeping these concepts separate allows WorkFlow360 to support local login, Google OpenID Connect, and Microsoft Entra ID later.

---

# 2. Architecture and Responsibility Flow

## Layer flow

```text
HTTP request
    ↓
api layer
    ↓
application layer
    ↓
infrastructure/persistence layer
    ↓
PostgreSQL
```

## What each package means

```text
com.workflow360.identity.api
```

Owns HTTP-specific concerns such as controllers, request bodies, response bodies, HTTP status codes, and URI paths.

```text
com.workflow360.identity.application
```

Owns use cases and orchestration. For example, creating a user requires normalizing the email, checking duplicates, creating the entity, saving it, and mapping the result.

```text
com.workflow360.identity.domain
```

Owns business language and rules that should not depend on HTTP or PostgreSQL. `UserStatus` belongs here because `PENDING`, `ACTIVE`, `LOCKED`, and `DISABLED` are business concepts.

```text
com.workflow360.identity.infrastructure.persistence
```

Owns database implementation details such as JPA entities and Spring Data repositories.

```text
com.workflow360.common.exception
```

Owns error types and the common API error contract used by multiple modules.

---

# 3. Final File Structure

Create this structure:

```text
com.workflow360/
├── common/
│   └── exception/
│       ├── ApiErrorResponse.java
│       ├── FieldValidationError.java
│       ├── GlobalExceptionHandler.java
│       ├── ResourceAlreadyExistsException.java
│       └── ResourceNotFoundException.java
│
└── identity/
    ├── api/
    │   ├── UserController.java
    │   └── dto/
    │       ├── CreateUserRequest.java
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

---

# 4. `UserEntity` — Database Mapping

## Why this file exists

`UserEntity` tells Hibernate how a Java object maps to the PostgreSQL `users` table.

```text
Java field       PostgreSQL column
id               id
email            email
displayName      display_name
status           status
emailVerified    email_verified
createdAt        created_at
updatedAt        updated_at
version          version
```

The entity is a persistence model. It must not become the public REST response because:

- Database changes should not automatically change the API.
- Future sensitive fields must not be accidentally serialized.
- Lazy-loaded relationships can cause unexpected queries or serialization problems.
- API clients may need a different data shape than the database.

## Entity code

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

## Annotation explanations

### `@Entity`

Marks the class as a persistent JPA entity. Hibernate scans the class and manages instances through the persistence context.

### `@Table(name = "users")`

Maps the entity to the exact PostgreSQL table name. Explicit naming removes ambiguity.

### `@Id`

Marks the primary key. UUID gives every user a stable identifier that can later be referenced by employees, tasks, notifications, conversations, messages, and audit records.

### `@Column`

Documents and enforces mapping details such as nullability, length, uniqueness, and exact column names.

Database constraints are still required. JPA metadata does not replace Flyway constraints.

### `@Enumerated(EnumType.STRING)`

Stores values such as `PENDING` instead of numeric positions such as `0`. String storage is safer because changing enum order does not corrupt meaning.

### `@Version`

Enables optimistic locking. When two requests update the same user concurrently, Hibernate can detect that one request used an outdated database version.

### `@PrePersist`

Runs before the first insert and initializes `createdAt` and `updatedAt`.

### `@PreUpdate`

Runs before an update and refreshes `updatedAt`.

### Protected no-argument constructor

JPA requires a no-argument constructor. It is protected because normal application code should construct a user using the meaningful public constructor.

### Why there are no public setters

Unrestricted setters allow any code to place the entity in an invalid state. Later, controlled methods can express business operations such as:

```java
public void activate() {
    this.status = UserStatus.ACTIVE;
}
```

This is clearer than generic code such as `setStatus(...)`.

---

# 5. `UserRepository` — Database Access Abstraction

## Why this file exists

The repository hides low-level persistence operations from the application service.

Instead of writing SQL for every basic operation, Spring Data provides methods such as:

```text
save
findById
findAll
deleteById
```

Custom method names can describe queries.

```java
package com.workflow360.identity.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow360.identity.infrastructure.persistence.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<UserEntity> findAllByOrderByCreatedAtDesc();
}
```

## Code explanations

### `JpaRepository<UserEntity, UUID>`

The first type is the managed entity. The second type is the primary-key type.

### `Optional<UserEntity>`

A lookup may return no user. `Optional` makes absence explicit and prevents a misleading assumption that a value always exists.

### `findByEmailIgnoreCase`

Finds a user without treating uppercase and lowercase email input as different accounts.

### `existsByEmailIgnoreCase`

Efficiently answers a business question: does this email already exist?

### `findAllByOrderByCreatedAtDesc`

Returns newest users first. This unpaged query is acceptable only for the first small milestone; pagination will replace it.

---

# 6. `CreateUserRequest` — Incoming API Contract

## Why this file exists

A request DTO defines exactly what an API client is allowed to send.

The client is allowed to send only:

```text
email
displayName
```

The client is not allowed to choose:

```text
id
status
emailVerified
createdAt
version
```

Those values are owned by the backend.

```java
package com.workflow360.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 320, message = "Email must not exceed 320 characters")
        String email,

        @NotBlank(message = "Display name is required")
        @Size(
            min = 2,
            max = 150,
            message = "Display name must contain between 2 and 150 characters"
        )
        String displayName
) {
}
```

## Why use a record?

A request DTO is a small immutable data carrier. Java records automatically provide a constructor, accessors, `equals`, `hashCode`, and `toString`.

## Validation explanations

### `@NotBlank`

Rejects null, empty, and whitespace-only strings.

### `@Email`

Checks that the value has a valid email-like format.

### `@Size`

Keeps request limits aligned with database column lengths and provides an understandable error before PostgreSQL rejects the value.

## Validation is not the same as business rules

```text
Email has valid format             DTO validation
Email is not already registered    Business validation
Email is unique under concurrency  Database constraint
```

All three protections have different responsibilities.

---

# 7. `UserResponse` — Outgoing API Contract

## Why this file exists

The response DTO defines what WorkFlow360 sends to API clients.

```java
package com.workflow360.identity.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.workflow360.identity.domain.UserStatus;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        UserStatus status,
        boolean emailVerified,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}
```

The response contains useful public user information, while isolating clients from the internal JPA entity.

The `version` value will later help with concurrency-aware update APIs.

---

# 8. `UserMapper` — Entity-to-DTO Conversion

## Why this file exists

The entity and API response intentionally have separate responsibilities. A mapper performs the conversion in one reusable place.

```java
package com.workflow360.identity.application;

import org.springframework.stereotype.Component;

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
}
```

## Why use `@Component`?

Spring creates and manages one `UserMapper` object, allowing constructor injection into `UserService`.

## What the mapper must not do

The mapper must not query PostgreSQL, validate duplicate emails, send notifications, or decide status transitions.

---

# 9. Custom Exceptions — Business Failure Names

## Why custom exceptions exist

A missing user and a duplicate email are different failures. Custom exception types let the global handler map each failure to the correct HTTP status and error code.

## `ResourceNotFoundException`

```java
package com.workflow360.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

Used when the requested UUID does not identify an existing user. The API converts this to HTTP `404 Not Found`.

## `ResourceAlreadyExistsException`

```java
package com.workflow360.common.exception;

public class ResourceAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
```

Used when creating a user would violate the unique-email rule. The API converts this to HTTP `409 Conflict`.

## Why extend `RuntimeException`?

These exceptions can move through the service and controller layers to centralized exception handling without adding repetitive `throws` declarations.

---

# 10. `FieldValidationError` — One Invalid Field

```java
package com.workflow360.common.exception;

public record FieldValidationError(
        String field,
        String message
) {
}
```

This record represents one specific validation issue:

```json
{
  "field": "email",
  "message": "Email must be valid"
}
```

A single request may contain several invalid fields, so the main API error response contains a list of these records.

---

# 11. `ApiErrorResponse` — Consistent Failure Contract

```java
package com.workflow360.common.exception;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldValidationError> errors
) {
}
```

## Why every field exists

```text
timestamp  When the failure happened
status     HTTP status number
code       Stable machine-readable application code
message    Human-readable summary
path       Endpoint that failed
errors     Detailed field validation failures
```

The frontend should eventually use stable `code` values for behavior and use `message` for display.

---

# 12. `GlobalExceptionHandler` — Central Error Translation

## Why this file exists

Without centralized handling, every controller would repeat `try/catch` blocks and might return inconsistent error formats.

```java
package com.workflow360.common.exception;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {

        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "RESOURCE_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            ResourceAlreadyExistsException exception,
            HttpServletRequest request) {

        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "RESOURCE_ALREADY_EXISTS",
                exception.getMessage(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        List<FieldValidationError> fieldErrors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldValidationError)
                .toList();

        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.badRequest().body(response);
    }

    private FieldValidationError toFieldValidationError(FieldError error) {
        return new FieldValidationError(
                error.getField(),
                error.getDefaultMessage()
        );
    }
}
```

## Annotation explanations

### `@RestControllerAdvice`

Applies exception handling across REST controllers and serializes response objects as JSON.

### `@ExceptionHandler`

Associates one exception type with one handler method.

### `MethodArgumentNotValidException`

Spring throws this when a controller parameter marked with `@Valid` fails Bean Validation.

### Why there is no generic handler yet

A generic handler can be added later with controlled logging and a safe message. During learning, unexpected errors should remain visible in development logs rather than being hidden too early.

---

# 13. `UserService` — The Create and Query Use Cases

## Why this file exists

The service contains application behavior. Controllers know HTTP; repositories know persistence; services coordinate the use case.

```java
package com.workflow360.identity.application;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow360.common.exception.ResourceAlreadyExistsException;
import com.workflow360.common.exception.ResourceNotFoundException;
import com.workflow360.identity.api.dto.CreateUserRequest;
import com.workflow360.identity.api.dto.UserResponse;
import com.workflow360.identity.domain.UserStatus;
import com.workflow360.identity.infrastructure.persistence.entity.UserEntity;
import com.workflow360.identity.infrastructure.persistence.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class UserService {

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

        UserEntity savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    public UserResponse getUser(UUID id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID " + id
                ));

        return userMapper.toResponse(user);
    }

    public List<UserResponse> getUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
```

## Annotation explanations

### `@Service`

Marks the class as an application service managed by Spring.

### Class-level `@Transactional(readOnly = true)`

Query methods use read-only transactions by default. This communicates intent and prevents every query method from repeating the annotation.

### Method-level `@Transactional`

`createUser` writes to PostgreSQL, so the method overrides the read-only default with a normal transaction.

## Why constructor injection?

Dependencies are explicit, final, and easy to replace with mocks in unit tests. Constructor injection avoids hidden mutable dependencies.

## Why normalize email?

These inputs should represent the same account:

```text
Abhishek@Example.com
abhishek@example.com
  abhishek@example.com
```

`Locale.ROOT` avoids locale-dependent lowercase behavior.

## Why check duplicates in service and database?

The service check creates a friendly `409` response. The unique database constraint protects correctness if two concurrent requests pass the check at nearly the same time.

Later, a database constraint exception should also be translated to the same conflict response.

## Why `UUID.randomUUID()`?

The backend—not the client—owns identity generation. The UUID becomes the stable user reference for future modules, including chat.

## Why start with `PENDING`?

The user identity exists, but onboarding, verification, credential setup, and role assignment are not complete.

---

# 14. `UserController` — HTTP Contract

## Why this file exists

The controller maps HTTP requests to application-service calls and converts results into HTTP responses. It should stay thin.

```java
package com.workflow360.identity.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow360.identity.api.dto.CreateUserRequest;
import com.workflow360.identity.api.dto.UserResponse;
import com.workflow360.identity.application.UserService;

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
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers() {
        return ResponseEntity.ok(userService.getUsers());
    }
}
```

## Annotation explanations

### `@RestController`

Marks the class as a REST controller whose return values are serialized to JSON.

### `@RequestMapping("/api/v1/users")`

Defines the versioned base resource path.

### `@RequestBody`

Converts incoming JSON into `CreateUserRequest`.

### `@Valid`

Runs the validation annotations on the request record before the service is called.

### `@PathVariable UUID id`

Converts the URL segment into a UUID. Invalid UUID syntax is rejected before the service lookup.

### Why return `201 Created`?

A new server-side resource was created.

### Why add a `Location` header?

The response tells clients where the newly created resource can be retrieved:

```text
Location: /api/v1/users/{id}
```

### Why no business logic in the controller?

A controller should not normalize email, query the repository, choose user status, or construct entities. Those operations belong in the service.

---

# 15. Complete Request Flow

For this request:

```http
POST /api/v1/users
Content-Type: application/json
```

```json
{
  "email": "  Abhishek@Example.com  ",
  "displayName": "  Abhishek  "
}
```

The flow is:

```text
1. Jackson converts JSON to CreateUserRequest
2. Bean Validation checks email and displayName
3. UserController calls UserService
4. UserService trims and lowercases the email
5. UserService trims the display name
6. UserRepository checks for duplicate email
7. UserService generates a UUID
8. UserService creates UserEntity with PENDING status
9. UserRepository saves through Hibernate
10. @PrePersist creates timestamps
11. PostgreSQL inserts the row and enforces constraints
12. UserMapper creates UserResponse
13. UserController returns 201 and Location header
```

---

# 16. API Testing

## Create user

```http
POST http://localhost:8080/api/v1/users
Content-Type: application/json
```

```json
{
  "email": "abhishek@example.com",
  "displayName": "Abhishek"
}
```

Expected:

```text
201 Created
```

## Duplicate email

Repeat the request with uppercase letters:

```json
{
  "email": "ABHISHEK@EXAMPLE.COM",
  "displayName": "Duplicate"
}
```

Expected:

```text
409 Conflict
RESOURCE_ALREADY_EXISTS
```

## Invalid request

```json
{
  "email": "invalid-email",
  "displayName": "A"
}
```

Expected:

```text
400 Bad Request
VALIDATION_ERROR
```

## Get existing user

```http
GET http://localhost:8080/api/v1/users/{created-user-id}
```

Expected:

```text
200 OK
```

## Get missing user

```http
GET http://localhost:8080/api/v1/users/00000000-0000-0000-0000-000000000999
```

Expected:

```text
404 Not Found
RESOURCE_NOT_FOUND
```

## List users

```http
GET http://localhost:8080/api/v1/users
```

Expected:

```text
200 OK
```

---

# 17. Database Verification

Run:

```sql
SELECT id,
       email,
       display_name,
       status,
       email_verified,
       created_at,
       updated_at,
       version
FROM users
ORDER BY created_at DESC;
```

Verify:

```text
The email is lowercase
The display name is trimmed
The status is PENDING
email_verified is false
A UUID exists
Both timestamps exist
The initial version is 0
```

---

# 18. Unit Testing the Service

## Why test `UserService`?

The service contains the important business behavior:

- Email normalization
- Duplicate detection
- Initial status selection
- Repository interaction

A unit test replaces the repository with a mock, allowing the behavior to be tested quickly without PostgreSQL.

Create:

```text
src/test/java/com/workflow360/identity/application/UserServiceTest.java
```

```java
package com.workflow360.identity.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workflow360.common.exception.ResourceAlreadyExistsException;
import com.workflow360.identity.api.dto.CreateUserRequest;
import com.workflow360.identity.api.dto.UserResponse;
import com.workflow360.identity.domain.UserStatus;
import com.workflow360.identity.infrastructure.persistence.entity.UserEntity;
import com.workflow360.identity.infrastructure.persistence.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, new UserMapper());
    }

    @Test
    void shouldCreatePendingUserWithNormalizedEmail() {
        CreateUserRequest request = new CreateUserRequest(
                "  Abhishek@Example.com  ",
                "  Abhishek  "
        );

        when(userRepository.existsByEmailIgnoreCase("abhishek@example.com"))
                .thenReturn(false);

        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.createUser(request);

        assertEquals("abhishek@example.com", response.email());
        assertEquals("Abhishek", response.displayName());
        assertEquals(UserStatus.PENDING, response.status());

        verify(userRepository)
                .existsByEmailIgnoreCase("abhishek@example.com");
    }

    @Test
    void shouldRejectDuplicateEmail() {
        CreateUserRequest request = new CreateUserRequest(
                "abhishek@example.com",
                "Abhishek"
        );

        when(userRepository.existsByEmailIgnoreCase("abhishek@example.com"))
                .thenReturn(true);

        assertThrows(
                ResourceAlreadyExistsException.class,
                () -> userService.createUser(request)
        );
    }
}
```

## Test annotation explanations

### `@ExtendWith(MockitoExtension.class)`

Activates Mockito support in JUnit 5.

### `@Mock`

Creates a test double for `UserRepository` so the test does not use PostgreSQL.

### `@BeforeEach`

Creates a fresh service before each test.

### `when(...).thenReturn(...)`

Defines simulated repository behavior.

### `thenAnswer(...)`

Returns the same entity supplied to `save`, simulating a basic successful save.

### `assertEquals`

Verifies the expected business result.

### `assertThrows`

Verifies that duplicate email behavior produces the correct exception.

### `verify`

Confirms that the service called the expected repository method.

## Lifecycle callback note

The mocked repository does not perform real JPA persistence, so `@PrePersist` does not run. Therefore, timestamps are not asserted in this unit test. A repository or integration test will verify persistence behavior later.

---

# 19. What We Are Deliberately Postponing

## Passwords and authentication

Passwords require secure hashing, credential state, failure tracking, and security configuration. Adding a plain password field now would mix identity management with authentication design.

## Role assignment

`user_roles` requires an explicit assignment model and authorization rules. User creation should work first.

## Pagination

The initial list is kept simple. Before real usage, `GET /users` must support page size, sorting, and search.

## React user screens

The backend contract should be verified first. The frontend should consume stable behavior rather than evolve at the same time as an untested API.

## Chat implementation

Chat depends on authenticated user identity and conversation-level authorization. The stable UUID created now becomes the future chat participant ID.

---

# 20. Common Mistakes

## Returning `UserEntity` from the controller

This couples the API to persistence and can expose fields unintentionally.

## Calling the repository directly from the controller

This bypasses normalization, duplicate checks, transactions, and business rules.

## Accepting status from `CreateUserRequest`

A client could create an already-active or locked account. The backend owns initial status.

## Accepting an ID from the client

The server owns identity generation.

## Storing emails without normalization

This can create visually duplicate accounts.

## Relying only on the service duplicate check

Concurrent requests can race. The unique database constraint remains essential.

## Using field injection

Constructor injection is explicit, testable, and supports immutable dependencies.

## Adding `try/catch` in every controller

Central exception handling produces one reliable error contract.

---

# 21. Definition of Done

- [ ] Package structure is created
- [ ] `UserEntity` maps correctly to `users`
- [ ] `UserRepository` exists
- [ ] `CreateUserRequest` exists and validates input
- [ ] `UserResponse` exists
- [ ] `UserMapper` exists
- [ ] Custom exceptions exist
- [ ] Field validation error record exists
- [ ] Standard API error response exists
- [ ] Global exception handler exists
- [ ] `UserService` exists
- [ ] `UserController` exists
- [ ] Create user returns `201 Created`
- [ ] Location header is returned
- [ ] Email is normalized
- [ ] Initial status is `PENDING`
- [ ] Initial verification state is false
- [ ] Duplicate email returns `409 Conflict`
- [ ] Invalid request returns `400 Bad Request`
- [ ] Missing user returns `404 Not Found`
- [ ] List users returns `200 OK`
- [ ] PostgreSQL contains the created user
- [ ] Unit tests pass
- [ ] Actuator health remains `UP`

---

# 22. Interview Explanation

After finishing this task, explain the design like this:

> I implemented a database-backed user-management API using Spring Boot and PostgreSQL. I separated request and response DTOs from JPA entities, applied Bean Validation at the API boundary, normalized emails and enforced duplicate-account rules in a transactional application service, retained a database unique constraint for concurrency safety, mapped entities through a dedicated mapper, and used centralized exception handling to produce consistent 400, 404, and 409 error responses. User identity uses UUIDs so it can later support employee records, audit events, notifications, and private chat participants.

---

# 23. Next Task

After this foundation works:

```text
Pagination and sorting
        ↓
User search
        ↓
Update user profile
        ↓
Change user status
        ↓
Explicit user-role assignment
        ↓
Controller integration tests
        ↓
OpenAPI documentation
        ↓
Local authentication and Spring Security
```
