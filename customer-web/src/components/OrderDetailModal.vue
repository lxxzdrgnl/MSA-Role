<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="od">
      <!-- Header -->
      <div class="od-head">
        <div class="od-head-left">
          <span class="od-tag">ORDER DETAIL</span>
          <h3 class="od-title">#{{ String(orderId).padStart(4,'0') }}</h3>
        </div>
        <button class="modal-x" @click="$emit('close')">✕</button>
      </div>

      <div v-if="loading" class="od-loading"><span class="spinner"></span></div>

      <div v-else-if="order" class="od-body">
        <!-- Status banner -->
        <div class="od-status" :class="`ods-${(order.status||'').toLowerCase()}`">
          <span class="od-status-icon">{{ ST[order.status]?.icon || '?' }}</span>
          <div class="od-status-text">
            <span class="od-status-label">{{ ST[order.status]?.label || order.status }}</span>
            <span class="od-status-desc">{{ ST[order.status]?.desc }}</span>
          </div>
        </div>

        <!-- Info cards -->
        <div class="od-info">
          <div class="od-info-item">
            <span class="od-info-k">주문 시각</span>
            <span class="od-info-v">{{ fmtDate(order.createdAt) }}</span>
          </div>
          <div class="od-info-item">
            <span class="od-info-k">총 금액</span>
            <span class="od-info-v od-info-accent mono">{{ fmtPrice(order.totalPrice) }}원</span>
          </div>
        </div>

        <!-- Items -->
        <div class="od-items-section">
          <div class="od-section-label">주문 항목</div>
          <div class="od-items">
            <div v-for="(it, idx) in order.items" :key="it.id" class="od-item" :style="{ animationDelay: `${idx * 40}ms` }">
              <div class="od-item-img-wrap">
                <img v-if="menuImages[it.menuId]" :src="menuImages[it.menuId]" alt="" class="od-item-img" @error="e => e.target.style.display='none'" />
                <div v-else class="od-item-img od-item-img-ph">{{ it.menuName?.charAt(0) || '?' }}</div>
              </div>
              <div class="od-item-info">
                <span class="od-item-name">{{ it.menuName }}</span>
                <span class="od-item-sub mono">{{ fmtPrice(it.price) }}원 × {{ it.quantity }}</span>
              </div>
              <span class="od-item-total mono">{{ fmtPrice(it.price * it.quantity) }}원</span>
            </div>
          </div>
        </div>

        <!-- Review section -->
        <div v-if="order.status !== 'CANCELLED'" class="od-review">
          <div class="od-section-label">{{ existingReview && !editing ? '내 리뷰' : editing ? '리뷰 수정' : '리뷰 작성' }}</div>

          <!-- Existing review -->
          <template v-if="existingReview && !editing">
            <div class="rv-card">
              <div class="rv-top">
                <span class="rv-stars">{{ '★'.repeat(existingReview.rating) }}{{ '☆'.repeat(5 - existingReview.rating) }}</span>
                <span class="rv-date">{{ fmtDate(existingReview.createdAt) }}</span>
              </div>
              <p class="rv-content">{{ existingReview.content }}</p>
              <div class="rv-actions">
                <button class="rv-btn" @click="startEdit">수정</button>
                <button class="rv-btn rv-btn-del" @click="deleteRv">삭제</button>
              </div>
            </div>
          </template>

          <!-- Write / Edit -->
          <template v-else>
            <ReviewForm
              :menu-name="order.items?.map(i => i.menuName).join(', ') || ''"
              :initial-rating="editing ? rvRating : 0"
              :initial-content="editing ? rvText : ''"
              :submit-label="editing ? '수정 완료' : '리뷰 등록'"
              :show-cancel="editing"
              :submitting="rvSubmitting"
              @submit="onFormSubmit"
              @cancel="editing = false"
            />
          </template>
        </div>

        <!-- WS live update -->
        <transition name="ws-fade">
          <div v-if="wsMsg" class="od-ws"><span class="od-ws-dot"></span>{{ wsMsg }}</div>
        </transition>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import api from '../api'
import { useFormatting } from '../composables/useFormatting'
import ReviewForm from './ReviewForm.vue'

const props = defineProps({ orderId: Number })
const emit = defineEmits(['close'])

const { formatPrice: fmtPrice, formatDate: fmtDate } = useFormatting()
const order = ref(null), loading = ref(true), wsMsg = ref('')
const menuImages = ref({})
let ws = null

const ST = {
  PENDING:   { label: '접수 대기', icon: '⏳', desc: '주문이 접수되길 기다리고 있어요' },
  ACCEPTED:  { label: '주문 수락', icon: '✓',  desc: '주문이 접수되었습니다' },
  COOKING:   { label: '조리 중',   icon: '🔥', desc: '셰프가 조리하고 있어요' },
  READY:     { label: '준비 완료', icon: '✨', desc: '음식이 준비되었습니다!' },
  COMPLETED: { label: '완료',     icon: '✓',  desc: '수령 완료' },
  CANCELLED: { label: '취소됨',   icon: '✕',  desc: '주문이 취소되었습니다' },
}

// Review
const existingReview = ref(null)
const existingReviews = ref([])
const editing = ref(false)
const rvRating = ref(0), rvText = ref(''), rvSubmitting = ref(false)

function startEdit() {
  rvRating.value = existingReview.value.rating
  rvText.value = existingReview.value.content
  editing.value = true
}

async function onFormSubmit({ rating, content }) {
  rvSubmitting.value = true
  try {
    if (editing.value && existingReview.value) {
      for (const rv of existingReviews.value) {
        try { await api.delete(`/reviews/${rv.id}`) } catch {}
      }
    }
    let lastRes = null
    for (const item of (order.value.items || [])) {
      lastRes = await api.post('/reviews', {
        orderId: order.value.id,
        menuId: item.menuId || item.id,
        menuName: item.menuName || item.name || '',
        rating,
        content
      })
    }
    if (lastRes) existingReview.value = lastRes.data
    await loadExistingReview()
    editing.value = false
  } catch {} finally { rvSubmitting.value = false }
}

async function deleteRv() {
  if (!confirm('리뷰를 삭제하시겠습니까?')) return
  try {
    for (const rv of existingReviews.value) await api.delete(`/reviews/${rv.id}`)
    existingReview.value = null
    existingReviews.value = []
  } catch {}
}

async function loadExistingReview() {
  try {
    const r = await api.get('/reviews/by-order', { params: { orderId: props.orderId } })
    const reviews = r.data.content || []
    existingReviews.value = reviews
    if (reviews.length > 0) existingReview.value = reviews[0]
  } catch {}
}

async function loadMenuImages() {
  if (!order.value?.items) return
  const ids = order.value.items.map(i => i.menuId).filter(Boolean)
  const map = {}
  await Promise.all(ids.map(async id => {
    try {
      const res = await api.get(`/menus/${id}`)
      if (res.data?.imageUrl) map[id] = res.data.imageUrl
    } catch {}
  }))
  menuImages.value = map
}

onMounted(async () => {
  try { order.value = (await api.get(`/orders/${props.orderId}`)).data } catch {} finally { loading.value = false }
  loadMenuImages()
  loadExistingReview()
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const userId = order.value?.userId || props.orderId
  ws = new WebSocket(`${proto}://${location.host}/ws/orders/${userId}`)
  ws.onmessage = e => {
    try {
      const d = JSON.parse(e.data)
      if (d.status) {
        order.value.status = d.status
        wsMsg.value = ST[d.status]?.label || d.status
        setTimeout(() => { wsMsg.value = '' }, 5000)
      }
    } catch {}
  }
})
onUnmounted(() => { if (ws) ws.close() })
</script>

<style scoped>
/* ── Modal shell ── */
.od {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  width: 480px; max-height: 85vh;
  display: flex; flex-direction: column;
  box-shadow: 0 32px 80px rgba(0,0,0,0.7), 0 0 0 1px rgba(255,255,255,0.03) inset;
  animation: odIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes odIn { from { opacity: 0; transform: translateY(20px) scale(0.97); } to { opacity: 1; transform: translateY(0) scale(1); } }

.od-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 24px 28px 18px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}
.od-head-left { display: flex; flex-direction: column; gap: 2px; }
.od-tag { font-size: 11px; font-weight: 700; letter-spacing: 0.14em; color: var(--accent); }
.od-title { font-size: 22px; font-weight: 700; color: var(--text-primary); margin: 0; letter-spacing: -0.5px; }

.od-loading { display: flex; justify-content: center; padding: 48px; }

.od-body { flex: 1; overflow-y: auto; }

/* ── Status ── */
.od-status {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 18px; margin: 16px 20px 0;
  border-radius: var(--radius-md); border: 1px solid var(--border);
}
.od-status-icon {
  font-size: 20px; width: 40px; height: 40px;
  border-radius: var(--radius-sm);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.od-status-label { font-size: 14px; font-weight: 700; color: var(--text-primary); display: block; }
.od-status-desc { font-size: 12px; color: var(--text-secondary); }

.ods-pending { background: rgba(251,191,36,0.04); }
.ods-pending .od-status-icon { background: rgba(251,191,36,0.1); }
.ods-accepted { background: rgba(96,165,250,0.04); }
.ods-accepted .od-status-icon { background: rgba(96,165,250,0.1); }
.ods-cooking { background: rgba(251,146,60,0.04); }
.ods-cooking .od-status-icon { background: rgba(251,146,60,0.1); }
.ods-ready { background: rgba(74,222,128,0.04); }
.ods-ready .od-status-icon { background: rgba(74,222,128,0.1); }
.ods-completed { background: var(--bg-card); }
.ods-completed .od-status-icon { background: var(--bg-subtle); }
.ods-cancelled { background: rgba(248,113,113,0.04); }
.ods-cancelled .od-status-icon { background: rgba(248,113,113,0.1); }

/* ── Info ── */
.od-info {
  display: grid; grid-template-columns: 1fr 1fr; gap: 10px;
  padding: 14px 20px 0;
}
.od-info-item {
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius-sm); padding: 12px 14px;
}
.od-info-k {
  display: block; font-size: 11px; color: var(--text-muted);
  font-weight: 600; letter-spacing: 0.06em; text-transform: uppercase;
  margin-bottom: 4px;
}
.od-info-v { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.od-info-accent { color: var(--accent-soft); }

/* ── Items ── */
.od-items-section { padding: 16px 20px 0; }
.od-section-label {
  font-size: 11px; font-weight: 700; color: var(--text-muted);
  text-transform: uppercase; letter-spacing: 0.08em;
  margin-bottom: 10px;
}

.od-items {
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius-md); overflow: hidden;
}
.od-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border);
  animation: odItemIn 0.3s cubic-bezier(0.16, 1, 0.3, 1) both;
}
@keyframes odItemIn { from { opacity: 0; transform: translateX(-6px); } to { opacity: 1; transform: translateX(0); } }
.od-item:last-child { border-bottom: none; }

.od-item-img-wrap { flex-shrink: 0; }
.od-item-img {
  width: 48px; height: 48px; border-radius: 10px;
  object-fit: cover; display: block;
  border: 1px solid var(--border);
}
.od-item-img-ph {
  background: var(--bg-subtle);
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; font-weight: 700; color: var(--text-muted);
}

.od-item-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.od-item-name { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.od-item-sub { font-size: 12px; color: var(--text-muted); }
.od-item-total { font-size: 14px; font-weight: 700; color: var(--accent-soft); flex-shrink: 0; }

/* ── Review ── */
.od-review {
  margin: 16px 20px 20px;
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius-md); padding: 16px;
  display: flex; flex-direction: column; gap: 12px;
}

.rv-card {
  background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: var(--radius-sm); padding: 14px;
}
.rv-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.rv-stars { font-size: 14px; color: #fbbf24; letter-spacing: 1px; }
.rv-date { font-size: 12px; color: var(--text-muted); }
.rv-content { font-size: 13px; color: var(--text-secondary); line-height: 1.6; margin-bottom: 10px; }
.rv-actions { display: flex; gap: 8px; }
.rv-btn {
  background: var(--bg-subtle); border: 1px solid var(--border);
  border-radius: var(--radius-sm); padding: 6px 14px;
  font-size: 12px; color: var(--text-secondary); font-family: inherit;
  cursor: pointer; transition: var(--transition);
}
.rv-btn:hover { border-color: var(--border-hover); color: var(--text-primary); }
.rv-btn-del { color: #f87171; }
.rv-btn-del:hover { border-color: #f87171; background: rgba(248,113,113,0.06); }

.star-row { display: flex; gap: 2px; }
.star {
  font-size: 26px; color: var(--bg-subtle); background: none;
  border: none; cursor: pointer; transition: var(--transition);
  padding: 0; line-height: 1;
}
.star.on { color: #fbbf24; text-shadow: 0 0 8px rgba(251,191,36,0.3); }

.kw-row { display: flex; flex-wrap: wrap; gap: 5px; }
.kw {
  background: var(--bg-subtle); border: 1px solid var(--border);
  border-radius: 99px; padding: 5px 12px;
  font-size: 12px; color: var(--text-secondary); font-family: inherit;
  cursor: pointer; transition: var(--transition);
}
.kw.sel { background: var(--accent-bg); border-color: var(--accent); color: var(--accent-soft); }
.kw-custom { display: flex; flex-wrap: wrap; gap: 5px; align-items: center; }
.kw-custom .kw { display: flex; align-items: center; gap: 4px; }
.kw-x { cursor: pointer; font-size: 13px; opacity: 0.6; }
.kw-x:hover { opacity: 1; }
.kw-input-form { display: flex; gap: 4px; align-items: center; }
.kw-input {
  width: 100px; padding: 5px 12px;
  background: var(--bg-subtle); border: 1px dashed var(--border);
  border-radius: 99px; font-size: 12px; color: var(--text-primary);
  font-family: inherit; outline: none; transition: var(--transition);
}
.kw-input:focus { border-color: var(--accent); width: 130px; }
.kw-input::placeholder { color: var(--text-muted); }
.kw-add-btn {
  width: 26px; height: 26px; border-radius: 99px;
  background: var(--accent); border: none; color: #fff;
  font-size: 15px; display: flex; align-items: center; justify-content: center;
  cursor: pointer; flex-shrink: 0;
}

.btn-draft {
  background: var(--bg-elevated); border: 1px dashed var(--accent);
  color: var(--accent-soft); padding: 9px 16px;
  border-radius: var(--radius-sm); font-size: 13px; font-weight: 600;
  font-family: inherit; cursor: pointer; transition: var(--transition);
  display: flex; align-items: center; justify-content: center; gap: 6px;
}
.btn-draft:hover:not(:disabled) { background: var(--accent-dim); }
.btn-draft:disabled { opacity: 0.4; cursor: not-allowed; }

.rv-ta {
  width: 100%; padding: 10px 14px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: var(--radius-sm); color: var(--text-primary);
  font-family: inherit; font-size: 13px; resize: vertical;
  min-height: 70px; outline: none; line-height: 1.5;
}
.rv-ta:focus { border-color: var(--accent); }

.rv-submit-row { display: flex; gap: 8px; justify-content: flex-end; }
.rv-submit-row .rv-btn { flex: 0; }
.rv-submit-btn {
  flex: 1;
  background: var(--accent); color: #fff; border: none;
  border-radius: var(--radius-sm); padding: 11px;
  font-size: 14px; font-weight: 700; font-family: inherit;
  cursor: pointer; transition: var(--transition);
}
.rv-submit-btn:hover:not(:disabled) { background: var(--accent-soft); }
.rv-submit-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* ── WS ── */
.od-ws {
  margin: 12px 20px 20px; padding: 10px 16px;
  background: rgba(74,222,128,0.06); border: 1px solid rgba(74,222,128,0.15);
  border-radius: var(--radius-sm); font-size: 13px; color: #4ade80;
  display: flex; align-items: center; gap: 8px;
}
.od-ws-dot {
  width: 6px; height: 6px; background: #4ade80;
  border-radius: 99px; animation: wsPulse 1.5s infinite;
}
@keyframes wsPulse { 0%,100% { opacity: 1; } 50% { opacity: 0.3; } }
.ws-fade-enter-active { animation: wsFade 0.3s ease; }
.ws-fade-leave-active { animation: wsFade 0.2s ease reverse; }
@keyframes wsFade { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
</style>
