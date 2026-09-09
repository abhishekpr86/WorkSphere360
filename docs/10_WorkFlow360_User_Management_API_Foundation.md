# WorkFlow360 — User Management API Foundation

## Goal

Build the first database-backed REST feature for WorkFlow360.

This task implements:

- Create a user
- Get a user by UUID
- List users
- Request and response DTOs
- Bean Validation
- Entity-to-DTO mapping
- Application service
- Centralized exception handling
- Consistent API error responses
- Basic service and controller tests

This task does **not** implement passwords, login, JWT, Spring Security, role assignment, employee records, or React user-management screens.

---

## 1. Prerequisite Check

Before starting, confirm that Flyway migrations V1, V2, and V3 succeeded:

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
1 | initialize database                 | workflow360_app | true
2 | create identity rbac tables         | workflow360_app | true
3 | seed default roles permissions      | workflow360_app | true
```

Verify the default data:

```sql
SELECT COUNT(*) FROM roles;
SELECT COUNT(*) FROM permissions;
```

Expected:

```text
roles: 3
permissions: 21
```

Also confirm:

```text
http://localhost:8080/actuator/health
```

returns:

```json
{
  "status": "UP"
}
```

---

## 2. API Design

This milestone introduces these endpoints:

```http
POST /api/v1/users
GET  /api/v1/users/{id}
GET  /api/v1/users
```

### Create-user request

```json
{
  "email": "abhishek@example.com",
  "displayName": "Abhishek"
}
```

### Create-user response

```json
{
  "id": "generated-uuid",
  "email": "abhishek@example.com",
  "displayName": "Abhishek",
  "status": "PENDING",
  "emailVerified": false,
  "createdAt": "2026-08-21T00:00:00Z",
  "updatedAt": "2026-08-21T00:00:00Z",
  "version": 0
}
```

New users begin with:

```text
status = PENDING
emailVerified = false
```

No password or role is assigned in this task.

---

## 3. Package Structure

Create the following packages and files:

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

## 4. Update `UserEntity`

Open:

```text
com.workflow360.identity.infrastructure.persistence.entity.UserEntity
```

Ensure the entity has the following constructor and methods. Keep the existing JPA annotations and lifecycle methods.

```java
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
```

Do not create public setters for every property. Changes to important user state should eventually happen through controlled methods.

---

## 5. Verify `UserRepository`

Use:

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

---

## 6. Create `CreateUserRequest`

Create:

```text
com.workflow360.identity.api.dto.CreateUserRequest
```

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

The request DTO validates transport-level input. Business validation, such as duplicate email detection, belongs in the application service.

---

## 7. Create `UserResponse`

Create:

```text
com.workflow360.identity.api.dto.UserResponse
```

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

The response DTO prevents the JPA entity from becoming the public API contract.

---

## 8. Create `UserMapper`

Create:

```text
com.workflow360.identity.application.UserMapper
```

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

Do not put persistence calls or business logic inside the mapper.

---

## 9. Create Common Exceptions

### `ResourceNotFoundException.java`

```java
package com.workflow360.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

### `ResourceAlreadyExistsException.java`

```java
package com.workflow360.common.exception;

public class ResourceAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
```

---

## 10. Create the Validation Error Record

Create:

```text
com.workflow360.common.exception.FieldValidationError
```

```java
package com.workflow360.common.exception;

public record FieldValidationError(
        String field,
        String message
) {
}
```

---

## 11. Create the Standard API Error Response

Create:

```text
com.workflow360.common.exception.ApiErrorResponse
```

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

The error contract will look like:

```json
{
  "timestamp": "2026-08-21T00:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/v1/users",
  "errors": [
    {
      "field": "email",
      "message": "Email must be valid"
    }
  ]
}
```

---

## 12. Create `GlobalExceptionHandler`

Create:

```text
com.workflow360.common.exception.GlobalExceptionHandler
```

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

Do not return stack traces, SQL errors, or internal implementation details to API clients.

---

## 13. Create `UserService`

Create:

```text
com.workflow360.identity.application.UserService
```

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

Important service responsibilities:

- Normalize email addresses
- Enforce duplicate-email business rules
- Generate a UUID
- Set the initial status
- Use a transaction for database changes
- Return DTOs rather than entities

---

## 14. Create `UserController`

Create:

```text
com.workflow360.identity.api.UserController
```

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

Do not add `@CrossOrigin` to every controller. A centralized CORS configuration will be introduced before connecting the React user-management UI.

---

## 15. Run the Application

In Eclipse:

```text
Project → Clean
```

Then start:

```text
Workflow360BackendApplication.java
→ Run As
→ Spring Boot App
```

Verify:

```text
http://localhost:8080/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

---

## 16. Test the Create-User API

Use Bruno, Postman, or another API client.

### Request

```http
POST http://localhost:8080/api/v1/users
Content-Type: application/json
```

Body:

```json
{
  "email": "abhishek@example.com",
  "displayName": "Abhishek"
}
```

### Expected response

Status:

```text
201 Created
```

Headers include:

```text
Location: /api/v1/users/{generated-uuid}
```

Body:

```json
{
  "id": "generated-uuid",
  "email": "abhishek@example.com",
  "displayName": "Abhishek",
  "status": "PENDING",
  "emailVerified": false,
  "createdAt": "...",
  "updatedAt": "...",
  "version": 0
}
```

---

## 17. Test Duplicate Email Handling

Submit the same email again, using different capitalization if desired:

```json
{
  "email": "ABHISHEK@EXAMPLE.COM",
  "displayName": "Duplicate User"
}
```

Expected status:

```text
409 Conflict
```

Expected error code:

```text
RESOURCE_ALREADY_EXISTS
```

The unique database constraint remains the final protection against race conditions, even though the service performs a friendly duplicate check.

---

## 18. Test Validation

### Invalid email

```json
{
  "email": "invalid-email",
  "displayName": "A"
}
```

Expected:

```text
400 Bad Request
```

Expected error code:

```text
VALIDATION_ERROR
```

### Missing fields

```json
{}
```

Expected response contains validation entries for both `email` and `displayName`.

---

## 19. Test Get User by ID

Copy the generated user UUID.

```http
GET http://localhost:8080/api/v1/users/{user-id}
```

Expected:

```text
200 OK
```

For a valid UUID that does not exist:

```http
GET http://localhost:8080/api/v1/users/00000000-0000-0000-0000-000000000999
```

Expected:

```text
404 Not Found
```

Expected error code:

```text
RESOURCE_NOT_FOUND
```

---

## 20. Test List Users

```http
GET http://localhost:8080/api/v1/users
```

Expected:

```text
200 OK
```

Initial response shape:

```json
[
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
]
```

Pagination will replace this unpaged list before the number of users becomes large.

---

## 21. Verify the Database

Run in pgAdmin:

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

Confirm:

```text
Email is stored in lowercase
Status is PENDING
Email verified is false
UUID is present
Timestamps are present
Version is 0
```

---

## 22. Add Basic Service Tests

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

import java.time.Instant;
import java.util.UUID;

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

### Important test note

The first test may fail because `@PrePersist` callbacks are not invoked by a mocked repository, leaving timestamps null. The assertions above intentionally focus on business behavior rather than persistence lifecycle callbacks. Remove unused imports such as `Instant` or `UUID` if Eclipse reports warnings.

---

## 23. Run Tests

In Eclipse:

```text
Right-click UserServiceTest.java
→ Run As
→ JUnit Test
```

Then run the full Maven test suite with the required database environment configured:

```powershell
cd C:\Development\workflow360\backend
$env:DB_PASSWORD="your-local-password"
.\mvnw.cmd clean test
```

Expected:

```text
BUILD SUCCESS
```

A dedicated test database strategy will be added later so unit tests do not depend on the developer's local database.

---

## 24. Important Rules

Do not:

- Add a password to the create-user request yet
- Add authentication or JWT yet
- Assign roles manually in the create-user service
- Expose JPA entities from controllers
- Put business logic in the controller
- Use field injection with `@Autowired`
- Return raw exception stack traces
- Disable the unique email constraint
- Add React screens before the backend behavior is verified

---

## 25. Definition of Done

- [ ] `CreateUserRequest` exists
- [ ] `UserResponse` exists
- [ ] Validation annotations exist
- [ ] `UserMapper` exists
- [ ] Common exceptions exist
- [ ] Standard API error response exists
- [ ] Global exception handler exists
- [ ] `UserService` exists
- [ ] `UserController` exists
- [ ] `POST /api/v1/users` returns 201
- [ ] `GET /api/v1/users/{id}` returns 200
- [ ] Missing user returns 404
- [ ] Duplicate email returns 409
- [ ] Invalid input returns 400
- [ ] `GET /api/v1/users` returns the stored users
- [ ] Email is normalized to lowercase
- [ ] New users have `PENDING` status
- [ ] New users have `emailVerified=false`
- [ ] UUID is generated
- [ ] User is saved in PostgreSQL
- [ ] Basic service tests pass
- [ ] Spring Boot starts
- [ ] Actuator health returns `UP`

---

## Next Task

The next task will improve the User Management API to enterprise standards:

```text
Pagination
        ↓
Sorting
        ↓
Search
        ↓
Update user
        ↓
Change user status
        ↓
Role assignment model and API
        ↓
Repository and controller integration tests
        ↓
OpenAPI documentation
```

After that, WorkFlow360 will begin the dedicated local-authentication and Spring Security milestone.
