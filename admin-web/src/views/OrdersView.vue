<template>
  <div class="page orders-page">
    <div class="page-header">
      <h1 class="page-title">주문 관리</h1>
    </div>

    <!-- Main Tabs -->
    <div class="tab-bar">
      <button class="tab" :class="{ active: activeTab === 'pending' }" @click="switchTab('pending')">
        <span class="tab-inner">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="6" stroke="currentColor" stroke-width="1.4"/><path d="M8 5V8.5L10.5 10" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
          신규 주문
        </span>
        <transition name="badge-pop">
          <span v-if="pendingOrders.length" class="tab-badge" :key="pendingOrders.length">{{ pendingOrders.length }}</span>
        </transition>
      </button>
      <button class="tab" :class="{ active: activeTab === 'list' }" @click="switchTab('list')">
        <span class="tab-inner">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M2 4H14M2 8H14M2 12H9" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
          주문 목록
        </span>
      </button>
      <div class="tab-indicator" :style="indicatorStyle"></div>
    </div>

    <div v-if="error" class="alert-box alert-error">{{ error }}</div>

    <!-- ═══════════════════════════════════════════
         TAB 1 — 신규 주문 (Split Pane)
         ═══════════════════════════════════════════ -->
    <transition name="tab-fade" mode="out-in">
      <div v-if="activeTab === 'pending'" key="pending" class="split-pane">
        <!-- Left: Queue -->
        <section class="queue-panel">
          <div class="panel-header">
            <div class="ph-left">
              <span class="ph-label">대기열</span>
              <span class="ph-count" v-if="pendingOrders.length">{{ pendingOrders.length }}건</span>
            </div>
            <button class="icon-btn" @click="fetchPending" title="새로고침">
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                <path d="M1.5 7a5.5 5.5 0 1 1 1 3.2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
                <path d="M1 10l.5-3 3 1" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>

          <div v-if="pendingLoading" class="panel-empty"><div class="spinner"></div></div>
          <div v-else-if="!pendingOrders.length" class="panel-empty">
            <span class="empty-icon">✓</span>
            <span class="empty-text">대기 중인 주문이 없습니다</span>
          </div>
          <div v-else class="queue-scroll">
            <button
              v-for="(o, i) in pendingOrders" :key="o.orderId || o.id"
              class="queue-card"
              :class="{ selected: isSelected(o) }"
              :style="{ animationDelay: `${i * 40}ms` }"
              @click="selectPending(o)"
            >
              <div class="qc-row">
                <span class="qc-id">#{{ String(o.orderId || o.id).padStart(4, '0') }}</span>
                <span class="qc-elapsed">{{ elapsed(o.createdAt || o.orderTime) }}</span>
              </div>
              <div class="qc-items">{{ summarize(o.items) }}</div>
              <div class="qc-row">
                <span class="qc-price">{{ formatPrice(o.totalPrice) }}</span>
                <span class="qc-count">{{ (o.items || []).length }}개 메뉴</span>
              </div>
            </button>
          </div>
        </section>

        <!-- Right: Detail -->
        <section class="detail-panel">
          <transition name="detail-slide" mode="out-in">
            <div v-if="selectedPending" :key="selectedPending.orderId || selectedPending.id" class="detail-content">
              <div class="panel-header">
                <div class="ph-left">
                  <span class="ph-label">주문 상세</span>
                  <span class="detail-badge">#{{ String(selectedPending.orderId || selectedPending.id).padStart(4, '0') }}</span>
                </div>
                <span class="detail-time">{{ formatDateFull(selectedPending.createdAt || selectedPending.orderTime) }}</span>
              </div>

              <!-- Meta -->
              <div class="detail-meta">
                <div class="meta-chip">
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><circle cx="7" cy="5" r="2.5" stroke="currentColor" stroke-width="1.2"/><path d="M2.5 12.5a4.5 4.5 0 0 1 9 0" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
                  고객 {{ selectedPending.userId }}
                </div>
                <div class="meta-chip accent">
                  {{ formatPrice(selectedPending.totalPrice) }}
                </div>
              </div>

              <!-- Items -->
              <div class="detail-items">
                <div class="di-header">주문 항목</div>
                <div v-for="item in (selectedPending.items || [])" :key="item.id" class="di-row">
                  <div class="di-left">
                    <span class="di-name">{{ item.menuName }}</span>
                  </div>
                  <span class="di-qty">{{ item.quantity }}개</span>
                  <span class="di-price">{{ formatPrice(item.price * item.quantity) }}</span>
                </div>
              </div>

              <!-- Actions -->
              <div class="detail-actions">
                <button class="act-btn act-reject" :disabled="!!actionLoading" @click="rejectPending">
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M4.5 4.5l7 7m0-7-7 7" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
                  {{ actionLoading === 'reject' ? '처리 중...' : '주문 취소' }}
                </button>
                <button class="act-btn act-accept" :disabled="!!actionLoading" @click="acceptPending">
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M3.5 8.5 6 11l6.5-6.5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>
                  {{ actionLoading === 'accept' ? '처리 중...' : '주문 확인' }}
                </button>
              </div>
            </div>

            <div v-else key="empty" class="detail-empty">
              <div class="de-visual">
                <svg width="48" height="48" viewBox="0 0 48 48" fill="none" opacity="0.2">
                  <rect x="8" y="6" width="32" height="36" rx="4" stroke="currentColor" stroke-width="2"/>
                  <path d="M16 16h16M16 24h10M16 32h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
              </div>
              <span class="de-text">주문을 선택하세요</span>
              <span class="de-sub">왼쪽 대기열에서 주문을 클릭하면 상세 정보가 표시됩니다</span>
            </div>
          </transition>
        </section>
      </div>

      <!-- ═══════════════════════════════════════════
           TAB 2 — 주문 목록
           ═══════════════════════════════════════════ -->
      <div v-else key="list" class="list-view">
        <div class="list-toolbar">
          <div class="filter-chips">
            <button
              v-for="s in statusOptions" :key="s.value"
              class="chip" :class="{ active: selectedStatus === s.value, [s.cls]: true }"
              @click="selectStatus(s.value)"
            >
              <span v-if="s.dot" class="chip-dot"></span>
              {{ s.label }}
            </button>
          </div>
          <div class="date-range-wrap" ref="dateRangeRef">
            <button class="date-range-btn" @click="showDatePicker = !showDatePicker">
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><rect x="1.5" y="2.5" width="11" height="10" rx="1.5" stroke="currentColor" stroke-width="1.2"/><path d="M1.5 5.5h11M4.5 1v2.5M9.5 1v2.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
              {{ dateRangeLabel }}
            </button>
            <div v-if="showDatePicker" class="date-dropdown">
              <div class="dd-presets">
                <button class="dd-preset" :class="{ active: datePreset === 'today' }" @click="setPreset('today')">오늘</button>
                <button class="dd-preset" :class="{ active: datePreset === '3d' }" @click="setPreset('3d')">3일</button>
                <button class="dd-preset" :class="{ active: datePreset === '7d' }" @click="setPreset('7d')">7일</button>
                <button class="dd-preset" :class="{ active: datePreset === '30d' }" @click="setPreset('30d')">30일</button>
                <button class="dd-preset" :class="{ active: datePreset === 'custom' }" @click="datePreset = 'custom'">직접 설정</button>
              </div>
              <div v-if="datePreset === 'custom'" class="dd-custom">
                <label class="dd-label">시작일<input type="date" class="dd-input" v-model="dateFrom" /></label>
                <span class="dd-sep">~</span>
                <label class="dd-label">종료일<input type="date" class="dd-input" v-model="dateTo" /></label>
                <button class="dd-apply" @click="applyCustomDate">적용</button>
              </div>
            </div>
          </div>
        </div>

        <div class="card table-wrap">
          <table>
            <thead>
              <tr>
                <th style="width:80px">번호</th>
                <th>메뉴</th>
                <th style="width:100px">금액</th>
                <th style="width:100px">상태</th>
                <th style="width:120px">시각</th>
                <th style="width:140px">관리</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="6" class="state-cell"><div class="spinner"></div> 불러오는 중...</td>
              </tr>
              <tr v-else-if="!orders.length">
                <td colspan="6" class="state-cell">주문이 없습니다</td>
              </tr>
              <tr v-for="(order, idx) in orders" :key="order.orderId || order.id">
                <td class="cell-id">#{{ dailyNumber(order, idx) }}</td>
                <td class="cell-menu">{{ summarize(order.items) }}</td>
                <td class="cell-price">{{ formatPrice(order.totalPrice) }}</td>
                <td>
                  <span class="status-pill" :class="statusClass(order.status)">{{ statusLabel(order.status) }}</span>
                </td>
                <td class="cell-time">{{ formatDate(order.createdAt || order.orderTime) }}</td>
                <td>
                  <div class="row-actions">
                    <button
                      v-if="nextStatus(order.status)"
                      class="flow-btn"
                      :class="flowBtnClass(order.status)"
                      :disabled="updatingId === (order.orderId || order.id)"
                      @click="changeStatus(order, nextStatus(order.status))"
                    >{{ nextLabel(order.status) }}</button>
                    <button
                      v-if="canCancel(order.status)"
                      class="icon-btn icon-btn-danger"
                      :disabled="updatingId === (order.orderId || order.id)"
                      @click="cancelOrder(order)"
                      title="취소"
                    >
                      <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M4 4l6 6m0-6-6 6" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
                    </button>
                    <span v-if="!nextStatus(order.status) && !canCancel(order.status)" class="cell-done">—</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pagination" v-if="totalPages > 1">
          <button class="btn btn-secondary btn-sm" :disabled="page === 0" @click="changePage(page - 1)">이전</button>
          <span class="page-info">{{ page + 1 }} / {{ totalPages }}</span>
          <button class="btn btn-secondary btn-sm" :disabled="page >= totalPages - 1" @click="changePage(page + 1)">다음</button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { getOrders, updateOrderStatus } from '../api.js'
import { formatPrice, formatDate, formatDateFull, timeAgo as elapsed, summarizeItems as summarize, extractListData } from '../utils/formatters.js'
import { STATUS_MAP, statusLabel, statusClass, nextStatus, nextStatusLabel as nextLabel, flowBtnClass, canCancel } from '../utils/status.js'
import { useAsync } from '../composables/useAsync.js'

const activeTab = ref('pending')
const { error, run: runAsync } = useAsync()

function switchTab(t) { activeTab.value = t }

const indicatorStyle = computed(() => {
  const idx = activeTab.value === 'pending' ? 0 : 1
  return { transform: `translateX(${idx * 100}%)` }
})

// ── Pending ──
const pendingOrders = ref([])
const pendingLoading = ref(false)
const selectedPending = ref(null)
const actionLoading = ref(null)
let pendingTimer = null

function isSelected(o) {
  if (!selectedPending.value) return false
  return (selectedPending.value.orderId || selectedPending.value.id) === (o.orderId || o.id)
}

async function fetchPending() {
  pendingLoading.value = true
  try {
    const { list } = extractListData(await getOrders({ status: 'PENDING', size: 50 }))
    pendingOrders.value = list
    if (selectedPending.value) {
      const sid = selectedPending.value.orderId || selectedPending.value.id
      if (!list.find(o => (o.orderId || o.id) === sid)) selectedPending.value = null
    }
  } catch {} finally { pendingLoading.value = false }
}

function selectPending(o) { selectedPending.value = o }

async function acceptPending() {
  if (!selectedPending.value) return
  const id = selectedPending.value.orderId || selectedPending.value.id
  actionLoading.value = 'accept'
  try {
    await runAsync(async () => {
      await updateOrderStatus(id, 'ACCEPTED')
      pendingOrders.value = pendingOrders.value.filter(o => (o.orderId || o.id) !== id)
      selectedPending.value = null
    }, `주문 #${id} 확인 실패`)
  } catch {} finally { actionLoading.value = null }
}

async function rejectPending() {
  if (!selectedPending.value) return
  const id = selectedPending.value.orderId || selectedPending.value.id
  if (!confirm(`주문 #${id}을(를) 취소하시겠습니까?`)) return
  actionLoading.value = 'reject'
  try {
    await runAsync(async () => {
      await updateOrderStatus(id, 'CANCELLED')
      pendingOrders.value = pendingOrders.value.filter(o => (o.orderId || o.id) !== id)
      selectedPending.value = null
    }, `주문 #${id} 취소 실패`)
  } catch {} finally { actionLoading.value = null }
}

// ── List ──
const orders = ref([])
const loading = ref(false)
const selectedStatus = ref('')
const page = ref(0)
const totalPages = ref(1)
const updatingId = ref(null)

// Date range
const today = () => new Date().toISOString().substring(0, 10)
const dateFrom = ref(today())
const dateTo = ref(today())
const datePreset = ref('today')
const showDatePicker = ref(false)
const dateRangeRef = ref(null)

const dateRangeLabel = computed(() => {
  if (dateFrom.value === dateTo.value) return dateFrom.value
  return `${dateFrom.value} ~ ${dateTo.value}`
})

function setPreset(p) {
  datePreset.value = p
  const t = new Date()
  dateTo.value = today()
  if (p === 'today') dateFrom.value = today()
  else if (p === '3d') { t.setDate(t.getDate() - 2); dateFrom.value = t.toISOString().substring(0, 10) }
  else if (p === '7d') { t.setDate(t.getDate() - 6); dateFrom.value = t.toISOString().substring(0, 10) }
  else if (p === '30d') { t.setDate(t.getDate() - 29); dateFrom.value = t.toISOString().substring(0, 10) }
  if (p !== 'custom') { showDatePicker.value = false; fetchOrders() }
}

function applyCustomDate() {
  if (dateFrom.value > dateTo.value) { const tmp = dateFrom.value; dateFrom.value = dateTo.value; dateTo.value = tmp }
  showDatePicker.value = false
  fetchOrders()
}

// Close date picker on outside click
function onClickOutside(e) {
  if (dateRangeRef.value && !dateRangeRef.value.contains(e.target)) showDatePicker.value = false
}

const statusOptions = [
  { value: '', label: '전체', cls: 'c-all' },
  { value: 'ACCEPTED', label: '주문 확인', dot: true, cls: 'c-accepted' },
  { value: 'COOKING', label: '조리 중', dot: true, cls: 'c-cooking' },
  { value: 'COMPLETED', label: '완료', dot: true, cls: 'c-done' },
  { value: 'CANCELLED', label: '취소됨', dot: true, cls: 'c-cancel' },
]

// formatPrice, formatDate, formatDateFull, elapsed, summarize, statusLabel, statusClass,
// nextStatus, nextLabel, flowBtnClass, canCancel — imported from utils

function dailyNumber(order, idx) {
  const d = (order.createdAt || order.orderTime || '').substring(0, 10)
  let n = 1
  for (let i = 0; i < idx; i++) {
    if ((orders.value[i].createdAt || orders.value[i].orderTime || '').substring(0, 10) === d) n++
  }
  return String(n).padStart(3, '0')
}

async function fetchOrders() {
  loading.value = true; error.value = ''
  try {
    const params = { page: page.value, size: 100 }
    if (selectedStatus.value) params.status = selectedStatus.value
    const { list: raw, totalPages: tp } = extractListData(await getOrders(params))
    orders.value = raw
      .filter(o => o.status !== 'PENDING')
      .filter(o => {
        const d = (o.createdAt || o.orderTime || '').substring(0, 10)
        return d >= dateFrom.value && d <= dateTo.value
      })
    totalPages.value = tp
  } catch { error.value = '주문 목록을 불러오지 못했습니다.' } finally { loading.value = false }
}

function selectStatus(s) { selectedStatus.value = s; page.value = 0; fetchOrders() }
function changePage(p) { page.value = p; fetchOrders() }

async function changeStatus(order, newStatus) {
  const id = order.orderId || order.id
  if (newStatus === order.status) return
  updatingId.value = id
  try { await runAsync(() => updateOrderStatus(id, newStatus), '상태 변경 실패'); order.status = newStatus }
  catch {} finally { updatingId.value = null }
}

async function cancelOrder(order) {
  const id = order.orderId || order.id
  if (!confirm(`주문 #${id}을(를) 취소하시겠습니까?`)) return
  updatingId.value = id
  try { await runAsync(() => updateOrderStatus(id, 'CANCELLED'), '취소 실패'); order.status = 'CANCELLED' }
  catch {} finally { updatingId.value = null }
}

onMounted(() => {
  fetchPending(); fetchOrders()
  pendingTimer = setInterval(fetchPending, 12000)
  document.addEventListener('click', onClickOutside)
})
onUnmounted(() => {
  if (pendingTimer) clearInterval(pendingTimer)
  document.removeEventListener('click', onClickOutside)
})
</script>

<style scoped>
/* ═══════════════════════════════════════════
   TAB BAR
   ═══════════════════════════════════════════ */
.tab-bar {
  display: flex;
  position: relative;
  margin-bottom: 28px;
  border-bottom: 1px solid var(--border-strong);
}
.tab {
  flex: 0 0 auto;
  padding: 12px 28px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-muted);
  background: none;
  border: none;
  cursor: pointer;
  font-family: inherit;
  transition: color 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
  z-index: 1;
}
.tab:hover { color: var(--text-secondary); }
.tab.active { color: var(--accent-brass); }
.tab-inner { display: flex; align-items: center; gap: 8px; }
.tab-indicator {
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 50%;
  height: 2px;
  background: var(--accent-brass);
  border-radius: 2px 2px 0 0;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.tab-badge {
  background: var(--accent-brass);
  color: var(--text-inverse);
  font-size: 11px;
  font-weight: 700;
  padding: 1px 8px;
  border-radius: 10px;
  min-width: 22px;
  text-align: center;
  line-height: 18px;
}
.badge-pop-enter-active { animation: pop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
@keyframes pop { from { transform: scale(0); opacity: 0; } to { transform: scale(1); opacity: 1; } }

/* ═══════════════════════════════════════════
   TAB TRANSITIONS
   ═══════════════════════════════════════════ */
.tab-fade-enter-active { animation: fadeUp 0.3s ease; }
.tab-fade-leave-active { animation: fadeUp 0.15s ease reverse; }
@keyframes fadeUp { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }

.detail-slide-enter-active { animation: slideIn 0.25s ease; }
.detail-slide-leave-active { animation: slideIn 0.12s ease reverse; }
@keyframes slideIn { from { opacity: 0; transform: translateX(12px); } to { opacity: 1; transform: translateX(0); } }

/* ═══════════════════════════════════════════
   SPLIT PANE — 신규 주문
   ═══════════════════════════════════════════ */
.split-pane {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 20px;
  min-height: 520px;
}

/* Panel shared */
.queue-panel, .detail-panel {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}
.ph-left { display: flex; align-items: center; gap: 10px; }
.ph-label { font-size: 12px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.08em; }
.ph-count { font-size: 12px; font-weight: 600; color: var(--accent-brass); }

.panel-empty {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 6px; color: var(--text-muted); padding: 40px;
}
.empty-icon { font-size: 24px; opacity: 0.3; }
.empty-text { font-size: 13px; }

.icon-btn {
  width: 32px; height: 32px; border-radius: var(--radius-sm);
  border: 1px solid var(--border-strong); background: transparent;
  color: var(--text-muted); cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.icon-btn:hover { color: var(--text-primary); border-color: var(--accent-brass-border); background: var(--bg-hover); }
.icon-btn-danger:hover { color: var(--danger); border-color: rgba(200,120,100,0.3); background: var(--danger-bg); }

/* ── Queue Cards ── */
.queue-scroll { flex: 1; overflow-y: auto; padding: 10px; display: flex; flex-direction: column; gap: 6px; }

.queue-card {
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  cursor: pointer;
  background: transparent;
  text-align: left;
  font-family: inherit;
  color: var(--text-primary);
  transition: all 0.15s;
  animation: cardIn 0.35s ease both;
}
@keyframes cardIn { from { opacity: 0; transform: translateY(8px); } }

.queue-card:hover { border-color: var(--border-strong); background: var(--bg-hover); }
.queue-card.selected {
  border-color: var(--accent-brass);
  background: var(--accent-brass-glow);
  box-shadow: inset 3px 0 0 var(--accent-brass);
}

.qc-row { display: flex; justify-content: space-between; align-items: center; }
.qc-id { font-family: 'SF Mono', 'Fira Code', monospace; font-weight: 700; font-size: 14px; color: var(--accent-brass); }
.qc-elapsed { font-size: 11px; color: var(--text-muted); }
.qc-items { font-size: 13px; color: var(--text-secondary); margin: 6px 0 4px; line-height: 1.4; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.qc-price { font-size: 15px; font-weight: 700; font-variant-numeric: tabular-nums; }
.qc-count { font-size: 11px; color: var(--text-muted); }

/* ── Detail Panel ── */
.detail-content { display: flex; flex-direction: column; height: 100%; }
.detail-badge { font-family: 'SF Mono', monospace; font-size: 15px; font-weight: 700; color: var(--accent-brass); }
.detail-time { font-size: 12px; color: var(--text-muted); }

.detail-meta { display: flex; gap: 8px; padding: 16px 20px 12px; flex-wrap: wrap; }
.meta-chip {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 14px; border-radius: 99px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  font-size: 13px; font-weight: 500; color: var(--text-secondary);
}
.meta-chip.accent { color: var(--accent-brass); font-weight: 700; border-color: var(--accent-brass-border); background: var(--accent-brass-glow); font-size: 15px; }

.detail-items { flex: 1; overflow-y: auto; border-top: 1px solid var(--border); }
.di-header { padding: 12px 20px; font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.08em; }
.di-row { display: flex; align-items: center; padding: 12px 20px; border-top: 1px solid var(--border-subtle); gap: 12px; }
.di-left { flex: 1; min-width: 0; }
.di-name { font-size: 14px; font-weight: 500; }
.di-qty { font-size: 13px; color: var(--text-muted); font-family: 'SF Mono', monospace; }
.di-price { font-size: 14px; font-weight: 600; color: var(--accent-brass); font-variant-numeric: tabular-nums; min-width: 80px; text-align: right; }

.detail-actions {
  display: flex; gap: 12px; padding: 20px;
  border-top: 1px solid var(--border);
  flex-shrink: 0;
}
.act-btn {
  flex: 1; padding: 14px; border-radius: var(--radius-md);
  font-size: 14px; font-weight: 700; font-family: inherit; cursor: pointer; transition: all 0.15s;
  display: flex; align-items: center; justify-content: center; gap: 8px; border: none;
}
.act-accept { background: var(--accent-brass); color: var(--text-inverse); }
.act-accept:hover:not(:disabled) { filter: brightness(1.1); box-shadow: 0 4px 20px rgba(212,134,60,0.25); }
.act-reject { background: var(--danger-bg); color: var(--danger); border: 1px solid rgba(200,120,100,0.2); }
.act-reject:hover:not(:disabled) { background: rgba(200,120,100,0.18); }
.act-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.detail-empty {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; padding: 40px;
}
.de-visual { margin-bottom: 8px; color: var(--text-muted); }
.de-text { font-size: 15px; font-weight: 600; color: var(--text-secondary); }
.de-sub { font-size: 12px; color: var(--text-muted); text-align: center; max-width: 220px; line-height: 1.5; }

/* ═══════════════════════════════════════════
   LIST VIEW — 주문 목록
   ═══════════════════════════════════════════ */
.list-toolbar { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 20px; flex-wrap: wrap; }
.filter-chips { display: flex; gap: 6px; flex-wrap: wrap; }

.chip {
  padding: 6px 14px; border-radius: 99px; border: 1px solid var(--border-strong);
  background: transparent; font-size: 12px; font-weight: 600; color: var(--text-secondary);
  cursor: pointer; transition: all 0.15s; font-family: inherit;
  display: flex; align-items: center; gap: 6px;
}
.chip:hover { border-color: var(--accent-brass-border); color: var(--text-primary); }
.chip.active { background: var(--accent-brass-glow); border-color: var(--accent-brass-border); color: var(--accent-brass); }
.chip-dot { width: 6px; height: 6px; border-radius: 50%; }
.c-pending .chip-dot { background: var(--accent-brass); }
.c-accepted .chip-dot { background: var(--accent-brass); }
.c-cooking .chip-dot { background: var(--accent-brass); }
.c-done .chip-dot { background: var(--text-muted); }
.c-cancel .chip-dot { background: var(--danger); }
.chip-clear { color: var(--text-muted); border-style: dashed; }
.chip-clear:hover { color: var(--danger); border-color: rgba(200,120,100,0.3); }

/* ── Date Range ── */
.date-range-wrap { position: relative; }
.date-range-btn {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 14px; border: 1px solid var(--border-strong); border-radius: var(--radius-sm);
  background: transparent; color: var(--text-secondary); font-size: 13px; font-weight: 500;
  font-family: inherit; cursor: pointer; transition: all 0.15s; white-space: nowrap;
}
.date-range-btn:hover { border-color: var(--accent-brass-border); color: var(--text-primary); }

.date-dropdown {
  position: absolute; top: calc(100% + 6px); right: 0;
  background: var(--bg-elevated); border: 1px solid var(--border-strong); border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg); z-index: 50; min-width: 280px;
  animation: ddIn 0.15s ease;
}
@keyframes ddIn { from { opacity: 0; transform: translateY(-4px); } }

.dd-presets { display: flex; gap: 4px; padding: 12px; border-bottom: 1px solid var(--border); flex-wrap: wrap; }
.dd-preset {
  padding: 5px 12px; border-radius: 99px; border: 1px solid var(--border-strong);
  background: transparent; color: var(--text-secondary); font-size: 12px; font-weight: 600;
  font-family: inherit; cursor: pointer; transition: all 0.15s;
}
.dd-preset:hover { border-color: var(--accent-brass-border); color: var(--text-primary); }
.dd-preset.active { background: var(--accent-brass-glow); border-color: var(--accent-brass-border); color: var(--accent-brass); }

.dd-custom { padding: 12px; display: flex; align-items: flex-end; gap: 8px; flex-wrap: wrap; }
.dd-label { display: flex; flex-direction: column; gap: 4px; font-size: 11px; color: var(--text-muted); font-weight: 600; }
.dd-input {
  padding: 6px 10px; border: 1px solid var(--border-strong); border-radius: var(--radius-sm);
  background: var(--bg-input); color: var(--text-primary); font-family: inherit; font-size: 13px;
  outline: none; color-scheme: dark; width: 140px;
}
.dd-input:focus { border-color: var(--accent-brass); }
.dd-sep { color: var(--text-muted); font-size: 13px; padding-bottom: 6px; }
.dd-apply {
  padding: 6px 16px; border-radius: var(--radius-sm); border: none;
  background: var(--accent-brass); color: var(--text-inverse); font-size: 12px; font-weight: 700;
  font-family: inherit; cursor: pointer; transition: all 0.15s;
}
.dd-apply:hover { filter: brightness(1.1); }

/* Table cells */
.cell-id { font-family: 'SF Mono', monospace; font-weight: 700; color: var(--accent-brass); font-size: 13px; }
.cell-menu { font-size: 13px; color: var(--text-secondary); max-width: 260px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cell-price { font-weight: 600; font-variant-numeric: tabular-nums; }
.cell-time { font-size: 12px; color: var(--text-muted); }
.cell-done { color: var(--text-muted); }

/* Status pills */
.status-pill {
  display: inline-block; padding: 3px 10px; border-radius: 99px;
  font-size: 11px; font-weight: 700; letter-spacing: 0.02em;
}
.pill-pending { background: var(--status-active-bg); color: var(--status-active-text); }
.pill-active { background: var(--status-active-bg); color: var(--status-active-text); }
.pill-done { background: var(--status-done-bg); color: var(--status-done-text); }
.pill-cancel { background: var(--status-danger-bg); color: var(--status-danger-text); }

/* Row actions */
.row-actions { display: flex; align-items: center; gap: 6px; }
.flow-btn {
  padding: 5px 14px; border-radius: var(--radius-sm); border: 1px solid var(--border-strong);
  background: transparent; color: var(--text-secondary);
  font-size: 12px; font-weight: 600; font-family: inherit; cursor: pointer; transition: all 0.15s; white-space: nowrap;
}
.flow-btn:hover:not(:disabled) { color: var(--text-primary); }
.flow-accept, .flow-cooking, .flow-complete {
  border-color: var(--accent-brass-border); color: var(--accent-brass);
}
.flow-accept:hover:not(:disabled), .flow-cooking:hover:not(:disabled), .flow-complete:hover:not(:disabled) {
  background: var(--accent-brass-glow);
}
.flow-btn:disabled { opacity: 0.35; cursor: not-allowed; }

/* Spinner */
.spinner { width: 18px; height: 18px; border: 2px solid var(--border-strong); border-top-color: var(--accent-brass); border-radius: 50%; animation: spin 0.6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
