<template>
  <div class="page-container">
    <h1 class="page-title">장바구니</h1>

    <div v-if="cart.length === 0" class="empty-state">
      <div class="empty-icon">🛒</div>
      <p>장바구니가 비어 있습니다.</p>
      <router-link to="/" class="btn-secondary" style="display:inline-block; margin-top:16px; width:auto; padding: 10px 24px;">
        메뉴 보러가기
      </router-link>
    </div>

    <div v-else>
      <div class="cart-list">
        <div v-for="item in cart" :key="item.menuId" class="cart-item">
          <div class="item-info">
            <span class="item-name">{{ item.name }}</span>
            <span class="item-unit-price">{{ formatPrice(item.price) }}원</span>
          </div>
          <div class="item-controls">
            <button class="qty-btn" @click="changeQty(item, -1)">−</button>
            <span class="qty-value">{{ item.quantity }}</span>
            <button class="qty-btn" @click="changeQty(item, 1)">+</button>
            <span class="item-subtotal">{{ formatPrice(item.price * item.quantity) }}원</span>
            <button class="btn-remove" @click="removeItem(item)" title="삭제">✕</button>
          </div>
        </div>
      </div>

      <div class="cart-summary">
        <div class="summary-row">
          <span>총 상품 금액</span>
          <span class="summary-total">{{ formatPrice(totalPrice) }}원</span>
        </div>
      </div>

      <div v-if="errorMsg" class="error-msg" style="margin-bottom:12px;">{{ errorMsg }}</div>

      <button
        class="btn-primary"
        style="margin-top:8px;"
        :disabled="loading || cart.length === 0"
        @click="placeOrder"
      >
        {{ loading ? '주문 처리 중...' : `${formatPrice(totalPrice)}원 주문하기` }}
      </button>

      <button
        class="btn-clear"
        @click="clearCart"
      >
        장바구니 비우기
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const emit = defineEmits(['cart-updated'])
const router = useRouter()

const cart = ref([])
const loading = ref(false)
const errorMsg = ref('')

onMounted(() => {
  loadCart()
})

function loadCart() {
  try {
    cart.value = JSON.parse(localStorage.getItem('cart') || '[]')
  } catch {
    cart.value = []
  }
}

function saveCart() {
  localStorage.setItem('cart', JSON.stringify(cart.value))
  emit('cart-updated')
}

const totalPrice = computed(() => {
  return cart.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

function formatPrice(price) {
  return Number(price).toLocaleString('ko-KR')
}

function changeQty(item, delta) {
  const newQty = item.quantity + delta
  if (newQty <= 0) {
    removeItem(item)
    return
  }
  item.quantity = newQty
  saveCart()
}

function removeItem(item) {
  cart.value = cart.value.filter((i) => i.menuId !== item.menuId)
  saveCart()
}

function clearCart() {
  cart.value = []
  saveCart()
}

async function placeOrder() {
  if (cart.value.length === 0) return
  errorMsg.value = ''
  loading.value = true
  try {
    const items = cart.value.map((i) => ({
      menuId: i.menuId,
      quantity: i.quantity
    }))
    const res = await api.post('/orders', { items })
    const orderId = res.data.id

    // Clear cart after successful order
    clearCart()

    // Redirect to order detail
    router.push(`/orders/${orderId}`)
  } catch (err) {
    errorMsg.value = err.response?.data?.message || '주문 처리 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #222;
  margin-bottom: 20px;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #aaa;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-state p {
  font-size: 16px;
}

.cart-list {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  margin-bottom: 16px;
}

.cart-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
  gap: 12px;
  flex-wrap: wrap;
}

.cart-item:last-child {
  border-bottom: none;
}

.item-info {
  flex: 1;
  min-width: 120px;
}

.item-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #222;
  margin-bottom: 2px;
}

.item-unit-price {
  font-size: 12px;
  color: #999;
}

.item-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.qty-btn {
  width: 28px;
  height: 28px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.qty-btn:hover {
  border-color: #e05c2a;
  color: #e05c2a;
}

.qty-value {
  font-size: 14px;
  font-weight: 600;
  min-width: 24px;
  text-align: center;
}

.item-subtotal {
  font-size: 14px;
  font-weight: 700;
  color: #e05c2a;
  min-width: 80px;
  text-align: right;
}

.btn-remove {
  background: none;
  border: none;
  color: #bbb;
  cursor: pointer;
  font-size: 14px;
  padding: 4px;
  transition: color 0.15s;
}

.btn-remove:hover {
  color: #d32f2f;
}

.cart-summary {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  margin-bottom: 16px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 15px;
}

.summary-total {
  font-size: 20px;
  font-weight: 700;
  color: #e05c2a;
}

.btn-clear {
  width: 100%;
  background: none;
  border: none;
  color: #bbb;
  font-size: 13px;
  cursor: pointer;
  padding: 10px;
  margin-top: 8px;
  transition: color 0.15s;
}

.btn-clear:hover {
  color: #d32f2f;
}
</style>
