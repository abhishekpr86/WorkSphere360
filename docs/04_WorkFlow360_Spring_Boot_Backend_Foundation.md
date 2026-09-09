# WorkFlow360 — Spring Boot Backend Foundation

## Objective

Generate and run the first Spring Boot modular-monolith backend without Docker, PostgreSQL, authentication, or business entities.

## Spring Initializr settings

Open `https://start.spring.io` and use:

```text
Project: Maven
Language: Java
Spring Boot: 4.1.0 (stable; do not choose SNAPSHOT)
Group: com.workflow360
Artifact: workflow360-backend
Name: WorkFlow360 Backend
Description: Enterprise employee and project management platform
Package name: com.workflow360
Packaging: Jar
Java: 25
Configuration: YAML
```

Select only:

```text
Spring Web
Validation
Spring Boot Actuator
```

Do not select JPA, PostgreSQL, Flyway, Security, Redis, Kafka, WebSocket, Eureka, Gateway, or Lombok yet.

## Extract the project

1. Click **Generate**.
2. Extract the downloaded ZIP.
3. Copy the contents of the extracted `workflow360-backend` folder into:

```text
C:\Development\workflow360\backend
```

The `pom.xml` must be directly inside `backend`, not in a second nested backend folder.

Correct:

```text
workflow360\backend\pom.xml
```

Incorrect:

```text
workflow360\backend\workflow360-backend\pom.xml
```

## Open in IntelliJ

Open the folder:

```text
C:\Development\workflow360\backend
```

Allow IntelliJ to import Maven dependencies. Configure the Project SDK and Maven Runner JRE to Java 25.

## Verify generated files

```text
backend/
├── .mvn/
├── src/
│   ├── main/
│   │   ├── java/com/workflow360/
│   │   │   └── Workflow360BackendApplication.java
│   │   └── resources/
│   │       └── application.yaml
│   └── test/
│       └── java/com/workflow360/
│           └── Workflow360BackendApplicationTests.java
├── mvnw
├── mvnw.cmd
└── pom.xml
```

## Configure `application.yaml`

Replace its content with:

```yaml
spring:
  application:
    name: workflow360-backend

server:
  port: 8080
  shutdown: graceful

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never

info:
  application:
    name: WorkFlow360 Backend
    architecture: Modular Monolith
    version: 0.0.1-SNAPSHOT
```

## Create the first module

Create these packages under `src/main/java/com/workflow360`:

```text
system
system.api
```

The initial structure becomes:

```text
com.workflow360/
├── Workflow360BackendApplication.java
└── system/
    └── api/
        ├── SystemStatusController.java
        └── SystemStatusResponse.java
```

## Create `SystemStatusResponse.java`

```java
package com.workflow360.system.api;

import java.time.Instant;

public record SystemStatusResponse(
        String application,
        String status,
        String architecture,
        Instant timestamp
) {
}
```

## Create `SystemStatusController.java`

```java
package com.workflow360.system.api;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {

    @GetMapping("/status")
    public ResponseEntity<SystemStatusResponse> getStatus() {
        var response = new SystemStatusResponse(
                "WorkFlow360 Backend",
                "UP",
                "MODULAR_MONOLITH",
                Instant.now()
        );

        return ResponseEntity.ok(response);
    }
}
```

## Create the controller test

Create the same package under `src/test/java/com/workflow360`:

```text
system.api
```

Create `SystemStatusControllerTest.java`:

```java
package com.workflow360.system.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SystemStatusController.class)
class SystemStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnSystemStatus() throws Exception {
        mockMvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("WorkFlow360 Backend"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.architecture").value("MODULAR_MONOLITH"));
    }
}
```

If the generated Spring Boot version does not resolve `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, use IntelliJ's import suggestions and confirm the current package from the generated dependency version. Do not copy imports from older tutorials blindly.

## Run tests

From PowerShell:

```powershell
cd C:\Development\workflow360\backend
.\mvnw.cmd clean test
```

Expected final result:

```text
BUILD SUCCESS
```

## Run the application

```powershell
.\mvnw.cmd spring-boot:run
```

Or run `Workflow360BackendApplication` from IntelliJ.

## Verify endpoints

Open:

```text
http://localhost:8080/actuator/health
```

Expected:

```json
{"status":"UP"}
```

Open:

```text
http://localhost:8080/api/v1/system/status
```

Expected shape:

```json
{
  "application": "WorkFlow360 Backend",
  "status": "UP",
  "architecture": "MODULAR_MONOLITH",
  "timestamp": "..."
}
```

## Planned module structure

Do not create all folders now. Add each module when its milestone starts:

```text
com.workflow360/
├── common/
├── system/
├── identity/
├── organization/
├── department/
├── employee/
├── skill/
├── project/
├── task/
├── timesheet/
├── leave/
├── allocation/
├── notification/
├── chat/
├── report/
└── audit/
```

For a substantial business module, use:

```text
employee/
├── api/
├── application/
├── domain/
└── infrastructure/
```

Do not add empty layers merely for appearance.

## Definition of Done

- [ ] Spring Initializr project extracted directly into `backend`
- [ ] Java 25 is configured in IntelliJ
- [ ] Maven dependencies load successfully
- [ ] `application.yaml` is configured
- [ ] `/actuator/health` returns `UP`
- [ ] `/api/v1/system/status` returns HTTP 200
- [ ] Controller test passes
- [ ] `mvnw.cmd clean test` reports `BUILD SUCCESS`
- [ ] No database, Docker, security, Kafka, Redis, or chat implementation has been added

## Next step

Generate the React + TypeScript frontend and make it call `/api/v1/system/status`. PostgreSQL integration comes after the frontend-backend connection is proven.
