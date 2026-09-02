import { useAuthStore } from '@/stores/auth'

const TOKEN_KEY = 'bbpms_admin_token'
const REFRESH_KEY = 'bbpms_admin_refresh'

// memory cache
let inMemoryToken: string | null = null

export function getToken(): string | null {
  if (inMemoryToken) return inMemoryToken
  const t = sessionStorage.getItem(TOKEN_KEY)
  inMemoryToken = t
  return t
}

export function setToken(token: string | null): void {
  inMemoryToken = token
  if (token) {
    sessionStorage.setItem(TOKEN_KEY, token)
  } else {
    sessionStorage.removeItem(TOKEN_KEY)
  }
}

export function removeToken(): void {
  inMemoryToken = null
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(REFRESH_KEY)
}

export function getRefreshToken(): string | null {
  return sessionStorage.getItem(REFRESH_KEY)
}

export function setRefreshToken(token: string | null): void {
  if (token) {
    sessionStorage.setItem(REFRESH_KEY, token)
  } else {
    sessionStorage.removeItem(REFRESH_KEY)
  }
}

export function logoutLocal(): void {
  removeToken()
  const auth = useAuthStore()
  auth.resetState()
}