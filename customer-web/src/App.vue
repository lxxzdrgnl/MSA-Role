<template>
  <div id="app-root">
    <router-view @cart-updated="refreshCartCount" />

    <!-- Bottom Navigation -->
    <nav v-if="showNav" class="bottom-nav">
      <router-link to="/" class="nav-item" :class="{ active: isRoute('/') }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/>
          <polyline points="9,22 9,12 15,12 15,22"/>
        </svg>
        <span>메뉴</span>
      </router-link>

      <router-link to="/cart" class="nav-item" :class="{ active: isRoute('/cart') }">
        <div class="cart-icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/>
            <path d="M1 1h4l2.68 13.39a2 2 0 002 1.61h9.72a2 2 0 001.95-1.56l1.35-6.44H6"/>
          </svg>
          <span v-if="cartCount > 0" class="cart-dot">{{ cartCount > 9 ? '9+' : cartCount }}</span>
        </div>
        <span>장바구니</span>
      </router-link>

      <router-link to="/orders" class="nav-item" :class="{ active: isRoute('/orders') }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
          <polyline points="14,2 14,8 20,8"/>
          <line x1="16" y1="13" x2="8" y2="13"/>
          <line x1="16" y1="17" x2="8" y2="17"/>
          <polyline points="10,9 9,9 8,9"/>
        </svg>
        <span>주문내역</span>
      </router-link>

      <button class="nav-item" @click="logout">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/>
          <polyline points="16,17 21,12 16,7"/>
          <line x1="21" y1="12" x2="9" y2="12"/>
        </svg>
        <span>로그아웃</span>
      </button>
    </nav>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const cartCount = ref(0)

const showNav = computed(() =>
  route.path !== '/login' && route.path !== '/register'
)

function isRoute(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function getCartCount() {
  try {
    const cart = JSON.parse(localStorage.getItem('cart') || '[]')
    return cart.reduce((sum, item) => sum + item.quantity, 0)
  } catch { return 0 }
}

function refreshCartCount() {
  cartCount.value = getCartCount()
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('userRole')
  router.push('/login')
}

watch(route, refreshCartCount)
onMounted(() => {
  refreshCartCount()
  window.addEventListener('storage', refreshCartCount)
})
</script>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  background: rgba(24, 22, 18, 0.92);
  backdrop-filter: blur(20px) saturate(1.5);
  -webkit-backdrop-filter: blur(20px) saturate(1.5);
  border-top: 1px solid rgba(255,255,255,0.07);
  display: flex;
  align-items: stretch;
  height: 64px;
  padding: 0 8px;
  z-index: 200;
  padding-bottom: env(safe-area-inset-bottom);
}

.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: var(--text-muted);
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.03em;
  background: none;
  border: none;
  transition: color 0.2s;
  padding: 8px 0;
  position: relative;
  text-decoration: none;
  font-family: 'Noto Sans KR', sans-serif;
}
.nav-item svg {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  transition: var(--transition);
}
.nav-item:hover { color: var(--text-secondary); }
.nav-item.active { color: var(--accent); }
.nav-item.active svg { filter: drop-shadow(0 0 6px rgba(232,92,30,0.5)); }

.cart-icon-wrap { position: relative; width: 20px; height: 20px; }
.cart-dot {
  position: absolute;
  top: -5px;
  right: -7px;
  background: var(--accent);
  color: #fff;
  font-size: 9px;
  font-weight: 700;
  line-height: 1;
  padding: 2px 4px;
  border-radius: 99px;
  min-width: 16px;
  text-align: center;
  border: 1.5px solid var(--bg-elevated);
}
</style>
