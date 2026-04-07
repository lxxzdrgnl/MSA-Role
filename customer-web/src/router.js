import { createRouter, createWebHistory } from 'vue-router'
import LoginView from './views/LoginView.vue'
import RegisterView from './views/RegisterView.vue'
import MenuView from './views/MenuView.vue'
import CartView from './views/CartView.vue'
import OrderListView from './views/OrderListView.vue'
import OrderDetailView from './views/OrderDetailView.vue'

const routes = [
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/register', component: RegisterView, meta: { public: true } },
  { path: '/', component: MenuView },
  { path: '/cart', component: CartView },
  { path: '/orders', component: OrderListView },
  { path: '/orders/:id', component: OrderDetailView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('userRole')

  if (to.meta.public) {
    // Already logged in → redirect to menu
    if (token) return next('/')
    return next()
  }

  if (!token) {
    return next('/login')
  }

  // Admin users cannot use the customer app
  if (role === 'ADMIN') {
    localStorage.removeItem('token')
    localStorage.removeItem('userRole')
    return next({ path: '/login', query: { adminBlocked: '1' } })
  }

  next()
})

export default router
