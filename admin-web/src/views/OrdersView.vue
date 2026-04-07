<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">주문 관리</h1>
      <button class="btn btn-secondary" @click="fetchOrders">새로고침</button>
    </div>

    <!-- Status filter tabs -->
    <div class="filter-tabs">
      <button
        v-for="s in statusOptions"
        :key="s.value"
        class="filter-tab"
        :class="{ active: selectedStatus === s.value }"
        @click="selectStatus(s.value)"
      >
        {{ s.label }}
        <span v-if="s.value === '' && totalElements > 0" class="tab-count">{{ totalElements }}</span>
      </button>
    </div>

    <div v-if="error" class="alert-box alert-error">{{ error }}</div>

    <div class="card table-wrap">
      <table>
        <thead>
          <tr>
            <th>주문 번호</th>
            <th>고객 ID</th>
            <th>총 금액</th>
            <th>메뉴</th>
            <th>상태</th>
            <th>주문 시각</th>
            <th>상태 변경</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7" class="loading-cell">불러오는 중...</td>
          </tr>
          <tr v-else-if="orders.length === 0">
            <td colspan="7" class="empty-cell">주문이 없습니다.</td>
          </tr>
          <tr v-for="order in orders" :key="order.orderId || order.id">
            <td class="order-id">#{{ order.orderId || order.id }}</td>
            <td>{{ order.userId }}</td>
            <td class="price">{{ formatPrice(order.totalPrice) }}</td>
            <td class="menu-list">
              <span v-if="order.items && order.items.length">
                {{ order.items.map(i => `${i.menuName} ×${i.quantity}`).join(', ') }}
              </span>
              <span v-else class="muted">-</span>
            </td>
            <td>
              <span class="tag" :class="statusClass(order.status)">{{ statusLabel(order.status) }}</span>
            </td>
            <td class="muted">{{ formatDate(order.createdAt || order.orderTime) }}</td>
            <td>
              <select
                class="status-select"
                :value="order.status"
                :disabled="order.status === 'COMPLETED' || order.status === 'CANCELLED' || updatingId === (order.orderId || order.id)"
                @change="(e) => changeStatus(order, e.target.value)"
              >
                <option v-for="opt in nextStatuses(order.status)" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </option>
              </select>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="totalPages > 1">
      <button class="btn btn-secondary btn-sm" :disabled="page === 0" @click="changePage(page - 1)">이전</button>
      <span class="page-info">{{ page + 1 }} / {{ totalPages }}</span>
      <button class="btn btn-secondary btn-sm" :disabled="page >= totalPages - 1" @click="changePage(page + 1)">다음</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOrders, updateOrderStatus } from '../api.js'

const orders = ref([])
const loading = ref(false)
const error = ref('')
const selectedStatus = ref('')
const page = ref(0)
const totalPages = ref(1)
const totalElements = ref(0)
const updatingId = ref(null)

const statusOptions = [
  { value: '', label: '전체' },
  { value: 'PENDING', label: '대기 중' },
  { value: 'ACCEPTED', label: '접수됨' },
  { value: 'COOKING', label: '조리 중' },
  { value: 'READY', label: '준비 완료' },
  { value: 'COMPLETED', label: '완료' },
  { value: 'CANCELLED', label: '취소됨' },
]

// status → next possible statuses (including current for display)
const statusFlow = {
  PENDING:   ['PENDING', 'ACCEPTED', 'CANCELLED'],
  ACCEPTED:  ['ACCEPTED', 'COOKING', 'CANCELLED'],
  COOKING:   ['COOKING', 'READY', 'CANCELLED'],
  READY:     ['READY', 'COMPLETED', 'CANCELLED'],
  COMPLETED: ['COMPLETED'],
  CANCELLED: ['CANCELLED'],
}

function nextStatuses(current) {
  const keys = statusFlow[current] || [current]
  return keys.map((v) => ({ value: v, label: statusLabel(v) }))
}

function statusLabel(s) {
  const map = { PENDING: '대기 중', ACCEPTED: '접수됨', COOKING: '조리 중', READY: '준비 완료', COMPLETED: '완료', CANCELLED: '취소됨' }
  return map[s] || s
}

function statusClass(s) {
  const map = {
    PENDING: 'tag-yellow',
    ACCEPTED: 'tag-blue',
    COOKING: 'tag-orange',
    READY: 'tag-green',
    COMPLETED: 'tag-gray',
    CANCELLED: 'tag-red',
  }
  return map[s] || 'tag-gray'
}

function formatPrice(v) {
  if (v == null) return '-'
  return Number(v).toLocaleString('ko-KR') + '원'
}

function formatDate(v) {
  if (!v) return '-'
  const d = new Date(v)
  return d.toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function fetchOrders() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: 20 }
    if (selectedStatus.value) params.status = selectedStatus.value
    const res = await getOrders(params)
    const data = res.data
    // Handle both paged response and plain array
    if (Array.isArray(data)) {
      orders.value = data
      totalPages.value = 1
      totalElements.value = data.length
    } else {
      orders.value = data.content || data.orders || []
      totalPages.value = data.totalPages || 1
      totalElements.value = data.totalElements || orders.value.length
    }
  } catch (e) {
    error.value = '주문 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function selectStatus(s) {
  selectedStatus.value = s
  page.value = 0
  fetchOrders()
}

function changePage(p) {
  page.value = p
  fetchOrders()
}

async function changeStatus(order, newStatus) {
  const id = order.orderId || order.id
  if (newStatus === order.status) return
  updatingId.value = id
  try {
    await updateOrderStatus(id, newStatus)
    order.status = newStatus
  } catch {
    error.value = `주문 #${id} 상태 변경에 실패했습니다.`
  } finally {
    updatingId.value = null
  }
}

onMounted(fetchOrders)
</script>

<style scoped>
.filter-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-tab {
  padding: 7px 14px;
  border-radius: 20px;
  border: 1px solid #e5e7eb;
  background: #fff;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-tab:hover { border-color: #2563eb; color: #2563eb; }
.filter-tab.active { background: #2563eb; border-color: #2563eb; color: #fff; }

.tab-count {
  background: rgba(255,255,255,.25);
  padding: 1px 6px;
  border-radius: 10px;
  font-size: 11px;
}
.filter-tab:not(.active) .tab-count { background: #f3f4f6; color: #374151; }

.loading-cell, .empty-cell {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
  font-size: 14px;
}

.order-id { font-family: monospace; font-weight: 600; color: #2563eb; }
.price { font-weight: 600; }
.menu-list { max-width: 200px; font-size: 13px; }
.muted { color: #9ca3af; font-size: 13px; }

.status-select {
  padding: 5px 8px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  background: #fff;
  outline: none;
}

.status-select:focus { border-color: #2563eb; }
.status-select:disabled { opacity: 0.5; cursor: not-allowed; }

/* Status tag colors */
.tag-yellow { background: #fef9c3; color: #854d0e; }
.tag-blue   { background: #dbeafe; color: #1e40af; }
.tag-orange { background: #ffedd5; color: #9a3412; }
.tag-green  { background: #dcfce7; color: #166534; }
.tag-gray   { background: #f3f4f6; color: #6b7280; }
.tag-red    { background: #fee2e2; color: #991b1b; }

.pagination {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: center;
  margin-top: 24px;
}

.page-info { font-size: 14px; color: #6b7280; }
</style>
