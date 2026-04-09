import axios from 'axios'
import { useAuth } from './composables/useAuth'

const { getToken, logout } = useAuth()

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// Request interceptor: attach JWT
api.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor: handle 401 only for non-public endpoints
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      const url = error.config?.url || ''
      if (getToken() && !url.includes('/auth/')) {
        logout()
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default api
