export interface InstallerLocation {
  installerId: number | string
  name: string
  phone?: string
  lng: number
  lat: number
  online: boolean
  status?: string
  workload?: number
  rating?: number
  updatedAt?: string
}

export interface InstallerProfile {
  id: number | string
  name: string
  username?: string
  phone?: string
  status: 'IDLE' | 'WORKING' | 'OFFLINE' | 'ON_BREAK'
  workload?: number
  rating?: number
  lastActiveAt?: string
}