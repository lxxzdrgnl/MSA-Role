import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// Request interceptor: attach JWT
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
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
      // Only force logout if a token was sent but rejected
      if (localStorage.getItem('token') && !url.includes('/auth/')) {
        localStorage.removeItem('token')
        localStorage.removeItem('userRole')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default api
