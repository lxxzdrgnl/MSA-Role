import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// Request interceptor: attach JWT
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor: handle 401 only when token was sent but rejected
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const url = error.config?.url || ''
      if (localStorage.getItem('accessToken') && !url.includes('/auth/')) {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('role')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

// ── Auth ──────────────────────────────────────────────
export const authLogin = (email, password) =>
  api.post('/auth/login', { email, password })

export const authVerify = () => {
  const token = localStorage.getItem('accessToken')
  return api.post('/auth/verify', { token })
}

// ── Orders ───────────────────────────────────────────
export const getOrders = (params) =>
  api.get('/orders/all', { params })

export const updateOrderStatus = (id, status) =>
  api.patch(`/orders/${id}/status`, { status })

export const getRevenue = (date) =>
  api.get('/orders/revenue', { params: { date } })

export const getActiveCount = () =>
  api.get('/orders/active-count')

export const getBestSellers = () =>
  api.get('/orders/best-sellers')

// ── Menus ─────────────────────────────────────────────
export const getMenus = (params) =>
  api.get('/menus', { params })

export const createMenu = (formData) =>
  api.post('/menus', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })

export const updateMenu = (id, formData) =>
  api.put(`/menus/${id}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })

export const toggleSoldOut = (id, isSoldOut) =>
  api.patch(`/menus/${id}/sold-out`, { isSoldOut })

export const toggleBestSeller = (id, isBestSeller) =>
  api.patch(`/menus/${id}/best-seller`, { isBestSeller })

export const deleteMenu = (id) =>
  api.delete(`/menus/${id}`)

export const getMenuById = (id) =>
  api.get(`/menus/${id}`)

// ── Categories ────────────────────────────────────────
export const getCategories = () =>
  api.get('/menus/categories')

export const createCategory = (name) =>
  api.post('/menus/categories', { name })

export const deleteCategory = (id) =>
  api.delete(`/menus/categories/${id}`)

// ── Operations ────────────────────────────────────────
export const getCongestion = () =>
  api.get('/operations/congestion')

// ── Reviews ───────────────────────────────────────────
export const getReviewSummary = (menuId) =>
  api.get('/reviews/summary', { params: { menuId } })

export default api
