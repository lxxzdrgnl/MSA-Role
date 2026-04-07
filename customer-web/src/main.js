import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import api from './api'
import './style.css'

async function bootstrap() {
  const token = localStorage.getItem('token')

  if (token) {
    try {
      // Verify token validity
      const res = await api.post('/auth/verify', { token })
      const role = res.data?.role || localStorage.getItem('userRole')
      localStorage.setItem('userRole', role)

      if (role === 'ADMIN') {
        localStorage.removeItem('token')
        localStorage.removeItem('userRole')
      }
    } catch {
      localStorage.removeItem('token')
      localStorage.removeItem('userRole')
    }
  }

  const app = createApp(App)
  app.use(router)
  app.mount('#app')
}

bootstrap()
