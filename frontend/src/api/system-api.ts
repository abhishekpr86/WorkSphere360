import type { SystemStatus } from '../types/system-status'
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'


console.log('Backend URL:', API_BASE_URL)
export async function getSystemStatus(): Promise<SystemStatus> {
    const response = await fetch(`${API_BASE_URL}/api/v1/system/status`)

    if(!response.ok) {
        throw new Error(`Failed to fetch system status: ${response.status} ${response.statusText}`)
    }

    const data: SystemStatus = await response.json()
    return data

}
