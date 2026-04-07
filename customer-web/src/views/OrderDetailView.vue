<template>
  <div class="page">
    <div class="page-header">
      <button class="btn-back" @click="$router.back()">← 뒤로</button>
      <h2>주문 상세</h2>
    </div>

    <div v-if="loading" class="loading">불러오는 중...</div>

    <div v-else-if="order" class="order-card">
      <div class="order-status-badge" :class="statusClass">
        {{ statusLabel }}
      </div>

      <div class="order-info">
        <div class="info-row">
          <span class="label">주문 번호</span>
          <span>#{{ order.id }}</span>
        </div>
        <div class="info-row">
          <span class="label">주문 시각</span>
          <span>{{ formatDate(order.createdAt) }}</span>
        </div>
        <div class="info-row">
          <span class="label">총 금액</span>
          <span class="price">{{ order.totalPrice?.toLocaleString() }}원</span>
        </div>
      </div>

      <div class="order-items">
        <h3>주문 항목</h3>
        <div v-for="item in order.items" :key="item.id" class="item-row">
          <span class="item-name">{{ item.menuName }}</span>
          <span class="item-qty">× {{ item.quantity }}</span>
          <span class="item-price">{{ (item.price * item.quantity).toLocaleString() }}원</span>
        </div>
      </div>

      <div v-if="wsMessage" class="ws-notify">
        {{ wsMessage }}
      </div>
    </div>

    <div v-else class="empty">주문 정보를 찾을 수 없습니다.</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'

const route = useRoute()
const order = ref(null)
const loading = ref(true)
const wsMessage = ref('')
let ws = null

const STATUS_LABELS = {
  PENDING: '접수 대기',
  ACCEPTED: '주문 수락',
  COOKING: '조리 중',
  READY: '준비 완료',
  COMPLETED: '완료',
  CANCELLED: '취소됨',
}

const statusLabel = computed(() => STATUS_LABELS[order.value?.status] || order.value?.status)
const statusClass = computed(() => `status-${(order.value?.status || '').toLowerCase()}`)

async function loadOrder() {
  try {
    const res = await api.get(`/orders/${route.params.id}`)
    order.value = res.data
  } finally {
    loading.value = false
  }
}

function connectWs() {
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  const url = `${protocol}://${location.host}/ws/orders/${route.params.id}`
  ws = new WebSocket(url)

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.status) {
        order.value.status = data.status
        wsMessage.value = data.message || `상태 변경: ${STATUS_LABELS[data.status] || data.status}`
        setTimeout(() => { wsMessage.value = '' }, 5000)
      }
    } catch {}
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('ko-KR')
}

onMounted(async () => {
  await loadOrder()
  connectWs()
})

onUnmounted(() => {
  if (ws) ws.close()
})
</script>

<style scoped>
.page { max-width: 600px; margin: 0 auto; padding: 24px 16px; }
.page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
.page-header h2 { margin: 0; font-size: 20px; }
.btn-back { background: none; border: none; font-size: 14px; color: #e05c2a; cursor: pointer; }

.order-card { background: #fff; border-radius: 12px; padding: 24px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }

.order-status-badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 20px;
  font-weight: 700;
  font-size: 14px;
  margin-bottom: 20px;
}
.status-pending { background: #fff3cd; color: #856404; }
.status-accepted { background: #cfe2ff; color: #084298; }
.status-cooking { background: #fff3cd; color: #664d03; }
.status-ready { background: #d1e7dd; color: #0a3622; }
.status-completed { background: #d1e7dd; color: #0a3622; }
.status-cancelled { background: #f8d7da; color: #842029; }

.order-info { margin-bottom: 20px; }
.info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f0f0f0; font-size: 14px; }
.label { color: #888; }
.price { font-weight: 700; color: #e05c2a; }

.order-items h3 { font-size: 15px; margin-bottom: 12px; }
.item-row { display: flex; gap: 8px; align-items: center; padding: 8px 0; border-bottom: 1px solid #f8f8f8; font-size: 14px; }
.item-name { flex: 1; }
.item-qty { color: #888; }
.item-price { font-weight: 600; }

.ws-notify { margin-top: 16px; padding: 12px; background: #e8f5e9; border-radius: 8px; color: #2e7d32; font-size: 14px; text-align: center; }
.loading, .empty { text-align: center; padding: 48px; color: #aaa; }
</style>
