<template>
  <div class="orders-wrap">
    <header class="orders-header">
      <button class="back-btn" @click="$router.push('/')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><polyline points="15,18 9,12 15,6"/></svg>
        메뉴로
      </button>
      <div>
        <h1 class="display orders-title">Orders</h1>
        <p class="orders-sub">주문 내역</p>
      </div>
    </header>

    <!-- Loading -->
    <div v-if="loading" class="order-list">
      <div v-for="i in 4" :key="i" class="order-skeleton">
        <div class="skeleton" style="height:14px;width:60px;margin-bottom:10px"></div>
        <div class="skeleton" style="height:12px;width:180px;margin-bottom:8px"></div>
        <div class="skeleton" style="height:18px;width:100px"></div>
      </div>
    </div>

    <!-- Empty -->
    <div v-else-if="orders.length === 0" class="empty">
      <span class="empty-glyph">∅</span>
      <span>주문 내역이 없어요</span>
      <router-link to="/" class="empty-link">메뉴 보러가기 →</router-link>
    </div>

    <!-- Order list -->
    <div v-else class="order-list">
      <div
        v-for="(order, i) in orders" :key="order.id"
        class="order-card fade-up"
        :style="`animation-delay:${i * 0.05}s`"
        @click="goToDetail(order.id)"
      >
        <div class="oc-top">
          <span class="oc-id mono">#{{ String(order.id).padStart(4, '0') }}</span>
          <span class="oc-status" :class="`st-${(order.status||'').toLowerCase()}`">{{ statusLabel(order.status) }}</span>
        </div>

        <div class="oc-date">{{ formatDate(order.createdAt) }}</div>

        <div v-if="order.items?.length" class="oc-items">
          {{ order.items.slice(0, 3).map(i => i.menuName || i.name).join(' · ') }}
          <span v-if="order.items.length > 3" class="oc-more"> 외 {{ order.items.length - 3 }}개</span>
        </div>

        <div class="oc-bottom">
          <span class="oc-price mono">{{ formatPrice(order.totalPrice) }}원</span>
          <svg class="oc-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><polyline points="9,18 15,12 9,6"/></svg>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="paging">
      <button class="pg-btn" :disabled="currentPage === 0" @click="loadPage(currentPage - 1)">‹</button>
      <span class="pg-info mono">{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button class="pg-btn" :disabled="currentPage >= totalPages - 1" @click="loadPage(currentPage + 1)">›</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const orders = ref([])
const loading = ref(false)
const currentPage = ref(0)
const totalPages = ref(1)

onMounted(() => loadPage(0))

async function loadPage(page) {
  loading.value = true
  try {
    const r = await api.get('/orders', { params: { page, size: 10 } })
    const d = r.data
    orders.value = Array.isArray(d) ? d : (d.content || [])
    totalPages.value = d.totalPages || 1
    currentPage.value = page
  } catch {} finally { loading.value = false }
}

function goToDetail(id) { router.push(`/orders/${id}`) }
function formatPrice(p) { return Number(p).toLocaleString('ko-KR') }
function formatDate(s) {
  if (!s) return ''
  return new Date(s).toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const STATUS_MAP = {
  PENDING: '접수 대기', ACCEPTED: '주문 수락', COOKING: '조리 중',
  READY: '준비 완료', COMPLETED: '완료', CANCELLED: '취소됨'
}
function statusLabel(s) { return STATUS_MAP[s] || s || '-' }
</script>

<style scoped>
.orders-wrap {
  max-width: 720px;
  margin: 0 auto;
  padding: 40px 36px 80px;
  min-height: 100vh;
}

.orders-header { margin-bottom: 32px; }
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-muted);
  background: none;
  border: none;
  cursor: pointer;
  font-family: inherit;
  margin-bottom: 10px;
  transition: var(--transition);
}
.back-btn:hover { color: var(--accent-soft); }

.orders-title { font-size: 42px; color: var(--text-primary); margin: 0; }
.orders-sub { font-size: 13px; color: var(--text-muted); margin-top: 2px; }

.order-list { display: flex; flex-direction: column; gap: 12px; }

.order-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px 22px;
  cursor: pointer;
  transition: var(--transition);
}
.order-card:hover {
  border-color: var(--border-hover);
  box-shadow: var(--shadow-glow);
  transform: translateY(-2px);
}

.oc-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.oc-id { font-size: 13px; color: var(--text-muted); font-weight: 600; }

.oc-status {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 12px;
  border-radius: 99px;
  letter-spacing: 0.02em;
}
.st-pending   { background: rgba(251,191,36,0.12); color: #fbbf24; }
.st-accepted  { background: rgba(96,165,250,0.12); color: #60a5fa; }
.st-cooking   { background: rgba(251,146,60,0.12); color: #fb923c; }
.st-ready     { background: rgba(74,222,128,0.12); color: #4ade80; }
.st-completed { background: rgba(156,163,175,0.1); color: #9ca3af; }
.st-cancelled { background: rgba(248,113,113,0.1); color: #f87171; }

.oc-date { font-size: 12px; color: var(--text-muted); margin-bottom: 10px; }

.oc-items {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 14px;
  line-height: 1.4;
}
.oc-more { color: var(--text-muted); font-size: 11px; }

.oc-bottom { display: flex; align-items: center; justify-content: space-between; padding-top: 12px; border-top: 1px solid var(--border); }
.oc-price { font-size: 17px; font-weight: 700; color: var(--accent-soft); }
.oc-arrow { color: var(--text-muted); }

.order-skeleton {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px 22px;
}

.paging { display: flex; align-items: center; justify-content: center; gap: 16px; margin-top: 28px; }
.pg-btn {
  width: 36px; height: 36px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: var(--transition);
}
.pg-btn:hover:not(:disabled) { border-color: var(--accent); color: var(--accent); }
.pg-btn:disabled { opacity: 0.25; cursor: not-allowed; }
.pg-info { font-size: 13px; color: var(--text-muted); }

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  gap: 12px;
  color: var(--text-muted);
}
.empty-glyph { font-size: 48px; opacity: 0.3; font-family: 'Playfair Display', serif; }
.empty-link { font-size: 13px; color: var(--accent-soft); margin-top: 4px; text-decoration: none; transition: var(--transition); }
.empty-link:hover { color: var(--accent); }
</style>
