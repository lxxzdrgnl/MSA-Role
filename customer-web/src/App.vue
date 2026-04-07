<template>
  <div id="app-root">
    <nav v-if="showNav" class="navbar">
      <div class="nav-brand">🍽 레스토랑</div>
      <div class="nav-links">
        <router-link to="/" class="nav-link">메뉴</router-link>
        <router-link to="/cart" class="nav-link cart-link">
          장바구니
          <span v-if="cartCount > 0" class="cart-badge">{{ cartCount }}</span>
        </router-link>
        <router-link to="/orders" class="nav-link">주문 내역</router-link>
        <button class="btn-logout" @click="logout">로그아웃</button>
      </div>
    </nav>

    <main :class="{ 'with-nav': showNav }">
      <router-view @cart-updated="refreshCartCount" />
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const cartCount = ref(0)

const showNav = computed(() => {
  return route.path !== '/login' && route.path !== '/register'
})

function getCartCount() {
  try {
    const cart = JSON.parse(localStorage.getItem('cart') || '[]')
    return cart.reduce((sum, item) => sum + item.quantity, 0)
  } catch {
    return 0
  }
}

function refreshCartCount() {
  cartCount.value = getCartCount()
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('userRole')
  router.push('/login')
}

// Update cart count on route change
watch(route, () => {
  refreshCartCount()
})

onMounted(() => {
  refreshCartCount()
  // Listen for storage events from other tabs
  window.addEventListener('storage', refreshCartCount)
})
</script>

<style>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}

.nav-brand {
  font-size: 18px;
  font-weight: 700;
  color: #e05c2a;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 20px;
}

.nav-link {
  text-decoration: none;
  color: #555;
  font-size: 14px;
  font-weight: 500;
  transition: color 0.15s;
  position: relative;
}

.nav-link:hover,
.nav-link.router-link-active {
  color: #e05c2a;
}

.cart-link {
  display: flex;
  align-items: center;
  gap: 4px;
}

.cart-badge {
  background: #e05c2a;
  color: #fff;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
  min-width: 18px;
  text-align: center;
}

.btn-logout {
  background: none;
  border: 1px solid #ddd;
  border-radius: 6px;
  color: #888;
  cursor: pointer;
  font-size: 13px;
  padding: 5px 12px;
  transition: all 0.15s;
}

.btn-logout:hover {
  border-color: #e05c2a;
  color: #e05c2a;
}

main {
  min-height: 100vh;
}

main.with-nav {
  padding-top: 56px;
}

.page-container {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 16px;
}

/* Common form styles */
.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #555;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.15s;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: #e05c2a;
}

.btn-primary {
  background: #e05c2a;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 11px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
  width: 100%;
}

.btn-primary:hover {
  background: #c74d20;
}

.btn-primary:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.btn-secondary {
  background: #fff;
  color: #e05c2a;
  border: 1px solid #e05c2a;
  border-radius: 8px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-secondary:hover {
  background: #fff5f2;
}

.error-msg {
  color: #d32f2f;
  font-size: 13px;
  margin-top: 10px;
  text-align: center;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #888;
  font-size: 14px;
}
</style>
