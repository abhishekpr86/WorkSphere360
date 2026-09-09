# WorkFlow360 — Project Foundation

## Step 1: Verify the Development Prerequisites

> **Architecture for now:** React + TypeScript → Spring Boot modular monolith → PostgreSQL  
> **Do not install/configure yet:** Kafka, Redis, Eureka, API Gateway, Kubernetes or multiple backend services.

---

## Objective

Before generating the project, verify that the tools required for local development are installed and working.

At the end of this step, you should have:

- Java available from the terminal
- Maven available from the terminal
- Node.js and npm available from the terminal
- Git available from the terminal
- Docker available and running
- An IDE for Java
- VS Code or another editor for React
- A GitHub account

PostgreSQL will initially run through Docker, so a separate local PostgreSQL installation is optional.

---

## 1. Create a Temporary Workspace

Choose a parent folder where the project will live. Do not generate the backend or frontend yet.

### Windows PowerShell

```powershell
mkdir C:\Development -ErrorAction SilentlyContinue
cd C:\Development
```

You may use another location, but avoid folders synchronized by OneDrive until you are comfortable resolving file-locking and path issues.

### macOS or Linux

```bash
mkdir -p ~/Development
cd ~/Development
```

---

## 2. Check Java

Run:

```bash
java --version
javac --version
```

### Expected result

- Both commands work.
- `java` and `javac` report compatible versions.
- Use a supported Java LTS release for the project.

If `java` works but `javac` does not, you may have only a runtime or an incorrect `PATH` configuration instead of a complete JDK.

---

## 3. Check Maven

Run:

```bash
mvn --version
```

### Expected result

The output should show:

- Apache Maven version
- Java version used by Maven
- Java home
- Operating system

Confirm that Maven is using the intended JDK.

If Maven is unavailable, we can still use the Maven Wrapper generated with Spring Boot later, but installing Maven is useful for learning and troubleshooting.

---

## 4. Check Node.js and npm

Run:

```bash
node --version
npm --version
```

### Expected result

Both commands return version numbers. Use an active Node.js LTS release rather than an experimental/current release for this project.

---

## 5. Check Git

Run:

```bash
git --version
```

Configure your identity if it is not already configured:

```bash
git config --global user.name "Your Name"
git config --global user.email "your-email@example.com"
```

Verify:

```bash
git config --global user.name
git config --global user.email
```

Use the email associated with your GitHub account if you want commits attributed to your profile. Do not put passwords or access tokens in Git configuration files.

---

## 6. Check Docker

Start Docker Desktop first if you are on Windows or macOS. Then run:

```bash
docker --version
docker compose version
docker info
```

### Expected result

- Docker version is displayed.
- Docker Compose version is displayed.
- `docker info` connects successfully to the Docker engine.

If the first two commands work but `docker info` fails, the Docker CLI is installed but the Docker engine is probably not running.

Run a small verification container:

```bash
docker run --rm hello-world
```

This is only an environment test. It does not become part of WorkFlow360.

---

## 7. Check the Editors

Recommended setup:

- **IntelliJ IDEA** for Java and Spring Boot
- **VS Code** for React and TypeScript

Useful VS Code extensions can be installed later. Do not install a large collection of extensions before the project needs them.

Optional command checks:

```bash
code --version
```

The `idea` terminal command is optional; IntelliJ can be opened normally from the desktop.

---

## 8. Verify GitHub Access

Check that you can sign in to GitHub from the browser.

Do not create the remote repository yet unless you already know how you want to configure its visibility. The repository will be created in Step 2 after the local folder structure is prepared.

---

## 9. Capture the Tool Versions

Create a simple version record using this template:

```text
Operating System:
Java:
Javac:
Maven:
Node.js:
npm:
Git:
Docker:
Docker Compose:
Java IDE:
Frontend Editor:
```

You can collect the terminal output in one session.

### Windows PowerShell

```powershell
java --version
javac --version
mvn --version
node --version
npm --version
git --version
docker --version
docker compose version
```

### macOS or Linux

```bash
java --version
javac --version
mvn --version
node --version
npm --version
git --version
docker --version
docker compose version
```

---

## 10. Do Not Install These Yet

Do not configure these during Step 1:

- Redis
- Kafka
- ZooKeeper
- Eureka
- Spring Cloud Config Server
- API Gateway
- Kubernetes
- Jenkins
- Prometheus
- Grafana
- Elasticsearch/OpenSearch

They will be introduced only when WorkFlow360 has a business or operational requirement for them.

---

## Troubleshooting Guide

### `java` is not recognized

- Confirm that a JDK is installed.
- Configure `JAVA_HOME`.
- Add the JDK `bin` directory to `PATH`.
- Close and reopen the terminal.

### Maven uses the wrong Java version

- Check `mvn --version`.
- Correct `JAVA_HOME`.
- Reopen the terminal and check again.

### `node` or `npm` is not recognized

- Confirm Node.js is installed.
- Reopen the terminal after installation.
- Confirm the Node.js installation directory is in `PATH`.

### Docker commands exist but containers do not start

- Start Docker Desktop or the Docker service.
- Run `docker info`.
- On Windows, confirm the required virtualization/WSL environment is functioning.

### Commands work in one terminal but not another

The editors may have been opened before environment-variable changes. Restart the terminal and, if necessary, the editor.

---

## Definition of Done

- [ ] `java --version` works
- [ ] `javac --version` works
- [ ] `mvn --version` works
- [ ] `node --version` works
- [ ] `npm --version` works
- [ ] `git --version` works
- [ ] Git user name is configured
- [ ] Git email is configured
- [ ] `docker --version` works
- [ ] `docker compose version` works
- [ ] `docker info` connects to the engine
- [ ] `docker run --rm hello-world` succeeds
- [ ] Java IDE is installed
- [ ] React/TypeScript editor is installed
- [ ] GitHub sign-in works
- [ ] Installed versions have been recorded

---

## Completion Notes

Paste your results here after checking:

```text
Operating System:
Java:
Javac:
Maven:
Node.js:
npm:
Git:
Docker:
Docker Compose:
Java IDE:
Frontend Editor:
Problems encountered:
```

---

## Next Step

After this checklist passes, proceed to:

**Step 2 — Create the WorkFlow360 repository and modular-monolith folder structure.**
