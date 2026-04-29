import { createRouter, createWebHistory } from 'vue-router'

import LoginView from './views/LoginView.vue'
import DashboardView from './views/DashboardView.vue'
import OrdersView from './views/OrdersView.vue'
import MenusView from './views/MenusView.vue'
import ReviewsView from './views/ReviewsView.vue'

const routes = [
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/', redirect: '/orders' },
  { path: '/orders', component: OrdersView },
  { path: '/menus', component: MenusView },
  { path: '/reviews', component: ReviewsView },
  { path: '/dashboard', component: DashboardView },
  { path: '/:pathMatch(.*)*', redirect: '/orders' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  if (to.meta.public) return true

  const token = localStorage.getItem('accessToken')
  if (!token) {
    return { path: '/login' }
  }

  const role = localStorage.getItem('role')
  if (role !== 'ADMIN') {
    alert('관리자 권한이 필요합니다.')
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('role')
    return { path: '/login' }
  }

  return true
})

export default router
