import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from './auth'
import router from '@/router'

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  success: boolean
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 20000,
  withCredentials: true
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    // file blob passthrough
    if (response.config.responseType === 'blob') return response

    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 0 || res.code === 200 || res.success) {
        return res.data as any
      }
      if (res.code === 401) {
        handleUnauthorized()
        return Promise.reject(new Error(res.message || 'Unauthenticated'))
      }
      ElMessage.error(res.message || 'Request failed')
      return Promise.reject(new Error(res.message || 'Request failed'))
    }
    return res as any
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      handleUnauthorized()
    } else {
      const msg = error?.response?.data?.message || error.message || 'Network error'
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

function handleUnauthorized() {
  removeToken()
  ElMessage.error('Session expired. Please log in again.')
  const cur = router.currentRoute.value.fullPath
  router.replace({ path: '/login', query: { redirect: cur } })
}

export function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return service.request<unknown, T>(config)
}

export default service