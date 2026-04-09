<template>
  <div id="app-root">
    <router-view @logout="logout" />

    <transition-group name="notif-slide" tag="div" class="notif-stack">
      <div v-for="n in notifications" :key="n.id" class="notif" :class="`notif--${n.tone}`">
        <div class="notif-accent"></div>
        <div class="notif-content">
          <div class="notif-label">{{ n.label }}</div>
          <div class="notif-title">{{ n.title }}</div>
        </div>
        <button class="notif-close" @click="removeNotif(n.id)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
    </transition-group>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from './api'
import { useAuth } from './composables/useAuth'
import { STATUS_MAP } from './constants'

const router = useRouter()
const route = useRoute()
const { isAuthenticated, logout: authLogout, getToken } = useAuth()
const notifications = ref([])
let ws = null
let notifId = 0


function addNotif(title, label, tone) {
  notifications.value.push({ id: ++notifId, title, label, tone })
}

function removeNotif(id) {
  notifications.value = notifications.value.filter(n => n.id !== id)
}

function connectWs() {
  if (ws) { ws.close(); ws = null }
  if (!isAuthenticated()) return

  api.get('/auth/profile').then(r => {
    const userId = r.data.id
    const proto = location.protocol === 'https:' ? 'wss' : 'ws'
    ws = new WebSocket(`${proto}://${location.host}/ws/orders/${userId}`)
    ws.onmessage = e => {
      try {
        const data = JSON.parse(e.data)
        if (data.status) {
          const st = STATUS_MAP[data.status] || { label: data.status || '상태 변경', tone: 'default' }
          const orderId = data.orderId ? `#${data.orderId}` : ''
          addNotif(st.label, `주문${orderId}`, st.tone)
        }
      } catch {}
    }
    ws.onclose = () => { setTimeout(() => { if (isAuthenticated()) connectWs() }, 5000) }
  }).catch(() => {})
}

function logout() {
  if (ws) { ws.close(); ws = null }
  authLogout()
  router.push('/login')
}

watch(route, () => {
  if (!ws && isAuthenticated()) connectWs()
})

onMounted(() => {
  if (isAuthenticated()) connectWs()
})

onUnmounted(() => {
  if (ws) ws.close()
})
</script>

<style scoped>
#app-root {
  min-height: 100vh;
}

.notif-stack {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  display: flex;
  flex-direction: column-reverse;
  gap: 8px;
  pointer-events: none;
}

.notif {
  pointer-events: auto;
  display: flex;
  align-items: center;
  gap: 0;
  background: #1f1a14;
  border: 1px solid rgba(212,134,60,0.3);
  border-radius: 10px;
  padding: 0;
  min-width: 280px;
  max-width: 340px;
  overflow: hidden;
  box-shadow: 0 4px 24px rgba(0,0,0,0.5), 0 0 40px rgba(212,134,60,0.08);
}

.notif-accent {
  width: 4px;
  align-self: stretch;
  flex-shrink: 0;
  background: #d4863c;
}
.notif--danger { border-color: rgba(220,70,60,0.3); background: #1f1514; box-shadow: 0 4px 24px rgba(0,0,0,0.5), 0 0 40px rgba(220,70,60,0.08); }
.notif--danger .notif-accent { background: #dc4640; }
.notif--danger .notif-label { color: #dc4640; }

.notif-content {
  flex: 1;
  padding: 14px 16px;
  min-width: 0;
}

.notif-label {
  font-size: 10px;
  font-weight: 600;
  color: #d4863c;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  margin-bottom: 3px;
}

.notif-title {
  font-size: 14px;
  font-weight: 700;
  color: #ede8df;
}

.notif-close {
  width: 40px;
  height: 100%;
  min-height: 48px;
  background: none;
  border: none;
  border-left: 1px solid rgba(212,134,60,0.12);
  color: #9c9589;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.15s ease;
}
.notif-close:hover {
  background: rgba(212,134,60,0.08);
  color: #d4863c;
}

.notif-slide-enter-active { transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1); }
.notif-slide-leave-active { transition: all 0.15s ease; }
.notif-slide-enter-from { opacity: 0; transform: translateY(8px) scale(0.97); }
.notif-slide-leave-to { opacity: 0; transform: translateX(20px); }
</style>
