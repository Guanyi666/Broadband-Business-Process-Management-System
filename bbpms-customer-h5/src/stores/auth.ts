import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import JSEncrypt from 'jsencrypt'
import * as api from '@/api/auth'
import type { UserInfo } from '@/types'

const TOKEN_KEY = 'bbpms_customer_token'
const USER_KEY = 'bbpms_customer_user'

export const useAuthStore = defineStore('customer-auth', () => {
  const token = ref(sessionStorage.getItem(TOKEN_KEY) || '')
  const user = ref<UserInfo | null>(JSON.parse(sessionStorage.getItem(USER_KEY) || 'null'))
  const authenticated = computed(() => !!token.value)

  async function signIn(username: string, password: string) {
    const key = await api.fetchPublicKey()
    const rsa = new JSEncrypt()
    rsa.setPublicKey(key)
    const encrypted = rsa.encrypt(password)
    if (!encrypted) throw new Error('密码加密失败')
    const result = await api.login({ username, password: encrypted })
    if (!result.user?.roles?.includes('CUSTOMER')) {
      throw new Error('该账号不是客户自助账号')
    }
    token.value = result.accessToken || result.token || ''
    user.value = result.user
    sessionStorage.setItem(TOKEN_KEY, token.value)
    sessionStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  async function signOut() {
    try { await api.logout() } catch { /* local logout still succeeds */ }
    token.value = ''
    user.value = null
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(USER_KEY)
  }
  return { token, user, authenticated, signIn, signOut }
})
