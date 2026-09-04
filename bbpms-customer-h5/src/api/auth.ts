import { get, post } from './http'
import type { LoginResult, UserInfo } from '@/types'

export const fetchPublicKey = () => get<string>('/auth/public-key')
export const login = (data: { username: string; password: string }) => post<LoginResult>('/auth/login', data)
export const logout = () => post<void>('/auth/logout')
export const me = () => get<UserInfo>('/auth/me')
