<template>
  <div class="cart-page page-wrapper">
    <header class="page-header">
      <h1 class="display page-title">CART</h1>
      <span v-if="cart.length" class="item-count">{{ totalItems }}개</span>
    </header>

    <!-- Empty -->
    <div v-if="cart.length === 0" class="empty-state fade-up">
      <span class="empty-icon">🛒</span>
      <span class="empty-label">장바구니가 비어 있어요</span>
      <router-link to="/" class="btn btn-secondary" style="margin-top:4px">메뉴 보러가기</router-link>
    </div>

    <div v-else>
      <!-- Item list -->
      <div class="cart-list fade-up">
        <div v-for="(item, i) in cart" :key="item.menuId" class="cart-item" :style="`animation-delay:${i*0.05}s`">
          <div class="item-left">
            <div class="item-name">{{ item.name }}</div>
            <div class="item-unit">개당 {{ formatPrice(item.price) }}원</div>
          </div>
          <div class="item-right">
            <div class="qty-control">
              <button class="qty-btn" @click="changeQty(item, -1)">−</button>
              <span class="qty-num">{{ item.quantity }}</span>
              <button class="qty-btn" @click="changeQty(item, 1)">+</button>
            </div>
            <span class="item-subtotal">{{ formatPrice(item.price * item.quantity) }}원</span>
            <button class="btn-remove" @click="removeItem(item)" aria-label="삭제">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- Summary -->
      <div class="summary-card fade-up fade-up-2">
        <div class="summary-row">
          <span class="summary-label">상품 합계</span>
          <span class="summary-val">{{ formatPrice(totalPrice) }}원</span>
        </div>
        <div class="divider" style="margin:12px 0"></div>
        <div class="summary-row">
          <span class="summary-total-label">최종 결제금액</span>
          <span class="summary-total">{{ formatPrice(totalPrice) }}<span class="price-unit">원</span></span>
        </div>
      </div>

      <div v-if="errorMsg" class="alert-error fade-up">{{ errorMsg }}</div>

      <!-- CTA -->
      <div class="cta-wrap fade-up fade-up-3">
        <button class="btn btn-primary order-btn" :disabled="loading" @click="placeOrder">
          <span v-if="loading" class="spinner"></span>
          {{ loading ? '주문 처리 중...' : `${formatPrice(totalPrice)}원 주문하기` }}
        </button>
        <button class="btn-clear" @click="clearCart">장바구니 비우기</button>
      </div>
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
  try { cart.value = JSON.parse(localStorage.getItem('cart') || '[]') } catch { cart.value = [] }
})

function saveCart() { localStorage.setItem('cart', JSON.stringify(cart.value)); emit('cart-updated') }

const totalPrice = computed(() => cart.value.reduce((s, i) => s + i.price * i.quantity, 0))
const totalItems = computed(() => cart.value.reduce((s, i) => s + i.quantity, 0))

function formatPrice(p) { return Number(p).toLocaleString('ko-KR') }

function changeQty(item, d) {
  const n = item.quantity + d
  if (n <= 0) { removeItem(item); return }
  item.quantity = n; saveCart()
}
function removeItem(item) { cart.value = cart.value.filter(i => i.menuId !== item.menuId); saveCart() }
function clearCart() { cart.value = []; saveCart() }

async function placeOrder() {
  if (!cart.value.length) return
  errorMsg.value = ''; loading.value = true
  try {
    const res = await api.post('/orders', { items: cart.value.map(i => ({ menuId: i.menuId, quantity: i.quantity })) })
    clearCart()
    router.push(`/orders/${res.data.id}`)
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '주문 처리 중 오류가 발생했습니다.'
  } finally { loading.value = false }
}
</script>

<style scoped>
.cart-page { padding: 24px 16px 100px; }

.page-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;
}
.page-title { font-size: 40px; color: var(--text-primary); }
.item-count {
  background: var(--accent);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 99px;
}

/* Items */
.cart-list {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 12px;
}
.cart-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border);
  gap: 10px;
}
.cart-item:last-child { border-bottom: none; }

.item-left { flex: 1; min-width: 0; }
.item-name { font-size: 14px; font-weight: 600; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.item-unit { font-size: 11px; color: var(--text-muted); margin-top: 2px; }

.item-right { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.item-subtotal { font-size: 13px; font-weight: 700; color: var(--accent-soft); min-width: 64px; text-align: right; }
.btn-remove {
  background: none; border: none;
  color: var(--text-muted);
  display: flex; align-items: center; justify-content: center;
  width: 24px; height: 24px; border-radius: 99px;
  transition: var(--transition);
}
.btn-remove:hover { color: #ef4444; background: rgba(239,68,68,0.1); }

/* Summary */
.summary-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  margin-bottom: 16px;
}
.summary-row { display: flex; justify-content: space-between; align-items: center; }
.summary-label { font-size: 13px; color: var(--text-secondary); }
.summary-val { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.summary-total-label { font-size: 14px; font-weight: 700; color: var(--text-primary); }
.summary-total { font-size: 22px; font-weight: 800; color: var(--accent-soft); }
.price-unit { font-size: 13px; font-weight: 500; margin-left: 1px; }

/* CTA */
.cta-wrap { display: flex; flex-direction: column; gap: 10px; }
.order-btn {
  width: 100%;
  padding: 15px;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.02em;
  display: flex; align-items: center; justify-content: center; gap: 8px;
}
.btn-clear {
  background: none; border: none;
  color: var(--text-muted); font-size: 12px;
  font-family: inherit; padding: 6px;
  transition: var(--transition);
}
.btn-clear:hover { color: #ef4444; }

.spinner {
  width: 16px; height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
