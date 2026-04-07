<template>
  <div class="detail-wrap">
    <header class="detail-header">
      <button class="back-btn" @click="$router.push('/orders')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><polyline points="15,18 9,12 15,6"/></svg>
        주문 목록
      </button>
      <div>
        <h1 class="display detail-title">Order <span class="detail-id mono">#{{ String(route.params.id).padStart(4, '0') }}</span></h1>
      </div>
    </header>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <span>불러오는 중...</span>
    </div>

    <div v-else-if="order" class="detail-body">
      <!-- Status banner -->
      <div class="status-banner" :class="`sb-${(order.status||'').toLowerCase()}`">
        <div class="sb-icon">{{ statusIcon }}</div>
        <div>
          <div class="sb-label">{{ statusLabel }}</div>
          <div class="sb-sub">{{ statusDesc }}</div>
        </div>
      </div>

      <!-- Info grid -->
      <div class="info-grid">
        <div class="info-cell">
          <span class="info-key">주문 시각</span>
          <span class="info-val">{{ formatDate(order.createdAt) }}</span>
        </div>
        <div class="info-cell">
          <span class="info-key">총 금액</span>
          <span class="info-val accent mono">{{ formatPrice(order.totalPrice) }}원</span>
        </div>
      </div>

      <!-- Items -->
      <div class="items-section">
        <h3 class="section-title">주문 항목</h3>
        <div v-for="item in order.items" :key="item.id" class="item-row">
          <span class="item-name">{{ item.menuName }}</span>
          <span class="item-qty mono">×{{ item.quantity }}</span>
          <span class="item-price mono">{{ formatPrice(item.price * item.quantity) }}원</span>
        </div>
      </div>

      <!-- Review Section -->
      <div class="review-section" v-if="order.status === 'COMPLETED' || order.status === 'READY'">
        <div v-if="reviewSubmitted" class="review-done">
          <span class="review-done-icon">✓</span> 리뷰가 등록되었습니다
        </div>
        <template v-else>
          <h3 class="section-title">리뷰 작성</h3>
          <div class="review-inner">
            <div class="star-row">
              <button v-for="n in 5" :key="n" class="star-btn" :class="{ filled: reviewRating >= n }" @click="reviewRating = n">★</button>
              <span v-if="reviewRating > 0" class="star-label">{{ ['', '별로예요', '그저 그래요', '괜찮아요', '맛있어요', '최고예요'][reviewRating] }}</span>
            </div>

            <div class="kw-chips">
              <button v-for="kw in kwOptions" :key="kw" class="kw-chip" :class="{ sel: reviewKws.includes(kw) }" @click="toggleKw(kw)">{{ kw }}</button>
            </div>

            <button class="btn-ai-draft" :disabled="draftLoading || reviewRating === 0" @click="genDraft">
              <span v-if="draftLoading" class="spinner"></span>
              {{ draftLoading ? 'AI 작성 중...' : '✦ AI로 리뷰 초안 생성' }}
            </button>

            <textarea v-model="reviewText" class="review-ta" rows="3" placeholder="리뷰를 작성하세요..."></textarea>

            <button class="btn-submit-review" :disabled="!reviewText.trim() || submitting" @click="submitReview">
              {{ submitting ? '등록 중...' : '리뷰 등록' }}
            </button>
          </div>
        </template>
      </div>

      <!-- WebSocket notification -->
      <transition name="toast-pop">
        <div v-if="wsMessage" class="ws-toast">
          <span class="ws-dot"></span> {{ wsMessage }}
        </div>
      </transition>
    </div>

    <div v-else class="empty">
      <span class="empty-glyph">?</span>
      <span>주문 정보를 찾을 수 없습니다</span>
      <router-link to="/orders" class="empty-link">목록으로 →</router-link>
    </div>
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

const STATUS = {
  PENDING:   { label: '접수 대기',  icon: '⏳', desc: '주문이 접수되길 기다리고 있어요' },
  ACCEPTED:  { label: '주문 수락',  icon: '✓',  desc: '주문이 접수되었습니다' },
  COOKING:   { label: '조리 중',    icon: '🔥', desc: '셰프가 조리하고 있어요' },
  READY:     { label: '준비 완료',  icon: '✨', desc: '음식이 준비되었습니다!' },
  COMPLETED: { label: '완료',       icon: '✓',  desc: '수령 완료' },
  CANCELLED: { label: '취소됨',     icon: '✕',  desc: '주문이 취소되었습니다' },
}

const statusLabel = computed(() => STATUS[order.value?.status]?.label || order.value?.status)
const statusIcon = computed(() => STATUS[order.value?.status]?.icon || '?')
const statusDesc = computed(() => STATUS[order.value?.status]?.desc || '')

// ── Review ──
const reviewRating = ref(0)
const reviewText = ref('')
const reviewKws = ref([])
const draftLoading = ref(false)
const submitting = ref(false)
const reviewSubmitted = ref(false)
const kwOptions = ['맛있다', '양이 많다', '빠르다', '친절하다', '국물이 좋다', '매콤하다', '신선하다', '가성비 좋다']

function toggleKw(kw) {
  const i = reviewKws.value.indexOf(kw)
  i >= 0 ? reviewKws.value.splice(i, 1) : reviewKws.value.push(kw)
}

async function genDraft() {
  draftLoading.value = true
  try {
    const menuName = order.value.items?.map(i => i.menuName).join(', ') || ''
    const r = await api.post('/reviews/generate', { menu_name: menuName, rating: reviewRating.value, keywords: reviewKws.value })
    reviewText.value = r.data.draft || r.data.review || ''
  } catch {} finally { draftLoading.value = false }
}

async function submitReview() {
  if (!reviewText.value.trim()) return
  submitting.value = true
  try {
    await api.post('/reviews', { orderId: order.value.id, rating: reviewRating.value, content: reviewText.value })
    reviewSubmitted.value = true
  } catch {} finally { submitting.value = false }
}

function formatPrice(p) { return Number(p).toLocaleString('ko-KR') }
function formatDate(s) {
  if (!s) return ''
  return new Date(s).toLocaleString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function loadOrder() {
  try {
    const res = await api.get(`/orders/${route.params.id}`)
    order.value = res.data
  } finally { loading.value = false }
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
        wsMessage.value = `상태 변경: ${STATUS[data.status]?.label || data.status}`
        setTimeout(() => { wsMessage.value = '' }, 5000)
      }
    } catch {}
  }
}

onMounted(async () => { await loadOrder(); connectWs() })
onUnmounted(() => { if (ws) ws.close() })
</script>

<style scoped>
.detail-wrap {
  max-width: 600px;
  margin: 0 auto;
  padding: 40px 36px 80px;
  min-height: 100vh;
}

.detail-header { margin-bottom: 32px; }
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

.detail-title { font-size: 36px; color: var(--text-primary); margin: 0; }
.detail-id { color: var(--accent-soft); font-size: 28px; }

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 60px;
  color: var(--text-muted);
  font-size: 14px;
}

.detail-body { display: flex; flex-direction: column; gap: 20px; }

/* Status banner */
.status-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
}
.sb-icon { font-size: 28px; width: 48px; height: 48px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; }
.sb-label { font-size: 16px; font-weight: 700; color: var(--text-primary); }
.sb-sub { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }

.sb-pending   { background: rgba(251,191,36,0.06); }
.sb-pending .sb-icon { background: rgba(251,191,36,0.12); }
.sb-accepted  { background: rgba(96,165,250,0.06); }
.sb-accepted .sb-icon { background: rgba(96,165,250,0.12); }
.sb-cooking   { background: rgba(251,146,60,0.06); }
.sb-cooking .sb-icon { background: rgba(251,146,60,0.12); }
.sb-ready     { background: rgba(74,222,128,0.06); }
.sb-ready .sb-icon { background: rgba(74,222,128,0.12); }
.sb-completed { background: var(--bg-card); }
.sb-completed .sb-icon { background: var(--bg-subtle); }
.sb-cancelled { background: rgba(248,113,113,0.06); }
.sb-cancelled .sb-icon { background: rgba(248,113,113,0.12); }

/* Info grid */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.info-cell {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.info-key { font-size: 11px; color: var(--text-muted); font-weight: 600; text-transform: uppercase; letter-spacing: 0.08em; }
.info-val { font-size: 15px; font-weight: 600; color: var(--text-primary); }
.info-val.accent { color: var(--accent-soft); }

/* Items */
.items-section {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.section-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  padding: 16px 20px 10px;
  margin: 0;
}
.item-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-top: 1px solid var(--border);
}
.item-name { flex: 1; font-size: 14px; font-weight: 500; color: var(--text-primary); }
.item-qty { font-size: 13px; color: var(--text-muted); }
.item-price { font-size: 14px; font-weight: 600; color: var(--accent-soft); }

/* Review */
.review-section {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.review-inner { padding: 16px 20px 20px; display: flex; flex-direction: column; gap: 14px; }

.star-row { display: flex; align-items: center; gap: 4px; }
.star-btn { font-size: 26px; color: var(--bg-subtle); transition: var(--transition); background: none; border: none; cursor: pointer; line-height: 1; padding: 0; }
.star-btn.filled { color: #fbbf24; text-shadow: 0 0 8px rgba(251,191,36,0.35); }
.star-btn:hover { transform: scale(1.15); }
.star-label { font-size: 12px; color: var(--text-secondary); margin-left: 8px; }

.kw-chips { display: flex; flex-wrap: wrap; gap: 6px; }
.kw-chip { background: var(--bg-subtle); border: 1px solid var(--border); border-radius: 99px; padding: 5px 12px; font-size: 12px; color: var(--text-secondary); font-family: inherit; cursor: pointer; transition: var(--transition); }
.kw-chip:hover { border-color: var(--border-hover); color: var(--text-primary); }
.kw-chip.sel { background: var(--accent-bg, rgba(212,134,60,0.06)); border-color: var(--accent, #d4863c); color: var(--accent-soft, #e8a862); }

.btn-ai-draft { background: var(--bg-elevated); border: 1px dashed var(--accent, #d4863c); color: var(--accent-soft, #e8a862); padding: 10px 16px; border-radius: var(--radius-md); font-size: 13px; font-weight: 600; font-family: inherit; cursor: pointer; transition: var(--transition); display: flex; align-items: center; justify-content: center; gap: 6px; }
.btn-ai-draft:hover:not(:disabled) { background: var(--accent-bg, rgba(212,134,60,0.06)); }
.btn-ai-draft:disabled { opacity: 0.4; cursor: not-allowed; }

.review-ta { width: 100%; padding: 10px 14px; background: var(--bg-elevated); border: 1px solid var(--border); border-radius: var(--radius-md); color: var(--text-primary); font-family: inherit; font-size: 13px; resize: vertical; min-height: 70px; outline: none; transition: var(--transition); }
.review-ta::placeholder { color: var(--text-muted); }
.review-ta:focus { border-color: var(--accent, #d4863c); }

.btn-submit-review { background: var(--accent, #d4863c); color: #fff; border: none; border-radius: var(--radius-md); padding: 11px; font-size: 14px; font-weight: 700; font-family: inherit; cursor: pointer; transition: var(--transition); }
.btn-submit-review:hover:not(:disabled) { background: var(--accent-soft, #e8a862); }
.btn-submit-review:disabled { opacity: 0.4; cursor: not-allowed; }

.review-done { padding: 20px; display: flex; align-items: center; gap: 8px; font-size: 14px; color: #4ade80; font-weight: 600; }
.review-done-icon { width: 28px; height: 28px; background: rgba(74,222,128,0.12); border-radius: 99px; display: flex; align-items: center; justify-content: center; font-size: 14px; }

/* WS notification */
.ws-toast {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 18px;
  background: rgba(74,222,128,0.08);
  border: 1px solid rgba(74,222,128,0.2);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: #4ade80;
  font-weight: 500;
}
.ws-dot {
  width: 8px; height: 8px;
  background: #4ade80;
  border-radius: 99px;
  animation: pulse 1.5s infinite;
}
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }

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
.empty-link { font-size: 13px; color: var(--accent-soft); text-decoration: none; }

.toast-pop-enter-active { animation: fadeIn 0.3s ease; }
.toast-pop-leave-active { animation: fadeIn 0.2s ease reverse; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
</style>
