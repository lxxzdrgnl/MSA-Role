<template>
  <div id="layout">
    <!-- Sidebar nav — hidden on login page -->
    <aside v-if="isAuthenticated" class="sidebar">
      <div class="sidebar-header">
        <div class="logo-mark">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="8" fill="var(--accent-brass)" fill-opacity="0.15"/>
            <path d="M8 18.5C8 18.5 9.5 14 14 14C18.5 14 20 18.5 20 18.5" stroke="var(--accent-brass)" stroke-width="1.8" stroke-linecap="round"/>
            <circle cx="14" cy="10" r="2.5" stroke="var(--accent-brass)" stroke-width="1.8"/>
            <path d="M7 20H21" stroke="var(--accent-brass)" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="logo-text">
          <span class="logo-title">Rheon Kitchen</span>
          <span class="logo-sub">Admin Console</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/dashboard" class="nav-item">
          <svg class="nav-icon" width="20" height="20" viewBox="0 0 18 18" fill="none">
            <rect x="1" y="1" width="6" height="7" rx="1.5" stroke="currentColor" stroke-width="1.4"/>
            <rect x="11" y="1" width="6" height="4" rx="1.5" stroke="currentColor" stroke-width="1.4"/>
            <rect x="1" y="12" width="6" height="5" rx="1.5" stroke="currentColor" stroke-width="1.4"/>
            <rect x="11" y="9" width="6" height="8" rx="1.5" stroke="currentColor" stroke-width="1.4"/>
          </svg>
          <span>대시보드</span>
        </router-link>

        <router-link to="/orders" class="nav-item" @click="clearBadge">
          <svg class="nav-icon" width="20" height="20" viewBox="0 0 18 18" fill="none">
            <path d="M3 5H15M3 9H15M3 13H10" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
          </svg>
          <span>주문 관리</span>
          <span v-if="newOrderCount > 0" class="badge">{{ newOrderCount > 99 ? '99+' : newOrderCount }}</span>
        </router-link>

        <router-link to="/menus" class="nav-item">
          <svg class="nav-icon" width="20" height="20" viewBox="0 0 18 18" fill="none">
            <rect x="2" y="2" width="14" height="14" rx="3" stroke="currentColor" stroke-width="1.4"/>
            <path d="M6 7H12M6 11H10" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
          </svg>
          <span>메뉴 관리</span>
        </router-link>

        <router-link to="/reviews" class="nav-item">
          <svg class="nav-icon" width="20" height="20" viewBox="0 0 18 18" fill="none">
            <path d="M9 2L11 7H16L12 10L13.5 15L9 12L4.5 15L6 10L2 7H7L9 2Z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/>
          </svg>
          <span>리뷰 관리</span>
        </router-link>

      </nav>

      <div class="sidebar-footer">
        <button class="logout-btn" @click="logout">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M6 2H4C3.44772 2 3 2.44772 3 3V13C3 13.5523 3.44772 14 4 14H6M11 11L14 8M14 8L11 5M14 8H6" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          로그아웃
        </button>
      </div>
    </aside>

    <!-- Main content -->
    <main :class="isAuthenticated ? 'content with-sidebar' : 'content full'">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const instance = getCurrentInstance()

const newOrderCount = instance.appContext.config.globalProperties.$newOrderCount

const isAuthenticated = computed(() => {
  return route.path !== '/login' && !!localStorage.getItem('accessToken')
})

let ws = null

function connectWebSocket() {
  if (!localStorage.getItem('accessToken')) return

  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const host = window.location.host
  const wsUrl = `${protocol}://${host}/ws/orders/admin`

  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    console.log('[WS] 관리자 주문 WebSocket 연결됨')
  }

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      // New order notification — increment badge
      if (data.status === 'PENDING' || !data.status) {
        newOrderCount.value++
      }
    } catch (e) {
      console.error('[WS] 메시지 파싱 오류', e)
    }
  }

  ws.onclose = (event) => {
    console.log('[WS] WebSocket 연결 종료, 5초 후 재연결 시도')
    if (localStorage.getItem('accessToken')) {
      setTimeout(connectWebSocket, 5000)
    }
  }

  ws.onerror = (err) => {
    console.error('[WS] WebSocket 오류', err)
  }
}

function clearBadge() {
  newOrderCount.value = 0
}

function logout() {
  if (ws) {
    ws.onclose = null // prevent reconnect
    ws.close()
    ws = null
  }
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('role')
  router.push('/login')
}

onMounted(() => {
  if (localStorage.getItem('accessToken')) {
    connectWebSocket()
  }

  // Re-connect when navigating from login
  router.afterEach(() => {
    if (localStorage.getItem('accessToken') && (!ws || ws.readyState === WebSocket.CLOSED)) {
      connectWebSocket()
    }
  })
})

onUnmounted(() => {
  if (ws) {
    ws.onclose = null
    ws.close()
  }
})
</script>

<style>
/* ============================================================
   LAYOUT
   ============================================================ */
#layout {
  display: flex;
  min-height: 100vh;
}

/* ── Sidebar ── */
.sidebar {
  width: var(--sidebar-width);
  min-height: 100vh;
  background: var(--bg-surface);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
  font-size: 15px;
}

.sidebar-header {
  padding: 24px 20px 22px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-mark {
  flex-shrink: 0;
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.logo-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.5px;
  line-height: 1.2;
}

.logo-sub {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-muted);
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 15px;
  font-weight: 500;
  transition: all var(--transition-fast);
  position: relative;
}

.nav-item:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.nav-item.router-link-active {
  background: var(--accent-brass-glow);
  color: var(--accent-brass);
  box-shadow: inset 0 0 0 1px var(--accent-brass-border);
}

.nav-item.router-link-active .nav-icon {
  color: var(--accent-brass);
}

.nav-icon {
  flex-shrink: 0;
  opacity: 0.7;
}

.nav-item:hover .nav-icon,
.nav-item.router-link-active .nav-icon {
  opacity: 1;
}

.badge {
  margin-left: auto;
  background: var(--danger);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 1px 7px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
  line-height: 18px;
  animation: badge-pulse 2s ease-in-out infinite;
}

@keyframes badge-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(200, 120, 100, 0.4); }
  50% { box-shadow: 0 0 0 6px rgba(200, 120, 100, 0); }
}

.sidebar-footer {
  padding: 16px 12px 20px;
  border-top: 1px solid var(--border);
}

.logout-btn {
  width: 100%;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-strong);
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-family: inherit;
}

.logout-btn:hover {
  background: var(--danger-bg);
  color: var(--danger);
  border-color: rgba(248, 113, 113, 0.2);
}

/* ── Content ── */
.content.with-sidebar {
  margin-left: var(--sidebar-width);
  flex: 1;
  min-height: 100vh;
  background: var(--bg-base);
}

.content.full {
  flex: 1;
  min-height: 100vh;
  background: var(--bg-base);
}

/* Shared utility classes → see /styles/global.css */
</style>
