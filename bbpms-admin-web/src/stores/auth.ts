import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, logout as apiLogout, getCurrentUser, getUserMenus } from '@/api/auth'
import { setToken, removeToken, setRefreshToken, getToken } from '@/utils/auth'
import type { LoginPayload, MenuNode, UserInfo } from '@/types/auth'
import { ElMessage } from 'element-plus'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserInfo | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const menus = ref<MenuNode[]>([])

  const isLoggedIn = computed(() => !!token.value)
  const isSuper = computed(() => roles.value.includes('SUPER_ADMIN'))

  function setAuth(t: string, refresh?: string) {
    token.value = t
    setToken(t)
    if (refresh) setRefreshToken(refresh)
  }

  async function login(payload: LoginPayload) {
    const res = await apiLogin(payload)
    setAuth(res.token, res.refreshToken)
    return res
  }

  async function fetchUserInfo() {
    const info = await getCurrentUser()
    userInfo.value = info
    roles.value = info.roles || []
    permissions.value = info.permissions || []
  }

  async function fetchMenus() {
    const data = await getUserMenus()
    menus.value = data || []
    return data
  }

  async function logout() {
    try {
      await apiLogout()
    } catch (e) {
      // ignore
    }
    resetState()
    removeToken()
  }

  function resetState() {
    token.value = null
    userInfo.value = null
    roles.value = []
    permissions.value = []
    menus.value = []
  }

  function hasPermission(perm: string | string[]): boolean {
    if (isSuper.value) return true
    if (!perm) return true
    if (Array.isArray(perm)) return perm.some((p) => permissions.value.includes(p))
    return permissions.value.includes(perm)
  }

  return {
    token,
    userInfo,
    roles,
    permissions,
    menus,
    isLoggedIn,
    isSuper,
    setAuth,
    login,
    logout,
    fetchUserInfo,
    fetchMenus,
    resetState,
    hasPermission
  }
})

export function showError(msg: string) {
  ElMessage.error(msg)
}