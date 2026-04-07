<template>
  <div class="page-container">
    <h1 class="page-title">주문 내역</h1>

    <div v-if="loading" class="loading">불러오는 중...</div>

    <div v-else-if="orders.length === 0" class="empty-state">
      <div class="empty-icon">📋</div>
      <p>주문 내역이 없습니다.</p>
      <router-link to="/" class="btn-secondary" style="display:inline-block; margin-top:16px; width:auto; padding:10px 24px;">
        메뉴 보러가기
      </router-link>
    </div>

    <div v-else class="order-list">
      <div
        v-for="order in orders"
        :key="order.id"
        class="order-card"
        @click="goToDetail(order.id)"
      >
        <div class="order-header">
          <span class="order-id">#{{ order.id }}</span>
          <span class="order-status" :class="statusClass(order.status)">
            {{ statusLabel(order.status) }}
          </span>
        </div>
        <div class="order-meta">
          <span class="order-date">{{ formatDate(order.createdAt) }}</span>
          <span class="order-total">{{ formatPrice(order.totalPrice) }}원</span>
        </div>
        <div v-if="order.items && order.items.length" class="order-items-preview">
          {{ order.items.slice(0, 3).map(i => i.menuName || i.name).join(', ') }}
          <span v-if="order.items.length > 3"> 외 {{ order.items.length - 3 }}개</span>
        </div>
        <div class="order-arrow">›</div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="pagination">
      <button
        class="page-btn"
        :disabled="currentPage === 0"
        @click="loadPage(currentPage - 1)"
      >
        ‹ 이전
      </button>
      <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button
        class="page-btn"
        :disabled="currentPage >= totalPages - 1"
        @click="loadPage(currentPage + 1)"
      >
        다음 ›
      </button>
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
const pageSize = 10

onMounted(() => {
  loadPage(0)
})

async function loadPage(page) {
  loading.value = true
  try {
    const res = await api.get('/orders', { params: { page, size: pageSize } })
    const data = res.data
    // Handle both paged response and plain array
    if (Array.isArray(data)) {
      orders.value = data
      totalPages.value = 1
    } else {
      orders.value = data.content || []
      totalPages.value = data.totalPages || 1
    }
    currentPage.value = page
  } catch (err) {
    console.error('주문 목록 로드 실패', err)
  } finally {
    loading.value = false
  }
}

function goToDetail(id) {
  router.push(`/orders/${id}`)
}

function formatPrice(price) {
  return Number(price).toLocaleString('ko-KR')
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleString('ko-KR', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

function statusLabel(status) {
  const map = {
    PENDING: '주문 접수',
    CONFIRMED: '주문 확인',
    PREPARING: '준비 중',
    READY: '준비 완료',
    DELIVERED: '배달 완료',
    CANCELLED: '취소됨'
  }
  return map[status] || status
}

function statusClass(status) {
  const map = {
    PENDING: 'status-pending',
    CONFIRMED: 'status-confirmed',
    PREPARING: 'status-preparing',
    READY: 'status-ready',
    DELIVERED: 'status-delivered',
    CANCELLED: 'status-cancelled'
  }
  return map[status] || 'status-default'
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

.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  cursor: pointer;
  transition: transform 0.12s, box-shadow 0.12s;
  position: relative;
}

.order-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.10);
}

.order-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.order-id {
  font-size: 13px;
  color: #999;
  font-weight: 600;
}

.order-status {
  font-size: 12px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 12px;
}

.status-pending  { background: #fff3cd; color: #856404; }
.status-confirmed { background: #cce5ff; color: #004085; }
.status-preparing { background: #d4edda; color: #155724; }
.status-ready    { background: #d1ecf1; color: #0c5460; }
.status-delivered { background: #e2e3e5; color: #383d41; }
.status-cancelled { background: #f8d7da; color: #721c24; }
.status-default  { background: #f0f0f0; color: #555; }

.order-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.order-date {
  font-size: 12px;
  color: #aaa;
}

.order-total {
  font-size: 16px;
  font-weight: 700;
  color: #e05c2a;
}

.order-items-preview {
  font-size: 13px;
  color: #666;
  margin-top: 4px;
  padding-right: 24px;
}

.order-arrow {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 22px;
  color: #ccc;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
}

.page-btn {
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.page-btn:hover:not(:disabled) {
  border-color: #e05c2a;
  color: #e05c2a;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: #888;
}
</style>
