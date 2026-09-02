import axios, { AxiosInstance, AxiosError, AxiosRequestConfig, AxiosResponse } from 'axios'
import { showToast, showFailToast, showNotify } from 'vant'
import { useAuthStore } from '@/stores/auth'

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp?: number
}

const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' }
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token && config.headers) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    if (res.code === 200 || res.code === 0) {
      return res.data as unknown as AxiosResponse
    }
    showNotify({ type: 'danger', message: res.message || '请求失败' })
    return Promise.reject(new Error(res.message || 'Error'))
  },
  (error: AxiosError<ApiResponse>) => {
    const status = error.response?.status
    const msg = error.response?.data?.message || error.message
    if (status === 401) {
      const auth = useAuthStore()
      auth.logout()
      showFailToast('登录已过期,请重新登录')
      setTimeout(() => location.replace('/login'), 800)
    } else if (status === 403) {
      showFailToast('无访问权限')
    } else if (status && status >= 500) {
      showFailToast('服务器异常')
    } else if (error.code === 'ECONNABORTED') {
      showFailToast('请求超时')
    } else {
      showToast(msg || '网络异常')
    }
    return Promise.reject(error)
  }
)

export function get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return http.get<T, T>(url, config) as unknown as Promise<T>
}
export function post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return http.post<T, T>(url, data, config) as unknown as Promise<T>
}
export function put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return http.put<T, T>(url, data, config) as unknown as Promise<T>
}
export function del<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return http.delete<T, T>(url, config) as unknown as Promise<T>
}

export default http
