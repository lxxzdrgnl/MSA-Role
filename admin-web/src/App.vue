<template>
  <div id="layout">
    <!-- Sidebar nav — hidden on login page -->
    <aside v-if="isAuthenticated" class="sidebar">
      <div class="sidebar-header">
        <span class="logo-icon">🍽</span>
        <span class="logo-text">관리자</span>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/dashboard" class="nav-item">
          <span class="nav-icon">📊</span>
          <span>대시보드</span>
        </router-link>

        <router-link to="/orders" class="nav-item" @click="clearBadge">
          <span class="nav-icon">📋</span>
          <span>주문 관리</span>
          <span v-if="newOrderCount > 0" class="badge">{{ newOrderCount > 99 ? '99+' : newOrderCount }}</span>
        </router-link>

        <router-link to="/menus" class="nav-item">
          <span class="nav-icon">🍱</span>
          <span>메뉴 관리</span>
        </router-link>

        <router-link to="/categories" class="nav-item">
          <span class="nav-icon">🗂</span>
          <span>카테고리</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <button class="logout-btn" @click="logout">로그아웃</button>
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
#layout {
  display: flex;
  min-height: 100vh;
}

/* ── Sidebar ── */
.sidebar {
  width: 220px;
  min-height: 100vh;
  background: #1a1d23;
  color: #c9ced8;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
}

.sidebar-header {
  padding: 24px 20px 20px;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  border-bottom: 1px solid #2d3240;
  display: flex;
  align-items: center;
  gap: 10px;
  letter-spacing: -0.3px;
}

.logo-icon { font-size: 22px; }

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  color: #8a92a3;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: background 0.15s, color 0.15s;
  position: relative;
}

.nav-item:hover {
  background: #252a35;
  color: #e0e4ef;
}

.nav-item.router-link-active {
  background: #2563eb;
  color: #fff;
}

.nav-icon { font-size: 16px; flex-shrink: 0; }

.badge {
  margin-left: auto;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
}

.sidebar-footer {
  padding: 16px 12px 20px;
  border-top: 1px solid #2d3240;
}

.logout-btn {
  width: 100%;
  padding: 9px;
  border-radius: 8px;
  border: 1px solid #3a3f4d;
  background: transparent;
  color: #8a92a3;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.logout-btn:hover {
  background: #ef4444;
  color: #fff;
  border-color: #ef4444;
}

/* ── Content ── */
.content.with-sidebar {
  margin-left: 220px;
  flex: 1;
  min-height: 100vh;
  background: #f0f2f5;
}

.content.full {
  flex: 1;
  min-height: 100vh;
  background: #f0f2f5;
}

/* ── Shared utility classes used in views ── */
.page {
  padding: 32px 36px;
  max-width: 1200px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1d23;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: opacity 0.15s, transform 0.1s;
}

.btn:active { transform: scale(0.97); }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-primary { background: #2563eb; color: #fff; }
.btn-primary:hover { background: #1d4ed8; }

.btn-danger { background: #ef4444; color: #fff; }
.btn-danger:hover { background: #dc2626; }

.btn-secondary { background: #e5e7eb; color: #374151; }
.btn-secondary:hover { background: #d1d5db; }

.btn-sm { padding: 5px 12px; font-size: 13px; }

.card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 4px rgba(0,0,0,.05);
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

th {
  padding: 12px 14px;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: .5px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

td {
  padding: 12px 14px;
  border-bottom: 1px solid #f5f5f5;
  color: #374151;
  vertical-align: middle;
}

tr:last-child td { border-bottom: none; }
tr:hover td { background: #fafbff; }

.tag {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.alert-box {
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 16px;
}

.alert-error { background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca; }
.alert-success { background: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; }

.form-group {
  margin-bottom: 18px;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
}

.form-input, .form-select, .form-textarea {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  color: #1f2937;
  background: #fff;
  transition: border-color 0.15s;
  outline: none;
}

.form-input:focus, .form-select:focus, .form-textarea:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37,99,235,.1);
}

.form-textarea { resize: vertical; min-height: 80px; }
</style>
