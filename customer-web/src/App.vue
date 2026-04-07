<template>
  <div id="app-root">
    <router-view @cart-updated="refreshCartCount" @logout="logout" />
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const cartCount = ref(0)

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
#app-root {
  min-height: 100vh;
}
</style>
