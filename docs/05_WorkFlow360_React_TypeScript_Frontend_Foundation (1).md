# WorkFlow360 — React + TypeScript Frontend Foundation

## Objective

Generate the React + TypeScript frontend with Vite, run it locally, organize the initial source structure, and prepare it to call the Spring Boot backend.

## Prerequisite

Before starting, verify:

```powershell
node --version
npm --version
```

Use a supported Node.js LTS version.

## 1. Generate React inside the existing frontend folder

Open PowerShell:

```powershell
cd C:\Development\workflow360\frontend
npm create vite@latest . -- --template react-ts
npm install
```

The dot (`.`) means: generate the project in the current folder.

If Vite asks whether to continue because the folder is not empty, remove only safe placeholder files such as `.gitkeep`, then run the command again. Do not delete planning or application files accidentally.

## 2. Run the generated application

```powershell
npm run dev
```

Open the URL printed in the terminal, normally:

```text
http://localhost:5173
```

Confirm that the initial Vite React page appears.

Stop it with:

```text
Ctrl + C
```

## 3. Verify the production build

```powershell
npm run build
```

Expected: the command succeeds and a `dist` folder is produced.

## 4. Clean the starter UI

Inside `src`, keep `main.tsx` and replace `App.tsx` with a simple foundation component:

```tsx
import './App.css'

function App() {
  return (
    <main className="app-shell">
      <h1>WorkFlow360</h1>
      <p>Enterprise employee and project management platform</p>
      <p>Frontend status: UP</p>
    </main>
  )
}

export default App
```

Replace `src/App.css` with:

```css
.app-shell {
  max-width: 960px;
  margin: 0 auto;
  padding: 3rem 1.5rem;
  font-family: Arial, sans-serif;
}
```

Replace `src/index.css` with:

```css
:root {
  font-family: Arial, sans-serif;
  color: #1f2937;
  background: #f8fafc;
  font-synthesis: none;
  text-rendering: optimizeLegibility;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  min-width: 320px;
  min-height: 100vh;
}
```

Delete unused starter assets if they are no longer referenced.

## 5. Create only the initial frontend folders

Inside `src`, create:

```text
app/
api/
components/
features/
layouts/
routes/
types/
utils/
styles/
```

Do not create every future business feature yet.

The structure should be:

```text
frontend/
├── public/
├── src/
│   ├── api/
│   ├── app/
│   ├── components/
│   ├── features/
│   ├── layouts/
│   ├── routes/
│   ├── styles/
│   ├── types/
│   ├── utils/
│   ├── App.css
│   ├── App.tsx
│   ├── index.css
│   └── main.tsx
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## 6. Add the backend API base URL

Create this file in the frontend root:

```text
.env.example
```

Content:

```properties
VITE_API_BASE_URL=http://localhost:8080
```

Create a local `.env` file with the same value:

```properties
VITE_API_BASE_URL=http://localhost:8080
```

Later, `.env` must not be committed to Git. `.env.example` should be committed because it documents required variables without secrets.

## 7. Create a typed system-status client

Create `src/types/system-status.ts`:

```ts
export interface SystemStatus {
  application: string
  status: string
  architecture: string
  timestamp: string
}
```

Create `src/api/system-api.ts`:

```ts
import type { SystemStatus } from '../types/system-status'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL

export async function getSystemStatus(): Promise<SystemStatus> {
  const response = await fetch(`${API_BASE_URL}/api/v1/system/status`)

  if (!response.ok) {
    throw new Error(`System status request failed with HTTP ${response.status}`)
  }

  return response.json() as Promise<SystemStatus>
}
```

## 8. Configure backend CORS for local React development

Because React runs on port 5173 and Spring Boot runs on 8080, the browser treats them as different origins.

For this foundation only, add this annotation to `SystemStatusController`:

```java
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {
```

This is temporary and limited to one controller. Later, replace it with centralized CORS configuration, especially when Spring Security is introduced.

## 9. Display the backend status

Replace `App.tsx` with:

```tsx
import { useEffect, useState } from 'react'
import { getSystemStatus } from './api/system-api'
import type { SystemStatus } from './types/system-status'
import './App.css'

function App() {
  const [systemStatus, setSystemStatus] = useState<SystemStatus | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function loadSystemStatus() {
      try {
        const data = await getSystemStatus()
        setSystemStatus(data)
      } catch (requestError) {
        const message =
          requestError instanceof Error
            ? requestError.message
            : 'An unexpected error occurred'

        setError(message)
      } finally {
        setLoading(false)
      }
    }

    void loadSystemStatus()
  }, [])

  return (
    <main className="app-shell">
      <h1>WorkFlow360</h1>
      <p>Enterprise employee and project management platform</p>

      {loading && <p>Checking backend status...</p>}

      {error && <p className="status-error">Backend error: {error}</p>}

      {systemStatus && (
        <section className="status-card">
          <h2>System Status</h2>
          <p>Application: {systemStatus.application}</p>
          <p>Status: {systemStatus.status}</p>
          <p>Architecture: {systemStatus.architecture}</p>
          <p>Checked at: {systemStatus.timestamp}</p>
        </section>
      )}
    </main>
  )
}

export default App
```

Update `App.css`:

```css
.app-shell {
  max-width: 960px;
  margin: 0 auto;
  padding: 3rem 1.5rem;
  font-family: Arial, sans-serif;
}

.status-card {
  margin-top: 2rem;
  padding: 1.5rem;
  border: 1px solid #dbe3ec;
  border-radius: 0.75rem;
  background: #ffffff;
}

.status-error {
  color: #b91c1c;
}
```

## 10. Run backend and frontend together

Terminal 1:

```powershell
cd C:\Development\workflow360\backend
.\mvnw.cmd spring-boot:run
```

Terminal 2:

```powershell
cd C:\Development\workflow360\frontend
npm run dev
```

Open:

```text
http://localhost:5173
```

The page should show the backend application name, status, architecture, and timestamp.

## 11. Verify quality checks

```powershell
npm run lint
npm run build
```

Both commands should succeed.

## Definition of Done

- [ ] React + TypeScript project generated with Vite
- [ ] `npm install` succeeds
- [ ] `npm run dev` starts the frontend
- [ ] `npm run build` succeeds
- [ ] Starter UI is removed
- [ ] Initial feature-based directories exist
- [ ] `.env.example` documents `VITE_API_BASE_URL`
- [ ] Typed system-status API client exists
- [ ] Backend allows the local frontend origin for the status endpoint
- [ ] Loading state is visible during the request
- [ ] Error state is visible if the backend is stopped
- [ ] Successful backend status is displayed when both applications run
- [ ] No React Router, UI library, authentication, state library, chat, Redis, or Kafka has been added yet

## Next milestone

After this works, add PostgreSQL locally and connect Spring Boot using Spring Data JPA and Flyway. Docker can still be introduced later.
