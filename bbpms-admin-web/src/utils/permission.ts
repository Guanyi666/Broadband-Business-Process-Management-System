import { useAuthStore } from '@/stores/auth'

export function hasPermission(perm: string | string[]): boolean {
  const auth = useAuthStore()
  if (auth.isSuper) return true
  if (!perm) return true
  if (Array.isArray(perm)) return perm.some((p) => auth.permissions.includes(p))
  return auth.permissions.includes(perm)
}

export function hasRole(role: string | string[]): boolean {
  const auth = useAuthStore()
  if (Array.isArray(role)) return role.some((r) => auth.roles.includes(r))
  return auth.roles.includes(role)
}

export function hasAnyRole(roles: string[]): boolean {
  const auth = useAuthStore()
  return auth.roles.some((r) => roles.includes(r))
}