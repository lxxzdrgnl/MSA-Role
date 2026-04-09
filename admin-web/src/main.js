import { createApp, ref } from 'vue'
import App from './App.vue'
import router from './router.js'
import { authVerify } from './api.js'
import './styles/global.css'

// Global new-order badge counter — shared via app.config.globalProperties
const newOrderCount = ref(0)

async function bootstrap() {
  const token = localStorage.getItem('accessToken')

  if (token) {
    try {
      const res = await authVerify()
      const role = res.data?.role || res.data?.userRole
      if (role !== 'ADMIN') {
        alert('관리자 권한이 필요합니다.')
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('role')
      } else {
        localStorage.setItem('role', role)
      }
    } catch {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('role')
    }
  }

  const app = createApp(App)
  app.config.globalProperties.$newOrderCount = newOrderCount
  app.use(router)
  app.mount('#app')
}

bootstrap()
