<template>
  <div class="orders-page page-wrapper">
    <header class="page-header">
      <h1 class="display page-title">ORDERS</h1>
    </header>

    <!-- Skeleton -->
    <div v-if="loading" class="order-list">
      <div v-for="i in 4" :key="i" class="order-skeleton">
        <div style="display:flex;justify-content:space-between;margin-bottom:10px">
          <div class="skeleton" style="height:11px;width:50px"></div>
          <div class="skeleton" style="height:18px;width:70px;border-radius:99px"></div>
        </div>
        <div class="skeleton" style="height:11px;width:120px;margin-bottom:8px"></div>
        <div style="display:flex;justify-content:space-between">
          <div class="skeleton" style="height:11px;width:160px"></div>
          <div class="skeleton" style="height:16px;width:80px"></div>
        </div>
      </div>
    </div>

    <div v-else-if="orders.length === 0" class="empty-state fade-up">
      <span class="empty-icon">📋</span>
      <span class="empty-label">주문 내역이 없어요</span>
      <router-link to="/" class="btn btn-secondary" style="margin-top:4px">메뉴 보러가기</router-link>
    </div>

    <div v-else class="order-list">
      <div
        v-for="(order, i) in orders"
        :key="order.id"
        class="order-card fade-up"
        :style="`animation-delay:${i * 0.06}s`"
        @click="goToDetail(order.id)"
      >
        <div class="order-top">
          <span class="order-id">#{{ String(order.id).padStart(4, '0') }}</span>
          <span class="status-pill" :class="`status-${(order.status||'').toLowerCase()}`">
            {{ statusLabel(order.status) }}
          </span>
        </div>

        <div class="order-date">{{ formatDate(order.createdAt) }}</div>

        <div v-if="order.items?.length" class="order-preview">
          {{ order.items.slice(0, 3).map(i => i.menuName || i.name).join(' · ') }}
          <span v-if="order.items.length > 3" class="more-items">외 {{ order.items.length - 3 }}개</span>
        </div>

        <div class="order-bottom">
          <span class="order-price">{{ formatPrice(order.totalPrice) }}원</span>
          <span class="order-arrow">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <polyline points="9,18 15,12 9,6"/>
            </svg>
          </span>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="pagination">
      <button class="btn btn-secondary page-btn" :disabled="currentPage === 0" @click="loadPage(currentPage - 1)">‹</button>
      <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button class="btn btn-secondary page-btn" :disabled="currentPage >= totalPages - 1" @click="loadPage(currentPage + 1)">›</button>
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
.orders-page { padding: 24px 16px 100px; }

.page-header { margin-bottom: 20px; }
.page-title { font-size: 40px; color: var(--text-primary); }

.order-list { display: flex; flex-direction: column; gap: 10px; }

.order-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 16px 18px;
  cursor: pointer;
  transition: var(--transition);
  position: relative;
}
.order-card:hover {
  border-color: var(--border-hover);
  box-shadow: 0 4px 20px rgba(232,92,30,0.12);
  transform: translateY(-1px);
}

.order-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 7px;
}
.order-id { font-size: 12px; color: var(--text-muted); font-weight: 600; letter-spacing: 0.06em; }

.status-pill {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 99px;
  letter-spacing: 0.02em;
}
.status-pending  { background: rgba(251,191,36,0.15); color: #fbbf24; }
.status-accepted { background: rgba(59,130,246,0.15); color: #60a5fa; }
.status-cooking  { background: rgba(249,115,22,0.15); color: #fb923c; }
.status-ready    { background: rgba(34,197,94,0.15);  color: #4ade80; }
.status-completed{ background: rgba(107,114,128,0.15);color: #9ca3af; }
.status-cancelled{ background: rgba(239,68,68,0.15);  color: #f87171; }

.order-date { font-size: 11px; color: var(--text-muted); margin-bottom: 8px; }

.order-preview {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 12px;
  line-height: 1.4;
}
.more-items { color: var(--text-muted); font-size: 11px; }

.order-bottom { display: flex; align-items: center; justify-content: space-between; }
.order-price { font-size: 16px; font-weight: 800; color: var(--accent-soft); }
.order-arrow { color: var(--text-muted); display: flex; }

/* Skeleton cards */
.order-skeleton {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 16px 18px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 24px;
}
.page-btn { padding: 8px 16px; font-weight: 700; }
.page-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.page-info { font-size: 13px; color: var(--text-muted); }
</style>
