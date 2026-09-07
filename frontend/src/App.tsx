import { useEffect, useState } from 'react'
import { getSystemStatus } from './api/system-api'
import type { SystemStatus } from './types/system-status'
import './App.css'

function App() {
  const [systemStatus, setSystemStatus] =
    useState<SystemStatus | null>(null)

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

      <p>
        Enterprise employee and project management platform
      </p>

      {loading && <p>Checking backend status...</p>}

      {error && (
        <p className="status-error">
          Backend error: {error}
        </p>
      )}

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