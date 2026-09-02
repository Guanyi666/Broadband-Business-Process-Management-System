import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import JSEncrypt from 'jsencrypt'
import * as authApi from '@/api/auth'
import type { LoginParams, LoginResult, UserInfo } from '@/types/auth'

const TOKEN_KEY = 'bbpms_installer_token'
const REFRESH_KEY = 'bbpms_installer_refresh'
const USER_KEY = 'bbpms_installer_user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(sessionStorage.getItem(TOKEN_KEY) || '')
  const refreshToken = ref<string>(localStorage.getItem(REFRESH_KEY) || '')
  const userInfo = ref<UserInfo | null>(JSON.parse(sessionStorage.getItem(USER_KEY) || 'null'))

  const isAuthenticated = computed(() => !!token.value)

  function setSession(payload: LoginResult) {
    token.value = payload.accessToken
    refreshToken.value = payload.refreshToken || ''
    userInfo.value = payload.user
    sessionStorage.setItem(TOKEN_KEY, payload.accessToken)
    if (payload.refreshToken) sessionStorage.setItem(REFRESH_KEY, payload.refreshToken)
    if (payload.user) sessionStorage.setItem(USER_KEY, JSON.stringify(payload.user))
  }

  function encryptPassword(pwd: string, publicKey?: string): string {
    if (!publicKey) return pwd
    const enc = new JSEncrypt()
    enc.setPublicKey(publicKey)
    return enc.encrypt(pwd) || pwd
  }

  async function login(params: LoginParams, publicKey?: string): Promise<void> {
    const res = await authApi.login({ ...params, password: encryptPassword(params.password, publicKey) })
    setSession(res)
  }

  function logout(): void {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(REFRESH_KEY)
    sessionStorage.removeItem(USER_KEY)
  }

  return { token, refreshToken, userInfo, isAuthenticated, login, logout }
})
