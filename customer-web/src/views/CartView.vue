<template>
  <div class="cart-page page-wrapper">
    <header class="cart-header">
      <h1 class="display cart-title">Cart</h1>
      <span v-if="cart.length" class="cart-count">{{ totalItems }}개</span>
    </header>

    <!-- Empty -->
    <div v-if="cart.length === 0" class="cart-empty fade-up">
      <div class="cart-empty-icon">🛒</div>
      <span class="cart-empty-label">장바구니가 비어 있어요</span>
      <router-link to="/" class="cart-empty-link">메뉴 보러가기 →</router-link>
    </div>

    <div v-else>
      <!-- Item list -->
      <div class="cart-list fade-up">
        <div v-for="(item, i) in cart" :key="item.menuId" class="ci" :style="`animation-delay:${i*0.04}s`">
          <img v-if="item.imageUrl" :src="item.imageUrl" alt="" class="ci-img" @error="e => e.target.style.display='none'" />
          <div v-else class="ci-img ci-img-ph">{{ item.name?.charAt(0) || '?' }}</div>
          <div class="ci-info">
            <div class="ci-name">{{ item.name }}</div>
            <div class="ci-unit mono">{{ formatPrice(item.price) }}원</div>
          </div>
          <div class="ci-qty">
            <button class="ci-qty-btn" @click="changeQty(item, -1)">
              <svg width="12" height="12" viewBox="0 0 12 12"><path d="M3 6h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
            </button>
            <span class="ci-qty-num">{{ item.quantity }}</span>
            <button class="ci-qty-btn" @click="changeQty(item, 1)">
              <svg width="12" height="12" viewBox="0 0 12 12"><path d="M6 3v6M3 6h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
            </button>
          </div>
          <span class="ci-subtotal mono">{{ formatPrice(item.price * item.quantity) }}원</span>
          <button class="ci-remove" @click="removeItem(item)" aria-label="삭제">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M4 4l6 6m0-6-6 6" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
          </button>
        </div>
      </div>

      <!-- Summary -->
      <div class="cart-summary fade-up" style="animation-delay:0.1s">
        <div class="cs-row">
          <span class="cs-label">총 금액</span>
          <span class="cs-total mono">{{ formatPrice(totalPrice) }}<small>원</small></span>
        </div>
      </div>

      <div v-if="errorMsg" class="alert-error fade-up">{{ errorMsg }}</div>

      <!-- CTA -->
      <div class="cart-cta fade-up" style="animation-delay:0.15s">
        <button class="cart-order-btn" :disabled="loading" @click="placeOrder">
          <span v-if="loading" class="spinner"></span>
          <template v-else>
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M3 3h1.5l1 9h8l1.5-6H5.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="7" cy="15" r="1" fill="currentColor"/><circle cx="13" cy="15" r="1" fill="currentColor"/></svg>
            {{ formatPrice(totalPrice) }}원 주문하기
          </template>
        </button>
        <button class="cart-clear-btn" @click="clearCart">장바구니 비우기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { useFormatting } from '../composables/useFormatting'
import { useCart } from '../composables/useCart'

const emit = defineEmits(['cart-updated'])
const router = useRouter()
const { formatPrice } = useFormatting()
const { cartItems: cart, changeQty, removeItem, clear: clearCart, totalQty: totalItems, totalPrice } = useCart()
const loading = ref(false)
const errorMsg = ref('')

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

/* ── Header ── */
.cart-header { display: flex; align-items: baseline; gap: 10px; margin-bottom: 20px; }
.cart-title { font-size: 36px; color: var(--text-primary); }
.cart-count {
  background: var(--accent); color: var(--text-inverse);
  font-size: 12px; font-weight: 700;
  padding: 2px 9px; border-radius: 99px;
}

/* ── Empty ── */
.cart-empty {
  display: flex; flex-direction: column; align-items: center;
  padding: 60px 0; gap: 8px;
}
.cart-empty-icon { font-size: 40px; opacity: 0.25; margin-bottom: 4px; }
.cart-empty-label { font-size: 14px; color: var(--text-muted); }
.cart-empty-link {
  font-size: 13px; font-weight: 600; color: var(--accent);
  text-decoration: none; margin-top: 4px;
  transition: color 0.2s;
}
.cart-empty-link:hover { color: var(--accent-soft); }

/* ── Cart items ── */
.cart-list {
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius-lg); overflow: hidden;
  margin-bottom: 12px;
}

.ci {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  transition: background 0.15s;
}
.ci:last-child { border-bottom: none; }
.ci:hover { background: rgba(255,255,255,0.01); }

.ci-img {
  width: 48px; height: 48px; border-radius: 10px;
  object-fit: cover; flex-shrink: 0;
  border: 1px solid var(--border);
}
.ci-img-ph {
  background: var(--bg-subtle);
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; font-weight: 700; color: var(--text-muted);
}

.ci-info { flex: 1; min-width: 0; }
.ci-name { font-size: 14px; font-weight: 600; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ci-unit { font-size: 12px; color: var(--text-muted); margin-top: 2px; }

.ci-qty {
  display: flex; align-items: center;
  background: var(--bg-subtle); border: 1px solid var(--border);
  border-radius: 8px; overflow: hidden; flex-shrink: 0;
}
.ci-qty-btn {
  width: 30px; height: 28px;
  display: flex; align-items: center; justify-content: center;
  background: transparent; border: none;
  color: var(--text-secondary); cursor: pointer;
  transition: all 0.15s;
}
.ci-qty-btn:hover { background: var(--accent-dim); color: var(--accent); }
.ci-qty-num {
  width: 26px; text-align: center;
  font-size: 13px; font-weight: 700; color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

.ci-subtotal {
  font-size: 14px; font-weight: 700; color: var(--accent-soft);
  min-width: 68px; text-align: right; flex-shrink: 0;
}

.ci-remove {
  width: 28px; height: 28px; border-radius: 99px;
  background: none; border: 1px solid transparent;
  color: var(--text-muted); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s; flex-shrink: 0;
}
.ci-remove:hover { color: #f87171; border-color: rgba(248,113,113,0.2); background: rgba(248,113,113,0.06); }

/* ── Summary ── */
.cart-summary {
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 18px 20px; margin-bottom: 16px;
}
.cs-row { display: flex; justify-content: space-between; align-items: baseline; }
.cs-label { font-size: 14px; font-weight: 600; color: var(--text-secondary); }
.cs-total { font-size: 22px; font-weight: 800; color: var(--accent-soft); letter-spacing: -0.5px; }
.cs-total small { font-size: 14px; font-weight: 500; color: var(--text-muted); margin-left: 1px; }

/* ── CTA ── */
.cart-cta { display: flex; flex-direction: column; gap: 10px; }

.cart-order-btn {
  width: 100%; padding: 15px;
  background: var(--accent); color: #fff;
  border: none; border-radius: var(--radius-md);
  font-size: 15px; font-weight: 700; font-family: inherit;
  cursor: pointer; transition: all 0.2s;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  box-shadow: 0 2px 16px rgba(212,134,60,0.25);
}
.cart-order-btn:hover:not(:disabled) { background: var(--accent-soft); box-shadow: 0 4px 24px rgba(212,134,60,0.4); transform: translateY(-1px); }
.cart-order-btn:active:not(:disabled) { transform: translateY(0); }
.cart-order-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none; box-shadow: none; }

.cart-clear-btn {
  background: none; border: none;
  color: var(--text-muted); font-size: 12px;
  font-family: inherit; padding: 6px;
  cursor: pointer; transition: color 0.15s;
  text-align: center;
}
.cart-clear-btn:hover { color: #f87171; }

.spinner {
  width: 16px; height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: var(--text-inverse);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
